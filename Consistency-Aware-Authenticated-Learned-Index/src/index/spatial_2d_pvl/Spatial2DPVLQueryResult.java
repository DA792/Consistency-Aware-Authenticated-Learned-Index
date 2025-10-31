package index.spatial_2d_pvl;

import index.PVL_tree_index.PVL_Res;
import utils.*;
import java.util.List;

/**
 * 单个Z-order区间的查询结果 - 客户端过滤架构
 * 
 * 架构说明:
 * - 服务端返回所有候选点（含假阳性）
 * - 客户端负责过滤假阳性
 * - 验证基于候选点的完整性
 */
public class Spatial2DPVLQueryResult {
    public final ZOrderDecomposition.ZInterval interval;
    public final PVL_Res pvlResult;
    public final List<Point2D> candidatePoints;  // 候选点（含假阳性）
    private final int totalCandidates;           // 候选点总数（应该等于candidatePoints.size()）
    
    public Spatial2DPVLQueryResult(ZOrderDecomposition.ZInterval interval, 
                                  PVL_Res pvlResult, 
                                  List<Point2D> candidatePoints,
                                  int totalCandidates) {
        this.interval = interval;
        this.pvlResult = pvlResult;
        this.candidatePoints = candidatePoints;
        this.totalCandidates = totalCandidates;
    }
    
    // 候选点数（含假阳性）
    public int getCandidateCount() {
        return candidatePoints.size();  // 直接返回实际候选点数量
    }
    
    // 获取候选点列表（含假阳性）
    public List<Point2D> getCandidatePoints() {
        return candidatePoints;
    }
    
    // 获取原始候选点总数（用于统计）
    public int getTotalCandidates() {
        return totalCandidates;
    }
}


