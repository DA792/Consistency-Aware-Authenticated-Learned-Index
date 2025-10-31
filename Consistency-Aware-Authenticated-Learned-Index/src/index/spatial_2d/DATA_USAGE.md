# 使用真实数据集

## 数据集格式

项目提供的数据集位于 `src/data/uniform_10k.csv`，格式为：

```
x1 y1
x2 y2
x3 y3
...
```

每行包含一个二维点的坐标，用空格分隔。

### 示例数据
```
14995 19412
19857 17234
10893 6179
6453 18767
7836 8251
```

## 使用方法

### 方法1: 使用 RealDataExample（推荐）

这是专门为真实数据集创建的示例程序：

```bash
cd Consistency-Aware-Authenticated-Learned-Index/src
javac index/spatial_2d/*.java
java index.spatial_2d.RealDataExample
```

### 方法2: 在代码中加载数据

```java
import index.spatial_2d.*;
import java.util.List;

// 1. 加载数据
String dataPath = "src/data/uniform_10k.csv";
int loadCount = 100000;  // 加载10万个点，0表示加载全部
List<Point2D> points = DataLoader.loadFromCSV(dataPath, loadCount);

// 2. 分析数据
DataLoader.DataStats stats = DataLoader.analyzeData(points);
System.out.println(stats);

// 3. 构建索引
Spatial2DIndex index = new Spatial2DIndex(points, 64);

// 4. 生成适合该数据集的测试查询
List<Rectangle2D> queries = DataLoader.generateTestQueries(
    stats, 
    0.001,  // 选择率: 0.1%
    100     // 查询数量
);

// 5. 执行查询
for (Rectangle2D query : queries) {
    Spatial2DQueryResponse response = index.rectangleQuery(query);
    // ... 处理结果
}
```

## DataLoader 工具类

### 主要方法

#### 1. 加载数据
```java
// 加载指定数量
List<Point2D> points = DataLoader.loadFromCSV("path/to/file.csv", 10000);

// 加载全部数据
List<Point2D> points = DataLoader.loadFromCSV("path/to/file.csv");
```

#### 2. 分析数据统计
```java
DataLoader.DataStats stats = DataLoader.analyzeData(points);
System.out.println(stats);

// 输出示例:
// 数据统计:
//   数据量: 100000
//   X范围: [0, 19999] (跨度: 19999)
//   Y范围: [0, 19999] (跨度: 19999)
//   空间面积: 399960001
```

#### 3. 生成测试查询
```java
List<Rectangle2D> queries = DataLoader.generateTestQueries(
    stats,       // 数据统计信息
    0.001,       // 选择率（查询范围占总空间的比例）
    100          // 生成查询数量
);
```

### 选择率说明

选择率决定了查询矩形的大小：

| 选择率 | 查询范围 | 预期结果数量（对于均匀分布） |
|--------|----------|------------------------------|
| 0.0001 | 0.01%    | ~10个点（对于10万点数据）    |
| 0.001  | 0.1%     | ~100个点                     |
| 0.01   | 1%       | ~1000个点                    |
| 0.1    | 10%      | ~10000个点                   |

## 数据集统计

### uniform_10k.csv
- **数据量**: 约100万个点
- **分布**: 均匀分布
- **坐标范围**: 0-20000 (X和Y)
- **适用场景**: 测试索引在均匀分布数据上的性能

## 性能测试

### 测试不同数据量

```java
int[] dataSizes = {10000, 50000, 100000, 500000, 1000000};

for (int size : dataSizes) {
    List<Point2D> points = DataLoader.loadFromCSV(dataPath, size);
    
    long start = System.nanoTime();
    Spatial2DIndex index = new Spatial2DIndex(points, 64);
    long buildTime = System.nanoTime() - start;
    
    System.out.println("数据量: " + size);
    System.out.println("构建时间: " + buildTime / 1000000.0 + " ms");
    System.out.println("平均每点: " + (buildTime / size) + " ns\n");
}
```

### 测试不同查询选择率

```java
DataLoader.DataStats stats = DataLoader.analyzeData(points);
double[] selectivities = {0.0001, 0.001, 0.01, 0.1};

for (double sel : selectivities) {
    List<Rectangle2D> queries = DataLoader.generateTestQueries(stats, sel, 100);
    
    // 执行查询并统计
    long totalTime = 0;
    for (Rectangle2D query : queries) {
        long start = System.nanoTime();
        Spatial2DQueryResponse response = index.rectangleQuery(query);
        totalTime += System.nanoTime() - start;
    }
    
    System.out.println("选择率: " + sel);
    System.out.println("平均查询时间: " + (totalTime / 100) / 1000000.0 + " ms\n");
}
```

## 添加自己的数据集

### 1. 准备CSV文件

确保文件格式为：
```
x1 y1
x2 y2
...
```

### 2. 放置文件

将文件放在 `src/data/` 目录下

### 3. 加载数据

```java
String myDataPath = "src/data/my_data.csv";
List<Point2D> points = DataLoader.loadFromCSV(myDataPath);
```

## 常见问题

### Q1: 数据加载很慢怎么办？
**A**: 使用 `loadFromCSV(path, maxCount)` 只加载部分数据进行测试。

### Q2: 如何知道应该使用多大的errorBound？
**A**: 
- 小数据集（< 10万）: 32-64
- 中等数据集（10-100万）: 64-128
- 大数据集（> 100万）: 128-256

### Q3: 查询太慢怎么办？
**A**: 
1. 减小查询选择率
2. 增大errorBound（降低精度换取速度）
3. 使用更小的数据集测试

### Q4: 内存不够怎么办？
**A**: 减少加载的数据量，或增加JVM堆内存：
```bash
java -Xmx4g index.spatial_2d.RealDataExample
```

## 完整示例输出

运行 `RealDataExample` 的预期输出：

```
===== 使用真实数据集的二维空间索引 =====

1. 加载数据集
成功加载 100000 个数据点

2. 数据集分析
数据统计:
  数据量: 100000
  X范围: [0, 19999] (跨度: 19999)
  Y范围: [0, 19999] (跨度: 19999)
  空间面积: 399960001

3. 构建二维索引
误差边界: 64
索引构建完成
构建时间: 523.45 ms
平均每个点: 5234 ns

4. 生成测试查询

--- 选择率: 0.01% ---
生成查询矩形: 选择率=1.0E-4, 边长≈63
测试查询数量: 100
平均查询时间: 2.34 ms
平均验证时间: 1.56 ms
平均结果数量: 10
平均候选数量: 12
平均假阳性: 2 (16.67%)
平均VO大小: 8.45 KB

...

===== 测试完成 =====
```


