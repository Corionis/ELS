@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints "libraries\Hint Server.json" -k "system\hint.keys" -c debug -d debug -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\060-23_Publisher-One-backup_mismatches.txt -W output\060-23_Publisher-One-backup_whatsnew.txt -F output\060-23_Publisher-One-backup.log

cd /d "%base%"

