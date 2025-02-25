package index.HPVL_tree_index;

import index.PVLB_tree_index.PVLBTree;
import index.PVLB_tree_index.PVLB_Res;
import index.PVL_tree_index.PVLTree;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import utils.IOTools;
import utils.Utils;

import java.util.Arrays;
import java.util.HashSet;


public class HPVLIndex {
    public static String sk0;
    public static String sk1;
    int chainLen;
    VCChain vcChain;
    LevelTrees state0 = new LevelTrees();
    LevelTrees state1 = new LevelTrees();

    int currentVersion;

    public HPVLIndex(int chainLen) {
        this.chainLen = chainLen;
        vcChain = new VCChain(chainLen);
        currentVersion = 0; // currentVersion = 1 is first version
    }

    public void insert(long key) {
        PVLBTree PVLBTree = vcChain.file();
        //need state update
        if (PVLBTree != null) {
            state0 = new LevelTrees(state1);
            state1.insert(PVLBTree);
        }
        vcChain.insert(key);
        currentVersion++;
    }

    public Res rangeQuery(long low, long high, int version) {
        PVLB_Res[] PVLB_res = new PVLB_Res[2];
        // vcChain query

        PVLB_res[0] = vcChain.rangeQuery(low, high, version, currentVersion);

        Res res = new Res(PVLB_res, null);

        //ALTrees query
        if (currentVersion - version > vcChain.front) {
            state0.rangeQuery(low, high, res);
        } else {
            state1.rangeQuery(low, high, res);
        }
        return res;
    }

    public VersionTrees getVersionTrees(int version) {
        VersionTrees versionTrees = new VersionTrees();
        versionTrees.PVLBTree = new PVLBTree[2];
        versionTrees.PVLBTree[0] = vcChain.getVersionTree(version, currentVersion);

        LevelTrees state;
        if (currentVersion - version > vcChain.front) {
            state = state0;
        } else {
            state = state1;
        }
        versionTrees.PVLBTree[1] = state.PVLBTree;
        versionTrees.PVLTrees = state.PVLTrees.toArray(new PVLTree[state.PVLTrees.size()]);
        return versionTrees;
    }


    public boolean verify(VersionTrees versionTrees, long low, long high, Res res) {
        //vcChain verify
        for (int i = 0; i < versionTrees.PVLBTree.length; ++i) {
            if (res.PVLB_res[i] != null) {
                if ( !versionTrees.PVLBTree[i].verify(low, high, res.PVLB_res[i]) )
                    return false;
            }
        }

        //ALTrees verify
        for (int i = 0; i < versionTrees.PVLTrees.length; ++i) {
            if (res.PVL_res[i] != null) {
                if ( !versionTrees.PVLTrees[i].verify(low, high, res.PVL_res[i]) )
                    return false;
            }
        }

        return true;
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long sz = 0;
        HashSet<PVLTree> st = new HashSet<>();
        for (PVLTree PVLTree : state0.PVLTrees) {
            if (!st.contains(PVLTree)) {
                st.add(PVLTree);
                sz += ObjectSizeCalculator.getObjectSize(PVLTree);
            }
        }
        for (PVLTree PVLTree : state1.PVLTrees) {
            if (!st.contains(PVLTree)) {
                st.add(PVLTree);
                sz += ObjectSizeCalculator.getObjectSize(PVLTree);
            }
        }

        sz += ObjectSizeCalculator.getObjectSize(vcChain);
        System.out.println("ALBTree size:" + sz / 1024.0 / 1024.0 + "mb");
    }



    public static void main(String[] args) {


        long s,e;

        int len = 100000000;
        int queryLen = 1000;
        int chainLen = 10000;
        HPVLIndex HPVLIndex = new HPVLIndex(chainLen);

        long low = 10, high = 1000000000L;

        long[] dataset = IOTools.readData("D:\\paper_source\\work_4\\dataset\\Longitudes_100M", len);
        double[] queryRange = new double[]{0.0001, 0.001, 0.005, 0.01, 0.015, 0.02};
        low = IOTools.low; high = IOTools.high;

        Arrays.sort(dataset);

        s = System.nanoTime();
        for (long d : dataset) {
            HPVLIndex.insert(d);
        }
        e = System.nanoTime();
        System.out.println("build chain time：" + (e - s) / 1000000000.0 + "s");
        HPVLIndex.getIndexSize();

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

                int searchVersion = HPVLIndex.currentVersion - (int) (Math.random() * chainLen);

                searchVersion = HPVLIndex.currentVersion;

                VersionTrees versionTrees = HPVLIndex.getVersionTrees(searchVersion);

                s = System.nanoTime();
                Res res = HPVLIndex.rangeQuery(queryArr[i][0], queryArr[i][1], searchVersion);
                e = System.nanoTime();
                queryTime += (e - s);

                voSize += res.getVOSize() / 1024.0;

                s = System.nanoTime();
                boolean isPass = HPVLIndex.verify(versionTrees, queryArr[i][0], queryArr[i][1], res);
                e = System.nanoTime();
                verifyTime += (e - s);
//            System.out.println();
            }

            System.out.println("query range: " + r + "!!!");
            System.out.println("queryTime:" + queryTime / queryLen + "ns");
            System.out.println("verifyTime:" + verifyTime / queryLen + "ns");
            System.out.println("vo size:" + voSize / queryLen + "kb");
        }

    }
}

