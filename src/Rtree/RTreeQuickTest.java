package Rtree;

import utils.Point2D;
import utils.Rectangle2D;
import utils.DataLoader;
import java.util.List;

/**
 * R-tree快速测试程序
 * 使用uniform_500k.csv的子集进行快速验证
 */
public class RTreeQuickTest {
    
    public static void main(String[] args) {
        System.out.println("=== R-tree 快速测试 (uniform_500k.csv 子集) ===\n");
        
        // 加载数据子集 (前10000个点)
        System.out.println("1. 加载数据子集");
        System.out.println("===============");
        
        long startTime = System.nanoTime();
        List<Point2D> points = DataLoader.loadFromCSV("src/data/uniform_500k.csv", 10000);
        long endTime = System.nanoTime();
        
        if (points.isEmpty()) {
            System.out.println("数据加载失败!");
            return;
        }
        
        double loadTime = (endTime - startTime) / 1_000_000.0;
        System.out.printf("加载完成: %d 个点 (%.2f ms)\n", points.size(), loadTime);
        
        // 分析数据
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println(stats);
        
        // 构建R-tree
        System.out.println("\n2. 构建R-tree");
        System.out.println("==============");
        
        RTree rtree = new RTree();
        startTime = System.nanoTime();
        
        for (int i = 0; i < points.size(); i++) {
            rtree.insert(points.get(i), "Data_" + i);
        }
        
        endTime = System.nanoTime();
        double buildTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("构建完成: %.2f ms (%.3f μs/点)\n", 
                        buildTime, buildTime * 1000 / points.size());
        
        // 树统计
        RTree.TreeStats treeStats = rtree.getStats();
        System.out.println("树统计: " + treeStats);
        
        // 快速查询测试
        System.out.println("\n3. 查询测试");
        System.out.println("============");
        
        // 测试不同大小的查询矩形
        Rectangle2D[] testQueries = {
            // 小范围查询 (约0.1%选择率)
            new Rectangle2D(stats.minX, stats.minY, 
                          stats.minX + stats.rangeX/100, 
                          stats.minY + stats.rangeY/100),
            
            // 中等范围查询 (约1%选择率)
            new Rectangle2D(stats.minX + stats.rangeX/4, stats.minY + stats.rangeY/4,
                          stats.minX + stats.rangeX/4 + stats.rangeX/10,
                          stats.minY + stats.rangeY/4 + stats.rangeY/10),
            
            // 大范围查询 (约10%选择率)
            new Rectangle2D(stats.minX + stats.rangeX/3, stats.minY + stats.rangeY/3,
                          stats.minX + 2*stats.rangeX/3, stats.minY + 2*stats.rangeY/3)
        };
        
        String[] queryNames = {"小范围查询", "中等范围查询", "大范围查询"};
        
        for (int i = 0; i < testQueries.length; i++) {
            Rectangle2D query = testQueries[i];
            
            RTreeQueryResult result = rtree.rangeQuery(query);
            
            System.out.printf("%s:\n", queryNames[i]);
            System.out.printf("  查询矩形: %s\n", query);
            System.out.printf("  结果数量: %d 个点\n", result.getResultCount());
            System.out.printf("  查询时间: %.2f μs\n", result.getStats().getQueryTimeMicros());
            System.out.printf("  访问节点: %d 个\n", result.getStats().getNodesVisited());
            System.out.printf("  访问叶子: %d 个\n", result.getStats().getLeavesVisited());
            System.out.printf("  选择率: %.3f%%\n", 
                            result.getResultCount() * 100.0 / points.size());
            System.out.println();
        }
        
        // 点查询测试
        System.out.println("4. 点查询测试");
        System.out.println("==============");
        
        // 测试几个已知存在的点
        for (int i = 0; i < Math.min(5, points.size()); i += points.size()/5) {
            Point2D testPoint = points.get(i);
            RTreeQueryResult result = rtree.pointQuery(testPoint);
            
            System.out.printf("查询点 %s: ", testPoint);
            if (result.getResultCount() > 0) {
                System.out.printf("找到 (%.2f μs)\n", result.getStats().getQueryTimeMicros());
            } else {
                System.out.println("未找到 (错误!)");
            }
        }
        
        System.out.println("\n快速测试完成!");
        System.out.println("如需完整的50万数据实验，请运行 RTreeExperiment");
    }
}
