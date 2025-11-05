package index.spatial_2d_pvl;

import index.PVL_tree_index.PVLTree;
import index.PVL_tree_index.PVL_Res;
import utils.*;
import java.util.*;

/**
 * 二维PVL树 - 客户端过滤架构
 * 
 * 架构特点:
 * - 服务端: 只返回候选点集合（含假阳性），生成验证对象
 * - 客户端: 自行过滤假阳性，获得最终结果
 * - 验证: 验证候选点集合的完整性和正确性
 * 
 * 优势:
 * ✅ 验证完整性: 可以验证服务端返回的候选点是否完整
 * ✅ 不需信任: 客户端控制过滤过程，无需信任服务端过滤
 * ✅ 性能分离: 查询时间和过滤时间分别测量
 */
public class Spatial2DPVLTree {
    private PVLTree pvlTree;
    private Map<Long, Point2D> zToPoint;
    private int errorBound;
    
    public Spatial2DPVLTree(List<Point2D> points, int errorBound) {
        this.errorBound = errorBound;
        buildIndex(points);
    }
    
    private void buildIndex(List<Point2D> points) {
        zToPoint = new HashMap<>();
        for (Point2D point : points) {
            zToPoint.put(point.zValue, point);
        }
        
        long[] zValues = points.stream()
                .mapToLong(p -> p.zValue)
                .sorted()
                .toArray();
        
        pvlTree = new PVLTree(zValues, errorBound);
    }
    
    public Spatial2DPVL_Res rectangleQuery(Rectangle2D queryRect) {
        Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
        Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
        List<ZOrderDecomposition.ZInterval> intervals = 
            ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        
        // 使用并行查询(每个区间独立,利用多核)
        return rectangleQueryParallel(queryRect, intervals);
    }
    
    /**
     * 并行查询实现 - 客户端过滤架构
     * 服务端只返回候选点，客户端负责过滤假阳性
     * 这样可以保证验证的完整性和可信性
     */
    private Spatial2DPVL_Res rectangleQueryParallel(Rectangle2D queryRect, 
                                                     List<ZOrderDecomposition.ZInterval> intervals) {
        // 并行处理每个Z区间(每个区间独立生成VO)
        List<Spatial2DPVLQueryResult> intervalResults = intervals.parallelStream()
            .map(interval -> {
                // 每个区间独立查询,生成自己的完整VO
                PVL_Res pvlResult = pvlTree.rangeQuery(interval.start, interval.end);
                
                // 🎯 关键改进：服务端不过滤，返回所有候选点
                List<Point2D> candidatePoints = new ArrayList<>();
                List<Long> zValues = pvlResult.getResults();  // 使用公开方法，无需反射！
                int totalCandidates = zValues.size();
                
                for (Long zValue : zValues) {
                    Point2D point = zToPoint.get(zValue);
                    if (point != null) {
                        candidatePoints.add(point);  // ← 不过滤，返回所有候选点（含假阳性）
                    }
                }
                
                return new Spatial2DPVLQueryResult(interval, pvlResult, candidatePoints, totalCandidates);
            })
            .collect(java.util.stream.Collectors.toList());
        
        // 合并所有候选点（含假阳性）
        List<Point2D> allCandidates = new ArrayList<>();
        for (Spatial2DPVLQueryResult result : intervalResults) {
            allCandidates.addAll(result.getCandidatePoints());
        }
        
        // 去重候选点（但不过滤假阳性）
        Set<Point2D> uniqueCandidates = new HashSet<>(allCandidates);
        return new Spatial2DPVL_Res(new ArrayList<>(uniqueCandidates), intervalResults, intervals);
    }

    
    public boolean verify(Rectangle2D queryRect, Spatial2DPVL_Res response) {
        // 优化1: 使用缓存的Z区间,避免重复计算
        List<ZOrderDecomposition.ZInterval> intervals;
        if (response.zIntervals != null) {
            intervals = response.zIntervals;
        } else {
            Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
            Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
            intervals = ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        }
        
        if (intervals.size() != response.intervalResults.size()) {
            return false;
        }
        
        // 🎯 关键改进：验证候选点的完整性，而不是过滤后的结果
        Set<Point2D> reconstructedCandidates = new HashSet<>();
        
        for (int i = 0; i < intervals.size(); i++) {
            ZOrderDecomposition.ZInterval interval = intervals.get(i);
            Spatial2DPVLQueryResult intervalResult = response.intervalResults.get(i);
            
            // 验证PVL树的查询结果
            boolean isValid = pvlTree.verify(interval.start, interval.end, intervalResult.pvlResult);
            if (!isValid) {
                return false;
            }
            
            // 重建候选点集合（不过滤假阳性）
            List<Long> zValues = intervalResult.pvlResult.getResults();  // 使用公开方法，无需反射！
            for (Long zValue : zValues) {
                Point2D point = zToPoint.get(zValue);
                if (point != null) {
                    reconstructedCandidates.add(point);  // ← 验证所有候选点，含假阳性
                }
            }
        }
        
        // 验证候选点集合的完整性
        Set<Point2D> claimedCandidates = new HashSet<>(response.results);
        return reconstructedCandidates.equals(claimedCandidates);
    }
    
