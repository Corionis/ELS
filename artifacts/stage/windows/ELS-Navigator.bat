@echo off

set base=%~dp0
cd /d "%base%"

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -n -F output\ELS-Navigator.log %*

cd /d "%base%"

