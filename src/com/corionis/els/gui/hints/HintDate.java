package com.corionis.els.gui.hints;

import com.corionis.els.Context;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class HintDate implements Comparable
{
    private Context context;
    private long date;

    private HintDate()
    {
        // hide default constructor
    }

    public HintDate(Context context, long date)
    {
        this.context = context;
        this.date = date;
    }

    @Override
    public int compareTo(Object o)
    {
        return date < ((HintDate)o).date ? -1 : date == ((HintDate)o).date ? 0 : 1;
    }

    @Override
    public String toString()
    {
        Instant instant = Instant.ofEpochMilli(date);
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        date = zdt.toInstant().toEpochMilli();
        SimpleDateFormat dateFormatter = new SimpleDateFormat(context.preferences.getDateFormat());
        String string = dateFormatter.format(date);
        return string;
    }
}
