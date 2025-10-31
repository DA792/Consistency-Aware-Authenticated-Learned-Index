package utils;

/**
 * Z-order曲线转换工具类
 */
public class ZOrderCurve {
    
    /**
     * 将二维坐标转换为Z-order值
     * @param x X坐标
     * @param y Y坐标
     * @return Z-order值
     */
    public static long encode(long x, long y) {
        return interleave(x, y);
    }
    
    /**
     * 将Z-order值解码为二维坐标
     * @param zValue Z-order值
     * @return Point2D对象
     */
    public static Point2D decode(long zValue) {
        long[] coords = deinterleave(zValue);
        return new Point2D(coords[0], coords[1], zValue);
    }
    
    /**
     * 交错位操作 - 核心编码算法
     * 将x和y的位交错组合成Z-order值
     */
    private static long interleave(long x, long y) {
        x = (x | (x << 16)) & 0x0000FFFF0000FFFFL;
        x = (x | (x << 8))  & 0x00FF00FF00FF00FFL;
        x = (x | (x << 4))  & 0x0F0F0F0F0F0F0F0FL;
        x = (x | (x << 2))  & 0x3333333333333333L;
        x = (x | (x << 1))  & 0x5555555555555555L;
        
        y = (y | (y << 16)) & 0x0000FFFF0000FFFFL;
        y = (y | (y << 8))  & 0x00FF00FF00FF00FFL;
        y = (y | (y << 4))  & 0x0F0F0F0F0F0F0F0FL;
        y = (y | (y << 2))  & 0x3333333333333333L;
        y = (y | (y << 1))  & 0x5555555555555555L;
        
        return x | (y << 1);
    }
    
    /**
     * 反交错位操作 - 核心解码算法
     * 从Z-order值中分离出x和y坐标
     */
    private static long[] deinterleave(long z) {
        long x = z & 0x5555555555555555L;
        long y = (z >> 1) & 0x5555555555555555L;
        
        x = (x | (x >> 1))  & 0x3333333333333333L;
        x = (x | (x >> 2))  & 0x0F0F0F0F0F0F0F0FL;
        x = (x | (x >> 4))  & 0x00FF00FF00FF00FFL;
        x = (x | (x >> 8))  & 0x0000FFFF0000FFFFL;
        x = (x | (x >> 16)) & 0x00000000FFFFFFFFL;
        
        y = (y | (y >> 1))  & 0x3333333333333333L;
        y = (y | (y >> 2))  & 0x0F0F0F0F0F0F0F0FL;
        y = (y | (y >> 4))  & 0x00FF00FF00FF00FFL;
        y = (y | (y >> 8))  & 0x0000FFFF0000FFFFL;
        y = (y | (y >> 16)) & 0x00000000FFFFFFFFL;
        
        return new long[]{x, y};
    }
}



