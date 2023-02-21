@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T libraries/targets.json -m output/10-22_Backup-dryrun_mismatches.txt -W output/10-22_Backup-dryrun_whatsnew.txt -F output/10-22_Backup-dryrun.log --dry-run

cd /d "%base%"

