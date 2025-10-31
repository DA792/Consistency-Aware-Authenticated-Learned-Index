@echo off
chcp 65001 >nul
echo ===================================
echo R-tree uniform_500k.csv Experiment
echo ===================================
echo.

:: Create bin directory if not exists
if not exist bin mkdir bin

:: Set classpath
set CLASSPATH=.;jars\*

:: Compile utils classes first
echo Compiling utils classes...
javac -cp %CLASSPATH% -d bin src\utils\*.java
if %ERRORLEVEL% NEQ 0 (
    echo Utils compilation failed!
    pause
    exit /b 1
)

:: Compile R-tree classes with utils in classpath
echo Compiling R-tree classes...
javac -cp bin;%CLASSPATH% -d bin src\Rtree\*.java
if %ERRORLEVEL% NEQ 0 (
    echo R-tree compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Starting uniform_500k.csv dataset experiment...
echo Note: 500K data experiment may take several minutes
echo.

:: Set JVM parameters for performance
set JVM_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC

:: Run experiment
java %JVM_OPTS% -cp bin;%CLASSPATH% Rtree.RTreeExperiment

echo.
echo Experiment completed!
pause
