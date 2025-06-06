@echo off
REM reset [-f]

set base=%~dp0
cd /d "%base%"
cd ..\..

if not exist .\copy-only_media-base goto NoDir
if "z%1" == "z-f" goto Execute
echo/
echo Reset Test Data
set r=
set /P R=Confirm: DESTROY Test data directories and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:Execute
rmdir /s /q .\test
del /q .\*.log
del /q .\*received*.json
del /q .\*generated*.json

:NoDir
xcopy /I /E .\copy-only_media-base .\test
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
cd /d "%base%"
