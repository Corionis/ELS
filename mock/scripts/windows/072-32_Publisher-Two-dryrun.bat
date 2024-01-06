@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hint-server libraries/hint-server.json -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/072-32_Publisher-Two-dryrun_mismatches.txt -W output/072-32_Publisher-Two-dryrun_whatsnew.txt -F output/072-32_Publisher-Two-dryrun.log --dry-run

cd /d "%base%"

