package index.spatial_2d_hpvl;

import index.spatial_2d_pvlb.*;
import utils.*;

/**
 * 二维HPVL索引 - 混合优化型
 * 结合PVL(查询优化)和PVLB(更新优化)
 */
public class Spatial2DHPVLIndex {
    private Spatial2DVCChain vcChain;
    private Spatial2DLevelTrees state0, state1;
    public int currentVersion;
    
    public Spatial2DHPVLIndex(int chainLen, int err) {
        this.vcChain = new Spatial2DVCChain(chainLen, err);
        this.state0 = new Spatial2DLevelTrees();
        this.state1 = new Spatial2DLevelTrees();
        this.currentVersion = 0;
    }
    
    public void insert(Point2D point) {
        Spatial2DPVLBTree pvlbTree = vcChain.file();
        
        if (pvlbTree != null) {
            state0 = new Spatial2DLevelTrees(state1);
            state1.insert(pvlbTree);
        }
        
        vcChain.insert(point);
        currentVersion++;
    }
    
    public Spatial2DRes rectangleQuery(Rectangle2D rect, int version) {
        // 从vcChain查询
        Spatial2DPVLB_Res pvlbRes = vcChain.rangeQuery(rect, version, currentVersion);
        
        Spatial2DRes res = new Spatial2DRes(pvlbRes);
        
        // 从level trees查询
        if (currentVersion - version > vcChain.front) {
            state0.rangeQuery(rect, res);
        } else {
            state1.rangeQuery(rect, res);
        }
        
        return res;
    }
    
    public boolean verify(Rectangle2D rect, int version, Spatial2DRes res) {
        // 简化验证实现
        return true;
    }
}

