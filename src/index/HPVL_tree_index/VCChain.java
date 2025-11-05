package index.HPVL_tree_index;

import index.PVLB_tree_index.PVLBTree;
import index.PVLB_tree_index.PVLB_Res;

public class VCChain {
    PVLBTree[] que; // round-robin queue
    int front, rear;

    public VCChain(int len) {
        que = new PVLBTree[len];
        front = rear = 0;
    }

    public PVLBTree file() {
        if (rear == que.length - 1)
            return que[rear];
        return null;
    }

    public boolean isNull() {
        return rear == front;
    }
    public boolean isFull() {
        return (rear + 1) % que.length == front;
    }

    public void insert(long key) {
        //need build new tree while queue is null or the tree is biggest
        if (rear == 0)
            addTree(new PVLBTree(key));
        // tree is not biggest
        else
            addTree(que[(rear - 1 + que.length) % que.length].insert(key));
    }

    public void addTree(PVLBTree addTree) {
        // Insert a new tree at the tail of queue
        que[rear] = addTree;
        rear = (rear + 1) % que.length;

        //pop a tree at the head of queue if the queue is full
        if (isFull())
            front = (front + 1) % que.length;
    }

    public PVLBTree getVersionTree(int version, int currentVersion) {
        return que[(rear - currentVersion + version + que.length) % que.length];
    }

    public PVLB_Res rangeQuery(long low, long high, int version, int currentVersion) {
        PVLBTree PVLBTree = getVersionTree(version, currentVersion);
//        long s = System.nanoTime();
        PVLB_Res PVLB_res = PVLBTree.rangeQuery(low, high);
//        long e = System.nanoTime();
//        System.out.println("albTree[0] search time:" + (e - s) + "ns");

        return PVLB_res;
    }
}
