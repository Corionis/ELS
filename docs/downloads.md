---
layout: default
show_blog: false
---
# Downloads & Updates

## &bull; Supported Platforms

Entertainment Library Synchronizer (ELS) runs on Linux, macOS and Windows. 

## &bull; Disclaimer

Any software downloaded from this site is free and without warranty or guarantee of any
kind expressed or implied. The software may not work or serve any intended or particular
purpose. Use at your own risk. If you break it you own both parts.

## &bull; Support

If a bug is found please create an 
<a href="{{ site.issues_url }}" target="_blank"><b>Issue <img src="assets/images/link.png" alt="" title="On GitHub" align="bottom"  border="0"></b></a>
on the GitHub Project.


## &bull; Downloads

**Note:** ELS 4.0 is in development therefore bugs are to be expected. The command line back-up component works well. The GUI is new.

Use the buttons at the top of this page <img style="vertical-align:middle" src="assets/images/swoop-up-arrow.png" border="0"/> for your platform.

## &bull; Installation

 * ELS may be used with only the ELS jar. Requires a Java 19+ JRE.
 * The other downloads contain an embedded Java JRE that will not interfere with any existing Java installation, native
   launchers for macOS and Windows and other supporting files.
 * The default configuration directory for Library and other files is: **[USER HOME]/.els/** which may be changed
   using the `` -C [directory] `` option.
 * The software installation directory may be the same as the configuration directory if desired. Updates only
   replace the **bin/** and **rt/** directories and the original launcher script or program. Other directories
   in the configuration directory are not changed.
 * Using remote connections may require adjustments to network settings, firewalls, NAT, etc.
   which is beyond the scope of this document.

The following downloads contain an embedded JRE for that platform.

### &nbsp;&nbsp;&nbsp; Linux tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
 * In a terminal change to the  **[install directory]/bin/**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: ``` ./ELS-Navigator.sh ```

### &nbsp;&nbsp;&nbsp; macOS tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
 * In a terminal change directory to the **[install directory]/bin/**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: `` ./fix-permissions.sh `` to adjust the permissions of the executables.
 * Change directory to the **[install directory]**<br/>
   &nbsp;&nbsp;&nbsp;&nbsp;Run: ``` ./ELS-Navigator.sh ```
 * Use ELS Navigator File, Generate to generate a desktop 'open' script for the current configuration
   that will use the ELS-Navigator.app launcher app. 

### &nbsp;&nbsp;&nbsp; Windows installer

 * The installation directory may be changed with the Options button.<br/>
   A desktop shortcut is created. A standard uninstaller is included. Entertainment Library Synchronizer will
   appear in the Start Menu, and in Control Panel, Programs & Features.

### &nbsp;&nbsp;&nbsp; Windows zip

 * Unpack the .zip anywhere you have write permission.<br/>
   Run ``` ELS-Navigator.exe ``` in the root of that directory.


## &bull; Updates

Use Help, Check for Updates ... to see if a new build is available.

ELS contains a built-in Updater except for macOS. Mac users may download and replace the
ELS.jar in the **[install directory]/bin/** directory or replace ELS using the .tar.

For Windows users the Control Panel, Programs and Features, will not reflect the latest update
when using the internal updater. 

### &nbsp;&nbsp;&nbsp; Linux and Windows

 * A dialog is displayed with Yes, No, and Changelist that will display the list of the latest changes.
   Yes abd the latest auto-updater is downloaded and run from the GitHub project.

### &nbsp;&nbsp;&nbsp; macOS

 * A dialog is displayed with Changelist, Cancel, and ELS website. Due to constraints there is no
   auto-updater for macOS. Mac users may download the latest ELS Jar or update ELS entirely 
   using the .mac.tar.gz.

## &bull; Configuration

 * By default ELS data files are store in your home directory in the **.els** subdirectory.
   * That may be changed with the -C \| \--config option.
 * If ELS is installed in a separate directory that folder may be replaced with an update.
