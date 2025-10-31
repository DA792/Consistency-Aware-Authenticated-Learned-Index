package Rtree;

import utils.Point2D;
import utils.Rectangle2D;
import utils.DataLoader;
import java.util.List;
import java.util.Random;

/**
 * R-tree测试类
 * 演示R-tree的基本功能和性能测试
 */
public class RTreeTest {
    
    public static void main(String[] args) {
        System.out.println("=== R-tree 测试程序 ===\n");
        
        // 基本功能测试
        testBasicFunctionality();
        
        // 性能测试
        testPerformance();
        
        // 使用真实数据集测试
        testWithRealData();
    }
    
    /**
     * 基本功能测试
     */
    public static void testBasicFunctionality() {
        System.out.println("1. 基本功能测试");
        System.out.println("================");
        
        RTree rtree = new RTree();
        
        // 插入一些测试点
        Point2D[] testPoints = {
            new Point2D(10, 20),
            new Point2D(30, 40),
            new Point2D(50, 60),
            new Point2D(15, 25),
            new Point2D(35, 45),
            new Point2D(55, 65),
            new Point2D(5, 10),
            new Point2D(75, 80)
        };
        
        System.out.println("插入测试点...");
        for (int i = 0; i < testPoints.length; i++) {
            rtree.insert(testPoints[i], "Data_" + i);
            System.out.printf("插入点 %s (数据: Data_%d)\n", testPoints[i], i);
        }
        
        System.out.println("\n树统计信息:");
        System.out.println(rtree.getStats());
        
        // 范围查询测试
        System.out.println("\n范围查询测试:");
        Rectangle2D queryRect = new Rectangle2D(0, 0, 50, 50);
        System.out.printf("查询矩形: %s\n", queryRect);
        
        RTreeQueryResult result = rtree.rangeQuery(queryRect);
        System.out.printf("查询结果: %d 个点\n", result.getResultCount());
        System.out.println("查询统计: " + result.getStats());
        
        for (int i = 0; i < result.getResultCount(); i++) {
            Point2D point = result.getResultPoint(i);
            Object data = result.getResultData(i);
            System.out.printf("  结果 %d: %s (数据: %s)\n", i + 1, point, data);
        }
        
        // 点查询测试
        System.out.println("\n点查询测试:");
        Point2D queryPoint = new Point2D(30, 40);
        System.out.printf("查询点: %s\n", queryPoint);
        
        RTreeQueryResult pointResult = rtree.pointQuery(queryPoint);
        System.out.printf("点查询结果: %d 个匹配点\n", pointResult.getResultCount());
        
        System.out.println();
    }
    
    /**
     * 性能测试
     */
    public static void testPerformance() {
        System.out.println("2. 性能测试");
        System.out.println("============");
        
        int[] dataSizes = {1000, 5000, 10000, 50000};
        
        for (int dataSize : dataSizes) {
            System.out.printf("\n测试数据量: %d\n", dataSize);
            System.out.println("-".repeat(20));
            
            // 生成随机数据
            RTree rtree = new RTree();
            Random random = new Random(42); // 固定种子保证可重复性
            
            // 插入性能测试
            long insertStart = System.nanoTime();
            for (int i = 0; i < dataSize; i++) {
                long x = random.nextInt(100000);
                long y = random.nextInt(100000);
                rtree.insert(new Point2D(x, y), "Data_" + i);
            }
            long insertEnd = System.nanoTime();
            
            double insertTime = (insertEnd - insertStart) / 1_000_000.0;
            System.out.printf("插入时间: %.2f ms (%.2f μs/点)\n", 
                            insertTime, insertTime * 1000 / dataSize);
            
            // 树统计
            RTree.TreeStats stats = rtree.getStats();
            System.out.println("树统计: " + stats);
            
            // 查询性能测试
            int numQueries = 100;
            long totalQueryTime = 0;
            int totalResults = 0;
            
            for (int i = 0; i < numQueries; i++) {
                // 生成随机查询矩形
                long x1 = random.nextInt(90000);
                long y1 = random.nextInt(90000);
                long x2 = x1 + random.nextInt(10000);
                long y2 = y1 + random.nextInt(10000);
                
                Rectangle2D queryRect = new Rectangle2D(x1, y1, x2, y2);
                
                long queryStart = System.nanoTime();
                RTreeQueryResult result = rtree.rangeQuery(queryRect);
                long queryEnd = System.nanoTime();
                
                totalQueryTime += (queryEnd - queryStart);
                totalResults += result.getResultCount();
            }
            
            double avgQueryTime = totalQueryTime / (double) numQueries / 1_000.0;
            System.out.printf("平均查询时间: %.2f μs (%d 次查询)\n", 
                            avgQueryTime, numQueries);
            System.out.printf("平均查询结果: %.1f 个点\n", 
                            totalResults / (double) numQueries);
        }
        
        System.out.println();
    }
    
