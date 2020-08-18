
## To Do
 * Test large files.
 * BUG: Library name in JSON has to match physical directory name.
 * Duplicate finder

## Media
 * Mark all not-so-good movies with fubar, or whatever
 * Mark Korean Subtitled movies with R6 at end of filename
 * Deal with SRTs
 * Go over how to handle Director cuts See Underworld
 x Fix Avatar & its extended edition
 * Search function
 * REMEMBER - Put NEW Targets in library so they are scanned

## Code
 * Library name in JSON and physical directory names must match
 * Auto Renamer - xpaus
 * Add library name option so only certain libraries are processed
   -l Partially done, see TODO's
 * Add a find dupes in collection
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
 * To munge two operational systems will require one to use
   a collection file for the target system configured from
   its own connection perspective, e.g. drive letters, etc.  

### Collection Files
 * No two library names may be the same, i.e. library names must be unique

### Target Files
 * One or more may be specified
 * Space check perform before copying
 * Automatic roll-over to the next target
   
---

## Other

### Convenience Utilities
 * [Sent-To utility](send-to utility.md) volmunger.json file generator.
 * [Plex-to-VolMunger generator](plex-to-VolMunger.md) for automated collection file generation.

---

## volmunger.json control file Use Cases

 1. volmunger.json, Deletes, very rare. 

 2. volmunger.json, Changes of existing file(s)
   - Different image, subtitles ... any file
   - Cannot trust dates

 3. volmunger.json, Moves do not matter, this is location inspecific with a library

 4. volmunger.json, Move to library, e.g. Movies to Documentary Movies
 
