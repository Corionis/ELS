@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

if not exist output mkdir output

java -jar ../deploy/ELS.jar --navigator -c debug -d debug -F output/80-01_Navigator.log

cd /d %base%
