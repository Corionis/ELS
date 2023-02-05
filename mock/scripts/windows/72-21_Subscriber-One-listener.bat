@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

cd ..\..

java -jar ../deploy/ELS.jar -c debug -d debug --hint-server test/hints/hint-server.json -k test/test-hints.keys --remote S --authorize sharkbait -p test/publisher/publisher.json -S test/subscriber-one/subscriber-one.json -T -F output/72-21_Subscriber-One-listener.log

