@echo off
REM Create a .els file for all arguments

:loop
@echo %1
echo {} >%1.els
shift
if not "%~1"=="" goto loop

pause
