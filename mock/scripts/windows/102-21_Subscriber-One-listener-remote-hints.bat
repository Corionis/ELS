@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -k test/test-hints.keys --remote S -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -F output/102-21_Subscriber-listener-remomte-hints.log

