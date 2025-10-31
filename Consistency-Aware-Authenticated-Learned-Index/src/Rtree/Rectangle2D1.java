package Rtree;

/**
 * R-tree专用的二维矩形数据结构
 * 独立于utils包，专门为R-tree设计
 */
public class Rectangle2D1 {
    public final long minX, minY, maxX, maxY;
    
    public Rectangle2D1(long minX, long minY, long maxX, long maxY) {
        // 确保坐标顺序正确
        this.minX = Math.min(minX, maxX);
        this.minY = Math.min(minY, maxY);
        this.maxX = Math.max(minX, maxX);
        this.maxY = Math.max(minY, maxY);
    }
    
    /**
     * 检查点是否在矩形内
     */
    public boolean contains(Point2D1 point) {
        return point.x >= minX && point.x <= maxX && 
               point.y >= minY && point.y <= maxY;
    }
    
    /**
     * 检查两个矩形是否相交
     */
    public boolean intersects(Rectangle2D1 other) {
        return !(maxX < other.minX || minX > other.maxX ||
                 maxY < other.minY || minY > other.maxY);
    }
    
    /**
     * 检查此矩形是否完全包含另一个矩形
     */
    public boolean contains(Rectangle2D1 other) {
        return minX <= other.minX && minY <= other.minY &&
               maxX >= other.maxX && maxY >= other.maxY;
    }
    
    /**
     * 计算矩形面积
     */
    public long area() {
        return (maxX - minX + 1) * (maxY - minY + 1);
    }
    
    /**
     * 计算矩形周长
     */
    public long perimeter() {
        return 2 * ((maxX - minX) + (maxY - minY));
    }
    
    /**
     * 获取矩形宽度
     */
    public long width() {
        return maxX - minX;
    }
    
    /**
     * 获取矩形高度
     */
    public long height() {
        return maxY - minY;
    }
    
    /**
     * 获取矩形中心点
     */
    public Point2D1 center() {
        return new Point2D1((minX + maxX) / 2, (minY + maxY) / 2);
    }
    
    /**
     * 扩展矩形以包含指定点
     */
    public Rectangle2D1 expandToInclude(Point2D1 point) {
        return new Rectangle2D1(
            Math.min(minX, point.x),
            Math.min(minY, point.y),
            Math.max(maxX, point.x),
            Math.max(maxY, point.y)
        );
    }
    
    /**
     * 扩展矩形以包含另一个矩形
     */
    public Rectangle2D1 expandToInclude(Rectangle2D1 other) {
        return new Rectangle2D1(
            Math.min(minX, other.minX),
            Math.min(minY, other.minY),
            Math.max(maxX, other.maxX),
            Math.max(maxY, other.maxY)
        );
    }
    
    /**
     * 计算与另一个矩形的重叠面积
     */
    public long overlapArea(Rectangle2D1 other) {
        if (!intersects(other)) {
            return 0;
        }
        
        long overlapMinX = Math.max(minX, other.minX);
        long overlapMinY = Math.max(minY, other.minY);
        long overlapMaxX = Math.min(maxX, other.maxX);
        long overlapMaxY = Math.min(maxY, other.maxY);
        
        return (overlapMaxX - overlapMinX + 1) * (overlapMaxY - overlapMinY + 1);
    }
    
    /**
     * 计算包含此矩形和另一个矩形的最小矩形的面积增长
     */
    public long areaGrowthToInclude(Rectangle2D1 other) {
        Rectangle2D1 expanded = expandToInclude(other);
        return expanded.area() - this.area();
    }
    
    /**
     * 计算到点的最小距离的平方
     */
    public long minDistanceSquaredTo(Point2D1 point) {
        long dx = 0, dy = 0;
        
        if (point.x < minX) dx = minX - point.x;
        else if (point.x > maxX) dx = point.x - maxX;
        
        if (point.y < minY) dy = minY - point.y;
        else if (point.y > maxY) dy = point.y - maxY;
        
        return dx * dx + dy * dy;
    }
    
    /**
     * 检查矩形是否为空（面积为0）
     */
    public boolean isEmpty() {
        return minX > maxX || minY > maxY;
    }
    
    /**
     * 检查矩形是否为点（宽度和高度都为0）
     */
    public boolean isPoint() {
        return minX == maxX && minY == maxY;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Rectangle2D1)) return false;
        Rectangle2D1 other = (Rectangle2D1) obj;
        return minX == other.minX && minY == other.minY &&
               maxX == other.maxX && maxY == other.maxY;
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(minX) ^ Long.hashCode(minY) ^ 
               Long.hashCode(maxX) ^ Long.hashCode(maxY);
    }
    
    @Override
    public String toString() {
        return String.format("[(%d,%d) to (%d,%d)]", minX, minY, maxX, maxY);
    }
    
    /**
     * 从字符串解析矩形
     * 格式: "minX,minY,maxX,maxY" 或 "[(minX,minY) to (maxX,maxY)]"
     */
    public static Rectangle2D1 parse(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Rectangle string cannot be null or empty");
        }
        
        String cleaned = str.trim().replaceAll("[\\[\\]()to\\s]", "");
        String[] parts = cleaned.split(",");
        
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid rectangle format: " + str);
        }
        
        try {
            long minX = Long.parseLong(parts[0].trim());
            long minY = Long.parseLong(parts[1].trim());
            long maxX = Long.parseLong(parts[2].trim());
            long maxY = Long.parseLong(parts[3].trim());
            return new Rectangle2D1(minX, minY, maxX, maxY);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in rectangle: " + str, e);
        }
    }
    
    /**
     * 创建空矩形
     */
    public static Rectangle2D1 empty() {
        return new Rectangle2D1(1, 1, 0, 0); // minX > maxX 表示空矩形
    }
}


