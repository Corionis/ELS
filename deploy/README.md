# Directory deploy

This directory contains the latest build of ELS and some files
required during development.

The distributable files are:

 * ELS.jar - The ELS software, _it's all built-in_.
 * ELS for Windows - An archive and batch file for Windows including a built-in JRE.
 * ELS for Linux - An archive and script for Linux & Mac including a built-in JRE.
   * ELS has not been tested on Apple systems although provisions have
     been made in the code for Apple Macintosh macOS. Do not know if the
     embedded JRE for Linux will run on a Mac where a separate distributable
     file may (likely) be required. Input requested if interested.

## Installation

Unpack the Linux or Windows archive in the desired location.

Execute the script or batch file to start a basic Navigator desktop
application with no publisher or subscriber.

## Upgrading

ELS 4 uses a specific directory structure for libraries, tools, jobs, etc.

For users of ELS _prior to 4.0.0_: 
1. Ensure any Hints in-process are completed.
2. Copy any previous library JSON files to the ELS 
   "[current working directory]/libraries/" AFTER running it the first 
   time, default current working directory is "[user home directory]/.els"
   but may be defined using the -C option (case-sensitive).
