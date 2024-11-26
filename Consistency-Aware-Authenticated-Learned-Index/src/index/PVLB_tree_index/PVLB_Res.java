package index.PVLB_tree_index;

import java.util.ArrayList;
import java.util.List;

public class PVLB_Res {
    MBNode node;
    long maxKey;
    List<Long> res;

    public PVLB_Res() {
        this.res = new ArrayList<>();
    }

    public void add(List<Long> subRes) {
        res.addAll(subRes);
        maxKey = Math.max(maxKey, subRes.get(subRes.size() - 1));
    }

    public void add(PVLB_Res subRes) {
        res.addAll(subRes.res);
        maxKey = Math.max(maxKey, subRes.maxKey);
    }
}
