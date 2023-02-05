@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -K test/test-hints.keys -c debug -d debug -p test/publisher/publisher.json -T -F output/50-01_Hints-publisher.log

