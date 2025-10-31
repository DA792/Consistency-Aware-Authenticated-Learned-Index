package index.spatial_2d_pvl_partitioned;

import index.PVL_tree_index.PVL_Res;
import index.spatial_2d_pvl.Spatial2DPVL_Res;
import index.spatial_2d_pvl.Spatial2DPVLQueryResult;
import utils.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分区版二维PVL树
 * 使用Z-order Clustering分区策略提升性能
 * 
 * 核心特点:
 * - 按Z值顺序切分数据为多个分区
 * - 每个分区独立的PVL树(更浅的树)
 * - 查询和验证可并行处理
 * - 验证方法完全不变
 */
public class Spatial2DPVLTreePartitioned {
    private final List<Partition> partitions;
    private final List<PartitionMeta> partitionMeta;
    private final int errorBound;
    private final int partitionCount;
    
    /**
     * 构造函数
     * @param points 2D点列表
     * @param errorBound PVL树误差界限
     * @param partitionCount 分区数量
     */
    public Spatial2DPVLTreePartitioned(List<Point2D> points, int errorBound, int partitionCount) {
        this.errorBound = errorBound;
        this.partitionCount = partitionCount;
        this.partitions = new ArrayList<>(partitionCount);
        this.partitionMeta = new ArrayList<>(partitionCount);
        
        buildPartitionedIndex(points);
    }
    
    /**
     * 自动选择分区数的构造函数
     */
    public Spatial2DPVLTreePartitioned(List<Point2D> points, int errorBound) {
        this(points, errorBound, calculateOptimalPartitions(points.size()));
    }
    
    /**
     * 构建分区索引
     */
    private void buildPartitionedIndex(List<Point2D> points) {
        if (points.isEmpty()) {
            throw new IllegalArgumentException("数据点列表不能为空");
        }
        
        System.out.println("开始构建分区索引...");
        System.out.println("  数据量: " + points.size());
        System.out.println("  分区数: " + partitionCount);
        System.out.println("  误差界限: " + errorBound);
        
        long startTime = System.nanoTime();
        
        // 1. 按Z值排序
        List<Point2D> sortedPoints = new ArrayList<>(points);
        sortedPoints.sort(Comparator.comparingLong(p -> p.zValue));
        System.out.println("  √ Z值排序完成");
        
        // 2. 计算分区大小
        int partitionSize = points.size() / partitionCount;
        int remainder = points.size() % partitionCount;
        
        // 3. 切分并构建分区
        int currentIndex = 0;
        for (int i = 0; i < partitionCount; i++) {
            // 处理余数,让前几个分区多1个点
            int size = partitionSize + (i < remainder ? 1 : 0);
            int endIndex = Math.min(currentIndex + size, points.size());
            
            if (currentIndex >= points.size()) {
                break;
            }
            
            // 提取该分区的点
            List<Point2D> partitionPoints = sortedPoints.subList(currentIndex, endIndex);
            
            // 构建分区
            Partition partition = new Partition(i, partitionPoints, errorBound);
            partitions.add(partition);
            
            // 添加元数据
            partitionMeta.add(new PartitionMeta(i, partition.getZMin(), partition.getZMax()));
            
            System.out.println(String.format("  √ 分区%d构建完成: %d点, Z范围[%d, %d]", 
                                            i, partition.getPointCount(), 
                                            partition.getZMin(), partition.getZMax()));
            
            currentIndex = endIndex;
        }
        
        long buildTime = System.nanoTime() - startTime;
        System.out.println(String.format("分区索引构建完成,耗时: %.2f ms\n", buildTime / 1000000.0));
    }
    
    /**
     * 2D矩形范围查询
     */
    public Spatial2DPVL_Res rectangleQuery(Rectangle2D queryRect) {
        // 1. Z-order分解
        Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
        Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
        List<ZOrderDecomposition.ZInterval> intervals = 
            ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        
        // 2. 映射Z区间到分区
        Map<Integer, List<ZOrderDecomposition.ZInterval>> partitionQueries = new HashMap<>();
        
        for (ZOrderDecomposition.ZInterval interval : intervals) {
            List<Integer> relevantPartitions = findRelevantPartitions(interval);
            
            for (int partitionId : relevantPartitions) {
                Partition partition = partitions.get(partitionId);
                
                // 裁剪区间到分区范围
                long clippedStart = Math.max(interval.start, partition.getZMin());
                long clippedEnd = Math.min(interval.end, partition.getZMax());
                
                ZOrderDecomposition.ZInterval clippedInterval = 
                    new ZOrderDecomposition.ZInterval(clippedStart, clippedEnd);
                
                partitionQueries.computeIfAbsent(partitionId, id -> new ArrayList<>())
                               .add(clippedInterval);
            }
        }
        
        // 3. 并行查询各分区
        List<Spatial2DPVLQueryResult> allResults = partitionQueries.entrySet()
            .parallelStream()
            .flatMap(entry -> {
                int partitionId = entry.getKey();
                List<ZOrderDecomposition.ZInterval> partIntervals = entry.getValue();
                return queryPartition(partitionId, partIntervals, queryRect).stream();
            })
            .collect(Collectors.toList());
        
        // 4. 合并结果
        Set<Point2D> uniqueResults = new HashSet<>();
        for (Spatial2DPVLQueryResult result : allResults) {
            uniqueResults.addAll(result.filteredPoints);
        }
        
        return new Spatial2DPVL_Res(new ArrayList<>(uniqueResults), allResults, intervals);
    }
    
