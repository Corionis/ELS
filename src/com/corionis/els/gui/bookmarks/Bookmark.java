package com.corionis.els.gui.bookmarks;

import java.io.Serializable;

public class Bookmark implements Cloneable, Serializable
{
    public String name;
    public String panel;
    public String[] pathElements;

    @Override
    public Object clone()
    {
        Bookmark clone =  new Bookmark();
        clone.name = name;
        clone.panel = panel;
        clone.pathElements = pathElements;
        return clone;
    }

    public String toString()
    {
        return name;
    }
}
