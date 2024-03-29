@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . --hints libraries\hint-server.json -k system\hint.keys -c debug -d debug -r P -p libraries\publisher.json -s libraries\subscriber-two.json -T -m output\062-33_Publisher-Two-backup_mismatches.txt -W output\062-33_Publisher-Two-backup_whatsnew.txt -F output\062-33_Publisher-Two-backup.log

cd /d "%base%"

