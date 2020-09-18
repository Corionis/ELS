@echo off
REM Run ELS as a remote publisher manual (interactive) process
REM
REM Run subscriber-listener.bat first.
REM
REM Requests new collection and targets files from the subscriber.
REM This allows the subscriber to make changes without sending those
REM to the publisher separately.
REM
REM This script may be executed from a file browser.
REM All logging, Mismatches, and What's New files are written to the ..\output directory.
REM Any existing log file is deleted first.

set base=%~dp0
cd /d %base%

set name=%~n0

if not exist ..\output mkdir ..\output

if exist ..\output\%name%.log del /q ..\output\%name%.log

java -jar %base%\..\ELS.jar -d debug --remote M -p ..\meta\publisher.json -s  ..\meta\subscriber.json -t ..\meta\targets.json -f ..\output\%name%.log
