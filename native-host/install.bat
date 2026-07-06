@echo off
setlocal enabledelayedexpansion

echo ============================================
echo  Open Folder - Native Host Installer
echo ============================================
echo.

REM Resolve the directory this script lives in
set SCRIPT_DIR=%~dp0
set BAT_PATH=%SCRIPT_DIR%open_folder_host.bat
set JSON_PATH=%SCRIPT_DIR%open_folder_host.json
set HOST_NAME=com.example.open_folder

REM Ask for the extension ID
echo Step 1: Load the extension in Chrome
echo   - Open chrome://extensions
echo   - Enable "Developer mode" (top-right toggle)
echo   - Click "Load unpacked" and select the "extension" folder
echo   - Copy the Extension ID shown under the extension name
echo.
set /p EXT_ID=Paste your Extension ID here:

if "%EXT_ID%"=="" (
    echo ERROR: Extension ID cannot be empty.
    pause
    exit /b 1
)

REM Write the updated JSON manifest
(
echo {
echo   "name": "com.example.open_folder",
echo   "description": "Opens a Windows folder in Explorer",
echo   "path": "%BAT_PATH:\=\\%",
echo   "type": "stdio",
echo   "allowed_origins": [
echo     "chrome-extension://%EXT_ID%/"
echo   ]
echo }
) > "%JSON_PATH%"

echo.
echo Manifest written to: %JSON_PATH%

REM Register in Windows registry
reg add "HKCU\Software\Google\Chrome\NativeMessagingHosts\%HOST_NAME%" /ve /t REG_SZ /d "%JSON_PATH%" /f

if %ERRORLEVEL% EQU 0 (
    echo.
    echo SUCCESS! Native host registered.
    echo.
    echo Step 2: Reload the extension in Chrome
    echo   - Go to chrome://extensions
    echo   - Click the reload icon on "Open Folder"
    echo   - Click the extension icon and test opening a folder
) else (
    echo.
    echo ERROR: Failed to write to registry. Try running as Administrator.
)

echo.
pause
