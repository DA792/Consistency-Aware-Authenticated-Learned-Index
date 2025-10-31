package index.baseline;

import utils.*;
import java.util.*;

/**
 * 2D空间Merkle Hash Tree
 * 
 * 功能:
 * 1. 将2D矩形查询转换为1D Z-order范围查询
 * 2. 使用MHT进行认证查询
 * 3. 提供与Spatial2DPVLTree相同的接口,便于对比
 * 
 * 对比PVL树:
 * - MHT: 传统哈希树,无学习模型
 * - PVL: 学习索引,使用线性模型预测
 */
public class Spatial2DMHT {
    private MerkleHashTree mht;
    private Map<Long, Point2D> zToPoint;  // Z值到点的映射
    private int leafSize;
    
    // 性能统计
    private long buildTime;
    private long lastQueryTime;
    private long lastVerifyTime;
    
    /**
     * 构造函数
     * @param leafSize MHT叶子节点大小
     */
    public Spatial2DMHT(int leafSize) {
        this.leafSize = leafSize;
        this.zToPoint = new HashMap<>();
    }
    
    /**
     * 构建索引
     * @param points 2D数据点
     */
    public void build(List<Point2D> points) {
        long startTime = System.nanoTime();
        
        // 1. 计算Z值并排序
        List<Point2D> sortedPoints = new ArrayList<>();
        for (Point2D p : points) {
            if (p.zValue == 0) {
                // 重新创建带Z值的Point2D
                sortedPoints.add(new Point2D(p.x, p.y, ZOrderCurve.encode(p.x, p.y)));
            } else {
                sortedPoints.add(p);
            }
        }
        sortedPoints.sort(Comparator.comparingLong(p -> p.zValue));
        
        // 2. 构建Z值映射
        zToPoint.clear();
        for (Point2D p : sortedPoints) {
            zToPoint.put(p.zValue, p);
        }
        
        // 3. 构建MHT
        mht = new MerkleHashTree(leafSize);
        mht.build(sortedPoints);
        
        buildTime = System.nanoTime() - startTime;
    }
    
    /**
     * 2D矩形范围查询
     * @param rect 查询矩形
     * @return 查询结果
     */
    public Spatial2DMHTResult rectangleQuery(Rectangle2D rect) {
        long queryStartTime = System.nanoTime();
        
        // 1. Z-order分解
        Point2D qStart = new Point2D(rect.minX, rect.minY);
        Point2D qEnd = new Point2D(rect.maxX, rect.maxY);
        List<ZOrderDecomposition.ZInterval> zIntervals = ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        
        // 2. 对每个Z区间查询MHT
        List<Point2D> allCandidates = new ArrayList<>();
        List<MHTQueryResult> allMHTResults = new ArrayList<>();
        
        for (ZOrderDecomposition.ZInterval interval : zIntervals) {
            MHTQueryResult mhtResult = mht.rangeQuery(interval.start, interval.end);
            allMHTResults.add(mhtResult);
            allCandidates.addAll(mhtResult.getResults());
        }
        
        lastQueryTime = System.nanoTime() - queryStartTime;
        
        // 3. 空间过滤
        List<Point2D> filteredResults = new ArrayList<>();
        for (Point2D p : allCandidates) {
            if (rect.contains(p)) {
                filteredResults.add(p);
            }
        }
        
        // 4. 构建结果
        Spatial2DMHTResult result = new Spatial2DMHTResult(
            rect, filteredResults, allMHTResults, zIntervals
        );
        result.setTotalCandidates(allCandidates.size());
        
        return result;
    }
    
    /**
     * 验证查询结果
     * @param result 查询结果
     * @return true=验证通过
     */
    public boolean verify(Spatial2DMHTResult result) {
        long verifyStartTime = System.nanoTime();
        
        boolean isValid = true;
        
        // 验证每个MHT查询结果
        for (MHTQueryResult mhtResult : result.getMHTResults()) {
            if (!mht.verify(mhtResult)) {
                isValid = false;
                break;
            }
        }
        
        // 验证空间过滤的正确性
        if (isValid) {
            for (Point2D p : result.getFilteredPoints()) {
                if (!result.getQueryRect().contains(p)) {
                    isValid = false;
                    break;
                }
            }
        }
        
        lastVerifyTime = System.nanoTime() - verifyStartTime;
        
        return isValid;
    }
    
    /**
     * 获取索引统计信息
     */
    public String getIndexStats() {
        return String.format(
            "=== Spatial2DMHT 索引信息 ===\n" +
            "总点数: %d\n" +
            "Z-order映射表大小: %d 个条目\n" +
            "MHT树高度: %d\n" +
            "MHT叶子大小: %d\n" +
            "根哈希: %s\n" +
            "构建时间: %.4f ms",
            mht.getTotalPoints(),
            zToPoint.size(),
            mht.getTreeHeight(),
            leafSize,
            mht.getRootHashHex(),
            buildTime / 1_000_000.0
        );
    }
    
    // ==================== Getters ====================
    
    public long getBuildTime() {
        return buildTime;
    }
    
    public long getLastQueryTime() {
        return lastQueryTime;
    }
    
    public long getLastVerifyTime() {
        return lastVerifyTime;
    }
    
    public MerkleHashTree getMHT() {
        return mht;
    }
    
    public int getTreeHeight() {
        return mht.getTreeHeight();
    }
    
    /**
     * 2D MHT查询结果
     */
    public static class Spatial2DMHTResult {
        private Rectangle2D queryRect;
        private List<Point2D> filteredPoints;
        private List<MHTQueryResult> mhtResults;
        private List<ZOrderDecomposition.ZInterval> zIntervals;
        private int totalCandidates;
        
        public Spatial2DMHTResult(Rectangle2D queryRect, 
                                  List<Point2D> filteredPoints,
                                  List<MHTQueryResult> mhtResults,
                                  List<ZOrderDecomposition.ZInterval> zIntervals) {
            this.queryRect = queryRect;
            this.filteredPoints = filteredPoints;
            this.mhtResults = mhtResults;
            this.zIntervals = zIntervals;
            this.totalCandidates = 0;
        }
        
        public void setTotalCandidates(int count) {
            this.totalCandidates = count;
        }
        
        public Rectangle2D getQueryRect() {
            return queryRect;
        }
        
        public List<Point2D> getFilteredPoints() {
            return filteredPoints;
        }
        
        public List<MHTQueryResult> getMHTResults() {
            return mhtResults;
        }
        
        public List<ZOrderDecomposition.ZInterval> getZIntervals() {
            return zIntervals;
        }
        
        public int getResultCount() {
            return filteredPoints.size();
        }
        
        public int getCandidateCount() {
            return totalCandidates;
        }
        
        public int getFalsePositiveCount() {
            return totalCandidates - filteredPoints.size();
        }
        
        public int getZIntervalCount() {
            return zIntervals.size();
        }
        
        /**
         * 计算总VO大小
         */
        public long getTotalVOSize() {
            long totalSize = 0;
            for (MHTQueryResult mhtResult : mhtResults) {
                totalSize += mhtResult.getVOSize();
            }
            return totalSize;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Spatial2DMHTResult[results=%d, candidates=%d, falsePositives=%d, zIntervals=%d, voSize=%d bytes]",
                getResultCount(), getCandidateCount(), getFalsePositiveCount(), 
                getZIntervalCount(), getTotalVOSize()
            );
        }
    }
}

