@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================
echo  Open Folder - Build Script
echo ============================================
echo.

REM ── Check javac (need JDK, JRE alone is not enough) ────────────────────────
where javac >nul 2>&1
if %ERRORLEVEL% EQU 0 goto :build

echo [INFO] Java JDK not found. Attempting automatic installation...
echo.

REM ── Try winget (built into Windows 10/11) ───────────────────────────────────
where winget >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] winget is not available on this machine.
    echo.
    echo Please install Java JDK 21 manually:
    echo   https://adoptium.net/
    echo.
    echo After installation, re-run this script.
    pause
    exit /b 1
)

echo [INFO] Installing Eclipse Temurin JDK 21 via winget...
echo        This may take a few minutes.
echo.
winget install --id EclipseAdoptium.Temurin.21.JDK --silent --accept-source-agreements --accept-package-agreements
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo [ERROR] winget installation failed.
    echo         Please install Java JDK 21 manually: https://adoptium.net/
    pause
    exit /b 1
)

echo.
echo [INFO] Installation complete. Locating JDK bin directory...

REM ── Find the newly installed JDK in common locations and add to PATH ─────────
set "JDK_BIN="
for /d %%d in ("C:\Program Files\Eclipse Adoptium\jdk*") do (
    if exist "%%d\bin\javac.exe" set "JDK_BIN=%%d\bin"
)
if not defined JDK_BIN (
    for /d %%d in ("C:\Program Files\Microsoft\jdk*") do (
        if exist "%%d\bin\javac.exe" set "JDK_BIN=%%d\bin"
    )
)
if not defined JDK_BIN (
    for /d %%d in ("C:\Program Files\Java\jdk*") do (
        if exist "%%d\bin\javac.exe" set "JDK_BIN=%%d\bin"
    )
)

if defined JDK_BIN (
    echo [OK] Found JDK at: !JDK_BIN!
    set "PATH=!JDK_BIN!;%PATH%"
) else (
    echo [INFO] JDK installed but PATH will only update in a new terminal.
    echo        Please close this window and re-run build.bat.
    pause
    exit /b 0
)

REM ── Confirm javac is now reachable ──────────────────────────────────────────
where javac >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo [INFO] Please close this window and re-run build.bat.
    pause
    exit /b 0
)

echo.

:build
echo Compiling NativeHost.java...
javac NativeHost.java
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Compilation failed.
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
echo Next step: run install.bat to register the native host.
echo.
pause
