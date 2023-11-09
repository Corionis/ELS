@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -c debug -d debug --auth-keys system/authorization.keys --remote S -p libraries/publisher.json -s libraries/subscriber-one.json -T -F output/020-51_Subscriber-listener-auth.log

cd /d "%base%"

