@echo off
REM reset [-f]

set base=%~dp0
cd /d "%base%"
cd ..\..

if not exist .\config-base_copy-only goto NoDir
if "z%1" == "z-f" goto Execute
echo/
echo Reset Test Configuration
set r=
set /P R=Confirm: DESTROY Configuration directories and recreate from templates (y/N)? 
if "z%R%" == "zy" goto Execute
if "z%R%" == "zY" goto Execute
goto Cancel

:Execute
rmdir /s /q .\datastore
rmdir /s /q .\jobs
rmdir /s /q .\libraries
rmdir /s /q .\local
rmdir /s /q .\output
rmdir /s /q .\system
rmdir /s /q .\tools

:NoDir
xcopy /I /E .\config-base_copy-only .
echo Done
goto JXT

:Cancel
echo Cancelled

:JXT
echo/
cd /d "%base%"
