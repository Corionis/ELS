@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/targets.json -m output/10-24_Backup-exclude-lib_mismatches.txt -W output/10-24_Backup-exclude-lib_whatsnew.txt -F output/10-24_Backup-exclude-lib.log -L "TV Shows"

cd /d "%base%"

