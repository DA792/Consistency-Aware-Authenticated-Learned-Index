package index.PVLB_tree_index;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class PVLB_Res {
    MBNode node;
    long maxKey;
    List<Long> res;

    public PVLB_Res() {
        this.res = new ArrayList<>();
    }

    public void add(List<Long> subRes) {
        res.addAll(subRes);
        maxKey = Math.max(maxKey, subRes.get(subRes.size() - 1));
    }

    public void add(PVLB_Res subRes) {
        res.addAll(subRes.res);
        maxKey = Math.max(maxKey, subRes.maxKey);
    }


    public long getVOSize() {
        long fileSize = 0;
        try {
            // 使用内存流计算大小,避免文件冲突
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteStream);
            objectOutputStream.writeObject(node);
            objectOutputStream.close();
            fileSize = byteStream.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileSize;
    }
}
