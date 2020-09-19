# ELS : Entertainment Library Synchronizer

This directory contains an example set of JSON files that describe
the entertainment libraries on the media server system and it's
back-up and example Windows batch and Linux bash scripts to help
users get started with ELS.

## Download Contents

 * ELS.jar : The self-contained ELS application software.
 * linux subdirectory : Example Linux bash scripts.
 * meta subdirectory : Example publisher and subscriber library files and targets file.
 * windows subdirectory : Example Windows batch script.

ELS has many options that may be used in different combinations. Only a
few are shown in these example files.

## Installation

1. Edit the files in the meta directory for your publisher and subscriber and subscriber targets.
   a. Be sure to change the key UUID.
2. Edit the appropriate scripts.
   a. Be sure to change all --auth password parameters.
3. Run the appropriate dryrun or backup script for Windows or Linux (Apple).
4. On Linux (Apple) make the scripts executable: chmod 750 *.sh
5. Explore the other options and possibilities!

More information is available at:  https://github.com/GrokSoft/ELS/wiki
