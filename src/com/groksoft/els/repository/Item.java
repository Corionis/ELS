package com.groksoft.els.repository;

import com.groksoft.els.Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The type Item.
 */
public class Item implements Serializable
{
    // @formatter:off
    private String fullPath;
    private String itemPath;
    private String library;
    private long size = -1L;
    private boolean directory = false;
    private boolean symLink = false;

    private transient List<Item> hasList = null;
    private transient boolean hintExecuted = false;
    private transient String itemShortName;
    private transient String itemSubdirectory;
    private transient boolean reported = false;
    // @formatter:on

    /**
     * Instantiates a new Item.
     */
    public Item()
    {
        super();
        this.hasList = new ArrayList<>();
    }

    /**
     * Add has.
     *
     * @param item The item to add
     */
    public void addHas(Item item)
    {
        if (!hasList.contains(item))
            hasList.add(item);
    }

    /**
     * Gets full path.
     *
     * @return the full path
     */
    public String getFullPath()
    {
        return fullPath;
    }

    /**
     * Sets full path.
     *
     * @param fullPath the full path
     */
    public void setFullPath(String fullPath)
    {
        this.fullPath = fullPath;
    }

    /**
     * Get has item.
     *
     * @return the matching item or null
     */
    public List<Item> getHas()
    {
        return hasList;
    }

    /**
     * Gets item path.
     * <p>
     * The item path is the right-side of the full path
     * with the library path removed from the left side.
     *
     * @return the item path
     */
    public String getItemPath()
    {
        return itemPath;
    }

    /**
     * Sets item path.
     *
     * @param itemPath the item path
     */
    public void setItemPath(String itemPath)
    {
        this.itemPath = itemPath;
    }

    /**
     * Get the item's subdirectory within the library.
     *
     * @return String of subdirectory or null
     */
    public String getItemSubdirectory()
    {
        return itemSubdirectory;
    }

    /**
     * Set the item's subdirectory within the library.
     *
     * @param itemSubdirectory
     */
    public void setItemSubdirectory(String itemSubdirectory)
    {
        this.itemSubdirectory = itemSubdirectory;
    }

    /**
     * Gets library.
     *
     * @return the library
     */
    public String getLibrary()
    {
        return library;
    }

    /**
     * Sets library.
     *
     * @param library the library
     */
    public void setLibrary(String library)
    {
        this.library = library;
    }

    /**
     * Gets size.
     * <p>
     * This is the physical size of each file, or the item count for a directory
     *
     * @return the size
     */
    public long getSize()
    {
        return size;
    }

    /**
     * Sets size.
     * <p>
     * This is the physical size of each file, or the item count for a directory
     *
     * @param size the size
     */
    public void setSize(long size)
    {
        this.size = size;
    }

    /**
     * Is directory boolean.
     *
     * @return the boolean
     */
    public boolean isDirectory()
    {
        return directory;
    }

    /**
     * Sets directory.
     *
     * @param directory the directory
     */
    public void setDirectory(boolean directory)
    {
        this.directory = directory;
    }

    /**
     * Has this hint Item been executed?
     *
     * @return true if executed
     */
    public boolean isHintExecuted()
    {
        return hintExecuted;
    }

    /**
     * Set the value of this hint Item being executed
     *
     * @param hintExecuted
     */
    public void setHintExecuted(boolean hintExecuted)
    {
        this.hintExecuted = hintExecuted;
    }

    /**
     * Has this item been reported?
     *
     * @return reported boolean, initially false
     */
    public boolean isReported()
    {
        return reported;
    }

    /**
     * Set when this item has been reported
     *
     * @param reported If this has been reported
     */
    public void setReported(boolean reported)
    {
        this.reported = reported;
    }

    /**
     * Get the item short name
     *
     * @return String short name
     */
    public String getItemShortName()
    {
        return itemShortName;
    }

    /**
     * Set the item short name
     *
     * @param itemShortName The short name
     */
    public void setItemShortName(String itemShortName)
    {
        this.itemShortName = itemShortName;
    }

    /**
     * Is sym link boolean.
     *
     * @return the boolean
     */
    public boolean isSymLink()
    {
        return symLink;
    }

    /**
     * Sets sym link.
     *
     * @param symLink the sym link
     */
    public void setSymLink(boolean symLink)
    {
        this.symLink = symLink;
    }

    /**
     * Return the itemShortName
     *
     * @return itemShortName
     */
    public String toString()
    {
        return itemShortName;
    }

}
