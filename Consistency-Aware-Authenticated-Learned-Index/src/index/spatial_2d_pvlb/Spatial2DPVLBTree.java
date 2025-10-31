package index.spatial_2d_pvlb;

import index.PVLB_tree_index.PVLBTree;
import index.PVLB_tree_index.PVLB_Res;
import utils.*;
import java.util.*;

/**
 * 二维PVLB树 - 更新优化型
 * 对应一维的PVLBTree
 */
public class Spatial2DPVLBTree {
    private PVLBTree pvlbTree;
    private Map<Long, Point2D> zToPoint;
    
    public Spatial2DPVLBTree(Point2D initialPoint) {
        this.zToPoint = new HashMap<>();
        zToPoint.put(initialPoint.zValue, initialPoint);
        pvlbTree = new PVLBTree(initialPoint.zValue);
    }
    
    public Spatial2DPVLBTree insert(Point2D point) {
        zToPoint.put(point.zValue, point);
        PVLBTree newTree = pvlbTree.insert(point.zValue);
        
        Spatial2DPVLBTree result = new Spatial2DPVLBTree(point);
        result.pvlbTree = newTree;
        result.zToPoint = new HashMap<>(this.zToPoint);
        result.zToPoint.put(point.zValue, point);
        
        return result;
    }
    
    public Spatial2DPVLB_Res rectangleQuery(Rectangle2D queryRect) {
        List<Point2D> results = new ArrayList<>();
        
        Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
        Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
        List<ZOrderDecomposition.ZInterval> intervals = 
            ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        
        List<PVLB_Res> pvlbResults = new ArrayList<>();
        
        for (ZOrderDecomposition.ZInterval interval : intervals) {
            PVLB_Res pvlbRes = pvlbTree.rangeQuery(interval.start, interval.end);
            pvlbResults.add(pvlbRes);
            
            List<Long> zValues = getResultList(pvlbRes);
            for (Long zValue : zValues) {
                Point2D point = zToPoint.get(zValue);
                if (point != null && queryRect.contains(point)) {
                    results.add(point);
                }
            }
        }
        
        return new Spatial2DPVLB_Res(results, pvlbResults);
    }
    
    public boolean verify(Rectangle2D queryRect, Spatial2DPVLB_Res response) {
        Point2D qStart = new Point2D(queryRect.minX, queryRect.minY);
        Point2D qEnd = new Point2D(queryRect.maxX, queryRect.maxY);
        List<ZOrderDecomposition.ZInterval> intervals = 
            ZOrderDecomposition.decomposeQuery(qStart, qEnd);
        
        if (intervals.size() != response.pvlbResults.size()) {
            return false;
        }
        
        for (int i = 0; i < intervals.size(); i++) {
            ZOrderDecomposition.ZInterval interval = intervals.get(i);
            PVLB_Res pvlbRes = response.pvlbResults.get(i);
            
            boolean isValid = pvlbTree.verify(interval.start, interval.end, pvlbRes);
            if (!isValid) {
                return false;
            }
        }
        
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private List<Long> getResultList(PVLB_Res pvlbResult) {
        try {
            java.lang.reflect.Field field = PVLB_Res.class.getDeclaredField("res");
            field.setAccessible(true);
            return (List<Long>) field.get(pvlbResult);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}



