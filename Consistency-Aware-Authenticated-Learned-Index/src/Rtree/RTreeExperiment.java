package Rtree;

import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.io.*;

/**
 * R-tree针对uniform_500k.csv数据集的专门实验程序
 * 测试50万数据的插入和查询性能
 * 使用生成的测试查询集进行性能测试
 */
public class RTreeExperiment {
    
    private static final String DATA_FILE = "Consistency-Aware-Authenticated-Learned-Index/src/data/uniform_500k.csv";
    private static final int WARMUP_QUERIES = 100;
    private static final int TEST_QUERIES = 1000;
    
    public static void main(String[] args) {
        System.out.println("=== R-tree uniform_500k.csv 数据集实验 ===\n");
        
        // 加载数据集
        System.out.println("1. 加载数据集");
        System.out.println("=============");
        List<Point2D1> points = loadDataset();
        if (points.isEmpty()) {
            System.out.println("数据集加载失败，退出实验");
            return;
        }
        
        // 分析数据集
        analyzeDataset(points);
        
        // 构建R-tree
        System.out.println("\n2. 构建R-tree");
        System.out.println("==============");
        RTree rtree = buildRTree(points);
        
        // 分析树结构
        analyzeTreeStructure(rtree);
        
        // 性能测试
        System.out.println("\n3. 性能测试");
        System.out.println("============");
        performanceTest(rtree, points);
        
        // 不同选择率的查询测试
        System.out.println("\n4. 不同选择率查询测试");
        System.out.println("===================");
        selectivityTest(rtree, points);
        
        // 内存使用分析
        System.out.println("\n5. 内存使用分析");
        System.out.println("===============");
        memoryAnalysis(rtree);
    }
    
    /**
     * 加载数据集
     */
    private static List<Point2D1> loadDataset() {
        long startTime = System.nanoTime();
        List<Point2D1> points = loadFromCSV(DATA_FILE);
        long endTime = System.nanoTime();
        
        double loadTime = (endTime - startTime) / 1_000_000.0;
        System.out.printf("数据加载完成: %d 个点 (%.2f ms)\n", points.size(), loadTime);
        
        return points;
    }
    
    /**
     * 简化的CSV加载功能
     */
    private static List<Point2D1> loadFromCSV(String filePath) {
        List<Point2D1> points = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 跳过CSV头 (x,y)
                if (firstLine && line.toLowerCase().contains("x")) {
                    firstLine = false;
                    continue;
                }
                firstLine = false;
                
                // 支持逗号、空格、制表符分隔
                String[] parts = line.trim().split("[,\\s\\t]+");
                if (parts.length >= 2) {
                    try {
                        long x = Long.parseLong(parts[0]);
                        long y = Long.parseLong(parts[1]);
                        points.add(new Point2D1(x, y));
                    } catch (NumberFormatException e) {
                        // 跳过无法解析的行
                    }
                }
            }
            
