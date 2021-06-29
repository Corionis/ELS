Listed here are changes to ELS versions from 3.0.0 onward.

# Version 3.0.0

 1. Issue #16 *'Add more granular control of target minimum free space'*.
    
    Added a 'locations' clause to the library/collection files that specifies minimum
    available storage space for individual devices, as opposed to the library level
    that is defined in targets.json.
    
    If targets.json is specified on the command line that value is used. If the targets
    file or the particular library is not specified the value from the locations
    in the subscriber file is used for the matching device. If that is not specified
    the default of 100 GB is used.
    
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
    are situations where a targets.json file is desirable. 
    
    If targets.json is defined the locations and minimums override subscriber.json
    sources and locations IF the particular library is defined within the targets.json
    file. Otherwise the sources for that library will be used.
    
    This applies to local and remote sessions. If the subscriber's targets are either
    requested by the publisher or the subscriber forces it to the publisher this
    behavior is consistent. If the subscriber only specifies the option, or the
    particular library is not defined in the subscriber's targets.json, then its
    library/collection sources will be used by the publisher.    

    If the subscriber's **collection** is either requested by the publisher or the
    subscriber forces it to the publisher those locations and sources are used.

    All previous files and behavior are supported. A separate targets.json
    file may now contain just the libraries and particular target locations needing
    to be managed. Or the details for an entire collection. But much of the information
    is also in the subscriber.json file.

