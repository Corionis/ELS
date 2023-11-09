@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -S libraries/subscriber-two.json -T -m output/070-33_Publisher-Two-backup_mismatches.txt -W output/070-33_Publisher-Two-bacup_whatsnew.txt -F output/070-33_Publisher-Two-backup.log

cd /d "%base%"

