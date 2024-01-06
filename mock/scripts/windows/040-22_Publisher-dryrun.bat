@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/040-22_Publisher-dryrun_mismatches.txt -W output/040-22_Publisher-dryrun_whatsnew.txt -F output/040-22_Publisher-dryrun.log --dry-run

cd /d "%base%"

