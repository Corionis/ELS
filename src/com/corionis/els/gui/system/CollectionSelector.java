package com.corionis.els.gui.system;

public class CollectionSelector implements Comparable
{
    public String description;
    public String key;

    @Override
    public int compareTo(Object o)
    {
        return description.compareTo(((CollectionSelector)o).description);
    }

    @Override
    public String toString()
    {
        return description;
    }


}
