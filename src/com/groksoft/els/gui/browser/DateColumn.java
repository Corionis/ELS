package com.groksoft.els.gui.browser;

import com.groksoft.els.gui.Navigator;

import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

public class DateColumn implements Comparable
{
    FileTime time;

    public DateColumn()
    {
        // hide default constructor
    }

    public DateColumn(FileTime t)
    {
        time = t;
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
            SimpleDateFormat dateFormatter = new SimpleDateFormat(Navigator.guiContext.preferences.getDateFormat());
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
