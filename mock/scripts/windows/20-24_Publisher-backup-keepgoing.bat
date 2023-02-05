@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --remote P --listener-keep-going -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T test/subscriber-one/targets.json -m output/20-24_Publisher-backup-keepgoing_mismatches.txt -W output/20-24_Publisher-backup-keepgoing_whatsnew.txt -F output/20-24_Publisher-keepgoing-backup.log

