package com.groksoft.els.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Libraries.
 */
public class Libraries
{
    public static final String APPLE = "apple";
    public static final String LINUX = "linux";
    public static final String WINDOWS = "windows";

    // @formatter:off
    /**
     * The Description of this set of libraries.
     */
    public String description;

    /**
     * The host for outgoing connections, [host name|IP address]:[port]
     * Default port is 50271 if not specified
     */
    public String host;

    /**
     * The listen for incoming connections, [host name|IP address]:[port]
     * Default port is 50271 if not specified
     */
    public String listen;

    /**
     * Flavor of system: Windows, Linux, or Mac (only)
     */
    public String flavor;

    /**
     * If remote terminal session is allowed then true, else false
     */
    public Boolean terminal_allowed;

    /**
     * The UUID of this system
     */
    public String key;

    /**
     * If case-sensitive true/false.
     */
    public Boolean case_sensitive;

    /**
     * Ignore patterns. Regular expressions are supported.
     */
    public String[] ignore_patterns;

    /**
     * Compiled patterns of ignore_patterns, transient
     */
    public transient List<Pattern> compiledPatterns = new ArrayList<>();

    /**
     * Storage. v3.0.0
     */
    public Location[] locations;

    /**
     * Substitutions. From-side regular expressions are supported.
     */
    public Renaming[] renaming;

    /**
     * The list of libraries.
     */
    public Library[] bibliography;

}
