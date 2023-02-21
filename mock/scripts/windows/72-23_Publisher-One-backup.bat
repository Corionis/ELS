@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/72-23_Publisher-One-backup_mismatches.txt -W output/72-23_Publisher-One-backup_whatsnew.txt -F output/72-23_Publisher-One-backup.log

cd /d "%base%"

