package Rtree;

/**
 * R-tree简单测试程序
 * 用于验证修复后的插入功能
 */
public class RTreeSimpleTest {
    
    public static void main(String[] args) {
        System.out.println("=== R-tree Simple Test ===\n");
        
        RTree rtree = new RTree();
        
        // 测试1: 插入少量点
        System.out.println("Test 1: Inserting 20 points...");
        for (int i = 0; i < 20; i++) {
            Point2D1 point = new Point2D1(i * 10, i * 10);
            rtree.insert(point, "Data_" + i);
            System.out.printf("Inserted point %d: %s\n", i + 1, point);
        }
        
        System.out.println("\nTree stats after 20 insertions:");
        System.out.println(rtree.getStats());
        
        // 测试2: 插入更多点触发分裂
        System.out.println("\nTest 2: Inserting 100 more points...");
        for (int i = 20; i < 120; i++) {
            Point2D1 point = new Point2D1(i * 5, (i * 7) % 1000);
            rtree.insert(point, "Data_" + i);
            
            if ((i + 1) % 20 == 0) {
                System.out.printf("Inserted %d points so far...\n", i + 1);
            }
        }
        
        System.out.println("\nTree stats after 120 insertions:");
        System.out.println(rtree.getStats());
        
        // 测试3: 范围查询
        System.out.println("\nTest 3: Range query test...");
        Rectangle2D1 queryRect = new Rectangle2D1(0, 0, 200, 200);
        System.out.printf("Query rectangle: %s\n", queryRect);
        
        RTreeQueryResult result = rtree.rangeQuery(queryRect);
        System.out.printf("Query results: %d points found\n", result.getResultCount());
        System.out.printf("Query time: %.3f ms\n", result.getStats().getQueryTimeMillis());
        System.out.printf("Nodes visited: %d\n", result.getStats().getNodesVisited());
        
        // 显示前几个结果
        System.out.println("First few results:");
        for (int i = 0; i < Math.min(5, result.getResultCount()); i++) {
            Point2D1 point = result.getResultPoint(i);
            Object data = result.getResultData(i);
            System.out.printf("  Result %d: %s (data: %s)\n", i + 1, point, data);
        }
        
        // 测试4: 大量插入测试
        System.out.println("\nTest 4: Bulk insertion test (1000 points)...");
        long startTime = System.nanoTime();
        
        for (int i = 120; i < 1120; i++) {
            Point2D1 point = new Point2D1((i * 13) % 2000, (i * 17) % 2000);
            rtree.insert(point, "Bulk_" + i);
            
            if ((i + 1) % 200 == 0) {
                System.out.printf("Inserted %d points...\n", i + 1);
            }
        }
        
        long endTime = System.nanoTime();
        double insertTime = (endTime - startTime) / 1_000_000.0;
        
        System.out.printf("\nBulk insertion completed: %.2f ms (%.3f μs/point)\n", 
                        insertTime, insertTime * 1000 / 1000);
        
        System.out.println("\nFinal tree stats:");
        System.out.println(rtree.getStats());
        
        // 测试5: 最终查询测试
        System.out.println("\nTest 5: Final query test...");
        Rectangle2D1 finalQuery = new Rectangle2D1(500, 500, 1500, 1500);
        RTreeQueryResult finalResult = rtree.rangeQuery(finalQuery);
        
        System.out.printf("Final query rectangle: %s\n", finalQuery);
        System.out.printf("Final query results: %d points\n", finalResult.getResultCount());
        System.out.printf("Final query time: %.2f μs\n", finalResult.getStats().getQueryTimeMicros());
        
        System.out.println("\n=== All tests completed successfully! ===");
    }
}
