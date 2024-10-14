@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -F output\000-02_Bad-arguments.log -a-bad-argument

cd /d "%base%"

