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

    public List<Long> getResults() {
        return res;
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
