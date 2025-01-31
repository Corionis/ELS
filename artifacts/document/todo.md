
# Project To Do

## Current Short-List

 !! Different launch techniques open multiple Hints 
 ! Test with pub and sub collections NOT in configuration directory
 ! Analyze use of clientSftpMetadata & clientSftp
 ! Test with full paths in JSON
 ! Test with pub and sub collections in different places
 ! Test terminals

 * TEST with new blank configuration
 * TEST File, Open Subscriber failure and retry
 * TEST switching repos in Navigator during Job

   JOB RUN BUTTON
    Add validateRun() method to check task requirements; Jobs iterate their Tasks

 * Add Hint processing to Tools as needed
 * Add Preference to not allow standard sftp sessions; stty not connected
 * Optimize remote Hint processing by sending all pending Hints at once

#### Short List
 * Regression Test from a scratch installation.
   * No pre-defined JSON files.
 * Error handling
 * Statistics
 * Test scripts, and match run profiles to o/s scripts

#### Thoughts
 * Opens possibility of the Hint Status Server optionally auto-pushing new Hints to systems in Hint Keys.
 * ELS is going to need "reconnect" logic if a remote Subscriber or Hint Server connection fails.

### Should Workstation/Collection be in the JSON file?
 * Controls whether a Hint is created
 * If Hints are enabled - and the Hint Server does not connect:
    * If publisher is Collection option to operate in read-only mode
    * Subscriber - by definition is a Collection - is read-only


## Operations Tool

### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P library or (edited) collection options, -s|-S same problem
  * Add a check if edited Library is loaded. Prompt to reload, or just do it automatically. 


## Red Flags

 * Make a pass over copy, move and rename for "if exists" and the overwrite option 


## New Web Site

 * Edit the wiki in the project:
   * https://docs.github.com/en/communities/documenting-your-project-with-wikis/adding-or-editing-wiki-pages


## Ideas

 * Add a statistics tracker to accumulate values:
   * Title counts for each Library
   * File counts
   * Additions, by date, month, quarter, year

 * Invite mechanism
   * Special code/key + command to "register" a new Navigator user
   * A manual, one-time, process
   * Use a complex stty + sftp protocol; unhackable

## Fragments

```
    Object[] opts = { localContext.cfg.gs("Z.ok") };
    JOptionPane.showOptionDialog(this, localContext.cfg.gs("EmptyDirectoryFinder.removal.of.empties.successful"),
        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
        null, opts, opts[0]);
```

Close a JComboBox during debugging to avoid desktop lock-up:
```
   combo.getUI().setPopupVisible(combo, false);
   ```

#### _-end-_