    public void printIndexSize() {
        pvlTree.getIndexSize();
        System.out.println("Z-order映射表大小: " + zToPoint.size() + " 个条目");
    }
    
    
    public static void main(String[] args) {
        // 支持命令行参数指定误差界限
        int[] errorBounds;
        if (args.length > 0) {
            // 如果提供了参数,只测试指定的误差界限
            errorBounds = new int[]{Integer.parseInt(args[0])};
        } else {
            // 默认使用 err=128 (50万数据用适中误差界限)
            errorBounds = new int[]{128};
        }
        
        System.out.println("===== 二维PVL树误差界限对比测试 =====\n");
        
        // 1. 加载数据(只加载一次)
        System.out.println("1. 加载数据集 (50万点)");
        String dataPath = "Consistency-Aware-Authenticated-Learned-Index/src/data/uniform_500k.csv";
        int loadCount = 500000;
        List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);
        
        if (points.isEmpty()) {
            System.out.println("数据文件未找到,使用随机生成数据");
            points = generateRandomData(500000);
        }
        
        System.out.println("实际加载: " + points.size() + " 个数据点");
        
        DataLoader.DataStats stats = DataLoader.analyzeData(points);
        System.out.println("数据统计: " + points.size() + " 个点");
        System.out.println("X范围: [" + stats.minX + ", " + stats.maxX + "]");
        System.out.println("Y范围: [" + stats.minY + ", " + stats.maxY + "]\n");
        
        // 2. 测试每个误差界限
        for (int err : errorBounds) {
            System.out.println("========================================");
            System.out.println("测试误差界限: " + err);
            System.out.println("========================================\n");
            
            // 构建索引
            System.out.println("2. 构建二维PVL索引");
            long startTime = System.nanoTime();
            Spatial2DPVLTree tree = new Spatial2DPVLTree(points, err);
            long buildTime = System.nanoTime() - startTime;
            System.out.println("构建时间: " + buildTime / 1000000.0 + " ms");
            System.out.println("误差界限: ±" + err);
            tree.printIndexSize();
            System.out.println();
            
            // 3. 性能测试
            System.out.println("3. 查询性能测试 (并行查询)");
            double[] queryRange = new double[]{0.0001, 0.001, 0.01, 0.1};
            int queryLen = 500;  // 50万数据,适当增加查询次数
            
            System.out.println("查询次数: " + queryLen);
            System.out.println("查询选择性: " + Arrays.toString(queryRange));
            System.out.println();
            
            // 性能测试
            runPerformanceTest(tree, stats, queryRange, queryLen);
            
            System.out.println();
        }
        
