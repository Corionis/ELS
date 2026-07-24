@echo off
REM Do not change or delete this script.
REM It is used by processes in ELS.
REM It is replaced during updates.

set base=%~dp0
cd /d "%base%"

if "%~1"=="" goto NoArgs
rt\bin\java -jar bin\ELS.jar %*
goto Done

:NoArgs
rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -n -F output\ELS-Navigator.log

:Done
cd /d "%base%"
