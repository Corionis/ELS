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

 00-    Basic Functionality

 10-    Local Backup

 20-    Remote Backup

 30-    Interactive terminals

 40-    Local Hints

 50-    Remote Hints

 60-    Local Hint Tracker - Local Backup

 62-    Local Hint Tracker - Remote Backup

 70-    Remote Hint Server - Local Backup

 72-    Remote Hint Server - Remote Backup

80-    Navigator


## Test Utility Scripts

``clear-output`` : Removes all files from the mock/output/ directory

``reset`` : Resets the mock/test/ directory by deleting it and copying the 
mock/media-base_copy-only directory to a new mock/test/ directory


## Tests & Sequence

Some of the following scripts are run once. Others are run multiple times to complete a particular test
sequence.

Examine the screen and/or log output for warnings, errors and exceptions. Examine the test/ directory
for the appropriate changes at each step of the test sequences.

Automation and result-checking have not been, and might not be, implemented. It's a lot of work.
At this point it's a manual and visual process.

### 00-00  Basic Functionality

 * ``00-01_Version`` : Run once, does not create a log file
 * ``00-02_Bad-arguments`` : Run once, should see an exception
 * ``00-03_Validate`` : Run once
 * ``00-04_Export`` : Run once
 * ``00-05_Duplicates`` : Show dupes
 * ``00-06_Duplicates-crosscheck`` : Duplicate shown due to cross-check
 * ``00-07_Empty-directories`` : Empty directory check 


### 10-00  Local Backup

 * ``reset`` : Reset the test/ directory
 * ``10-22_Backup-dryrun`` : Run once
 * ``10-23_Backup`` : Run once
 * ``reset`` : Reset the test/ directory
 * ``10-24_Backup-exclude-lib`` : Run once
 * ``reset`` : Reset the test/ directory
 * ``10-25_Backup-include-lib`` : Run once


### 20-00 Remote Backup

 * ``reset`` : Reset the test/ directory
 * ``20-21_Subscriber-listener`` ; Separate terminal 1, stops when done
 * ``20-22_Publisher-dryrun`` : Separate terminal 2
 * ``20-21_Subscriber-listener`` : Separate terminal 1, stops when done
 * ``20-23_Publisher-backup`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``20-21_Subscriber-listener`` : Separate terminal 2
 * ``20-24_Publisher-backup-keepgoing`` : Separate terminal 1
 * ``20-99_Quit-Subscriber-listener`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``20-31_Subscriber-listener+blacklist`` : Separate terminal 1
 * ``20-23_Publisher-backup`` : Separate terminal 2, should fail to connect
 <p></p>

 * Ctrl-C on 20-31_Subscriber-listener+blacklist
 <p></p>

 * ``reset`` : Reset the test/ directory
 * ``20-41_Subscriber-listener+whitelist`` : Separate terminal 1
 * ``20-23_Publisher-backup`` : Separate terminal 2, should connect and run
 * ``reset`` : Reset the test/ directory
 * ``20-51_Subscriber-listener-auth`` : Separate terminal 1
 * ``20-23_Publisher-backup`` : Separate terminal 2
 * ``reset`` : Reset the test/ directory
 * ``20-61_Subscriber-listener-keepgoing`` : Separate terminal 1
 * ``20-22_Publisher-dryrun`` : Separate terminal 2, subscriber should keep going
 * ``20-23_Publisher-backup`` : Separate terminal 2
 * ``20-99_Quit-Subscriber-listener`` : Separate terminal 2


### 30-00 Interactive Terminals

Special command authorization use, with quotes:  auth "sharkbait"

 * ``reset`` : Reset the test/ directory
 * ``30-21_Subscriber-listener`` : Separate terminal 1
 * ``30-29_Publisher-manual`` : Separate terminal 2, bye leaves listener running, quit ends listener
 * ``30-31_Publisher-listener`` :  Separate terminal 1
 * ``30-39_Subscriber-terminal`` :  Separate terminal 2, bye leaves listener running, quit ends listener


### 40-00 Local Hints

Local Hints are used when both the publisher and subscriber (back-up) drives are connected
to the same system.

