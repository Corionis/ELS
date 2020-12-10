![ELS logo](https://github.com/GrokSoft/ELS/blob/master/artifacts/images/els-logo-98px.jpg)

# ELS : Entertainment Library Synchronizer

Entertainment Library Synchronizer (ELS) is a back-up tool for home 
media systems. ELS views media spanning multiple hard drives the same 
way modern media systems do - on a logical library basis, such as movies 
or TV shows. ELS combines the content for each library and performs name 
comparisons to determine what needs to be backed-up. The exact location 
of files for each library do not have to match on the back-up allowing 
a media library to grow "organically". 

## Features

 * Supports movies, television shows with season subdirectories, 
   music with artists and albums, etc.
 * Supports any mix of storage devices of different sizes.
 * Optionally copies new files to an existing movie or TV show if space is 
   available (back-fill).
 * Multiple targets may be defined for each library, e.g. movies. As 
   one reaches a minimum available space the next target is used (automatic roll-over).
 * Optionally generates a What's New text file of what items were copied.
 * Optionally generates a Mismatches text file of the detailed differences between the publisher and subscriber.
 * Stand-alone and client/server modes of operation are supported.
 * An interactive terminal is available for both publisher and subscriber.
 * Standard SFTP may interactively connect to ELS when in listener mode.
 * May be scheduled using operating system tools, e.g. Windows Task Scheduler or Linux cron.
 * Nothing is added, no overhead.
 * Runs on Windows, Linux and Mac.

ELS relies on a common directory structure used by modern home media
systems such as [Plex](https://plex.tv). Each item must be contained in
a unique directory within a library directory.

For example:

![library directory structure](artifacts/images/library-directory.jpg "Library directory")

## Wiki Pages

|                                                      |                                                      |
|------------------------------------------------|------------------------------------------------------|
|[Command-Line How-To](Command-Line-How-To)      | Describes the various command-line options.          |
|[Communications How-To](Communications-How-To)  | Describes the use of ELS over a LAN or the Internet. |
|[Developer Notes](Developer-Notes)              | Notes for developers extending or modifying ELS.      |
|[Downloads](Downloads)                          | Requirements and downloads.                          |
|[ELS Plex Generator Utility](ELS-Plex-Generator-Utility)                    | Add-on utility for [Plex Media Server](https://www.plex.tv).                 |
|[JSON Structure](JSON-Structure)                | Structure and definition of library and target JSON  |
|[Modes of Operation](Modes-of-Operation)        | Describes the two different ways to run ELS.         |
|[Regular Expressions](Regular-Expressions)      | Details of supported pattern matching.               |
