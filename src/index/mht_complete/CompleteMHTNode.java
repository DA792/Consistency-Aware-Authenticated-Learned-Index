package index.mht_complete;

import utils.Point2D;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * 完整的Merkle Hash Tree节点
 * 
 * 支持完整的Merkle路径验证
 */
public class CompleteMHTNode {
    // 键值范围
    private long minKey;
    private long maxKey;
    
    // 节点哈希
    private byte[] hash;
    
    // 子节点
    private CompleteMHTNode left;
    private CompleteMHTNode right;
    
    // 叶子节点数据
    private List<Point2D> data;
    private boolean isLeaf;
    
    // 节点在树中的位置信息
    private int level;        // 层级 (0=叶子)
    private int position;     // 在当前层的位置
    
    /**
     * 构造叶子节点
     */
    public CompleteMHTNode(List<Point2D> points, int position) {
        this.isLeaf = true;
        this.data = new ArrayList<>(points);
        this.level = 0;
        this.position = position;
        
        if (!points.isEmpty()) {
            this.minKey = points.get(0).zValue;
            this.maxKey = points.get(points.size() - 1).zValue;
        }
        
        // 计算叶子节点哈希
        this.hash = computeLeafHash();
    }
    
    /**
     * 构造内部节点
     */
    public CompleteMHTNode(CompleteMHTNode left, CompleteMHTNode right, int level, int position) {
        this.isLeaf = false;
        this.left = left;
        this.right = right;
        this.data = null;
        this.level = level;
        this.position = position;
        
        // 更新键值范围
        this.minKey = left.minKey;
        this.maxKey = right.maxKey;
        
        // 计算内部节点哈希
        this.hash = computeInternalHash();
    }
    
    /**
     * 计算叶子节点哈希
     * Hash("LEAF:" || minKey || maxKey || data)
     */
    private byte[] computeLeafHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // 添加前缀
            md.update("LEAF:".getBytes());
            
            // 添加键值范围
            md.update(longToBytes(minKey));
            md.update(longToBytes(maxKey));
            
            // 添加数据点（按Z值顺序）
            for (Point2D p : data) {
                md.update(longToBytes(p.x));
                md.update(longToBytes(p.y));
                md.update(longToBytes(p.zValue));
            }
            
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("哈希计算失败", e);
        }
    }
    
    /**
     * 计算内部节点哈希
     * Hash("NODE:" || minKey || maxKey || leftHash || rightHash)
     */
    private byte[] computeInternalHash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            
            // 添加前缀
            md.update("NODE:".getBytes());
            
            // 添加键值范围
            md.update(longToBytes(minKey));
            md.update(longToBytes(maxKey));
            
            // 添加子节点哈希
            md.update(left.hash);
            md.update(right.hash);
            
            return md.digest();
        } catch (Exception e) {
            throw new RuntimeException("哈希计算失败", e);
        }
    }
    
    /**
     * 重新计算哈希 (用于验证)
     */
    public void recomputeHash() {
        if (isLeaf) {
            this.hash = computeLeafHash();
        } else {
            this.hash = computeInternalHash();
        }
    }
    
    /**
     * 工具方法: long转字节数组
     */
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }
    
    // ==================== Getters ====================
    
    public long getMinKey() {
        return minKey;
    }
    
    public long getMaxKey() {
        return maxKey;
    }
    
    public byte[] getHash() {
        return hash;
    }
    
    public CompleteMHTNode getLeft() {
        return left;
    }
    
    public CompleteMHTNode getRight() {
        return right;
    }
    
    public List<Point2D> getData() {
        return data;
    }
    
    public boolean isLeaf() {
        return isLeaf;
    }
    
    public int getLevel() {
        return level;
    }
    
    public int getPosition() {
        return position;
    }
    
    /**
     * 判断键值范围是否与查询范围相交
     */
    public boolean intersects(long queryStart, long queryEnd) {
        return !(maxKey < queryStart || minKey > queryEnd);
    }
    
    /**
     * 判断键值范围是否完全包含在查询范围内
     */
    public boolean containedIn(long queryStart, long queryEnd) {
        return minKey >= queryStart && maxKey <= queryEnd;
    }
    
    /**
     * 获取哈希的十六进制字符串 (用于调试)
     */
    public String getHashHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.substring(0, Math.min(16, sb.length()));
    }
    
    @Override
    public String toString() {
        if (isLeaf) {
            return String.format("LeafNode[level=%d, pos=%d, range=%d-%d, %d points, hash=%s]", 
                level, position, minKey, maxKey, data.size(), getHashHex());
        } else {
            return String.format("InternalNode[level=%d, pos=%d, range=%d-%d, hash=%s]", 
                level, position, minKey, maxKey, getHashHex());
        }
    }
}

