@echo off
REM compare-clear
REM Useful for doing TestRun directory compares

set base=%~dp0
cd /d %base%

if not exist .\TestRun goto NoDir

del /s TestRun\*export.json
del /s TestRun\*received*
del /s TestRun\*generated*
del /s TestRun\*.log
del /s TestRun\*.txt

:NoDir
