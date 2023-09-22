# Directory deploy

This directory contains the latest build of ELS.

## Distributable Files

 | File                 | Description                             |
 |----------------------|-----------------------------------------|
 | ELS.jar              | The ELS software, _it's all built-in_.  |
 | ELS-*.tar.gz         | Linux archive including built-in JRE.   |
 | ELS-*.zip            | Windows archive including built-in JRE. |         

 ## Files Used By ELS Navigator 

_Note_: These files are not intended to be executed manually.

 | File                 | Description                             |
 |----------------------|-----------------------------------------|
 | ELS_Updater.jar      | Updater for existing ELS installations. |
 | ELS_Updater-*.tar.gz | Linux archive including built-in JRE.   |
 | ELS_Updater-*.zip    | Windows archive including built-in JRE. |
 | update.info          | Used to check for updates.              |
 | version.info         | Downloaded for update parameters.       |

### Note

ELS has not been tested on Apple systems although provisions have
been made in the code for Apple macOS.

At the simplest all that is needed to run ELS is ELS.jar and Java.
The rest is for the convenience of the user and recommended for
workstations users, as opposed to developers.

## Installation

Unpack the Linux or Windows archive in the desired location.

Execute the script or batch file in that directory to start a basic Navigator desktop
application with no publisher or subscriber.

## Updating

Using ELS Navigator select Help then Check for Updates. 

ELS Updater is an automated process and not intended to be executed manually.

## For Users Of ELS _Prior to 4.0.0_ :

ELS 4 uses a specific directory structure for libraries, tools, jobs, etc.

1. Ensure any Hints in-process are completed.
2. Copy any previous library JSON files to the ELS 
   "[current working directory]/libraries/" AFTER running it the first 
   time, default current working directory is "[user home directory]/.els"
   but may be defined using the -C option (case-sensitive).
