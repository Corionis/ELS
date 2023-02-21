@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -T libraries/targets.json -e output/00-04_Export.txt -i output/00-04_Export_collection.json -F output/00-04_Export.log

cd /d "%base%"

