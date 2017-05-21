package com.groksoft.volmonger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The type Libraries.
 */
public class Libraries
{
    public transient List<Pattern> compiledPatterns = new ArrayList<>();

    /**
     * The Description of this set of libraries.
     */
    public String description;

    /**
     * If case-sensitive.
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
