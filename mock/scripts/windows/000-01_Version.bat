@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar --version

cd /d "%base%"
