@echo off

REM Get window width (from https://stackoverflow.com/a/14981267)
IF "%IGNORE_CONSOLE_WIDTH%"=="" (
    for /F "usebackq tokens=2* delims=: " %%W in (`mode con ^| findstr Columns`) do set COLUMNS=%%W
)

REM Switch to UTF-8
chcp 65001 >nul

REM Build with Gradle in regular console mode, so that we get a progress
REM bar for the slow first-time build

call "%~dp0\..\gradlew.bat" compileJava --project-dir "%~dp0\.." --warn

REM It's apparently not possible to get bat file's path as the user typed it:
REM https://superuser.com/questions/1748050/is-it-possible-for-a-windows-batch-file-to-get-its-own-name-as-the-user-typed-i
REM We thus assume the student is following the directions from Moodle, is in
REM the project root dir, and typed `bin\puzzle` to run this script:

set "puzzle_command=bin\puzzle"

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
