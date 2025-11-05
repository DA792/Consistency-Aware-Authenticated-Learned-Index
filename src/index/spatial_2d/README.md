# 二维空间索引扩展

这个包实现了基于Z-order曲线的二维空间索引，完全复用现有的PVL树认证学习索引。

## 核心组件

### 1. 数据结构
- **Point2D.java** - 二维点，自动计算Z-order值
- **Rectangle2D.java** - 二维矩形，用于范围查询

### 2. Z-order转换
- **ZOrderCurve.java** - Z-order曲线编码/解码
  - `encode(x, y)` - 将二维坐标转换为一维Z值
  - `decode(zValue)` - 将Z值还原为二维坐标

### 3. 查询分解
- **ZOrderDecomposition.java** - 精确的Z-order区间分解算法
  - 实现查询区域的智能分割
  - 减少假阳性数据
  - 支持空间覆盖完整性检查

### 4. 主索引类
- **Spatial2DIndex.java** - 二维空间索引主类
  - 构建索引：`new Spatial2DIndex(points, errorBound)`
  - 查询：`rectangleQuery(queryRect)`
  - 验证：`verify(queryRect, response)`

### 5. 查询结果
- **Spatial2DQueryResult.java** - 单个区间的查询结果
- **Spatial2DQueryResponse.java** - 完整的查询响应，包含统计信息

### 6. 示例
- **Spatial2DExample.java** - 完整的使用示例

## 使用方法

### 1. 准备数据
```java
List<Point2D> points = new ArrayList<>();
points.add(new Point2D(100, 200));
points.add(new Point2D(150, 250));
// ... 添加更多点
```

### 2. 构建索引
```java
int errorBound = 64;  // 学习模型的误差边界
Spatial2DIndex index = new Spatial2DIndex(points, errorBound);
```

### 3. 执行查询
```java
// 定义查询矩形
Rectangle2D queryRect = new Rectangle2D(100, 100, 200, 200);

// 执行查询
Spatial2DQueryResponse response = index.rectangleQuery(queryRect);

// 获取结果
List<Point2D> results = response.results;
System.out.println("找到 " + results.size() + " 个点");

// 查看统计信息
Spatial2DQueryResponse.QueryStats stats = response.getStats();
System.out.println(stats);
```

### 4. 验证结果
```java
boolean isValid = index.verify(queryRect, response);
if (isValid) {
    System.out.println("查询结果验证通过！");
} else {
    System.out.println("查询结果验证失败！");
}
```

## 查询流程

```
输入: 二维矩形 [minX, minY, maxX, maxY]
  ↓
步骤1: Z-order区间分解
  将矩形分解为多个连续的Z值区间
  ↓
步骤2: 一维查询 (复用现有PVL树算法)
  对每个Z值区间执行一维范围查询
  ↓
步骤3: 二维过滤
  将Z值转换回二维点，过滤假阳性
  ↓
输出: 矩形内的所有点
```

## 验证机制

验证分为两个层次：

### 1. 一维验证
- 对每个Z-order区间使用现有的 `PVLTree.verify()`
- 验证数据的完整性和真实性
- 检查哈希链的一致性

### 2. 二维验证
- 重新计算Z-order区间分解
- 验证区间数量和范围的一致性
- 检查二维结果的完整性

## 性能特点

### 优势
1. **完全复用现有代码** - 不修改任何PVL树代码
2. **高效查询** - 利用学习模型预测位置
3. **低假阳性** - 精确的区间分解算法
4. **完整认证** - 保持现有的安全保证

### 复杂度
- **构建时间**: O(n log n) - 与一维情况相同
- **查询时间**: O(k * (log n + error_bound)) - k是区间数量
- **验证时间**: O(k * (log n + result_size))
- **空间开销**: O(n) - 额外的Z值映射表

## 运行示例

```bash
# 编译
javac -cp . index/spatial_2d/*.java

# 运行示例
java index.spatial_2d.Spatial2DExample
```

## 扩展性

这个设计可以轻松扩展到：
- 三维或更高维空间（修改Z-order编码）
- 不同的空间填充曲线（如Hilbert曲线）
- 其他类型的空间查询（KNN、多边形查询等）

## 注意事项

1. **坐标范围**: 当前支持32位坐标（0到2^32-1）
2. **误差边界**: 建议使用64-128之间的值
3. **数据分布**: 聚集分布的数据会获得更好的性能
4. **并发安全**: 当前实现不是线程安全的

## 与现有系统的集成

可以将此二维索引集成到现有的三种索引结构中：

1. **PVL-tree扩展**: `Spatial2DPVLTree`
2. **PVLB-tree扩展**: `Spatial2DPVLBTree`
3. **HPVL-tree扩展**: `Spatial2DHPVLTree`

每种扩展都保持原有索引的特性（查询优化/更新优化/混合优化）。

