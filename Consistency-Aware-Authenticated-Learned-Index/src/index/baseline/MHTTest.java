package index.baseline;

import index.spatial_2d_pvl.*;
import utils.*;
import java.util.*;

/**
 * MHT vs PVL 性能对比测试
 * 
 * 对比指标:
 * 1. 构建时间
 * 2. 查询时间
 * 3. 验证时间
 * 4. VO大小
 * 5. 树高度
 * 6. 假阳性率
 */
public class MHTTest {
    
    public static void main(String[] args) {
        System.out.println("===== MHT vs PVL 性能对比测试 =====\n");
        
        // 测试配置
        String dataFile = "src/data/uniform_500k.csv";
        int loadCount = 500000;
        int errorBound = 128;  // PVL误差界限
        int mhtLeafSize = 256; // MHT叶子大小
        
        double[] queryRange = new double[]{0.0001, 0.001, 0.01};
        int queryLen = 300;
        
        System.out.println("测试配置:");
        System.out.println("  数据集: " + dataFile);
        System.out.println("  数据量: " + loadCount);
        System.out.println("  PVL误差界限: " + errorBound);
        System.out.println("  MHT叶子大小: " + mhtLeafSize);
        System.out.println("  查询选择性: " + Arrays.toString(queryRange));
        System.out.println("  查询次数/选择性: " + queryLen);
        System.out.println();
        
        // 1. 加载数据
        System.out.println("1. 加载数据集...");
        List<Point2D> points = DataLoader.loadFromCSV(dataFile, loadCount);
        System.out.println("   成功加载 " + points.size() + " 个数据点\n");
        
        // 计算数据范围
        long minX = Long.MAX_VALUE, maxX = Long.MIN_VALUE;
        long minY = Long.MAX_VALUE, maxY = Long.MIN_VALUE;
        for (Point2D p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
        }
        DataStats stats = new DataStats(minX, maxX, minY, maxY);
        System.out.println("数据范围:");
        System.out.println("  X: [" + stats.minX + ", " + stats.maxX + "] (跨度: " + stats.rangeX + ")");
        System.out.println("  Y: [" + stats.minY + ", " + stats.maxY + "] (跨度: " + stats.rangeY + ")");
        System.out.println();
        
        // 2. 构建MHT索引
        System.out.println("========================================");
        System.out.println("测试 1: MHT索引");
        System.out.println("========================================\n");
        
        Spatial2DMHT mhtIndex = new Spatial2DMHT(mhtLeafSize);
        mhtIndex.build(points);
        
        System.out.println(mhtIndex.getIndexStats());
        System.out.println();
        
        // 3. 构建PVL索引
        System.out.println("========================================");
        System.out.println("测试 2: PVL索引");
        System.out.println("========================================\n");
        
        long pvlBuildStart = System.nanoTime();
        Spatial2DPVLTree pvlTree = new Spatial2DPVLTree(points, errorBound);
        long pvlBuildTime = System.nanoTime() - pvlBuildStart;
        
        System.out.println("PVL索引构建完成");
        System.out.println("  构建时间: " + String.format("%.4f ms", pvlBuildTime / 1_000_000.0));
        System.out.println("  误差界限: " + errorBound);
        System.out.println();
        
        // 4. 性能对比测试
        System.out.println("========================================");
        System.out.println("性能对比测试");
        System.out.println("========================================\n");
        
        for (double selectivity : queryRange) {
            System.out.println("----------------------------------------");
            System.out.println("选择性: " + selectivity);
            System.out.println("----------------------------------------");
            
            // 生成查询矩形
            List<Rectangle2D> queries = generateQueries(stats, selectivity, queryLen);
            
            // 测试MHT
            PerformanceMetrics mhtMetrics = testMHT(mhtIndex, queries);
            
            // 测试PVL
            PerformanceMetrics pvlMetrics = testPVL(pvlTree, queries);
            
            // 打印对比结果
            printComparison(selectivity, mhtMetrics, pvlMetrics);
            System.out.println();
        }
        
        System.out.println("\n===== 测试完成 =====");
    }
    
    /**
     * 测试MHT性能
     */
    private static PerformanceMetrics testMHT(Spatial2DMHT mhtIndex, List<Rectangle2D> queries) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        for (Rectangle2D query : queries) {
            // 查询
            long queryStart = System.nanoTime();
            Spatial2DMHT.Spatial2DMHTResult result = mhtIndex.rectangleQuery(query);
            long queryTime = System.nanoTime() - queryStart;
            
            // 验证
            long verifyStart = System.nanoTime();
            mhtIndex.verify(result);  // 验证结果
            long verifyTime = System.nanoTime() - verifyStart;
            
            // 收集指标
            metrics.addQuery(
                queryTime,
                verifyTime,
                result.getTotalVOSize(),
                result.getZIntervalCount(),
                result.getResultCount(),
                result.getCandidateCount(),
                result.getFalsePositiveCount()
            );
        }
        
