package index.HPVL_tree_index;

import index.PVLB_tree_index.PVLB_Res;
import index.PVL_tree_index.PVL_Res;

public class Res {
    PVLB_Res[] PVLB_res;
    PVL_Res[] PVL_res;

    public Res(PVLB_Res[] PVLB_res, PVL_Res[] PVL_res) {
        this.PVLB_res = PVLB_res;
        this.PVL_res = PVL_res;
    }
}
