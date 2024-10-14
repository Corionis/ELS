@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints "libraries\Hint Server.json" --keys-only "system\hint.keys" -c debug -d debug -p "libraries\Publisher.json" -T -F output\072-01_Hints-publisher.log

cd /d "%base%"

