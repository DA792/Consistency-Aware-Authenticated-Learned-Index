package index.spatial_2d_pvl;

import utils.*;
import java.util.*;

/**
 * 二维PVL树版本链 - 对应PVLTreeChain
 */
public class Spatial2DPVLTreeChain {
    static int err;
    Spatial2DPVLTree[] chain;
    int front, rear;
    int currentVersion;
    private Map<Long, Point2D> globalZToPoint;
    
    public Spatial2DPVLTreeChain(int chainLen, int err) {
        this.chain = new Spatial2DPVLTree[chainLen];
        this.currentVersion = 0;
        this.front = this.rear = 0;
        Spatial2DPVLTreeChain.err = err;
        this.globalZToPoint = new HashMap<>();
    }
    
    public boolean isNull() {
        return front == rear;
    }
    
    public boolean isFull() {
        return (rear + 1) % chain.length == front;
    }
    
    public void insert(Point2D point) {
        globalZToPoint.put(point.zValue, point);
        
        if (isNull()) {
            List<Point2D> points = new ArrayList<>();
            points.add(point);
            chain[rear] = new Spatial2DPVLTree(points, err);
        } else {
            List<Point2D> allPoints = new ArrayList<>(globalZToPoint.values());
            chain[rear] = new Spatial2DPVLTree(allPoints, err);
            
            if (isFull()) {
                front = (front + 1) % chain.length;
            }
        }
        
        rear = (rear + 1) % chain.length;
        currentVersion++;
    }
    
    public void insertBatch(List<Point2D> points) {
        for (Point2D point : points) {
            globalZToPoint.put(point.zValue, point);
        }
        
        if (isNull()) {
            chain[rear] = new Spatial2DPVLTree(points, err);
            rear = (rear + 1) % chain.length;
            currentVersion = points.size();
        }
    }
    
    public Spatial2DPVLTree getVersionTree(int version) {
        int index = (rear - (currentVersion - version) + chain.length) % chain.length;
        return chain[index];
    }
    
    public Spatial2DPVL_Res rangeQuery(Rectangle2D queryRect, int version) {
        Spatial2DPVLTree tree = getVersionTree(version);
        if (tree == null) {
            return new Spatial2DPVL_Res(new ArrayList<>(), new ArrayList<>());
        }
        return tree.rectangleQuery(queryRect);
    }
    
    public Spatial2DPVL_Res rangeQuery(Rectangle2D queryRect) {
        return rangeQuery(queryRect, currentVersion);
    }
    
    public boolean verify(Rectangle2D queryRect, int version, Spatial2DPVL_Res response) {
        Spatial2DPVLTree tree = getVersionTree(version);
        if (tree == null) {
            return false;
        }
        return tree.verify(queryRect, response);
    }
    
    public int getCurrentVersion() {
        return currentVersion;
    }
    
    public void getIndexSize() {
        System.out.println("二维PVL索引链统计:");
        System.out.println("  链长度: " + chain.length);
        System.out.println("  当前版本: " + currentVersion);
        System.out.println("  全局点映射: " + globalZToPoint.size() + " 个点");
    }
}


