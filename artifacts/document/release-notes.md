## Version 4.0.0

This major release 4.0.0 of ELS adds the ELS Navigator desktop application and a
variety of related enhancements and changes. 

The Navigator is purpose-built to make building and on-going maintenance of collections
of all types of media across multiple storage devices easier. Originally built for home
media collections ELS 4.0 adds a new dimension to library-based, multi-device, cross-platform
file management. Using the built-in capabilities of ELS the Navigator provides a
visual tool for curating a collection either locally or remotely.
Tools and jobs are provided to make performing repetitive tasks easier. And the original
back-up tool is built-in of course.

_A work in progress._

__Features:__

 * Modes (where the Navigator is running)
     * On a media collection
     * On a separate workstation
 * Browser
     * Split-pane Publisher/Subscriber view     
     * Collection and System tabs for each
     * Local or remote subscriber
     * Drag 'n Drop and Copy, Cut, Paste
     * Automatic ELS Hint Tracking
     * Multiple named tool configurations
        * Duplicate Finder
        * Empty Directory Finder
        * Junk Remover
        * Renamer
        * External tools supported
     * Named jobs of sequenced named tools to automate repetitive tasks
 * BackUp
     * Configure named ELS back-ups with different configurations
     * Execute and monitor back-up runs
     * Generate scripts for command line and/or background scheduled execution
 * Libraries
     * Create and edit ELS Publisher, Subscriber, and Hint Server JSON files
     * Create and edit ELS Hint keys file

Like the rest of ELS the new Navigator is a general tool for anyone manipulating
media across multiple storage devices and is also compatible with modern media
systems such a Plex Media Server. Works on Windows, Linux and Mac.

_It's all built-in_ with the -n | --navigator option.

### Upgrade Notes

ELS 4.0.0 is significantly different than previous versions. Changes and bug fixes have been
made throughout the code, too numerous to list every change. Read these notes thoroughly when
upgrading for changes, additions and enhancements.

 1. When upgrading to ELS 4.0.0 from previous versions be sure to complete all Hint
    processing ***prior to the upgrade***. Ensure the Hint Tracking datastore is empty. The 
    syntax used in the .els Hint files has been updated.
 2. FILE LOCATIONS & ~/.els directory !!!!!!!!!!!!

### Enhancements

 1. ELS Navigator.
    1. Navigator is a publisher in ELS terms, by design.
    2. Navigator supports two scenarios:
       1. On a media collection such as a media server system, and optionally
          connected to a back-up running an ELS subscriber listener.
       2. On a separate workstation, and optionally connected to a media
          collection system or back-up running an ELS subscriber listener.
       3. The File, Open Publisher dialog shows radio buttons for Collection or Workstation.
          1. This setting determines whether Hints are stored when Hint Tracking is enabled.
             1. If Collection then the Hints are added, and tracked if enabled.
             2. If Workstation then Hints are not used during operations.
       4. Command-line behavior changes when using -n | --navigator:
          1. -P sets the Navigator for running on a Collection
          2. -p sets the Navigator as a Workstation
          3. -S sets the Navigator for a remote Subscriber
          4. -s sets the Navigator as a local Subscriber
          5. Note: The distinction of collection or library is not used with
             the Navigator. All data displayed are from active storage scans.

 2. New ELS project download options including an all-in-one with an embedded Java JRE.

 3. The remote communication paradigm has been changed to provide more reliability. 
 

### Command Line Changes

 1. The -n | --rename option has been removed in favor of the rename tool in the
    new ELS Navigator.

 2. The -n option has been repurposed for the Navigator and the --navigator option has been added.

 3. Added option -y | --preserve-dates to retain original file dates.

 4. Added option -z | --decimal-scale to format numeric (B, KB, MB, GB, TB) values with
    a 1000 decimal scale instead of a 1024 binary scale.
 
 5. Added option -j | --job to execute a previously-defined ELS Job. If the name contains
    whitespace enclose it in quotations. In this mode the job controls ELS actions.

 6. Added option -A | --auth-keys for subscriber and publisher listeners. This is the same
    format as Hint Keys. Authentication keys are used to authenticate both workstations and
    publishers instead of requiring a specific system defined by -s|S.

 7. Added option -g | --listener-keep-going. For a Publisher the "keep going" option skips
    sending the quit command to the subscriber when the backup operation is complete. For a
    subscriber it skips ending with a fault on an unexpected disconnect (EOL) and ignores
    quit commands. To stop a subscriber in this mode use the --listener-quit command.

 8. Added option -G | --listener-quit that only sends the quit command to a remote
    subscriber, then exits. Similar to the -Q | --force-quit option.

 9. Added option -B | --blacklist that uses a text file of one-line IP addresses to filter
    and block incoming connections. The blacklist supports # style comments and blank lines.
    Each IP address is an IPv4 dotted address, e.g. 127.0.0.1, on separate lines.

