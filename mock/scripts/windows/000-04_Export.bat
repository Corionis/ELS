@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -e output/00-04_Export.txt -i output/00-04_Export_collection.json -F output/00-04_Export.log

cd /d "%base%"

