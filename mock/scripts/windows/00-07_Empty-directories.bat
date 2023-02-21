@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -F output/00-07_Empty-directories.log --empty-directories

cd /d "%base%"

