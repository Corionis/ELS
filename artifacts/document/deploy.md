# Deployment Procedure

After testing, etc.:

 1. Build ant target All-Deploy
 2. Copy out/ELS/zip/ELS/ to the Windows Installer root directory, delete any previous ELS/ directory
 3. In Windows run:  build-els-installer.bat
 4. Copy els-install.exe to the deploy/ directory
 5. Rename with the time-stamped name of the current build
 6. Commit and push, be sure to "add" the new files to git first

The date and time will be updated on the ELS website at https://corionis.github.io/ELS/
as part of the build and push.
