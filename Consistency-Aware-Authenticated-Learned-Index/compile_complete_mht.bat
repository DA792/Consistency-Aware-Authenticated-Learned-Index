@echo off
chcp 65001 >nul
echo ===== 编译完整MHT =====
echo.

echo 1. 创建输出目录...
if not exist "bin\" mkdir bin
echo    输出目录: bin\
echo.

echo 2. 编译工具类 (只编译MHT需要的)...
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
echo    工具类编译成功!
echo.

echo 3. 编译完整MHT...
javac -encoding UTF-8 -cp bin -d bin ^
    src/index/mht_complete/CompleteMHTNode.java ^
    src/index/mht_complete/CompleteMHTVO.java ^
    src/index/mht_complete/CompleteMerkleHashTree.java ^
    src/index/mht_complete/CompleteMHTSimpleTest.java

if errorlevel 1 (
    echo    编译完整MHT失败!
    pause
    exit /b 1
)
echo    完整MHT编译成功!
echo.

echo ===== 编译完成 =====
echo.
echo 运行测试:
echo   java -cp bin index.mht_complete.CompleteMHTSimpleTest
echo.

