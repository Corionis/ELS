@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -k system/hint.keys -c debug -d debug --remote P -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/50-33_Publisher-Two-dryrun_mismatches.txt -W output/50-33_Publisher-Two-dryrun_whatsnew.txt -F output/50-33_Publisher-Two-dryrun.log

