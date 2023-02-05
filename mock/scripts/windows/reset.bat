@echo off
REM reset [-f]

set base=%~dp0
cd /d %base%
cd ..\..

if not exist .\media-base_copy-only goto NoDir
if "z%1" == "z-f" goto Execute
echo/
echo Reset TestRun Directory
set r=
set /P R=Confirm: DESTROY test directory and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:Execute
rmdir /s /q .\test
del .\*.log
del .\*received*.json
del .\*generated*.json
del .\output\*

:NoDir
xcopy /I /E .\media-base_copy-only .\test
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
cd /d %base%
