@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/subscriber-one.json -s libraries/publisher.json -T -m output/60-44_Subscriber-One-backup_mismatches.txt -W output/60-44_Subscriber-One-backup_whatsnew.txt -F output/60-44_Subscriber-One-backup.log

cd /d "%base%"

