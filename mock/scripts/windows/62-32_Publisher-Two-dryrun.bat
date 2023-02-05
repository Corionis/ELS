@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar --hints test/hints/hint-server.json -k test/test-hints.keys -c debug -d debug -r P -p test/publisher/publisher.json -s test/subscriber-two/subscriber-two.json -T -m output/62-32_Publisher-Two-dryrun_mismatches.txt -W output/62-32_Publisher-Two-dryrun_whatsnew.txt -F output/62-32_Publisher-Two-dryrun.log --dry-run

