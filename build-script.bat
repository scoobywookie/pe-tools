@echo off
setlocal

:: --- 1. DEFINE PATHS ---
SET "PROJECT_ROOT=%~dp0"
SET "PYTHON_SOURCE=%PROJECT_ROOT%scripts"
SET "JAVA_RESOURCES=%PROJECT_ROOT%src\main\resources\scripts"

echo ==========================================
echo      PATH DIAGNOSTICS
echo ==========================================
echo Project Root:   %PROJECT_ROOT%
echo Python Source:  %PYTHON_SOURCE%
echo Java Target:    %JAVA_RESOURCES%
echo.

:: --- 2. COMPILE PYTHON (THE FIX) ---
echo [1/3] Compiling Python script...
echo       NOTE: This will take a moment (collecting map engines)...
cd /d "%PYTHON_SOURCE%"

:: --- MASSIVE COMMAND TO FORCE FIONA/SHAPELY INCLUSION ---
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

:: --- 3. COPY THE FILE ---
echo.
echo [2/3] Copying .exe to Java Resources...

if not exist "%JAVA_RESOURCES%" mkdir "%JAVA_RESOURCES%"

copy /y "dist\address_to_scr.exe" "%JAVA_RESOURCES%\address_to_scr.exe"

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ❌ ERROR: Copy failed!
    pause
    exit /b
)

:: Keep local copy
move /y "dist\address_to_scr.exe" "address_to_scr.exe"

:: --- 4. CLEANUP ---
echo.
echo [3/3] Cleaning up...
rd /s /q build
rd /s /q dist
del /q address_to_scr.spec
if exist __pycache__ rd /s /q __pycache__

echo.
echo ==========================================
echo      ✅ SUCCESS!
echo      File copied to: %JAVA_RESOURCES%
echo ==========================================
pause