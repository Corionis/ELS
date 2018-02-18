# Next Synch

## Media
 * Mark all not-so-good movies with fubar, or whatever
 * Mark Korean Subtitled movies with R6 at end of filename
 * Deal with SRTs
 * Go over how to handle Director cuts See Underworld
 x Fix Avatar & its extended edition
 * Should we rename "Documentary Movies" to ...Movie
 x Go though Documentary libraries and verify all have been changed
 * REMEMBER - Put NEW Targets in libray so they are scaned

## Code
 * Put counter in log file
 * Fix Exclusions
 * Auto Renamer
 * Rename GitHub project to VolMunger
 x Add log flushes
 * Add library name option so only certain libraries are processed
 * Add a find dupes in collection
 x NOT NEEDED - Add option to overwrite output files
 * Fix library name issue. The directories should not have to match the
   library or group name.
 * Avoid cascading errors if all targets are full
 * For .volmunger actions:
   - transcode option for high resource demand items, 
     e.g. Bring It On, Sons of Anarchy
 * Add a time metric.

# Ideas

The existence of an empty (or not) volmonger.json file triggers the
"I Win" behavior.

"I Win" means - make the target look like my source - including any
deletions necessary.

If volmonger.json files exist on both systems it is a conflict.
The conflict is flagged, logged into a input file for re-rerun purposes,
then skipped for that monge run.

Because volumes can be quite large there should be a mechanism
to feed the conflicts back into another VolMonger run so once
the conflict is resolved it is easier to complete the synch.

The utility must allow for the collection file to be specified
as an argument. That allows multiple combinations ... for instance
both source and target collection files when regularly monging
on a different computer where drive letters would change.

## Features
Various features and nuances of VolMonger.

 * Both lower- and upper-case -p/-P or -s/-S options may be used
   to send multiple publishers to one subscriber, and vice-versa.
   Example: A normal publisher with hard drives and a separate
   shuttle drive as publishers.


## Organization
 * A publisher is the provider of the data.
 * A subscriber is the consumer of the data.
 * Both have Libraries.
 * A subscriber libraries file may be a subset, i.e. only what
   content is desired from the publisher.
 
 * Libraries describe one or more Library objects.
 * Each Library is described by:
    - a name 
    - one or more sources (directories) containing Items
 * An item can be either a directory or file.
 
 * A Collection is the list of Items from scanning one or more libraries.
 * The Collection file is described by:
   - a list of the libraries 
   
 * A Group is an internal set of items in the same
   directory. Items are synchronized by group.
   - This works for a movie and that directory's contents,
   - and works for a TV show and a season directory.
 

## Assumptions
 * The exact locations of the contents of each library is
   not known at the beginning of a VolMonger run.
 * A directory of only one collection or the other will be
   modified between mongings.
   - If a volmonger.json exists on both it is a conflict
   - Conflicts are logged for re-run
 * To monge two operational systems will require one to use
  a collection file for the target system configured from
  its own connection perspective, e.g. drive letters, etc.  

### Collection Files
 * No two library names may be the same, i.e. library names must be unique

### Target Files
 * One or more may be specified
 * Space check perform before copying
 * Automatic roll-over to the next target
   
## Design

### Questions
 * Can a target directory be found more quickly by:
   - Pre-scanning all sources of a given library then search that list?
   - Look through each source for its exact location?
 * How are comparisons done?
   - Must be done within some kind of context
   - How to establish the context without adding a "type of content" parameter?
     - A movie is in one directory, generally
     - A tv show could be in 1, 2 or 3 levels of subdirectories
     - What are the other permutations?
 * How to handle N-sided monges? Cannot delete volmonger.json if so.

### Command Line Options
 * -c level : Console logging level, default debug
 
 * -d level : File logging level, default info 
      Levels= off, fatal, error, warn, info, debug, trace, all.
      If level = info then Java method and line number are not added.
 
 * -D : Dry run, validate, scan, and match but do not make any changes
 
 * -e file : Export publisher items to file
 
 * -f file : Log file, default VolMonger.log in directory where "run" is executed

 * -k : Keep volmonger.json files, default is to delete them as they are processed
 
 * -l library : Publisher library to process, default all
      May need to add specific item(s)
 
 * -m file : Mismatch output file (differences)

 * -n file : Show What's new in a readable format, to send to your Plex users

 * -p file : Publisher libraries file
 
 * -P file : Publisher collection import items file
 
 * -s file : Subscriber libraries  file
 
 * -S file : Subscriber collection import items file
 
 * -t file : Targets filename

 * -v : Validate collections files only then exit

Log Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF.


The -k option applies to the Provider. Subscriber volmonger.json files are not
involved in a run.

 * RULES OF COMMAND-LINE OPTIONS, in no particular order:
   * -e is only done then it stops, so other options like -s or -m do not make sense
   * -i and -s together do not make sense
   * -P and -S used at the same time also requires -t.

---

IDEA: A way to manage who got what with multiple Subscribers.

Have an option to scan a Publisher for "completed" volmonger.json files.

A "completed" volmonger.json file contains "done" markers by date for each Subscriber.

Subscribers, by name, could be kept in a separate JSON file.

As runs are made with Subscribers a stamp is added to the volmonger.json file.

Work-out the stamp and "done" logic.

Once it all works it could just be part of the process.


IDEA: Add to the data inside a volmonger.json file:

 * Tag:  permanent      == do not delete this volmonger.json file
 * Tag:  ignore         == skip this item.

---

### Process
This is a file-for-file matching/synchronization process.

 1. Iterate through libraries
 2. Walk each source library comparing against target
 3. If a volmonger.json file exists on source "I Win" logic is triggered
 4. Reverse "Provider" and "Subscriber" sides and repeat for a bi-directional monge.

---

## Other

### Convenience Utilities
 * [Sent-To utility](send-to utility.md) volmonger.json file generator.
 * [Plex-to-VolMonger generator](plex-to-VolMonger.md) for automated collection file generation.

---

## Use Cases

 1. Subscriber visits Publisher
    * Publisher has own drives
    * Subscriber drives (E,F,...) will not mount on those same drive letters
 
 2. Publisher visits Subscriber
    * Subscriber has own drives
    * Publisher drives (E,F,...) will not mount on those same drive letters

 3. Internet transport
    * Publisher has own drives
    * Subscriber has own drives

 4. Transport drive(s)
    * Publisher has own drives
    * Subscribers drive letters will not match 

 5. Local Backup
    * Publisher has own drives
    * Subscriber has own drives

 6. Cloud Synchronization
    * Publisher has ALL drives
 
 7. Move content within a library - not a VolMonger use case.

 8. Move content to a different library - delete + add, but can it be done with a move?

 9. Add new content from provider, default behavior.

## volmonger.json Use Cases

 1. volmonger.json, Deletes, very rare. 
   - A volmonger.json extension on a Publisher directory with no
   content indicates the directory should be deleted.

 2. volmonger.json, Changes of existing file(s)
   - Different image, subtitles ... any file
   - Cannot trust dates

 3. volmonger.json, Moves do not matter, this is location inspecific with a library

