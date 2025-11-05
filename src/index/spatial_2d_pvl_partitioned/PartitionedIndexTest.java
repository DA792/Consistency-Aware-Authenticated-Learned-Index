package index.spatial_2d_pvl_partitioned;

import index.spatial_2d_pvl.Spatial2DPVLTree;
import index.spatial_2d_pvl.Spatial2DPVL_Res;
import utils.*;
import java.util.*;

/**
 * 分区索引性能测试
 * 对比全局索引 vs 分区索引的性能
 */
public class PartitionedIndexTest {
    
    public static void main(String[] args) {
        System.out.println("===== 全局索引 vs 分区索引性能对比 =====\n");
        
        // 1. 加载数据
        System.out.println("1. 加载数据集 (50万点)");
        String dataPath = "src/data/uniform_500k.csv";
        int loadCount = 500000;
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.out.println("数据文件未找到,使用随机生成数据");
            points = generateRandomData(100000);
            loadCount = 100000;
        }
        
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println("实际加载: " + points.size() + " 个数据点");
        System.out.println("X范围: [" + stats.minX + ", " + stats.maxX + "]");
        System.out.println("Y范围: [" + stats.minY + ", " + stats.maxY + "]\n");
        
        // 2. 测试配置
        int errorBound = 256;
        int partitionCount = 8;  // 50万数据,8个分区最优
        double[] queryRange = new double[]{0.0001, 0.001, 0.01, 0.1};
        int queryLen = 300;
        
        System.out.println("测试配置:");
        System.out.println("  误差界限: " + errorBound);
        System.out.println("  分区数: " + partitionCount);
        System.out.println("  查询选择性: " + Arrays.toString(queryRange));
        System.out.println("  查询次数/选择性: " + queryLen);
        System.out.println();
        
        // 3. 构建全局索引
        System.out.println("========================================");
        System.out.println("测试 1: 全局索引 (无分区)");
        System.out.println("========================================\n");
        
        long startTime = System.nanoTime();
        Spatial2DPVLTree globalTree = new Spatial2DPVLTree(points, errorBound);
        long buildTime = System.nanoTime() - startTime;
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms");
        globalTree.printIndexSize();
        System.out.println();
        
        testPerformance("全局索引", globalTree, stats, queryRange, queryLen);
        
        // 4. 构建分区索引
        System.out.println("\n========================================");
        System.out.println("测试 2: 分区索引 (" + partitionCount + "个分区)");
        System.out.println("========================================\n");
        
        startTime = System.nanoTime();
        Spatial2DPVLTreePartitioned partitionedTree = 
            new Spatial2DPVLTreePartitioned(points, errorBound, partitionCount);
        buildTime = System.nanoTime() - startTime;
        System.out.println("总构建时间: " + buildTime / 1000000.0 + " ms");
        partitionedTree.printIndexSize();
        System.out.println();
        
        testPartitionedPerformance("分区索引", partitionedTree, stats, queryRange, queryLen);
        
