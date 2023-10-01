
# Project To Do

## To Do
 1. Re-write embedded help pages in Markdown then convert to HTML so the same files can
    be used in the GitHub Wiki and new ELS web site.
 2. Go through ClientSftp and ClientStty for handling subscriber disconnects in Navigator.
 3. Logic problem when both publisher and subscriber use -s where the subscriber
    does not scan it's collection so operations fail with no items. 

## Scenarios

Create a series of scenarios describing how ELS can be used for various situations.

 * Basic multi-volume back-up.
 * ...
 * With ELS 5 and user-level authentication and authorization a family rotation
   back-up sequence:
     * Each member can manage their libraries but can only display others, depending
       on library share restrictions.
     * Changes by each are propagated via Hints.
     * Everyone backs-up everyone else.

## System Editors for Keys & IP Lists

For Auth Keys test whether that key is all that is needed to make a connection, then 
request subscriber collection.


## Operations Tool

### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P library or (edited) collection options, -s|-S same problem


## Hint Tracker / Hint Server in Navigator

### Keys, local tracker, remote status server:

If only keys mark as For; with simple single-backup rigs it works; make
sure it skips failures during a munge; with a later tracker or status server
update status accordingly.

If with a tracker mark as Done; test it a lot.

### Hint Processing Sequence

The problem: If manual changes are made on a subscriber, such as a rename, the
publisher will think the original file is missing and send it. Should processing
be changed so publisher receives Hints from Subscriber before the munge operation?

### Hint Use Issues

 1. Multiple operations on the same item :: A rename, then move may not be
    executed in the correct order.


## Red Flags

 * Make a pass over copy, move and rename for "if exists" and the overwrite option 


## New Web Site

 * Point Documentation links from a new GitHub Pages site to the wiki
 * Edit the wiki in the project:
   * https://docs.github.com/en/communities/documenting-your-project-with-wikis/adding-or-editing-wiki-pages


## Ideas

 * Knob for "Show examples" then:
   + Include, or not, files from examples directories

 * Add a statistics tracker to accumulate values:
   * Title counts for each Library
   * File counts
   * Additions, by date, month, quarter, year


## Fragments

```
    Object[] opts = { context.cfg.gs("Z.ok") };
    JOptionPane.showOptionDialog(this, context.cfg.gs("EmptyDirectoryFinder.removal.of.empties.successful"),
        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
        null, opts, opts[0]);
```

#### _-end-_
