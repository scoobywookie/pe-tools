@echo off
echo ==========================================
echo      PE TOOLS INSTALLER GENERATOR
echo ==========================================
echo.

:: 1. Ask for the version number (e.g. 1.2.0)
set /p AppVersion="Enter version number (e.g. 1.0): "

echo.
echo [1/3] Building Project with Maven...
call mvn clean package dependency:copy-dependencies -DoutputDirectory=target/libs

echo.
echo [2/3] Updating JAR files...
copy /y "target\pe-tools-1.0-SNAPSHOT.jar" "target\libs\pe-tools-1.0-SNAPSHOT.jar"

echo.
echo [3/3] Generating MSI Installer (Version %AppVersion%)...
jpackage --type msi ^
    --dest output ^
    --input target/libs ^
    --name "PE Tools" ^
    --main-jar pe-tools-1.0-SNAPSHOT.jar ^
    --main-class com.petools.Launcher ^
    --icon pe_logo.ico ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --app-version %AppVersion%

echo.
echo ==========================================
echo      DONE! Installer created in 'output'
echo ==========================================
pause