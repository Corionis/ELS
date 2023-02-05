@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --auth-keys test/test-auth.keys --remote S -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/20-51_Subscriber-listener-auth.log

