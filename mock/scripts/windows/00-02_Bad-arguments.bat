@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/targets.json -F output/00-02_Bad-arguments.log -a-bad-argument

