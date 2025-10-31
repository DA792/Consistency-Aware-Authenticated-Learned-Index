@echo off
chcp 65001 >nul
echo ===== MHT vs PVL 性能对比测试 =====
echo.

echo 说明:
echo   - 对比MHT和PVL两种认证索引
echo   - 数据集: uniform_500k.csv (50万点)
echo   - PVL误差界限: 128
echo   - MHT叶子大小: 256
echo   - 查询选择性: [0.0001, 0.001, 0.01]
echo.

echo 1. 创建编译输出目录...
if not exist "bin\" mkdir bin
echo    输出目录: bin\
echo.

echo 2. 编译代码...
echo    - 编译工具类...
javac -encoding UTF-8 -d bin ^
    src/utils/Point2D.java ^
    src/utils/Rectangle2D.java ^
    src/utils/ZOrderCurve.java ^
    src/utils/ZOrderDecomposition.java ^
    src/utils/DataLoader.java

if errorlevel 1 (
    echo    编译工具类失败!
    pause
    exit /b 1
)

echo    - 编译索引基础类...
javac -encoding UTF-8 -cp bin -d bin ^
    src/index/learned_node_info/*.java ^
    src/index/PVL_tree_index/*.java ^
    src/index/HPVL_tree_index/*.java

if errorlevel 1 (
    echo    编译索引基础类失败!
    pause
    exit /b 1
)

echo    - 编译PVL索引...
javac -encoding UTF-8 -cp bin -d bin ^
    src/index/spatial_2d_pvl/*.java

if errorlevel 1 (
    echo    编译PVL索引失败!
    pause
    exit /b 1
)

echo    - 编译MHT索引 (新)...
javac -encoding UTF-8 -cp bin -d bin ^
    src/index/baseline/MHTNode.java ^
    src/index/baseline/MHTQueryResult.java ^
    src/index/baseline/MerkleHashTree.java ^
    src/index/baseline/Spatial2DMHT.java ^
    src/index/baseline/MHTTest.java

if errorlevel 1 (
    echo    编译MHT索引失败!
    pause
    exit /b 1
)

echo    编译成功!
echo.

echo 3. 运行性能对比测试...
echo.
echo    说明:
echo      - 对比MHT vs PVL性能
echo      - 测试3种查询选择性
echo      - 收集查询/验证时间、VO大小等指标
echo.

java -Xmx1g -cp bin index.baseline.MHTTest

if errorlevel 1 (
    echo.
    echo 测试失败!
    pause
    exit /b 1
)

echo.
echo ===== 测试完成 =====
pause

