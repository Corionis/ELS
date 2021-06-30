Listed here are changes to ELS versions from 3.0.0 onward.

# Version 3.0.0

## Bug Fixes and Enhancements

 1. Issue #16 *'Add more granular control of target minimum free space'*.
    
    Added a 'locations' clause to the library/collection files that specifies minimum
    available storage space for individual devices, as opposed to the library level
    that is defined in targets.json. This clause is optional.
    
    If targets.json is specified on the command line that value is used. If the targets
    file or the particular library is not specified the value from the locations
    in the subscriber file is used for the matching device. If that is not specified
    the default of 100 GB is used. Linux and Windows location values are supported.
    
    The syntax of the locations and library sources must match for string comparisons.
    
    Example:
    ```
        "locations": [
            {
                "location": "/home/plex/Plex/Media/Plex01",
                "minimum": "42GB"
            },
            {
                "location": "/home/plex/Plex/Media/Plex02",
                "minimum": "10GB"
            }
        ]
    ```

 2. Issue #22 *'Add option to use subscriber sources as targets'*.

    Changed the behavior of -t | --targets and -T | --force-targets to allow the
    targets.json to not be specified. The option alone enables a back-up run
    unless the --dry-run option is specified.
    
    When no targets.json is defined the sources from the subscriber.json file are
    used instead. When combined with the new 'locations' clause this allows all
    sources to be used as target locations making targets.json unnecessary. But there
    are situations where a targets.json file is desirable and is still fully supported.
    
    If targets.json is defined those locations and minimums override subscriber.json
    sources IF the particular library is defined within the targets.json
    file. Otherwise the sources for that library will be used.
    
    This applies to local and remote sessions. For a remote session if the 
    subscriber's targets are either requested by the publisher or the subscriber
    forces it to the publisher this behavior is consistent. If the subscriber
    only specifies the option, or the particular library is not defined in the 
    subscriber's targets.json, then its library/collection sources will be 
    used by the publisher.    

    If the subscriber's **collection** is either requested by the publisher or the
    subscriber forces it to the publisher those locations and sources are used when
    a library's sources are used for the targets.

    All previous files and behavior are supported. A separate targets.json
    file may now contain just the libraries and particular target locations needing
    to be managed. Or the details for an entire collection. But much of the information
    is also in the subscriber.json file.
    
 3. Issue #21 *'Catch exceptions better'*

    Adjusted the exception handling to hopefully catch common problems for both local
    and remote sessions. Makes detection of the 'Process completed normally' line
    in the log file, that marks a 'successful' run, more reliable for automation purposes.
    
    A reminder: Although it's a little weird a FATAL log level is used for this
    log line so it is always written to the log file regardless of the command line
    log level argument. 

 4. Issue #23 *'Add -L | --exclude option to exclude certain libraries'*.

    Like the -l | --library option the new exclude option may be specified multiple
    times. Each library is excluded from processing. Useful when one or two
    libraries are to be skipped in a collection with many libraries.
    
    A change from version 2.2.0 is the -l | --library and -L | --exclude options
    may also be specified on the subscriber-side to limit which libraries are
    processed. Previously -l | --library only applied to the publisher side.

