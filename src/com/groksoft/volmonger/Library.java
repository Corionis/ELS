package com.groksoft.volmonger;

import java.util.List;

/**
 * The type Library.
 */
public class Library
{
    /**
     * The library Name.
     */
    public String name;

    // QUESTION Should a library "type" be added?
    // Plex types: Movies, TV Shows, Music, Photos, Other Videos

    /**
     * One or more Sources.
     */
    public String[] sources;

    /**
     * One or more Items.
     */
    public List<Item> items;
}
