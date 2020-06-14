package com.groksoft.volmunger.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Libraries.
 */
public class Libraries
{
    /**
     * The compiled patterns of ignore_patterns.
     */
    public transient List<Pattern> compiledPatterns = new ArrayList<>();

    /**
     * The Description of this set of libraries.
     */
    public String description;

    /**
     * The site for communications, [host name|IP address]:[port]
     * Default port is 50271 if not specified
     */
    public String site;

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
