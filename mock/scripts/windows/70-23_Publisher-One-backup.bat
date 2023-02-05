@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -k test/test-hints.keys -p test/publisher/publisher.json -s test/subscriber-one/subscriber-one.json -T -m output/70-23_Publisher-One-backup_mismatches.txt -W output/70-23_Publisher-One-backup_whatsnew.txt -F output/70-23_Publisher-One-backup.log

