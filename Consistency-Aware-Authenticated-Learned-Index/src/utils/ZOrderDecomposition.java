package utils;

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
        public final Rectangle2D region;
        
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
    
    public static List<ZInterval> decomposeQuery(Point2D qStart, Point2D qEnd) {
        List<ZInterval> intervals = new ArrayList<>();
        Rectangle2D queryRegion = new Rectangle2D(qStart.x, qStart.y, qEnd.x, qEnd.y);
        long zStart = qStart.zValue;
        long zEnd = qEnd.zValue;
        decomposeInterval(zStart, zEnd, queryRegion, 0, intervals);
        
        List<ZInterval> merged = mergeAdjacentIntervals(intervals);
        
        // 限制区间数量，防止过度分解影响性能
      
        return merged;
    }
    
    private static void decomposeInterval(long zStart, long zEnd, Rectangle2D queryRegion, 
                                        int level, List<ZInterval> intervals) {
        if (level > 20 || zStart > zEnd) return;
        
        Rectangle2D zRegion = getZIntervalSpatialRegion(zStart, zEnd, level);
        
        if (isCompletelyInside(zRegion, queryRegion)) {
            intervals.add(new ZInterval(zStart, zEnd, zRegion));
            return;
        }
        
        if (!intersects(zRegion, queryRegion)) return;
        
        // 优化VO大小: 减少区间数量，控制VO生成开销
        // 目标: 区间数量5-20，VO大小<50KB
        if (level >= 5 || (zEnd - zStart) <= 1) {
            intervals.add(new ZInterval(zStart, zEnd, zRegion));
            return;
        }
        
        List<ZInterval> subIntervals = splitByZCurveBranches(zStart, zEnd, level);
        for (ZInterval subInterval : subIntervals) {
            decomposeInterval(subInterval.start, subInterval.end, queryRegion, level + 1, intervals);
        }
    }
    
    private static Rectangle2D getZIntervalSpatialRegion(long zStart, long zEnd, int level) {
        Point2D startPoint = ZOrderCurve.decode(zStart);
        Point2D endPoint = ZOrderCurve.decode(zEnd);
        
        long minX = Math.min(startPoint.x, endPoint.x);
        long maxX = Math.max(startPoint.x, endPoint.x);
        long minY = Math.min(startPoint.y, endPoint.y);
        long maxY = Math.max(startPoint.y, endPoint.y);
        
        // 适度扩展区域，平衡精度和区间数量
        long expansion = Math.max(1, 1L << Math.max(0, 7 - level));
        
        return new Rectangle2D(
            Math.max(0, minX - expansion),
            Math.max(0, minY - expansion),
            maxX + expansion,
            maxY + expansion
        );
    }
    
    private static List<ZInterval> splitByZCurveBranches(long zStart, long zEnd, int level) {
        List<ZInterval> subIntervals = new ArrayList<>();
        int bitLevel = Math.max(0, 14 - level);
        
        if (bitLevel == 0) {
            subIntervals.add(new ZInterval(zStart, zEnd));
            return subIntervals;
        }
        
        long mask = (1L << (bitLevel * 2));
        long currentZ = zStart;
        
        while (currentZ <= zEnd) {
            long nextBoundary = ((currentZ / mask) + 1) * mask;
            long intervalEnd = Math.min(nextBoundary - 1, zEnd);
            
            if (currentZ <= intervalEnd) {
                subIntervals.add(new ZInterval(currentZ, intervalEnd));
            }
            
            currentZ = nextBoundary;
        }
        
        if (subIntervals.isEmpty() && zStart < zEnd) {
            long mid = zStart + (zEnd - zStart) / 2;
            subIntervals.add(new ZInterval(zStart, mid));
            subIntervals.add(new ZInterval(mid + 1, zEnd));
        }
        
        return subIntervals;
    }
    
    private static List<ZInterval> mergeAdjacentIntervals(List<ZInterval> intervals) {
        if (intervals.isEmpty()) return intervals;
        
        intervals.sort(Comparator.comparingLong(i -> i.start));
        
        List<ZInterval> merged = new ArrayList<>();
        ZInterval current = intervals.get(0);
        
        for (int i = 1; i < intervals.size(); i++) {
            ZInterval next = intervals.get(i);
            
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



