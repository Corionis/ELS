@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --remote P -p libraries\publisher.json -s libraries\subscriber-one.json -T -m output\020-22_Publisher-dryrun_mismatches.txt -W output\020-22_Publisher-dryrun_whatsnew.txt -F output\020-22_Publisher-dryrun.log --dry-run

cd /d "%base%"

