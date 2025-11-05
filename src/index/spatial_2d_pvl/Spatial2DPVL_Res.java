package index.spatial_2d_pvl;

import utils.*;
import java.util.*;

/**
 * 二维PVL查询结果 - 客户端过滤架构
 * 
 * 架构说明:
 * - results: 候选点集合（含假阳性），由服务端返回
 * - 客户端需要自行过滤假阳性得到最终结果
 * - 验证基于候选点集合的完整性
 */
public class Spatial2DPVL_Res {
    public final List<Point2D> results;  // 候选点（含假阳性）
    public final List<Spatial2DPVLQueryResult> intervalResults;
    public final List<ZOrderDecomposition.ZInterval> zIntervals;  // 缓存Z区间,避免重复计算
    
    public Spatial2DPVL_Res(List<Point2D> results, 
                           List<Spatial2DPVLQueryResult> intervalResults) {
        this(results, intervalResults, null);
    }
    
    public Spatial2DPVL_Res(List<Point2D> results, 
                           List<Spatial2DPVLQueryResult> intervalResults,
                           List<ZOrderDecomposition.ZInterval> zIntervals) {
        this.results = results;
        this.intervalResults = intervalResults;
        this.zIntervals = zIntervals;
    }
    
    public double getTotalVOSize() {
        double totalSize = 0;
        for (Spatial2DPVLQueryResult result : intervalResults) {
            totalSize += result.pvlResult.getVOSize();
        }
        return totalSize;
    }
    
    public QueryStats getStats() {
        int totalCandidates = 0;
        
        for (Spatial2DPVLQueryResult result : intervalResults) {
            totalCandidates += result.getCandidateCount();
        }
        
        // 在客户端过滤架构下，服务端无法计算假阳性数量
        // 假阳性数量需要客户端过滤后才能确定
        return new QueryStats(
            results.size(),        // 候选点数量（含假阳性）
            totalCandidates,       // 总候选点数量
            0,                     // 假阳性数量（服务端无法计算）
            intervalResults.size(),
            getTotalVOSize()
        );
    }
    
    // 客户端过滤后的统计信息
    public QueryStats getStatsWithFiltering(int truePositiveCount) {
        int totalCandidates = results.size();
        int falsePositiveCount = totalCandidates - truePositiveCount;
        
        return new QueryStats(
            truePositiveCount,     // 真阳性数量（过滤后）
            totalCandidates,       // 候选点数量（含假阳性）
            falsePositiveCount,    // 假阳性数量
            intervalResults.size(),
            getTotalVOSize()
        );
    }
    
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


