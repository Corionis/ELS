package com.groksoft;

import java.io.Serializable;

/**
 * The type Item.
 */
public class Item implements Serializable
{
    private String context;
    private String fullPath;

    public Item() {
        super();
    }


    // Add a compare(String otherContext) method


    /**
     * Gets context.
     *
     * @return the context
     */
    public String getContext() {
        return context;
    }

    /**
     * Sets context.
     *
     * @param context the context
     */
    public void setContext(String context) {
        this.context = context;
    }

    /**
     * Gets full path.
     *
     * @return the full path
     */
    public String getFullPath() {
        return fullPath;
    }

    /**
     * Sets full path.
     *
     * @param fullPath the full path
     */
    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

}
