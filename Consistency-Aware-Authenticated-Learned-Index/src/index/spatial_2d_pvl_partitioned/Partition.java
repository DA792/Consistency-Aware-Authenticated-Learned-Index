package index.spatial_2d_pvl_partitioned;

import index.PVL_tree_index.PVLTree;
import index.PVL_tree_index.PVL_Res;
import utils.Point2D;
import java.util.*;

/**
 * 分区类
 * 每个分区包含一个独立的PVL树和Z值到点的映射
 */
public class Partition {
    // 元数据
    private final int partitionId;
    private final long zMin;
    private final long zMax;
    private final int pointCount;
    
    // 索引结构
    private final PVLTree pvlTree;
    private final Map<Long, Point2D> zToPoint;
    
    /**
     * 构造函数
     * @param id 分区ID
     * @param points 该分区的点列表 (必须已按Z值排序)
     * @param errorBound PVL树的误差界限
     */
    public Partition(int id, List<Point2D> points, int errorBound) {
        this.partitionId = id;
        this.pointCount = points.size();
        
        if (points.isEmpty()) {
            throw new IllegalArgumentException("分区不能为空");
        }
        
        // 计算Z值范围 (points已排序)
        this.zMin = points.get(0).zValue;
        this.zMax = points.get(points.size() - 1).zValue;
        
        // 构建索引
        this.zToPoint = new HashMap<>(pointCount);
        long[] zValues = new long[pointCount];
        
        for (int i = 0; i < pointCount; i++) {
            Point2D point = points.get(i);
            zValues[i] = point.zValue;
            zToPoint.put(point.zValue, point);
        }
        
        // 构建PVL树
        this.pvlTree = new PVLTree(zValues, errorBound);
    }
    
    /**
     * 范围查询
     * @param zStart Z值起始
     * @param zEnd Z值结束
     * @return PVL树查询结果 (包含VO)
     */
    public PVL_Res rangeQuery(long zStart, long zEnd) {
        return pvlTree.rangeQuery(zStart, zEnd);
    }
    
    /**
     * 验证查询结果
     * @param zStart Z值起始
     * @param zEnd Z值结束
     * @param result 查询结果
     * @return 验证是否通过
     */
    public boolean verify(long zStart, long zEnd, PVL_Res result) {
        return pvlTree.verify(zStart, zEnd, result);
    }
    
    /**
     * 根据Z值获取点
     */
    public Point2D getPoint(long zValue) {
        return zToPoint.get(zValue);
    }
    
    // Getters
    public int getPartitionId() {
        return partitionId;
    }
    
    public long getZMin() {
        return zMin;
    }
    
    public long getZMax() {
        return zMax;
    }
    
    public int getPointCount() {
        return pointCount;
    }
    
    public PVLTree getPvlTree() {
        return pvlTree;
    }
    
    @Override
    public String toString() {
        return String.format("Partition[id=%d, points=%d, zRange=[%d, %d]]",
                           partitionId, pointCount, zMin, zMax);
    }
}

