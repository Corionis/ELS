@echo off
REM reset [-f]

set base=%~dp0
cd /d %base%
cd ..\..

if not exist .\media-base_copy-only goto NoDir
if "z%1" == "z-f" goto Execute
echo/
echo Reset Test Configuration and Data
set r=
set /P R=Confirm: DESTROY Test Configuration and Data directories and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:NoDir
cmd /c ./reset-config.bat %1
cmd /c ./reset-test.bat %1

:Cancel
echo Cancelled

:JXT
echo/
cd /d %base%

