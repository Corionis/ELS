package com.groksoft.els.gui;

import com.groksoft.els.Utils;

public class SizeColumn implements Comparable
{
    private boolean raw = false;
    private long size;

    private SizeColumn()
    {
        // hide default constructor
    }

    public SizeColumn(long sz)
    {
        size = sz;
    }

    public SizeColumn(long sz, boolean r)
    {
        size = sz;
        raw = r;
    }

    @Override
    public int compareTo(Object o)
    {
        return Long.compare(size, ((SizeColumn) o).size);
    }

    public long getSize()
    {
        return size;
    }

    public void setSize(long size)
    {
        this.size = size;
    }

    @Override
    public String toString()
    {
        if (raw)
            return Long.toString(size);
        return Utils.formatLong(size, false);
    }

}
