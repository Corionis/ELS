@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p libraries\publisher.json -s libraries\subscriber-one.json -T libraries\subscriber-one-targets.json -m output\010-23_Backup_mismatches.txt -W output\010-23_Backup_whatsnew.txt -F output\010-23_Backup.log

cd /d "%base%"

