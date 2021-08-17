Hints are a new feature beginning with ELS 3.0.0.

While curating a media collection files and directories are renamed, moved and
deleted. To avoid unnecessary copies and duplicates on back-ups a mechanism is
needed to coordinate manual changes.

A "hint" is a special file used to keep track of manual changes to a collection.
The hint is used by ELS to coordinate those changes with one or more back-ups.

A hint file contains one or more status lines, one for each back-up, and one or
more commands. As each system processes the hint it's status line is updated.
When all systems have executed the hint the file is automatically deleted.

## Enabling ELS Hints

  Hints are optional. Hint processing is enabled using the -k | --keys or
  -K | --keys-only options  specifying a keys filename. The -K | --keys-only
  variants only process and skip the main backup process.

  To correlate publisher and subscriber collections with status keys in ELS hint
  files a single keys file is needed.

  Example:

  ```
      # Name and key matching the key from publisher and subscriber JSON files
      # Comments are supported

      MediaServer  a025f8e2-c404-4394-b285-a3e84bae0713
      MediaBackup  81ce0c01-547c-40c2-bf3b-19789d85d747
  ```
    
## Hint Processing Modes
        
  1. Local mode - Processes hint files locally only. Enabled by specifying a
     keys file and publisher library files but not a subscriber file.
  2. Publish mode - Processes hints publisher-to-subscriber.
    
## Local-only Hint Processing
    
  Hints are generally "For" the back-up systems(s). However the media server
  where manual changes are made may also execute hints locally (only), as
  opposed to something being done manually then a hint created for that. In
  other words a hint may be created then executed instead of doing the action
  by hand.
    
  Because targets are required for hint processing a special format command
  line is used to execute hints locally (only).
        
  1. The publisher's targets file is used instead of a subscriber's.
  2. No subscriber file is specified.
  3. All other options related to hint processing are the same.

## Hint Files
    
  A hint file is a simple text file with a .els extension. It contains
  status lines for each system involved and commands.
    
  Example:
    
  ```
      # Move the Cosmos TV show to TV documentaries
      # Comments are supported

      For MediaServer
      For MediaBackup

      rm "TV Shows|Cosmos (1980)/cover.jpg"        
      mv 'Cosmos (1980)' "TV Documentaries | Cosmos (1980)"
  ```
        
  1. Two commands are currently supported:
     1. rm : remove a file or directory.
     2. mv : move *or rename* a file or directory.
  2. Paths may include an optional library name followed by a pipe | character.
     1. Spaces or tabs around the pipe character are supported.
  3. Matching single- or double-quotes are required around paths.
  4. Moving a file to a directory requires a full path, e.g.
     mv "cover.jpg" "CoverArt/cover.jpg"
    
## How Hints Work
    
  Hint files are processed on a back-up (subscriber) before a back-up operation
  so the back-up reflects manual changes made to the media server (publisher).
    
  When each system executes a hint that system's status is changed from "For"
  to "Done". When all systems have Done the hint their status is changed to
  "Seen". When all systems have Seen the hint the hint .els file is deleted.
    
  **Important Note**: In the current implementation if more than one back-up
  is used there is an "odd man out" issue where the next to last system will
  have an orphaned hint .els file that is not deleted. This is due to a basic
  logistical logic "Catch-22" that will be addressed in a later version.
    
## Other Notes
        
  * Option -D | --dry-run applies.
        
    When using the --dry-run option with hints in a back-up run:
      1. The backup results may be wrong because the hints were not actually performed
         on the subscriber.
      2. Because hints are copied and processed immediately publisher hint files are
         only validated during a --dry-run.
        
  * Option -x | --cross-check applies..
        
  * Filenames in .els hint files are relative to the directory containing the .els file.
        
  * Changes made with ELS hints will trigger a rescan of the affected libraries.
