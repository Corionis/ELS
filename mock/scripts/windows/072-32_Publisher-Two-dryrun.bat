@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -J --hint-server "libraries\Hint Server.json" -k "system\hint.keys" -c debug -d debug -O --remote P -p "libraries\Publisher.json" -s "libraries\Subscriber Two.json" -T -m output\072-32_Publisher-Two-dryrun_mismatches.txt -W output\072-32_Publisher-Two-dryrun_whatsnew.txt -F output\072-32_Publisher-Two-dryrun.log --dry-run

cd /d "%base%"

