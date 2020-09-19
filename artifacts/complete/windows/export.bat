@echo off
REM Run ELS as a stand-alone local export-text and export-items process
REM
REM Use -d to add a date/time on the end of output filenames.
REM
REM This script may be executed from a file browser.
REM All logging, text and JSON files are written to the ..\output directory.
REM Any existing log file is deleted first.

set base=%~dp0
cd /d %base%

set name=%~n0

if not exist ..\output mkdir ..\output

if exist ..\output\%name%.log del /q ..\output\%name%.log

set dtime=
if %1z == -dz set dtime=%date:~-4%%date:~4,2%%date:~7,2%-%time:~0,2%%time:~3,2%%time:~6,2%

java -jar %base%\..\ELS.jar -d debug --dry-run -p ..\meta\publisher.json -s  ..\meta\subscriber.json -e ..\output\%name%-text%dtime%.txt -i ..\output\%name%-collection%dtime%.json -f ..\output\%name%%dtime%.log
