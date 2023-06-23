ELS is written for Java version 1.8 to support older systems that might be used as back-ups.
The resulting jar is completely self-contained. ELS runs on any version 1.8 or newer of 
either Oracle Java or the OpenJDK.

ELS is developed using [JetBrains Intellij IDEA](https://www.jetbrains.com/idea/). Eclipse
users can adapt the project easily.

Designed using:
  - JFormDesigner, https://www.formdev.com/jformdesigner/doc/

With open source components from FormDev:
  - FlatLaf look 'n feel, https://www.formdev.com/flatlaf/
  - https://github.com/JFormDesigner/FlatLaf
  - Download from: https://search.maven.org/artifact/com.formdev/flatlaf

See also:
 - https://github.com/JFormDesigner/FlatLaf/tree/main/flatlaf-extras
 - https://github.com/JFormDesigner/svgSalamander

In the interest of readability and extensibility some portions of ELS are written-out in "long hand" code.
Heavy abstration and terse "denso-code" have been avoided, while other areas are
more object-oriented. To that end many data elements do not have one or more layers
of getters and setters. Instead the longer, direct, more descriptive and readable, 
approach has been used (right or wrong).

The SFTP client is implemented using [Jsch : Java Secure Channel](http://www.jcraft.com/jsch/).

The SFTP server implementation utilizes [Apache MINA Project](https://mina.apache.org/sshd-project/)
technology.

The STTY implementation is entirely custom. The command set available is
easily extensible. Commands are used by both automated and interactive (-r M and -r T) sessions
in a request/response REST style. Interactive sessions display a built-in terminal developed
in Java Swing. The Look 'n Feel of the terminal depends on the operating system. The default LnF
supported by the installed Java VM is used by default. The LnF used may be changed, see the
Communications How-To for details.

All remote communication is encrypted. Remote being not on the local computer where two
systems running ELS are being used.

The mock directory contains pre-set publisher and subscriber collections and hint files
to support testing and provide a completely self-contained development and test environment.
The mock/scripts/ directory has many scripts to perform application-level tests.

These scripts show many of the various ways ELS may be executed using different combinations
of options. See the **README** in that directory for more information and a description of 
the testing sequence.

For IntelliJ users several run/debug configurations have been added that match the scripts in
the mock/scripts/ directory organized in the same way and use the same mock/ data.

## Code Disclaimer

Code in the newest branch and not ```master``` is work in progress. _It may not work_ or make
sense while features are being implemented. Commits to branches in this repository are
for back-up purposes. That said commits *usually* do not break the build.

## Jump Start / Demo

### Using the Linux or Windows distribution files:

To run the ELS Navigator with a new (empty) or existing ELS configuration:

1. Goto the directory where you unpacked the downloaded archive.
2. Run ```ELS-Navigator``` .sh or .bat as appropriate for your O/S.

### Using a clone of the ELS GitHub repository:

To run the ELS Navigator GUI with the built-in test environment, assuming
deploy/ELS.jar exists:

1. Goto mock/scripts/<your O/S, linux/mac or windows>/
2. Run reset.bat or reset.sh  that creates a mock/test environment
3. Run ```80-10-Navigator-local.bat``` or ```80-10-Navigator-local.sh```

## Building

To build ELS from sources use Apache ant in the root of the project directory:

 1. ```ant -f els.xml``` Build the ELS default target 'all' for Linux and Windows
 2. ```ant -f els.xml stamp``` Sets the build stamp and copies the default locale
 3. ```ant -f els.xml jar``` Build ELS.jar 
 4. ```ant -f els.xml linux``` Build ELS .tar.gz for Linux
 5. ```ant -f els.xml windows``` Build ELS .zip for Windows 

In IntelliJ most/all Run configurations execute the els.xml _stamp_ ant target.
Also, previous IntelliJ build configurations for Linux and Windows have
been removed in favor of the ant build script. The ant build script 
is cross-platform and builds all distributable files.

## Developer Tips

 1. Various places in the code have several distinct sections, e.g. case statements and
    initialization segments. In many of those places each "section" may be found by
    searching for:  `````// ---`````
    <br/><br/>
 2. Communications time-out handling and the many changes those areas have gone through
   made finding the code locations more important. To find the relevant code locations 
   search for:  ```time-out```
   <br/><br/>
 3. The locations that handle the internal heartbeat for remote connections can be found
   by searching the code for: ```ping```
    <br/><br/>
 4. IntelliJ can be configured with additional To Do markers. In addition to those included
    in the IDE the ELS project also uses: 
      * QUESTION case-sensitive
      * IDEA case-sensitive
      * ISSUE case-sensitive
      * LEFTOFF case-sensitive, where we stopped for the nightdaynight
      * TEST case-sensitive
