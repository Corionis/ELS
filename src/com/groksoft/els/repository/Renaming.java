package com.groksoft.els.repository;

import java.util.regex.Pattern;

/**
 * The type Renaming
 */
public class Renaming
{
    /**
     * Compiled patterns of ignore_patterns.
     */
    public transient Pattern compiledPattern;

    /**
     * The string to find. Regular expressions are supported.
     */
    public String from;

    /**
     * The string to replace the find. Simple string.
     */
    public String to;

}
