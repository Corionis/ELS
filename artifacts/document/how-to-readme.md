
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

A library file lists the desired libraries to compare, a collection file
also contains all the individual items in each library. Both are JSON files.

A collection JSON file may be generated using the -i item export option.
Collection data is used to perform a munge.

The -e export paths option lists each individual file in the desired
libraries. But that data is not used by any other operation. It is 
intended for visual information and possibly comparison with another
similar file.

## Command Line Options

 * -c level : Console logging level, default debug
 
 * -d level : File logging level, default info 
      Levels= off, fatal, error, warn, info, debug, trace, all.
      If level = info then Java method and line number are not added.
 
 * -D : Dry run, validate, scan, and match but do not make any changes
 
 * -e file : Export publisher libraries to text file
 
 * -f file : Log file, default VolMunger.log in directory where "run" is executed

 * -k : Keep volmunger.json files, default is to delete them as they are processed
 
 * -i file : Export JSON items to file
 
 * -l library : Publisher library to process, default all
      May need to add specific item(s)
 
 * -m file : Mismatch output file (differences)

 * -n file : Show What's new in a readable format, to send to your Plex users

 * -p file : Publisher libraries file
 
 * -P file : Publisher JSON collection import items file

 * -r P|S : This is a remote session and this is the Publisher or Subscriber 
 
 * -s file : Subscriber libraries file
 
 * -S file : Subscriber JSON collection import items file
 
 * -t file : Targets filename, see Notes
 
 * -T file : Targets import filename, see Notes

 * -v : Validate collections files only then exit

### Notes

The -i option requires either -p or -P options. 

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
