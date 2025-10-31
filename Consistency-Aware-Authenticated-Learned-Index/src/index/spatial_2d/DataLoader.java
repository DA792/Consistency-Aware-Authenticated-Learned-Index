package index.spatial_2d;

import java.io.*;
import java.util.*;

/**
 * 数据加载工具类
 * 从CSV文件加载二维点数据
 */
public class DataLoader {
    
    /**
     * 从CSV文件加载二维点数据
     * @param filePath CSV文件路径
     * @param maxCount 最大加载数量（0表示加载全部）
     * @return 二维点列表
     */
    public static List<Point2D> loadFromCSV(String filePath, int maxCount) {
        List<Point2D> points = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            int count = 0;
            
            while ((line = br.readLine()) != null) {
                if (maxCount > 0 && count >= maxCount) {
                    break;
                }
                
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 解析坐标
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        long x = Long.parseLong(parts[0]);
                        long y = Long.parseLong(parts[1]);
                        points.add(new Point2D(x, y));
                        count++;
                    } catch (NumberFormatException e) {
                        // 跳过无法解析的行
                        System.err.println("警告: 无法解析行: " + line);
                    }
                }
            }
            
            System.out.println("成功加载 " + points.size() + " 个数据点");
            
        } catch (IOException e) {
            System.err.println("读取文件失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        return points;
    }
    
    /**
     * 加载全部数据
     */
    public static List<Point2D> loadFromCSV(String filePath) {
        return loadFromCSV(filePath, 0);
    }
    
    /**
     * 分析数据集的统计信息
     */
    public static DataStats analyzeData(List<Point2D> points) {
        if (points.isEmpty()) {
            return new DataStats(0, 0, 0, 0, 0);
        }
        
        long minX = Long.MAX_VALUE;
        long maxX = Long.MIN_VALUE;
        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        
        for (Point2D point : points) {
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        
        return new DataStats(points.size(), minX, maxX, minY, maxY);
    }
    
    /**
     * 数据统计信息
     */
    public static class DataStats {
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
    
    /**
     * 生成测试查询矩形
     * @param stats 数据统计信息
     * @param selectivity 查询选择率（0.001表示0.1%的数据区域）
     * @param count 生成数量
     * @return 查询矩形列表
     */
    public static List<Rectangle2D> generateTestQueries(DataStats stats, 
                                                       double selectivity, 
                                                       int count) {
        List<Rectangle2D> queries = new ArrayList<>();
        Random random = new Random(42);
        
        // 计算查询矩形的边长
        long area = (long) (stats.rangeX * stats.rangeY * selectivity);
        long sideLength = (long) Math.sqrt(area);
        
        System.out.println("生成查询矩形: 选择率=" + selectivity + 
                         ", 边长≈" + sideLength);
        
        for (int i = 0; i < count; i++) {
            // 随机选择左下角
            long x1 = stats.minX + (long) (random.nextDouble() * 
                                          (stats.rangeX - sideLength));
            long y1 = stats.minY + (long) (random.nextDouble() * 
                                          (stats.rangeY - sideLength));
            
            // 计算右上角
            long x2 = Math.min(x1 + sideLength, stats.maxX);
            long y2 = Math.min(y1 + sideLength, stats.maxY);
            
            queries.add(new Rectangle2D(x1, y1, x2, y2));
        }
        
        return queries;
    }
}


