@echo off
chcp 65001 >nul
echo ===== 分区索引性能对比测试 =====
echo.

cd Consistency-Aware-Authenticated-Learned-Index

echo 1. 创建编译输出目录...
if not exist "bin" mkdir bin
echo 输出目录: bin\
echo.

echo 2. 编译代码...
echo    - 编译工具类...
javac -encoding UTF-8 -cp "jars/*" -d bin src/utils/*.java

echo    - 编译索引基础类...
javac -encoding UTF-8 -cp "jars/*" -d bin src/index/learned_node_info/*.java
javac -encoding UTF-8 -cp "jars/*;bin" -d bin src/index/PVLB_tree_index/*.java
javac -encoding UTF-8 -cp "jars/*;bin" -d bin src/index/PVL_tree_index/*.java
javac -encoding UTF-8 -cp "jars/*;bin" -d bin src/index/HPVL_tree_index/*.java

echo    - 编译2D PVL索引...
javac -encoding UTF-8 -cp "jars/*;bin" -d bin src/index/spatial_2d_pvl/*.java

echo    - 编译分区索引 (新)...
javac -encoding UTF-8 -cp "jars/*;bin" -d bin src/index/spatial_2d_pvl_partitioned/*.java

if %errorlevel% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo 编译成功!
echo.

echo 3. 运行性能对比测试...
echo.
echo 说明:
echo   - 对比全局索引 vs 分区索引性能
echo   - 使用50万点数据集 (uniform_500k.csv)
echo   - 误差界限 err=256, 分区数=8
echo   - 测试选择性: [0.0001, 0.001, 0.01]
echo.

java -Xmx1g -cp "jars/*;bin" index.spatial_2d_pvl_partitioned.PartitionedIndexTest

echo.
echo ===== 测试完成 =====
pause

