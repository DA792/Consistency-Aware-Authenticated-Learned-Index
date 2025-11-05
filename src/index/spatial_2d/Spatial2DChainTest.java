package index.spatial_2d;

import java.util.*;

/**
 * 二维空间索引版本链测试
 * 对应PVLTreeChain的main方法
 */
public class Spatial2DChainTest {
    
    public static void main(String[] args) {
        long queryTime = 0, verifyTime = 0;
        long s, e;
        
        // 参数设置
        int len = 10000;           // 数据量
        int queryLen = 1000;       // 查询数量
        int err = 64;              // 误差边界
        int chainLen = 100;        // 版本链长度
        
        System.out.println("===== 二维空间索引版本链测试 =====");
        System.out.println("数据量: " + len);
        System.out.println("查询数量: " + queryLen);
        System.out.println("误差边界: " + err);
        System.out.println("版本链长度: " + chainLen);
        System.out.println();
        
        // 1. 生成或加载数据
        System.out.println("1. 准备数据...");
        List<Point2D> dataset;
        
        // 尝试从文件加载
        dataset = DataLoader.loadFromCSV("src/data/uniform_10k.csv", len);
        
        if (dataset.isEmpty()) {
            // 如果加载失败，生成随机数据
            System.out.println("   从文件加载失败，生成随机数据");
            dataset = generateRandomData(len, 0, 20000);
        }
        
        DataLoader.DataStats stats = DataLoader.analyzeData(dataset);
        System.out.println(stats);
        System.out.println();
        
        // 2. 构建版本链
        System.out.println("2. 构建版本链...");
        Spatial2DChain chain = new Spatial2DChain(chainLen, err);
        
        s = System.nanoTime();
        // 使用批量插入初始化
        chain.insertBatch(dataset);
        e = System.nanoTime();
        
        System.out.println("   构建时间: " + (e - s) / 1000000000.0 + " s");
        chain.getIndexSize();
        System.out.println();
        
        // 3. 生成查询矩形
        System.out.println("3. 生成测试查询...");
        double selectivity = 0.001;  // 0.1% 选择率
        List<Rectangle2D> queries = DataLoader.generateTestQueries(
            stats, selectivity, queryLen);
        System.out.println("   生成了 " + queries.size() + " 个查询");
        System.out.println();
        
        // 4. 执行查询和验证
        System.out.println("4. 执行查询和验证...");
        
        for (int i = 0; i < queryLen; i++) {
            // 随机选择一个版本进行查询（模拟历史查询）
            int searchVersion = chain.getCurrentVersion() - 
                              (int) (Math.random() * Math.min(chainLen, chain.getCurrentVersion()));
            
            // 也可以只查询最新版本
            searchVersion = chain.getCurrentVersion();
            
            Rectangle2D queryRect = queries.get(i);
            
            // 执行查询
            s = System.nanoTime();
            Spatial2DQueryResponse response = chain.rangeQuery(queryRect, searchVersion);
            e = System.nanoTime();
            queryTime += (e - s);
            
            if (i < 5) {  // 只打印前5个查询的详细信息
                System.out.println("   查询 " + (i+1) + ": " + (e - s) / 1000000.0 + " ms");
            }
            
            // 验证结果
            s = System.nanoTime();
            boolean isValid = chain.verify(queryRect, searchVersion, response);
            e = System.nanoTime();
            verifyTime += (e - s);
            
            if (!isValid) {
                System.err.println("   警告: 查询 " + (i+1) + " 验证失败!");
            }
        }
        
        System.out.println();
        
        // 5. 输出统计结果
        System.out.println("5. 性能统计");
        System.out.println("   平均查询时间: " + (queryTime / queryLen) / 1000000.0 + " ms");
        System.out.println("   平均验证时间: " + (verifyTime / queryLen) / 1000000.0 + " ms");
        System.out.println("   总查询时间: " + queryTime / 1000000000.0 + " s");
        System.out.println("   总验证时间: " + verifyTime / 1000000000.0 + " s");
        
        System.out.println();
        System.out.println("===== 测试完成 =====");
    }
    
    /**
     * 生成随机数据
     */
    private static List<Point2D> generateRandomData(int count, long minCoord, long maxCoord) {
        List<Point2D> points = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < count; i++) {
            long x = minCoord + (long) (random.nextDouble() * (maxCoord - minCoord));
            long y = minCoord + (long) (random.nextDouble() * (maxCoord - minCoord));
            points.add(new Point2D(x, y));
        }
        
        return points;
    }
}



