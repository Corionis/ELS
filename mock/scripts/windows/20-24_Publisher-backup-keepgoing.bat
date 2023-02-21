@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --remote P --listener-keep-going -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/targets.json -m output/20-24_Publisher-backup-keepgoing_mismatches.txt -W output/20-24_Publisher-backup-keepgoing_whatsnew.txt -F output/20-24_Publisher-keepgoing-backup.log

cd /d "%base%"

