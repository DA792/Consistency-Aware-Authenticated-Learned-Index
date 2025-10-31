@echo off
chcp 65001 >nul
echo ===== 测试不同误差界限的性能 =====
echo.

cd Consistency-Aware-Authenticated-Learned-Index

echo 正在编译...
javac -encoding UTF-8 -cp "jars/*" -d bin src/utils/*.java src/index/learned_node_info/*.java src/index/PVLB_tree_index/*.java src/index/PVL_tree_index/*.java src/index/HPVL_tree_index/*.java src/index/spatial_2d_pvl/*.java

if %errorlevel% neq 0 (
    echo 编译失败!
    pause
    exit /b 1
)

echo.
echo ========================================
echo 测试 1: 误差界限 = 64
echo ========================================
java -cp "jars/*;bin" index.spatial_2d_pvl.TestErrorBounds 64

echo.
echo ========================================
echo 测试 2: 误差界限 = 128
echo ========================================
java -cp "jars/*;bin" index.spatial_2d_pvl.TestErrorBounds 128

echo.
echo ========================================
echo 测试 3: 误差界限 = 256
echo ========================================
java -cp "jars/*;bin" index.spatial_2d_pvl.TestErrorBounds 256

echo.
echo ===== 测试完成 =====
pause
