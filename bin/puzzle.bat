@echo off

REM Get window width (from https://stackoverflow.com/a/14981267)
for /F "usebackq tokens=2* delims=: " %%W in (`mode con ^| findstr Columns`) do set CONSOLE_WIDTH=%%W
set "COLUMNS=%CONSOLE_WIDTH%"

REM Switch to UTF-8
chcp 65001 >nul

"%~dp0\..\gradlew.bat" ^
    run-cli ^
    --project-dir "%~dp0\.." ^
    --args=" %*" ^
    --console=plain ^
    --warn
