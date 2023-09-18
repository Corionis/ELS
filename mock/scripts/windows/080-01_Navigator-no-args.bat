@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . -c debug -d debug -F output/080-01_Navigator.log %*

cd /d "%base%"

