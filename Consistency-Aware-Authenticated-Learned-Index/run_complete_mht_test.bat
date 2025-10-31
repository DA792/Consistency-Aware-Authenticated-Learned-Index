@echo off
chcp 65001 >nul
echo ===== 完整MHT性能测试 =====
echo.

echo 说明:
echo   - 使用与PVL相同的测试配置
echo   - 数据集: uniform_500k.csv (50万点)
echo   - 查询选择性: [0.0001, 0.001, 0.01, 0.1]
echo   - 查询次数: 300次/选择性
echo.

echo 1. 编译完整MHT...
call compile_complete_mht.bat
if errorlevel 1 (
    echo 编译失败!
    pause
    exit /b 1
)
echo.

echo 2. 运行测试...
echo.
java -Xmx1g -cp bin index.mht_complete.CompleteMHTSimpleTest

if errorlevel 1 (
    echo.
    echo 测试失败!
    pause
    exit /b 1
)

echo.
echo ===== 测试完成 =====
pause

