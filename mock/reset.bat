@echo off

set base=%~dp0
cd /d %base%

if not exists .\TestRun goto NoDir
echo/
echo Reset TestRun Directory
set /P R=Confirm: DESTROY TestRun directory and recreate from templates (y/N)?
if not "z%R%" == "zy" if not "z%R%" == "zY" goto Cancel

rmdir /r /q .\TestRun

:NoDir
xcopy .\Template_Copy-Only .\TestRun
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
