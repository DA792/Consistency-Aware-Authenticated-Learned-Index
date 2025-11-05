package index.spatial_2d_hpvl;

import index.spatial_2d_pvl.*;
import index.spatial_2d_pvlb.*;
import utils.*;
import java.util.*;

/**
 * 二维HPVL查询结果 - 对应Res
 */
public class Spatial2DRes {
    private List<Point2D> allResults;
    private List<Spatial2DPVL_Res> pvlResults;
    private List<Spatial2DPVLB_Res> pvlbResults;
    
    public Spatial2DRes(Spatial2DPVLB_Res initialResult) {
        this.allResults = new ArrayList<>(initialResult.results);
        this.pvlResults = new ArrayList<>();
        this.pvlbResults = new ArrayList<>();
        this.pvlbResults.add(initialResult);
    }
    
    public void addPVLResults(Spatial2DPVL_Res res) {
        allResults.addAll(res.results);
        pvlResults.add(res);
    }
    
    public void addPVLBResults(Spatial2DPVLB_Res res) {
        allResults.addAll(res.results);
        pvlbResults.add(res);
    }
    
    public List<Point2D> getResults() {
        Set<Point2D> unique = new HashSet<>(allResults);
        return new ArrayList<>(unique);
    }
    
    public double getTotalVOSize() {
        double total = 0;
        for (Spatial2DPVL_Res res : pvlResults) {
            total += res.getTotalVOSize();
        }
        for (Spatial2DPVLB_Res res : pvlbResults) {
            total += res.getTotalVOSize();
        }
        return total;
    }
}



