@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --blacklist system\blacklist.txt -O --remote S -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\020-31_Subscriber-listener+blacklist.log

cd /d "%base%"

