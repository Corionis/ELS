Entertainment Library Synchronizer is distributed as a single Java jar file, either complete with example JSON files and scripts or just the software.

# Requirements

* Media directory structure as described in these pages.
* Supported operating systems:
    * Windows
    * Linux
    * Apple Macintosh
* Java Runtime Environment (JRE) version 1.8 or greater. Both [Oracle Java](https://www.oracle.com/java/technologies/javase-jre8-downloads.html)
  and [OpenJDK](https://openjdk.java.net/) are supported.
* Memory requirements depend on the size of the media library
    * For 50,000 files about 1.5-2.0 GB
* Two computers are required to use remote communications with the -r option.
    * The two computers do not need to be running the same operating system.

# Disclaimer

Any software downloaded from this site is free and without warranty or guarantee of any kind expressed or implied. The software may not work or serve any intended or particular purpose. Use at your own risk. If you break it you own both parts.

# Privacy

ELS does not collect or send any data of any kind to anyone. It is a tool under your control and nothing more. Review the code.

# Downloads
The current version of ELS is: **3.1.0.** See [Release Notes](release-notes.md) for details.

* Latest build, software only: [ELS.jar](../blob/master/deploy/ELS.jar?raw=true)
* Latest build, with examples: [ELS-complete.zip](../blob/master/deploy/ELS-complete.zip?raw=true)

NOTE: ELS has been tested on Windows and Linux, but has not been tested on Apple. Efforts have been
made in the code to accommodate Apple Macintosh. Testing and feedback are requested. *__BE WARNED:__ it
may not work.*

# Installation
We recommend starting with the ELS-complete.zip. These instructions are based on that.

1. Download [ELS-complete.zip](../blob/master/deploy/ELS-complete.zip?raw=true).
2. Unpack ELS-complete.zip into the chosen directory.
    1. An "els" directory will be created from the zip.
3. Edit the files in the meta directory for your publisher, subscriber and targets.
    1. Be sure to change the key UUID.
    2. Describe each library and its sources.
    3. See the [JSON Structure](JSON-Structure) wiki page for details.
4. Edit the listener scripts.
    1. Change the *password* value of the "--authorize" parameter used by the built-in interactive terminal (-r M or -r T).
        1. The --authorize value is used interactively with the "auth" command to enable access to authorized commands.
5. On Linux (Apple) make the .sh bash scripts executable:  ```chmod 750 *.sh```
6. For a remote connection copy the entire ELS directory to the subscriber computer.
    1. Publisher-side runs publisher scripts.
    2. Subscriber-side runs subscriber scripts.
7. Run the appropriate validate, dryrun or backup script for Windows or Linux (Apple).
8. Explore the other options and possibilities!

An add-on tool is available to generate a basic ELS JSON file from a
[Plex Media Server](https://www.plex.tv), see the [ELS Plex
Generator](https://github.com/GrokSoft/ELS-Plex-Generator). ELS
supports any modern media system that uses the same directory structure.

ELS has many options that may be used in different combinations. Only a
few are shown in the examples.
