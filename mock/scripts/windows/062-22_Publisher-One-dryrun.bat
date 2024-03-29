@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints libraries\hint-server.json -k system\hint.keys -c debug -d debug -r P -p libraries\publisher.json -s libraries\subscriber-one.json -T -m output\062-22_Publisher-One-dryrun_mismatches.txt -W output\062-22_Publisher-One-dryrun_whatsnew.txt -F output\062-22_Publisher-One-dryrun.log --dry-run

cd /d "%base%"

