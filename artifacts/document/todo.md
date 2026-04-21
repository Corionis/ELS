
# Project To Do

* Clean-up scripts and Intellij profiles. Subscriber listener with Auth does not need -t or -p and does not use the Hint Server - right?

* Go through all email providers and try OAuth2 again.

* Test from a scratch install for a Publisher and separate User Subscriber - go through the setup sequence

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


## Meeting Points

!!! Setup custom Libraries with real names for meeting
!!! Generate Mismatches and What's New

* Start ELS with Hints and some pending

* General :: Display screen, no cursor
    * Visual changes, User:Connection, rounded edges, etc.
    * Check for Updates
    * New change indicator

* Preferences, Default Email Server, User-level security :: Display Screen, no cursor
* Browser security demo :: Display App w/ cursor
* No Subscriber System tab if Basic User

* Email Servers & OAuth2 :: Display Screen, no cursor
* New Mismatches and What's New

* User tab, Type, Resources, Grants
* Show a Library JSON file
* Login sequence and retrieving User from Subscriber

* Invite dialog
* Templates, built-ins, saved, custom & variable substitutions
* Making "us" the Subscriber in the Archive Contents dialog
* Using reduced "connection" Subscriber "us" Library file

* Libraries, New dialog and creating defaults

* DISCUSS
    * Hints and Basic (and Power?) Users
    * How to promote?


#### Thoughts
* ELS is going to need "reconnect" logic if a remote Subscriber or Hint Server connection fails, reboots, etc.


### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P libraryKey or (edited) collection options, -s|-S same problem
    * Add a check if edited Library is loaded. Prompt to reload, or just do it automatically.
* Possibly add "create directories" option to Libraries, Bibliography, Add Multiple


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
