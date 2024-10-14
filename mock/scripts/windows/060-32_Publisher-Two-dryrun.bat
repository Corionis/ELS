@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints "libraries\Hint Server.json" -k "system\hint.keys" -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber Two.json" -T -m output\060-32_Publisher-Two-dryrun_mismatches.txt -W output\060-32_Publisher-Two-dryrun_whatsnew.txt -F output\060-32_Publisher-Two-dryrun.log --dry-run

cd /d "%base%"

