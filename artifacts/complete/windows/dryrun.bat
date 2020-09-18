@echo off
REM Run ELS as a stand-alone local dry run process
REM
REM This script may be executed from a file browser.
REM All logging, Mismatches, and What's New files are written to the ..\output directory.
REM Any existing log file is deleted first.

set base=%~dp0
cd /d %base%

set name=%~n0

if not exist ..\output mkdir ..\output

if exist ..\output\%name%.log del /q ..\output\%name%.log

REM This is the same as the publisher.bat with the addition of --dry-run
java -jar %base%\..\ELS.jar -d debug --dry-run -p ..\meta\publisher.json -s  ..\meta\subscriber.json -T ..\meta\targets.json -m ..\output\%name%-Mismatches.txt -n ..\output\%name%-WhatsNew.txt -f ..\output\%name%.log
