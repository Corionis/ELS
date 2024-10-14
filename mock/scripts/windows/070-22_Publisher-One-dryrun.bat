@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -J --hint-server "libraries\Hint Server.json" -k "system\hint.keys" -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\070-22_Publisher-One-dryrun_mismatches.txt -W output\070-22_Publisher-One-dryrun_whatsnew.txt -F output\070-22_Publisher-One-dryrun.log --dry-run

cd /d "%base%"

