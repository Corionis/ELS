@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -j "500 Subscribe One Listener" -F "output\500 Subscribe One Listener.log"

cd /d "%base%"

