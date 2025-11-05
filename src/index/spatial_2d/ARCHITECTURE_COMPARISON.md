# 架构对比：一维 vs 二维索引

## 完整架构对照表

### 一维索引 (PVL_tree_index)

```
PVLTreeChain.java           ← 版本链管理
├── PVLTree[] chain         ← 存储多个版本
├── insert(key)             ← 动态插入
├── rangeQuery(low, high, version)  ← 版本化查询
└── verify(tree, low, high, res)    ← 验证

PVLTree.java                ← 学习型认证树
├── PVLNode root            ← 树根
├── buildLearnedTree()      ← 使用OptPLA构建
├── rangeQuery()            ← 一维范围查询
├── verify()                ← 认证验证
└── update()                ← 更新树结构

PVLNode.java                ← 树节点基类
├── PVLLeafNode             ← 叶子节点（存数据）
└── PVLNonLeafNode          ← 非叶子节点（存索引）

PVL_Res.java                ← 查询结果
├── VoInfo node             ← 验证对象
└── List<Long> res          ← 查询结果数据
```

### 二维索引 (spatial_2d) - 现在完整版

```
Spatial2DChain.java         ← 版本链管理 (NEW!)
├── Spatial2DIndex[] chain  ← 存储多个版本
├── insert(Point2D)         ← 动态插入二维点
├── rangeQuery(Rectangle2D, version) ← 版本化矩形查询
└── verify(rect, version, response)  ← 验证

Spatial2DIndex.java         ← 二维空间索引
├── PVLTree pvlTree         ← 复用一维树
├── Map<Long, Point2D> zToPoint  ← Z值到点的映射
├── rectangleQuery()        ← 二维矩形查询
└── verify()                ← 二维验证

ZOrderCurve.java            ← 空间填充曲线
├── encode(x, y)            ← 二维转一维
└── decode(z)               ← 一维转二维

ZOrderDecomposition.java    ← 查询分解
└── decomposeQuery()        ← 矩形分解为Z值区间

Spatial2DQueryResponse.java ← 查询结果 (对应PVL_Res)
├── List<Point2D> results           ← 最终二维结果
└── List<Spatial2DQueryResult> ...  ← 详细信息
```

## 代码结构对比

### 1. 版本链管理

#### PVLTreeChain
```java
public class PVLTreeChain {
    PVLTree[] chain;        // 一维树的版本链
    int front, rear;
    int currentVersion;
    
    public void insert(long key) {
        // 插入一维键值
        if (isNull()) {
            chain[rear] = new PVLTree(key, err);
        } else {
            chain[rear] = chain[prev].update(key);
        }
    }
    
    public PVL_Res rangeQuery(long low, long high, int version) {
        PVLTree tree = getVersionTree(version);
        return tree.rangeQuery(low, high);
    }
}
```

#### Spatial2DChain (对应的二维版本)
```java
public class Spatial2DChain {
    Spatial2DIndex[] chain;  // 二维索引的版本链
    int front, rear;
    int currentVersion;
    Map<Long, Point2D> globalZToPoint;
    
    public void insert(Point2D point) {
        // 插入二维点
        globalZToPoint.put(point.zValue, point);
        if (isNull()) {
            chain[rear] = new Spatial2DIndex(points, err);
        } else {
            chain[rear] = updateIndex(chain[prev], point);
        }
    }
    
    public Spatial2DQueryResponse rangeQuery(Rectangle2D rect, int version) {
        Spatial2DIndex index = getVersionIndex(version);
        return index.rectangleQuery(rect);
    }
}
```

### 2. 查询接口对比

| 功能 | 一维 (PVLTree) | 二维 (Spatial2DIndex) |
|------|----------------|----------------------|
| **输入** | `rangeQuery(low, high)` | `rectangleQuery(Rectangle2D)` |
| **输出** | `PVL_Res` | `Spatial2DQueryResponse` |
| **查询类型** | 一维区间 [low, high] | 二维矩形 [minX,minY,maxX,maxY] |
| **内部实现** | 直接在树上查询 | Z-order转换 + 多次一维查询 |

