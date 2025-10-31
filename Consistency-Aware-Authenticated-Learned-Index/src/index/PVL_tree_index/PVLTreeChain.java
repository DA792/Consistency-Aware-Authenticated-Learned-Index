package index.PVL_tree_index;

// import jdk.nashorn.internal.ir.debug.ObjectSizeCalculator; // Java 9+ 不可用
import utils.IOTools;
import utils.Utils;

public class PVLTreeChain {

    static int err;
    PVLTree[] chain;
    int front, rear;

    int currentVersion;

    public PVLTreeChain(int chainLen, int err) {
        chain = new PVLTree[chainLen];
        currentVersion = 0;
        front = rear = 0;
        this.err = err;
    }

    public boolean isNull() {
        return front == rear;
    }

    public boolean isFull() {
        return (rear + 1) % chain.length == front;
    }

    public void insert(long key) {
        if (isNull()) {
            chain[rear] = new PVLTree(key, err);
        } else {

            chain[rear] = chain[(rear + chain.length - 1) % chain.length].update(key);

            if (isFull()) {
                front = (front + 1) % chain.length;
            }
        }
        rear = (rear + 1) % chain.length;
    }


    public PVLTree getVersionTree(int version) {
        return chain[(rear - currentVersion + version + chain.length) % chain.length];
    }

    public PVL_Res rangeQuery(long low, long high, int version) {
        PVLTree albTree = getVersionTree(version);
        return albTree.rangeQuery(low, high);
    }

    public boolean verify(PVLTree tree, long low, long high, PVL_Res res) {
        return tree.verify(low, high, res);
    }

    public void getIndexSize() {
        System.setProperty("java.vm.name", "Java HotSpot(TM) ");
        // long sz = ObjectSizeCalculator.getObjectSize(chain);
        // System.out.println("ALBTree size:" + sz / 1024.0 / 1024.0 + "mb");
        System.out.println("ALBTree size: (需要 Java 8)");
    }




    public static void main(String[] args) {
        long queryTime = 0, verifyTime = 0;
        long s,e;

        int len = 100000;
        int queryLen = 10000;
        long low = 10, high = 1000000000L;

        long[] dataset = Utils.buildRandArr(len, low, high, null);

        low = IOTools.low; high = IOTools.high;
//        long[] dataset = Utils.buildRandArr(len, low, high, null);

        int err = 16;
        int chainLen = 1000;
        PVLTreeChain mbChain = new PVLTreeChain(chainLen, err);

        s = System.nanoTime();
        for (long d : dataset) {
            mbChain.insert(d);
        }
        e = System.nanoTime();
        System.out.println("build chain time：" + (e - s) / 1000000000.0 + "s");

        long[][] queryArr = new long[queryLen][2];
        long queryDis = (long) ((high - low) * 0.001);
        for (int i = 0; i < queryLen; ++i) {
            queryArr[i][0] = (long) (Math.random() * (high - low) + low);
            queryArr[i][1] = queryArr[i][0] + queryDis;
        }


        for (int i = 0; i < queryLen; ++i) {

            int searchVersion = mbChain.currentVersion - (int) (Math.random() * chainLen);
//            searchVersion = mbChain.currentVersion;

            s = System.nanoTime();
            PVL_Res bTree_res = mbChain.rangeQuery(queryArr[i][0], queryArr[i][1], searchVersion);
            e = System.nanoTime();
            queryTime += e - s;
            System.out.println("alTree query time:" + (e - s) + "ns");

            PVLTree versionTree = mbChain.getVersionTree(searchVersion);

            s = System.nanoTime();
            mbChain.verify(versionTree, queryArr[i][0], queryArr[i][1], bTree_res);
            e = System.nanoTime();
            verifyTime += e - s;
            System.out.println("alTree verify time:" + (e - s) + "ns");
        }


        System.out.println("query time:" +  queryTime / queryLen + "ns");
        System.out.println("verify time:" + verifyTime / queryLen + "ns");
    }

}
