package index.spatial_2d;

import java.util.List;

/**
 * 二维查询的完整响应
 */
public class Spatial2DQueryResponse {
    public final List<Point2D> results;                           // 最终的二维查询结果
    public final List<Spatial2DQueryResult> intervalResults;      // 每个区间的详细查询结果
    
    public Spatial2DQueryResponse(List<Point2D> results, 
                                 List<Spatial2DQueryResult> intervalResults) {
        this.results = results;
        this.intervalResults = intervalResults;
    }
    
    /**
     * 计算VO的总大小（字节）
     */
    public double getTotalVOSize() {
        double totalSize = 0;
        for (Spatial2DQueryResult result : intervalResults) {
            totalSize += result.pvlResult.getVOSize();
        }
        return totalSize;
    }
    
    /**
     * 获取查询统计信息
     */
    public QueryStats getStats() {
        int totalCandidates = 0;
        int totalFalsePositives = 0;
        
        for (Spatial2DQueryResult result : intervalResults) {
            totalCandidates += result.getCandidateCount();
            totalFalsePositives += result.getFalsePositiveCount();
        }
        
        return new QueryStats(
            results.size(),
            totalCandidates,
            totalFalsePositives,
            intervalResults.size(),
            getTotalVOSize()
        );
    }
    
    /**
     * 查询统计信息
     */
    public static class QueryStats {
        public final int resultCount;
        public final int candidateCount;
        public final int falsePositiveCount;
        public final int intervalCount;
        public final double voSize;
        public final double falsePositiveRate;
        
        public QueryStats(int resultCount, int candidateCount, int falsePositiveCount,
                         int intervalCount, double voSize) {
            this.resultCount = resultCount;
            this.candidateCount = candidateCount;
            this.falsePositiveCount = falsePositiveCount;
            this.intervalCount = intervalCount;
            this.voSize = voSize;
            this.falsePositiveRate = candidateCount > 0 ? 
                (double) falsePositiveCount / candidateCount : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "QueryStats{结果数=%d, 候选数=%d, 假阳性=%d(%.2f%%), 区间数=%d, VO大小=%.2fKB}",
                resultCount, candidateCount, falsePositiveCount, 
                falsePositiveRate * 100, intervalCount, voSize / 1024.0
            );
        }
    }
}


