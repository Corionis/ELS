@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p "libraries\Publisher.json" -e output\000-04_Export.txt -i output\000-04_Export_collection.json -F output\000-04_Export.log

cd /d "%base%"

