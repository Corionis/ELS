@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\010-23_Backup_mismatches.txt -W output\010-23_Backup_whatsnew.txt -F output\010-23_Backup.log

cd /d "%base%"

