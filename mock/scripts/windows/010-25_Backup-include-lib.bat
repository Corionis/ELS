@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\010-25_Backup-include-lib_mismatches.txt -W output\010-25_Backup-include-lib_whatsnew.txt -F output\010-25_Backup-include-lib.log -l Movies

cd /d "%base%"

