package index.spatial_2d;

import java.util.*;

/**
 * 二维空间索引的版本链管理
 * 对应PVLTreeChain，提供版本控制和动态更新能力
 */
public class Spatial2DChain {
    
    static int err;
    Spatial2DIndex[] chain;          // 版本链：存储多个版本的二维索引
    int front, rear;                 // 循环队列的前后指针
    int currentVersion;              // 当前版本号
    
    // 缓存所有点的Z-order映射（用于版本间共享）
    private Map<Long, Point2D> globalZToPoint;
    
    /**
     * 构造函数
     * @param chainLen 版本链长度
     * @param err 误差边界
     */
    public Spatial2DChain(int chainLen, int err) {
        this.chain = new Spatial2DIndex[chainLen];
        this.currentVersion = 0;
        this.front = this.rear = 0;
        Spatial2DChain.err = err;
        this.globalZToPoint = new HashMap<>();
    }
    
    /**
     * 判断版本链是否为空
     */
    public boolean isNull() {
        return front == rear;
    }
    
    /**
     * 判断版本链是否已满
     */
    public boolean isFull() {
        return (rear + 1) % chain.length == front;
    }
    
    /**
     * 插入一个二维点（创建新版本）
     * @param point 要插入的二维点
     */
    public void insert(Point2D point) {
        // 更新全局映射
        globalZToPoint.put(point.zValue, point);
        
        if (isNull()) {
            // 第一个版本：创建包含单个点的索引
            List<Point2D> points = new ArrayList<>();
            points.add(point);
            chain[rear] = createIndexFromPoints(points);
        } else {
            // 后续版本：基于前一个版本更新
            Spatial2DIndex prevIndex = chain[(rear + chain.length - 1) % chain.length];
            chain[rear] = updateIndex(prevIndex, point);
            
            // 如果链满了，移除最老的版本
            if (isFull()) {
                front = (front + 1) % chain.length;
            }
        }
        
        rear = (rear + 1) % chain.length;
        currentVersion++;
    }
    
    /**
     * 批量插入（用于初始化）
     * @param points 点集合
     */
    public void insertBatch(List<Point2D> points) {
        for (Point2D point : points) {
            globalZToPoint.put(point.zValue, point);
        }
        
        if (isNull()) {
            chain[rear] = createIndexFromPoints(points);
            rear = (rear + 1) % chain.length;
            currentVersion = points.size();
        } else {
            for (Point2D point : points) {
                insert(point);
            }
        }
    }
    
    /**
     * 从点集合创建索引
     */
    private Spatial2DIndex createIndexFromPoints(List<Point2D> points) {
        return new Spatial2DIndex(points, err);
    }
    
    /**
     * 更新索引（添加新点）
     * 简化实现：重新构建整个索引
     */
    private Spatial2DIndex updateIndex(Spatial2DIndex prevIndex, Point2D newPoint) {
        // 获取前一个版本的所有点
        List<Point2D> allPoints = new ArrayList<>(globalZToPoint.values());
        
        // 创建新索引
        return createIndexFromPoints(allPoints);
    }
    
    /**
     * 获取指定版本的索引
     * @param version 版本号
     * @return 该版本的二维索引
     */
    public Spatial2DIndex getVersionIndex(int version) {
        int index = (rear - (currentVersion - version) + chain.length) % chain.length;
        return chain[index];
    }
    
    /**
     * 版本化的矩形范围查询
     * @param queryRect 查询矩形
     * @param version 查询的版本号
     * @return 查询响应
     */
    public Spatial2DQueryResponse rangeQuery(Rectangle2D queryRect, int version) {
        Spatial2DIndex versionIndex = getVersionIndex(version);
        if (versionIndex == null) {
            return new Spatial2DQueryResponse(new ArrayList<>(), new ArrayList<>());
        }
        return versionIndex.rectangleQuery(queryRect);
    }
    
    /**
     * 查询最新版本
     */
    public Spatial2DQueryResponse rangeQuery(Rectangle2D queryRect) {
        return rangeQuery(queryRect, currentVersion);
    }
    
    /**
     * 验证查询结果
     * @param queryRect 查询矩形
     * @param version 版本号
     * @param response 查询响应
     * @return 验证是否通过
     */
    public boolean verify(Rectangle2D queryRect, int version, Spatial2DQueryResponse response) {
        Spatial2DIndex versionIndex = getVersionIndex(version);
        if (versionIndex == null) {
            return false;
        }
        return versionIndex.verify(queryRect, response);
    }
    
    /**
     * 获取索引大小
     */
    public void getIndexSize() {
        int validVersions = 0;
        
        for (int i = 0; i < chain.length; i++) {
            if (chain[i] != null) {
                validVersions++;
                // 这里简化处理，实际应该计算每个索引的大小
            }
        }
        
        System.out.println("二维索引链统计:");
        System.out.println("  链长度: " + chain.length);
        System.out.println("  有效版本数: " + validVersions);
        System.out.println("  当前版本: " + currentVersion);
        System.out.println("  全局点映射: " + globalZToPoint.size() + " 个点");
    }
    
    /**
     * 获取当前版本号
     */
    public int getCurrentVersion() {
        return currentVersion;
    }
}

