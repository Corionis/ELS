package com.groksoft.els.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Vector;

/**
 * The type Library.
 */
public class Library
{
    /**
     * Transient hash map for item look-ups
     * @see <a href="https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/collect/ArrayListMultimap.html">ArrayListMultimap class API doc</a>
     */
    public transient ArrayListMultimap<String, Integer> itemMap;

    /**
     * Library has been altered, transient
     */
    public transient boolean rescanNeeded = false;

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
