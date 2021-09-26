package com.groksoft.els.repository;

import com.google.common.collect.ArrayListMultimap;

import java.util.Vector;

/**
 * The type Library.
 */
public class Library implements Comparable<Library>
{
    // @formatter:off
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

    /**
     * Transient hash map for item look-ups
     *
     * @see <a href="https://guava.dev/releases/snapshot-jre/api/docs/com/google/common/collect/ArrayListMultimap.html">ArrayListMultimap class API doc</a>
     */
    public transient ArrayListMultimap<String, Integer> itemMap;

    /**
     * Library has been altered, transient
     */
    public transient boolean rescanNeeded = false;

    /**
     * Library name comparator
     * @param library
     * @return < 0, 0, or > 0
     */
    @Override
    public int compareTo(Library library)
    {
        return this.name.compareTo(library.name);
    }

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

    /**
     * Get index of an item
     *
     * @param itemPath to be found
     * @return Integer index or -1
     */
    public int getIndexOf(String itemPath)
    {
        int index = 0;
        for ( ; index < items.size(); ++index)
        {
            if (items.elementAt(index).getItemPath().equalsIgnoreCase(itemPath))
                return index;
        }
        return -1;
    }

    /**
     * Return the name
     *
     * @return name
     */
    public String toString()
    {
        return name;
    }

    // @formatter:on
}
