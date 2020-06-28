
# VolMunger: How To Read Me

VolMunger uses a publish/subscribe conceptual model. VolMunger compares
one or more Plex libraries from a media publisher with those on a 
media subscriber. VolMunger can then synchronize the subscriber with
the publisher.

VolMunger has many options and ways of combining those options to perform
a wide variety of operations.

## Basic Command

Once the JSON library-definition files are created and validated a basic
munge operation requires a publisher's library or collection file and
the same for the subscriber.

A "library" file lists the libraries only, a "collection" file also contains
all the individual items in each library. Both are JSON files.

A collection JSON file may be generated using the -i item export option.

Collection data are required to perform a munge. Generally the lowercase
variation of an option will cause the needed data to be dynamically generated
at runtime. Whereas the uppcase variation will import the data from a file.

## Command Line Options

 * -a password : The password required to access Authorize mode
      when allowing -r remote sttyClient access

 * -c level : Console logging level, default debug
 
 * -d level : File logging level, default info 
      Levels= off, fatal, error, warn, info, debug, trace, all.
      If level = info then Java method and line number are not added.
 
 * -D : Dry run, validate, scan, and match but do not make any changes
 
 * -e file : Export publisher items to flat text file
 
 * -f file : Log file, default VolMunger.log in directory where "run" is executed
 
 * -g item : Get the specific item from the -l library, -l required
             An "item" is the granularity of a movie (directory) or a tv season (directory).

 * -k : Keep volmunger.json files, default is to delete them as they are processed
 
 * -i file : Export publisher items to collection file
 
 * -l library : Publisher library to process, default all
 
 * -m file : Mismatch output file (differences)

 * -n file : Show What's new in a readable format, to send to your Plex users

 * -p file : Publisher JSON libraries file
 
 * -P file : Publisher JSON collection import items file

 * -r P|S : This is a remote session and this is the Publisher or Subscriber, P|S is case insensitive 

 * -s file : Subscriber JSON libraries file
 
 * -S file : Subscriber JSON collection import items file
 
 * -t file : Targets filename, see Notes
 
 * -T file : Targets import filename, see Notes

 * -v : Validate collections files only then exit

### Notes

The -e export paths option lists each individual file in the desired
libraries. But that data is not used by any other operation. It is 
intended for visual information and possibly comparison with another
similar file.

The -e and -i options require either -p or -P option. Also -e and -i do
immediate scans based on configuration. 

The -k option applies to the Provider. Subscriber volmunger.json files are not
involved in a run.

The -t and -T options are equivalent unless the -r option is enabled. Then
-T will use a local file and -t will request the targets file from
the subscriber.

Log Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF.

### Rules
 * -e is only performed then it stops, so other options like -s or -m do not make sense
 * -i and -s together do not make sense
 * To perform a munge operation -S/s, -P/p and -T/t are required.
