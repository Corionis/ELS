package com.groksoft.els.repository;

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

    /**
     * One or more Sources.
     */
    public String[] sources;

    /**
     * One or more Items.
     */
    public List<Item> items;
}
