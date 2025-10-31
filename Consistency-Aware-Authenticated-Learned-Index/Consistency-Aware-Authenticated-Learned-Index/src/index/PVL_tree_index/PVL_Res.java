package index.PVL_tree_index;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static utils.IOTools.getFileSize;

public class PVL_Res {
    VoInfo node;
    List<Long> res;

    public PVL_Res(VoInfo node, List<Long> res) {
        this.node = node;
        this.res = res;
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
