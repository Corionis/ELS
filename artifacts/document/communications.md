This document discusses using the -r | --remote mode of operation.

This approach may be used to back-up a media library over a LAN or
the Internet using two computers both running ELS. All the necessary
communications software is built-in. All data traffic is encrypted.

Communication follows the publisher/subscriber paradigm. The publisher
sends content to the subscriber. In this scenario the subscriber is the
server-side and the publisher is the client-side. Therefore the
subscriber listener must be up and running before the publisher-side is
started.

## Parameter Handling

### Authentication and Authorization

The publisher and subscriber perform a complex automated "handshake"
when a new connection is initiated so no manual "login" is required.

However, when using the built-in manual terminal to access more 
sensitive commands authorization is required. The interactive command:
```auth [password]``` is used where the password is the one specified by the 
listener with the -a | --authorize parameter.

### Remote Modes

```
  -r P = remote-publish     // automated back-up process for -r S
  -r L = pub-listener       // publish listener for -r T
  -r M = pub-manual         // publisher stty manual terminal for -r S

  -r S = sub-listener       // subscriber listener for -r P|M
  -r T = sub-terminal       // subscriber stty manual terminal for -r L

  The mode letter is case-insensitive.
```

### Behavior Changes With -r P|S Option

 1. For the Publisher (-r P):
    1. -p causes the publisher to scan for the latest media data
    2. -P the publisher will import a local collection file
    3. -s causes the publisher to import a local subscriber file
       for site-related data then request the subscriber's media data
    4. -S the publisher will import a local subscriber collection file
    5. -t causes the publisher to request the subscriber's targets
    6. -T the publisher will import a local subscriber targets file

 2. For the Subscriber (-r S):
    1. -p and -P are equivalent; only site-related data is used
    2. -s causes the subscriber to scan for the latest media data
    3. -S the subscriber will import a local collection file
       and will FORCE the publisher to take that media data
       when a connection is made
    4. -t the subscriber will import a local subscriber targets file
    5. -T causes the subscriber to FORCE the publisher to take the 
       subscriber's targets file

## Configuration

Some items are required in the JSON library file to support 
communications. Some are optional. 

 1. host : The hostname:port to connect to for outgoing connections,
    e.g. mybox.home.local:29900. If a port is not specified 50271 is
    used as the BASE port number. An IP address may be used instead
    of a hostname.    
 2. listen : Optional. If specified the hostname:port to listen for
    incoming connections as a listener, e.g. mybox.home.local:30000.
    If a port is not specified 50271 is used as the BASE port number.
    An IP address may be used instead of a hostname. If not specified
    the "host" value will be used.
 3. flavor : The flavor of operating system: windows, linux or apple (only)
 4. terminal_allowed : true or false (only). If an interactive manual
    terminal connection is allowed
 5. key : A formatted universally unique ID (UUID), e.g. 
    "025e2ddb-942a-4206-8458-902a87e42e62". The key for each publisher
    and subscriber must be unique.

Caution: Be sure DNS resolves a hostname correctly to ensure the correct
IP address is used for both outgoing connections and incoming listeners.
For example, be sure a listener is not listening on localhost 127.0.0.1
when a LAN or Internet connection is desired - the connection will fail.

### Port Numbers and Servers

Two servers are started for each session, one for commands using STTY
and one for transferring files using SFTP. For -r L + -r T a second set
of servers is started on the subscriber end.

The port numbers are sequential starting with the port specified in the
"host" and "listen" parameters. For example if the site is mybox.home.com:30000 that
port is used for STTY and 30001 is used for SFTP.

If it is a -r L + -r T publisher/subscriber session then the extra
STTY is 30002 and its SFTP on 30003.

### Network Address Translation (NAT)

The optional "listen" parameter supports using NAT or port forwarding
from an Internet router or gateway to a computer on its LAN.

The "host" parameter would be defined as the outside Internet IP address
or DNS name that routes incoming requests from a publisher to the computer
defined by the "listen" parameters where ELS is running -r L or -r S.

If the "listen" parameter is not defined "host" is used for the listener
IP address and port.

### Firewall Rules

ELS uses two TCP ports, one each for STTY and SFTP. The STTY port
is the port defined in the parameters, the SFTP port is that +1.
All port numbers must be > 1024, and > 20000 is recommended. The
maximum port number is 65533.

## Manual Terminal

### Interactive Sessions

The publisher (-p M) and subscriber (-p T) command-line options have
commands for interacting with the ELS STTY server manually using the
built-in terminal.

### Commands

Manual terminal commands:

 1. auth [password] : Access authorized commands
 2. collection : Get collection data from remote
 3. space [location] : Get the free space of location on remote
 4. targets : Get targets file from remote
 5. help
 6. logout : Exit current authorized level
 7. bye, exit, quit : Disconnect and inform the subscriber listener to exit

Manual terminal to subscriber (-r S and -r M) "authorized" commands:
 1. status = server and console status information
 2. (more on the way)

Manual terminal to publisher (-r L and -r T) "authorized" commands:
 1. status : Display server and console status information
 2. find [text] : Search collection for all matching text, use the
 'collection' command to refresh the data
 3. get [text] : Like find but offers the option to copy the
 listed items in overwrite mode

### Remote Desktop (RDP) Access to Linux Systems

If you are accessing a Linux media system with RDP there have been 
issues with the terminal GUI displaying correctly on some systems. This 
is due to the RDP color depth and it not caused by Java or ELS. 

Solution: In RDP go to the Options, then the Display tab. Change the color
depth to 16-bit instead of 32-bit.

### Look 'n Feel

The Java Look 'n Feel used by default depends on the operating system
Java is installed on, Windows, Linux or Macintosh.

The default Look 'n Feel can be overridden by adding an argument to
the Java command line to -D define the desired value.

Valid values, depending on the operating system, are:
 * Linux : -Dswing.defaultlaf=com.sun.java.swing.plaf.gtk.GTKLookAndFeel
 * Windows : -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel
 * Any : -Dswing.defaultlaf=com.sun.java.swing.plaf.motif.MotifLookAndFeel
 * Any : -Dswing.defaultlaf=com.sun.java.swing.plaf.metal.MetalLookAndFeel

## Using SFTP

When either a subscriber -r S listener or publisher -r L listener is
running it may also be accessed using a standard SFTP client such as
[FileZilla](https://filezilla-project.org/).

### Parameters

 * Protocol: SFTP 
 * Port: The host port **+ 1**
 * Login type: Normal
 * User name: Connecting-end (-r M or -r T) UUID key
 * Password: Server-end (-r S or -r L) UUID key

The session will be opened in the directory where the ELS.jar is located.
