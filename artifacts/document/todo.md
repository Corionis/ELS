
# Project To Do

## Current Short-List

 * ...

### Hints Reengineering

#### Short List
 * Error handling
 * Statistics
 * Test script, and match run profiles to o/s scripts

#### Thoughts
 * Opens possibility of the Hint Status Server optionally auto-pushing new Hints to systems in Hint Keys.

### Should Workstation/Collection be in the JSON file?
 * Controls whether a Hint is created
 * If Hints are enabled - and the Hint Server does not connect:
    * If publisher is Collection option to operate in read-only mode
    * Subscriber - by definition is a Collection - is read-only



## To Do
 1. Go through ClientSftp and ClientStty for handling subscriber disconnects in Navigator.
 2. Logic problem when both publisher and subscriber use -S where the subscriber
    does not scan it's collection so operations fail with no items. 

## Operations Tool

### Issues

* In-GUI execution of multiple (background) operations and managing those threads & tasks
* How to handle -p|-P library or (edited) collection options, -s|-S same problem

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

 * Edit the wiki in the project:
   * https://docs.github.com/en/communities/documenting-your-project-with-wikis/adding-or-editing-wiki-pages


## Ideas

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
