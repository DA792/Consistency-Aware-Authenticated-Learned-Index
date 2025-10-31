package index.spatial_2d;

import java.util.*;

/**
 * 使用真实数据集的二维索引示例
 */
public class RealDataExample {
    
    public static void main(String[] args) {
        System.out.println("===== 使用真实数据集的二维空间索引 =====\n");
        
        // 1. 加载数据
        System.out.println("1. 加载数据集");
        String dataPath = "src/data/uniform_10k.csv";
        
        // 可以选择加载部分数据或全部数据
        int loadCount = 100000;  // 加载10万个点，0表示加载全部
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.err.println("错误: 没有加载到数据！");
            System.err.println("请确保数据文件路径正确: " + dataPath);
            return;
        }
        
        // 2. 分析数据统计信息
        System.out.println("\n2. 数据集分析");
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println(stats);
        
        // 3. 构建索引
        System.out.println("\n3. 构建二维索引");
        int errorBound = 64;
        System.out.println("误差边界: " + errorBound);
        
        long startTime = System.nanoTime();
        Spatial2DIndex index = new Spatial2DIndex(points, errorBound);
        long buildTime = System.nanoTime() - startTime;
        
        System.out.println("索引构建完成");
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms");
        System.out.println("平均每个点: " + (buildTime / points.size()) + " ns");
        
        // 4. 生成测试查询
        System.out.println("\n4. 生成测试查询");
        double[] selectivities = {0.0001, 0.001, 0.01};  // 0.01%, 0.1%, 1%
        
        for (double selectivity : selectivities) {
            System.out.println("\n--- 选择率: " + (selectivity * 100) + "% ---");
            
            // 生成查询
            List<Rectangle2D> queries = DataLoader.generateTestQueries(
                stats, selectivity, 100);
            
            // 执行查询测试
            testQueries(index, queries, selectivity);
        }
        
        // 5. 具体查询示例
        System.out.println("\n5. 具体查询示例");
        demonstrateQuery(index, stats);
        
        System.out.println("\n===== 测试完成 =====");
    }
    
    /**
     * 测试一批查询
     */
    private static void testQueries(Spatial2DIndex index, 
                                   List<Rectangle2D> queries,
                                   double selectivity) {
        long totalQueryTime = 0;
        long totalVerifyTime = 0;
        double totalVOSize = 0;
        int totalResults = 0;
        int totalCandidates = 0;
        int totalFalsePositives = 0;
        
        for (Rectangle2D query : queries) {
            // 执行查询
            long start = System.nanoTime();
            Spatial2DQueryResponse response = index.rectangleQuery(query);
            totalQueryTime += System.nanoTime() - start;
            
            // 验证
            start = System.nanoTime();
            index.verify(query, response);
            totalVerifyTime += System.nanoTime() - start;
            
            // 统计
            Spatial2DQueryResponse.QueryStats stats = response.getStats();
            totalResults += stats.resultCount;
            totalCandidates += stats.candidateCount;
            totalFalsePositives += stats.falsePositiveCount;
            totalVOSize += stats.voSize;
        }
        
        int n = queries.size();
        System.out.println("测试查询数量: " + n);
        System.out.println("平均查询时间: " + (totalQueryTime / n) / 1000000.0 + " ms");
        System.out.println("平均验证时间: " + (totalVerifyTime / n) / 1000000.0 + " ms");
        System.out.println("平均结果数量: " + (totalResults / n));
        System.out.println("平均候选数量: " + (totalCandidates / n));
        System.out.println("平均假阳性: " + (totalFalsePositives / n) + 
                         " (" + String.format("%.2f%%", 
                         100.0 * totalFalsePositives / totalCandidates) + ")");
        System.out.println("平均VO大小: " + 
                         String.format("%.2f KB", totalVOSize / n / 1024.0));
    }
    
    /**
     * 演示具体查询过程
     */
    private static void demonstrateQuery(Spatial2DIndex index, 
                                        DataLoader.DataStats stats) {
        // 创建一个示例查询
        long centerX = (stats.minX + stats.maxX) / 2;
        long centerY = (stats.minY + stats.maxY) / 2;
        long range = Math.min(stats.rangeX, stats.rangeY) / 10;
        
        Rectangle2D queryRect = new Rectangle2D(
            centerX - range/2, 
            centerY - range/2,
            centerX + range/2, 
            centerY + range/2
        );
        
        System.out.println("查询矩形: " + queryRect);
        System.out.println("矩形面积: " + queryRect.area());
        
        // 执行查询
        long start = System.nanoTime();
        Spatial2DQueryResponse response = index.rectangleQuery(queryRect);
        long queryTime = System.nanoTime() - start;
        
        System.out.println("\n查询结果:");
        System.out.println("  查询时间: " + queryTime / 1000000.0 + " ms");
        System.out.println("  " + response.getStats());
        
        // 显示部分结果
        System.out.println("\n前10个结果点:");
        for (int i = 0; i < Math.min(10, response.results.size()); i++) {
            System.out.println("  " + response.results.get(i));
        }
        
        // 显示区间分解信息
        System.out.println("\nZ-order区间分解:");
        for (int i = 0; i < Math.min(5, response.intervalResults.size()); i++) {
            Spatial2DQueryResult ir = response.intervalResults.get(i);
            System.out.println("  区间" + (i+1) + ": " + ir.interval + 
                             " -> " + ir.getFilteredCount() + " 个点");
        }
        if (response.intervalResults.size() > 5) {
            System.out.println("  ... (共 " + response.intervalResults.size() + " 个区间)");
        }
        
        // 验证
        start = System.nanoTime();
        boolean isValid = index.verify(queryRect, response);
        long verifyTime = System.nanoTime() - start;
        
        System.out.println("\n验证结果: " + (isValid ? "✓ 通过" : "✗ 失败"));
        System.out.println("验证时间: " + verifyTime / 1000000.0 + " ms");
    }
}


