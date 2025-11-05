package index.spatial_2d_pvlb;

import utils.*;
import java.util.*;

/**
 * 二维PVLB索引示例 - 更新优化型
 */
public class Spatial2DPVLBExample {
    
    public static void main(String[] args) {
        System.out.println("===== 二维PVLB索引示例 (更新优化型) =====\n");
        
        System.out.println("1. 生成测试数据");
        List<Point2D> points = generateRandomData(1000);
        System.out.println("生成了 " + points.size() + " 个点\n");
        
        System.out.println("2. 构建二维PVLB索引（动态插入）");
        long startTime = System.nanoTime();
        
        Spatial2DPVLBTree tree = new Spatial2DPVLBTree(points.get(0));
        for (int i = 1; i < points.size(); i++) {
            tree = tree.insert(points.get(i));
        }
        
        long buildTime = System.nanoTime() - startTime;
        System.out.println("构建时间: " + buildTime / 1000000.0 + " ms\n");
        
        System.out.println("3. 查询测试");
        Rectangle2D queryRect = new Rectangle2D(5000, 5000, 10000, 10000);
        
        startTime = System.nanoTime();
        Spatial2DPVLB_Res response = tree.rectangleQuery(queryRect);
        long queryTime = System.nanoTime() - startTime;
        
        System.out.println("查询时间: " + queryTime / 1000000.0 + " ms");
        System.out.println("结果数量: " + response.results.size());
        
        startTime = System.nanoTime();
        boolean isValid = tree.verify(queryRect, response);
        long verifyTime = System.nanoTime() - startTime;
        
        System.out.println("验证时间: " + verifyTime / 1000000.0 + " ms");
        System.out.println("验证结果: " + (isValid ? "通过" : "失败"));
        
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



