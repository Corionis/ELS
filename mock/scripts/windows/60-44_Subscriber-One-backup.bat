@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -p test/subscriber-one/subscriber-one.json -s test/publisher/publisher.json -T -m output/60-44_Subscriber-One-backup_mismatches.txt -W output/60-44_Subscriber-One-backup_whatsnew.txt -F output/60-44_Subscriber-One-backup.log

