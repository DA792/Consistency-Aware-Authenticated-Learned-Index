package index.spatial_2d_hpvl;

import index.spatial_2d_pvlb.*;
import utils.*;
import java.util.*;

/**
 * 二维版本控制链 - 对应VCChain
 */
public class Spatial2DVCChain {
    Spatial2DPVLBTree[] chain;
    int front, rear;
    int err;
    
    public Spatial2DVCChain(int chainLen, int err) {
        this.chain = new Spatial2DPVLBTree[chainLen];
        this.front = 0;
        this.rear = 0;
        this.err = err;
    }
    
    public void insert(Point2D point) {
        if (isNull()) {
            chain[rear] = new Spatial2DPVLBTree(point);
        } else {
            chain[rear] = chain[(rear + chain.length - 1) % chain.length].insert(point);
            
            if (isFull()) {
                front = (front + 1) % chain.length;
            }
        }
        rear = (rear + 1) % chain.length;
    }
    
    public Spatial2DPVLBTree file() {
        if (isFull()) {
            return chain[rear];
        }
        return null;
    }
    
    public Spatial2DPVLB_Res rangeQuery(Rectangle2D rect, int version, int currentVersion) {
        Spatial2DPVLBTree tree = getVersionTree(version, currentVersion);
        if (tree == null) {
            return new Spatial2DPVLB_Res(new ArrayList<>(), new ArrayList<>());
        }
        return tree.rectangleQuery(rect);
    }
    
    private Spatial2DPVLBTree getVersionTree(int version, int currentVersion) {
        int index = (rear - (currentVersion - version) + chain.length) % chain.length;
        return chain[index];
    }
    
    private boolean isNull() {
        return front == rear;
    }
    
    private boolean isFull() {
        return (rear + 1) % chain.length == front;
    }
}