10. Added option -I | --ip-whitelist to filter and allow incoming connections. Similar to
    the -B | --blacklist file.

11. Changed the behavior of -u | --duplicates where duplicates are now only logged when
    this option is enabled.  Otherwise only a total number is reported in the statistics.
    Previously duplicates were always reported in a back-up or dry-run. 

12. Added option -E | --empty-directories where empty directories are logged when this
    option is enabled. Otherwise only a total number is reported in the statistics.

13. Added option -N | --ignored to log ignored files. For backup runs and the --duplicates option.

14. Implemented detailed logging of communications-related steps using the "trace" log level
    for the --console-level and --debug-level options.

15. Added remote mode J to the -r|--remote option. This is to support command-line use of
    the "Any Subscriber" origin option of Job tasks. Combined with -j|--job the remote
    subscriber defined with -s|-S is used. This is in contrast to the "Specific Subscriber"
    origin option where the subscriber defined for the task overrides the -s|-S option.

16. Added --dump-system that prints all JVM System.getProperties() values then exits.

17. Added -C | --config to set the location of the ELS configuration directory. Use
    "-C ." for the current directory.


### Other Changes

 1. The ELS Navigator has necessitated the introduction of a formal user-based directory
    structure to hold the various preference, bookmark, library, tool, job, etc. files. 
    All these items are kept in each user's HOME/.els/ directory.
 
    IMPORTANT: When upgrading from ELS versions earlier than 4.0.0 copy your existing library JSON
    files to your HOME/.els/libraries/ directory. If that directory does not exist create it.

 2. The listener daemons "bye" command behavior has changed. Now bye will leave the daemon
    running instead of shutting down. Quit, exit and logout still perform a shutdown.

 3. When using the ELS interactive terminal (not to be confused with ELS Navigator) the
    "bye" command has been changed to end the terminal session but leave the remote listener
    running. Commands quit, exit and logout will shutdown the remote listener.
 
 4. Added JSON library elements for temporary files:
    1. temp_dated "true/false" : If temporary files such as received collection files have
       date and time embedded in the filename. If false the same file is overwritten.
    2. temp_location "path" : Where to place temporary files. An empty string "" is the
       location of the ELS Jar file. If the path begins with "~/" the user's home directory
       is substituted for the "~".

 5. Removed JSON library element "renaming" and the related Java code.

 6. Changed the JSON library "ignore_patterns" behavior:
    1. If the pattern contains the path separator literal for that repository the full path is matched.
       1. For example pattern: ".*\\/Plex Versions.*" will exclude the directory "/Plex Versions" and any
          subdirectories and files.
    2. If the pattern does not contain the path separator literal only the right-end directory or file name is matched.

 7. Added a new authentication technique for subscriber and publisher listeners.
    1. Normally the publisher and subscriber are specific.
       1. If a connection is made to a listener and it is not the specific system expected
          the listener will fail and exit.
    2. However if the listener is running with the -A|--auth-keys Authentication Keys file option
       authentication matches against all the keys. So a single subscriber listener can connect to 
       one or more remote ELS publishers or ELS Navigators concurrently.
       1. The current limit is 10 concurrent connections. 
       2. This is the same technique used by the Hint Status Server using a Hint keys file.
    3. In either case if a connection is made and authentication fails the listener will fail and exit.
       1. This is to prevent hack attempts on listeners.
       2. May change in the future.
    4. The reasons for separate listener authentication keys and hint keys:
       1. A listener may want to allow Navigator sessions in addition to back-up systems.
       2. Hint keys control:
          1. Which back-up systems are setup to process hints.
          2. Which systems are tracked and status maintained in the Hint datastore.

 8. Modified the code for methodical exit code status values. Exit code 0 is normal, 1 indicates a
    fault occurred. Exit code 130 is returned if Ctrl-C is hit on the command line. Useful for error
    handling in multi-step automation batch files or scripts.

 9. Changed free space checking when backing-up a group of files so the value checked is reduced as
    each item in the group is copied. GitHub Issue #55.

