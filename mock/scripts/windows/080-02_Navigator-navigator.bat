@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --navigator -c debug -d debug -F output\080-02_Navigator-navigator.log $*

cd /d "%base%"

