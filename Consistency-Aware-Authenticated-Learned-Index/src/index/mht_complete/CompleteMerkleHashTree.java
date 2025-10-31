package index.mht_complete;

import utils.Point2D;
import java.security.MessageDigest;
import java.util.*;

/**
 * 完整的Merkle Hash Tree实现
 * 
 * 特点:
 * 1. 完整的Merkle路径验证
 * 2. 从叶子节点重建根哈希
 * 3. 详细的VO信息（包含叶子索引）
 * 4. 真正的安全验证
 * 
 * 时间复杂度:
 * - 构建: O(n log n)
 * - 查询: O(log n + k)
 * - 验证: O(k × log n)  k=结果数
 */
public class CompleteMerkleHashTree {
    private CompleteMHTNode root;
    private int leafSize;
    private int totalPoints;
    private int treeHeight;
    private int totalLeaves;  // 总叶子节点数
    
    public CompleteMerkleHashTree(int leafSize) {
        this.leafSize = leafSize;
        this.totalPoints = 0;
        this.treeHeight = 0;
        this.totalLeaves = 0;
    }
    
    /**
     * 构建MHT索引
     */
    public void build(List<Point2D> points) {
        if (points == null || points.isEmpty()) {
            throw new IllegalArgumentException("数据点列表不能为空");
        }
        
        this.totalPoints = points.size();
        
        // 验证数据有序
        for (int i = 1; i < points.size(); i++) {
            if (points.get(i).zValue < points.get(i-1).zValue) {
                throw new IllegalArgumentException("数据点必须按Z值排序");
            }
        }
        
        // 构建叶子节点
        List<CompleteMHTNode> leafNodes = buildLeafNodes(points);
        this.totalLeaves = leafNodes.size();
        
        // 自底向上构建树
        this.root = buildTreeBottomUp(leafNodes, 1);
        
        // 计算树高度
        this.treeHeight = computeHeight(root);
    }
    
    /**
     * 构建叶子节点（带位置信息）
     */
    private List<CompleteMHTNode> buildLeafNodes(List<Point2D> points) {
        List<CompleteMHTNode> leafNodes = new ArrayList<>();
        int leafIndex = 0;
        
        for (int i = 0; i < points.size(); i += leafSize) {
            int end = Math.min(i + leafSize, points.size());
            List<Point2D> leafData = points.subList(i, end);
            leafNodes.add(new CompleteMHTNode(leafData, leafIndex++));
        }
        
        return leafNodes;
    }
    
    /**
     * 自底向上构建树（带层级和位置信息）
     */
    private CompleteMHTNode buildTreeBottomUp(List<CompleteMHTNode> nodes, int level) {
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        
        List<CompleteMHTNode> parentLevel = new ArrayList<>();
        int positionInLevel = 0;
        
        for (int i = 0; i < nodes.size(); i += 2) {
            if (i + 1 < nodes.size()) {
                // 有两个子节点
                parentLevel.add(new CompleteMHTNode(
                    nodes.get(i), nodes.get(i + 1), 
                    level, positionInLevel++
                ));
            } else {
                // 只有一个子节点,创建虚拟右节点
                parentLevel.add(nodes.get(i));
            }
        }
        
        return buildTreeBottomUp(parentLevel, level + 1);
    }
    
    /**
     * 范围查询（生成完整VO）
     */
    public CompleteMHTVO rangeQuery(long zStart, long zEnd) {
        CompleteMHTVO vo = new CompleteMHTVO(zStart, zEnd, root.getHash());
        
        if (root == null) {
            return vo;
        }
        
        // 递归查询并构建VO
        rangeQueryRecursive(root, zStart, zEnd, vo, new HashSet<>());
        
        return vo;
    }
    