10. Added JSON "timeout" element for the stty protocol in minutes. This provides a mechanism to avoid
    process hangs and the implementation uses an internal heartbeat to keep the connection alive during
    long-running operations. The heartbeat is _not_ an actual ping.

11. Changed Hint syntax handling to use the more formal syntax generated by the Navigator.
    **Important:** When upgrading to ELS 4.0.0 from previous versions be sure to complete all Hint
    processing and make sure for any Hint Tracking being used, local or remote, the datastore is
    empty.

### Operational Notes

 1. When running Navigator with a remote Subscriber and executing a backup Job that would normally stop
    the listener when done be sure to start the remote subscriber listener with the
    -g | --listener-keep-going option to avoid a connection fault in Navigator when the backup is complete.

 2. When performing long copy/move operations multiple copy/paste and drag 'n drop operations
    may be batched. Each operation is added to the existing batch(es) of running operations and
    are processed in order.

 3. When running a backup operation or copying/moving content in Navigator the target path 
    is determined dynamically when the target is a library. Because of this the available
    free space is checked during the copy/move operation and cannot be checked before the
    copy/move begins.

### Developer Notes

 1. The ELS Navigator was built using [JFormDesigner](https://www.formdev.com/jformdesigner/doc/).<br/> 
    This inexpensive plug-in for IntelliJ allowed the creation of the Navigator
    much faster and with far fewer mistakes.
 
 2. For existing user-written scripts add the "-C ." option to set the working directory to the current
    directory. See Command Line Changes, item 17.
 

## Version 3.1.0

Release 3.1.0 of ELS adds the Hint Status Tracker and a new mode - the Hint Status
Server (HSS). The Tracker coordinates hint completion status locally. The Server
tracks hint completion status for remote operations. These are needed when two
or more back-ups are being used with hints.

The Tracker and new HSS mode are optional. All previous features and behavior remain the same.
The HSS is an additional separate process that is executed before any remote operation
requiring hint coordination. Options are available to allow the HSS to run continuously or
"ordered" to quit by a publisher or subscriber when an operation is completed. A
separate TCP/IP port is required for the status server listener.

### Enhancements

 1. ELS Hint Status Tracker and Hint Status Server, see [Hint Status](Hint-Status).

### Command Line Changes

 1. -h has been repurposed *and* -H added for hint support.

    Previously the -h | --version options were used for help that only displayed
    the version. The --version option still does that.

    The -h option is now -h | --hints [file] : Hints Status Server file to enable 
    connection to the new ELS Hint Status Server.

    Added -H | --hint-server [file] : Hints Status Server to execute continuous hint
    status server daemon

 2. Added -q | --quit-status : Send quit command to hint status server when operation
    is complete. Allows either a publisher or subscriber to tell the HSS to shutdown.

    The execution sequence **must** be the HSS, then subscriber, then publisher. The
    publisher commands the subscriber to quit automatically when the operation is
    done. So it is best to add the --quit-status option to the subscriber so when
    it shuts down it will command the Hint Status Server to quit - if desired.

 3. Added -Q | --force-quit : Special option that only connects to the HSS to
    send a quit command, then it ends. Requires a --hint file and -p | -P publisher
    file to specify the to/from connection ends respectively.

### Bug Fixes

 1. Issue #30 *'Fix terminal_allowed handling'*.

    Added the logic necessary to use the terminal_allowed value in the JSON file.
 
 2. Issue #34 *'Fix empty -t | -T handling'*.
 
    Fixed the issue when using an empty -t | -T to use the sources as targets.

 3. Issue #35 *'Fix --remote M'*.

    Fixed the automated login issue when using --remote M.

### Developer Notes

 1. With the addition of the Hint Status Server where a remote ELS session is
    employing 3 ELS processes - hint server, subscriber, and publisher - it was
    necessary to rearrange the disconnect/shutdown logic and sequences. These 
    changes implement a more formal, and less brute-force, disconnect and quit
    approach allowing for future n-way connection possibilities.
 
 2. For IntelliJ to run and debug the multiple processes the Multirun plugin
    has been added with a variety of configurations in the .idea project.

 3. The mock directory has been completely rearranged to support testing and 
    provide a completely self-contained development and test environment.
    In addition a mock/scripts/linux/ directory has been added with many scripts
    to perform application-level tests using pre-set publisher and subscriber
    collections and hint files.

    These scripts show many of the various ways ELS may be executed using
    different combinations of options. See the **README** in that directory for
    more information and a description of the testing sequence.

 4. For IntelliJ users several run/debug configurations have been added that
    match the scripts in the mock/scripts/linux/ directory organized in the
    same way and use the same mock/ data.


## Version 3.0.0

### Enhancements

 1. ELS Hints

    While curating a media collection files and directories are renamed, moved and
    deleted. To avoid unnecessary copies and duplicates on back-ups a mechanism is
    needed to coordinate manual changes.

    A "hint" is a special file used to keep track of manual changes to a collection.
    The hint is used by ELS to coordinate those changes with one or more back-ups.

    See [Hints](Hints) in the ELS wiki.

### Bug Fixes

 1. Issue #16 *'Add more granular control of target minimum free space'*.
    
    Added a 'locations' clause to the library/collection files that specifies minimum
    available storage space for individual devices, as opposed to the library level
    that is defined in targets.json. This clause is optional.
    
    If targets.json is specified on the command line that value is used. If the targets
    file or the particular library is not specified the value from the locations
    in the subscriber file is used for the matching device. If that is not specified
    the default of 100 GB is used. Linux and Windows location values are supported.
    
    The syntax of the locations and library sources must match for string comparisons.
    
    Example:
    ```
        "locations": [
            {
                "location": "/home/plex/Plex/Media/Plex01",
                "minimum": "42GB"
            },
            {
                "location": "/home/plex/Plex/Media/Plex02",
                "minimum": "10GB"
            }
        ]
    ```

 2. Issue #22 *'Add option to use subscriber sources as targets'*.

    Changed the behavior of -t | --targets and -T | --force-targets to allow the
    targets.json to not be specified. The option alone enables a back-up run
    unless the --dry-run option is specified.
    
    When no targets.json is defined the sources from the subscriber.json file are
    used instead. When combined with the new 'locations' clause this allows all
    sources to be used as target locations making targets.json unnecessary. But there
    are situations where a targets.json file is desirable and is still fully supported.
    
    If targets.json is defined those locations and minimums override subscriber.json
    sources IF the particular library is defined within the targets.json
    file. Otherwise the sources for that library will be used.
    
    This applies to local and remote sessions. For a remote session if the 
    subscriber's targets are either requested by the publisher or the subscriber
    forces it to the publisher this behavior is consistent. If the subscriber
    only specifies the option, or the particular library is not defined in the 
    subscriber's targets.json, then its library/collection sources will be 
    used by the publisher.    

    If the subscriber's **collection** is either requested by the publisher or the
    subscriber forces it to the publisher those locations and sources are used when
    a library's sources are used for the targets.

    All previous files and behavior are supported. A separate targets.json
    file may now contain just the libraries and particular target locations needing
    to be managed. Or the details for an entire collection. But much of the information
    is also in the subscriber.json file.
    
 3. Issue #21 *'Catch exceptions better'*

    Adjusted the exception handling to hopefully catch common problems for both local
    and remote sessions. Makes detection of the 'Process completed normally' line
    in the log file, that marks a 'successful' run, more reliable for automation purposes.
    
    A reminder: Although it's a little weird a FATAL log level is used for this
    log line so it is always written to the log file regardless of the command line
    log level argument. 

 4. Issue #23 *'Add -L | --exclude option to exclude certain libraries'*.

    Like the -l | --library option the new exclude option may be specified multiple
    times. Each library is excluded from processing. Useful when one or two
    libraries are to be skipped in a collection with many libraries.
    
    A change from version 2.2.0 is the -l | --library and -L | --exclude options
    may now be specified on the subscriber-side to limit which libraries are
    processed. Previously -l | --library only applied to the publisher side.

 5. Added options -F | --log-overwrite that will delete the log file when starting.
    Used instead of -f | --log-file that will append to an existing file.
