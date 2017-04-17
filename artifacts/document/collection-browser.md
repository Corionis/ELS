# Collection Browser

## The Idea

Write an client-server application where the server runs along-side
the Plex Media Server and uses a VolMonger collection file as the
definition of the file structure. The client-side would present
a navigable GUI to allow the user to see, browse, and manipulate
their collection from anywhere. Any manipulation would also handle
creating any needed *.volmonger.control files.

## Proposed Architecture

Build this application using node.js and Angular.js. The node.js
server is very light-weight and should not interfere with PMS
operations or transcoding. The client-side can be operated from
any capable browser.
