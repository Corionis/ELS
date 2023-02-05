@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T test/subscriber-one/targets.json -m output/10-25_Backup-include-lib_mismatches.txt -W output/10-25_Backup-include-lib_whatsnew.txt -F output/10-25_Backup-include-lib.log -l Movies

