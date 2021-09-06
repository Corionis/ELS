ELS has three primary ways of working - locally, client/server on two computers,
or run as the Hint Status Server.

# Local

Locally ELS uses any attached storage devices for both the publisher and 
subscriber. As long as the path to the storage device can be described 
in the JSON library file that device can be used. This includes Windows 
shares, NAS (Network Attached Storage) devices and cloud storage 
locations such as 
[DropBox](https://www.dropbox.com/features/cloud-storage) when the cloud 
drive is attached locally. 

# Remote

ELS can also operate over a LAN or the Internet (-r option) using two 
computers in a client/server arrangement both running ELS. The server or 
subscriber computer is the listener. The client or publisher computer 
attaches to the listener. Therefore the subscriber-side ELS must be 
running before the publisher-side is started. All data communication is 
encrypted. 

# Hint Status Server

When ELS dry-runs and back-ups are executed as remote operations with
hints and hint tracking enabled this mode provices an optional stand-alone
server process used coordinate hint completion status between multiple
back-ups. See [Hint Status](Hint-Status).
