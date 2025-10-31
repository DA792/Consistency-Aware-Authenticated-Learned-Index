package Rtree;

import java.util.ArrayList;
import java.util.List;

/**
 * R-tree查询结果类
 * 包含查询结果和性能统计信息
 */
public class RTreeQueryResult {
    private List<Point2D1> resultPoints;      // 查询结果点
    private List<Object> resultData;         // 对应的附加数据
    private QueryStats stats;                // 查询统计信息
    
    public RTreeQueryResult() {
        this.resultPoints = new ArrayList<>();
        this.resultData = new ArrayList<>();
        this.stats = new QueryStats();
    }
    
    /**
     * 添加查询结果点
     */
    public void addResult(Point2D1 point) {
        resultPoints.add(point);
        resultData.add(null);
    }
    
    /**
     * 添加查询结果点和对应数据
     */
    public void addResult(Point2D1 point, Object data) {
        resultPoints.add(point);
        resultData.add(data);
    }
    
    /**
     * 添加查询结果条目
     */
    public void addResult(RTreeEntry entry) {
        if (entry.isLeafEntry()) {
            resultPoints.add(entry.getDataPoint());
            resultData.add(entry.getData());
        }
    }
    
    /**
     * 获取结果数量
     */
    public int getResultCount() {
        return resultPoints.size();
    }
    
    /**
     * 检查是否有结果
     */
    public boolean hasResults() {
        return !resultPoints.isEmpty();
    }
    
    /**
     * 清空结果
     */
    public void clear() {
        resultPoints.clear();
        resultData.clear();
        stats.reset();
    }
    
    /**
     * 获取指定索引的结果点
     */
    public Point2D1 getResultPoint(int index) {
        if (index >= 0 && index < resultPoints.size()) {
            return resultPoints.get(index);
        }
        return null;
    }
    
    /**
     * 获取指定索引的结果数据
     */
    public Object getResultData(int index) {
        if (index >= 0 && index < resultData.size()) {
            return resultData.get(index);
        }
        return null;
    }
    
    // Getters
    public List<Point2D1> getResultPoints() {
        return new ArrayList<>(resultPoints);
    }
    
    public List<Object> getResultData() {
        return new ArrayList<>(resultData);
    }
    
    public QueryStats getStats() {
        return stats;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("RTreeQueryResult[\n");
        sb.append("  Results: ").append(resultPoints.size()).append("\n");
        sb.append("  Stats: ").append(stats).append("\n");
        
        if (resultPoints.size() <= 10) {
            sb.append("  Points: ");
            for (int i = 0; i < resultPoints.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(resultPoints.get(i));
            }
            sb.append("\n");
        } else {
            sb.append("  Points: ").append(resultPoints.size()).append(" points (showing first 5)\n");
            sb.append("  First 5: ");
            for (int i = 0; i < Math.min(5, resultPoints.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(resultPoints.get(i));
            }
            sb.append("...\n");
        }
        
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 查询统计信息类
     */
    public static class QueryStats {
        private long queryStartTime;
        private long queryEndTime;
        private int nodesVisited;
        private int leavesVisited;
        private int entriesChecked;
        private Rectangle2D1 queryRegion;
        
        public QueryStats() {
            reset();
        }
        
        /**
         * 开始查询计时
         */
        public void startQuery(Rectangle2D1 queryRegion) {
            this.queryStartTime = System.nanoTime();
            this.queryRegion = queryRegion;
            this.nodesVisited = 0;
            this.leavesVisited = 0;
            this.entriesChecked = 0;
        }
        
        /**
         * 结束查询计时
         */
        public void endQuery() {
            this.queryEndTime = System.nanoTime();
        }
        
        /**
         * 记录访问节点
         */
        public void visitNode() {
            nodesVisited++;
        }
        
        /**
         * 记录访问叶子节点
         */
        public void visitLeaf() {
            leavesVisited++;
        }
        
        /**
         * 记录检查条目
         */
        public void checkEntry() {
            entriesChecked++;
        }
        
        /**
         * 重置统计信息
         */
        public void reset() {
            queryStartTime = 0;
            queryEndTime = 0;
            nodesVisited = 0;
            leavesVisited = 0;
            entriesChecked = 0;
            queryRegion = null;
        }
        
        /**
         * 获取查询时间（纳秒）
         */
        public long getQueryTimeNanos() {
            return queryEndTime - queryStartTime;
        }
        
        /**
         * 获取查询时间（毫秒）
         */
        public double getQueryTimeMillis() {
            return (queryEndTime - queryStartTime) / 1_000_000.0;
        }
        
        /**
         * 获取查询时间（微秒）
         */
        public double getQueryTimeMicros() {
            return (queryEndTime - queryStartTime) / 1_000.0;
        }
        
        // Getters
        public int getNodesVisited() {
            return nodesVisited;
        }
        
        public int getLeavesVisited() {
            return leavesVisited;
        }
        
        public int getEntriesChecked() {
            return entriesChecked;
        }
        
        public Rectangle2D1 getQueryRegion() {
            return queryRegion;
        }
        
        @Override
        public String toString() {
            return String.format("QueryStats[time=%.3fms, nodes=%d, leaves=%d, entries=%d]",
                               getQueryTimeMillis(), nodesVisited, leavesVisited, entriesChecked);
        }
    }
}
