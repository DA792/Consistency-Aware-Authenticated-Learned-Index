package index.spatial_2d;

/**
 * 二维矩形数据结构
 */
public class Rectangle2D {
    public final long minX, minY, maxX, maxY;
    
    public Rectangle2D(long minX, long minY, long maxX, long maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
    
    /**
     * 检查点是否在矩形内
     */
    public boolean contains(Point2D point) {
        return point.x >= minX && point.x <= maxX && 
               point.y >= minY && point.y <= maxY;
    }
    
    /**
     * 检查两个矩形是否相交
     */
    public boolean intersects(Rectangle2D other) {
        return !(maxX < other.minX || minX > other.maxX ||
                 maxY < other.minY || minY > other.maxY);
    }
    
    /**
     * 计算矩形面积
     */
    public long area() {
        return (maxX - minX + 1) * (maxY - minY + 1);
    }
    
    @Override
    public String toString() {
        return String.format("[(%d,%d) to (%d,%d)]", minX, minY, maxX, maxY);
    }
}

