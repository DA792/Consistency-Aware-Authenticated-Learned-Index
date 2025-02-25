package index.PVLB_tree_index;

import utils.SHA;
import utils.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static index.PVLB_tree_index.PVLBTree.N;

public class MBNode implements Serializable {
    public long[] keys;
    public MBNode[] childes;
    public String hash;
    ConnectNode[] connectNodes;

    public int size() {
        return keys.length;
    }
    public boolean isLeafNode() {
        return childes != null && keys != null && childes.length == keys.length;
    }


    public void computeAndSetNodeHash() {
        StringBuilder builder = new StringBuilder();
        if (connectNodes != null) {
            for (ConnectNode chd : connectNodes) {
                builder.append(chd.hash);
            }
        } else {
            for (MBNode chd : childes)
                builder.append(chd.hash);
        }
        this.hash = SHA.HASHDataToString(builder.toString());
    }

    public MBNode() {
    }
    public MBNode(String hash) {this.hash = hash;}
    public MBNode(MBNode node) {
        this.childes = Arrays.copyOf(node.childes, node.childes.length);
        this.keys = Arrays.copyOf(node.keys, node.keys.length);
    }


    public MBNode(long[] keys, MBNode[] childes) {
        this.keys = keys;
        this.childes = childes;
    }

    public MBNode(long[] keys, ConnectNode[] newConNodes) {
        this.keys = keys;
        this.connectNodes = newConNodes;
    }


    public void travelTreeGetKeys(List<Long> keysList) {
        if (connectNodes != null) {
            for (ConnectNode conNode : connectNodes) {
                long[] modelKeys = conNode.modelNode.keys;
                List<Long> bufKeys = new ArrayList<>();
                if (conNode.buf != null)
                    conNode.buf.travelTreeGetKeys(bufKeys);
                keysList.addAll(Utils.sortMerge(modelKeys, bufKeys));
            }
        } else if (isLeafNode()){
            for (long k : keys) keysList.add(k);
        } else {
            for (MBNode chd : childes) {
                chd.travelTreeGetKeys(keysList);
            }
        }
    }


    public int findChdPosByKey(long k) {
        int pos = Utils.findLeftBound(keys, k, 0, keys.length - 1);
        if (isLeafNode()) return pos;
        else return pos + 1;
    }

    //update for buffer MB LeafNode(add a key)
    public UpdNodeInfo copyUpdate(int pos, long addKey) {
        int len = keys.length + 1;
        if (keys.length + 1 > N) return splitNode(pos, addKey);

        long[] newKeys = new long[len];
        MBNode[] keyHashNode = new MBNode[len];

        for (int i = 0; i < keys.length; ++i) {
            if (i < pos) {
                newKeys[i] = keys[i];
                keyHashNode[i] = childes[i];
            } else {
                newKeys[i + 1] = keys[i];
                keyHashNode[i + 1] = childes[i];
            }
        }

        newKeys[pos] = addKey;
        keyHashNode[pos] = new MBNode(SHA.HASHDataToString(String.valueOf(addKey)));

        return new UpdNodeInfo(new MBNode(newKeys, keyHashNode), false);
    }


    //update for MB nonLeafNode(add a batch of connectNode)
    public UpdNodeInfo copyUpdate(int pos, List<Long> addKeys, List<ConnectNode> addConNodes) {
        int len = keys.length + addKeys.size();
        if (len > N) return splitNode(pos, addKeys, addConNodes);

        //the addKeys.length == addConNodes.length - 1

        long[] newKeys = new long[len];
        ConnectNode[] newConNodes = new ConnectNode[len + 1];

        for (int i = 0; i < keys.length; ++i) {
            if (i < pos)
                newKeys[i] = keys[i];
            else
                newKeys[i + addKeys.size()] = keys[i];
        }
        for (int i = 0; i < connectNodes.length; ++i) {
            if (i < pos) {
                newConNodes[i] = connectNodes[i];
            } else if (i > pos) {
                newConNodes[i + addConNodes.size() - 1] = connectNodes[i];
            }
        }

        for (int i = 0; i < addKeys.size(); ++i) {
            newKeys[pos + i] = addKeys.get(i);
        }
        for (int i = 0; i < addConNodes.size(); ++i) {
            newConNodes[pos + i] = addConNodes.get(i);
        }

        return new UpdNodeInfo(new MBNode(newKeys, newConNodes), false);
    }

    //update for MB nonLeafNode
    public UpdNodeInfo copyUpdate(int pos, UpdNodeInfo updNodeInfo) {
        if (updNodeInfo.isAddNode) {
            if (keys.length + 1 > N) return splitNode(pos, updNodeInfo);
            long[] newKeys = new long[keys.length + 1];
            MBNode[] newChdNodes = new MBNode[childes.length + 1];
            for (int i = 0; i < keys.length; ++i) {
                if (i < pos) newKeys[i] = keys[i];
                else newKeys[i + 1] = keys[i];
            }
            for (int i = 0; i < childes.length; ++i) {
                if (i < pos) newChdNodes[i] = childes[i];
                else newChdNodes[i + 1] = childes[i];
            }
            newKeys[pos] = updNodeInfo.updatedNode.keys[0];
            newChdNodes[pos] = updNodeInfo.updatedNode.childes[0];
            newChdNodes[pos + 1] = updNodeInfo.updatedNode.childes[1];
            return new UpdNodeInfo(new MBNode(newKeys, newChdNodes), false);
        } else {
            MBNode copyNode = new MBNode(this);
            copyNode.childes[pos] = updNodeInfo.updatedNode;
            return new UpdNodeInfo(copyNode, false);
        }
    }