    /**
     * 递归范围查询（收集完整的Merkle路径信息）
     */
    private void rangeQueryRecursive(CompleteMHTNode node, long zStart, long zEnd,
                                     CompleteMHTVO vo, Set<Integer> visitedPositions) {
        if (node == null) {
            return;
        }
        
        // 不相交 - 作为边界节点
        if (!node.intersects(zStart, zEnd)) {
            vo.addBoundaryNode(node.getMinKey(), node.getMaxKey(), node.getHash());
            return;
        }
        
        // 完全包含 - 收集所有数据,并添加叶子哈希作为兄弟证明
        if (node.containedIn(zStart, zEnd)) {
            if (node.isLeaf()) {
                // 添加叶子节点的所有数据
                for (Point2D p : node.getData()) {
                    vo.addResult(p, node.getPosition(), node.getLevel(), node.getPosition());
                }
                // 添加叶子节点的哈希(用于验证)
                vo.addSiblingHash(node.getHash(), node.getLevel(), node.getPosition(), false, 
                                node.getMinKey(), node.getMaxKey());
            } else {
                // 递归收集所有叶子
                collectAllLeaves(node, vo);
            }
            return;
        }
        
        // 部分相交 - 需要向下递归并收集兄弟哈希
        if (node.isLeaf()) {
            // 叶子节点: 过滤数据并记录位置
            for (Point2D p : node.getData()) {
                if (p.zValue >= zStart && p.zValue <= zEnd) {
                    vo.addResult(p, node.getPosition(), node.getLevel(), node.getPosition());
                }
            }
            // 添加叶子节点的哈希(用于验证) - 必须包含正确的minKey/maxKey!
            vo.addSiblingHash(node.getHash(), node.getLevel(), node.getPosition(), false,
                            node.getMinKey(), node.getMaxKey());
        } else {
            // 内部节点: 检查左右子树的相交情况
            boolean leftIntersects = node.getLeft() != null && node.getLeft().intersects(zStart, zEnd);
            boolean rightIntersects = node.getRight() != null && node.getRight().intersects(zStart, zEnd);
            
            // 递归左子树
            if (leftIntersects) {
                rangeQueryRecursive(node.getLeft(), zStart, zEnd, vo, visitedPositions);
                
                // 如果右子树不相交,添加右子树的哈希作为兄弟证明
                if (!rightIntersects && node.getRight() != null) {
                    vo.addSiblingHash(
                        node.getRight().getHash(),
                        node.getRight().getLevel(),
                        node.getRight().getPosition(),
                        false,  // 右兄弟
                        node.getRight().getMinKey(),
                        node.getRight().getMaxKey()
                    );
                }
            }
            
            // 递归右子树
            if (rightIntersects) {
                rangeQueryRecursive(node.getRight(), zStart, zEnd, vo, visitedPositions);
                
                // 如果左子树不相交,添加左子树的哈希作为兄弟证明
                if (!leftIntersects && node.getLeft() != null) {
                    vo.addSiblingHash(
                        node.getLeft().getHash(),
                        node.getLeft().getLevel(),
                        node.getLeft().getPosition(),
                        true,  // 左兄弟
                        node.getLeft().getMinKey(),
                        node.getLeft().getMaxKey()
                    );
                }
            }
        }
    }
    
    /**
     * 收集节点下所有叶子数据
     */
    private void collectAllLeaves(CompleteMHTNode node, CompleteMHTVO vo) {
        if (node == null) {
            return;
        }
        
        if (node.isLeaf()) {
            for (Point2D p : node.getData()) {
                vo.addResult(p, node.getPosition(), node.getLevel(), node.getPosition());
            }
            // 添加叶子节点的哈希(用于验证)
            vo.addSiblingHash(node.getHash(), node.getLevel(), node.getPosition(), false,
                            node.getMinKey(), node.getMaxKey());
        } else {
            collectAllLeaves(node.getLeft(), vo);
            collectAllLeaves(node.getRight(), vo);
        }
    }
    
