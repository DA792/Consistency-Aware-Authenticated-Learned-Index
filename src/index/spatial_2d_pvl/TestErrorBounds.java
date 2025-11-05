package index.spatial_2d_pvl;

import utils.*;
import java.util.*;

/**
 * 测试不同误差界限的性能影响
 */
public class TestErrorBounds {
    
    public static void main(String[] args) {
        int errorBound = 32;  // 默认值
        
        if (args.length > 0) {
            errorBound = Integer.parseInt(args[0]);
        }
        
        System.out.println("===== 误差界限: " + errorBound + " =====\n");
        
        // 1. 加载数据
        System.out.println("1. 加载数据集");
        String dataPath = "src/data/uniform_10k.csv";
        int loadCount = 10000;
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.out.println("数据文件未找到,使用随机生成数据");
            points = generateRandomData(10000);
        }
        
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println("数据点数: " + points.size());
        System.out.println();
        
        // 2. 构建索引
        System.out.println("2. 构建索引 (误差界限=" + errorBound + ")");
        long startTime = System.nanoTime();
        Spatial2DPVLTree tree = new Spatial2DPVLTree(points, errorBound);
        long buildTime = System.nanoTime() - startTime;
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms");
        tree.printIndexSize();
        System.out.println();
        
        // 3. 性能测试 - 只测试几个关键选择性
        System.out.println("3. 查询性能测试 (并行查询)");
        double[] queryRange = new double[]{0.001, 0.01, 0.02};
        int queryLen = 1000;
        
        for (double selectivity : queryRange) {
            List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, queryLen);
            
            long totalQueryTime = 0;
            long totalVerifyTime = 0;
            double totalVOSize = 0;
            int totalResults = 0;
            int totalIntervals = 0;
            
            for (Rectangle2D query : queries) {
                startTime = System.nanoTime();
                Spatial2DPVL_Res response = tree.rectangleQuery(query);
                totalQueryTime += System.nanoTime() - startTime;
                
                startTime = System.nanoTime();
                tree.verify(query, response);
                totalVerifyTime += System.nanoTime() - startTime;
                
                totalVOSize += response.getTotalVOSize();
                totalResults += response.results.size();
                
                Spatial2DPVL_Res.QueryStats queryStats = response.getStats();
                totalIntervals += queryStats.intervalCount;
            }
            
            System.out.println("选择性 " + selectivity + ":");
            System.out.println("  查询时间: " + String.format("%.3f", totalQueryTime / queryLen / 1000000.0) + " ms");
            System.out.println("  验证时间: " + String.format("%.3f", totalVerifyTime / queryLen / 1000000.0) + " ms");
            System.out.println("  总时间:   " + String.format("%.3f", (totalQueryTime + totalVerifyTime) / queryLen / 1000000.0) + " ms");
            System.out.println("  Z区间数:  " + (totalIntervals / queryLen));
            System.out.println("  VO大小:   " + String.format("%.2f", totalVOSize / queryLen / 1024.0) + " KB");
            System.out.println("  结果数:   " + (totalResults / queryLen));
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