    /**
     * 查询单个分区
     */
    private List<Spatial2DPVLQueryResult> queryPartition(
            int partitionId, 
            List<ZOrderDecomposition.ZInterval> intervals,
            Rectangle2D queryRect) {
        
        Partition partition = partitions.get(partitionId);
        List<Spatial2DPVLQueryResult> results = new ArrayList<>();
        
        for (ZOrderDecomposition.ZInterval interval : intervals) {
            // PVL树查询
            PVL_Res pvlResult = partition.rangeQuery(interval.start, interval.end);
            
            // 获取候选点
            List<Long> candidates = getResultList(pvlResult);
            int totalCandidates = candidates.size();
            
            // 空间过滤
            List<Point2D> filteredPoints = new ArrayList<>();
            for (Long zValue : candidates) {
                Point2D point = partition.getPoint(zValue);
                if (point != null && queryRect.contains(point)) {
                    filteredPoints.add(point);
                }
            }
            
            results.add(new Spatial2DPVLQueryResult(
                interval, pvlResult, filteredPoints, totalCandidates
            ));
        }
        
        return results;
    }
    
    /**
     * 验证查询结果
     */
    public boolean verify(Rectangle2D queryRect, Spatial2DPVL_Res response) {
        // 1. 使用缓存的Z区间
        List<ZOrderDecomposition.ZInterval> intervals = response.zIntervals;
        if (intervals == null) {
            Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
            Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
            intervals = ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        }
        
        // 2. 重建结果集
        Set<Point2D> reconstructedResults = new HashSet<>();
        
        // 3. 验证每个分区的结果
        for (Spatial2DPVLQueryResult intervalResult : response.intervalResults) {
            // 找到对应的分区 (通过Z值范围)
            ZOrderDecomposition.ZInterval interval = intervalResult.interval;
            List<Integer> relevantPartitions = findRelevantPartitions(interval);
            
            for (int partitionId : relevantPartitions) {
                Partition partition = partitions.get(partitionId);
                
                // 裁剪区间
                long clippedStart = Math.max(interval.start, partition.getZMin());
                long clippedEnd = Math.min(interval.end, partition.getZMax());
                
                // 验证PVL树VO
                boolean isValid = partition.verify(clippedStart, clippedEnd, intervalResult.pvlResult);
                if (!isValid) {
                    return false;
                }
                
                // 重建结果
                List<Long> zValues = getResultList(intervalResult.pvlResult);
                for (Long zValue : zValues) {
                    Point2D point = partition.getPoint(zValue);
                    if (point != null && queryRect.contains(point)) {
                        reconstructedResults.add(point);
                    }
                }
            }
        }
        
        // 4. 比较结果集
        Set<Point2D> claimedResults = new HashSet<>(response.results);
        return reconstructedResults.equals(claimedResults);
    }
    
    /**
     * 找到与Z区间重叠的分区
     */
    private List<Integer> findRelevantPartitions(ZOrderDecomposition.ZInterval interval) {
        List<Integer> relevant = new ArrayList<>();
        
        for (PartitionMeta meta : partitionMeta) {
            if (meta.overlaps(interval.start, interval.end)) {
                relevant.add(meta.partitionId);
            }
        }
        
        return relevant;
    }
    
    /**
     * 从PVL_Res中提取结果列表
     */
    @SuppressWarnings("unchecked")
    private List<Long> getResultList(PVL_Res pvlResult) {
        try {
            java.lang.reflect.Field field = PVL_Res.class.getDeclaredField("res");
            field.setAccessible(true);
            return (List<Long>) field.get(pvlResult);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    
    /**
     * 计算最优分区数
     */
    private static int calculateOptimalPartitions(int dataSize) {
        if (dataSize < 100_000) return 1;
        if (dataSize < 200_000) return 4;
        if (dataSize < 500_000) return 8;
        if (dataSize < 2_000_000) return 16;
        return 32;
    }
    
    /**
     * 打印索引信息
     */
    public void printIndexSize() {
        System.out.println("分区索引信息:");
        System.out.println("  总分区数: " + partitions.size());
        System.out.println("  误差界限: " + errorBound);
        
        int totalPoints = 0;
        for (Partition partition : partitions) {
            totalPoints += partition.getPointCount();
            partition.getPvlTree().getIndexSize();
        }
        System.out.println("  总点数: " + totalPoints);
    }
    
    // Getters
    public int getPartitionCount() {
        return partitionCount;
    }
    
    public List<Partition> getPartitions() {
        return Collections.unmodifiableList(partitions);
    }
}

