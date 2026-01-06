@echo off
echo Cleaning and rebuilding PE Tools...
call mvn clean javafx:jlink
echo.
echo Build finished. Run the app from: target\petools\bin\petools.bat
pause