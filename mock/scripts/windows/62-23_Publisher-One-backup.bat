@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -r P -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -m output/62-23_Publisher-One-backup_mismatches.txt -W output/62-23_Publisher-One-backup_whatsnew.txt -F output/62-23_Publisher-One-backup.log

