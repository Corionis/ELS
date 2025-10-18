
# Project To Do

! Standardize on "Toggle All" instead of All / None

## Current Short-List

! Update ELS Plex Generator
! Test on Windows where software is in AppData
* Test scripts, and match run profiles to o/s scripts


### Ideas
 + Version 5 users can "subscribe" to Mismatches and What's New
 + Add stty command to retrieve Subscriber users who have "subscribed" to Mismatches and/or What's New.

#### Thoughts
 * ELS is going to need "reconnect" logic if a remote Subscriber or Hint Server connection fails, reboots, etc.

### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P library or (edited) collection options, -s|-S same problem
  * Add a check if edited Library is loaded. Prompt to reload, or just do it automatically.
* Possibly add "create directories" option to Libraries, Bibliography, Add Multiple

## Red Flags

 * Make a pass over copy, move and rename for "if exists" and the overwrite option 

## Ideas

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
