ELS uses a publish/subscribe conceptual model. ELS compares
one or more libraries from a media publisher with those on a 
media subscriber. ELS can then synchronize the subscriber with
the publisher.

ELS has many options and ways of combining those options to perform
a wide variety of operations.

## Basic Command

Once the JSON library files are created and validated a basic
munge operation requires a publisher and subscriber library or collection
files and a targets file that describes where each library's new items
will be copied to the subscriber.

A "library" file lists the libraries only, a "collection" file also contains
all the individual items in each library. Both are JSON files.

A collection JSON file may be generated using the -i or --export-items option
and specifying a publisher file.

Collection data are required to perform a munge. Generally the lowercase
variation of an option will cause the needed data to be dynamically generated
at runtime. Whereas the uppercase variation will import the data from a file.

Example basic back-up operation:
```
    java -jar ELS.jar -p publisher.json -s subscriber.json -t targets.json -f els.log
```

## Command Line Options

Options for short and long versions are case-sensitive.

### Actions

 * -D | --dry-run : Do everything except actually copy, used in -n and
     back-up operations.
 
 * -e | --export-text file : Export publisher collection as text to file 
 
 * -i | --export-items file : Export publisher collection as JSON to file

 * -n | --rename : Perform any defined renaming from the publisher library file
 
 * -r | --remote P|L|M|S|T : This is a remote session, see [Communications How-To](Communications-How-To) for details 

 * -u | --duplicates : Performs publisher duplicates and empty directories check
 
### Parameters

 * -a | --authorize password : The password required to access Authorized mode
      when allowing -r remote clientStty manual access

 * -b | --no-back-fill : Disables attempting to "back fill" original media locations
     with new files, e.g. new TV episode. Always uses the targets.

 * -c | --console-level level : Console logging level, default debug
 
 * -d | --debug-level level : File logging level, default info.
      Levels= off, fatal, error, warn, info, debug, trace, all.
      If level = info then Java method and line number are not added.
 
 * -f | --log-file file : Log file, default ELS.log in directory where "run" is executed
 
 * -k | --keep : Keep els.json files, default is to delete them as they are processed.
      Not implemented yet.

 * -l | --library libraryname : Publisher library to process, default all.
      This option may be specified more than once for each desired library.
 
 * -m | --mismatches file : Mismatches output file (differences)

 * -o | --overwrite : Overwrite any existing files instead of resuming a remote transfer.
      This option only applies to remote sessions. Local operation always overwrites.

 * -p | --publisher-libraries file : Publisher JSON libraries file
 
 * -P | --publisher-collection file : Publisher JSON collection items file

 * -s | --subscriber-libraries file : Subscriber JSON libraries file
 
 * -S | --subscriber-collection file : Subscriber JSON collection items file
 
 * -t | --targets file : Targets filename, see Notes
 
 * -T | --force-targets file : Targets import filename, see Notes
 
 * -v | --validate : Validate/verify publisher library or collection file
 
 * -w | --whatsnew file : What's New output file

 * -W | --whatsnew-all file : What's New output file showing all new items

 * -x | --cross-check : Cross-check ALL libraries for duplicates, instead of within a library. 


### Notes

The -b | --no-back-fill option disables the default behavior of attempting
to place new files for an existing item in the original location that might
not be a location in the targets file. For instance another episode of a
TV show.

The -D option applies to -n | --rename and back-up operations.

The -e export paths option lists each individual file in the desired
libraries. But that data is not used by any other operation. It is 
intended for visual information and possibly comparison with another
similar file.

The -e and -i options require either -p or -P option. Also -e and -i do
immediate scans based on configuration. 

The -k option applies to the Provider. Subscriber els.json files are not
involved in a run.

The -t and -T options are equivalent unless the -r option is enabled. Then
-T will use a local file and -t will request the targets file from
the subscriber.

Log Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF. The debug level is
the controller of the level. The console level may show less but not more than
the debug level.

### Multiple actions

Different actions can be performed during one execution, i.e. actions may be
combined. The order of processing of the actions is:

 1. Renaming with -n | --rename
 2. Text export with -e | --export-text 
 3. Item export with -i | --export-items
 4. Duplicates check with -u | --duplicates
 5. Finally a back-up operation if all necessary arguments are provided. 

### Rules
 * -e is only performed then it stops, so other options like -s or -m do not make sense
 * -i and -s together do not make sense
 * To perform a munge operation -S/s, -P/p and -T/t are required.