        System.out.println("\n===== 测试完成 =====");
    }
    
    /**
     * 测试全局索引性能
     */
    private static void testPerformance(String name, Spatial2DPVLTree tree,
                                       DataLoader.DataStats stats,
                                       double[] queryRange, int queryLen) {
        System.out.println("【" + name + " 性能测试】\n");
        
        for (double selectivity : queryRange) {
            List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, queryLen);
            
            long totalQueryTime = 0;
            long totalVerifyTime = 0;
            double totalVOSize = 0;
            int totalResults = 0;
            int totalIntervals = 0;
            int totalCandidates = 0;
            int totalFalsePositives = 0;
            
            for (Rectangle2D query : queries) {
                // 查询
                long startTime = System.nanoTime();
                Spatial2DPVL_Res response = tree.rectangleQuery(query);
                totalQueryTime += System.nanoTime() - startTime;
                
                // 验证
                startTime = System.nanoTime();
                boolean isValid = tree.verify(query, response);
                totalVerifyTime += System.nanoTime() - startTime;
                
                if (!isValid) {
                    System.out.println("警告: 验证失败!");
                }
                
                // 统计
                totalVOSize += response.getTotalVOSize();
                totalResults += response.results.size();
                
                Spatial2DPVL_Res.QueryStats queryStats = response.getStats();
                totalIntervals += queryStats.intervalCount;
                totalCandidates += queryStats.candidateCount;
                totalFalsePositives += queryStats.falsePositiveCount;
            }
            
            // 输出结果
            System.out.println("选择性 " + selectivity + ":");
            System.out.println(String.format("  查询时间: %.3f ms", totalQueryTime / queryLen / 1000000.0));
            System.out.println(String.format("  验证时间: %.3f ms", totalVerifyTime / queryLen / 1000000.0));
            System.out.println(String.format("  总时间:   %.3f ms", 
                                           (totalQueryTime + totalVerifyTime) / queryLen / 1000000.0));
            System.out.println(String.format("  VO大小:   %.2f KB", totalVOSize / queryLen / 1024.0));
            System.out.println("  Z区间数:  " + (totalIntervals / queryLen));
            System.out.println("  结果数:   " + (totalResults / queryLen));
            System.out.println("  候选数:   " + (totalCandidates / queryLen));
            System.out.println("  假阳性:   " + (totalFalsePositives / queryLen));
            System.out.println();
        }
    }
    
    /**
     * 测试分区索引性能
     */
    private static void testPartitionedPerformance(String name, Spatial2DPVLTreePartitioned tree,
                                                  DataLoader.DataStats stats,
                                                  double[] queryRange, int queryLen) {
        System.out.println("【" + name + " 性能测试】\n");
        
        for (double selectivity : queryRange) {
            List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, queryLen);
            
            long totalQueryTime = 0;
            long totalVerifyTime = 0;
            double totalVOSize = 0;
            int totalResults = 0;
            int totalIntervals = 0;
            int totalCandidates = 0;
            int totalFalsePositives = 0;
            
            for (Rectangle2D query : queries) {
                // 查询
                long startTime = System.nanoTime();
                Spatial2DPVL_Res response = tree.rectangleQuery(query);
                totalQueryTime += System.nanoTime() - startTime;
                
                // 验证
                startTime = System.nanoTime();
                boolean isValid = tree.verify(query, response);
                totalVerifyTime += System.nanoTime() - startTime;
                
                if (!isValid) {
                    System.out.println("警告: 验证失败!");
                }
                
                // 统计
                totalVOSize += response.getTotalVOSize();
                totalResults += response.results.size();
                
                Spatial2DPVL_Res.QueryStats queryStats = response.getStats();
                totalIntervals += queryStats.intervalCount;
                totalCandidates += queryStats.candidateCount;
                totalFalsePositives += queryStats.falsePositiveCount;
            }
            
            // 输出结果
            System.out.println("选择性 " + selectivity + ":");
            System.out.println(String.format("  查询时间: %.3f ms", totalQueryTime / queryLen / 1000000.0));
            System.out.println(String.format("  验证时间: %.3f ms", totalVerifyTime / queryLen / 1000000.0));
            System.out.println(String.format("  总时间:   %.3f ms", 
                                           (totalQueryTime + totalVerifyTime) / queryLen / 1000000.0));
            System.out.println(String.format("  VO大小:   %.2f KB", totalVOSize / queryLen / 1024.0));
            System.out.println("  Z区间数:  " + (totalIntervals / queryLen));
            System.out.println("  结果数:   " + (totalResults / queryLen));
            System.out.println("  候选数:   " + (totalCandidates / queryLen));
            System.out.println("  假阳性:   " + (totalFalsePositives / queryLen));
            System.out.println();
        }
    }
    
    private static List<Point2D> generateRandomData(int count) {
        List<Point2D> points = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < count; i++) {
            points.add(new Point2D(random.nextInt(20000), random.nextInt(20000)));
        }
        return points;
    }
}

