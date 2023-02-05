@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -k test/test-hints.keys -c debug -d debug --remote S --authorize sharkbait -p test/publisher/publisher.json -S test/subscriber-two/subscriber-two.json -T -F output/62-31_Subscriber-Two-listener.log

