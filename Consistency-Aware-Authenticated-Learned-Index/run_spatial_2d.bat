@echo off
REM 二维空间索引运行脚本 (Windows)

echo ===== 编译二维空间索引 =====
cd src
javac index/spatial_2d/*.java
if %errorlevel% neq 0 (
    echo 编译失败！
    pause
    exit /b %errorlevel%
)
echo 编译成功！
echo.

echo 请选择要运行的示例:
echo 1. QuickStartExample - 快速开始示例（随机数据）
echo 2. RealDataExample - 真实数据集示例（推荐）
echo 3. Spatial2DExample - 完整示例（可选真实数据或随机数据）
echo.

set /p choice=请输入选项 (1-3): 

if "%choice%"=="1" (
    echo.
    echo ===== 运行快速开始示例 =====
    java index.spatial_2d.QuickStartExample
) else if "%choice%"=="2" (
    echo.
    echo ===== 运行真实数据集示例 =====
    java index.spatial_2d.RealDataExample
) else if "%choice%"=="3" (
    echo.
    echo ===== 运行完整示例 =====
    java index.spatial_2d.Spatial2DExample
) else (
    echo 无效的选项！
)

echo.
pause