    //split for buffer MB LeafNode(split MB Node)
    public UpdNodeInfo splitNode(int pos, long addKey) {
        String addHash = SHA.HASHDataToString(String.valueOf(addKey));
        int len1 = (keys.length + 1) / 2;
        int len2 = keys.length + 1 - len1;
        long[] newKeys1 = new long[len1];
        long[] newKeys2 = new long[len2];
        MBNode[] keyHashNode1 = new MBNode[len1];
        MBNode[] keyHashNode2 = new MBNode[len2];

        int p1 = 0, p2 = 0;
        for (int i = 0; i <= keys.length; ++i) {
            if (i == pos) {
                if (p1 < len1) {
                    newKeys1[p1] = addKey;
                    keyHashNode1[p1++] = new MBNode(addHash);
                } else {
                    newKeys2[p2] = addKey;
                    keyHashNode2[p2++] = new MBNode(addHash);
                }
            }
            if (i < keys.length) {
                if (p1 < len1) {
                    newKeys1[p1] = keys[i];
                    keyHashNode1[p1++] = childes[i];
                } else {
                    newKeys2[p2] = keys[i];
                    keyHashNode2[p2++] = childes[i];
                }
            }
        }

        long[] splitNodeKey = new long[]{newKeys2[0]};
        MBNode[] splitChdNodes = new MBNode[]{new MBNode(newKeys1, keyHashNode1), new MBNode(newKeys2, keyHashNode2)};
        return new UpdNodeInfo(new MBNode(splitNodeKey, splitChdNodes), true);
    }


    //split for MB nonLeafNode(split ConnectNode)
    public UpdNodeInfo splitNode(int pos, List<Long> addKeys, List<ConnectNode> addConNodes) {

        //the addKeys.length == addConNodes.length - 1

        int len1 = (keys.length + addKeys.size() - 1) / 2;
        int len2 = keys.length + addKeys.size() - 1 - len1;

        long[] newKeys1 = new long[len1];
        long[] newKeys2 = new long[len2];
        ConnectNode[] newConNodes1 = new ConnectNode[len1 + 1];
        ConnectNode[] newConNodes2 = new ConnectNode[len2 + 1];


        int p1 = 0, p2 = 0;
        int updTag = 0;
        long tagKey = -1;
        for (int i = 0; i <= keys.length; ++i) {
            long k = i < keys.length ? keys[i] : -1;
            if (i == pos && updTag < addKeys.size()) {
                k = addKeys.get(updTag++);
                i--;
            }
            if (k != -1) {
                if (p1 < newKeys1.length) newKeys1[p1++] = k;
                else if (tagKey == -1) tagKey = k;
                else newKeys2[p2++] = k;
            }
        }
        p1 = 0; p2 = 0;
        updTag = 0;
        for (int i = 0; i <= connectNodes.length; ++i) {
            ConnectNode con = i < connectNodes.length ? connectNodes[i] : null;
            if (i == pos) {
                if (updTag == addConNodes.size())
                    continue;
                con = addConNodes.get(updTag++);
                i--;
            }
            if (con != null) {
                if (p1 < newConNodes1.length) newConNodes1[p1++] = con;
                else newConNodes2[p2++] = con;
            }
        }

        long[] splitNodeKey = new long[]{tagKey};
        MBNode[] splitChdNodes = new MBNode[]{new MBNode(newKeys1, newConNodes1), new MBNode(newKeys2, newConNodes2)};
        return new UpdNodeInfo(new MBNode(splitNodeKey, splitChdNodes), true);
    }

    //split for MB nonLeafNode
    public UpdNodeInfo splitNode(int pos, UpdNodeInfo updNodeInfo) {
        //leafNode split way and nonLeafNode split way are not equal
        int len1, len2;
        len1 = keys.length / 2;
        len2 = keys.length - len1;

        long[] newKeys1 = new long[len1];
        long[] newKeys2 = new long[len2];
        MBNode[] newChdNodes1 = new MBNode[len1 + 1];
        MBNode[] newChdNodes2 = new MBNode[len2 + 1];

        int p1 = 0, p2 = 0;
        int updTag = 0;
        long tagKey = -1;
        for (int i = 0; i <= keys.length; ++i) {
            long k = i < keys.length ? keys[i] : -1;
            if (i == pos && updTag == 0) {
                k = updNodeInfo.updatedNode.keys[0];
                updTag++;
                i--;
            }
            if (k != -1) {
                if (p1 < newKeys1.length) newKeys1[p1++] = k;
                else if (tagKey == -1) tagKey = k;
                else newKeys2[p2++] = k;
            }
        }
        p1 = 0; p2 = 0;
        updTag = 0;
        for (int i = 0; i <= childes.length; ++i) {
            MBNode chd = i < childes.length ? childes[i] : null;
            if (i == pos) {
                chd = updNodeInfo.updatedNode.childes[0];
                updTag++;
            } else if (i == pos + 1 && updTag == 1) {
                chd = updNodeInfo.updatedNode.childes[1];
                updTag++;
                i--;
            }
            if (chd != null) {
                if (p1 < newChdNodes1.length) newChdNodes1[p1++] = chd;
                else newChdNodes2[p2++] = chd;
            }
        }

        long[] splitNodeKey = new long[]{tagKey};
        MBNode[] splitChdNodes = new MBNode[]{new MBNode(newKeys1, newChdNodes1), new MBNode(newKeys2, newChdNodes2)};
        return new UpdNodeInfo(new MBNode(splitNodeKey, splitChdNodes), true);
    }
}
