@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --navigator -c debug -d debug -p test/publisher/publisher.json -F output/80-03_Navigator-publisher-only.log $*

