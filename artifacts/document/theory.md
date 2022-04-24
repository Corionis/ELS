Various ramblings on the notions and goodies of ELS.

## Purpose

Provide a purpose-built tool for the back-up and curation of home media collections
spanning multiple storage devices.

Entertainment Library Synchronizer (ELS) started as a command line back-up tool that
only used locally-connected storage for both publisher and subscriber. Over the years it
has evolved to include LAN and Internet capability, standard SFTP, an STTY interactive
terminal, Hint processing, the Hint Status Server and the ELS Navigator desktop application.

## The Publisher / Subscriber Paradigm

The publisher and subscriber paradigm is an approach to the way the code is organized
and how to think about what's going where.

In ELS terms for remote operations the subscriber is a server listener and the publisher
is the client. Whether local or remote the publisher matches it's content, based on media
libraries like Movies or TV Shows, that may span multiple storage devices, against the 
content of the subscriber.

ELS may be configured in many ways. For example:
* Curating
  * ELS subscriber listener on a collection media server
  * ELS Navigator (publisher) on a workstation that does pre-processing of media
* Back-up
  * ELS subscriber on a collection back-up system
  * ELS publisher on a collection media server

## ELS and Home Media Systems

ELS is not specific to any particular home media system provided it follows the general
conventions of directory organization as discussed elsewhere in this documentation. It
is configured separately from a media system to provide flexibility and autonomy.

Media libraries may be in a home media system that are not configured in ELS so that
content is not synchronized or shown. 

Also, logical libraries may be configured in ELS to synchronize directories that are
not part of a home media system.

It all depends on what is needed and how ELS is configured.

There is the Plex Profile Generator that is included in the ELS distribution. It is
included primarily because it happens to be the home media system we use. It is not
an endorsement of any particular product, although Plex is great.

