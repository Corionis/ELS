@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -k test/test-hints.keys -c debug -d debug --remote P -p test/publisher/publisher.json -s test/subscriber-two/subscriber-two.json -T -m output/50-33_Publisher-Two-dryrun_mismatches.txt -W output/50-33_Publisher-Two-dryrun_whatsnew.txt -F output/50-33_Publisher-Two-dryrun.log

