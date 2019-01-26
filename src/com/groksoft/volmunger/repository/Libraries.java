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
