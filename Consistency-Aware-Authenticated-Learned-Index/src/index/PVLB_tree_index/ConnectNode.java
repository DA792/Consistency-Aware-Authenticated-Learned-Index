package index.PVLB_tree_index;

import index.PVL_tree_index.PVLLeafNode;
import index.learned_node_info.Model;
import index.learned_node_info.OptPLA;
import index.learned_node_info.Segment;
import utils.SHA;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static index.PVLB_tree_index.PVLBTree.*;
import static index.HPVL_tree_index.HPVLIndex.*;

public class ConnectNode implements Serializable {
    public PVLLeafNode modelNode;
    public MBNode buf;
    public String hash;
    public BigInteger r;
    int bufSize;

    public ConnectNode() {}
    public ConnectNode(String hash) {
        this.hash = hash;
    }
    public ConnectNode(PVLLeafNode modelNode) {
        this.modelNode = modelNode;
    }
    public ConnectNode(PVLLeafNode modelNode, MBNode buf) {
        this.modelNode = modelNode;
        this.buf = buf;
    }

    public ConnectNode(ConnectNode connectNode, MBNode buf, int bufSize) {
        this.modelNode = connectNode.modelNode;
        this.r = connectNode.r;
        this.buf = buf;
        this.bufSize = bufSize;
    }

    public ConnectNode(long k) {
        this.modelNode = new PVLLeafNode(new Segment(new Model(), new long[]{k}));
        buildVCForModel();
        computeAndSetNodeHash();
    }

    public void computeAndSetNodeHash() {
        int sz = modelNode.voInfo == null ? modelNode.keys.length : modelNode.voInfo.n;
        String str = SHA.HASHDataToString(r.toString() + sz);
        if (buf != null) str += buf.hash;
        hash = SHA.HASHDataToString(str);
    }

    public void buildVCForModel() {
        //build authenticatedInfo for model node
        this.r = modelNode.computeRAndSetPies(sk0, sk1);
    }

    //buffer MB tree insert
    private UpdNodeInfo insert(MBNode node, long k) {
        // Position of new key
        int pos = node.findChdPosByKey(k);
        UpdNodeInfo updNodeInfo;

        if (node.isLeafNode()) {
            updNodeInfo = node.copyUpdate(pos + 1, k);
        } else {
            UpdNodeInfo newNode = insert(node.childes[pos], k);
            updNodeInfo = node.copyUpdate(pos, newNode);
        }

        if (updNodeInfo.isAddNode) {
            updNodeInfo.updatedNode.childes[0].computeAndSetNodeHash();
            updNodeInfo.updatedNode.childes[1].computeAndSetNodeHash();
        } else
            updNodeInfo.updatedNode.computeAndSetNodeHash();

        return updNodeInfo;
    }



    //retrain model data, buf data, and k. return all new ConnectNodes and nodes index keys
    void retrain(long k, List<Long> newKeys, List<ConnectNode> newConNodes) {
        OptPLA optPLA = new OptPLA(err);
        long[] modelKeys = modelNode.keys;
        List<Long> bufKeys = new ArrayList<>();
        if (buf != null) buf.travelTreeGetKeys(bufKeys);
        int i = 0, j = 0;
        boolean tag = false;
        while (i < modelKeys.length || j < bufKeys.size() || !tag) {
            long min = Long.MAX_VALUE;
            if (i < modelKeys.length) min = modelKeys[i];
            if (j < bufKeys.size() && bufKeys.get(j) < min) min = bufKeys.get(j);
            if (!tag && k < min) min = k;

            if (i < modelKeys.length && modelKeys[i] == min) i++;
            else if (j < bufKeys.size() && bufKeys.get(j) == min) j++;
            else tag = true;
            optPLA.addKey(min);
        }
        optPLA.stop();

        Segment[] segments = optPLA.getSegments();

        for (i = 0; i < segments.length; ++i) {
            ConnectNode connectNode = new ConnectNode(new PVLLeafNode(segments[i]));
            connectNode.buildVCForModel();
            connectNode.computeAndSetNodeHash();
            if (i > 0)
                newKeys.add(segments[i].segData[0]);
            newConNodes.add(connectNode);
        }
    }

    public void insert(long k, List<Long> newKeys, List<ConnectNode> newConNodes) {
        if (isFull()) {
            //full, return all retrained data
            retrain(k, newKeys, newConNodes);
        } else {
            ConnectNode updatedNode;
            if (buf == null) {
                MBNode bufNode = new MBNode(new long[]{k}, new MBNode[]{new MBNode(SHA.HASHDataToString(String.valueOf(k)))});
                bufNode.computeAndSetNodeHash();

                updatedNode = new ConnectNode(this, bufNode, bufSize + 1);
            } else {
                UpdNodeInfo newBufNode = insert(buf, k);
                if (newBufNode.isAddNode)
                    newBufNode.updatedNode.computeAndSetNodeHash();
                updatedNode = new ConnectNode(this, newBufNode.updatedNode, bufSize + 1);
            }

            updatedNode.computeAndSetNodeHash();
            //add updated node info
            newConNodes.add(updatedNode);
        }
    }

    boolean isFull() {
        return bufSize >= (int) (modelNode.keys.length * BUF_RATE);
    }
}
