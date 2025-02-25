package index.PVL_tree_index;

import index.learned_node_info.Model;
import index.learned_node_info.Segment;
import utils.Utils;

import java.io.Serializable;
import java.math.BigInteger;


public class PVLNode implements Serializable {
    public VoInfo voInfo;

    public byte[][] pies;

    public Model model;
    public long[] keys;
    public PVLNode[] chd;

    public PVLNode(VoInfo voInfo) {
        this.voInfo = voInfo;
    }

    public PVLNode(Segment segment) {
        this.model = segment.model;
        this.keys = segment.segData;
    }

    public int findLeftBound(long tar, int err) {
        int pos = model.find(tar);
        if (pos - err >= keys.length) return keys.length - 1;
        int l = Math.max(0, pos - err);
        int r = Math.min(keys.length - 1, pos + err);
        return Utils.findLeftBound(keys, tar, l, r);
    }

    public BigInteger computeRAndSetPies(String sk0, String sk1) {
        return null;
    }
}
