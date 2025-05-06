@echo off

set base=%~dp0
cd /d "%base%"

cd "C:\Users\%USERNAME%\AppData\Local\Temp\ELS_Updater_%USERNAME%"

REM Assumes 130-02_InstallUpdate.sh has been run
rt\bin\java -jar bin\ELS.jar -Y

cd /d "%base%"

