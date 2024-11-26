package index.PVL_tree_index;

import index.learned_node_info.*;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import utils.IOTools;
import utils.SHA;
import utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static index.HPVL_tree_index.HPVLIndex.*;

public class PVLTree {
    BigInteger rootR;
    PVLNode root;
    public long[] src;
    public int err;

    public PVLTree(PVLTree tree, PVLNode root, BigInteger rootR) {
        this.root = root;
        this.rootR = rootR;
        this.err = tree.err;
    }

    public PVLTree(long[] dataset, int err) {
        this.err = err;
        src = dataset;
        buildLearnedTree(dataset);
        rootR = buildAuthenticatedInfo(root);

    }

    public PVLTree(long key, int err) {
        this.err = err;
        buildLearnedTree(new long[]{key});
        rootR = buildAuthenticatedInfo(root);
    }

    //Create authentication information for each node
    BigInteger buildAuthenticatedInfo(PVLNode node) {
        if (node instanceof PVLNonLeafNode) {
            PVLNonLeafNode nonLeafNode = (PVLNonLeafNode) node;
            for (int i = 0; i < nonLeafNode.chd.length; ++i) {
                nonLeafNode.chdRes[i] = buildAuthenticatedInfo(nonLeafNode.chd[i]);
            }
        }
        return node.computeRAndSetPies(sk0, sk1);
    }

    //build learned based tree
    void buildLearnedTree(long[] dataset) {
        //build leaf node
        OptPLA optPLA = new OptPLA(dataset, err);
        Segment[] segments = optPLA.getSegments();
        PVLNode[] nodes = new PVLNode[segments.length];
        long[] keys = new long[segments.length];
        for (int i = 0; i < segments.length; ++i) {
            nodes[i] = new PVLLeafNode(segments[i]);
            keys[i] = segments[i].segData[0];
        }

        //build nonLeaf node
        while (keys.length > 1) {
            optPLA = new OptPLA(keys, err);
            segments = optPLA.getSegments();
            keys = new long[segments.length];
            PVLNode[] newNodes = new PVLNode[segments.length];
            int pos = 0;
            for (int i = 0; i < segments.length; ++i) {
                newNodes[i] = new PVLNonLeafNode(segments[i], Arrays.copyOfRange(nodes, pos, pos + segments[i].segData.length));
                pos += segments[i].segData.length;
                keys[i] = segments[i].segData[0];
            }
            nodes = newNodes;
        }

        root = nodes[0];
    }

    private VoInfo rangeQuery(long low, long high, PVLNode node, List<Long> res) {
        int i = Math.max(node.findLeftBound(low, err), 0);

        VoInfo voInfo = new VoInfo(node, i);

        //start pos vo info
        if (i != 0) {
            voInfo.add(node.pies[i - 1]);
        }

        if (node instanceof PVLNonLeafNode) { // non leaf node

            PVLNonLeafNode theNode = (PVLNonLeafNode) node;
            voInfo.chdNode = new ArrayList<>();

            for (; i < theNode.keys.length && theNode.keys[i] <= high; ++i) {
                voInfo.add(theNode.chdRes[i]);
                voInfo.chdNode.add(rangeQuery(low, high, theNode.chd[i], res));
            }

            // add right bound
            if (i < theNode.keys.length && (res.size() == 0 || res.get(res.size() - 1) < high)) {
                voInfo.chdNode.add(rangeQuery(low, high, theNode.chd[i], res));
                voInfo.add(theNode.chdRes[i]);
                i++;
            }

        } else { // leaf node

            PVLLeafNode theNode = (PVLLeafNode) node;

            // add res
            for (; i < theNode.keys.length && theNode.keys[i] < high; ++i) {
                res.add(theNode.keys[i]);
            }

            // add right bound
            if (i < theNode.keys.length) {
                res.add(theNode.keys[i++]);
            }
        }
        // end pos vo info
        voInfo.add(node.pies[i - 1]);
        voInfo.endPos = i - 1;

        return voInfo;
    }


