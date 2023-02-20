@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . --hints libraries/hint-server.json -k system/hint.keys -c debug -d debug -p libraries/publisher.json -s libraries/subscriber-two.json -T -m output/60-32_Publisher-Two-dryrun_mismatches.txt -W output/60-32_Publisher-Two-dryrun_whatsnew.txt -F output/60-32_Publisher-Two-dryrun.log --dry-run

