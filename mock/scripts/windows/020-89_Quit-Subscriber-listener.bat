@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --listener-quit -p "libraries\Publisher.json" -O -s "libraries\Subscriber One.json" -F output\020-89_Quit-subscriber-listener.log

cd /d "%base%"

