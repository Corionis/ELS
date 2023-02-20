@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --auth-keys system/auth.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/82-31_Subscriber-listener-auth.log

