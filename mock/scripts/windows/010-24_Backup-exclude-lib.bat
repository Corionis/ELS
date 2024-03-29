@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -p libraries\publisher.json -s libraries\subscriber-one.json -T libraries\subscriber-one-targets.json -m output\010-24_Backup-exclude-lib_mismatches.txt -W output\010-24_Backup-exclude-lib_whatsnew.txt -F output\010-24_Backup-exclude-lib.log -L "TV Shows"

cd /d "%base%"

