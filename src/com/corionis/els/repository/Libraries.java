package com.corionis.els.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Libraries.
 */
public class Libraries
{
    public static final String MAC = "Mac";
    public static final String LINUX = "Linux";
    public static final String WINDOWS = "Windows";

    // @formatter:off
    /**
     * The Description of this set of libraries.
     */
    public String description = "";

    /**
     * The UUID of this system
     */
    public String key = "";

    /**
     * The host for outgoing connections, [host name|IP address]:[port]
     * Default port is 50271 if not specified
     */
    public String host = "";

    /**
     * The listen for incoming connections, [host name|IP address]:[port]
     * Default is host if not specified
     */
    public String listen = "";

    /**
     * The connection timeout for stty connection, in minutes. 0 = infinite
     */
    public int timeout = 0;

    /**
     * Flavor of system: Windows, Linux, or Mac (only)
     */
    public String flavor = "Linux";

    /**
     * If case-sensitive true/false.
     */
    public Boolean case_sensitive = false;

    /**
     * If files in temp are dated (unique) or a single file that is overwritten
     */
    public Boolean temp_dated = false;

    /**
     * Optional location for temp files generated and/or received
     */
    public String temp_location = "output";

    /**
     * If remote terminal session is allowed then true, else false
     */
    public Boolean terminal_allowed = false;

    /**
     * Ignore patterns. Regular expressions are supported.
     */
    public String[] ignore_patterns = new String[0];

    /**
     * Compiled patterns of ignore_patterns, transient
     */
    public transient List<Pattern> compiledPatterns = new ArrayList<>();

    /**
     * Storage
     */
    public Location[] locations = new Location[0];

    /**
     * The list of libraries
     */
    public Library[] bibliography = new Library[0];

}
