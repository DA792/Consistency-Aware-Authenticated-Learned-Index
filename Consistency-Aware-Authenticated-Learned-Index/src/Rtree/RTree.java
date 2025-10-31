package Rtree;

import java.util.ArrayList;
import java.util.List;

/**
 * R-tree主类
 * 实现二维空间索引，支持插入和范围查询
 */
public class RTree {
    private RTreeNode root;
    private int size;
    private int height;
    
    public RTree() {
        this.root = new RTreeNode(true); // 初始为叶子节点
        this.size = 0;
        this.height = 1;
    }
    
    /**
     * 插入点到R-tree
     */
    public void insert(Point2D1 point) {
        insert(point, null);
    }
    
    /**
     * 插入点和关联数据到R-tree
     */
    public void insert(Point2D1 point, Object data) {
        RTreeEntry entry = new RTreeEntry(point, data);
        insertEntry(entry);
        size++;
    }
    
    /**
     * 插入条目到R-tree
     */
    private void insertEntry(RTreeEntry entry) {
        // 选择叶子节点
        RTreeNode leaf = chooseLeaf(entry);
        
        // 添加条目到叶子节点
        leaf.addEntry(entry);
        
        // 处理可能的分裂，从叶子节点开始向上传播
        handleSplit(leaf);
        
        // 向上更新MBR
        updateMBRUpward(leaf);
    }
    
    /**
     * 处理节点分裂，向上传播
     */
    private void handleSplit(RTreeNode node) {
        if (!node.needsSplit()) {
            return;
        }
        
        RTreeNode newNode = node.split();
        
        // 如果分裂的是根节点，创建新根
        if (node == root) {
            RTreeNode newRoot = new RTreeNode(false);
            newRoot.addEntry(new RTreeEntry(node));
            newRoot.addEntry(new RTreeEntry(newNode));
            root = newRoot;
            height++;
            return;
        }
        
        // 非根节点分裂：将新节点插入到父节点
        RTreeNode parent = node.getParent();
        if (parent != null) {
            parent.addEntry(new RTreeEntry(newNode));
            // 递归处理父节点可能的分裂
            handleSplit(parent);
        }
    }
    
    /**
     * 选择插入叶子节点
     * 使用最小面积增长策略
     */
    private RTreeNode chooseLeaf(RTreeEntry entry) {
        RTreeNode current = root;
        
        while (!current.isLeaf()) {
            RTreeEntry bestEntry = null;
            long minAreaGrowth = Long.MAX_VALUE;
            long minArea = Long.MAX_VALUE;
            
            // 选择面积增长最小的条目
            for (RTreeEntry e : current.getEntries()) {
                long areaGrowth = entry.calculateAreaGrowth(e.getMBR());
                long area = e.getMBR().area();
                
                if (areaGrowth < minAreaGrowth || 
                    (areaGrowth == minAreaGrowth && area < minArea)) {
                    minAreaGrowth = areaGrowth;
                    minArea = area;
                    bestEntry = e;
                }
            }
            
            current = bestEntry.getChild();
        }
        
        return current;
    }
    
    /**
     * 向上更新MBR
     */
    private void updateMBRUpward(RTreeNode node) {
        RTreeNode current = node;
        
        while (current != null) {
            // 更新当前节点的MBR
            Rectangle2D1 oldMBR = current.getMBR();
            
            // 重新计算MBR（通过重新添加所有条目）
            List<RTreeEntry> entries = new ArrayList<>(current.getEntries());
            current.getEntries().clear();
            for (RTreeEntry entry : entries) {
                current.addEntry(entry);
            }
            
            // 如果MBR没有变化，停止向上传播
            Rectangle2D1 newMBR = current.getMBR();
            if (oldMBR != null && newMBR != null && 
                oldMBR.minX == newMBR.minX && oldMBR.minY == newMBR.minY &&
                oldMBR.maxX == newMBR.maxX && oldMBR.maxY == newMBR.maxY) {
                break;
            }
            
            // 更新父节点中对应的条目
            RTreeNode parent = current.getParent();
            if (parent != null) {
                for (RTreeEntry entry : parent.getEntries()) {
                    if (entry.getChild() == current) {
                        entry.updateMBR();
                        break;
                    }
                }
            }
            
            current = parent;
        }
    }
    
    /**
     * 范围查询
     */
    public RTreeQueryResult rangeQuery(Rectangle2D1 queryRect) {
        RTreeQueryResult result = new RTreeQueryResult();
        result.getStats().startQuery(queryRect);
        
        if (root != null) {
            rangeQueryRecursive(root, queryRect, result);
        }
        
        result.getStats().endQuery();
        return result;
    }
    
