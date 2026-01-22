@echo off
setlocal

:: --- 1. DEFINE PATHS ---
SET "PROJECT_ROOT=%~dp0"
SET "PYTHON_SOURCE=%PROJECT_ROOT%scripts"
SET "JAVA_RESOURCES=%PROJECT_ROOT%src\main\resources"

if not exist "%JAVA_RESOURCES%" mkdir "%JAVA_RESOURCES%"

echo ==========================================
echo      PATH DIAGNOSTICS
echo ==========================================
echo Project Root:   %PROJECT_ROOT%
echo Python Source:  %PYTHON_SOURCE%
echo Java Target:    %JAVA_RESOURCES%
echo.

:: --- 2. COMPILE PYTHON ---
echo [1/3] Compiling Python script...
echo       NOTE: This will take a moment...
cd /d "%PYTHON_SOURCE%"

python -m PyInstaller --onefile --console ^
    --collect-all geopandas ^
    --collect-all fiona ^
    --collect-all shapely ^
    --hidden-import fiona.ogrext ^
    --hidden-import fiona._shim ^
    --hidden-import fiona.schema ^
    address_to_scr.py

if not exist "dist\address_to_scr.exe" (
    echo.
    echo ❌ ERROR: PyInstaller failed.
    pause
    exit /b
)

:: --- 3. MOVE TO RESOURCES ---
echo.
echo [2/3] Moving .exe to Java Resources...

copy /y "dist\address_to_scr.exe" "%JAVA_RESOURCES%\address_to_scr.exe"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: Copy failed!
    pause
    exit /b
)

:: --- 4. CLEANUP (FIXED) ---
echo.
echo [3/3] Cleaning up...

cd /d "%PROJECT_ROOT%"

:: WAIT 2 SECONDS for Windows Defender to release the file lock
timeout /t 2 /nobreak >nul

:: Force delete using full paths
if exist "%PYTHON_SOURCE%\build" rd /s /q "%PYTHON_SOURCE%\build"
if exist "%PYTHON_SOURCE%\dist" rd /s /q "%PYTHON_SOURCE%\dist"
if exist "%PYTHON_SOURCE%\address_to_scr.spec" del /q "%PYTHON_SOURCE%\address_to_scr.spec"
if exist "%PYTHON_SOURCE%\__pycache__" rd /s /q "%PYTHON_SOURCE%\__pycache__"

echo.
echo ==========================================
echo      ✅ SUCCESS!
echo      File located at: %JAVA_RESOURCES%\address_to_scr.exe
echo ==========================================
pause