@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug -p test/publisher/publisher.json -T test/subscriber-one/targets.json -e output/00-04_Export.txt -i output/00-04_Export_collection.json -F output/00-04_Export.log

