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
 * Desktop ELS Navigator GUI


## Test Organization

Tests are *generally* organized in increasing options and functionality.

 00-    Basic Functionality

 10-    Local Backup

 20-    Remote Backup

 30-    Interactive terminals

 40-    Local Hints

 50-    Remote Hints

 60-    Local Hint Tracker

 70-    Remote Hint Server

 80-    Navigator


## Test Utility Scripts

``clear-output.sh`` : Removes all files from the mock/output/ directory

``reset.sh`` : Resets the mock/test/ directory by deleting it and copying the 
mock/media-base_copy-only directory to a new mock/test/ directory


## Tests & Sequence

Some of the following scripts are run once. Others are run multiple times to complete a particular test
sequence.

Examine the screen and/or log output for warnings, errors and exceptions. Examine the test/ directory
for the appropriate changes at each step of the test sequences.

Automation and result-checking have not been, and might not be, implemented. It's a lot of work.
At this point it's a manual and visual process.

### 00-00  Basic Functionality

* ``00-01_Version.sh`` : Run once, does not create a log file

* ``00-02_Bad-arguments.sh`` : Run once, should see an exception

* ``00-03_Validate.sh`` : Run once

* ``00-04_Export.sh`` : Run once

* ``00-05_Duplicates.sh`` : Show dupes

* ``00-06_Duplicates-crosscheck.sh`` : Duplicate shown due to cross-check

* ``00-07_Empty-directories.sh`` : Empty directory check 

### 10-00  Local Backup

* ``reset.sh`` : Reset the test/ directory

* ``10-22_Backup-dryrun.sh`` : Run once

* ``10-23_Backup.sh`` : Run once

* ``reset.sh`` : Reset the test/ directory

* ``10-24_Backup-exclude-lib.sh`` : Run once

* ``reset.sh`` : Reset the test/ directory

* ``10-25_Backup-include-lib.sh`` : Run once

### 20-00 Remote Backup

* ``reset.sh`` : Reset the test/ directory

* ``20-21_Subscriber-listener.sh`` ; Separate terminal 1

* ``20-22_Publisher-dryrun.sh`` : Separate terminal 2

* ``20-21_Subscriber-listener.sh`` : Separate terminal 1

* ``20-23_Publisher-backup.sh`` : Separate terminal 2

* ``reset.sh`` : Reset the test/ directory

* ``20-21_Subscriber-listener.sh`` : Separate terminal 2

* ``20-24_Publisher-backup-keepgoing.sh`` : Separate terminal 1

* ``20-99_Quit-Subscriber-listener.sh`` : Separate terminal 2

* ``reset.sh`` : Reset the test/ directory

* ``20-31_Subscriber-listener+blacklist.sh`` : Separate terminal 1

* ``20-23_Publisher-backup.sh`` : Separate terminal 2, should fail to connect

* Ctrl-C on 20-31_Subscriber-listener+blacklist.sh

* ``reset.sh`` : Reset the test/ directory

* ``20-41_Subscriber-listener+whitelist.sh`` : Separate terminal 1

* ``20-23_Publisher-backup.sh`` : Separate terminal 2, should connect and run

* ``reset.sh`` : Reset the test/ directory

* ``20-51_Subscriber-listener-auth.sh`` : Separate terminal 1

* ``20-23_Publisher-backup.sh`` : Separate terminal 2

* ``reset.sh`` : Reset the test/ directory

* ``20-61_Subscriber-listener-keepgoing.sh`` : Separate terminal 1

* ``20-22_Publisher-dryrun.sh`` : Separate terminal 2, subscriber should keep going

* ``20-23_Publisher-backup.sh`` : Separate terminal 2

* ``20-99_Quit-Subscriber-listener.sh`` : Separate terminal 2

### 30-00 Interactive Terminals

Special command authorization use, with quotes:  auth "sharkbait"

* ``reset.sh`` : Reset the test/ directory

* ``30-21_Subscriber-listener.sh`` : Separate terminal 1

