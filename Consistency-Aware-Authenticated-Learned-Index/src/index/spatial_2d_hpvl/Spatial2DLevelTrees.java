package index.spatial_2d_hpvl;

import index.spatial_2d_pvl.*;
import index.spatial_2d_pvlb.*;
import utils.*;
import java.util.*;

/**
 * 二维分层树 - 对应LevelTrees
 */
public class Spatial2DLevelTrees {
    public List<Spatial2DPVLTree> PVLTrees;
    public Spatial2DPVLBTree PVLBTree;
    
    public Spatial2DLevelTrees() {
        this.PVLTrees = new ArrayList<>();
        this.PVLBTree = null;
    }
    
    public Spatial2DLevelTrees(Spatial2DLevelTrees other) {
        this.PVLTrees = new ArrayList<>(other.PVLTrees);
        this.PVLBTree = other.PVLBTree;
    }
    
    public void insert(Spatial2DPVLBTree pvlbTree) {
        // 简化实现：将PVLB树转换为PVL树
        // 实际实现需要更复杂的逻辑
        this.PVLBTree = pvlbTree;
    }
    
    public void rangeQuery(Rectangle2D rect, Spatial2DRes res) {
        // 在PVL树中查询
        for (Spatial2DPVLTree tree : PVLTrees) {
            Spatial2DPVL_Res pvlRes = tree.rectangleQuery(rect);
            res.addPVLResults(pvlRes);
        }
        
        // 在PVLB树中查询
        if (PVLBTree != null) {
            Spatial2DPVLB_Res pvlbRes = PVLBTree.rectangleQuery(rect);
            res.addPVLBResults(pvlbRes);
        }
    }
}