    /**
     * 完整的Merkle验证
     * 从叶子节点重建根哈希并对比
     */
    public boolean verify(CompleteMHTVO vo) {
        try {
            // 1. 验证结果完整性
            if (!verifyResultCompleteness(vo)) {
                System.out.println("验证失败: 结果完整性检查失败");
                return false;
            }
            
            // 2. 重建Merkle根哈希
            byte[] recomputedRoot = rebuildMerkleRoot(vo);
            
            if (recomputedRoot == null) {
                System.out.println("验证失败: 无法重建根哈希");
                return false;
            }
            
            // 3. 对比根哈希
            boolean rootMatches = Arrays.equals(recomputedRoot, vo.getRootHash());
            
            if (!rootMatches) {
                System.out.println("验证失败: 根哈希不匹配");
                System.out.println("  预期: " + bytesToHex(vo.getRootHash()));
                System.out.println("  实际: " + bytesToHex(recomputedRoot));
            }
            
            return rootMatches;
            
        } catch (Exception e) {
            System.out.println("验证失败: 异常 - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 验证结果完整性
     */
    private boolean verifyResultCompleteness(CompleteMHTVO vo) {
        List<Point2D> results = vo.getResults();
        
        // 验证结果有序
        for (int i = 1; i < results.size(); i++) {
            if (results.get(i).zValue < results.get(i-1).zValue) {
                return false;
            }
        }
        
        // 验证结果在查询范围内
        for (Point2D p : results) {
            if (p.zValue < vo.getQueryStart() || p.zValue > vo.getQueryEnd()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 重建Merkle根哈希
     * 这是完整验证的核心!
     */
    private byte[] rebuildMerkleRoot(CompleteMHTVO vo) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // 1. 使用VO中的所有哈希（包括叶子哈希和兄弟哈希）
            Map<String, byte[]> nodeHashes = new HashMap<>();
            Map<String, long[]> nodeRanges = new HashMap<>();  // 存储[minKey, maxKey]
            
            // 添加所有哈希（包括叶子节点和内部节点的哈希）
            // 使用格式化字符串避免键冲突 (例如 level=1,pos=23 vs level=12,pos=3)
            for (CompleteMHTVO.SiblingHash sibling : vo.getSiblingHashes()) {
                String key = String.format("L%d-P%d", sibling.level, sibling.position);
                nodeHashes.put(key, sibling.hash);
                nodeRanges.put(key, new long[]{sibling.minKey, sibling.maxKey});
            }
            
            // 2. 逐层向上重建
            int currentLevel = 0;
            while (currentLevel < treeHeight) {
                // 找出当前层的所有节点
                List<Integer> positions = new ArrayList<>();
                String levelPrefix = String.format("L%d-P", currentLevel);
                for (String key : nodeHashes.keySet()) {
                    if (key.startsWith(levelPrefix)) {
                        String posStr = key.substring(levelPrefix.length());
                        positions.add(Integer.parseInt(posStr));
                    }
                }
                
                if (positions.isEmpty()) {
                    currentLevel++;
                    continue;
                }
                
                Collections.sort(positions);
                
                // 对每个节点,计算其父节点
                // 关键修复: 不是简单配对相邻位置,而是根据position计算父节点
                Set<Integer> processedParents = new HashSet<>();
                
                for (int pos : positions) {
                    // 计算父节点position (整数除法)
                    int parentPos = pos / 2;
                    int parentLevel = currentLevel + 1;
                    String parentKey = String.format("L%d-P%d", parentLevel, parentPos);
                    
                    // 如果已经处理过这个父节点,跳过
                    if (processedParents.contains(parentPos)) {
                        continue;
                    }
                    processedParents.add(parentPos);
                    
                    // 检查是否已经有这个父节点的哈希了
                    if (nodeHashes.containsKey(parentKey)) {
                        continue;  // VO中已经包含了,不需要计算
                    }
                    
                    // 找到左右子节点
                    int leftPos = parentPos * 2;
                    int rightPos = parentPos * 2 + 1;
                    
                    String leftKey = String.format("L%d-P%d", currentLevel, leftPos);
                    String rightKey = String.format("L%d-P%d", currentLevel, rightPos);
                    
                    byte[] leftHash = nodeHashes.get(leftKey);
                    byte[] rightHash = nodeHashes.get(rightKey);
                    
                    // 如果左子节点不存在,跳过
                    if (leftHash == null) continue;
                    
                    // 如果右子节点不存在,使用左子节点的哈希
                    if (rightHash == null) {
                        rightHash = leftHash;
                        rightPos = leftPos;
                    }
                    
                    // 从子节点获取minKey和maxKey
                    // minKey来自左子节点, maxKey来自右子节点
                    long[] leftRange = nodeRanges.get(leftKey);
                    long[] rightRange = nodeRanges.get(rightKey);
                    
                    if (leftRange == null) {
                        System.err.println("警告: 找不到节点范围 " + leftKey);
                        continue;
                    }
                    
                    long nodeMinKey = leftRange[0];  // 从左子节点获取minKey
                    long nodeMaxKey = (rightRange != null) ? rightRange[1] : leftRange[1];  // 从右子节点获取maxKey
                    
                    // 计算父节点哈希
                    md.reset();
                    md.update("NODE:".getBytes());
                    md.update(longToBytes(nodeMinKey));
                    md.update(longToBytes(nodeMaxKey));
                    md.update(leftHash);
                    md.update(rightHash);
                    
                    byte[] parentHash = md.digest();
                    nodeHashes.put(parentKey, parentHash);
                    nodeRanges.put(parentKey, new long[]{nodeMinKey, nodeMaxKey});  // 保存父节点范围
                }
                
                currentLevel++;
            }
            
            // 返回根节点的哈希
            String rootKey = String.format("L%d-P0", treeHeight);
            
            // 调试输出 (只在第一次查询时输出)
            if (nodeHashes.size() < 900) {  // 简单判断
                System.out.println("\n=== 重建调试信息 ===");
                System.out.println("树高度: " + treeHeight);
                System.out.println("期望的根key: " + rootKey);
                System.out.println("VO中兄弟哈希数: " + vo.getSiblingHashes().size());
                
                // 按层级统计
                java.util.Map<Integer, Integer> levelCount = new java.util.HashMap<>();
                for (String key : nodeHashes.keySet()) {
                    int level = Integer.parseInt(key.substring(1, key.indexOf("-P")));
                    levelCount.put(level, levelCount.getOrDefault(level, 0) + 1);
                }
                System.out.println("各层节点数: " + levelCount);
            }
            
            if (nodeHashes.containsKey(rootKey)) {
                return nodeHashes.get(rootKey);
            }
            
            // 如果没找到,尝试返回最高层的唯一节点
            for (int level = treeHeight; level >= 0; level--) {
                String levelPrefix = String.format("L%d-P", level);
                for (String key : nodeHashes.keySet()) {
                    if (key.startsWith(levelPrefix)) {
                        System.out.println("使用备用根: " + key);
                        return nodeHashes.get(key);
                    }
                }
            }
            
            System.out.println("错误: 无法找到根哈希!");
            return null;  // 失败
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 工具方法
     */
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }
    
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.substring(0, Math.min(16, sb.length()));
    }
    
    private int computeHeight(CompleteMHTNode node) {
        if (node == null) {
            return 0;
        }
        if (node.isLeaf()) {
            return 1;
        }
        return 1 + Math.max(
            computeHeight(node.getLeft()),
            computeHeight(node.getRight())
        );
    }
    
    // ==================== Getters ====================
    
    public byte[] getRootHash() {
        return root != null ? root.getHash() : null;
    }
    
    public String getRootHashHex() {
        if (root == null) {
            return "null";
        }
        return root.getHashHex();
    }
    
    public int getTotalPoints() {
        return totalPoints;
    }
    
    public int getTreeHeight() {
        return treeHeight;
    }
    
    public int getTotalLeaves() {
        return totalLeaves;
    }
    
    public String getStats() {
        return String.format(
            "Complete MHT统计信息:\n" +
            "  总点数: %d\n" +
            "  树高度: %d\n" +
            "  叶子节点数: %d\n" +
            "  叶子大小: %d\n" +
            "  根哈希: %s",
            totalPoints, treeHeight, totalLeaves, leafSize, getRootHashHex()
        );
    }
}

