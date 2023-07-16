package com.groksoft.els.gui.libraries;

public class LibrarySelector implements Comparable
{
    public boolean selected = false;
    public String name;

    public LibrarySelector(String name)
    {
        this.name = name;
    }

    @Override
    public int compareTo(Object o)
    {
        return name.compareTo(((LibrarySelector)o).name);
    }

    @Override
    public String toString()
    {
        return name;
    }

}
