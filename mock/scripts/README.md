# Test Scripts

Command-line scripts to test specific ELS application-level functionalities.

 *Note:* These are also an excellent example of the various ways ELS can be executed.

ELS is composed of several different capabilities:

 * Stand-alone back-up tool - the original
 * Network back-up tool, LAN or Internet
 * Built-in SFTP
 * Built-in STTY
 * Built-in interactive terminals
 * Local hint processing
 * Networked hint processing
 * Local Hint Tracker
 * Networked Hint Status Server (Hint Status Server/HSS)
 * Desktop Navigator GUI application


## Test Organization

Tests are *generally* organized in increasing options and functionality.

 [000-00  Basic Functionality](#000-00--basic-functionality)<br/>
 [010-00  Local Backup](#010-00--local-backup)<br/>
 [020-00  Remote Backup](#020-00--remote-backup)<br/>
 [030-00  Interactive Terminals](#030-00--interactive-terminals)<br/>
 [040-00  Local Hints] - Deprecated<br/>
 [050-00  Remote Hints] - Deprecated<br/>
 [060-00  Local Hint Tracker - Local Backup](#060-00--local-hint-tracker---local-backup)<br/>
 [062-00  Local Hint Tracker - Remote Backup](#062-00--local-hint-tracker---remote-backup)<br/>
 [070-00  Remote Hint Server - Local Backup](#070-00--remote-hint-server---local-backup)<br/>
 [072-00  Remote Hint Server - Remote Backup](#072-00--remote-hint-server---remote-backup)<br/>
 [080-00 Navigator - Local Backup](#080-00--navigator---local-backup)<br/>
 [082-00  Navigator - Remote Backup](#082-00--navigator---remote-backup)<br/>
 [090-00  Navigator - Local Hint Tracker - Local Backup](#090-00--navigator---local-hint-tracker---local-backup)<br/>
 [092-00  Navigator - Local Hint Tracker - Remote Backup](#092-00--navigator---local-hint-tracker---remote-backup)<br/>
 [100-00  Navigator - Remote Hint Server - Local Backup](#100-00--navigator---remote-hint-server---local-backup)<br/>
 [102-00  Navigator - Remote Hint Server - Remote Backup](#102-00--navigator---remote-hint-server---remote-backup)<br/>
 [110-00  Jobs](#110-00--jobs)


## Test Utility Scripts

``clear-output`` : Removes all files from the mock/output/ directory

``reset`` : Resets the configuration and test data

``reset-config`` : Only resets the configuration data

``reset-test`` : Only resets the test data


## Tests & Sequence

Some of the following scripts are run once. Others are run multiple times to complete a particular test
sequence.

Examine the screen and/or log output for warnings, errors and exceptions. Examine the test/ directory
for the appropriate changes at each step of the test sequences.

Automation and result-checking have not been, and might not be, implemented. That's a lot of work.
At this point it's a manual and visual process.

### 000-00  Basic Functionality

 * ``000-01_Version`` : Run once, does not create a log file
 * ``000-02_Bad-arguments`` : Run once, should see an exception
 * ``000-03_Validate`` : Run once
 * ``000-04_Export`` : Run once
 * ``000-05_Duplicates`` : Show dupes
 * ``000-06_Duplicates-crosscheck`` : Duplicate shown due to cross-check
 * ``000-07_Empty-directories`` : Empty directory check 


### 010-00  Local Backup

 * ``reset`` : Reset the test/ directory
 * ``010-22_Backup-dryrun`` : Run once
 * ``010-23_Backup`` : Run once
 * ``reset`` : Reset the test/ directory
 * ``010-24_Backup-exclude-lib`` : Run once
 * ``reset`` : Reset the test/ directory
 * ``010-25_Backup-include-lib`` : Run once


### 020-00  Remote Backup

 * ``reset`` : Reset the test/ directory
 * ``020-21_Subscriber-listener`` ; Separate terminal 1, stops when done
 * ``020-22_Publisher-dryrun`` : Separate terminal 2
 * ``020-21_Subscriber-listener`` : Separate terminal 1, stops when done
 * ``020-23_Publisher-backup`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``020-21_Subscriber-listener`` : Separate terminal 2
 * ``020-24_Publisher-backup-keepgoing`` : Separate terminal 1
 * ``020-89_Quit-Subscriber-listener`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``020-31_Subscriber-listener+blacklist`` : Separate terminal 1
 * ``020-23_Publisher-backup`` : Separate terminal 2, should fail to connect
 <p></p>

 * Ctrl-C on 20-31_Subscriber-listener+blacklist
 <p></p>

 * ``reset`` : Reset the test/ directory
 * ``020-41_Subscriber-listener+whitelist`` : Separate terminal 1
 * ``020-23_Publisher-backup`` : Separate terminal 2, should connect and run
 * ``reset`` : Reset the test/ directory
 * ``020-51_Subscriber-listener-auth`` : Separate terminal 1
 * ``020-23_Publisher-backup`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``020-61_Subscriber-listener-keepgoing`` : Separate terminal 1
 * ``020-22_Publisher-dryrun`` : Separate terminal 2, subscriber should keep going
 * ``020-23_Publisher-backup`` : Separate terminal 2
 * ``020-89_Quit-Subscriber-listener`` : Separate terminal 2


### 030-00  Interactive Terminals

Special command authorization use, with quotes:  auth "sharkbait"

 * ``reset`` : Reset the test/ directory
 * ``030-21_Subscriber-listener`` : Separate terminal 1
 * ``030-29_Publisher-manual`` : Separate terminal 2, bye leaves listener running, quit ends listener
 * ``030-31_Publisher-listener`` :  Separate terminal 1
 * ``030-39_Subscriber-terminal`` :  Separate terminal 2, bye leaves listener running, quit ends listener


### 060-00  Local Hint Tracker - Local Backup

The Local Hint Tracker tracks the processing status of each Hint on each back-up locally.

 * ``reset`` : Reset the test/ directory
 * ``090-26_Navigator-pub-local`` : Perform the [Manual Tests](#Manual-tests) below to create some Hints
 * ``060-01_Hints-publisher`` : Run once, if not Done in Navigator
 * ``060-22_Publisher-One-dryrun`` : Run once
 * ``060-23_Publisher-One-backup`` : Run once
 * ``060-24_Subscriber-One-backup`` : Run once, back-up to Publisher and process Hints
 * ``060-32_Publisher-Two-dryrun`` : Run once
 * ``060-33_Publisher-Two-backup`` : Run once


### 062-00  Local Hint Tracker - Remote Backup

 * ``reset`` : Reset the test/ directory
 * ``062-21_Subscriber-One-listener`` : Separate terminal 1
 * ``092-26_Navigator-remote`` : Perform the [Manual Tests](#Manual-tests) below to create some Hints
 * ``062-22_Publisher-One-dryrun`` : Separate terminal 2
 * ``062-21_Subscriber-One-listener`` : Separate terminal 1
 * ``062-23_Publisher-One-backup`` : Separate terminal 2
 * ``062-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``062-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``062-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``062-33_Publisher-Two-backup`` : Separate terminal 2


### 070-00  Remote Hint Server - Local Backup

 * ``reset`` : Reset the test/ directory
 * ``070-10_Status-Server-listener`` : Separate terminal 1
 * ``100-26_Navigator-remote-hints`` :  Perform the [Manual Tests](#Manual-tests) below to create some Hints
 * ``070-22_Publisher-One-dryrun`` ; Separate terminal 2
 * ``070-23_Publisher-One-backup`` : Separate terminal 2
 * ``070-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``070-33_Publisher-Two-backup`` : Separate terminal 2
 * ``070-99_Quit-Status-Server`` : Stop Hint Status Server


### 072-00  Remote Hint Server - Remote Backup

 * ``reset`` : Reset the test/ directory
 * ``072-10_Status-Server-listener`` : Separate terminal 1.
 * ``072-21_Subscriber-One-listener`` : Separate terminal 2
 * ``100-26_Navigator-remote-hints`` :  Perform the [Manual Tests](#Manual-tests) below to create some Hints
 * ``072-01_Hints-publisher`` : Run once, hints are tracked locally
 * ``072-22_Publisher-One-dryrun`` ; Separate terminal 3
 * ``072-21_Subscriber-One-listener`` : Separate terminal 2
 * ``072-23_Publisher-One-backup`` : Separate terminal 3
 * ``072-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``072-32_Publisher-Two-dryrun`` : Separate terminal 3
 * ``072-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``072-33_Publisher-Two-backup`` : Separate terminal 3
 * ``072-99_Quit-Status-Server`` : Stop Hint Status Server
 

### 080-00  Navigator - Local Backup

 * ``reset`` : Reset the test/ directory
 * ``080-01_Navigator-no-args`` : ELS with no arguments
 * ``080-02_Navigator-navigator`` : With --navigator option only
 * ``080-03_Navigator-publisher-only`` :: With a publisher
 * ``080-26_Navigator-local`` : With publisher as a Collection and local subscriber
 * ``080-27_Navigator-workstation`` : With publisher as a Workstation and local subscriber


### 082-00  Navigator - Remote Backup

 * ``082-21_Subscriber-One-listener`` : Start a subscriber listener
 * ``082-26_Navigator-remote`` : Navigator with publisher as Collection and remote subscriber


### 090-00  Navigator - Local Hint Tracker - Local Backup

 * ``090-26_Navigator-hint-keys`` : Navigator
 * Run test series "_Testing Navigator with Hints & Hint Tracking_" below
 * Run series ``060-00 Local Hint Tracker - Local Backup`` to process Hints


### 092-00  Navigator - Local Hint Tracker - Remote Backup

 * ``092-21_Subscriber-One-listener`` : Subscriber One listener
 * ``092-26_Navigator-remote`` : Navigator with remote Subscriber One
 * Run series ``062-00 Local Hint Tracker - Remote Backup`` to process Hints


### 100-00  Navigator - Remote Hint Server - Local Backup

 * ``100-10_Status-Server-listener`` : Status Server listener
 * ``100-26_Navigator-remote-hints`` : Navigator with remote Hints
 * Run series ``070-00 Remote Hint Server - Local Backup`` to process Hints


### 102-00  Navigator - Remote Hint Server - Remote Backup

 * ``102-10_Status-Server-listener`` : Status Server listener
 * ``102-21_Subscriber-One-listener-remote-hints`` : Subscriber One listener
 * ``102-26_Navigator-remote-hints`` : Navigator with remote Hints & Subscriber One
 * Run series ``072-00 Remote Hint Server - Remote Backup`` to process Hints


### 110-00  Jobs

 * tbd


## Testing Navigator with Hints & Hint Tracking

---

### Manual Test Checks

Check the Hint Tracker datastore/ file:
 * The local system is marked as Done
 * The command is correct

### Manual tests

 * M1 : Move root-level file
 * M2 : Move root-level directory & subdirectories
 * M3 : Move file in a subdirectory
 * M4 : Move subdirectory in a subdirectory
 * M5 : Move root-level file cross-libraries
 * M6 : Move root-level directory & subdirectories cross-libraries
   <p><br></p>
 * R1 : Rename root-level file
 * R2 : Rename root-level directory
 * R3 : Rename file in a subdirectory
 * R4 : Rename subdirectory in a subdirectory
   <p><br></p>
 * D1 : Delete root-level file
 * D2 : Delete root-level directory & subdirectories
 * D3 : Delete file in a subdirectory
 * D4 : Delete subdirectory in a subdirectory


-end-
