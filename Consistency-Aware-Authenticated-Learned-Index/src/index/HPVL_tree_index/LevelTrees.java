package index.HPVL_tree_index;

import index.PVLB_tree_index.PVLBTree;
import index.PVL_tree_index.PVLTree;
import index.PVL_tree_index.PVL_Res;
import utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static index.HPVL_tree_index.VersionTrees.errList;

public class LevelTrees {
    PVLBTree PVLBTree;
    List<PVLTree> PVLTrees = new ArrayList<>();

    public LevelTrees() {

    }

    public LevelTrees(LevelTrees levelTrees) {
        this.PVLBTree = levelTrees.PVLBTree;
        this.PVLTrees = levelTrees.PVLTrees;
    }

    public void insert(PVLBTree newPVLBTree) {
        List<PVLTree> newPVLTrees = new ArrayList<>(PVLTrees.size() + 1);

        long[] dataList = null;
        if (PVLBTree == null) {
            PVLBTree = newPVLBTree;
        } else {
            List<Long> dataList1 = newPVLBTree.travelTreeGetKeys();
            List<Long> dataList2 = PVLBTree.travelTreeGetKeys();
            PVLBTree = null;
            dataList = Utils.sortMerge(dataList1, dataList2);
        }

        int i = 0;
        for (; i < PVLTrees.size(); ++i) {

            if (dataList != null && PVLTrees.get(i) == null) {
                newPVLTrees.add(new PVLTree(dataList, errList[i]));
                dataList = null;
            } else if (dataList != null) {
                dataList = Utils.sortMerge(dataList, PVLTrees.get(i).src);
                newPVLTrees.add(null);
            } else {
                newPVLTrees.add(PVLTrees.get(i));
            }
        }

        if (dataList != null) {
            newPVLTrees.add(new PVLTree(dataList, errList[i]));
        }

        this.PVLTrees = newPVLTrees;
    }


    public void rangeQuery(long low, long high, Res res) {
        long s, e;
        if (PVLBTree != null) {
//            s = System.nanoTime();
            res.PVLB_res[1] = PVLBTree.rangeQuery(low, high);
//            e = System.nanoTime();
//            System.out.println("albTree[1] search time:" + (e - s) + "ns");
        }

        res.PVL_res = new PVL_Res[PVLTrees.size()];
        for (int i = 0; i < PVLTrees.size(); ++i) {
            if (PVLTrees.get(i) != null) {
//                s = System.nanoTime();
                res.PVL_res[i] = PVLTrees.get(i).rangeQuery(low, high);
//                e = System.nanoTime();
//                System.out.println("alTree[" + i + "] search time:" + (e - s) + "ns");
            }

        }

    }
}
