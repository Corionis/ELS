![ELS logo](https://github.com/GrokSoft/ELS/blob/master/artifacts/images/els-logo-98px.png)

# ELS : Entertainment Library Synchronizer

Entertainment Library Synchronizer (ELS) is a backup tool for home
media systems. ELS views media spanning multiple hard drives the same
way modern media systems do - on a logical library basis such as movies
or TV shows. ELS combines the content of each library to determine what
needs to be backed-up. The exact location of files in each library do not
have to match on the back-up allowing a media library to grow "organically".

Movies, TV shows with seasons, music and more are handled by ELS. When
new content is added, for example another episode of a TV show, a check
is made whether it will fit in the original location as other episodes.
If it will not fit it is copied to a matching target location for new
content for that library.

The version 3.1.0 pre-built executable and a Zip including examples are available
on the **[ELS Wiki Downloads](https://github.com/GrokSoft/ELS/wiki/Downloads)** page.

See the **[ELS Wiki](https://github.com/GrokSoft/ELS/wiki)** for version 3.1.0
features, downloads and documentation.

ELS version 4.0 has been in development since September of 2021. 4.0 is a large
project that adds a powerful desktop application called the ELS Navigator. The
new version is not complete however large portions are done and quite useful.
See **[ELS Version 4.0.0](https://github.com/GrokSoft/ELS/tree/Version-4.0.0)**.

## Features

 * New ELS Hint Status Tracker to coordinate local hint status, new in 3.1.0.
 * New ELS Hint Status Server to corrdinate remote hint status, new in 3.1.0.
 * New ELS Hints to coordinate manual changes, new in 3.0.0.

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
 * Standard SFTP such as [Filezilla](https://filezilla-project.org/) may interactively connect to ELS when in listener mode.
 * May be scheduled using operating system tools, e.g. Windows Task Scheduler or Linux cron.
 * Nothing is added, no overhead except when using hints.
 * Runs on Windows, Linux and Mac.

ELS relies on a common directory structure used by modern home media
systems such as [Plex Media Server](https://plex.tv). Each media type,
such as a movie or television show, is contained in a unique directory
within a library directory.

For example:

![library directory structure](artifacts/images/library-directory.jpg "Library directory")

ELS uses two JSON files to describe the bibliographies of one or more
libraries spread across multiple hard drives, one for the media system
and the other for the backup.

Another JSON file describes the target location(s) for new content. Each
library may have multiple targets for automatic roll-over. When a target
reaches a specified minimum amount of free space the next target is
used.

An add-on tool is available to generate a basic ELS JSON file from a
[Plex Media Server](https://www.plex.tv), see the [ELS Plex
Generator](https://github.com/GrokSoft/ELS-Plex-Generator). However ELS
will support any modern media system that uses the same directory structure.

ELS can run locally with attached storage devices as a single process or
over a LAN or the Internet using two computers running ELS with built-in
communications options.

This software is written in Java and operates on Windows, Linux, and
Apple systems. The media system and back-up do not have to be the same
type.

See the **[ELS Wiki](https://github.com/GrokSoft/ELS/wiki)** for
features, downloads and documentation.