Reminder: Hints must be Done on the publisher before publishing to a subscriber - so the
two collections match during the backup operation. If not an exception is thrown. Changes
made with Hints, and optionally Hint Tracking, enabled are automatically marked as Done.

 * ``reset`` : Reset the test/ directory
 * ``40-01_Hints-publisher`` : Run Hints on publisher, once, if not Done by Navigator
 * ``reset`` : Reset the test/ directory
 * ``40-22_Publisher-dryrun`` : Run once, results & copies _will be wrong_ because hints not processed
 * ``40-23_Publisher-backup`` : Run once


### 50-00 Remote Hints

Remote Hints for networked back-ups are sent from publisher to a remote subscriber
then executed on the subscriber.

 * ``reset`` : Reset the test/ directory
 * ``50-01_Hints-publisher`` : Run once
 * ``50-21_Subscriber-One-listener`` : Separate terminal 1
 * ``50-22_Publisher-One-dryrun`` : Separate terminal 2, results & copies will be wrong because hints not processed
 * ``50-21_Subscriber-One-listener`` : Separate terminal 1
 * ``50-23_Publisher-One-backup`` : Separate terminal 2
 * ``50-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``50-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``50-31_Subscriber-Two-listener`` : Separate terminal 1, various "Does not exist" because of test setup
 * ``50-33_Publisher-Two-backup`` : Separate terminal 2

Note: This sequence results in orphaned .els files on Subscriber Two. The "odd man out" problem.


### 60-00 Local Hint Tracker - Local Backup

The Local Hint Tracker solves the "odd man out" problem for local back-ups by tracking the
processing status of each Hint on each back-up locally.

 * ``reset`` : Reset the test/ directory
 * ``60-01_Hints-publisher`` : Run once, if not Done in Navigator
 * ``60-22_Publisher-One-dryrun`` : Run once
 * ``60-23_Publisher-One-backup`` : Run once
 * ``60-32_Publisher-Two-dryrun`` : Run once
 * ``60-33_Publisher-Two-backup`` : Run once
 <p></p>

 * One more time:
 <p></p>

 * ``60-23_Publisher-One-backup`` : Run once
 * ``60-33_Publisher-Two-backup`` : Run once

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty.


### 62-00 Local Hint Tracker - Remote Backup

This permutation tests with a remote backup.

 * ``reset`` : Reset the test/ directory
 * ``62-01_Hints-publisher`` : Run once, if not done in Navigator
 * ``62-21_Subscriber-One-listener`` : Run Once
 * ``62-22_Publisher-One-dryrun`` : Run once
 * ``62-21_Subscriber-One-listener`` : Run Once
 * ``62-23_Publisher-One-backup`` : Run once
 * ``62-31_Subscriber-Two-listener`` : Run Once
 * ``62-32_Publisher-Two-dryrun`` : Run once
 * ``62-31_Subscriber-Two-listener`` : Run Once
 * ``62-33_Publisher-Two-backup`` : Run once
 <p></p>

 * One more time:
 <p></p>

 * ``62-21_Subscriber-One-listener`` : Run Once
 * ``62-23_Publisher-One-backup`` : Run once
 * ``62-31_Subscriber-Two-listener`` : Run Once
 * ``62-33_Publisher-Two-backup`` : Run once

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty.


### 70-00 Remote Hint Server - Local Backup

The Remote Hint Server solves the "odd man out" problem for networked back-ups by tracking the
processing status of each Hint on each back-up using a separate ELS process the publisher and
subscriber communicate with.

 * ``reset`` : Reset the test/ directory
 * ``70-01_Hints-publisher`` : Run once, hints are tracked locally
 * ``70-10_Status-Server-listener`` : Separate terminal 1.
 * ``70-22_Publisher-One-dryrun`` ; Separate terminal 2
 * ``70-23_Publisher-One-backup`` : Separate terminal 2
 * ``70-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``70-33_Publisher-Two-backup`` : Separate terminal 2
 <p></p>

 * One more time:
 <p></p>

 * ``70-23_Publisher-One-backup`` : Separate terminal 2
 * ``70-33_Publisher-Two-backup`` : Separate terminal 2
 * ``70-99_Quit-Status-Server`` : Separate terminal 2

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty


### 72-00 Remote Hint Server - Remote Backup

