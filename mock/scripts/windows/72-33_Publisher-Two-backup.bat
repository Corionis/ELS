@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --hint-server test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug --remote P -p test/publisher/publisher.json -S test/subscriber-two/subscriber-two.json -T -m output/72-33_Publisher-Two-backup_mismatches.txt -W output/72-33_Publisher-Two-bacup_whatsnew.txt -F output/72-33_Publisher-Two-backup.log

