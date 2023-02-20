@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -S libraries/subscriber-two.json -T -m output/70-33_Publisher-Two-backup_mismatches.txt -W output/70-33_Publisher-Two-bacup_whatsnew.txt -F output/70-33_Publisher-Two-backup.log

