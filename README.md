![ELS logo](https://github.com/Corionis/ELS/blob/master/artifacts/images/els-logo-98px.png)

# ELS : Entertainment Library Synchronizer

_**Beta Release**_

_Note_: ELS is 10 years old. The desktop application, Navigator, is new after 3.5 years of
work. Comments, bugs, and requests are invited in Discussions or Issues. Language 
translation contributors are requested.

Entertainment Library Synchronizer (ELS) Version 4 is a purpose-built library
and title oriented tool for managing and backing-up data with expandable storage
spanning multiple devices.

Any project with large amounts of data for home videos, YouTube, TikTok, Instagram, 
game development, 3D modeling, science, or media systems such as Plex and Jellyfin,
organizing and managing your work is important. And backing it up is critical.

<img src="https://corionis.github.io/ELS/assets/images/media-server-01.png" border="0"/>

**Downloads**: **[ELS user site](https://corionis.github.io/ELS/)** on GitHub.

**Documentation**: **[ELS Wiki](https://github.com/Corionis/ELS/wiki)**.

**Community**: **[ELS Discussions](https://github.com/Corionis/ELS/discussions)**.

## Features

ELS was started in 2015 as a command line-only tool for home media systems. 
Version 4.0 has been in development since September of 2021. It is a large
project that adds a powerful desktop application - ELS Navigator.

*   Modes (where the Navigator is running)
    *   On a data collection where Hints are tracked
    *   On a separate workstation where Hints are not tracked
*   Browser
    *   Split-pane Publisher/Subscriber view
    *   Collection and System tabs for each
    *   Local or remote subscriber
    *   Drag 'n Drop and Copy, Cut, Paste
    *   Automatic Hint Tracking
        *   Renames
        *   Moves
        *   Deletes
    *   Multiple named tool configurations
        *   Duplicate Finder
        *   Empty Directory Finder
        *   Junk Remover
        *   Operations
        *   Renamer
        *   Sleep
    *   Named jobs of sequenced tools to automate repetitive tasks
*   Back-Up
    *   Create named ELS back-up Jobs with different configurations
    *   Execute and monitor back-up runs
    *   Generate scripts for command line and/or scheduled background execution
*   Libraries
    *   Create and edit ELS Publisher, Subscriber, and Hint Server JSON files
    *   Create and edit ELS Authentication keys, Hint keys, blacklist and whitelist

Like the rest of ELS the Navigator is a general tool for anyone manipulating large
volumes of data across multiple storage devices. Also compatible with modern
media systems such a Plex Media Server and Jellyfin. Runs on Linux, Mac and Windows.

_It's all built-in_.

## Ways to Execute ELS

ELS is a multi-faceted application that may be executed in a variety of ways.
Workstations and Collections (back-ups) may be local with all storage devices
attached to one system, or remote over a LAN or the Internet with two systems
running ELS.

### From Navigator

The provided start-up applications for each supported operating system execute
the ELS Navigator desktop application with the provided arguments. If no arguments
are specified Navigator will use the previous publisher, subscriber, etc. and
default options.

Inside Navigator there are two execution tools:

*   File, Generate will generate a command line for the current Navigator
    configuration including loaded publisher, subscriber, etc. That command
    line may be copied to the clipboard or used to create a desktop shortcut.


*   Jobs, Manage has two options:
    <br/><br/>
    1. Run ... that will execute the selected Job inside the current Navigator.
       <br/><br/>
    2.  Generate / Run ... will generate a command line for the current Job 
        and offers immediate execution in the foreground or background with
        its Run button outside of the current Navigator. The command line
        may be copied to the clipboard or used create a desktop shortcut.

### From Command Line

ELS may also be executed from the command line two ways:

*   Using either short or long options to define all the arguments needed for an Operation.


*   Defining a Job inside Navigator then executing it from the command line.

### Note for Linux and macOS Users

When executing ELS with cron or a task scheduler set the -c | --console-level to Off so it only logs to the -d | --debug-level log file and not stdout to avoid cron error messages.


## Add-On Tools

An add-on tool is available to generate a basic ELS JSON file from a
[Plex Media Server](https://www.plex.tv), see the [ELS Plex
Generator](https://github.com/Corionis/ELS-Plex-Generator).

<br/><br/>
