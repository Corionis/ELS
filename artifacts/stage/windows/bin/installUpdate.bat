@echo off

set base=%~dp0
cd /d "%base%"
cd ..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -Y -F output/InstallUpdate.log

cd /d "%base%"

