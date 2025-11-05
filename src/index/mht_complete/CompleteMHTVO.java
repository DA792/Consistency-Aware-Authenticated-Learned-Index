package index.mht_complete;

import utils.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整的Merkle Hash Tree 验证对象 (VO)
 * 
 * 包含完整重建Merkle路径所需的所有信息:
 * 1. 查询结果数据
 * 2. 每个结果数据点在树中的叶子索引
 * 3. 兄弟节点哈希（用于重建路径）
 * 4. 边界节点信息（用于完整性证明）
 */
public class CompleteMHTVO {
    
    // ==================== 查询结果 ====================
    
    private List<Point2D> results;
    private long queryStart;
    private long queryEnd;
    
    // ==================== Merkle路径信息 ====================
    
    /**
     * 叶子节点信息
     * 每个结果数据点对应一个叶子节点
     */
    private List<LeafInfo> leafInfos;
    
    /**
     * 兄弟节点哈希
     * 按照从叶子到根的顺序存储
     */
    private List<SiblingHash> siblingHashes;
    
    /**
     * 边界节点信息
     * 用于证明查询范围外没有其他数据
     */
    private List<BoundaryNode> boundaryNodes;
    
    /**
     * 根节点哈希（用于最终验证）
     */
    private byte[] rootHash;
    
    // ==================== 构造函数 ====================
    
    public CompleteMHTVO(long queryStart, long queryEnd, byte[] rootHash) {
        this.queryStart = queryStart;
        this.queryEnd = queryEnd;
        this.rootHash = rootHash;
        this.results = new ArrayList<>();
        this.leafInfos = new ArrayList<>();
        this.siblingHashes = new ArrayList<>();
        this.boundaryNodes = new ArrayList<>();
    }
    
    // ==================== 添加数据方法 ====================
    
    public void addResult(Point2D point, int leafIndex, int level, int position) {
        results.add(point);
        leafInfos.add(new LeafInfo(leafIndex, level, position, point));
    }
    
    public void addSiblingHash(byte[] hash, int level, int position, boolean isLeftSibling) {
        siblingHashes.add(new SiblingHash(hash, level, position, isLeftSibling));
    }
    
    public void addSiblingHash(byte[] hash, int level, int position, boolean isLeftSibling, long minKey, long maxKey) {
        siblingHashes.add(new SiblingHash(hash, level, position, isLeftSibling, minKey, maxKey));
    }
    
    public void addBoundaryNode(long minKey, long maxKey, byte[] hash) {
        boundaryNodes.add(new BoundaryNode(minKey, maxKey, hash));
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 叶子节点信息
     */
    public static class LeafInfo {
        public final int leafIndex;      // 叶子节点在所有叶子中的索引
        public final int level;          // 层级 (0=叶子)
        public final int position;       // 在当前层的位置
        public final Point2D data;       // 数据点
        
        public LeafInfo(int leafIndex, int level, int position, Point2D data) {
            this.leafIndex = leafIndex;
            this.level = level;
            this.position = position;
            this.data = data;
        }
        
        @Override
        public String toString() {
            return String.format("Leaf[idx=%d, level=%d, pos=%d]", leafIndex, level, position);
        }
    }
    
    /**
     * 兄弟节点哈希
     */
    public static class SiblingHash {
        public final byte[] hash;
        public final int level;          // 在哪一层
        public final int position;       // 在该层的位置
        public final boolean isLeftSibling;  // 是否是左兄弟
        public final long minKey;        // 节点的最小键
        public final long maxKey;        // 节点的最大键
        
        public SiblingHash(byte[] hash, int level, int position, boolean isLeftSibling) {
            this(hash, level, position, isLeftSibling, 0, Long.MAX_VALUE);
        }
        
        public SiblingHash(byte[] hash, int level, int position, boolean isLeftSibling, long minKey, long maxKey) {
            this.hash = hash;
            this.level = level;
            this.position = position;
            this.isLeftSibling = isLeftSibling;
            this.minKey = minKey;
            this.maxKey = maxKey;
        }
        
        public String getHashHex() {
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, Math.min(16, sb.length()));
        }
        
        @Override
        public String toString() {
            return String.format("Sibling[level=%d, pos=%d, %s, hash=%s]", 
                level, position, isLeftSibling ? "LEFT" : "RIGHT", getHashHex());
        }
    }
    
    /**
     * 边界节点
     */
    public static class BoundaryNode {
        public final long minKey;
        public final long maxKey;
        public final byte[] hash;
        
        public BoundaryNode(long minKey, long maxKey, byte[] hash) {
            this.minKey = minKey;
            this.maxKey = maxKey;
            this.hash = hash;
        }
        
        @Override
        public String toString() {
            return String.format("Boundary[%d-%d]", minKey, maxKey);
        }
    }
    
    // ==================== Getters ====================
    
    public List<Point2D> getResults() {
        return results;
    }
    
    public long getQueryStart() {
        return queryStart;
    }
    
    public long getQueryEnd() {
        return queryEnd;
    }
    
    public List<LeafInfo> getLeafInfos() {
        return leafInfos;
    }
    
    public List<SiblingHash> getSiblingHashes() {
        return siblingHashes;
    }
    
    public List<BoundaryNode> getBoundaryNodes() {
        return boundaryNodes;
    }
    
    public byte[] getRootHash() {
        return rootHash;
    }
    
    public int getResultCount() {
        return results.size();
    }
    
    // ==================== VO大小计算 ====================
    
    /**
     * 计算VO大小 (字节)
     */
    public long getVOSize() {
        long size = 0;
        
        // 查询范围: 2 × 8 bytes
        size += 16;
        
        // 结果数据: resultCount × 24 bytes (x, y, zValue)
        size += results.size() * 24L;
        
        // 叶子信息: leafCount × 16 bytes (index, level, position, padding)
        size += leafInfos.size() * 16L;
        
        // 兄弟哈希: siblingCount × (32 + 8) bytes (hash + metadata)
        size += siblingHashes.size() * 40L;
        
        // 边界节点: boundaryCount × (16 + 32) bytes (keys + hash)
        size += boundaryNodes.size() * 48L;
        
        // 根哈希: 32 bytes
        size += 32;
        
        return size;
    }
    
    // ==================== 统计信息 ====================
    
    @Override
    public String toString() {
        return String.format(
            "CompleteMHTVO[range=%d-%d, results=%d, leafInfos=%d, siblings=%d, boundaries=%d, size=%d bytes]",
            queryStart, queryEnd, results.size(), leafInfos.size(), 
            siblingHashes.size(), boundaryNodes.size(), getVOSize()
        );
    }
    
    /**
     * 详细统计信息
     */
    public String getDetailedStats() {
        return String.format(
            "VO详细信息:\n" +
            "  查询范围: [%d, %d]\n" +
            "  结果数: %d\n" +
            "  叶子信息数: %d\n" +
            "  兄弟哈希数: %d\n" +
            "  边界节点数: %d\n" +
            "  VO总大小: %.2f KB",
            queryStart, queryEnd, results.size(), leafInfos.size(),
            siblingHashes.size(), boundaryNodes.size(), getVOSize() / 1024.0
        );
    }
}

