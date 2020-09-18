ELS uses two JSON files to describe the bibliographies of one or
more libraries spread across multiple hard drives, one for the source,
or publisher, and the other for the back-up, or the subscriber. Another
JSON file describes the target location(s) for new content.

These files require correct JSON syntax. JSON is a simple text format
to name elements and values.

A "library" file lists the libraries only, a "collection" file also contains
all the individual items in each library. Both are JSON files of the same
format.

ELS will automatically scan the libraries as needed at runtime so a collection file
is not required to run it. However if a collection file is specified, as opposed
to a library file, then only that data is used. This allows for generating a collection
file then hand-adjusting what ELS will use. A collection JSON file may be generated
using the -i or --export-items option and specifying a publisher library file. 

**IMPORTANT**: The JSON data here are example files with // comments. This is *NOT*
valid JSON format and is used here for informational purposes. Don't copy 'n paste
without removing from // to the end of each line. Better is to use the ELS Complete
download file that contains valid example files.

## Library File Structure

For publisher and subscriber library JSON files:
````
{
    "libraries": {                                          // required literal
        "description": "Name or description",               // anything useful 
        "host": "localhost:50271",                          // hostname or IP, colon, port number
        "listen": "localhost:50271",                        // hostname or IP, colon, port number
        "flavor": "windows",                                // apple, linux or windows (only)
        "terminal_allowed": "true",                         // allow interactive access true/false
        "key": "f9bd7a64-f8a7-11ea-adc1-0242ac120002",      // UUID unique to each publisher and subscriber
        "case_sensitive": false,                            // perform case-sensitive comparisons
        "ignore_patterns": [                                // one or more filenames to ignore/skip
            "desktop.ini",                                  //   separated by commas
            "Thumbs.db"
        ],
        "bibliography": [                                   // required literal
            {
                "name": "Movies",                           // library name, much match in publisher and subscriber
                "sources": [                                // required literal
                    "C:/media/MyMovies",                    // absolute or relative path
                    "D:/media/MoreMovies",                  //   paths are relative to location of ELS.jar
                    "E:/MoreNewMovies"
                ]
            },
            {
                "name": "TV Shows",                         // same as Movies
                "sources": [
                    "D:/media/MyTVShows",
                    "E:/MoreTVShows"
                ]
            }
        ]
    }
}
````

## Targets File Structure

For targets JSON file:
````
{
    "targets": {                                            // required literal
        "description": "My Subscriber Targets",             // anything useful
        "storage": [                                        // required literal
            {
                "name": "Movies",                           // library name, much match in subscriber                                                      
                "minimum": "50kb",                          // minimum space in kb, mb, gb, or tb
                "locations": [                              // required literal
                    "D:/media/MoreMovies",                  // absolute or relative path
                    "E:/MoreNewMovies"                      // automatically rolls-over when full
                ]
            },
            {
                "name": "TV Shows",                         // same as Movies
                "minimum": "1GB",
                "locations": [
                    "E:/MoreTVShows"
                ]
            }
        ]
    }
}
````

## UUID Generation
Library JSON files for each publisher and subscriber must have a unique key 
UUID (Universally Unique ID).

There are many tools and ways to generate a version-1 UUID for the "key" element.
this [Online UUID Generator](https://www.uuidgenerator.net/) works well.

## References
 * [json.org](https://www.json.org/json-en.html). 
 * [w3school.com : What is JSON?](https://www.w3schools.com/whatis/whatis_json.asp)

