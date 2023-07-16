package com.groksoft.els.repository;

public class Location implements Comparable
{
    /**
     * The location, drive:[\path] or mount point.
     */
    public String location;

    /**
     * The Minimum space available limit, scaled value.
     * <p>
     * See: Utils.getScaledValue and Utils.formatLong
     */
    public String minimum;

    @Override
    public int compareTo(Object o)
    {
        return location.compareTo(((Location)o).location);
    }
}
