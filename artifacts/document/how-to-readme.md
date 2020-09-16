
# ELS: How To Read Me

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

## Command Line Options

Options for short and long versions are case-sensitive.

### Actions

 * -D | --dry-run : Do everything except actually copy
 
 * -e | --export-text file : Export publisher collection as text to file 
 
 * -i | --export-items file : Export publisher collection as JSON to file

 * -r | --remote P|L|M|S|T : This is a remote session, see comm.md for details 

### Parameters

 * -a | --authorize password : The password required to access Authorized mode
      when allowing -r remote clientStty manual access

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

 * -n | --whatsnew file : What's New output file

 * -N | --whatsnew-all file : What's New output file showing all new items

 * -p | --publisher-libraries file : Publisher JSON libraries file
 
 * -P | --publisher-collection file : Publisher JSON collection items file

 * -s | --subscriber-libraries file : Subscriber JSON libraries file
 
 * -S | --subscriber-collection file : Subscriber JSON collection items file
 
 * -t | --targets file : Targets filename, see Notes
 
 * -T | --force-targets file : Targets import filename, see Notes


### Notes

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

Log Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF.

### Rules
 * -e is only performed then it stops, so other options like -s or -m do not make sense
 * -i and -s together do not make sense
 * To perform a munge operation -S/s, -P/p and -T/t are required.