* ``30-29_Publisher-manual.sh`` : Separate terminal 2, bye leaves listener running, quit ends listener

* ``30-31_Publisher-listener.sh`` :  Separate terminal 1

* ``30-39_Subscriber-terminal.sh`` :  Separate terminal 2, bye leaves listener running, quit ends listener

### 40-00 Local Hints

Reminder: Hints must be Done on the publisher before publishing to a subscriber - so the
two collections match during the backup operation. If not an exception is thrown.

* ``reset.sh`` : Reset the test/ directory

* ``40-01_Hints-publisher.sh`` : Run Hints on publisher, once

* ``reset.sh`` : Reset the test/ directory

* ``40-22_Publisher-dryrun.sh`` : Run once, results & copies will be wrong because hints not processed

* ``40-23_Publisher-backup.sh`` : Run once

### 50-00 Remote Hints

* ``reset.sh`` : Reset the test/ directory

* ``50-01_Hints-publisher.sh`` : Run once

* ``50-21_Subscriber-One-listener.sh`` : Separate terminal 1

* ``50-22_Publisher-One-dryrun.sh`` : Separate terminal 2, results & copies will be wrong because hints not processed

* ``50-21_Subscriber-One-listener.sh`` : Separate terminal 1

* ``50-23_Publisher-One-backup.sh`` : Separate terminal 2

* ``50-31_Subscriber-Two-listener.sh`` : Separate terminal 1

* ``50-32_Publisher-Two-dryrun.sh`` : Separate terminal 2

* ``50-31_Subscriber-Two-listener.sh`` : Separate terminal 1, various "Does not exist" because of test setup

* ``50-33_Publisher-Two-backup.sh`` : Separate terminal 2

Note: This sequence results in orphaned .els files in Subscriber Two.

### 60-00 Local Hint Tracker

* ``reset.sh`` : Reset the test/ directory

* ``60-01_Hints-publisher.sh`` : Run once, hints are tracked locally

* ``60-22_Publisher-One-dryrun.sh`` : Run once

* ``60-23_Publisher-One-backup.sh`` : Run once

* ``60-32_Publisher-Two-dryrun.sh`` : Run once

* ``60-33_Publisher-Two-backup.sh`` : Run once

* ``60-23_Publisher-One-backup.sh`` : Run once

* ``60-33_Publisher-Two-backup.sh`` : Run once

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty.

### 70-00 Remote Hint Server

* ``reset.sh`` : Reset the test/ directory

* ``70-01_Hints-publisher.sh`` : Run once, hints are tracked locally

* ``70-10_Status-Server-listener.sh`` : Separate terminal 1.

* ``70-21_Subscriber-One-listener-quit.sh`` : Separate terminal 2

* ``70-22_Publisher-One-dryrun.sh`` ; Separate terminal 3, all processes should stop when done

* ``70-10_Status-Server-listener.sh`` : Separate terminal 1

* ``70-21_Subscriber-One-listener-quit.sh`` : Separate terminal 2

* ``70-23_Publisher-One-backup.sh`` : Separate terminal 3, all processes should stop when done

* ``70-10_Status-Server-listener.sh`` : Separate terminal 1

* ``70-31_Subscriber-Two-listener.sh`` : Separate terminal 2

* ``70-32_Publisher-Two-dryrun.sh`` : Separate terminal 3, status server continues to run

* ``70-31_Subscriber-Two-listener.sh`` : Separate terminal 2

* ``70-33_Publisher-Two-backup.sh`` : Separate terminal 3, status server continues to run

* ``70-21_Subscriber-One-listener-quit.sh`` : Separate terminal 2

* ``70-23_Publisher-One-backup.sh`` : Separate terminal 3, all processes should stop when done

* ``70-10_Status-Server-listener.sh`` : Separate terminal 1

* ``70-31_Subscriber-Two-listener.sh`` : Separate terminal 2

* ``70-33_Publisher-Two-backup.sh`` : Separate terminal 3, status server continues to run

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty

* ``70-90_Quit-Status-Server.sh`` : Separate terminal 2, stop the status server directly

