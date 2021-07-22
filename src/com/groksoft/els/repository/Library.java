package com.groksoft.els.repository;

import com.google.common.collect.Multimap;
import java.util.Vector;

/**
 * The type Library.
 */
public class Library
{
    /**
     * Transient hash map for item look-ups
     */
    public transient Multimap<String, Integer> itemMap;

    /**
     * The library Name.
     */
    public String name;

    /**
     * One or more Sources.
     */
    public String[] sources;

    /**
     * One or more Items. Last member so name appears first in data.
     */
    public Vector<Item> items;

}
