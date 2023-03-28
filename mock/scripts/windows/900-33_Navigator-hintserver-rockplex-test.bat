@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug --hint-server private/hint-server-Win8Pro-64T.json -k system/hint.keys --remote P -P private/win8pro-64t.json -S private/rockplex-test.json -F output/902-23_Navigator-hintserver-rockplex-test.log

cd /d "%base%"

