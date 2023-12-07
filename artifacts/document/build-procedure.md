# ELS Build Procedure

ELS is built using Ant with implementations for Linux, MacOS and Windows.

A "build" is separate from a "deploy" so work can continue without
impacting ELS "Check for Updates".

The primary build platform is Linux, although ELS may be built
on and for MacOS and Windows with the available targets.

Final steps for MacOS and Windows are performed on those 
platforms then copied to the Linux build/ directory. Those two
build installers and DMG files.

Deployment is a separate step, a task in the els.xml Ant build
file. When "Deploy" is executed and committed to GitHub the ELS 
"Check for Updates" is triggered and the build information on
the web site is updated.

## Full Build Steps

 1. On Linux in ELS run: ```ant -f els.xml All``` or ```ant -f els.xml All-Deploy```

 2. On MacOS:
    1. Delete any previous ```ELS-MacOS-Imager/ELS/``` and ```ELS-MacOS-Imager/ELS-Updater/``` directories.
    2. Copy from Linux ```ELS/out/ELS/dmg/ELS``` to ```ELS-MacOS-Imager/```
    2. Copy from Linux ```ELS/out/updater/dmg/ELS_Updater``` to ```ELS-MacOS-Imager/```
    3. Change to ELS=MacOS-Imager/
    4. Run: ```./build-els-image.sh```
    5. Copy to Linux ```ELS=MacOS-Imager/build/ELS-[build-stamp].dmg``` to ```ELS/build/```
    6. Copy to Linux ```ELS=MacOS-Imager/build/ELS_Updater-[build-stamp].dmg``` to ```ELS/build/```
 
 3. On Windows:
    1. Delete any previous ```ELS-Windows-Installer\ELS\``` directory.
    2. Copy from Linux ```ELS/out/ELS/zip/ELS``` to ```ELS-Windows-Installer```
    3. Change to ELS-Windows-Installer\
    4. Run: ```build-els-installer.bat```
    5. Copy to Linux ```ELS-Windows-Installer\els-installer.exe``` to ```ELS/build/```
    6. Rename ```ELS/build/els-install.exe``` with the current ELS-date-stamped filename.
