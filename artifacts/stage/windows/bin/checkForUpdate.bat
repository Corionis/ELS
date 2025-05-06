@echo off

set base=%~dp0
cd /d "%base%"
cd ..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -V -F output/CheckForUpdates.log

cd /d "%base%"

