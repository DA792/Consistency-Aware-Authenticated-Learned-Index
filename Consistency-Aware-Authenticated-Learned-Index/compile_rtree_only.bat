@echo off
chcp 65001 >nul
echo ===============================
echo Compiling R-tree Only (No Utils)
echo ===============================
echo.

:: Create bin directory if not exists
if not exist bin mkdir bin

:: Clean previous R-tree compilation
echo Cleaning previous R-tree compilation...
if exist bin\Rtree rmdir /s /q bin\Rtree

:: Compile R-tree classes only
echo.
echo Compiling R-tree classes...
javac -d bin src\Rtree\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: R-tree compilation failed!
    pause
    exit /b 1
)
echo R-tree compilation successful!

echo.
echo ===============================
echo R-tree compilation completed!
echo ===============================
echo.
echo You can now run:
echo   - java -cp bin Rtree.RTreeSimpleTest
echo.
pause
