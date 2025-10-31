package index.mht_complete;

import utils.*;
import java.util.*;

/**
 * 完整MHT测试 - 与Spatial2DPVLTree使用相同的测试配置
 * 
 * 验证完整的Merkle路径重建功能
 * 使用与PVL相同的数据集、查询集和选择性
 */
public class CompleteMHTSimpleTest {
    
    public static void main(String[] args) {
        System.out.println("===== 完整MHT测试 (与PVL相同配置) =====\n");
        
        // 1. 加载数据 (与PVL相同)
        System.out.println("1. 加载数据集 (50万点)");
        String dataPath = "src/data/uniform_500k.csv";
        int loadCount = 500000;
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.out.println("数据文件未找到,使用随机生成数据");
            points = generateRandomData(100000);
        }
        
        System.out.println("实际加载: " + points.size() + " 个数据点");
        
        // 按Z值排序数据（MHT要求数据有序）
        points.sort(Comparator.comparingLong(p -> p.zValue));
        System.out.println("数据已按Z值排序");
        
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println("X范围: [" + stats.minX + ", " + stats.maxX + "]");
        System.out.println("Y范围: [" + stats.minY + ", " + stats.maxY + "]");
        
        // 计算Z值范围
        long minZ = points.get(0).zValue;
        long maxZ = points.get(points.size() - 1).zValue;
        System.out.println("Z值范围: [" + minZ + ", " + maxZ + "]");
        System.out.println();
        
        // 2. 构建完整MHT
        System.out.println("2. 构建完整MHT...");
        CompleteMerkleHashTree mht = new CompleteMerkleHashTree(256);
        
        long buildStart = System.nanoTime();
        mht.build(points);
        long buildTime = System.nanoTime() - buildStart;
        
        System.out.println(mht.getStats());
        System.out.println("   构建时间: " + String.format("%.4f ms", buildTime / 1_000_000.0));
        System.out.println();
        
        // 3. 性能测试 (与PVL相同的选择性)
        System.out.println("3. 查询性能测试...\n");
        
        double[] queryRange = new double[]{0.0001, 0.001, 0.01, 0.1};  // 与PVL相同
        int queryLen = 300;  // 与PVL相同
        
        System.out.println("查询次数: " + queryLen);
        System.out.println("查询选择性: " + Arrays.toString(queryRange));
        System.out.println();
        
        runPerformanceTest(mht, stats, queryRange, queryLen);
        
        System.out.println("\n===== 测试完成 =====");
    }
    
    /**
     * 性能测试 - 将2D矩形转换为Z值范围查询
     */
    private static void runPerformanceTest(CompleteMerkleHashTree mht, 
                                          DataLoader.DataStats stats,
                                          double[] queryRange, 
                                          int queryLen) {
        
        for (double selectivity : queryRange) {
            System.out.println("========================================");
            System.out.println("查询选择性: " + selectivity);
            System.out.println("========================================");
            
            // 生成2D矩形查询
            List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, queryLen);
            
            long totalQueryTime = 0;
            long totalVerifyTime = 0;
            long totalVOSize = 0;
            int totalResults = 0;
            int totalLeafInfos = 0;
            int totalSiblings = 0;
            int totalBoundaries = 0;
            int queryCount = 0;
            
            // 执行查询
            for (Rectangle2D query : queries) {
                queryCount++;
                // 将矩形的左下角和右上角转换为Z值
                long zStart = ZOrderCurve.encode(query.minX, query.minY);
                long zEnd = ZOrderCurve.encode(query.maxX, query.maxY);
                
                // 查询
                long queryStart = System.nanoTime();
                CompleteMHTVO vo = mht.rangeQuery(zStart, zEnd);
                long queryTime = System.nanoTime() - queryStart;
                totalQueryTime += queryTime;
                
                // 验证
                long verifyStart = System.nanoTime();
                boolean isValid = mht.verify(vo);
                long verifyTime = System.nanoTime() - verifyStart;
                totalVerifyTime += verifyTime;
                
                if (!isValid) {
                    // 只显示第一个失败
                    if (queryCount == 1) {
                        System.out.println("\n警告: 第一个查询验证失败!");
                        System.out.println("  查询范围: [" + zStart + ", " + zEnd + "]");
                        System.out.println("  结果数: " + vo.getResultCount());
                        System.out.println("  叶子信息数: " + vo.getLeafInfos().size());
                        System.out.println("  兄弟哈希数: " + vo.getSiblingHashes().size());
                    } else if (queryCount <= 3) {
                        System.out.println("查询" + queryCount + "验证失败");
                    }
                }
                
                // 统计
                totalVOSize += vo.getVOSize();
                totalResults += vo.getResultCount();
                totalLeafInfos += vo.getLeafInfos().size();
                totalSiblings += vo.getSiblingHashes().size();
                totalBoundaries += vo.getBoundaryNodes().size();
            }
            
            // 输出结果 (与PVL相同的格式)
            System.out.println("\n【查询性能】");
            System.out.println("  平均查询时间: " + String.format("%.3f ms", 
                totalQueryTime / queryLen / 1_000_000.0));
            System.out.println("  平均验证时间: " + String.format("%.3f ms", 
                totalVerifyTime / queryLen / 1_000_000.0));
            System.out.println("  平均总时间: " + String.format("%.3f ms", 
                (totalQueryTime + totalVerifyTime) / queryLen / 1_000_000.0));
            System.out.println("  验证开销: " + String.format("%.1f%%", 
                (double)totalVerifyTime / (totalQueryTime + totalVerifyTime) * 100));
            
            System.out.println("\n【统计信息】");
            System.out.println("  平均VO大小: " + String.format("%.2f KB", 
                totalVOSize / queryLen / 1024.0));
            System.out.println("  平均结果数: " + (totalResults / queryLen));
            System.out.println("  平均叶子信息数: " + (totalLeafInfos / queryLen));
            System.out.println("  平均兄弟哈希数: " + (totalSiblings / queryLen));
            System.out.println("  平均边界节点数: " + (totalBoundaries / queryLen));
            System.out.println();
        }
    }
    
    /**
     * 生成随机测试数据
     */
    private static List<Point2D> generateRandomData(int count) {
        List<Point2D> points = new ArrayList<>();
        Random random = new Random(42);
        for (int i = 0; i < count; i++) {
            long x = random.nextInt(100000);
            long y = random.nextInt(100000);
            points.add(new Point2D(x, y));
        }
        // 按Z值排序
        points.sort(Comparator.comparingLong(p -> p.zValue));
        return points;
    }
}

