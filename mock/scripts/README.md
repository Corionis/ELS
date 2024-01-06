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
 [040-00  Local Hints](#040-00--local-hints)<br/>
 [050-00  Remote Hints](#050-00--remote-hints)<br/>
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

``reset`` : Resets the mock/test/ directory by deleting it and copying the 
mock/media-base_copy-only directory to a new mock/test/ directory


## Tests & Sequence

Some of the following scripts are run once. Others are run multiple times to complete a particular test
sequence.

Examine the screen and/or log output for warnings, errors and exceptions. Examine the test/ directory
for the appropriate changes at each step of the test sequences.

Automation and result-checking have not been, and might not be, implemented. It's a lot of work.
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


### 040-00  Local Hints

Local Hints are used when both the publisher and subscriber (back-up) drives are connected
to the same system, i.e. appear in a file browser.

Reminder: Hints must be Done on the publisher before publishing to a subscriber - so the
two collections match during the backup operation. If not an exception is thrown. Changes
made with Hints, and optionally Hint Tracking, enabled are automatically marked as Done.

 * ``reset`` : Reset the test/ directory
 * ``080-26_Navigator-local`` : Navigator to create Hints
 * ``040-01_Hints-publisher`` : Run Hints on publisher, once, if not Done by Navigator
 * ``reset`` : Reset the test/ directory
 * ``040-22_Publisher-dryrun`` : Run once, results & copies _will be wrong_ because hints not processed
 * ``040-23_Publisher-backup`` : Run once


### 050-00  Remote Hints

Remote Hints for networked back-ups are sent from publisher to a remote subscriber
then executed on the subscriber.

 * ``reset`` : Reset the test/ directory
 * Edit test/system/hint.keys and remove all name/key pairs except MediaPublisher and SubscribeOne
 * BE SURE to keep the deleted lines to put back after this test
 * ``082-23_Navigator-local`` : Perform the Manual Tests below to create some Hints with no tracking
 * ``050-21_Subscriber-One-listener`` : Separate terminal 1
 * ``050-23_Publisher-One-backup`` : Separate terminal 2

When done MediaPublisher and SubscriberOne should be matched based on MediaPublisher content and
all .els Hint files should be removed (gone).

 * Replace name/key pairs in /test/system/hint.keys

 * ``reset`` : Reset the test/ directory
 * ``082-23_Navigator-local`` : Perform the Manual Tests below to create some Hints with no tracking
 * ``050-01_Hints-publisher`` : Run once
 * ``050-21_Subscriber-One-listener`` : Separate terminal 1
 * ``050-22_Publisher-One-dryrun`` : Separate terminal 2, results & copies will be wrong because hints not processed
 * ``050-21_Subscriber-One-listener`` : Separate terminal 1
 * ``050-23_Publisher-One-backup`` : Separate terminal 2
 * ``050-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``050-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``050-31_Subscriber-Two-listener`` : Separate terminal 1, various "Does not exist" because of test setup
 * ``050-33_Publisher-Two-backup`` : Separate terminal 2

Note: This sequence results in orphaned .els files on Subscriber Two. The "odd man out" problem.


### 060-00  Local Hint Tracker - Local Backup

The Local Hint Tracker solves the "odd man out" problem for local back-ups by tracking the
processing status of each Hint on each back-up locally.

 * ``reset`` : Reset the test/ directory
 * ``090-23_Navigator-local`` : Perform the Manual Tests below to create some Hints
 * ``060-01_Hints-publisher`` : Run once, if not Done in Navigator
 * ``060-22_Publisher-One-dryrun`` : Run once
 * ``060-23_Publisher-One-backup`` : Run once
 * ``060-32_Publisher-Two-dryrun`` : Run once
 * ``060-33_Publisher-Two-backup`` : Run once
 <p></p>

 * One more time:
 <p></p>

 * ``060-23_Publisher-One-backup`` : Run once
 * ``060-33_Publisher-Two-backup`` : Run once

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty.


### 062-00  Local Hint Tracker - Remote Backup

This permutation tests with a remote backup, and has the "odd man out" issue, because
the back-ups are remote but the Hint Tracker is local.

 * ``reset`` : Reset the test/ directory
 * ``090-23_Navigator-local`` : Perform the Manual Tests below to create some Hints
 * ``062-01_Hints-publisher`` : Run once, if not done in Navigator
 * ``062-21_Subscriber-One-listener`` : Separate terminal 1
 * ``062-22_Publisher-One-dryrun`` : Separate terminal 2
 * ``062-21_Subscriber-One-listener`` : Separate terminal 1
 * ``062-23_Publisher-One-backup`` : Separate terminal 2
 * ``062-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``062-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``062-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``062-33_Publisher-Two-backup`` : Separate terminal 2
 <p></p>

 * One more time:
 <p></p>

 * ``062-21_Subscriber-One-listener`` : Separate terminal 1
 * ``062-23_Publisher-One-backup`` : Separate terminal 2
 * ``062-31_Subscriber-Two-listener`` : Separate terminal 1
 * ``062-33_Publisher-Two-backup`` : Separate terminal 2

Note: This sequence results in orphaned .els files on Subscriber Two. The "odd man out" problem.


### 070-00  Remote Hint Server - Local Backup

The Remote Hint Server solves the "odd man out" problem for networked back-ups by tracking the
processing status of each Hint on each back-up using a separate ELS process the publisher and
subscriber communicate with.

 * ``reset`` : Reset the test/ directory
 * ``090-23_Navigator-local`` : Perform the Manual Tests below to create some Hints, OR
 * ``100-26_Navigator-remote-hints`` : and skip 070-01_Hints-publisher next
 * ``070-01_Hints-publisher`` : Run once, if not done in Navigator
 * ``070-10_Status-Server-listener`` : Separate terminal 1
 * ``070-22_Publisher-One-dryrun`` ; Separate terminal 2
 * ``070-23_Publisher-One-backup`` : Separate terminal 2
 * ``070-32_Publisher-Two-dryrun`` : Separate terminal 2
 * ``070-33_Publisher-Two-backup`` : Separate terminal 2
 <p></p>

 * One more time:
 <p></p>

 * ``070-23_Publisher-One-backup`` : Separate terminal 2
 * ``070-33_Publisher-Two-backup`` : Separate terminal 2
 * ``070-99_Quit-Status-Server`` : Separate terminal 2

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty


### 072-00  Remote Hint Server - Remote Backup

This permutation tests with both a remote Hint Server and remote backup.

 * ``reset`` : Reset the test/ directory
 * ``072-10_Status-Server-listener`` : Separate terminal 1.
 * ``100-23_Navigator-remote-hints`` : Perform the Manual Tests below to create some Hints
 * ``072-01_Hints-publisher`` : Run once, hints are tracked locally
 * ``072-21_Subscriber-One-listener`` : Separate terminal 2
 * ``072-22_Publisher-One-dryrun`` ; Separate terminal 3
 * ``072-21_Subscriber-One-listener`` : Separate terminal 2
 * ``072-23_Publisher-One-backup`` : Separate terminal 3
 * ``072-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``072-32_Publisher-Two-dryrun`` : Separate terminal 3
 * ``072-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``072-33_Publisher-Two-backup`` : Separate terminal 3
 <p></p>

 * One more time:
 <p></p>

 * ``072-21_Subscriber-One-listener`` : Separate terminal 2
 * ``072-23_Publisher-One-backup`` : Separate terminal 3
 * ``072-31_Subscriber-Two-listener`` : Separate terminal 2
 * ``072-33_Publisher-Two-backup`` : Separate terminal 3
 * ``072-99_Quit-Status-Server`` : Separate terminal 2 or 3

Note: All test/ directory .els files should be gone and the test/hints/datastore/ directory should be empty
 

### 080-00  Navigator - Local Backup

Run one at a time for basic local Navigator functionality.

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

 * Check the [hint].els file:
   * The local system is marked as Done
   * The command is correct
 * Check the Hint Tracker datastore hint tracking file:
   * The local system is marked as Done

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
