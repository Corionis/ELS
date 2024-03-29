@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug --remote P -p libraries\publisher.json -S libraries\subscriber-one.json -T -m output\020-23_Publisher-backup_mismatches.txt -W output\020-23_Publisher-backup_whatsnew.txt -F output\020-23_Publisher-backup.log

cd /d "%base%"

