@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -F output\080-01_Navigator.log $*

cd /d "%base%"

