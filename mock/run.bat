@echo off

set base=%~dp0
cd /d %base%

java -cp "..\out\production\VolMonger\;..\lib;..\lib\*" com.groksoft.VolMonger -t -p TestRun\publisher\publisher-collection.json -s TestRun/subscriber-1/subscriber-1-collection.json
