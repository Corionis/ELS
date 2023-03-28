@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -c debug -d debug --hint-server private/hint-server-Win8Pro-64T.json -k system/hint.keys --remote P -p private/win8pro-64t.json -s private/clavius-subscriber-one.json -T -m output/902-45_Publisher-One-backup_mismatches.txt -W output/902-45_Publisher-One-backup_whatsnew.txt -F output/902-45_Publisher-clavius-backup.log

cd /d "%base%"

