@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug -j "50 Any Renamer Tests" --remote J -p test/publisher/publisher.json -S test/subscriber-one/subscriber-one.json -F output/110-24_Publisher-any-publisher-job.log

