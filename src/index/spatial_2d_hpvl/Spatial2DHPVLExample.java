package index.spatial_2d_hpvl;

import utils.*;
import java.util.*;

/**
 * 二维HPVL索引示例 - 混合优化型
 */
public class Spatial2DHPVLExample {
    
    public static void main(String[] args) {
        System.out.println("===== 二维HPVL索引示例 (混合优化型) =====\n");
        
        System.out.println("1. 生成测试数据");
        List<Point2D> points = generateRandomData(5000);
        System.out.println("生成了 " + points.size() + " 个点\n");
        
        System.out.println("2. 构建二维HPVL索引");
        int chainLen = 100;
        int err = 64;
        
        long startTime = System.nanoTime();
        Spatial2DHPVLIndex index = new Spatial2DHPVLIndex(chainLen, err);
        
        for (Point2D point : points) {
            index.insert(point);
        }
        
        long buildTime = System.nanoTime() - startTime;
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms\n");
        
        System.out.println("3. 查询测试");
        Rectangle2D queryRect = new Rectangle2D(5000, 5000, 10000, 10000);
        
        startTime = System.nanoTime();
        int version = index.currentVersion;
        Spatial2DRes response = index.rectangleQuery(queryRect, version);
        long queryTime = System.nanoTime() - startTime;
        
        System.out.println("查询时间: " + queryTime / 1000000.0 + " ms");
        System.out.println("结果数量: " + response.getResults().size());
        System.out.println("VO大小: " + response.getTotalVOSize() / 1024.0 + " KB");
        
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

