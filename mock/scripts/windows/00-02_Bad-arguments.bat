@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug -p test/publisher/publisher.json -s test/subscribe-one/subscriber-one.json -T test/subscriber-one/targets.json -F output/00-02_Bad-arguments.log -a-bad-argument

