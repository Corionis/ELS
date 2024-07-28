@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -J --hint-server libraries\hint-server.json -k system\hint.keys -c debug -d debug -p libraries\publisher.json -s libraries\subscriber-two.json -T -m output\070-32_Publisher-Two-dryrun_mismatches.txt -W output\070-32_Publisher-Two-dryrun_whatsnew.txt -F output\070-32_Publisher-Two-dryrun.log --dry-run

cd /d "%base%"

