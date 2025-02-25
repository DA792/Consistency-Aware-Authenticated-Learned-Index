package index.HPVL_tree_index;

import index.PVLB_tree_index.PVLB_Res;
import index.PVL_tree_index.PVLTree;
import index.PVL_tree_index.PVL_Res;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import static utils.IOTools.getFileSize;

public class Res {
    PVLB_Res[] PVLB_res;
    PVL_Res[] PVL_res;

    public Res(PVLB_Res[] PVLB_res, PVL_Res[] PVL_res) {
        this.PVLB_res = PVLB_res;
        this.PVL_res = PVL_res;
    }


    public long getVOSize() {
        long fileSize = 0;

        for (PVLB_Res res : PVLB_res) {
            if (res == null) continue;
            fileSize += res.getVOSize();
        }

        for (PVL_Res res : PVL_res) {
            if (res == null) continue;
            fileSize += res.getVOSize();
        }

        return fileSize;
    }
}
