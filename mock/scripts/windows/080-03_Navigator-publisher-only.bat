@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -p libraries/publisher.json -F output/080-03_Navigator-publisher-only.log %*

cd /d "%base%"

