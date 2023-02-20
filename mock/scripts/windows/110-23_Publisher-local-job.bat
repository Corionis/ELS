@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -j "20 Renamer Tests" -F output/110-23_Publisher-local-job.log

