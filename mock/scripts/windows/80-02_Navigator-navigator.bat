@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --navigator -c debug -d debug -F output/80-02_Navigator-navigator.log $*

