@echo off

set base=%~dp0
cd /d "%base%"
cd ..\..

rt\bin\java -jar bin\ELS.jar -C . -c debug -d debug -O --remote P --listener-keep-going -p "libraries\Publisher.json" -s "libraries\Subscriber One.json" -T -m output\020-24_Publisher-backup-keepgoing_mismatches.txt -W output\020-24_Publisher-backup-keepgoing_whatsnew.txt -F output\020-24_Publisher-keepgoing-backup.log

cd /d "%base%"