        return metrics;
    }
    
    /**
     * 测试PVL性能
     */
    private static PerformanceMetrics testPVL(Spatial2DPVLTree pvlTree, List<Rectangle2D> queries) {
        PerformanceMetrics metrics = new PerformanceMetrics();
        
        for (Rectangle2D query : queries) {
            // 查询
            long queryStart = System.nanoTime();
            Spatial2DPVL_Res result = pvlTree.rectangleQuery(query);
            long queryTime = System.nanoTime() - queryStart;
            
            // 验证
            long verifyStart = System.nanoTime();
            pvlTree.verify(query, result);  // 验证结果
            long verifyTime = System.nanoTime() - verifyStart;
            
            // 获取统计信息
            Spatial2DPVL_Res.QueryStats queryStats = result.getStats();
            
            // 收集指标
            metrics.addQuery(
                queryTime,
                verifyTime,
                (long)queryStats.voSize,
                queryStats.intervalCount,
                queryStats.resultCount,
                queryStats.candidateCount,
                queryStats.falsePositiveCount
            );
        }
        
        return metrics;
    }
    
    /**
     * 打印对比结果
     */
    private static void printComparison(double selectivity, 
                                       PerformanceMetrics mht, 
                                       PerformanceMetrics pvl) {
        System.out.println("\n【MHT索引】");
        System.out.println(mht.toString());
        
        System.out.println("\n【PVL索引】");
        System.out.println(pvl.toString());
        
        System.out.println("\n【性能对比】");
        System.out.printf("  查询时间: MHT=%.3f ms, PVL=%.3f ms, 提升=%.1f%%\n",
            mht.avgQueryTime, pvl.avgQueryTime,
            (mht.avgQueryTime - pvl.avgQueryTime) / mht.avgQueryTime * 100);
        
        System.out.printf("  验证时间: MHT=%.3f ms, PVL=%.3f ms, 提升=%.1f%%\n",
            mht.avgVerifyTime, pvl.avgVerifyTime,
            (mht.avgVerifyTime - pvl.avgVerifyTime) / mht.avgVerifyTime * 100);
        
        System.out.printf("  总时间:   MHT=%.3f ms, PVL=%.3f ms, 提升=%.1f%%\n",
            mht.avgTotalTime, pvl.avgTotalTime,
            (mht.avgTotalTime - pvl.avgTotalTime) / mht.avgTotalTime * 100);
        
        System.out.printf("  VO大小:   MHT=%.2f KB, PVL=%.2f KB, 减少=%.1f%%\n",
            mht.avgVOSize / 1024.0, pvl.avgVOSize / 1024.0,
            (mht.avgVOSize - pvl.avgVOSize) / mht.avgVOSize * 100);
        
        System.out.printf("  假阳性:   MHT=%d, PVL=%d\n",
            (int)mht.avgFalsePositives, (int)pvl.avgFalsePositives);
    }
    
    /**
     * 数据统计信息
     */
    static class DataStats {
        long minX, maxX, minY, maxY;
        long rangeX, rangeY;
        
        DataStats(long minX, long maxX, long minY, long maxY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.rangeX = maxX - minX;
            this.rangeY = maxY - minY;
        }
    }
    
    /**
     * 生成查询矩形
     */
    private static List<Rectangle2D> generateQueries(DataStats stats, 
                                                     double selectivity, 
                                                     int count) {
        List<Rectangle2D> queries = new ArrayList<>();
        Random rand = new Random(42);
        
        long totalArea = stats.rangeX * stats.rangeY;
        long queryArea = (long) (totalArea * selectivity);
        long sideLength = (long) Math.sqrt(queryArea);
        
        for (int i = 0; i < count; i++) {
            long x = stats.minX + (long) (rand.nextDouble() * (stats.rangeX - sideLength));
            long y = stats.minY + (long) (rand.nextDouble() * (stats.rangeY - sideLength));
            
            queries.add(new Rectangle2D(x, y, x + sideLength, y + sideLength));
        }
        
        return queries;
    }
    
    /**
     * 性能指标收集器
     */
    static class PerformanceMetrics {
        private long totalQueryTime = 0;
        private long totalVerifyTime = 0;
        private long totalVOSize = 0;
        private int totalZIntervals = 0;
        private int totalResults = 0;
        private int totalCandidates = 0;
        private int totalFalsePositives = 0;
        private int queryCount = 0;
        
        // 平均值
        double avgQueryTime;
        double avgVerifyTime;
        double avgTotalTime;
        double avgVOSize;
        double avgZIntervals;
        double avgResults;
        double avgCandidates;
        double avgFalsePositives;
        
        void addQuery(long queryTime, long verifyTime, long voSize, 
                     int zIntervals, int results, int candidates, int falsePositives) {
            totalQueryTime += queryTime;
            totalVerifyTime += verifyTime;
            totalVOSize += voSize;
            totalZIntervals += zIntervals;
            totalResults += results;
            totalCandidates += candidates;
            totalFalsePositives += falsePositives;
            queryCount++;
            
            // 计算平均值
            avgQueryTime = totalQueryTime / 1_000_000.0 / queryCount;
            avgVerifyTime = totalVerifyTime / 1_000_000.0 / queryCount;
            avgTotalTime = avgQueryTime + avgVerifyTime;
            avgVOSize = (double) totalVOSize / queryCount;
            avgZIntervals = (double) totalZIntervals / queryCount;
            avgResults = (double) totalResults / queryCount;
            avgCandidates = (double) totalCandidates / queryCount;
            avgFalsePositives = (double) totalFalsePositives / queryCount;
        }
        
        @Override
        public String toString() {
            return String.format(
                "  查询时间: %.3f ms\n" +
                "  验证时间: %.3f ms\n" +
                "  总时间:   %.3f ms\n" +
                "  VO大小:   %.2f KB\n" +
                "  Z区间数:  %.1f\n" +
                "  结果数:   %.1f\n" +
                "  候选数:   %.1f\n" +
                "  假阳性:   %.1f",
                avgQueryTime, avgVerifyTime, avgTotalTime,
                avgVOSize / 1024.0, avgZIntervals, avgResults,
                avgCandidates, avgFalsePositives
            );
        }
    }
}