            System.out.println("成功加载 " + points.size() + " 个数据点");
            
        } catch (IOException e) {
            System.err.println("读取文件失败: " + filePath);
            System.err.println("错误: " + e.getMessage());
        }
        
        return points;
    }
    
    /**
     * 分析数据集特征
     */
    private static void analyzeDataset(List<Point2D1> points) {
        DataStats stats = analyzeData(points);
        System.out.println(stats);
        
        // 计算数据分布
        long[] xBuckets = new long[10];
        long[] yBuckets = new long[10];
        
        for (Point2D1 point : points) {
            int xBucket = (int) ((point.x - stats.minX) * 9 / stats.rangeX);
            int yBucket = (int) ((point.y - stats.minY) * 9 / stats.rangeY);
            xBuckets[Math.min(9, Math.max(0, xBucket))]++;
            yBuckets[Math.min(9, Math.max(0, yBucket))]++;
        }
        
        System.out.println("\n数据分布分析:");
        System.out.print("X轴分布: ");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%d ", xBuckets[i]);
        }
        System.out.print("\nY轴分布: ");
        for (int i = 0; i < 10; i++) {
            System.out.printf("%d ", yBuckets[i]);
        }
        System.out.println();
        
        // 计算数据密度
        double density = (double) points.size() / (stats.rangeX * stats.rangeY);
        System.out.printf("数据密度: %.6f 点/单位面积\n", density);
        
        // 检查数据聚集程度
        long maxBucketX = 0, maxBucketY = 0;
        for (int i = 0; i < 10; i++) {
            maxBucketX = Math.max(maxBucketX, xBuckets[i]);
            maxBucketY = Math.max(maxBucketY, yBuckets[i]);
        }
        double xSkewness = (double) maxBucketX / (points.size() / 10.0);
        double ySkewness = (double) maxBucketY / (points.size() / 10.0);
        System.out.printf("数据倾斜度 - X轴: %.2f, Y轴: %.2f (1.0=均匀, >2.0=高度聚集)\n", 
                        xSkewness, ySkewness);
    }
    
    /**
     * 构建R-tree
     */
    private static RTree buildRTree(List<Point2D1> points) {
        RTree rtree = new RTree();
        
        long startTime = System.nanoTime();
        
        // 批量插入，每10000个点显示进度
        for (int i = 0; i < points.size(); i++) {
            rtree.insert(points.get(i), i);
            
            if ((i + 1) % 10000 == 0) {
                double progress = (i + 1) * 100.0 / points.size();
                System.out.printf("插入进度: %.1f%% (%d/%d)\n", 
                                progress, i + 1, points.size());
            }
        }
        
        long endTime = System.nanoTime();
        double buildTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("R-tree构建完成: %.2f ms (%.3f μs/点)\n", 
                        buildTime, buildTime * 1000 / points.size());
        
        return rtree;
    }
    
    /**
     * 分析树结构
     */
    private static void analyzeTreeStructure(RTree rtree) {
        RTree.TreeStats stats = rtree.getStats();
        System.out.println("树结构统计:");
        System.out.println(stats);
        
        // 计算平均填充率
        if (stats.leafNodes > 0) {
            double avgFillRate = (double) stats.totalEntries / stats.leafNodes / RTreeNode.getMaxEntries();
            System.out.printf("平均叶子节点填充率: %.2f%%\n", avgFillRate * 100);
        }
        
        // 估算内存使用
        long estimatedMemory = estimateMemoryUsage(stats);
        System.out.printf("估算内存使用: %.2f MB\n", estimatedMemory / (1024.0 * 1024.0));
    }
    
    /**
     * 性能测试
     */
    private static void performanceTest(RTree rtree, List<Point2D1> points) {
        // 使用生成的查询集 (1%选择率)
        DataStats dataStats = analyzeData(points);
        double selectivity = 0.01;
        List<Rectangle2D1> queries = generateQueries(dataStats, selectivity, TEST_QUERIES + WARMUP_QUERIES);
        
        System.out.printf("生成 %d 个查询矩形 (选择率: %.1f%%)\n", queries.size(), selectivity * 100);
        
        // 预热查询
        System.out.println("执行预热查询...");
        for (int i = 0; i < WARMUP_QUERIES; i++) {
            rtree.rangeQuery(queries.get(i));
        }
        
        // 正式测试
        System.out.println("执行正式查询测试...");
        long totalQueryTime = 0;
        long totalResults = 0;
        long totalNodesVisited = 0;
        long totalLeavesVisited = 0;
        
        for (int i = WARMUP_QUERIES; i < queries.size(); i++) {
            RTreeQueryResult result = rtree.rangeQuery(queries.get(i));
            
            totalQueryTime += result.getStats().getQueryTimeNanos();
            totalResults += result.getResultCount();
            totalNodesVisited += result.getStats().getNodesVisited();
            totalLeavesVisited += result.getStats().getLeavesVisited();
        }
        
        // 输出结果
        double avgQueryTime = totalQueryTime / (double) TEST_QUERIES / 1_000_000.0;
        double avgResults = totalResults / (double) TEST_QUERIES;
        double avgNodesVisited = totalNodesVisited / (double) TEST_QUERIES;
        double avgLeavesVisited = totalLeavesVisited / (double) TEST_QUERIES;
        
        System.out.printf("查询性能统计 (%d 次查询):\n", TEST_QUERIES);
        System.out.printf("  平均查询时间: %.3f ms\n", avgQueryTime);
        System.out.printf("  平均结果数量: %.1f 个点\n", avgResults);
        System.out.printf("  平均访问节点: %.1f 个\n", avgNodesVisited);
        System.out.printf("  平均访问叶子: %.1f 个\n", avgLeavesVisited);
        
        // 计算查询效率
        RTree.TreeStats treeStats = rtree.getStats();
        double nodeVisitRatio = avgNodesVisited / treeStats.totalNodes;
        double leafVisitRatio = avgLeavesVisited / treeStats.leafNodes;
        
        System.out.printf("  节点访问比例: %.2f%%\n", nodeVisitRatio * 100);
        System.out.printf("  叶子访问比例: %.2f%%\n", leafVisitRatio * 100);
    }
    
    /**
     * 不同选择率的查询测试 - 使用生成的查询集
     */
    private static void selectivityTest(RTree rtree, List<Point2D1> points) {
        DataStats dataStats = analyzeData(points);
        // 调整选择率，适合50万数据
        double[] selectivities = {0.0001, 0.001, 0.01, 0.1};
        int queriesPerTest = 500; // 50万数据，适当增加查询数量
        
        System.out.println("选择率\t查询时间(ms)\t结果数量\t访问节点\t访问叶子");
        System.out.println("------\t-----------\t--------\t--------\t--------");
        
        for (double selectivity : selectivities) {
            List<Rectangle2D1> queries = generateQueries(dataStats, selectivity, queriesPerTest);
            
            long totalTime = 0;
            long totalResults = 0;
            long totalNodes = 0;
            long totalLeaves = 0;
            
            // 执行所有查询
            for (Rectangle2D1 query : queries) {
                RTreeQueryResult result = rtree.rangeQuery(query);
                totalTime += result.getStats().getQueryTimeNanos();
                totalResults += result.getResultCount();
                totalNodes += result.getStats().getNodesVisited();
                totalLeaves += result.getStats().getLeavesVisited();
            }
            
            double avgTime = totalTime / (double) queries.size() / 1_000_000.0;
            double avgResults = totalResults / (double) queries.size();
            double avgNodes = totalNodes / (double) queries.size();
            double avgLeaves = totalLeaves / (double) queries.size();
            
            System.out.printf("%.4f%%\t%.3f\t\t%.1f\t\t%.1f\t\t%.1f\n",
                            selectivity * 100, avgTime, avgResults, avgNodes, avgLeaves);
        }
    }
    
    /**
     * 内存使用分析
     */
    private static void memoryAnalysis(RTree rtree) {
        Runtime runtime = Runtime.getRuntime();
        
        // 强制垃圾回收
        System.gc();
        Thread.yield();
        
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        System.out.printf("JVM内存使用情况:\n");
        System.out.printf("  已用内存: %.2f MB\n", usedMemory / (1024.0 * 1024.0));
        System.out.printf("  总分配内存: %.2f MB\n", totalMemory / (1024.0 * 1024.0));
        System.out.printf("  最大可用内存: %.2f MB\n", maxMemory / (1024.0 * 1024.0));
        
        // 估算R-tree内存使用
        RTree.TreeStats stats = rtree.getStats();
        long estimatedRTreeMemory = estimateMemoryUsage(stats);
        System.out.printf("  估算R-tree内存: %.2f MB\n", estimatedRTreeMemory / (1024.0 * 1024.0));
        System.out.printf("  每个点平均内存: %.1f bytes\n", estimatedRTreeMemory / (double) stats.size);
    }
    
    /**
     * 生成测试查询
     */
    private static List<Rectangle2D1> generateQueries(DataStats stats, 
                                                   double selectivity, int count) {
        List<Rectangle2D1> queries = new ArrayList<>();
        Random random = new Random(42);
        
        // 计算查询矩形的边长 - 基于实际数据密度
        long totalArea = stats.rangeX * stats.rangeY;
        long queryArea = (long) (totalArea * selectivity);
        long sideLength = (long) Math.sqrt(queryArea);
        
        // 确保边长不会太小或太大
        sideLength = Math.max(sideLength, 1);
        sideLength = Math.min(sideLength, Math.min(stats.rangeX, stats.rangeY) / 2);
        
        System.out.printf("查询参数: 总面积=%d, 查询面积=%d, 边长=%d\n", 
                        totalArea, queryArea, sideLength);
        
        for (int i = 0; i < count; i++) {
            // 确保查询矩形在数据范围内
            long maxStartX = Math.max(stats.minX, stats.maxX - sideLength);
            long maxStartY = Math.max(stats.minY, stats.maxY - sideLength);
            
            long x1 = stats.minX + (long) (random.nextDouble() * (maxStartX - stats.minX + 1));
            long y1 = stats.minY + (long) (random.nextDouble() * (maxStartY - stats.minY + 1));
            long x2 = Math.min(x1 + sideLength, stats.maxX);
            long y2 = Math.min(y1 + sideLength, stats.maxY);
            
            // 确保矩形有效
            if (x2 > x1 && y2 > y1) {
                queries.add(new Rectangle2D1(x1, y1, x2, y2));
            } else {
                // 如果生成的矩形无效，重新生成
                i--;
            }
        }
        
        return queries;
    }
    
    /**
     * 估算内存使用
     */
    private static long estimateMemoryUsage(RTree.TreeStats stats) {
        // 估算每个节点和条目的内存使用
        long nodeOverhead = 64; // 对象头 + 字段
        long entryOverhead = 48; // 对象头 + 字段
        long pointOverhead = 32; // Point2D对象
        long rectangleOverhead = 40; // Rectangle2D对象
        
        long totalMemory = 0;
        
        // 节点内存
        totalMemory += stats.totalNodes * nodeOverhead;
        
        // 条目内存
        totalMemory += stats.totalEntries * entryOverhead;
        
        // 数据点内存
        totalMemory += stats.size * pointOverhead;
        
        // MBR内存
        totalMemory += stats.totalNodes * rectangleOverhead;
        
        return totalMemory;
    }
    
    /**
     * 分析数据统计
     */
    private static DataStats analyzeData(List<Point2D1> points) {
        if (points.isEmpty()) {
            return new DataStats(0, 0, 0, 0, 0);
        }
        
        long minX = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        
        for (Point2D1 point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        
        return new DataStats(points.size(), minX, maxX, minY, maxY);
    }
    
    /**
     * 数据统计类
     */
    private static class DataStats {
        public final int count;
        public final long minX, maxX, minY, maxY;
        public final long rangeX, rangeY;
        
        public DataStats(int count, long minX, long maxX, long minY, long maxY) {
            this.count = count;
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.rangeX = maxX - minX;
            this.rangeY = maxY - minY;
        }
        
        @Override
        public String toString() {
            return String.format(
                "数据统计:\n" +
                "  数据量: %d\n" +
                "  X范围: [%d, %d] (跨度: %d)\n" +
                "  Y范围: [%d, %d] (跨度: %d)\n" +
                "  空间面积: %d",
                count, minX, maxX, rangeX, minY, maxY, rangeY,
                rangeX * rangeY
            );
        }
    }
}
