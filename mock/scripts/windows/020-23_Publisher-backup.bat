@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --remote P -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/subscriber-one-targets.json -m output/020-23_Publisher-backup_mismatches.txt -W output/020-23_Publisher-backup_whatsnew.txt -F output/020-23_Publisher-backup.log

cd /d "%base%"

