@echo off

set base=%~dp0
cd /d "%base%"

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -C . -K system/hint.keys -c debug -d debug -p libraries/publisher.json -T -F output/40-02_Hints-publisher-dryrun.log --dry-run

cd /d "%base%"

