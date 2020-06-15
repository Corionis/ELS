# Communications Notes
This discusses using the -r option.

Communication follows the publisher/subscriber paradigm. The
publisher sends content to the subscriber.

## Changes From Original Code
 1. Changed -t to -T ... be sure to update batch/script files

## Parameter Handling
### Authorization
The publisher and subscriber automatically perform a complex
"handshake" when a new connection is initiating. It includes
comparing library keys. So no real "login" is required.

However, to access more sensitive commands **ON THE PUBLISHER**
an Authorized mode is used. Using the command "auth [password]"
where the password is the one used with the -a option when the
publisher-side was executed. See the -a option in the How-To.

There is no Authorized mode when access a subscriber because the
publisher must have full access to perform matches correctly.

### Remote modes
```
  -r P = pub-process                // automatic process for -r S
  -r L = pub-listener               // publish listener for -r T
  -r M = pub-terminal               // manual terminal for -r S

  -r S = sub-listener               // subscriber listener for -r P|M
  -r T = sub-terminal               // subscriber terminal for -r L
```

### When the -r P|S "Remote" option is used:

 1. For the Publisher (-r P):
    1. -p causes the Publisher to dynamically generate new JSON data
    2. -P the Publisher will import a local JSON collection file
    3. -s causes the Publisher to request the subscriber's -i JSON
    collection data, generated dynamically
    4. -S the Publisher will import a local JSON collection file
    5. -t causes the Publisher to request the subscriber's -T targets file
    6. -T the Publisher will import a site JSON targets file

 2. For the Subscriber (-r S):
    1. -p and -P are equivalent; the Subscriber only uses the key UUID
     to validate the connection
    2. -s causes the Subscriber to FORCE the publisher to take a new
    JSON export file when a connection is made by the publisher
    3. -S the Subscriber will import a local JSON collection file
    4. -t causes the Subscriber to FORCE the publisher to take a new
     subscriber's -T targets file
    5. -T the Subscriber will import a site JSON targets file


## Commands

 1. Scan and return JSON data
 2. Return targets file
 3. Return freespace on target
 4. Initiate file transfer via SFTP

 
### Special Commands
 Immediately after connect-time ...
 
 1. Subscriber's -s option, says: TAR
    1. Publisher goes to receive targets JSON data
    2. Publisher says: RDY
    3. Subscriber sends targets JSON data
    4. When done Publisher says: ACK

 2. HERE is where special content requests might be handled
    1. Get specific single item (directory level)
    2. Publisher says: ACK
 
