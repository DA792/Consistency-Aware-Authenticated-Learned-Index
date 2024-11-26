package index.PVLB_tree_index;

import index.PVL_tree_index.PVLLeafNode;
import index.PVL_tree_index.VoInfo;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import utils.SHA;
import utils.Utils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static index.HPVL_tree_index.HPVLIndex.sk0;
import static index.HPVL_tree_index.HPVLIndex.sk1;


public class PVLBTree {
    static int N = 32;
    static double BUF_RATE = 0.3;
    public static int err = 64;
    public MBNode root;

    public static void setParameter(int N, int err) {
        PVLBTree.N = N;
        PVLBTree.err = err;
    }

    public PVLBTree(long key) {
        this.root = insert(key).root;
    }

    public PVLBTree(MBNode node) {
        this.root = node;
    }

    public PVLBTree(int err, int N) {
        PVLBTree.err = err;
        PVLBTree.N = N;
    }


    public List<Long> travelTreeGetKeys() {
        List<Long> keysList = new ArrayList<>();
        if (root != null)
            root.travelTreeGetKeys(keysList);
        return keysList;
    }

    private class ResInfo {
        boolean hasLeafBound = false;
        boolean hasRightBound = false;
    }
    private int resTag = 0;
    private boolean verifyModelNode(long low, long high, BigInteger r, PVLLeafNode modelNode, List<Long> res) {
        VoInfo voInfo = modelNode.voInfo;
        int i = voInfo.startPos;

        //has not left bound
        if (i != 0 && res.get(resTag) > low)
            return false;

        byte[] bStart, bEnd;

        if (i == 0) {
            bStart = new byte[32];
            bEnd = Utils.encPosHash(sk1, r, modelNode.voInfo.voPies.get(0), modelNode.voInfo.endPos);
        } else {
            bStart = Utils.encPosHash(sk1, r, modelNode.voInfo.voPies.get(0), modelNode.voInfo.startPos);
            bEnd = Utils.encPosHash(sk1, r, modelNode.voInfo.voPies.get(1), modelNode.voInfo.endPos);
        }

        for (; i <= modelNode.voInfo.endPos; ++i) {
            bStart = SHA.bytesXor(bStart, SHA.hashToBytes(sk0 + res.get(resTag++)));
        }

        if (!Arrays.equals(bStart, bEnd))
            return false;

        // has not right bound
        if (i != voInfo.n - 1 && res.get(resTag - 1) < high)
            return false;

        return true;
    }

    private void verifyConnectNode(long low, long high, ConnectNode conNode, List<Long> res, boolean isFirstKey, boolean isLastKey, ResInfo resInfo) {
        //verify model result
        boolean isPass = verifyModelNode(low, high, conNode.r, conNode.modelNode, res);
        if (!isPass)
            return;

        // whether all tree has left bound
        if (res.get(0) <= low || isFirstKey) {
            resInfo.hasLeafBound = true;
        }

        // whether all tree has right bound
        if (res.get(resTag - 1) >= high) {
            resInfo.hasRightBound = true;
        }

        //verify buffer result
        if (conNode.buf != null) {
            if (conNode.buf.hash == null) {
                ResInfo bufSubInfo = new ResInfo();
                travelMBTree(low, high, conNode.buf, res, true, true, bufSubInfo);

                // buffer has not left or right bound
                if (!bufSubInfo.hasLeafBound || !bufSubInfo.hasRightBound)
                    return;

                // the model node must have isLastKey, otherwise hasRightBound is true
                if (res.get(resTag - 1) >= high || isLastKey) {
                    resInfo.hasRightBound = true;
                }
            }
        } else {
            if (isLastKey)
                resInfo.hasRightBound = true;
        }

        conNode.computeAndSetNodeHash();
    }

    private void travelMBTree(long low, long high, MBNode node, List<Long> res, boolean isFirstKey, boolean isLastKey, ResInfo resInfo) {
        if (node.hash != null)
            return;

        if (node.connectNodes != null) {

            for (int i = 0; i < node.connectNodes.length; ++i) {
                if (node.connectNodes[i].hash == null) {
                    verifyConnectNode(low, high, node.connectNodes[i], res, isFirstKey && i == 0, isLastKey && i == node.connectNodes.length - 1, resInfo);
                }
            }

        } else {

            for (int i = 0; i < node.childes.length; ++i) {
                // leaf node, compute hash
                if (node.childes[i] == null) {
                    //determine whether it is a left and right boundary
                    if (isFirstKey && i == 0 || res.get(resTag) <= low)
                        resInfo.hasLeafBound = true;
                    if (isLastKey && i == node.childes.length - 1 || res.get(resTag) >= high)
                        resInfo.hasRightBound = true;

                    node.childes[i] = new MBNode(SHA.HASHDataToString(res.get(resTag++).toString()));
                } else
                    travelMBTree(low, high, node.childes[i], res, isFirstKey && i == 0, isLastKey && i == node.childes.length - 1, resInfo);
            }
        }

        node.computeAndSetNodeHash();
    }


