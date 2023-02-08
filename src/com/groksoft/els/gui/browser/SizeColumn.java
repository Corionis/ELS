package com.groksoft.els.gui.browser;

import com.groksoft.els.Utils;

public class SizeColumn implements Comparable
{
    private boolean raw = false;
    private long size;
    private double scale;

    private SizeColumn()
    {
        // hide default constructor
    }

    public SizeColumn(long size, double scale)
    {
        this.size = size;
        this.scale = scale;
    }

    public SizeColumn(long size, double scale, boolean raw)
    {
        this.size = size;
        this.scale = scale;
        this.raw = raw;
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
        return Utils.formatLong(size, false, scale);
    }

}