### 3. 验证机制对比

#### 一维验证
```java
// PVLTree.verify()
public boolean verify(long low, long high, PVL_Res res) {
    // 1. 验证哈希链
    // 2. 验证边界完整性
    return travelVoTree(low, high, rootR, res.node, res.res, ...);
}
```

#### 二维验证
```java
// Spatial2DIndex.verify()
public boolean verify(Rectangle2D rect, Spatial2DQueryResponse response) {
    // 1. 重新计算Z-order区间
    List<ZInterval> intervals = decomposeQuery(...);
    
    // 2. 对每个区间调用一维验证
    for (ZInterval interval : intervals) {
        if (!pvlTree.verify(interval.start, interval.end, pvlResult)) {
            return false;
        }
    }
    
    // 3. 验证二维结果完整性
    return verifyResultCompleteness(...);
}
```

## 为什么需要不同的设计？

### 1. 数据维度差异

```
一维: key → 直接存储在树中
二维: (x, y) → Z-order转换 → z值 → 存储在树中
                              ↓
                         需要保持反向映射
```

### 2. 查询复杂度差异

```
一维: [low, high] → 1个连续区间 → 1次树查询

二维: [minX,minY,maxX,maxY] → 分解为N个Z值区间
                              ↓
                         N次树查询 + 二维过滤
```

### 3. 验证复杂度差异

```
一维: 验证1个区间的哈希链

二维: 验证N个区间的哈希链 + 验证二维完整性
```

## 现在的完整对应关系

| 一维组件 | 二维组件 | 关系 |
|----------|----------|------|
| `PVLTreeChain` | `Spatial2DChain` | ✓ 完全对应 |
| `PVLTree` | `Spatial2DIndex` | ✓ 对应（但内部复用PVLTree） |
| `PVLNode` | - | 直接复用（通过Z-order） |
| `PVL_Res` | `Spatial2DQueryResponse` | ✓ 对应 |
| `VoInfo` | `Spatial2DQueryResult` | ✓ 对应 |
| - | `ZOrderCurve` | 新增（维度转换） |
| - | `ZOrderDecomposition` | 新增（查询分解） |

## 使用示例对比

### 一维使用
```java
// 创建版本链
PVLTreeChain chain = new PVLTreeChain(chainLen, err);

// 插入数据
for (long key : dataset) {
    chain.insert(key);
}

// 查询
PVL_Res res = chain.rangeQuery(low, high, version);

// 验证
PVLTree tree = chain.getVersionTree(version);
boolean valid = chain.verify(tree, low, high, res);
```

### 二维使用（现在完整版）
```java
// 创建版本链
Spatial2DChain chain = new Spatial2DChain(chainLen, err);

// 插入数据
for (Point2D point : dataset) {
    chain.insert(point);
}

// 查询
Rectangle2D rect = new Rectangle2D(100, 100, 200, 200);
Spatial2DQueryResponse res = chain.rangeQuery(rect, version);

// 验证
boolean valid = chain.verify(rect, version, res);
```

## 总结

现在二维索引已经完全对应一维索引的架构：

1. ✅ **版本链管理** - `Spatial2DChain` 对应 `PVLTreeChain`
2. ✅ **树结构** - 复用 `PVLTree`（通过Z-order映射）
3. ✅ **查询接口** - `rectangleQuery` 对应 `rangeQuery`
4. ✅ **验证机制** - 完整的二维验证流程
5. ✅ **结果结构** - `Spatial2DQueryResponse` 对应 `PVL_Res`

主要区别在于：
- 二维需要额外的 **Z-order转换层**
- 二维需要 **查询分解** 将矩形转换为多个一维区间
- 二维验证需要 **额外的完整性检查**

但整体架构和使用方式保持一致！



