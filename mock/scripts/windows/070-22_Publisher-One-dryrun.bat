@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-one.json -T -m output/070-22_Publisher-One-dryrun_mismatches.txt -W output/070-22_Publisher-One-dryrun_whatsnew.txt -F output/070-22_Publisher-One-dryrun.log --dry-run

cd /d "%base%"

