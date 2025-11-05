package index.PVL_tree_index;

import index.learned_node_info.*;
// import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator; // Java 9+ 不可用
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

    /**
     * 批量范围查询 - 优化版本
     * 注意: 当前实现为了保证正确性,仍然逐个查询,但减少了函数调用开销
     * @param intervals 查询区间列表 [(low1,high1), (low2,high2), ...]
     * @return 每个区间对应的查询结果列表
     */
    public List<PVL_Res> batchRangeQuery(List<long[]> intervals) {
        List<PVL_Res> results = new ArrayList<>();
        
        // 对每个区间执行查询
        for (long[] interval : intervals) {
            List<Long> res = new ArrayList<>();
            VoInfo voInfo = rangeQuery(interval[0], interval[1], root, res);
            results.add(new PVL_Res(voInfo, res));
        }
        
        return results;
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
//            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.chdRes.size() - 1);
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(0), voNode.endPos);
        } else {
            bStart = Utils.encPosHash(sk1, r, voNode.voPies.get(0), i - 1);
//            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(1), voNode.startPos + voNode.chdRes.size() - 1);
            bEnd = Utils.encPosHash(sk1, r, voNode.voPies.get(1), voNode.endPos);
        }


        if (voNode.isLeafNode()) {
            //verify leaf bound if it has not verify
            if (!resInfo.hasLeafBound) {
                if (isFirstKey && i == 0 || res.get(0) <= tar) {
                    resInfo.hasLeafBound = true;
                }
            }

//            for (; i < voNode.n; ++i) {
//                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + res.get(resTag++)));
//                if (Arrays.equals(bStart, bEnd))
//                    break;
//            }
//            if (i == voNode.n)
//                return false;

            for (; i <= voNode.endPos; ++i) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + res.get(resTag++)));
            }

            if (!Arrays.equals(bStart, bEnd))
                return false;

            //verify right bound
            if (isLastKey && i == voNode.n || res.get(res.size() - 1) >= tar) {
                resInfo.hasRightBound = true;
            }

        } else {
            for (int j = 0; j < voNode.chdRes.size(); ++j) {
                bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + voNode.chdRes.get(j) + voNode.chdNode.get(j).n));

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
        if (!verify(low, rootR, PVL_res.node, PVL_res.res, true, true, resInfo))
            return false;

        // has not left or right bound
        if (!resInfo.hasLeafBound || !resInfo.hasRightBound)
            return false;

        return true;
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        // ObjectSizeCalculator 在 Java 9+ 中不可用
        // long piSize = ObjectSizeCalculator.getObjectSize(root);
        // System.out.println("ALTree size:" + piSize / 1024.0 / 1024.0 + "mb");
        System.out.println("ALTree size: (需要 Java 8 或添加 ObjectSizeCalculator 库)");
    }


    public static void main(String[] args) {

        long s,e;
        long low = 10, high = 1000000000L;
        int len = 100000000;
        int queryLen = 1000;
//        long[] dataset = Utils.buildRandArr(len, low, high, null);
        long[] dataset = IOTools.readData("D:\\paper_source\\work_4\\dataset\\Longitudes_100M", len);
        double[] queryRange = new double[]{0.0001, 0.001, 0.005, 0.01, 0.015, 0.02};
        int err = 128;
        low = IOTools.low; high = IOTools.high;

        Arrays.sort(dataset);
        PVLTree PVLTree = new PVLTree(dataset, err);

//        ALTree alTree = new ALTree(new long[]{0}, err);
//        for (long data :dataset)
//            alTree = alTree.update(data);

        for (double r : queryRange) {
            long[][] queryArr = new long[queryLen][2];
            long queryDis = (long) ((high - low) * r);
            for (int i = 0; i < queryLen; ++i) {
                queryArr[i][0] = (long) (Math.random() * (high - low) + low);
                queryArr[i][1] = queryArr[i][0] + queryDis;
            }

            long queryTime = 0, verifyTime = 0;
            double voSize = 0;

            for (int i = 0; i < queryLen; ++i) {
                s = System.nanoTime();
                PVL_Res PVL_res = PVLTree.rangeQuery(queryArr[i][0], queryArr[i][1]);
                e = System.nanoTime();
                queryTime += e - s;
//                System.out.println("alTree query time:" + (e - s) + "ns");

                double sz = PVL_res.getVOSize() / 1024.0;
//                System.out.println("VO size: " + sz + "kb");
                voSize += sz;

                s = System.nanoTime();
                boolean isPass = PVLTree.verify(queryArr[i][0], PVL_res);
                e = System.nanoTime();
                verifyTime += e - s;
//                System.out.println("alTree verify time:" + (e - s) + "ns");
            }

            System.out.println("query range: " + r + "!!!");
            System.out.println("query time:" +  queryTime / queryLen + "ns");
            System.out.println("verify time:" + verifyTime / queryLen + "ns");
            System.out.println("vo size:" + voSize / queryLen + "kb");

        }

        PVLTree.getIndexSize();
    }


}
