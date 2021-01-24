![ELS-Plex logo](https://github.com/GrokSoft/ELS-Plex-Generator/blob/master/artifacts/images/els-plex-logo.png)

ELS-Plex-Generator is an add-on tool for Entertainment Library Synchronizer (ELS),
available at [https://github.com/GrokSoft/ELS-Plex-Generator](https://github.com/GrokSoft/ELS-Plex-Generator), that generates the publisher library
JSON file required by ELS.

ELS-Plex-Generator queries a [Plex Media Server](https://www.plex.tv) (PMS) directly using the PMS REST interface, typically on port :32400
to gather the necessary data.

A PMS X-Plex-Token is required for authentication, see [Finding an authentication token / X-Plex-Token](https://support.plex.tv/articles/204059436-finding-an-authentication-token-x-plex-token/) on the Plex support site.

This is an optional tool specifically for Plex. ELS supports any modern media system that uses the same directory structure.
