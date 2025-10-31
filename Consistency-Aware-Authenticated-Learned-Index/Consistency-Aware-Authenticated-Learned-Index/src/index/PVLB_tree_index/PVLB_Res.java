package index.PVLB_tree_index;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static utils.IOTools.getFileSize;

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
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(Files.newOutputStream(Paths.get("VO")));
            objectOutputStream.writeObject(node);
            objectOutputStream.close();
            fileSize = getFileSize("./VO");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileSize;
    }
}
