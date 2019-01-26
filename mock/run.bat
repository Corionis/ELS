@echo off

set base=%~dp0
cd /d %base%

java -cp "..\out\production\VolMunger\;..\lib;..\lib\*" com.groksoft.volmunger.Main %*
