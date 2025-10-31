package index.spatial_2d_pvlb;

import index.PVLB_tree_index.PVLB_Res;
import utils.*;
import java.util.*;

/**
 * 二维PVLB查询结果
 */
public class Spatial2DPVLB_Res {
    public final List<Point2D> results;
    public final List<PVLB_Res> pvlbResults;
    
    public Spatial2DPVLB_Res(List<Point2D> results, List<PVLB_Res> pvlbResults) {
        this.results = results;
        this.pvlbResults = pvlbResults;
    }
    
    public double getTotalVOSize() {
        double totalSize = 0;
        for (PVLB_Res result : pvlbResults) {
            totalSize += result.getVOSize();
        }
        return totalSize;
    }
}



