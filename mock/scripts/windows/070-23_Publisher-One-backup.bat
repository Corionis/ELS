@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -J --hint-server "libraries\Hint Server.json" -k "system\hint.keys" -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\070-23_Publisher-One-backup_mismatches.txt -W output\070-23_Publisher-One-backup_whatsnew.txt -F output\070-23_Publisher-One-backup.log

cd /d "%base%"