    public boolean verify(long low, long high, PVLB_Res PVLB_res) {
        resTag = 0;
        ResInfo info = new ResInfo();

        travelMBTree(low, high, PVLB_res.node, PVLB_res.res, true, true, info);

        // has not left or right bound
        if (!info.hasLeafBound || !info.hasRightBound)
            return false;

        return root.hash.equals(PVLB_res.node.hash);
    }



    public PVLB_Res rangeQuery(long low, long high) {
        PVLB_Res PVLB_res = new PVLB_Res();
        PVLB_res.node = rangeQueryInMB(low, high, root, PVLB_res);
        return PVLB_res;
    }

    private PVLLeafNode rangeQueryInModel(long low, long high, PVLLeafNode PVLLeafNode, List<Long> res) {
        int i = Math.max(0, PVLLeafNode.findLeftBound(low, err));

        VoInfo voInfo = new VoInfo(PVLLeafNode, i);

        //start pos vo info
        if (i != 0) {
            voInfo.add(PVLLeafNode.pies[i - 1]);
        }

        // add res
        for (; i < PVLLeafNode.keys.length && PVLLeafNode.keys[i] < high; ++i) {
            res.add(PVLLeafNode.keys[i]);
        }

        // add right bound
        if (i < PVLLeafNode.keys.length) {
            res.add(PVLLeafNode.keys[i++]);
        }

        // end pos vo info
        voInfo.add(PVLLeafNode.pies[i - 1]);
        voInfo.endPos = i - 1;
        return new PVLLeafNode(voInfo);
    }

    private ConnectNode rangeQueryInCon(long low, long high, ConnectNode conNode, PVLB_Res resInfo) {
        //query in model
        List<Long> modelResInfo = new ArrayList<>();
        PVLLeafNode alVoNode = rangeQueryInModel(low, high, conNode.modelNode, modelResInfo);
        resInfo.add(modelResInfo);

        // query in buffer
        PVLB_Res bufResInfo = new PVLB_Res();
        MBNode bufVoNode = null;
        if (conNode.buf != null)
            bufVoNode = rangeQueryInMB(low, high, conNode.buf, bufResInfo);
        resInfo.add(bufResInfo);

        ConnectNode connectNode = new ConnectNode(alVoNode, bufVoNode);
        connectNode.r = conNode.r;
        return connectNode;
    }

    private MBNode rangeQueryInMB(long low, long high, MBNode node, PVLB_Res resInfo) {
        int startPos = Math.max(0, node.findChdPosByKey(low));
        MBNode voNode = new MBNode();
        if (node.isLeafNode()) {
            int n = node.keys.length;
            voNode.childes = new MBNode[n];
            int i = 0;

            for (; i < startPos; ++i)
                voNode.childes[i] = new MBNode(node.childes[i].hash);

            for (; i < node.keys.length && node.keys[i] <= high; ++i)
                resInfo.res.add(node.keys[i]);

            //add right bound
            if (i == 0 || i < n && node.keys[i - 1] < high)
                resInfo.res.add(node.keys[i++]);

            for (; i < n; ++i)
                voNode.childes[i] = new MBNode(node.childes[i].hash);
            resInfo.maxKey = Math.max(resInfo.maxKey, resInfo.res.get(resInfo.res.size() - 1));
        } else if (node.connectNodes != null) {
            int n = node.connectNodes.length;
            voNode.connectNodes = new ConnectNode[n];
            int i = 0;

            for (; i < startPos; ++i)
                voNode.connectNodes[i] = new ConnectNode(node.connectNodes[i].hash);

            for (; i == 0 || i < node.connectNodes.length && node.keys[i - 1] <= high; ++i)
                voNode.connectNodes[i] = rangeQueryInCon(low, high, node.connectNodes[i], resInfo);

            // has not right bound currently, continue find right bound
            if (i < node.connectNodes.length && resInfo.maxKey < high) {
                // add model node
                //just need find model node, the first key of node is right bound
                PVLLeafNode subModelNode = node.connectNodes[i].modelNode;
                VoInfo modelVoInfo = new VoInfo(subModelNode, 0);
                resInfo.maxKey = Math.max(resInfo.maxKey, subModelNode.keys[0]);
                resInfo.res.add(subModelNode.keys[0]);
                modelVoInfo.add(subModelNode.pies[0]);

                //add buffer node
                MBNode buf = null;
                if (node.connectNodes[i].buf != null) {
                    buf = new MBNode(node.connectNodes[i].buf.hash);
                }

                //return connect node
                voNode.connectNodes[i] = new ConnectNode(new PVLLeafNode(modelVoInfo), buf);
                voNode.connectNodes[i].r = node.connectNodes[i].r;
                i++;
            }

            for (; i < n; ++i)
                voNode.connectNodes[i] = new ConnectNode(node.connectNodes[i].hash);

        } else {
            // nonLeafNode
            int n = node.childes.length;
            voNode.childes = new MBNode[n];
            int i = 0;

            for (; i < startPos; ++i)
                voNode.childes[i] = new MBNode(node.childes[i].hash);

            for (; i == 0 || i < n && node.keys[i - 1] <= high; ++i)
                voNode.childes[i] = rangeQueryInMB(low, high, node.childes[i], resInfo);

            // has not right bound currently, continue find right bound
            if (i < n && resInfo.maxKey < high) {
                voNode.childes[i] = rangeQueryInMB(low, high, node.childes[i], resInfo);
                i++;
            }

            for (; i < n; ++i) voNode.childes[i] = new MBNode(node.childes[i].hash);
        }
        return voNode;
    }

