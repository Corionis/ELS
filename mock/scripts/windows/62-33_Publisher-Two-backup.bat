@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -r P -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/62-33_Publisher-Two-backup_mismatches.txt -W output/62-33_Publisher-Two-backup_whatsnew.txt -F output/62-33_Publisher-Two-backup.log

cd /d "%base%"

