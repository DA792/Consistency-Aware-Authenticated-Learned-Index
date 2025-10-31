package utils;

/**
 * 二维点数据结构
 */
public class Point2D {
    public final long x;
    public final long y;
    public final long zValue; // 缓存的Z-order值
    
    public Point2D(long x, long y) {
        this.x = x;
        this.y = y;
        this.zValue = ZOrderCurve.encode(x, y);
    }
    
    public Point2D(long x, long y, long zValue) {
        this.x = x;
        this.y = y;
        this.zValue = zValue;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point2D)) return false;
        Point2D other = (Point2D) obj;
        return x == other.x && y == other.y;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(x) ^ Long.hashCode(y);
    }
    
    @Override
    public String toString() {
        return String.format("(%d, %d)", x, y);
    }
}