    public PVLBTree insert(long key) {
        if (root == null) {
            MBNode root_ = new MBNode(new long[]{}, new ConnectNode[]{new ConnectNode(key)});
            root_.computeAndSetNodeHash();
            return new PVLBTree(root_);
        } else {
            UpdNodeInfo newNode = insert(root, key);
            if (newNode.isAddNode)
                newNode.updatedNode.computeAndSetNodeHash();
            return new PVLBTree(newNode.updatedNode);
        }
    }

    private UpdNodeInfo insert(MBNode node, long k) {

        // Position of new key
        int pos = node.findChdPosByKey(k);
        UpdNodeInfo updNodeInfo;

        // connect node insert
        if (node.connectNodes != null) {
            //initializes the incoming parameters
            List<Long> newKeys = new ArrayList<>();
            List<ConnectNode> newConNodes = new ArrayList<>();

            //insert to connectNodes
            node.connectNodes[pos].insert(k, newKeys, newConNodes);
            updNodeInfo = node.copyUpdate(pos, newKeys, newConNodes);
        } else {
            UpdNodeInfo subInfo = insert(node.childes[pos], k);
            updNodeInfo = node.copyUpdate(pos, subInfo);
        }

        if (updNodeInfo.isAddNode) {
            updNodeInfo.updatedNode.childes[0].computeAndSetNodeHash();
            updNodeInfo.updatedNode.childes[1].computeAndSetNodeHash();
        } else
            updNodeInfo.updatedNode.computeAndSetNodeHash();

        return updNodeInfo;
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long piSize = ObjectSizeCalculator.getObjectSize(root);
        System.out.println("ALBTree size:" + piSize / 1024.0 / 1024.0 + "mb");
    }


    public static void main(String[] args) {

        long queryTime = 0, verifyTime = 0;
        long s,e;

        int len = 1000000;
        int queryLen = 10000;

        int n = 128;
        int err = 256;

        long low = 10, high = 1000000000L;

        long[] dataset = Utils.buildRandArr(len, low, high, null);
//        dataset = new long[]{7051, 1209, 7161, 5523, 6576, 3078, 875, 5453, 8767, 2320, 7712, 8513, 7656, 8656, 7825, 6096};
//        dataset = new long[]{6193, 7820, 7136, 1545, 5862, 9350, 5375, 9001, 9097, 2194, 7223, 4225, 4990, 3778, 3887, 6053, 5852, 5652, 1983, 2898, 6202, 3326, 515, 865, 9772, 779, 7982, 4928, 5625, 4816, 3878, 3594, 283, 7504, 4803, 6931, 1354, 3290, 9170, 7611, 9577, 1091, 6042, 4711, 417, 616, 3055, 414, 3403, 6526, 5540, 464, 7235, 3625, 3991, 255, 1357, 1208, 9852, 9776, 196, 4964, 200, 2271, 4509, 359, 399, 3462, 5436, 2297, 5, 732, 2070, 1306, 1787, 9417, 4045, 1777, 3980, 573, 1401, 1380, 1102, 4436, 2315, 3204, 9975, 9030, 5429, 1225, 5435, 3628, 6190, 2112, 5194, 1071, 3690, 8957, 8082, 4596};

        PVLBTree alBTree = new PVLBTree(err, n);


        s = System.nanoTime();
        for (long d : dataset)
            alBTree = alBTree.insert(d);
        e = System.nanoTime();
        System.out.println("build chain time：" + (e - s) / 1000000000.0 + "s");


        long[][] queryArr = new long[queryLen][2];
        long queryDis = (long) ((high - low) * 0.001);
        for (int i = 0; i < queryLen; ++i) {
            queryArr[i][0] = (long) (Math.random() * (high - low) + low);
            queryArr[i][1] = queryArr[i][0] + queryDis;
        }

        for (int i = 0; i < queryLen; ++i) {

//            System.out.println(i);

            s = System.nanoTime();
            PVLB_Res al_res = alBTree.rangeQuery(queryArr[i][0], queryArr[i][1]);
            e = System.nanoTime();
            queryTime += e - s;

            s = System.nanoTime();
            boolean isPass = alBTree.verify(queryArr[i][0], queryArr[i][1], al_res);
            e = System.nanoTime();
            verifyTime += e - s;
        }

        System.out.println("query time:" +  queryTime / queryLen + "ns");
        System.out.println("verify time:" + verifyTime / queryLen + "ns");
    }
}
