@echo off

REM Get window width (from https://stackoverflow.com/a/14981267)
for /F "usebackq tokens=2* delims=: " %%W in (`mode con ^| findstr Columns`) do set CONSOLE_WIDTH=%%W
set "COLUMNS=%CONSOLE_WIDTH%"

REM Switch to UTF-8
chcp 65001 >nul

REM Build with Gradle in regular console mode, so that we get a progress
REM bar for the slow first-time build

"%~dp0\..\gradlew.bat" build --project-dir "%~dp0\.." --warn

REM Now run the app with --console=plain, because Gradle likes to
REM clobber the program output with its own progress bar.
REM
REM Note the space before $* below:
REM
REM Gradle blows up if --args receives an empty string as its argument.
REM However, it trims space from the end of the --args argument, and
REM a single space thus shows up as an empty args array by the time
REM it makes it to our main method.

"%~dp0\..\gradlew.bat" ^
    run-cli ^
    --project-dir "%~dp0\.." ^
    --args=" %*" ^
    --console=plain ^
    --warn