        System.out.println("===== 测试完成 =====");
        if (errorBounds.length > 1) {
            System.out.println("\n【对比建议】");
            System.out.println("err=64:  树深度大,查询慢,但假阳性少");
            System.out.println("err=128: 平衡配置,适中性能 (当前默认)");
            System.out.println("err=256: 树深度小,查询快,但假阳性多");
            System.out.println("\n根据上述结果,选择查询时间+验证时间最优的配置");
        } else {
            System.out.println("\n【配置说明】");
            System.out.println("当前使用 err=" + errorBounds[0] + " 配置");
            System.out.println("如需对比测试,可运行: java -cp \"jars/*;bin\" index.spatial_2d_pvl.Spatial2DPVLTree 64");
        }
    }
    
    private static void runPerformanceTest(Spatial2DPVLTree tree, DataLoader.DataStats stats,
                                          double[] queryRange, int queryLen) {
        
        for (double selectivity : queryRange) {
            // 生成测试查询
            List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, selectivity, queryLen);
            
            long totalQueryTime = 0;
            long totalVerifyTime = 0;
            double totalVOSize = 0;
            int totalResults = 0;
            int totalIntervals = 0;
            int totalCandidates = 0;
            int totalFalsePositives = 0;
            
            // 执行查询
            long totalFilterTime = 0;
            int totalTruePositives = 0;
            
            for (Rectangle2D query : queries) {
                // 1. 查询候选点（服务端，不含过滤）
                long startTime = System.nanoTime();
                Spatial2DPVL_Res response = tree.rectangleQuery(query);
                long queryTime = System.nanoTime() - startTime;
                totalQueryTime += queryTime;
                
                // 2. 客户端过滤假阳性
                startTime = System.nanoTime();
                List<Point2D> filteredResults = new ArrayList<>();
                for (Point2D candidate : response.results) {
                    if (query.contains(candidate)) {
                        filteredResults.add(candidate);
                    }
                }
                long filterTime = System.nanoTime() - startTime;
                totalFilterTime += filterTime;
                
                // 3. 验证候选点完整性
                startTime = System.nanoTime();
                boolean isValid = tree.verify(query, response);
                totalVerifyTime += System.nanoTime() - startTime;
                
                if (!isValid) {
                    System.out.println("警告: 验证失败!");
                }
                
                // 统计
                totalVOSize += response.getTotalVOSize();
                totalResults += response.results.size();  // 候选点数量（含假阳性）
                totalTruePositives += filteredResults.size();  // 真阳性数量
                totalFalsePositives += (response.results.size() - filteredResults.size());  // 假阳性数量
                
                // 使用客户端过滤后的统计信息
                Spatial2DPVL_Res.QueryStats queryStats = response.getStatsWithFiltering(filteredResults.size());
                totalIntervals += queryStats.intervalCount;
                totalCandidates += queryStats.candidateCount;
            }
            
            // 输出结果
            System.out.println("===== 查询选择性: " + selectivity + " =====");
            System.out.println("【查询性能 - 客户端过滤架构】");
            System.out.println("  平均查询时间: " + (totalQueryTime / queryLen / 1000000.0) + " ms (服务端，返回候选点)");
            System.out.println("  平均过滤时间: " + (totalFilterTime / queryLen / 1000000.0) + " ms (客户端，过滤假阳性)");
            System.out.println("  平均验证时间: " + (totalVerifyTime / queryLen / 1000000.0) + " ms (验证候选点完整性)");
            System.out.println("  平均总时间: " + ((totalQueryTime + totalFilterTime + totalVerifyTime) / queryLen / 1000000.0) + " ms");
            System.out.println("  过滤开销: " + String.format("%.1f%%", 
                             (double)totalFilterTime / (totalQueryTime + totalFilterTime + totalVerifyTime) * 100));
            System.out.println("  验证开销: " + String.format("%.1f%%", 
                             (double)totalVerifyTime / (totalQueryTime + totalFilterTime + totalVerifyTime) * 100));
            System.out.println("【统计信息】");
            System.out.println("  平均VO大小: " + String.format("%.2f", totalVOSize / queryLen / 1024.0) + " KB");
            System.out.println("  平均候选数: " + (totalResults / queryLen) + " (含假阳性)");
            System.out.println("  平均真阳性: " + (totalTruePositives / queryLen) + " (过滤后结果)");
            System.out.println("  平均假阳性: " + (totalFalsePositives / queryLen));
            System.out.println("  假阳性率: " + String.format("%.2f%%", 
                             (totalResults > 0 ? (double)totalFalsePositives / totalResults * 100 : 0)));
            System.out.println("  平均Z区间数: " + (totalIntervals / queryLen));
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


