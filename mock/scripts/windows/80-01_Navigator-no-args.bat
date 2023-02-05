@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug -F output/80-01_Navigator.log $*

