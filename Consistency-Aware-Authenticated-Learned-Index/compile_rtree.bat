@echo off
chcp 65001 >nul
echo ===============================
echo Compiling R-tree Implementation
echo ===============================
echo.

:: Create bin directory if not exists
if not exist bin mkdir bin

:: Set classpath
set CLASSPATH=.;jars\*

:: Clean previous compilation
echo Cleaning previous compilation...
if exist bin\utils rmdir /s /q bin\utils
if exist bin\Rtree rmdir /s /q bin\Rtree

:: Compile utils classes first
echo.
echo Step 1: Compiling utils classes...
javac -cp %CLASSPATH% -d bin src\utils\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Utils compilation failed!
    pause
    exit /b 1
)
echo Utils compilation successful!

:: Compile R-tree classes
echo.
echo Step 2: Compiling R-tree classes...
javac -cp bin;%CLASSPATH% -d bin src\Rtree\*.java
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: R-tree compilation failed!
    pause
    exit /b 1
)
echo R-tree compilation successful!

echo.
echo ===============================
echo All compilation completed successfully!
echo ===============================
echo.
echo You can now run:
echo   - run_rtree_quick_test.bat (for quick test)
echo   - run_rtree_experiment.bat (for full experiment)
echo.
pause
