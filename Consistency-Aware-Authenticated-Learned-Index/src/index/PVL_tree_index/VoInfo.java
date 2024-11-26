package index.PVL_tree_index;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class VoInfo implements Serializable {
    public int n; //node keys size

    public int startPos;
    public int endPos;
    public List<byte[]> voPies;

    public List<BigInteger> chdRes;
    public List<VoInfo> chdNode;

    public boolean isLeafNode() {
        return chdNode == null;
    }

    public VoInfo(PVLNode node, int startPos) {
        this.n = node.keys.length;
        this.startPos = startPos;
        voPies = new ArrayList<>();

        if (node instanceof PVLNonLeafNode)
            chdRes = new ArrayList<>();
    }

    public void add(BigInteger chdR) {
        this.chdRes.add(chdR);
    }

    public void add(byte[] voPie) {
        this.voPies.add(voPie);
    }
}
