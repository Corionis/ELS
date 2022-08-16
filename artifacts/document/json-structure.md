ELS uses two JSON files to describe the bibliographies of one or more 
libraries spread across storage devices, one for the publisher and 
the other for the subscriber, or back-up. Another JSON file describes 
the target location(s) for new content.

The optional Hint Status Server JSON file is identical and used to define
the communications parameters, if run as the Hint Status Server, and the
storage location for the Hint Tracker datastore.

These files require correct JSON syntax. JSON is a simple text format
to name keyword/value pairs.

A ```"library"``` file lists the libraries only, a ```"collection"``` file also contains
all the individual items in each library. Both are JSON files of the same
format.

ELS will automatically scan the libraries as needed at runtime so a collection file
is not required to run it. However if a collection file is specified, as opposed
to a library file, then only that data is used. This allows for generating a collection
file then hand-adjusting what ELS will use. A collection JSON file may be generated
using the -i or --export-items option and specifying a publisher library file as input. 

**IMPORTANT**: The JSON data here are example files with // comments. This is ***NOT***
valid JSON format and is used here for informational purposes. Do not copy 'n paste
without removing from // to the end of each line. Better to use the ELS Complete
download file that contains valid example files.

## Library File Structure

For publisher and subscriber library JSON files:
````
{
    "libraries": {                                      // required literal
        "description": "Name or description",           // anything useful 
        "host": "localhost:50271",                      // hostname or IP, colon, port number
        "listen": "localhost:50271",                    // hostname or IP, colon, port number
        "flavor": "windows",                            // apple, linux or windows (only)
        "terminal_allowed": "true",                     // allow interactive access true/false
        "key": "f9bd7a64-f8a7-11ea-adc1-0242ac120002",  // UUID unique to each publisher and subscriber
        "case_sensitive": false,                        // perform case-sensitive comparisons
        "temp_dated": "false",                          // if temp filenames have dates embedded
        "temp_location": "",                            // where to place temp files, e.g. received collection files
        "ignore_patterns": [                            // one or more filenames to ignore/skip,
            "(?i)desktop\\.ini",                        //   separated by commas
            ".*\\/Plex Versions.*",                     //   regular expressions supported
            "Thumbs\\.db"
        ],
        "locations": [
            {
                "location": "test/publisher/media",
                "minimum": "42GB"
            },
            {
                "location": "/mnt/plex/nas",
                "minimum": "20GB"
            },
        ],
        "bibliography": [                               // required literal
            {
                "name": "Movies",                       // library name
                "sources": [                            // required literal
                    "C:/media/MyMovies",                // absolute or relative path,
                    "D:/media/MoreMovies",              //   paths are relative to the location of ELS.jar
                    "E:/MoreNewMovies"
                ]
            },
            {
                "name": "TV Shows",                     // same as Movies
                "sources": [
                    "D:/media/MyTVShows",
                    "E:/MoreTVShows"
                ]
            }
        ]
    }
}
````

### Library Element Notes

 1. The host element is only used with the -r or --remote option.
 2. If a :port number is not specified 50271 is used as the BASE port number.
 3. The listen element is optional. It is useful for NAT/port forwarding. If not specified the host is used.
 4. The flavor element may only be: apple, linux, or windows.
    1. Flavor is defined instead of being "discovered" so it may be forced.
 5. The terminal_allowed can disable interactive access. However, a complex automatic handshake is done so it is relatively safe.
 6. The key element ***must be unique*** for each publisher and subscriber. It is a version-1 UUID, see below.
 7. The case_sensitive element controls the type of comparison that is done between publisher and subscriber content.
 8. ignore_patterns section is optional.
    1. ignore_patterns support regular expressions, see [Regular Expressions](Regular-Expressions).
    2. If the pattern contains the path separator literal for that repository the full path is matched.
    3. If the pattern does not contain the path separator literal only the right-end directory or file name is matched.
 9. locations are matched against library sources to get the desired minimum disk free space. However if a -t | -T targets
    file is specified it overrides values in locations, new in version 3.0.0.
 10. Any number of libraries may be added to the bibliography.
 11. Library names must match between publisher, subscriber and targets.
 12. Paths may be absolute, e.g. C:\Media\Movies or relative, e.g. ..\Media\Movies\
    1. Paths are relative to the location of ELS.jar.

## Targets File Structure

For targets JSON file:
````
{
    "targets": {                                        // required literal
        "description": "My Subscriber Targets",         // anything useful
        "storage": [                                    // required literal
            {
                "name": "Movies",                       // library name
                "minimum": "50mb",                      // minimum space in kb, mb, gb, or tb
                "locations": [                          // required literal
                    "D:/media/MoreMovies",              // absolute or relative path
                    "E:/MoreNewMovies"                  // automatically rolls-over when full
                ]
            },
            {
                "name": "TV Shows",                     // same as Movies
                "minimum": "1GB",
                "locations": [
                    "E:/MoreTVShows"
                ]
            }
        ]
    }
}
````

### Targets Elements Notes

 1. Library names must match between publisher, subscriber and targets.
 2. If the minimum element is not specified it defaults to 1 GB.
 3. There may be any number of locations for a library, separated by commas.
 4. Targets are processed in order. When the first is "full" based on the minimum
    the next location is used.
 5. **BE SURE** to include the target locations as sources in the library files otherwise
    the content there will not be found and items will be re-copied.

## UUID Generation

Library JSON files for each publisher and subscriber must have a unique key 
UUID (Universally Unique ID).

There are many tools and ways to generate a version-1 UUID for the "key" element.
this [Online UUID Generator](https://www.uuidgenerator.net/) works well.

## References

 * [json.org](https://www.json.org/json-en.html). 
 * [w3school.com : What is JSON?](https://www.w3schools.com/whatis/whatis_json.asp)
