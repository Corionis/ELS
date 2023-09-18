@echo off
REM reset [-f]

set base=%~dp0
cd /d "%base%"

if not exist .\copy-only_media-base goto NoDir
if "z%1" == "z-f" goto NoDir
echo/
echo Reset Test Configuration and Data
set r=
set /P R=Confirm: DESTROY Test Configuration and Data directories and recreate from templates (y/N)? 
if "z%R%" == "zy" goto NoDir
if "z%R%" == "zY" goto NoDir
goto Cancel

:NoDir
cmd /c .\reset-config.bat %1
cd /d "%base%"
cmd /c .\reset-test.bat %1
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
cd /d "%base%"

