@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hint-server private/hint-server-Win8Pro-64T.json -K system/hint.keys -F output/902-10_Status-Server-listener.log

cd /d "%base%"