    /**
     * 使用真实数据集测试
     */
    public static void testWithRealData() {
        System.out.println("3. 真实数据集测试");
        System.out.println("==================");
        
        // 尝试加载不同的数据集
        String[] dataFiles = {
            "src/data/uniform_10k.csv",
            "src/data/uniform_500k.csv"
        };
        
        for (String dataFile : dataFiles) {
            System.out.printf("\n测试数据集: %s\n", dataFile);
            System.out.println("-".repeat(30));
            
            try {
                // 加载数据
                long loadStart = System.nanoTime();
                List<Point2D> points = DataLoader.loadFromCSV(dataFile);
                long loadEnd = System.nanoTime();
                
                if (points.isEmpty()) {
                    System.out.println("数据集为空或加载失败，跳过测试");
                    continue;
                }
                
                System.out.printf("数据加载完成: %d 个点 (%.2f ms)\n", 
                                points.size(), (loadEnd - loadStart) / 1_000_000.0);
                
                // 构建R-tree
                RTree rtree = new RTree();
                long buildStart = System.nanoTime();
                
                for (int i = 0; i < points.size(); i++) {
                    rtree.insert(points.get(i), i);
                }
                
                long buildEnd = System.nanoTime();
                double buildTime = (buildEnd - buildStart) / 1_000_000.0;
                
                System.out.printf("R-tree构建完成: %.2f ms (%.2f μs/点)\n", 
                                buildTime, buildTime * 1000 / points.size());
                
                // 树统计
                RTree.TreeStats stats = rtree.getStats();
                System.out.println("树统计: " + stats);
                
                // 执行一些范围查询
                Random random = new Random(42);
                int numQueries = 50;
                long totalQueryTime = 0;
                int totalResults = 0;
                
                // 计算数据范围
                long minX = Long.MAX_VALUE, minY = Long.MAX_VALUE;
                long maxX = Long.MIN_VALUE, maxY = Long.MIN_VALUE;
                
                for (Point2D point : points) {
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                }
                
                System.out.printf("数据范围: [%d,%d] x [%d,%d]\n", minX, minY, maxX, maxY);
                
                for (int i = 0; i < numQueries; i++) {
                    // 生成查询矩形（覆盖约1%的数据空间）
                    long rangeX = (maxX - minX) / 10;
                    long rangeY = (maxY - minY) / 10;
                    
                    long x1 = minX + random.nextLong() % (maxX - minX - rangeX);
                    long y1 = minY + random.nextLong() % (maxY - minY - rangeY);
                    long x2 = x1 + rangeX;
                    long y2 = y1 + rangeY;
                    
                    Rectangle2D queryRect = new Rectangle2D(x1, y1, x2, y2);
                    
                    long queryStart = System.nanoTime();
                    RTreeQueryResult result = rtree.rangeQuery(queryRect);
                    long queryEnd = System.nanoTime();
                    
                    totalQueryTime += (queryEnd - queryStart);
                    totalResults += result.getResultCount();
                }
                
                double avgQueryTime = totalQueryTime / (double) numQueries / 1_000.0;
                System.out.printf("平均查询时间: %.2f μs (%d 次查询)\n", 
                                avgQueryTime, numQueries);
                System.out.printf("平均查询结果: %.1f 个点\n", 
                                totalResults / (double) numQueries);
                
            } catch (Exception e) {
                System.out.printf("测试数据集 %s 时出错: %s\n", dataFile, e.getMessage());
            }
        }
        
        System.out.println();
    }
    
    /**
     * 创建演示数据集
     */
    public static void createDemoDataset() {
        System.out.println("4. 创建演示数据集");
        System.out.println("==================");
        
        RTree rtree = new RTree();
        
        // 创建一些聚集的点群
        addCluster(rtree, 20, 30, 10, 50);  // 左下角聚集
        addCluster(rtree, 70, 80, 10, 50);  // 右上角聚集
        addCluster(rtree, 50, 50, 5, 20);   // 中心小聚集
        
        System.out.println("演示数据集创建完成");
        System.out.println("树统计: " + rtree.getStats());
        
        // 演示不同大小的范围查询
        Rectangle2D[] queryRects = {
            new Rectangle2D(15, 25, 25, 35),  // 小范围查询
            new Rectangle2D(10, 10, 60, 60),  // 中等范围查询
            new Rectangle2D(0, 0, 100, 100)   // 大范围查询
        };
        
        for (int i = 0; i < queryRects.length; i++) {
            Rectangle2D rect = queryRects[i];
            RTreeQueryResult result = rtree.rangeQuery(rect);
            System.out.printf("查询 %d: 矩形 %s -> %d 个结果 (%.2f μs)\n", 
                            i + 1, rect, result.getResultCount(), 
                            result.getStats().getQueryTimeMicros());
        }
    }
    
    /**
     * 添加点聚集
     */
    private static void addCluster(RTree rtree, long centerX, long centerY, 
                                  long radius, int numPoints) {
        Random random = new Random();
        
        for (int i = 0; i < numPoints; i++) {
            long x = centerX + (random.nextLong() % (2 * radius)) - radius;
            long y = centerY + (random.nextLong() % (2 * radius)) - radius;
            rtree.insert(new Point2D(x, y), "Cluster_" + centerX + "_" + centerY + "_" + i);
        }
    }
}
