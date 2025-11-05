# R-tree 二维空间索引实现

这是一个简单的 R-tree 实现，支持二维范围查询，不包含验证功能。

## 文件结构

- `RTree.java` - 主要的 R-tree 类，实现插入和查询功能
- `RTreeNode.java` - R-tree 节点类，支持内部节点和叶子节点
- `RTreeEntry.java` - R-tree 条目类，表示数据点或子节点引用
- `RTreeQueryResult.java` - 查询结果类，包含结果和性能统计
- `RTreeTest.java` - 测试类，演示基本功能和性能测试
- `run_rtree_test.bat` - 编译和运行脚本

## 主要功能

### 1. 插入操作
```java
RTree rtree = new RTree();
rtree.insert(new Point2D(10, 20));           // 插入点
rtree.insert(new Point2D(30, 40), "数据");    // 插入点和关联数据
```

### 2. 范围查询
```java
Rectangle2D queryRect = new Rectangle2D(0, 0, 50, 50);
RTreeQueryResult result = rtree.rangeQuery(queryRect);

System.out.println("查询结果: " + result.getResultCount() + " 个点");
System.out.println("查询时间: " + result.getStats().getQueryTimeMillis() + " ms");
```

### 3. 点查询
```java
Point2D queryPoint = new Point2D(30, 40);
RTreeQueryResult result = rtree.pointQuery(queryPoint);
```

### 4. 树统计信息
```java
RTree.TreeStats stats = rtree.getStats();
System.out.println("树高度: " + stats.height);
System.out.println("节点数: " + stats.totalNodes);
System.out.println("叶子节点数: " + stats.leafNodes);
```

## 算法特性

- **分裂策略**: 使用线性分裂算法，选择最远的两个条目作为种子
- **插入策略**: 选择面积增长最小的路径插入新条目
- **节点容量**: 最大 10 个条目，最小 4 个条目
- **查询优化**: 只访问与查询矩形相交的节点

## 性能特点

- **插入时间复杂度**: O(log n) 平均情况
- **查询时间复杂度**: O(log n + k)，其中 k 是结果数量
- **空间复杂度**: O(n)
- **适用场景**: 二维点数据的范围查询

## 编译和运行

### Windows
```batch
run_rtree_test.bat
```

### 手动编译
```bash
# 编译工具类
javac -cp ".;jars\*" -d bin src\utils\*.java

# 编译R-tree类
javac -cp ".;jars\*" -d bin Rtree\*.java

# 运行测试
java -cp bin;jars\* Rtree.RTreeTest
```

## 测试内容

测试程序包含以下内容：

1. **基本功能测试**: 插入点、范围查询、点查询
2. **性能测试**: 不同数据量下的插入和查询性能
3. **真实数据测试**: 使用项目中的 CSV 数据集进行测试

## 依赖项

- 依赖 `utils` 包中的 `Point2D`、`Rectangle2D`、`DataLoader` 类
- 需要 Java 8 或更高版本

## 注意事项

- 这是一个简化的 R-tree 实现，主要用于教学和演示
- 不包含删除操作
- 不包含任何验证或认证功能
- 适合处理中等规模的二维点数据（几万到几十万个点）

## 扩展可能

- 添加删除操作
- 实现 R*-tree 的优化分裂策略
- 支持矩形数据（不仅仅是点）
- 添加最近邻查询
- 支持动态调整节点容量
