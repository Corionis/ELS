# Communications Notes
This assumes the -r option is used.

## Roles
Communication follows the publisher/subscriber paradigm. The
publisher pushes content to the subscriber.

A remote session (-r S) is started on the subscriber so it
is listening and ready.

???

## Parameter Handling
When the -r option is used:

 1. For the Publisher (-r P):
    1. -p causes the Publisher to dynamically generate new JSON data
    2. -P the Publisher will import a local JSON collection file
    3. -s causes the Publisher to request the subscriber's -i JSON
    collection data, generated dynamically
    4. -S the Publisher will import a local JSON collection file
    5. -t causes the Publisher to request the subscriber's -T targets file
    6. -T the Publisher will import a location JSON targets file
 2. For the Subscriber (-r S):
    1. -p and -P are equivalent; the Subscriber only uses the key UUID
     to validate the connection
    2. -s causes the Subscriber to FORCE the publisher to take a new
    JSON export file when a connection is made by the publisher
    3. -S the Subscriber will import a local JSON collection file
    4. -t causes the Subscriber to FORCE the publisher to take a new
     subscriber's -T targets file
    5. -T the Subscriber will import a location JSON targets file
    
 2. When a local listener is running 

## At Connect-Time
Start the subscriber-side first, then the publisher.

 1. Publisher connects to subscriber.
 2. Subscriber says: HELO:[subscriber key]
 3. Publisher compares to local subscriber key
    1. If mismatch Publisher disconnects
 4. Publisher says: HELO:[publisher key]
 5. Subscriber compares to local publisher key
    1. If mismatch Subscriber disconnects
 5. Subscriber says: VolMunger:[VOLMUNGER_VERSION]
 6. Publisher parses and compares versions:
    1. If wrong match Pubisher says: ERROR:Version mismatch, publisher (me) VOLMUNGER_VERSION, subscriber (you) VOLMUNGER_VERSION
       2. Publisher disconnects
 7. Publisher says: ACK
 
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
 
 ## Waiting
 Wait for a command ...
 
 1. Subscriber says: ACK 

## Commands

 1. Scan and return JSON data
 2. Return targets file
 3. Return freespace on target
 4. Initiate file transfer via SFTP

