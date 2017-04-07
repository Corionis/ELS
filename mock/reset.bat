@echo off

set base=%~dp0
cd /d %base%

if not exist .\TestRun goto NoDir
echo/
echo Reset TestRun Directory
set r=
set /P R=Confirm: DESTROY TestRun directory and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:Execute
rmdir /s /q .\TestRun

:NoDir
xcopy /I /E .\Template_Copy-Only .\TestRun
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
