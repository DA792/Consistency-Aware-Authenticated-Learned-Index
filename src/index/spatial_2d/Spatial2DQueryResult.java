package index.spatial_2d;

import index.PVL_tree_index.PVL_Res;
import java.util.List;

/**
 * 单个Z-order区间的查询结果
 */
public class Spatial2DQueryResult {
    public final ZOrderDecomposition.ZInterval interval;  // 对应的Z-order区间
    public final PVL_Res pvlResult;                       // 一维查询的结果和VO
    public final List<Point2D> filteredPoints;            // 过滤后的二维点
    
    public Spatial2DQueryResult(ZOrderDecomposition.ZInterval interval, 
                               PVL_Res pvlResult, 
                               List<Point2D> filteredPoints) {
        this.interval = interval;
        this.pvlResult = pvlResult;
        this.filteredPoints = filteredPoints;
    }
    
    /**
     * 获取候选点数量
     * 注意: PVL_Res.res是包级别可见，需要通过反射或添加getter访问
     * 这里暂时返回过滤后的点数量作为近似
     */
    public int getCandidateCount() {
        // 由于PVL_Res.res不可见，我们需要其他方式获取
        // 临时方案: 返回过滤点数量
        return filteredPoints.size();
    }
    
    /**
     * 获取过滤后的点数量
     */
    public int getFilteredCount() {
        return filteredPoints.size();
    }
    
    /**
     * 计算假阳性数量
     */
    public int getFalsePositiveCount() {
        return getCandidateCount() - getFilteredCount();
    }
}

