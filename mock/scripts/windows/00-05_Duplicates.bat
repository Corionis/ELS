@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -T libraries/targets.json -F output/00-05_Duplicates.log --duplicates

