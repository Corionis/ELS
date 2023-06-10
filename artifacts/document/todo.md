
# Project To Do

## To Do
 1. Re-write embedded help pages in Markdown then convert to HTML so the same files can
    be used in the GitHub Wiki and new ELS web site.
 2. Go through ClientSftp and ClientStty for handling subscriber disconnects in Navigator.
 3. Logic problem when both publisher and subscriber use -s where the subscriber
    does not scan it's collection so operations fail with no items. 


## Hint Tracker / Hint Server in Navigator

!! keys, local tracker, remote status server:

If only keys mark as For; with simple single-backup rigs it works; make
sure it skips failures during a munge; with a later tracker or status server
update status accordingly.

If with a tracker mark as Done; test it a lot.

### Hint Use Issues

 1. Multiple operations on the same item :: A rename, then move may not be
    executed in the correct order.


## Operations Tool

### Rules

 * If Hints or Hint Server (hint tracking) is enabled Hint Keys are required.

### Issues

 * In-GUI execution of multiple (background) operations and managing those threads & tasks
 * How to handle -p|-P library or (edited) collection options, -s|-S same problem


## Red Flags

 * Make a pass over copy, move and rename for "if exists" and the overwrite option 


## Ideas

 * Knob for "Show examples" then:
   + Include, or not, files from examples directories

 * Add a statistics tracker to accumulate values:
   * Title counts for each Library
   * File counts
   * Additions, by date, month, quarter, year


## Catch-22

How to setup command-line options for a non-Operation tool, e.g. Renamer?
* Possible solution:
   * Currently a Renamer JOB uses the current configuration
   * Setup a Job Operation to setup a configuration then runs the Renamer job
   * Kinda weird ... but holds the command line paradigm


## Fragments

```
    Object[] opts = { context.cfg.gs("Z.ok") };
    JOptionPane.showOptionDialog(this, context.cfg.gs("EmptyDirectoryFinder.removal.of.empties.successful"),
        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
        null, opts, opts[0]);
```

#### _-end-_
