@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --auth-keys test/test-auth.keys --remote S -p test/workstation.json -s test/publisher/publisher.json -T -F output/82-31_Subscriber-listener-auth.log

