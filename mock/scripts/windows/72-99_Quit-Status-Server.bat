@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --force-quit --hint-server test/hints/hint-server.json -p test/publisher/publisher.json -F output/72-99_Quit-Status-Server.log

