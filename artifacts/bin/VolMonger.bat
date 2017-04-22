@echo off
REM Create a .volmonger file for all arguments

:loop
@echo %1
echo {} >%1.volmonger
shift
if not "%~1"=="" goto loop

pause
