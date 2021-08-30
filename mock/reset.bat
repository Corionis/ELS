@echo off
REM reset [-f]

set base=%~dp0
cd /d %base%

if not exist .\test goto NoDir
if "z%1" == "z-f" goto Execute
echo/
echo Reset Test Directory
set r=
set /P R=Confirm: DESTROY Test directory and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:Execute
rmdir /s /q .\test
if exist .\*.log del /q .\*.log

:NoDir
xcopy /I /E .\media-base_copy-only .\test
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/

