@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -j "503 Sub+Pub" -F "output\110-24_Subscriber-listener-job+Remote-publish-job.log"

cd /d "%base%"

