![ELS logo](https://github.com/GrokSoft/ELS/blob/master/artifacts/images/els-logo-98px.jpg)

# ELS : Entertainment Library Synchronizer
Entertainment Library Synchronizer is a back-up tool for home media systems. ELS
views media spanning multiple hard drives on a logical library basis, such as Movies
or TV, and combines all the content for each library to determine what needs to be
backed-up. The exact location of content within each library does not have to match
on the back-up allowing a media library to grow "organically".

## Features
 * Handles movies, television shows with season subdirectories, music with artists and albums, etc.
 * Supports any mix of storage devices of different sizes.
 * Will copy new files to an existing movie or TV show if space is available.
 * Multiple targets may be defined for a given library, e.g. Movies. As one reaches the defined minimum
   available space the next target will be used (automatic roll-over).
 * Optionally generates a What's New text file of what items were copied.
 * Optionally generates a Mismatches text file of the detailed differences between the publisher and subscriber.
 * Stand-alone and client/server modes of operation are supported.
 * May be scheduled using the operating system's tool, e.g. Windows Task Scheduler or Linux cron.
 * An interactive terminal is available for both publisher and subscriber.
 * Standard SFTP may interactively connect to ELS when in listener mode.
 * Nothing is added, e.g. no files
 * Runs on Windows, Linux and Mac.

|Wiki Page                                       |                                                      |
|------------------------------------------------|------------------------------------------------------|
|[Command-Line How-To](Command-Line-How-To)      | Describes the various command-line options.          |
|[Communications How-To](Communications-How-To)  | Describes the use of ELS over a LAN or the Internet. |
|[Developer Notes](Developer-Notes)              | Notes for developers extended or modifying ELS.      |
|[Downloads](Downloads)                          | Requirements and downloads.                          |
|[ELS Plex Generator Utility](ELS-Plex-Generator-Utility)                    | Add-on utility for [Plex Media Server](https://www.plex.tv).                 |
|[JSON Structure](JSON-Structure)                | Structure and definition of library and target JSON  |
|[Modes of Operation](Modes-of-Operation)        | Describes the two different ways to run ELS.         |
