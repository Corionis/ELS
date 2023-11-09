@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -r P -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/062-23_Publisher-One-backup_mismatches.txt -W output/062-23_Publisher-One-backup_whatsnew.txt -F output/062-23_Publisher-One-backup.log

cd /d "%base%"

