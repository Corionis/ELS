
# Project To Do

!!!!!!! Rework Invite ...
* Move editing templates to System, Email Templates.
  * Use Email Servers approach
* Change Invite to select one of the configured templates


LEFTOFF can there be a Library JSON stripped-down to "Connect Only" metadata?
* Use for Invite archive
* Stage archive contents in separate directory?
    * Paths are critical
* DEFAULT Auth keys and Whitelist selected

## Security Approach

### Login

There are two levels of "logging-in".
 * Repository-level. The original complex handshake.
 * User-level. If Users are enabled, optional:
   * Each user definition is contained in their respective Repository.
   * Individual read/write Grants may be assigned:
     * For each available Repository and,
     * For each Library within that Repository, including their own.
   * Grants are enforced by both the Publisher and Remote Subscriber.
     * Navigator desktop application
     * Command line process
     * Remote Subscriber only returns libraries and items the Publisher user has access to.
       * Local Subscriber is local and therefore does not.

User Invite Mechanism
    * Template(s) for invitation / update email 
      * Ask about Library basics
        * Type, Basic, Advanced, Administrator
        * Library name, check for conflicts
        * Host
        * Listen
        * Flavor
        * Email address
        * First invitation or update?
      * Where to download ELS
      * Links to Plex organizing Photos and Videos
      * Which Plex users to share with?
   * Invite knobs:
      * Add to Authentication keys if not there
      * Add to Whitelist if not there
      * Add to Hint keys, if Type is not Basic and if not there
    * Invite, archive: 
      * Their library
      * Our library
      * If not Basic AND doing automated backups
        * Hint Server
        * Hint keys
    * Discuss needing to add Location(s) and Bibliography libraries
    * What Is My IP? 
    * Discuss the use of the Custom IP/port option
    * Syncing "system" files by date
      * Do they need some data member to indicate "system" for non-Admin types

+ Version 5 users can "subscribe" to Mismatches and What's New
+ Add stty command to retrieve Subscriber users who have "subscribed" to Mismatches and/or What's New.

* Clean-up scripts and Intellij profiles. Subscriber listener with Auth does not need -t or -p and does not use the Hint Server - right?

* Go through all email providers and try OAuth2 again.

* Test from a scratch install for a Publisher and separate User Subscriber - go through the setup sequence !!!!!!!

* SCRUB internal docs. Cover dialog details but NOT how-to and concepts. Write that in te Wiki and provide links in internals.

* Update website:
  * Reword intro for SEO
  * Discuss the fact the Publisher and Subscriber can switch roles
      * And what parts of the Library file are used for each role
  * Drawings:
    * Remove Hint Server path to Subscriber
    * Add diagram explaining Host & Listen with NAT and firewall

* Update Wiki:
  * Add Whitelist/Blacklist discussion to Communications, especially for persistent listeners
  * Add Security page to cover the optional layers
  * Add User-level security page
  * Publishers have Grants to themselves and Subscribers
  * Subscribers only access themselves

* Add the other FlatLaf themes?

* Add a RHEL VM and ELS distro?

! Libraries, Bibliography, up/down characters do not dim like text

## Areas of Security

* Drag 'n Drop
* Copy, Cut, Paste
* New folder
* Rename
* Touch
* Visual indicator of read/write access
* Main menu options
* Operations choices

### Current Short-List

! Update ELS Plex Generator
! Test on Windows where software is in AppData
* Test scripts, and match run profiles to o/s scripts

#### Thoughts
 * ELS is going to need "reconnect" logic if a remote Subscriber or Hint Server connection fails, reboots, etc.

### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P libraryKey or (edited) collection options, -s|-S same problem
  * Add a check if edited Library is loaded. Prompt to reload, or just do it automatically.
* Possibly add "create directories" option to Libraries, Bibliography, Add Multiple

## Red Flags

 * Make a pass over copy, move and rename for "if exists" and the overwrite option 

## Ideas

 # Fragments

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
