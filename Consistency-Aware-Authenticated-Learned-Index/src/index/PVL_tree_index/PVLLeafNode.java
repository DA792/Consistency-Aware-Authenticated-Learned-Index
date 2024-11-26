package index.PVL_tree_index;

import index.learned_node_info.Segment;

import utils.SHA;
import utils.Utils;

import java.math.BigInteger;
import java.util.Random;

public class PVLLeafNode extends PVLNode {

    public PVLLeafNode(VoInfo voInfo) {
        super(voInfo);
    }
    public PVLLeafNode(Segment segment) {
        super(segment);
    }

    public BigInteger computeRAndSetPies(String sk0, String sk1) {
        BigInteger r = new BigInteger(256, new Random());
        int n = keys.length;
        byte[] hash = new byte[32];
        pies = new byte[n][];
        for (int i = 0; i < n; ++i) {
            hash = SHA.bytesXor(hash, SHA.hashToBytes(sk0 + keys[i]));
            pies[i] = Utils.encPosHash(sk1, r, hash, i);
        }
        return r;
    }
}
