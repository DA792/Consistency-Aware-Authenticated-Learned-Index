package index.spatial_2d;

import java.util.*;

/**
 * 二维空间索引快速开始示例
 * 演示最基本的使用方法
 */
public class QuickStartExample {
    public static void main(String[] args) {
        System.out.println("===== 二维空间索引快速开始 =====\n");
        
        // 1. 创建数据
        System.out.println("1. 创建测试数据...");
        List<Point2D> points = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < 10000; i++) {
            long x = random.nextInt(1000);
            long y = random.nextInt(1000);
            points.add(new Point2D(x, y));
        }
        System.out.println("   创建了 " + points.size() + " 个点\n");
        
        // 2. 构建索引
        System.out.println("2. 构建二维索引...");
        long startTime = System.nanoTime();
        Spatial2DIndex index = new Spatial2DIndex(points, 64);
        long buildTime = System.nanoTime() - startTime;
        System.out.println("   构建完成，耗时: " + buildTime / 1000000.0 + " ms\n");
        
        // 3. 执行查询
        System.out.println("3. 执行矩形查询");
        Rectangle2D queryRect = new Rectangle2D(200, 200, 400, 400);
        System.out.println("   查询矩形: " + queryRect);
        System.out.println("   矩形面积: " + queryRect.area() + " 平方单位");
        
        startTime = System.nanoTime();
        Spatial2DQueryResponse response = index.rectangleQuery(queryRect);
        long queryTime = System.nanoTime() - startTime;
        
        System.out.println("   查询完成，耗时: " + queryTime / 1000000.0 + " ms\n");
        
        // 4. 显示结果
        System.out.println("4. 查询结果");
        System.out.println("   找到 " + response.results.size() + " 个点");
        
        // 显示前5个点
        System.out.println("   前5个结果点:");
        for (int i = 0; i < Math.min(5, response.results.size()); i++) {
            System.out.println("     " + response.results.get(i));
        }
        
        // 显示统计信息
        System.out.println("\n   " + response.getStats());
        System.out.println();
        
        // 5. 验证结果
        System.out.println("5. 验证查询结果");
        startTime = System.nanoTime();
        boolean isValid = index.verify(queryRect, response);
        long verifyTime = System.nanoTime() - startTime;
        
        if (isValid) {
            System.out.println("   ✓ 验证通过！");
        } else {
            System.out.println("   ✗ 验证失败！");
        }
        System.out.println("   验证耗时: " + verifyTime / 1000000.0 + " ms\n");
        
        // 6. 多次查询性能测试
        System.out.println("6. 性能测试 (100次随机查询)");
        performanceTest(index, 100);
        
        System.out.println("\n===== 示例完成 =====");
    }
    
    /**
     * 简单的性能测试
     */
    private static void performanceTest(Spatial2DIndex index, int queryCount) {
        Random random = new Random(123);
        long totalQueryTime = 0;
        long totalVerifyTime = 0;
        int totalResults = 0;
        
        for (int i = 0; i < queryCount; i++) {
            // 生成随机查询矩形
            long x = random.nextInt(800);
            long y = random.nextInt(800);
            long size = 100 + random.nextInt(100);
            Rectangle2D queryRect = new Rectangle2D(x, y, x + size, y + size);
            
            // 查询
            long start = System.nanoTime();
            Spatial2DQueryResponse response = index.rectangleQuery(queryRect);
            totalQueryTime += System.nanoTime() - start;
            
            // 验证
            start = System.nanoTime();
            index.verify(queryRect, response);
            totalVerifyTime += System.nanoTime() - start;
            
            totalResults += response.results.size();
        }
        
        System.out.println("   平均查询时间: " + (totalQueryTime / queryCount) / 1000000.0 + " ms");
        System.out.println("   平均验证时间: " + (totalVerifyTime / queryCount) / 1000000.0 + " ms");
        System.out.println("   平均结果数量: " + (totalResults / queryCount) + " 个点");
    }
}

