@echo off
echo ==========================================
echo      PE TOOLS INSTALLER GENERATOR
echo ==========================================
echo.
echo RULES FOR AUTO-UPDATE TO WORK:
echo 1. You MUST uninstall the old version manually ONE LAST TIME before using this new system.
echo 2. For future updates, you MUST increase the version number (e.g. 1.0 -> 1.1).
echo.

:: 1. Ask for the version number
set /p AppVersion="Enter NEW version number (e.g. 1.1): "

echo.
echo [1/3] Building Project with Maven...
call mvn clean package dependency:copy-dependencies -DoutputDirectory=target/libs

echo.
echo [2/3] Updating JAR files...
copy /y "target\pe-tools-1.0-SNAPSHOT.jar" "target\libs\pe-tools-1.0-SNAPSHOT.jar"

echo.
echo [3/3] Generating MSI Installer (Version %AppVersion%)...

:: --- EXPLANATION OF CHANGES ---
:: --win-upgrade-uuid: This specific ID tells Windows this is the "PE Tools" family.
::                     KEEP THIS ID THE SAME FOREVER. Do not change it.
:: --win-dir-chooser:  Removed. Allowing users to change install paths breaks auto-updates often.
::                     Standard install location is safer for upgrades.

jpackage --type msi ^
    --dest "%USERPROFILE%\Desktop" ^
    --input target/libs ^
    --name "PE-Tools" ^
    --main-jar pe-tools-1.0-SNAPSHOT.jar ^
    --main-class com.petools.Launcher ^
    --icon pe_logo.ico ^
    --win-menu ^
    --win-shortcut ^
    --win-upgrade-uuid "a06c8882-7f21-41d3-9723-57a53c664320" ^
    --app-version %AppVersion%

echo.
echo ==========================================
echo      DONE! Installer created on your DESKTOP.
echo ==========================================
pause