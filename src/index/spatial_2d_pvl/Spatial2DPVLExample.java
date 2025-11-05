package index.spatial_2d_pvl;

import utils.*;
import java.util.*;

/**
 * 二维PVL索引示例 - 查询优化型
 */
public class Spatial2DPVLExample {
    
    public static void main(String[] args) {
        System.out.println("===== 二维PVL索引示例 (查询优化型) =====\n");
        
        // 1. 加载数据
        System.out.println("1. 加载数据");
        String dataPath = "src/data/uniform_10k.csv";
        int loadCount = 10000;
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.out.println("使用随机数据");
            points = generateRandomData(10000);
        }
        
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println(stats);
        System.out.println();
        
        // 2. 构建索引
        System.out.println("2. 构建二维PVL索引");
        int errorBound = 64;
        long startTime = System.nanoTime();
        Spatial2DPVLTree index = new Spatial2DPVLTree(points, errorBound);
        long buildTime = System.nanoTime() - startTime;
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms\n");
        
        // 3. 执行查询测试
        System.out.println("3. 查询测试");
        double selectivity = 0.001;
        List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, 100);
        
        long totalQueryTime = 0;
        long totalVerifyTime = 0;
        
        for (Rectangle2D query : queries) {
            startTime = System.nanoTime();
            Spatial2DPVL_Res response = index.rectangleQuery(query);
            totalQueryTime += System.nanoTime() - startTime;
            
            startTime = System.nanoTime();
            index.verify(query, response);
            totalVerifyTime += System.nanoTime() - startTime;
        }
        
        System.out.println("平均查询时间: " + (totalQueryTime / queries.size()) / 1000000.0 + " ms");
        System.out.println("平均验证时间: " + (totalVerifyTime / queries.size()) / 1000000.0 + " ms");
        
        System.out.println("\n===== 测试完成 =====");
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


