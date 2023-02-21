@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/40-22_Publisher-dryrun_mismatches.txt -W output/40-22_Publisher-dryrun_whatsnew.txt -F output/40-22_Publisher-dryrun.log --dry-run

cd /d "%base%"

