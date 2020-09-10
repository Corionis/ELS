package com.groksoft.volmunger.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Libraries.
 */
public class Libraries
{
    public static final String WINDOWS = "windows";
    public static final String LINUX = "linux";
    public static final String APPLE = "apple";

    /**
     * The compiled patterns of ignore_patterns.
     */
    public transient List<Pattern> compiledPatterns = new ArrayList<>();

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
    public String terminal_allowed;

    /**
     * The UUID of this system
     */
    public String key;

    /**
     * If case-sensitive true/false.
     */
    public Boolean case_sensitive;

    /**
     * Ignore patterns.
     */
    public String[] ignore_patterns;

    /**
     * The list of libraries.
     */
    public Library[] bibliography;
}
