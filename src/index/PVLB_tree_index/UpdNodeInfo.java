package index.PVLB_tree_index;

public class UpdNodeInfo {
    MBNode updatedNode;
    boolean isAddNode;

    public UpdNodeInfo(MBNode updatedNode, boolean isAddNode) {
        this.updatedNode = updatedNode;
        this.isAddNode = isAddNode;
    }
}
