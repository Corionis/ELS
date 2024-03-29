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

Use the buttons at the top of this page <img style="vertical-align:middle" src="assets/images/swoop-up-arrow.png" border="0"/> for your platform.

## &bull; Installation

 * Using the ELS jar requires a Java 19+ JRE.
 * Using remote connections may require adjustments to network settings, firewalls, etc.
   which is beyond the scope of this document.

The following downloads contain an embedded JRE for that platform.

### &nbsp;&nbsp;&nbsp; Linux tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
   Run ``` ELS-Navigator.sh ``` script in the root of that directory.

### &nbsp;&nbsp;&nbsp; macOS tar

 * Unpack the .tar.gz anywhere you have write permissions.<br/>
   Run ``` ELS-Navigator.sh ``` script in the root of that directory.
 * If you see an error dialog saying it cannot be validated open a terminal, change to the installed directory:<br/>
   ``xattr -d com.apple.quarantine ELS/rt/Contents/Home/bin/java``

### &nbsp;&nbsp;&nbsp; Windows installer

 * The installation directory may be changed with the Options button.<br/>
   A desktop shortcut is created. A standard uninstaller is included. Entertainment Library Synchronizer will
   appear in the Start Menu, and in Control Panel, Programs & Features.

### &nbsp;&nbsp;&nbsp; Windows zip

 * Unpack the .zip anywhere you have write permission.<br/>
   Run ``` ELS-Navigator.exe ``` in the root of that directory.


## &bull; Updates

Use Help, Check for Updates ... to see if a new build is available.

### &nbsp;&nbsp;&nbsp; Linux and Windows

 * A dialog is displayed with Yes, No, and Changelist that will display the list of the latest changes.
   Yes abd the latest auto-updater is downloaded and run from the GitHub project.

### &nbsp;&nbsp;&nbsp; macOS

 * A dialog is displayed with Changelist, No, and ELS website. Due to constraints there is no
   auto-updater for macOS. Mac users may download the latest ELS Jar or update ELS entirely 
   using the .mac.tar.gz.
 * If you see an error dialog saying it cannot be validated open a terminal, change to the installed directory:<br/>
   ``xattr -d com.apple.quarantine ELS/rt/Contents/Home/bin/java``

## &bull; Configuration

 * By default ELS data files are store in your home directory in the **.els** subdirectory.
   * That may be changed with the -C \| \--config option.
 * If ELS is installed in a separate directory that folder may be replaced with an  update.
