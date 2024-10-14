@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints "libraries\Hint Server.json" -k "system\hint.keys" -c debug -d debug -O --remote P -p "libraries\Subscriber One.json" -s "libraries\Publisher.json" -T -m output\062-44_Subscriber-One-backup_mismatches.txt -W output\062-44_Subscriber-One-backup_whatsnew.txt -F output\062-44_Subscriber-One-backup.log

cd /d "%base%"

