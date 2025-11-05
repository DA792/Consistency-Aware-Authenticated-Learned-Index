package Rtree;

import java.util.ArrayList;
import java.util.List;

/**
 * R-tree节点类
 * 支持内部节点和叶子节点
 */
public class RTreeNode {
    private static final int MAX_ENTRIES = 10; // 最大条目数
    private static final int MIN_ENTRIES = 4;  // 最小条目数
    
    private List<RTreeEntry> entries;
    private boolean isLeaf;
    private Rectangle2D1 mbr; // 最小边界矩形
    private RTreeNode parent;
    
    public RTreeNode(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.entries = new ArrayList<>();
        this.mbr = null;
        this.parent = null;
    }
    
    /**
     * 添加条目到节点
     */
    public void addEntry(RTreeEntry entry) {
        entries.add(entry);
        if (entry.getChild() != null) {
            entry.getChild().setParent(this);
        }
        updateMBR();
    }
    
    /**
     * 移除条目
     */
    public void removeEntry(RTreeEntry entry) {
        entries.remove(entry);
        if (entry.getChild() != null) {
            entry.getChild().setParent(null);
        }
        updateMBR();
    }
    
    /**
     * 更新最小边界矩形
     */
    private void updateMBR() {
        if (entries.isEmpty()) {
            mbr = null;
            return;
        }
        
        long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE;
        
        for (RTreeEntry entry : entries) {
            Rectangle2D1 rect = entry.getMBR();
            minX = Math.min(minX, rect.minX);
            minY = Math.min(minY, rect.minY);
            maxX = Math.max(maxX, rect.maxX);
            maxY = Math.max(maxY, rect.maxY);
        }
        
        mbr = new Rectangle2D1(minX, minY, maxX, maxY);
    }
    
    /**
     * 检查节点是否需要分裂
     */
    public boolean needsSplit() {
        return entries.size() > MAX_ENTRIES;
    }
    
    /**
     * 检查节点是否条目过少
     */
    public boolean hasUnderflow() {
        return entries.size() < MIN_ENTRIES;
    }
    
    /**
     * 分裂节点
     * 使用线性分裂算法
     */
    public RTreeNode split() {
        if (!needsSplit()) {
            return null;
        }
        
        RTreeNode newNode = new RTreeNode(isLeaf);
        
        // 找到最远的两个条目作为种子
        int[] seeds = findSeeds();
        RTreeEntry seed1 = entries.get(seeds[0]);
        RTreeEntry seed2 = entries.get(seeds[1]);
        
        // 创建两个组
        List<RTreeEntry> group1 = new ArrayList<>();
        List<RTreeEntry> group2 = new ArrayList<>();
        
        group1.add(seed1);
        group2.add(seed2);
        
        // 移除种子
        entries.remove(seed1);
        entries.remove(seed2);
        
        // 分配剩余条目
        while (!entries.isEmpty()) {
            RTreeEntry entry = entries.remove(0);
            
            // 计算添加到每个组的面积增长
            long growth1 = calculateAreaGrowth(group1, entry);
            long growth2 = calculateAreaGrowth(group2, entry);
            
            if (growth1 < growth2) {
                group1.add(entry);
            } else if (growth2 < growth1) {
                group2.add(entry);
            } else {
                // 面积增长相同，选择面积较小的组
                long area1 = calculateGroupArea(group1);
                long area2 = calculateGroupArea(group2);
                if (area1 <= area2) {
                    group1.add(entry);
                } else {
                    group2.add(entry);
                }
            }
        }
        
        // 重新分配条目
        entries.clear();
        for (RTreeEntry entry : group1) {
            addEntry(entry);
        }
        
        for (RTreeEntry entry : group2) {
            newNode.addEntry(entry);
        }
        
        return newNode;
    }
    
    /**
     * 找到最远的两个条目作为分裂种子
     */
    private int[] findSeeds() {
        int seed1 = 0, seed2 = 1;
        long maxWaste = -1;
        
        for (int i = 0; i < entries.size(); i++) {
            for (int j = i + 1; j < entries.size(); j++) {
                Rectangle2D1 mbr1 = entries.get(i).getMBR();
                Rectangle2D1 mbr2 = entries.get(j).getMBR();
                
                // 计算组合MBR
                long minX = Math.min(mbr1.minX, mbr2.minX);
                long minY = Math.min(mbr1.minY, mbr2.minY);
                long maxX = Math.max(mbr1.maxX, mbr2.maxX);
                long maxY = Math.max(mbr1.maxY, mbr2.maxY);
                
                long combinedArea = (maxX - minX + 1) * (maxY - minY + 1);
                long waste = combinedArea - mbr1.area() - mbr2.area();
                
                if (waste > maxWaste) {
                    maxWaste = waste;
                    seed1 = i;
                    seed2 = j;
                }
            }
        }
        
        return new int[]{seed1, seed2};
    }
    
    /**
     * 计算添加条目到组后的面积增长
     */
    private long calculateAreaGrowth(List<RTreeEntry> group, RTreeEntry entry) {
        if (group.isEmpty()) {
            return entry.getMBR().area();
        }
        
        // 计算当前组的MBR
        Rectangle2D1 groupMBR = calculateGroupMBR(group);
        long originalArea = groupMBR.area();
        
        // 计算添加新条目后的MBR
        Rectangle2D1 entryMBR = entry.getMBR();
        long minX = Math.min(groupMBR.minX, entryMBR.minX);
        long minY = Math.min(groupMBR.minY, entryMBR.minY);
        long maxX = Math.max(groupMBR.maxX, entryMBR.maxX);
        long maxY = Math.max(groupMBR.maxY, entryMBR.maxY);
        
        long newArea = (maxX - minX + 1) * (maxY - minY + 1);
        return newArea - originalArea;
    }
    
    /**
     * 计算组的MBR
     */
    private Rectangle2D1 calculateGroupMBR(List<RTreeEntry> group) {
        if (group.isEmpty()) {
            return null;
        }
        
        long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE;
        
        for (RTreeEntry entry : group) {
            Rectangle2D1 rect = entry.getMBR();
            minX = Math.min(minX, rect.minX);
            minY = Math.min(minY, rect.minY);
            maxX = Math.max(maxX, rect.maxX);
            maxY = Math.max(maxY, rect.maxY);
        }
        
        return new Rectangle2D1(minX, minY, maxX, maxY);
    }
    
    /**
     * 计算组的总面积
     */
    private long calculateGroupArea(List<RTreeEntry> group) {
        Rectangle2D1 groupMBR = calculateGroupMBR(group);
        return groupMBR != null ? groupMBR.area() : 0;
    }
    
    // Getters and Setters
    public List<RTreeEntry> getEntries() {
        return entries;
    }
    
    public boolean isLeaf() {
        return isLeaf;
    }
    
    public Rectangle2D1 getMBR() {
        return mbr;
    }
    
    public RTreeNode getParent() {
        return parent;
    }
    
    public void setParent(RTreeNode parent) {
        this.parent = parent;
    }
    
    public static int getMaxEntries() {
        return MAX_ENTRIES;
    }
    
    public static int getMinEntries() {
        return MIN_ENTRIES;
    }
    
    @Override
    public String toString() {
        return String.format("RTreeNode[leaf=%b, entries=%d, mbr=%s]", 
                           isLeaf, entries.size(), mbr);
    }
}
