@echo off

set base=%~dp0
cd /d %base%

set name=%~n0

%base%\rt\bin\java -jar %base%\bin\ELS.jar -n -c Debug -d Debug -F output\Navigator.log

