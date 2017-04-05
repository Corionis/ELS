# Ideas

The existence of an empty (or not) .volmonger file triggers the
"I Win" behavior.

"I Win" means - make the target look like my source - including any
deletions necessary.

If .volmonger files exist on both systems it is a conflict.
The conflict is flagged, logged into a input file for re-rerun purposes,
then skipped for that monge run.

Because volumes can be quite large there should be a mechanism
to feed the conflicts back into another volmonger run so once
the conflict is resolved it is easier to complete the synch.

The utility must allow for the collection file to be specified
as an argument. That allows multiple combinations ... for instance
both source and target collection files when regularly monging
on a different computer where drive letters would change.

## Organization
 * A publisher is the source of the data.
 * A subscriber is the consumer of the data.
 * A collection is a set of named libraries.
   - A collection is described in a single collection file.
 * A named library is one or more directories, possibly on
 different drives.
 * Each source ...
 * Each target is one or more directories, possibly on
 different drives.
 * As each target becomes full to a specified minimum the
 next target is used.

## Assumptions
 * The exact locations of the contents of each library is
 not known at the beginning of a volmonger run.
 * A directory of only one collection or the other will be
  modified between mongings.
   - If a .volmonger exists on both it is a conflict
   - Conflicts are logged for re-run
 * To monge two operational systems will require one to use
  a collection file for the target system configured from
  its own connection perspective, e.g. drive letters, etc.

## Use Cases
 * Add new content from source
   - Automatically synchronized, default behavior
 * Deletes, very rare. 
   - ***This scheme does not handle a whole-directory delete.***
 * Changes of existing file(s)
   - Different image, subtitles ... any file
   - Cannot trust dates
 * Moves do not matter, this is location inspecific with a library

## Collection File

```JSON
    {
        "metadata" : {
            "name" : "RockPlex"
        },
        "libraries" : {
            "Movies" : {
                "metadata" : {
                    "minimum" : "10G"
                },
                "targets" : [
                    "H:/media/movies",
                    "I:/media/movies"
                ],
                "sources" : [
                    {"dir": "D:/media/movies"},
                    {"dir": "F:/media/movies", "context": 3},
                    "H:/media/movies",
                    "C:/overflow"
                ]
            },
            "TV Shows" : {
                "metadata" : {
                    "minimum" : "4G"
                },
                "targets" : [
                    "H:/media/tv shows",
                    "I:/media/tv shows"
                ],
                "sources" : [
                    "E:/media/tv shows",
                    "G:/media/tv shows",
                    "H:/media/tv shows"
                ]
            }
        }
    }
```
 * Targets
   - One or more may be specified
   - Space check perform before copying
   - Automatic roll-over to the next target

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
 * How to handle N-sided monges? Cannot delete .volmonger if so.

### Process
This is a file-for-file matching/synchronization process.

 1. Iterate through libraries
 2. Walk each source library comparing against target
 3. If a .volmonger file exists on source "I Win" logic is triggered
 4. Reverse "source" and "target" sides and repeat for a bi-directional monge.

## Other

### Convenience Utilities
 * [Sent-To utility](send-to utility.md) .volmonger file generator.
 * [Plex-to-volmonger generator](plex-to-volmonger.md) for
 automated collection file generation.



