package index.spatial_2d;

import java.util.*;

/**
 * Z-order区间分解算法
 * 实现精确的查询区域分割，减少假阳性
 */
public class ZOrderDecomposition {
    
    /**
     * Z值区间类
     */
    public static class ZInterval {
        public final long start;
        public final long end;
        public final Rectangle2D region; // 对应的空间区域（可选）
        
        public ZInterval(long start, long end, Rectangle2D region) {
            this.start = start;
            this.end = end;
            this.region = region;
        }
        
        public ZInterval(long start, long end) {
            this(start, end, null);
        }
        
        @Override
        public String toString() {
            return String.format("[%d, %d]", start, end);
        }
    }
    
    /**
     * 精确的查询区域分割算法
     * @param qStart 查询区域下界点
     * @param qEnd 查询区域上界点
     * @return 分割后的连续Z值区间列表
     */
    public static List<ZInterval> decomposeQuery(Point2D qStart, Point2D qEnd) {
        List<ZInterval> intervals = new ArrayList<>();
        
        // 创建查询区域
        Rectangle2D queryRegion = new Rectangle2D(qStart.x, qStart.y, qEnd.x, qEnd.y);
        
        // 计算初始Z值区间
        long zStart = qStart.zValue;
        long zEnd = qEnd.zValue;
        
        // 递归分割初始区间
        decomposeInterval(zStart, zEnd, queryRegion, 0, intervals);
        
        // 合并相邻的连续区间
        return mergeAdjacentIntervals(intervals);
    }
    
    /**
     * 递归分割Z值区间
     */
    private static void decomposeInterval(long zStart, long zEnd, Rectangle2D queryRegion, 
                                        int level, List<ZInterval> intervals) {
        
        // 递归终止条件
        if (level > 20 || zStart > zEnd) {
            return;
        }
        
        // 检查当前Z值区间的空间覆盖
        Rectangle2D zRegion = getZIntervalSpatialRegion(zStart, zEnd, level);
        
        // 如果完全在查询区域内，直接添加
        if (isCompletelyInside(zRegion, queryRegion)) {
            intervals.add(new ZInterval(zStart, zEnd, zRegion));
            return;
        }
        
        // 如果完全在查询区域外，跳过
        if (!intersects(zRegion, queryRegion)) {
            return;
        }
        
        // 如果到达最小粒度，添加区间（可能有少量假阳性）
        if (level >= 15 || (zEnd - zStart) <= 1) {
            intervals.add(new ZInterval(zStart, zEnd, zRegion));
            return;
        }
        
        // 按Z曲线分支分割区间
        List<ZInterval> subIntervals = splitByZCurveBranches(zStart, zEnd, level);
        
        // 递归处理每个子区间
        for (ZInterval subInterval : subIntervals) {
            decomposeInterval(subInterval.start, subInterval.end, queryRegion, level + 1, intervals);
        }
    }
    
    /**
     * 获取Z值区间对应的空间区域
     */
    private static Rectangle2D getZIntervalSpatialRegion(long zStart, long zEnd, int level) {
        Point2D startPoint = ZOrderCurve.decode(zStart);
        Point2D endPoint = ZOrderCurve.decode(zEnd);
        
        long minX = Math.min(startPoint.x, endPoint.x);
        long maxX = Math.max(startPoint.x, endPoint.x);
        long minY = Math.min(startPoint.y, endPoint.y);
        long maxY = Math.max(startPoint.y, endPoint.y);
        
        // 根据层级扩展区域（考虑Z-order的空间填充特性）
        long expansion = Math.max(1, 1L << Math.max(0, 10 - level));
        
        return new Rectangle2D(
            Math.max(0, minX - expansion),
            Math.max(0, minY - expansion),
            maxX + expansion,
            maxY + expansion
        );
    }
    
    /**
     * 按Z曲线分支分割区间
     */
    private static List<ZInterval> splitByZCurveBranches(long zStart, long zEnd, int level) {
        List<ZInterval> subIntervals = new ArrayList<>();
        
        // 计算当前层级的分割位数
        int bitLevel = Math.max(0, 14 - level);
        
        if (bitLevel == 0) {
            // 无法继续分割，直接返回
            subIntervals.add(new ZInterval(zStart, zEnd));
            return subIntervals;
        }
        
        // 找到Z曲线的主要分支点
        long mask = (1L << (bitLevel * 2));
        long currentZ = zStart;
        
        while (currentZ <= zEnd) {
            // 计算下一个分支边界
            long nextBoundary = ((currentZ / mask) + 1) * mask;
            long intervalEnd = Math.min(nextBoundary - 1, zEnd);
            
            if (currentZ <= intervalEnd) {
                subIntervals.add(new ZInterval(currentZ, intervalEnd));
            }
            
            currentZ = nextBoundary;
        }
        
        // 如果没有分割成功，使用二分法
        if (subIntervals.isEmpty() && zStart < zEnd) {
            long mid = zStart + (zEnd - zStart) / 2;
            subIntervals.add(new ZInterval(zStart, mid));
            subIntervals.add(new ZInterval(mid + 1, zEnd));
        }
        
        return subIntervals;
    }
    
    /**
     * 合并相邻的连续区间
     */
    private static List<ZInterval> mergeAdjacentIntervals(List<ZInterval> intervals) {
        if (intervals.isEmpty()) return intervals;
        
        // 按起始Z值排序
        intervals.sort(Comparator.comparingLong(i -> i.start));
        
        List<ZInterval> merged = new ArrayList<>();
        ZInterval current = intervals.get(0);
        
        for (int i = 1; i < intervals.size(); i++) {
            ZInterval next = intervals.get(i);
            
            // 检查是否可以合并（连续）
            if (current.end + 1 == next.start) {
                current = new ZInterval(current.start, next.end, 
                    mergeRegions(current.region, next.region));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        
        return merged;
    }
    
    // 辅助方法
    private static boolean isCompletelyInside(Rectangle2D inner, Rectangle2D outer) {
        return inner.minX >= outer.minX && inner.maxX <= outer.maxX &&
               inner.minY >= outer.minY && inner.maxY <= outer.maxY;
    }
    
    private static boolean intersects(Rectangle2D rect1, Rectangle2D rect2) {
        return !(rect1.maxX < rect2.minX || rect1.minX > rect2.maxX ||
                 rect1.maxY < rect2.minY || rect1.minY > rect2.maxY);
    }
    
    private static Rectangle2D mergeRegions(Rectangle2D r1, Rectangle2D r2) {
        if (r1 == null) return r2;
        if (r2 == null) return r1;
        
        return new Rectangle2D(
            Math.min(r1.minX, r2.minX),
            Math.min(r1.minY, r2.minY),
            Math.max(r1.maxX, r2.maxX),
            Math.max(r1.maxY, r2.maxY)
        );
    }
}

