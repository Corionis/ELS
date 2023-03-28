@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug --remote P -P private/win8pro-64t.json -S private/rockplex-test.json -F output/900-23_Navigator-RockPlex.log

cd /d "%base%"

