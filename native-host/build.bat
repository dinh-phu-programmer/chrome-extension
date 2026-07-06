@echo off
setlocal
cd /d "%~dp0"

echo Compiling NativeHost.java...
javac NativeHost.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed. Make sure JDK is installed and javac is on PATH.
    pause
    exit /b 1
)

echo Packaging NativeHost.jar...
jar cfe NativeHost.jar NativeHost NativeHost.class
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] JAR creation failed.
    pause
    exit /b 1
)

del NativeHost.class

echo.
echo [OK] NativeHost.jar built successfully.
echo Now run install.bat to register the native host.
echo.
pause
