@echo off
REM compare-copy

set base=%~dp0
cd /d %base%

if not exist .\TestRun goto NoDir

if exist .\TestRun-compare rmdir /q /s .\TestRun-compare

move TestRun TestRun-compare

echo/
echo Run reset.bat to setup a new TestRun directory

:NoDir