    /**
     * 递归范围查询
     */
    private void rangeQueryRecursive(RTreeNode node, Rectangle2D1 queryRect, RTreeQueryResult result) {
        result.getStats().visitNode();
        
        if (node.isLeaf()) {
            result.getStats().visitLeaf();
            
            // 检查叶子节点中的所有条目
            for (RTreeEntry entry : node.getEntries()) {
                result.getStats().checkEntry();
                
                if (entry.intersects(queryRect)) {
                    // 对于点数据，检查点是否在查询矩形内
                    if (entry.isLeafEntry() && queryRect.contains(entry.getDataPoint())) {
                        result.addResult(entry);
                    }
                }
            }
        } else {
            // 内部节点：递归检查相交的子节点
            for (RTreeEntry entry : node.getEntries()) {
                result.getStats().checkEntry();
                
                if (entry.intersects(queryRect)) {
                    rangeQueryRecursive(entry.getChild(), queryRect, result);
                }
            }
        }
    }
    
    /**
     * 点查询
     */
    public RTreeQueryResult pointQuery(Point2D1 point) {
        Rectangle2D1 pointRect = new Rectangle2D1(point.x, point.y, point.x, point.y);
        return rangeQuery(pointRect);
    }
    
    /**
     * 获取所有点
     */
    public List<Point2D1> getAllPoints() {
        List<Point2D1> allPoints = new ArrayList<>();
        if (root != null) {
            collectAllPoints(root, allPoints);
        }
        return allPoints;
    }
    
    /**
     * 递归收集所有点
     */
    private void collectAllPoints(RTreeNode node, List<Point2D1> points) {
        if (node.isLeaf()) {
            for (RTreeEntry entry : node.getEntries()) {
                if (entry.isLeafEntry()) {
                    points.add(entry.getDataPoint());
                }
            }
        } else {
            for (RTreeEntry entry : node.getEntries()) {
                if (entry.getChild() != null) {
                    collectAllPoints(entry.getChild(), points);
                }
            }
        }
    }
    
    /**
     * 获取树的统计信息
     */
    public TreeStats getStats() {
        TreeStats stats = new TreeStats();
        stats.size = this.size;
        stats.height = this.height;
        
        if (root != null) {
            calculateStats(root, stats, 0);
        }
        
        return stats;
    }
    
    /**
     * 递归计算统计信息
     */
    private void calculateStats(RTreeNode node, TreeStats stats, int level) {
        stats.totalNodes++;
        
        if (node.isLeaf()) {
            stats.leafNodes++;
            stats.totalEntries += node.getEntries().size();
            stats.maxEntriesPerLeaf = Math.max(stats.maxEntriesPerLeaf, node.getEntries().size());
            stats.minEntriesPerLeaf = Math.min(stats.minEntriesPerLeaf, node.getEntries().size());
        } else {
            stats.internalNodes++;
            for (RTreeEntry entry : node.getEntries()) {
                if (entry.getChild() != null) {
                    calculateStats(entry.getChild(), stats, level + 1);
                }
            }
        }
        
        stats.maxLevel = Math.max(stats.maxLevel, level);
    }
    
    /**
     * 检查树是否为空
     */
    public boolean isEmpty() {
        return size == 0;
    }
    
    /**
     * 清空树
     */
    public void clear() {
        root = new RTreeNode(true);
        size = 0;
        height = 1;
    }
    
    // Getters
    public int size() {
        return size;
    }
    
    public int getHeight() {
        return height;
    }
    
    public RTreeNode getRoot() {
        return root;
    }
    
    @Override
    public String toString() {
        return String.format("RTree[size=%d, height=%d, root=%s]", 
                           size, height, root);
    }
    
    /**
     * 树统计信息类
     */
    public static class TreeStats {
        public int size;
        public int height;
        public int totalNodes;
        public int leafNodes;
        public int internalNodes;
        public int totalEntries;
        public int maxEntriesPerLeaf;
        public int minEntriesPerLeaf;
        public int maxLevel;
        
        public TreeStats() {
            this.minEntriesPerLeaf = Integer.MAX_VALUE;
        }
        
        @Override
        public String toString() {
            return String.format(
                "TreeStats[size=%d, height=%d, nodes=%d(leaf=%d,internal=%d), " +
                "entries=%d, leaf_entries=[%d-%d], max_level=%d]",
                size, height, totalNodes, leafNodes, internalNodes,
                totalEntries, minEntriesPerLeaf == Integer.MAX_VALUE ? 0 : minEntriesPerLeaf, 
                maxEntriesPerLeaf, maxLevel
            );
        }
    }
}
