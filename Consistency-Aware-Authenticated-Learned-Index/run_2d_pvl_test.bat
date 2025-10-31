@echo off
chcp 65001 >nul
echo ===== 编译并运行二维PVL树性能测试 =====
echo.

cd Consistency-Aware-Authenticated-Learned-Index

echo 1. 创建编译输出目录...
if not exist "bin" mkdir bin
echo 输出目录: bin\
echo.

echo 2. 编译代码...
javac -encoding UTF-8 -cp "jars/*" -d bin ^
    src/utils/*.java ^
    src/index/learned_node_info/*.java ^
    src/index/PVLB_tree_index/*.java ^
    src/index/PVL_tree_index/*.java ^
    src/index/HPVL_tree_index/*.java ^
    src/index/spatial_2d_pvl/*.java

if %errorlevel% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo 编译成功! 编译文件已保存到 bin\ 目录
echo.

echo 3. 运行二维PVL树性能测试 (100万数据, err=256)...
echo.
echo 说明: 使用100万点数据集, 误差界限 err=256
echo.
java -Xmx2g -cp "jars/*;bin" index.spatial_2d_pvl.Spatial2DPVLTree

echo.
echo ===== 测试完成 =====
pause

