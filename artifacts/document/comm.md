# VolMunger: Communications Notes
This discusses using the -r option.

Communication follows the publisher/subscriber paradigm. The
publisher sends content to the subscriber.

## Changes From Original Code
 1. Changed -t to -T ... be sure to update batch/script files

## Parameter Handling
### Authorization
The publisher and subscriber automatically perform a complex
"handshake" when a new connection is initiated. That includes
comparing library keys and other things. So no manual "login"
is required.

However, to access more sensitive commands 
an Authorized mode is used. Using the command "auth [password]"
where the password is the one used with the -a option when the
listener-side was executed. See the -a option in the How-To.

### Remote modes
```
  -r L = pub-listener       // publish listener for -r T
  -r M = pub-manual         // publisher stty manual terminal connect to -r S
  -r P = remote-publish     // automated publish process to -r S

  -r S = sub-listener       // subscriber listener for -r P|M
  -r T = sub-terminal       // subscriber stty manual terminal connect to -r L
  
  The mode letter is case-insensitive.
```

## Commands
Manual terminal commands:

 1. auth [password] = access Authorized commands
 2. collection = get collection data from remote
 3. space [location] = free space at location on remote
 4. targets = get targets file from remote
 5. help
 6. logout = exit current level
 7. bye, exit, quit = disconnect

Manual terminal to subscriber (-r S and -r M) "authorized" commands:
 1. status = server and console status information

Manual terminal to publisher (-r L and -r T) "authorized" commands:
 1. status = server and console status information
 2. find [text] = search collection for all matching text, use 
 collection command to refresh
 3. get [text] = like find but offers the option to get/copy the
 listed items in overwrite mode

### When the -r P|S "Remote" option is used:

 1. For the Publisher (-r P):
    1. -p causes the Publisher to dynamically generate new JSON data
    2. -P the Publisher will import a local JSON collection file
    3. -s causes the Publisher to import a local JSON library file
       for site-related data then request the subscriber's -i JSON
       collection data
    4. -S the Publisher will import a local JSON collection file
    5. -t causes the Publisher to request the subscriber's -T targets file
    6. -T the Publisher will import a subscriber JSON targets file

 2. For the Subscriber (-r S):
    1. -p and -P are equivalent; only site-related data is used
    2. -s causes the Subscriber to load a JSON libraries file,
       then will do a full scan for the collection
    3. -S the Subscriber will import a local JSON collection file
       and will FORCE the publisher to take that JSON export file
       when a connection is made by the publisher
    4. -t the Subscriber will import a subscriber JSON targets file
    5. -T causes the Subscriber to FORCE the publisher to take a new
       subscriber's -T targets file

 3. The combination of behavior changes when using -r cause and require
    the Process.munge() method to start with a complete collection
    file. Therefore mid-munge scans are not done on the subscribe-side. 

## Configuration
Some items are required in the JSON library file configuration to
support communications. Some are optional.

 1. host : The hostname:port to connect to for outgoing connections,
    e.g. mybox.home.com:29900. If a port is not specified 50271 is
    used as the BASE port number. An IP address may be used instead
    of a hostname.
 2. listen : Optional. If specified the hostname:port to listen for
    incoming connections as a listen, e.g. mybox.home.com:30000.
    If a port is not specified 50271 is used as the BASE port number. 
    An IP address may be used instead of a hostname. If not specified
    the "host" value will be used.
 3. flavor : The flavor of operating system: windows, linux or apple (only)
 4. terminal_allowed : true or false (only). If an interactive manual
    terminal connection is allowed
 5. key : A formatted universally unique ID (UUID), e.g. "025e2ddb-942a-4206-8458-902a87e42e62"

### Port Numbers and Servers
Two servers are started for each session, one for commands using STTY
and one for transferring files using SFTP. For -r L + -r T a second set
of servers is started on the subscriber end.

The port numbers are sequential starting with the port specified in the
"site" parameter. For example if the site is mybox.home.com:30000 that
port is used for STTY and 30001 is used for SFTP.

If it is a -r L + -r T publisher/subscriber session then the extra
STTY is 30002 and its SFTP on 30003.

### Alternate Access using SFTP
When either a subscriber -r S listener or publisher -r L listener is
running it may also be accessed using a standard SFTP client such as
FileZilla.

The protocol is SFTP, the user name is connecting-end's (-r M or -r T)
UUID key, and the password is the server-end's (-r S or -r L) UUID key.
