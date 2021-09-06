The Hint Status Tracker and Hint Status Server are new features beginning with ELS 3.1.0.

ELS Hints track the status of completion for each back-up inside the .els hint file.
When using a single media back-up hints work well. However when using multiple
back-ups coordination between them is needed. This allows any back-up to perform
operations with any other back-up or the media server and keep it all straight, and
avoid the "odd man out" problem.

ELS 3.1.0 and later adds the optional Hint Status Tracker to coordinate hint status
between the media server and multiple back-ups. The tracker may be used locally or
the Hint Status Server may be run to provide the needed functionality when using
the -r | --remote option for ELS runs.

ELS Hints are optional. The Hint Tracker is optional when using hints. But the ELS
Hint Status Server (HSS) is **required** when using hints and hint tracking with the
-r | --remote option. The Hint Status Server is the remote variant of the Hint
Tracker and is run as a separate stand-alone process.

 * Hints are enabled with the -k | -K option, see [Hints](Hints).
 * Hint tracking is enabled with the -h option.
 * The Hint Status Server is run with the -H option.

The -h and -H option use the same JSON file. -h connects to the Hint
Status Server running with -H when using the -r | --remote option for ELS runs.

## Hint Tracker/Status Server JSON

This JSON file is used to define:

 1. The communications parameters if run as the Hint Status Server.

 2. The storage location for the Hint Tracker datastore.

### Notes

 * The file is the same format as publisher and subscriber JSON files. The communications
   parameters are the same as described in [Communications](communications).

 * Things like terminal_allowed, ignore_patterns, renaming are ignored and not needed.
   * A terminal to the Hint Status Server is never allowed.

 * The datastore location is taken from the first source of the first library
   in the bibliography. Only the first source is used. The library name can be any
   valid text.

 * For remote sessions the ELS Hint Keys file, introduced with 3.0.0, is used for
   authentication.
