@echo off
REM Create a .volmunger file for all arguments

:loop
@echo %1
echo {} >%1.volmunger
shift
if not "%~1"=="" goto loop

pause