    public PVL_Res rangeQuery(long low, long high) {
        List<Long> res = new ArrayList<>();
        VoInfo voInfo = rangeQuery(low, high, root, res);
        return new PVL_Res(voInfo, res);
    }


    private PVLNode[] update(PVLNode node, long key) {
        PVLNode[] updatedNodes;
        int pos = node.findLeftBound(key, err);
        OptPLA pla = new OptPLA(err);
        if (node instanceof PVLNonLeafNode) {
            pos = Math.max(pos, 0);
            PVLNode[] newNodes = update(node.chd[pos], key);

            PVLNonLeafNode theNode = (PVLNonLeafNode) node;

            for (int i = 0 ; i < pos; ++i) {
                pla.addKey(theNode.keys[i]);
            }
            for (int j = 0; j < newNodes.length; ++j) {
                pla.addKey(newNodes[j].keys[0]);
            }
            for (int i = pos + 1; i < theNode.keys.length; ++i) {
                pla.addKey(theNode.keys[i]);
            }
            pla.stop();
            Segment[] segments = pla.getSegments();
            PVLNode[][] chdNodes = new PVLNode[segments.length][];
            BigInteger[][] chdRes = new BigInteger[segments.length][];

            int i = 0, j = 0;
            chdNodes[i] = new PVLNode[segments[i].segData.length];
            chdRes[i] = new BigInteger[segments[i].segData.length];
            for (int k = 0; k < pos; ++k) {
                if (j == chdNodes[i].length) {
                    ++i; j= 0;
                    chdNodes[i] = new PVLNode[segments[i].segData.length];
                    chdRes[i] = new BigInteger[segments[i].segData.length];
                }
                chdNodes[i][j] = theNode.chd[k];
                chdRes[i][j] = theNode.chdRes[k];
                ++j;
            }
            for (int k = 0;  k < newNodes.length; ++k) {
                if (j == chdNodes[i].length) {
                    ++i; j= 0;
                    chdNodes[i] = new PVLNode[segments[i].segData.length];
                    chdRes[i] = new BigInteger[segments[i].segData.length];
                }
                chdNodes[i][j] = newNodes[k];
                chdRes[i][j] = newNodes[k].computeRAndSetPies(sk0, sk1);
                ++j;
            }
            for (int k = pos + 1; k < theNode.chd.length; ++k) {
                if (j == chdNodes[i].length) {
                    ++i; j= 0;
                    chdNodes[i] = new PVLNode[segments[i].segData.length];
                    chdRes[i] = new BigInteger[segments[i].segData.length];
                }
                chdNodes[i][j] = node.chd[k];
                chdRes[i][j] = theNode.chdRes[k];
                ++j;
            }

            updatedNodes = new PVLNode[segments.length];
            for (int k = 0; k < segments.length; ++k) {
                updatedNodes[k] = new PVLNonLeafNode(segments[k], chdNodes[k], chdRes[k]);
            }
        } else {
            for (int i = 0; i <= pos; ++i) {
                pla.addKey(node.keys[i]);
            }
            pla.addKey(key);
            for (int i = Math.max(pos + 1, 0); i < node.keys.length; ++i) {
                pla.addKey(node.keys[i]);
            }
            pla.stop();
            Segment[] segments = pla.getSegments();

            updatedNodes = new PVLNode[segments.length];
            for (int k = 0; k < segments.length; ++k) {
                updatedNodes[k] = new PVLLeafNode(segments[k]);
            }
        }

        return updatedNodes;
    }


