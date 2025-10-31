package Rtree;

/**
 * R-tree条目类
 * 可以表示叶子节点中的数据点或内部节点中的子节点引用
 */
public class RTreeEntry {
    private Rectangle2D1 mbr;        // 最小边界矩形
    private Point2D1 dataPoint;     // 数据点（仅叶子节点使用）
    private RTreeNode child;       // 子节点（仅内部节点使用）
    private Object data;           // 附加数据
    
    /**
     * 创建叶子节点条目（包含数据点）
     */
    public RTreeEntry(Point2D1 dataPoint) {
        this.dataPoint = dataPoint;
        this.mbr = new Rectangle2D1(dataPoint.x, dataPoint.y, dataPoint.x, dataPoint.y);
        this.child = null;
        this.data = null;
    }
    
    /**
     * 创建叶子节点条目（包含数据点和附加数据）
     */
    public RTreeEntry(Point2D1 dataPoint, Object data) {
        this.dataPoint = dataPoint;
        this.mbr = new Rectangle2D1(dataPoint.x, dataPoint.y, dataPoint.x, dataPoint.y);
        this.child = null;
        this.data = data;
    }
    
    /**
     * 创建内部节点条目（包含子节点引用）
     */
    public RTreeEntry(RTreeNode child) {
        this.child = child;
        this.mbr = child.getMBR();
        this.dataPoint = null;
        this.data = null;
    }
    
    /**
     * 创建带有指定MBR的条目
     */
    public RTreeEntry(Rectangle2D1 mbr, RTreeNode child) {
        this.mbr = mbr;
        this.child = child;
        this.dataPoint = null;
        this.data = null;
    }
    
    /**
     * 检查条目是否为叶子条目（包含数据点）
     */
    public boolean isLeafEntry() {
        return dataPoint != null;
    }
    
    /**
     * 检查条目是否为内部节点条目（包含子节点）
     */
    public boolean isInternalEntry() {
        return child != null;
    }
    
    /**
     * 更新MBR（通常在子节点发生变化时调用）
     */
    public void updateMBR() {
        if (child != null) {
            this.mbr = child.getMBR();
        }
    }
    
    /**
     * 计算添加此条目到指定矩形后的面积增长
     */
    public long calculateAreaGrowth(Rectangle2D1 rect) {
        if (rect == null) {
            return mbr.area();
        }
        
        long originalArea = rect.area();
        
        // 计算扩展后的矩形
        long minX = Math.min(rect.minX, mbr.minX);
        long minY = Math.min(rect.minY, mbr.minY);
        long maxX = Math.max(rect.maxX, mbr.maxX);
        long maxY = Math.max(rect.maxY, mbr.maxY);
        
        long expandedArea = (maxX - minX + 1) * (maxY - minY + 1);
        return expandedArea - originalArea;
    }
    
    /**
     * 计算此条目与指定矩形的重叠面积
     */
    public long calculateOverlapArea(Rectangle2D1 rect) {
        if (rect == null || !mbr.intersects(rect)) {
            return 0;
        }
        
        long overlapMinX = Math.max(mbr.minX, rect.minX);
        long overlapMinY = Math.max(mbr.minY, rect.minY);
        long overlapMaxX = Math.min(mbr.maxX, rect.maxX);
        long overlapMaxY = Math.min(mbr.maxY, rect.maxY);
        
        if (overlapMinX <= overlapMaxX && overlapMinY <= overlapMaxY) {
            return (overlapMaxX - overlapMinX + 1) * (overlapMaxY - overlapMinY + 1);
        }
        
        return 0;
    }
    
    /**
     * 检查条目是否与查询矩形相交
     */
    public boolean intersects(Rectangle2D1 queryRect) {
        return mbr.intersects(queryRect);
    }
    
    /**
     * 检查条目是否包含指定点
     */
    public boolean contains(Point2D1 point) {
        return mbr.contains(point);
    }
    
    /**
     * 计算条目中心到指定点的距离的平方
     */
    public long distanceSquaredTo(Point2D1 point) {
        long centerX = (mbr.minX + mbr.maxX) / 2;
        long centerY = (mbr.minY + mbr.maxY) / 2;
        
        long dx = centerX - point.x;
        long dy = centerY - point.y;
        
        return dx * dx + dy * dy;
    }
    
    // Getters and Setters
    public Rectangle2D1 getMBR() {
        return mbr;
    }
    
    public void setMBR(Rectangle2D1 mbr) {
        this.mbr = mbr;
    }
    
    public Point2D1 getDataPoint() {
        return dataPoint;
    }
    
    public RTreeNode getChild() {
        return child;
    }
    
    public void setChild(RTreeNode child) {
        this.child = child;
        if (child != null) {
            updateMBR();
        }
    }
    
    public Object getData() {
        return data;
    }
    
    public void setData(Object data) {
        this.data = data;
    }
    
    @Override
    public String toString() {
        if (isLeafEntry()) {
            return String.format("LeafEntry[point=%s, data=%s]", dataPoint, data);
        } else {
            return String.format("InternalEntry[mbr=%s, child=%s]", mbr, 
                               child != null ? "Node" : "null");
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RTreeEntry)) return false;
        
        RTreeEntry other = (RTreeEntry) obj;
        
        if (isLeafEntry() && other.isLeafEntry()) {
            return dataPoint.equals(other.dataPoint);
        } else if (isInternalEntry() && other.isInternalEntry()) {
            return child == other.child;
        }
        
        return false;
    }
    
    @Override
    public int hashCode() {
        if (isLeafEntry()) {
            return dataPoint.hashCode();
        } else if (isInternalEntry()) {
            return System.identityHashCode(child);
        }
        return 0;
    }
}
