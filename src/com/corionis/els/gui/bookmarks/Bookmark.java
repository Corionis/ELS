package com.corionis.els.gui.bookmarks;

import java.io.Serializable;

public class Bookmark implements Serializable
{
    public String name;
    public String panel;
    public String[] pathElements;

    public String toString()
    {
        return name;
    }
}
