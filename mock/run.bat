@echo off

set base=%~dp0
cd /d %base%

java -cp "..\out\production\ELS\;..\lib;..\lib\*" com.groksoft.els.Main %*
