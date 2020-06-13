# Communications Notes
This discusses using the -r option.

Communication follows the publisher/subscriber paradigm. The
publisher sends content to the subscriber.

## Changes From Original Code
 1. Changed -t to -T ... be sure to update batch/script files

## Parameter Handling
Remote modes:
```
  -r P = pub-process                // automatic process for -r S
  -r B = pub-terminal               // by-hand terminal for -r S
  -r L = pub-listener               // publish listener for -r T

  -r S = sub-listener               // subscriber listener for -r P|B
  -r T = sub-terminal               // subscriber terminal for -r L
```

When the -r P|S "Remote" option is used:

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


## At Connect-Time
Start the subscriber-side first, then the publisher initiates connection.

 1. Publisher connects to subscriber
 2. Subscriber says: HELO
    1. See Interactive below
 3. Publisher says: [publisher key]
    1. Subscriber compares to key of library/collection file
       1. If mismatch Subscriber disconnects
 4. Subscriber returns: [subscriber key]
    1. Publisher compares to key of library/collection file
       1. If mismatch Publisher disconnects
 5. Publisher says: VolMunger:[VOLMUNGER_VERSION]
    1. Subscriber parses and compares versions:
       1. If wrong match says: ERROR:Version mismatch, subscriber (me) VOLMUNGER_VERSION, publisher (you) VOLMUNGER_VERSION
          1. Disconnects
 6. Subscriber returns: OK
 7. Subscriber waits for next command

## Interactive
When the publisher connects to subscriber a manual mode may be accessed.

 1. Publisher connects to subscriber
 2. Subscriber says: HELO
 3. User enters: auth [auth-password]
 4. From then on the user is in interactive command-line mode.
 5. For next-level security user enters: secret [secret-password]

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
 
