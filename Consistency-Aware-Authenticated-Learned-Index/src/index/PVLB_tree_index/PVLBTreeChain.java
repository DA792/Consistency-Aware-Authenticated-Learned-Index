package index.PVLB_tree_index;

import index.PVL_tree_index.PVLTree;
import index.PVL_tree_index.PVL_Res;
import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator;
import utils.IOTools;
import utils.Utils;

import java.util.Arrays;

public class PVLBTreeChain {


    PVLBTree[] chain;
    int front, rear;

    int currentVersion;

    public PVLBTreeChain(int chainLen) {
        chain = new PVLBTree[chainLen];
        currentVersion = 0;
        front = rear = 0;
    }

    public boolean isNull() {
        return front == rear;
    }

    public boolean isFull() {
        return (rear + 1) % chain.length == front;
    }

    public void insert(long key) {
        if (isNull()) {
            chain[rear] = new PVLBTree(key);
        } else {

            chain[rear] = chain[(rear + chain.length - 1) % chain.length].insert(key);

            if (isFull()) {
                front = (front + 1) % chain.length;
            }
        }
        rear = (rear + 1) % chain.length;
    }


    public PVLBTree getVersionTree(int version) {
        return chain[(rear - currentVersion + version + chain.length) % chain.length];
    }

    public PVLB_Res rangeQuery(long low, long high, int version) {
        PVLBTree PVLBTree = getVersionTree(version);
        return PVLBTree.rangeQuery(low, high);
    }

    public boolean verify(PVLBTree tree, long low, long high, PVLB_Res res) {
        return tree.verify(low, high, res);
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        long sz = ObjectSizeCalculator.getObjectSize(chain);
        System.out.println("ALBTree size:" + sz / 1024.0 / 1024.0 + "mb");
    }


    public static void main(String[] args) {

        long s,e;

        int len = 100000000;
        int queryLen = 1000;

//        long[] dataset = Utils.buildRandArr(len, low, high, null);

//        long[] dataset = IOTools.readData("D:\\paper_source\\work_4\\dataset\\Uniform_100M", len);
        long[] dataset = IOTools.readData("D:\\paper_source\\work_4\\dataset\\Longitudes_100M", len);
        double[] queryRange = new double[]{0.0001, 0.001, 0.005, 0.01, 0.015, 0.02};
        int err = 32;
        long low = IOTools.low; long high = IOTools.high;
        int chainLen = 10000;

        PVLBTree.setParameter(64, err);
        PVLBTreeChain mbChain = new PVLBTreeChain(chainLen);
        Arrays.sort(dataset);

        s = System.nanoTime();
        for (long d : dataset) {
            mbChain.insert(d);
        }
        e = System.nanoTime();
        System.out.println("build chain time：" + (e - s) / 1000000000.0 + "s");
        mbChain.getIndexSize();

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

//                queryArr[i][0] = 979298;
//                queryArr[i][1] = 979397;

                int searchVersion = mbChain.currentVersion - (int) (Math.random() * chainLen);
                searchVersion = mbChain.currentVersion;

                s = System.nanoTime();
                PVLB_Res bTree_res = mbChain.rangeQuery(queryArr[i][0], queryArr[i][1], searchVersion);
                e = System.nanoTime();
                queryTime += e - s;

                PVLBTree versionTree = mbChain.getVersionTree(searchVersion);

                s = System.nanoTime();
                boolean isPass = mbChain.verify(versionTree, queryArr[i][0], queryArr[i][1], bTree_res);
                e = System.nanoTime();
                verifyTime += e - s;


                s = System.nanoTime();
                PVLB_Res PVLB_res = mbChain.rangeQuery(queryArr[i][0], queryArr[i][1], searchVersion);
                e = System.nanoTime();
                queryTime += e - s;
//                System.out.println("alTree query time:" + (e - s) + "ns");

                double sz = PVLB_res.getVOSize() / 1024.0;
//                System.out.println("VO size: " + sz + "kb");
                voSize += sz;
            }

            System.out.println("query range: " + r + "!!!");
            System.out.println("query time:" +  queryTime / queryLen + "ns");
            System.out.println("verify time:" + verifyTime / queryLen + "ns");
            System.out.println("vo size:" + voSize / queryLen + "kb");
        }

        mbChain.getIndexSize();


    }

}
