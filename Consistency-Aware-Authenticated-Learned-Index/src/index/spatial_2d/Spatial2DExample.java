package index.spatial_2d;

import java.util.*;

/**
 * 二维空间索引使用示例
 */
public class Spatial2DExample {
    
    public static void main(String[] args) {
        System.out.println("===== 二维空间索引示例 =====\n");
        
        // 选择数据源
        boolean useRealData = true;  // 改为false使用随机生成的数据
        List<Point2D> points;
        
        if (useRealData) {
            // 1a. 使用真实数据集
            System.out.println("1. 加载真实数据集");
            String dataPath = "src/data/uniform_10k.csv";
            int loadCount = 100000;  // 加载10万个点
            points = DataLoader.loadFromCSV(dataPath, loadCount);
            
            if (points.isEmpty()) {
                System.err.println("加载数据失败，切换到随机数据");
                points = generateTestData(100000);
            } else {
                DataLoader.DataStats stats = DataLoader.analyzeData(points);
                System.out.println(stats);
            }
        } else {
            // 1b. 生成测试数据
            int dataSize = 100000;
            System.out.println("1. 生成测试数据: " + dataSize + " 个点");
            points = generateTestData(dataSize);
        }
        
        // 2. 构建二维索引
        int errorBound = 64;
        System.out.println("2. 构建二维索引 (errorBound=" + errorBound + ")...");
        long startTime = System.nanoTime();
        Spatial2DIndex index = new Spatial2DIndex(points, errorBound);
        long buildTime = System.nanoTime() - startTime;
        System.out.println("   索引构建时间: " + buildTime / 1000000.0 + " ms\n");
        
        // 3. 执行多个测试查询
        Rectangle2D[] testQueries = {
            new Rectangle2D(100, 100, 200, 200),      // 小矩形
            new Rectangle2D(200, 200, 400, 400),      // 中矩形
            new Rectangle2D(100, 100, 500, 500),      // 大矩形
        };
        
        System.out.println("3. 执行矩形查询测试\n");
        for (int i = 0; i < testQueries.length; i++) {
            Rectangle2D queryRect = testQueries[i];
            System.out.println("--- 查询 " + (i + 1) + " ---");
            System.out.println("查询矩形: " + queryRect);
            System.out.println("矩形面积: " + queryRect.area());
            
            // 执行查询
            startTime = System.nanoTime();
            Spatial2DQueryResponse response = index.rectangleQuery(queryRect);
            long queryTime = System.nanoTime() - startTime;
            
            // 显示查询统计
            Spatial2DQueryResponse.QueryStats stats = response.getStats();
            System.out.println("查询时间: " + queryTime / 1000000.0 + " ms");
            System.out.println(stats);
            
            // 显示部分结果
            System.out.println("前5个结果点:");
            for (int j = 0; j < Math.min(5, response.results.size()); j++) {
                System.out.println("  " + response.results.get(j));
            }
            
            // 验证结果
            System.out.println("\n验证查询结果...");
            startTime = System.nanoTime();
            boolean isValid = index.verify(queryRect, response);
            long verifyTime = System.nanoTime() - startTime;
            
            if (isValid) {
                System.out.println("✓ 验证通过! (耗时: " + verifyTime / 1000000.0 + " ms)");
            } else {
                System.out.println("✗ 验证失败!");
            }
            System.out.println();
        }
        
        // 4. 性能测试
        System.out.println("4. 性能测试 (1000次查询)");
        performanceTest(index, 1000);
        
        // 5. 显示索引大小
        System.out.println("\n5. 索引大小");
        index.printIndexSize();
    }
    
    /**
     * 生成测试数据
     */
    private static List<Point2D> generateTestData(int count) {
        List<Point2D> points = new ArrayList<>();
        Random random = new Random(42);
        
        // 生成聚集分布的数据
        for (int i = 0; i < count; i++) {
            long x, y;
            if (random.nextDouble() < 0.7) {
                // 70%的数据聚集在中心区域
                x = 250 + random.nextInt(250);
                y = 250 + random.nextInt(250);
            } else {
                // 30%的数据分散分布
                x = random.nextInt(1000);
                y = random.nextInt(1000);
            }
            points.add(new Point2D(x, y));
        }
        
        return points;
    }
    
    /**
     * 性能测试
     */
    private static void performanceTest(Spatial2DIndex index, int queryCount) {
        Random random = new Random(123);
        long totalQueryTime = 0;
        long totalVerifyTime = 0;
        double totalVOSize = 0;
        int totalResults = 0;
        
        for (int i = 0; i < queryCount; i++) {
            // 生成随机查询矩形
            long x1 = random.nextInt(800);
            long y1 = random.nextInt(800);
            long size = 50 + random.nextInt(150);
            Rectangle2D queryRect = new Rectangle2D(x1, y1, x1 + size, y1 + size);
            
            // 执行查询
            long start = System.nanoTime();
            Spatial2DQueryResponse response = index.rectangleQuery(queryRect);
            totalQueryTime += System.nanoTime() - start;
            
            // 验证
            start = System.nanoTime();
            index.verify(queryRect, response);
            totalVerifyTime += System.nanoTime() - start;
            
            totalVOSize += response.getTotalVOSize();
            totalResults += response.results.size();
        }
        
        System.out.println("平均查询时间: " + (totalQueryTime / queryCount) / 1000000.0 + " ms");
        System.out.println("平均验证时间: " + (totalVerifyTime / queryCount) / 1000000.0 + " ms");
        System.out.println("平均VO大小: " + (totalVOSize / queryCount) / 1024.0 + " KB");
        System.out.println("平均结果数量: " + (totalResults / queryCount));
    }
}

