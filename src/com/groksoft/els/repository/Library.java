package com.groksoft.els.repository;

import com.google.common.collect.ArrayListMultimap;

import java.util.Vector;

/**
 * The type Library.
 */
public class Library
{
    /**
     * Transient hash map for item look-ups
     *
     * @see <a href="https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/collect/ArrayListMultimap.html">ArrayListMultimap class API doc</a>
     */
    public transient ArrayListMultimap<String, Integer> itemMap;
    /**
     * One or more Items. Last member so name appears first in data.
     */
    public Vector<Item> items;
    /**
     * The library Name.
     */
    public String name;
    /**
     * Library has been altered, transient
     */
    public transient boolean rescanNeeded = false;
    /**
     * One or more Sources.
     */
    public String[] sources;

    /**
     * Get an item by itemPath in a linear search
     *
     * @param itemPath
     * @return Item matching itemPath
     */
    public Item get(String itemPath)
    {
        for (Item item : items)
        {
            if (item.getItemPath().equalsIgnoreCase(itemPath))
                return item;
        }
        return null;
    }

}
