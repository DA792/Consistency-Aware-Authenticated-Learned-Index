@echo off
chcp 65001 >nul
REM 运行所有二维索引测试

echo ===== 编译所有二维索引 =====

cd Consistency-Aware-Authenticated-Learned-Index

echo.
echo 1. 创建编译输出目录...
if not exist "bin" mkdir bin
echo 输出目录: bin\
echo.

echo 2. 编译所有代码...
javac -encoding UTF-8 -cp "jars/*" -d bin ^
    src/utils/*.java ^
    src/index/learned_node_info/*.java ^
    src/index/PVL_tree_index/*.java ^
    src/index/PVLB_tree_index/*.java ^
    src/index/HPVL_tree_index/*.java ^
    src/index/spatial_2d_pvl/*.java ^
    src/index/spatial_2d_pvlb/*.java ^
    src/index/spatial_2d_hpvl/*.java

if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b %errorlevel%
)

echo.
echo ===== 编译成功！编译文件已保存到 bin\ 目录 =====
echo.

echo 请选择要运行的测试:
echo 1. 二维PVL索引测试 (查询优化型)
echo 2. 二维PVLB索引测试 (更新优化型)
echo 3. 二维HPVL索引测试 (混合优化型)
echo 4. 运行全部测试
echo.

set /p choice=请输入选项 (1-4): 

if "%choice%"=="1" (
    echo.
    echo ===== 运行二维PVL索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_pvl.Spatial2DPVLExample
) else if "%choice%"=="2" (
    echo.
    echo ===== 运行二维PVLB索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_pvlb.Spatial2DPVLBExample
) else if "%choice%"=="3" (
    echo.
    echo ===== 运行二维HPVL索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_hpvl.Spatial2DHPVLExample
) else if "%choice%"=="4" (
    echo.
    echo ===== 运行二维PVL索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_pvl.Spatial2DPVLExample
    echo.
    echo.
    echo ===== 运行二维PVLB索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_pvlb.Spatial2DPVLBExample
    echo.
    echo.
    echo ===== 运行二维HPVL索引测试 =====
    java -cp "jars/*;bin" index.spatial_2d_hpvl.Spatial2DHPVLExample
) else (
    echo 无效的选项！
)

echo.
pause


