@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar bin/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -S libraries/subscriber-two.json -T -m output/050-32_Publisher-Two-dryrun_mismatches.txt -W output/050-32_Publisher-Two-dryrun_whatsnew.txt -F output/050-32_Publisher-Two-dryrun.log --dry-run

cd /d "%base%"

