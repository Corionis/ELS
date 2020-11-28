![ELS logo](https://github.com/GrokSoft/ELS/blob/master/artifacts/images/els-logo-98px.jpg)

# ELS : Entertainment Library Synchronizer

Entertainment Library Synchronizer is a back-up tool for home media systems. ELS
views media spanning multiple hard drives on a logical library basis, such as Movies
or TV, and combines all the content for each library to determine what needs to be
backed-up. The exact location of content within each library does not have to match
on the back-up allowing a media library to grow "organically".

Movies, TV shows with seasons, music and more are handled by ELS. When new content
is added, for example another episode of a TV show, a check is made whether it will
fit in the original location as the other episodes. If it will not fit it is copied
to a matching target location for that library.

ELS relies on a common directory structure used by modern home media systems such
as [Plex](https://plex.tv). Each item must be contained in a unique directory
within a library directory.

For example:

![library directory structure](artifacts/images/library-directory.jpg "Library directory") 

ELS uses two JSON files to describe the bibliographies of one or more libraries
spread across multiple hard drives, one for the media system and the other for the
backup. Another JSON file describes the target location(s) for new content. Multiple
targets may be defined for automatic roll-over when a target reaches a specified
minimum amount of free space the next target will be used.

ELS can run locally with attached storage devices as a single process, or
over a LAN or the Internet using two computers running ELS with built-in communications
options.
 
This software is written in Java and operates on Windows, Linux, and Apple systems. The
media system and back-up do not have to be the same type.

See the **[ELS Wiki](https://github.com/GrokSoft/ELS/wiki)** for features, downloads
 and information.
