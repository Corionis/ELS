
## Media
 * Mark all not-so-good movies with fubar, or whatever
 * Mark Korean Subtitled movies with R6 at end of filename
 * Deal with SRTs
 * Go over how to handle Director cuts See Underworld
 x Fix Avatar & its extended edition
 * Should we rename "Documentary Movies" to ...Movie
 x Go though Documentary libraries and verify all have been changed
 * Command-line search function
 * REMEMBER - Put NEW Targets in library so they are scanned
 * Add carrying total bytes copied for showing progress

## Code
 * Library name in JSON and physical directory names must match
T * Put counter in log file
T * Total size math is wrong
T * Fix Exclusions
 * Auto Renamer - xpaus
 x Rename GitHub project to VolMunger
 x Add log flushes - there is no Flush
 * Add library name option so only certain libraries are processed
   - Partially done, see TODO's
 * Add a find dupes in collection
 x NOT NEEDED - Add option to overwrite output files
 * Avoid cascading errors if all targets are full
 * For .volmunger actions:
   - transcode option for high resource demand items, 
     e.g. Bring It On, Sons of Anarchy
 * Add a time metric.
 * Add "double-dash" option equivalents to make commands more readable
    * Examples:
       * -p and --publisher-library
       * -P and --publisher-collection
       * -D and --dry-run
       * -e and --export-text
       * -i and --export-collection
       * -r P and --remote-publisher
       * -r S and --remote-subscriber
       * -g and --get-item

# Ideas

The existence of an empty (or not) volmunger.json file triggers the
"I Win" behavior.

"I Win" means - make the target look like my source - including any
deletions necessary.

If volmunger.json files exist on both systems it is a conflict.
The conflict is flagged, logged into a input file for re-rerun purposes,
then skipped for that munge run.

Because volumes can be quite large there should be a mechanism
to feed the conflicts back into another VolMunger run so once
the conflict is resolved it is easier to complete the synch.

The utility must allow for the collection file to be specified
as an argument. That allows multiple combinations ... for instance
both source and target collection files when regularly munging
on a different computer where drive letters would change.

## Features
Various features and nuances of VolMunger.

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
    - one or more sources (directories)
 
 * A Collection is the list of Items from scanning one or more libraries.
 * The Collection file is described by:
   - a list of the libraries 
   - a list of each item in each library
 * An item can be either a directory or file.

 * A Repository is a generic term that is either a Library or Collection.

 * A Group is an internal set of items in the same
   directory. Items are synchronized by group.
   - This works for a movie and that directory's contents,
   - and works for a TV show and a season directory.
 

## Assumptions
 * The exact locations of the contents of each library is
   not known at the beginning of a VolMunger run.
 * A directory of only one collection or the other will be
   modified between mungings.
   - If a volmunger.json exists on both it is a conflict
   - Conflicts are logged for re-run
 * To munge two operational systems will require one to use
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
 * How to handle N-sided munges? Cannot delete volmunger.json if so.


---

IDEA: A way to manage who got what with multiple Subscribers.

Have an option to scan a Publisher for "completed" volmunger.json files.

A "completed" volmunger.json file contains "done" markers by date for each Subscriber.

Subscribers, by name, could be kept in a separate JSON file.

As runs are made with Subscribers a stamp is added to the volmunger.json file.

Work-out the stamp and "done" logic.

Once it all works it could just be part of the process.


IDEA: Add to the data inside a volmunger.json file:

 * Tag:  permanent      == do not delete this volmunger.json file
 * Tag:  ignore         == skip this item.

---

### Process
This is a file-for-file matching/synchronization process.

 1. Iterate through libraries
 2. Walk each source library comparing against target
 3. If a volmunger.json file exists on source "I Win" logic is triggered
 4. Reverse "Provider" and "Subscriber" sides and repeat for a bi-directional munge.

---

## Other

### Convenience Utilities
 * [Sent-To utility](send-to utility.md) volmunger.json file generator.
 * [Plex-to-VolMunger generator](plex-to-VolMunger.md) for automated collection file generation.

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
 
 7. Move content within a library - not a VolMunger use case.

 8. Move content to a different library - delete + add, but can it be done with a move?

 9. Add new content from provider, default behavior.

## volmunger.json Use Cases

 1. volmunger.json, Deletes, very rare. 
   - A volmunger.json extension on a Publisher directory with no
   content indicates the directory should be deleted.

 2. volmunger.json, Changes of existing file(s)
   - Different image, subtitles ... any file
   - Cannot trust dates

 3. volmunger.json, Moves do not matter, this is location inspecific with a library

 4. volmunger.json, Move to library, e.g. Movies to Documentary Movies
 