    public PVLTree update(long key) {
        PVLNode[] nodes = update(root, key);

        while (nodes.length > 1) {
            long[] keys = new long[nodes.length];
            int pos = 0;
            for (int i = 0; i < nodes.length; ++i) {
                keys[i] = nodes[i].keys[0];
            }
            OptPLA optPLA = new OptPLA(keys, err);
            Segment[] segments = optPLA.getSegments();
            PVLNode[] newNodes = new PVLNode[segments.length];
            for (int i = 0; i < segments.length; ++i) {
                PVLNonLeafNode tmpNode = new PVLNonLeafNode(segments[i], Arrays.copyOfRange(nodes, pos, pos + segments[i].segData.length));
                for (int j = 0; j < tmpNode.chd.length; ++j) {
                    tmpNode.chdRes[j] = tmpNode.chd[j].computeRAndSetPies(sk0, sk1);
                }
                newNodes[i] = tmpNode;
                pos += segments[i].segData.length;
            }
            nodes = newNodes;
        }
        return new PVLTree(this, nodes[0], nodes[0].computeRAndSetPies(sk0, sk1));
    }


    //lookup verify
    public boolean verify(long tar, PVL_Res res) {
        resTag = 0;
        ResInfo resInfo = new ResInfo();
        boolean isPass = verify(tar, rootR, res.node, res.res, true, true, resInfo);

        if (!isPass || !resInfo.hasRightBound || !resInfo.hasLeafBound)
            return false;

        return true;
    }