This permutation tests with both a remote Hint Server and remote backup.

 * ``reset`` : Reset the test/ directory
 * ``72-01_Hints-publisher`` : Run once, hints are tracked locally
 * ``72-10_Status-Server-listener`` : Separate terminal 1.
 * ``72-21_Subscriber-One-listener`` : Separate terminal 2
 * ``72-22_Publisher-One-dryrun`` ; Separate terminal 3
 * ``72-21_Subscriber-One-listener`` : Separate terminal 2
 * ``72-23_Publisher-One-backup`` : Separate terminal 3
 * ``72-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``72-32_Publisher-Two-dryrun`` : Separate terminal 3
 * ``72-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``72-33_Publisher-Two-backup`` : Separate terminal 3
 <p></p>

 * One more time:
 <p></p>

 * ``72-21_Subscriber-One-listener`` : Separate terminal 2
 * ``72-23_Publisher-One-backup`` : Separate terminal 3
 * ``72-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``72-33_Publisher-Two-backup`` : Separate terminal 3
 * ``72-99_Quit-Status-Server`` : Separate terminal 2 or 3

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty


### 80-00 Navigator - Local Backup

Run one at a time for basic local Navigator funtionality.

 * ``80-01_Navigator-no-args`` : ELS with no arguments
 * ``80-02_Navigator-navigator`` : With --navigator option only
 * ``80-03_Navigator-publisher-only`` :: With a publisher
 * ``80-23_Navigator-local`` : With publisher as a Collection and local subscriber
 * ``80-33_Navigator-workstation`` : With publisher as a Workstation and local subscriber


### 82-00 Navigator - Remote Backup

 * ``82-21_Subscriber-One-listener`` : Start a subscriber listener
 * ``82-23_Navigator-remote`` : Navigator with publisher as Collection and remove subscriber


### 90-00 Navigator - Local Hint Tracker - Local Backup

 * ``90-23_Navigator-local`` : Navigator
 * Run test series "_Testing Navigator with Hints & Hint Tracking_" below.
 * Run series ``60-00 Local Hint Tracker - Local Backup`` to process Hints.


### 92-00 Navigator - Local Hint Tracker - Remote Backup

 * ``92-21_Subscriber-One-listener`` : Subscriber One listener
 * ``92-23_Navigator-remote`` : Navigator with remote Subscriber One
 * Run tests from one of the series above depending on what is done.


### 100-00 Navigator - Remote Hint Server - Local Backup

 * ``100-10_Status-Server-listener`` : Status Server listener
 * ``100-23_Navigator-remote-hints`` : Navigator with remote Hints
 * Run tests from one of the series above depending on what is done.


### 102-00 Navigator - Remote Hint Server - Remote Backup

 * ``102-10_Status-Server-listener`` : Status Server listener
 * ``102-21_Subscriber-One-listener-remote-hints`` : Subscriber One listener
 * ``102-23_Navigator-remote-hints`` : Navigator with remote Hints & Subscriber One
 * Run tests from one of the series above depending on what is done.


### 110-00 Jobs

 * tbd


## Testing Navigator with Hints & Hint Tracking

---

### Manual Test Checks

 * Check the [hint].els file:
   * The local system is marked as Done
   * The command is correct
 * Check the Hint Tracker datastore hint tracking file:
   * The local system is marked as Done

### Manual tests

 * M1 : Move root-level file
 * M2 : Move root-level directory & subdirectories
 * M3 : Move file in a subdirectory
 * M4 : Move subdirectory in a subdirectory !
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

#### Test Hint processing on publisher

 1. Perform above tests on publisher
 2. Optional: Copy the mock/test directory for later comparison 
 3. Reset the test directory and Hint files
    1. Run ``capture -d -r`` that:
        1. Captures the .els files in the output directory
        2. Changes Done to For
        3. Resets the test directory with reset -f
        4. Restores the .els files
 4. Run the Hint and back-up series for that test set to process hints
 5. Check hints executed correctly at each step
 6. Check status updated correctly in [hint].els to Done
 7. Check hint tracking files in datastore updated correct
 8. Perform any desired comparison 
 
#### Test Hint processing on subscriber

 1. Perform above tests on subscriber
 2. Reset the test directory and Hint files
    1.Run ``capture -d -r`` that:
      1. Captures the .els files in the output directory
      2. Changes Done to For
      3. Resets the test directory with reset -f
      4. Restores the .els files
 3. Run the Hint and back-up series for that test set to process hints
 4. Check hints executed correctly on publisher
 5. Check local system status updated in [hint].els to Done
 6. Check hint tracking files correct



-end-
