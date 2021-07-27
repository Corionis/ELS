ELS uses a publish/subscribe conceptual model. ELS compares one or more
libraries from a media subscriber (the back-up) with those on a media
publisher (the home media system). ELS then synchronizes each subscriber
library with the publisher.

ELS has many options and ways of combining those to perform a wide
variety of operations.

## Basic Command

A basic back-up requires publisher and subscriber library or collection
files and a targets file that describes the locations for each library's
new content.

Using the publish/subscribe paradigm ELS goes through the subscriber's
libraries (subscriptions) looking for new items from the publisher. That
is, the subscriber controls which libraries are backed-up to its
storage.

### Library & Collection JSON Files

Different options are available for specifying either a library file
or collection file for the publisher and subscriber.

A *"library"* file has basic information describing the media server and
lists the libraries and their sources. It is the minimum required.

A *"collection"* file also includes the individual items in each library,
files and directories.

If a library file is specified automatic scans of those library sources
are performed. If a collection file is specified only the items in the
file are used, no scans are performed.

A library file may be created by hand. Also, an add-on tool is available
to generate a basic ELS library file from a [Plex Media
Server](https://www.plex.tv), see the [ELS Plex
Generator](https://github.com/GrokSoft/ELS-Plex-Generator). However ELS
supports any modern media system that uses the same directory structure.

### Generating a Collection JSON File

A collection JSON file may be generated using the -i or --export-items option
and specifying a publisher library file as input.

If a collection file is specified only the items in that file are processed.
This allows the file to be edited if desired.

Note however that a collection file is not required to run ELS. Only a
library file is required.

### Basic Example

Generally the lowercase variation of an option will cause the needed
data to be dynamically generated at runtime. Whereas the uppercase
variation will import the data from a file for that option: publisher,
subscriber and targets.

Basic back-up with a log file:
```
    java -jar ELS.jar -p publisher.json -s subscriber.json -t targets.json -f els.log
```

As a starting point the ELS Complete distribution file contains
Windows batch files and Linux shell scripts for several common operations.

## Command Line Options

Options for short and long versions are case-sensitive.

### Actions

The default action is to perform a back-up if the publisher, subscriber and targets
files have been specified.

* -e | --export-text [file] : Export publisher collection as text to file

* -i | --export-items [file] : Export publisher collection as JSON to file

* -n | --rename [F|D|B] : Perform any defined renaming from a publisher JSON file
  on **F**iles, **D**irectories or **B**oth

* -u | --duplicates : Scan a publisher for duplicate items and empty directories

* -v | --validate : Validate a publisher library or collection file

### Parameters

* -a | --authorize [password] : The password required for authorized accesss
  when in -r | --remote mode and allowing STTY interactive access to a listener

* -b | --no-back-fill : Disables attempting to "back fill" original media sources
  with new files, e.g. a new TV episode. Always uses the target locations

* -c | --console-level [level] : Console logging level, default debug

* -d | --debug-level [level] : File logging level, default info.

* -D | --dry-run : Do everything except the actual action, used in --rename and
  back-up actions

* -f | --log-file [file] : Log file, in append mode

* -F | --log-overwrite [file] : Log file, in overwrite mode

* -h | --version : Display version information

* -l | --library [libraryname] : Library to process, if not specified process
  all libraries. Applies to publisher-side only. This option may be specified
  more than once

* -m | --mismatches [file] : Mismatches list of differences output text file

* -o | --overwrite : Overwrite any existing files instead of resuming a remote transfer.
  This option only applies to remote sessions.

* -p | --publisher-libraries [file] : Publisher JSON library file

* -P | --publisher-collection [file] : Publisher JSON collection file

* -s | --subscriber-libraries [file] : Subscriber JSON library file

* -S | --subscriber-collection [file] : Subscriber JSON collection file

* -t | --targets [file] : Targets JSON file, see Notes

* -T | --force-targets [file] : Forced targets for -r | --remote, see Notes

* -w | --whatsnew [file] : What's New output text file as a summary

* -W | --whatsnew-all [file] : What's New output text file with all new items

* -x | --cross-check : Cross-check ALL libraries for an item instead of just within
  that item's library. Applies to --rename and back-up actions

### Modes

The default is local mode where all storage locations are accessible to one ELS process.

* -r | --remote [P|L|M|S|T] : This is a remote session,
  see the [Communications How-To](Communications-How-To) for details



## Notes

The -b | --no-back-fill option disables the default behavior of attempting
to place new files for an existing item in the original source as related
items. For instance
another episode of a TV show.

The -D | --dry-run option applies to -n | --rename and back-up actions.

The -e | --export-text option lists each individual file in the desired
libraries. That data is not used by any other action. It is
intended for visual information and possible comparison with a
similar file.

The -i | --export-items option generates a collection JSON file.

Both the -e | --export-text and -i | --export-items options require a
publisher JSON file.

The -m | --mismatches option lists the differences between the publisher and
subscriber and apply to back-up actions.

The -n | --rename with the B (Both) type will rename files then directories.
Note all directories are renamed up to the library name level only. Any
path leading to the library is not touched.

The -p | --publisher-libraries option will perform library scans as needed.
The -P | --publisher-collection option does not do any scans.

The -s | --subscriber-libraries option will perform library scans as needed.
The -S | --subscriber-collection option does not do any scans.

The -t and -T options are equivalent unless the -r | --remote option is enabled,
see the [Communications How-To](Communications-How-To) for details

The -v | --validate action may be used with only a publisher specified
so a subscriber file is not required.

The -w | --whatsnew and -W | --whatsnew-all options apply to back-up
actions.

### Log Levels

Log Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF. The debug level is
the controller of the level. The console level may show less but not more than
the debug level.

If the level is set to "info" then Java method and line number information is not included.

## Multiple actions

Different actions can be performed during one execution, i.e. actions may be
combined. The order of processing of the actions is:

1. Renaming with -n | --rename
2. Text export with -e | --export-text
3. Item export with -i | --export-items
4. Duplicates check with -u | --duplicates
5. Finally a back-up if all necessary arguments are provided.

Note that --dry-run applies to --rename and back-up actions.

## Linux Caution

On Linux-based systems ELS should be executed as the user that owns the
media to ensure the ownership and permissions of copied content have
the appropriate attributes.

In addition, it is not possible to login as 'root' then sudo ELS in
-r M (manual) or -r T (terminal) modes because the program will not
have the necessary permissions to open the display.
