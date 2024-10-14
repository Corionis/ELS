@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p "libraries\Publisher.json" -F output\000-06_Duplicates-crosscheck.log --duplicates --cross-check

cd /d "%base%"