    public boolean verify(long tar, BigInteger r, VoInfo voNode, List<Long> res, boolean isFirstKey, boolean isLastKey, ResInfo resInfo) {
        byte[] bStart, bEnd;
        int i = voNode.startPos;
        if (i == 0) {
            bStart = new byte[32];
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.chdRes.size() - 1);
        } else {
            bStart = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.startPos);
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(1), voNode.startPos + voNode.chdRes.size() - 1);
        }


        if (voNode.isLeafNode()) {
            //verify leaf bound if it has not verify
            if (!resInfo.hasLeafBound) {
                if (isFirstKey && i == 0 || res.get(0) <= tar) {
                    resInfo.hasLeafBound = true;
                }
            }

            for (; i < voNode.n; ++i) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + res.get(resTag++)));
                if (Arrays.equals(bStart, bEnd))
                    break;
            }

            if (i == voNode.n)
                return false;

            //verify right bound
            if (isLastKey && i == voNode.n - 1 || res.get(res.size() - 1) >= tar) {
                resInfo.hasRightBound = true;
            }

        } else {
            for (int j = 0; j < voNode.chdRes.size(); ++j) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + voNode.chdRes.get(j)));

                if (!verify(tar, voNode.chdRes.get(j), voNode.chdNode.get(j), res, isFirstKey && i + j == 0, isLastKey && i + j == voNode.n - 1, resInfo))
                    return false;
            }

            if (!Arrays.equals(bStart, bEnd))
                return false;
        }

        return true;
    }


    private int resTag;
    private class ResInfo {
        boolean hasLeafBound = false;
        boolean hasRightBound = false;
    }
    private boolean travelVoTree(long low, long high, BigInteger r, VoInfo voNode, List<Long> res, boolean isFirstKey, boolean isLastKey, ResInfo resInfo) {
        byte[] bStart, bEnd;
        int i = voNode.startPos;
        if (i == 0) {
            bStart = new byte[32];
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.endPos);
        } else {
            bStart = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.startPos);
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(1), voNode.endPos);
        }

        if (voNode.isLeafNode()) {

            // has not left bound
            if (i != 0 && res.get(resTag) > low)
                return false;

            //check whether the key res is the left boundary
            if (res.get(resTag) <= low || isFirstKey && i == 0)
                resInfo.hasLeafBound = true;

            for (; i <= voNode.endPos; ++i) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + res.get(resTag++)));
            }

            if (!Arrays.equals(bStart, bEnd))
                return false;

            //check whether the key res is the right boundary
            if (res.get(resTag - 1) >= high || isLastKey && i == voNode.n - 1)
                resInfo.hasRightBound = true;

            // has not right bound
            if (i != voNode.n - 1 && res.get(resTag - 1) < high)
                return false;

        } else {
            for (int j = 0; j < voNode.chdRes.size(); ++j) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + voNode.chdRes.get(j)));

                if (!travelVoTree(low, high, voNode.chdRes.get(j), voNode.chdNode.get(j), res, isFirstKey && j + i == 0, isLastKey && i + j == voNode.n - 1, resInfo))
                    return false;
            }

            if (!Arrays.equals(bStart, bEnd))
                return false;
        }

        return true;
    }

    public boolean verify(long low, long high, PVL_Res PVL_res) {
        resTag = 0;
        ResInfo resInfo = new ResInfo();

        // verify every node pies
        if (!travelVoTree(low, high, rootR, PVL_res.node, PVL_res.res, true, true, resInfo))
            return false;

        // has not left or right bound
        if (!resInfo.hasLeafBound || !resInfo.hasRightBound)
            return false;

        return true;
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long piSize = ObjectSizeCalculator.getObjectSize(root);
        System.out.println("ALTree size:" + piSize / 1024.0 / 1024.0 + "mb");
    }

    public static void main(String[] args) {
        long queryTime = 0, verifyTime = 0;
        double voSize = 0;
        long s,e;
        long low = 10, high = 1000000000L;
        int len = 10000000;
        int queryLen = 1000;
        long[] dataset = Utils.buildRandArr(len, low, high, null);

        int err = 128;
        low = IOTools.low; high = IOTools.high;

//        long[] dataset = Utils.buildRandArr(len, low, high, null);
//        dataset = new long[]{919236, 728491, 712752, 385900, 430038, 553467, 448787, 194499, 512871, 826561, 137554, 53547, 631968, 738143, 555379, 160345, 293370, 566062, 148117, 711035, 274037, 964254, 364288, 513393, 877288, 359824, 974334, 393946, 173322, 582523, 979664, 500622, 668856, 312812, 716277, 204481, 616840, 927399, 540009, 937969, 224523, 846018, 43837, 972276, 347767, 538457, 102408, 832355, 869575, 938122, 165045, 776116, 852431, 126377, 125541, 522753, 726941, 801243, 785663, 205368, 233414, 225098, 399015, 344310, 568236, 57292, 32688, 490206, 203680, 721463, 516697, 69379, 147378, 731229, 564376, 879459, 987129, 703756, 241707, 629350, 926780, 627777, 339659, 894943, 160583, 320544, 360726, 871261, 356422, 907763, 769288, 341238, 722302, 591254, 875310, 352981, 115740, 930762, 221675, 199798};
//        Arrays.sort(dataset);

        PVLTree PVLTree = new PVLTree(dataset, err);

//        ALTree alTree = new ALTree(new long[]{0}, err);
//        for (long data :dataset)
//            alTree = alTree.update(data);

        long[][] queryArr = new long[queryLen][2];
        long queryDis = (long) ((high - low) * 0.001);
        for (int i = 0; i < queryLen; ++i) {
            queryArr[i][0] = (long) (Math.random() * (high - low) + low);
            queryArr[i][1] = queryArr[i][0] + queryDis;
        }

        for (int i = 0; i < queryLen; ++i) {

            s = System.nanoTime();
            PVL_Res PVL_res = PVLTree.rangeQuery(queryArr[i][0], queryArr[i][1]);
            e = System.nanoTime();
            queryTime += e - s;
            System.out.println("alTree query time:" + (e - s) + "ns");

            double sz = PVL_res.getVOSize() / 1024.0;
            System.out.println("VO size: " + sz + "kb");
            voSize += sz;

            s = System.nanoTime();
            PVLTree.verify(queryArr[i][0], PVL_res);
            e = System.nanoTime();
            verifyTime += e - s;
            System.out.println("alTree verify time:" + (e - s) + "ns");
        }

        System.out.println("query time:" +  queryTime / queryLen / 1000 + "ms");
        System.out.println("verify time:" + verifyTime / queryLen / 1000 + "ms");
        System.out.println("vo size:" + voSize / queryLen + "kb");

        PVLTree.getIndexSize();
    }

}
