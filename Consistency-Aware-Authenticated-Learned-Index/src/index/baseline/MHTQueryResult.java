package index.baseline;

import utils.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * MHT查询结果
 * 
 * 包含:
 * 1. 查询结果数据
 * 2. 验证对象 (VO) - 用于验证结果完整性
 */
public class MHTQueryResult {
    // 查询结果
    private List<Point2D> results;
    
    // 验证对象 (VO)
    private VerificationObject vo;
    
    // 查询范围
    private long queryStart;
    private long queryEnd;
    
    public MHTQueryResult(long queryStart, long queryEnd) {
        this.queryStart = queryStart;
        this.queryEnd = queryEnd;
        this.results = new ArrayList<>();
        this.vo = new VerificationObject();
    }
    
    public void addResult(Point2D point) {
        results.add(point);
    }
    
    public void addResults(List<Point2D> points) {
        results.addAll(points);
    }
    
    public List<Point2D> getResults() {
        return results;
    }
    
    public VerificationObject getVO() {
        return vo;
    }
    
    public long getQueryStart() {
        return queryStart;
    }
    
    public long getQueryEnd() {
        return queryEnd;
    }
    
    public int getResultCount() {
        return results.size();
    }
    
    /**
     * 计算VO大小 (字节)
     */
    public long getVOSize() {
        return vo.getSize();
    }
    
    /**
     * 验证对象 (VO)
     * 
     * 包含验证所需的所有信息:
     * 1. 边界节点哈希
     * 2. 兄弟节点哈希
     * 3. 路径上的节点信息
     */
    public static class VerificationObject {
        // 验证路径上的节点哈希
        private List<byte[]> pathHashes;
        
        // 边界节点信息 (左右边界)
        private List<BoundaryNode> boundaryNodes;
        
        // 兄弟节点哈希 (用于重建父节点)
        private List<byte[]> siblingHashes;
        
        public VerificationObject() {
            this.pathHashes = new ArrayList<>();
            this.boundaryNodes = new ArrayList<>();
            this.siblingHashes = new ArrayList<>();
        }
        
        public void addPathHash(byte[] hash) {
            pathHashes.add(hash);
        }
        
        public void addBoundaryNode(long minKey, long maxKey, byte[] hash) {
            boundaryNodes.add(new BoundaryNode(minKey, maxKey, hash));
        }
        
        public void addSiblingHash(byte[] hash) {
            siblingHashes.add(hash);
        }
        
        public List<byte[]> getPathHashes() {
            return pathHashes;
        }
        
        public List<BoundaryNode> getBoundaryNodes() {
            return boundaryNodes;
        }
        
        public List<byte[]> getSiblingHashes() {
            return siblingHashes;
        }
        
        /**
         * 计算VO大小 (字节)
         */
        public long getSize() {
            long size = 0;
            
            // 路径哈希: 每个32字节 (SHA-256)
            size += pathHashes.size() * 32L;
            
            // 边界节点: minKey(8) + maxKey(8) + hash(32) = 48字节
            size += boundaryNodes.size() * 48L;
            
            // 兄弟哈希: 每个32字节
            size += siblingHashes.size() * 32L;
            
            return size;
        }
        
        public int getPathHashCount() {
            return pathHashes.size();
        }
        
        public int getBoundaryNodeCount() {
            return boundaryNodes.size();
        }
        
        public int getSiblingHashCount() {
            return siblingHashes.size();
        }
        
        @Override
        public String toString() {
            return String.format("VO[paths=%d, boundaries=%d, siblings=%d, size=%d bytes]",
                pathHashes.size(), boundaryNodes.size(), siblingHashes.size(), getSize());
        }
    }
    
    /**
     * 边界节点信息
     * 用于验证查询范围的边界
     */
    public static class BoundaryNode {
        private long minKey;
        private long maxKey;
        private byte[] hash;
        
        public BoundaryNode(long minKey, long maxKey, byte[] hash) {
            this.minKey = minKey;
            this.maxKey = maxKey;
            this.hash = hash;
        }
        
        public long getMinKey() {
            return minKey;
        }
        
        public long getMaxKey() {
            return maxKey;
        }
        
        public byte[] getHash() {
            return hash;
        }
        
        @Override
        public String toString() {
            return String.format("Boundary[%d-%d]", minKey, maxKey);
        }
    }
    
    @Override
    public String toString() {
        return String.format("MHTQueryResult[range=%d-%d, results=%d, vo=%s]",
            queryStart, queryEnd, results.size(), vo);
    }
}

