package com.corionis.els.gui.browser;

import com.corionis.els.Context;

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

public class DateColumn implements Comparable
{
    private Context context;
    private FileTime time;

    public DateColumn()
    {
        // hide default constructor
    }

    public DateColumn(Context context, FileTime time)
    {
        this.context = context;
        this.time = time;
    }

    @Override
    public int compareTo(Object o)
    {
        return time.compareTo(((DateColumn) o).time);
    }

    public String formatFileTime(FileTime stamp)
    {
        if (stamp != null)
        {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(context.preferences.getDateFormat());
            return dateFormatter.format(stamp.toMillis());
        }
        return "";
    }

    public FileTime getTime()
    {
        return time;
    }

    public void setTime(FileTime time)
    {
        this.time = time;
    }

    @Override
    public String toString()
    {
        return formatFileTime(time);
    }

}
