package Rtree;

/**
 * R-tree专用的二维点数据结构
 * 独立于utils包，专门为R-tree设计
 */
public class Point2D1 {
    public final long x;
    public final long y;
    
    public Point2D1(long x, long y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * 计算到另一个点的距离的平方
     */
    public long distanceSquaredTo(Point2D1 other) {
        long dx = this.x - other.x;
        long dy = this.y - other.y;
        return dx * dx + dy * dy;
    }
    
    /**
     * 计算到另一个点的曼哈顿距离
     */
    public long manhattanDistanceTo(Point2D1 other) {
        return Math.abs(this.x - other.x) + Math.abs(this.y - other.y);
    }
    
    /**
     * 检查点是否在指定的矩形范围内
     */
    public boolean isInRectangle(long minX, long minY, long maxX, long maxY) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY;
    }
    
    /**
     * 创建包含此点的最小矩形（点矩形）
     */
    public Rectangle2D1 toRectangle() {
        return new Rectangle2D1(x, y, x, y);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Point2D1)) return false;
        Point2D1 other = (Point2D1) obj;
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
    
    /**
     * 从字符串解析点坐标
     * 格式: "x,y" 或 "(x,y)"
     */
    public static Point2D1 parse(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Point string cannot be null or empty");
        }
        
        String cleaned = str.trim().replaceAll("[()\\s]", "");
        String[] parts = cleaned.split(",");
        
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid point format: " + str);
        }
        
        try {
            long x = Long.parseLong(parts[0].trim());
            long y = Long.parseLong(parts[1].trim());
            return new Point2D1(x, y);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in point: " + str, e);
        }
    }
    
    /**
     * 创建原点
     */
    public static Point2D1 origin() {
        return new Point2D1(0, 0);
    }
    
    /**
     * 创建随机点（在指定范围内）
     */
    public static Point2D1 random(long minX, long minY, long maxX, long maxY) {
        long x = minX + (long) (Math.random() * (maxX - minX + 1));
        long y = minY + (long) (Math.random() * (maxY - minY + 1));
        return new Point2D1(x, y);
    }
}
