@echo off
REM Capture .els Hint files from test directory
REM
REM   -c  Capture .els files and datastore
REM   -r  Reset test data and restore .els files into it
REM   -d  Change captured .els files status from Done to For

set base=%~dp0
cd /d "%base%"

set name=%~n0

if "z%1" == "z-c" goto Capture
if "z%1" == "z-r" goto Reset
if "z%1" == "z-d" goto Replace
goto Done


REM -c
:Capture
cd /d "%base%"

cd ../..
if exist capture rmdir /s /q capture

echo Copying .els files
cd test
robocopy /s . ..\capture *.els

cd ..
if not exist datastore goto Done
if exist capture-ds rmdir /s /q capture-ds
robocopy /s .\datastore .\capture-ds
goto Done


REM -r 
:Reset
cd /d "%base%"
call .\reset.bat -f

cd /d "%base%"
cd ..\..
if not exist capture goto Done
echo Copying .els files to test
cd capture
robocopy /s . ..\test *.els

cd ..
if not exist capture-ds goto Done
echo Copying .els files to datastore
robocopy /s .\capture-ds .\datastore
goto Done


REM -d
:Replace
cd /d "%base%"
echo Changing Done to For in test .els files

cd ..\..\test
..\scripts\windows\fart -i -r -- *.els Done For

cd ..
if not exist datastore goto Done
echo/
echo Changing Done to For in datastore.els files
cd datastore
..\scripts\windows\fart -i -- *.els Done For
goto Done


:Done
cd /d "%base%"
