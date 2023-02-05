@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -K test/test-hints.keys -F output/70-10_Status-Server-listener.log

