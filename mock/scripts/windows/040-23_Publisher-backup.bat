@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt/bin/java -jar bin/ELS.jar -C . -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/040-23_Publisher-backup_mismatches.txt -W output/040-23_Publisher-backup_whatsnew.txt -F output/040-23_Publisher-backup.log

cd /d "%base%"

