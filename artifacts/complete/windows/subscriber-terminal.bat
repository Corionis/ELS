@echo off
REM Run ELS as a remote subscriber terminal (interactive) process
REM
REM NOTE: Publisher and Subscriber are reversed!
REM
REM Use -d to add a date/time on the end of output filenames.
REM
REM Run publisher-listener.bat first.
REM
REM Forces the remote publisher to get new collection and targets files.
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

set dtime=
if %1z == -dz set dtime=%date:~-4%%date:~4,2%%date:~7,2%-%time:~0,2%%time:~3,2%%time:~6,2%

java -jar %base%\..\ELS.jar -d debug --remote T -p ..\meta\publisher.json -S  ..\meta\subscriber.json -T ..\meta\targets.json -f ..\output\%name%-%dtime%.log
