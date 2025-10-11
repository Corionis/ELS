---
layout: default
show_blog: false
---
# Downloads & Updates

## &bull; Disclaimer

Any software downloaded from this site is free and without warranty or guarantee of any
kind expressed or implied. The software may not work or serve any intended or particular
purpose. Use at your own risk. If you break it you own both parts.

## &bull; Downloads

Use the buttons at the top of this page <img style="vertical-align:middle" src="assets/images/swoop-up-arrow.png" border="0"/> for your platform.

If you are interested in the latest development work see: <a href="https://github.com/Corionis/ELS/tree/Version-4.1.0/deploy" target="_blank"><b>Version 4.1 deploy <img src="assets/images/link.png" alt="" title="On GitHub" align="bottom" border="0"/></b></a>

## &bull; Installation

### &nbsp;&nbsp;&nbsp; General

 * ELS may be used with only the ELS jar. Requires a Java 19+ JRE.
 * The other downloads contain an embedded Java JRE that will not interfere with any existing Java installation, native
   launchers for each operating system and other supporting files.
 * The default configuration directory for Library and other files is: **[USER HOME]/.els/** which may be changed
   using the `` -C [directory] `` option.
 * The software installation directory may be the same as the configuration directory if desired. Updates only
   replace the **bin/** and **rt/** directories and the original launcher script or program. Other directories
   in the configuration directory are not changed.
 * Directories or symbolic links for collection data may be created in the
   configuration directory. 
 * Using remote connections may require adjustments to network settings, firewalls, NAT, etc.
   which is beyond the scope of this document.
 
### &nbsp;&nbsp;&nbsp; Linux tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
 * In a terminal change to the  **[install directory]/**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: ``` ./ELS-Navigator.sh ```
* Use ELS Navigator File, Generate to generate a desktop shortcut for the current
  configuration that will use the ELS-Navigator.sh launcher script.

### &nbsp;&nbsp;&nbsp; macOS tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
 * In a terminal change directory to the **[install directory]/bin/**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: `` ./fix-permissions.sh `` to adjust the permissions
   of the executables.
 * Change directory to the **[install directory]**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: ``` ./ELS-Navigator.sh ``` or the ``ELS-Navigator`` app.
 * Use ELS Navigator File, Generate to generate a desktop 'open' script for the
   current configuration. There is an option to use either the script or launcher app
   in System, Preferences.

### &nbsp;&nbsp;&nbsp; Windows installer

 * The installation directory may be changed with the Options button.<br/>
   A desktop shortcut is created. A standard uninstaller is included. Entertainment Library Synchronizer will
   appear in the Start Menu, and in Control Panel, Programs & Features.

### &nbsp;&nbsp;&nbsp; Windows zip

 * Unpack the .zip anywhere you have write permission.<br/>
   Run ``` ELS-Navigator.exe ``` in the root of that directory.


## &bull; Updates

### From Navigator

A widget is displayed in the upper-right corner if an update is available. Or use
Help, Check for Updates.

A dialog is displayed with Yes, No, and Changelist that will display the list of the latest changes.

For Windows users Control Panel, Programs and Features, will not reflect the latest update
information when using the internal updater. The Windows Installer may be
downloaded and run to update that information if desired.

### From Command Line

To support systems that do not have a graphic desktop 2 scripts are available in
the bin/ directory. 

 * ``checkUpdate`` will check for availability. Exits with 0 if none or 1 if available.
 * ``installUpdate`` will download and install the update regardless of whether the
   versions match.

## &bull; Configuration

 * By default ELS data files are stored in your home directory in the **.els/** subdirectory.
   * That may be changed with the -C \| \--config option.
 

 * If ELS is installed in a separate directory from the configuration that folder may be replaced with an update.

<br/><br/>