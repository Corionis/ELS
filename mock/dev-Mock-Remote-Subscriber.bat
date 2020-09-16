@echo off

set base=%~dp0
cd /d %base%

java -cp "..\out\production\ELS\;..\lib;..\lib\*" com.groksoft.els.Main -d debug -r S -p TestRun/publisher/publisher-libraries.json -s TestRun/subscriber-1/subscriber-1-libraries.json -T TestRun/targets-1.json -m TestRun/mismatches.txt -n TestRun/whatsnew.txt -f TestRun/els.log

