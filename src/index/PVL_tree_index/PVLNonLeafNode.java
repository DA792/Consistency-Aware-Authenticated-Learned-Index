package index.PVL_tree_index;

import index.learned_node_info.Segment;
import utils.SHA;
import utils.Utils;

import java.math.BigInteger;
import java.util.Random;


public class PVLNonLeafNode extends PVLNode {

    public BigInteger[] chdRes;

    public PVLNonLeafNode(Segment segment, PVLNode[] chd, BigInteger[] chdRes) {
        super(segment);
        this.chd = chd;
        this.chdRes = chdRes;
    }
    public PVLNonLeafNode(Segment segment, PVLNode[] chd) {
        super(segment);
        this.chd = chd;
        this.chdRes = new BigInteger[segment.segData.length];
    }

    public BigInteger computeRAndSetPies(String sk0, String sk1) {
        BigInteger r = new BigInteger(256, new Random());
        int n = keys.length;
        byte[] hash = new byte[32];
        pies = new byte[n][];
        for (int i = 0; i < n; ++i) {
            hash = SHA.bytesXor(hash, SHA.hashToBytes(sk0 + chdRes[i] + chd[i].keys.length));
            pies[i] = Utils.encPosHash(sk1, r, hash, i);
        }
        return r;
    }
}
