package com.corionis.els.hints;

import com.corionis.els.Context;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Hint implements Comparable, Serializable
{
    public long utc;

    public String author = null;
    public String system = null;

    public String action = null;

    public String fromLibrary = null;
    public String fromItemPath = null;
    public boolean directory = false;

    public String toLibrary = null;
    public String toItemPath = null;

    public List<HintStatus> statuses = null;

    // For Navigator
    public transient boolean selected = false;

    public Hint()
    {
        utc = Instant.now().toEpochMilli();
    }

    @Override
    public int compareTo(Object o)
    {
        return this.utc < ((Hint)o).utc ? -1 : (this.utc == ((Hint)o).utc) ? 0 : 1;
    }

    public void copyStatusFrom(Hint hint)
    {
        this.statuses = new ArrayList<>();
        for (HintStatus hs : hint.statuses)
        {
            this.statuses.add(hs);
        }
    }

    public HintStatus findStatus(String system)
    {
        HintStatus hs = null;
        if (statuses != null && statuses.size() > 0)
        {
            for (int i = 0; i < statuses.size(); ++i)
            {
                HintStatus stat = statuses.get(i);
                if (stat != null && stat.system.toLowerCase().equals(system.toLowerCase()))
                {
                    hs = stat;
                    break;
                }
            }
        }
        return hs;
    }

    public String getLocalUtc(Context context)
    {
        Instant instant = Instant.ofEpochMilli(utc);
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        String zdtStr = zdt.format(DateTimeFormatter.ofPattern(context.preferences.getDateFormat()));
        return zdtStr;
    }

    public String getStatus(String system, String defaultStatus)
    {
        String status = null;
        HintStatus stat = findStatus(system);
        if (stat != null)
            status = stat.status;
        else
            status = defaultStatus;
        return status;
    }

    /**
     * Is this Hint For system?
     *
     * @param hintSystemName HintKey system name
     * @return -2 if set and Done or Deleted; -1 if not found; index of status if set and not Done or Deleted, i.e. For. So "For" is >= -1
     */
    public int isFor(String hintSystemName)
    {
        if (statuses != null && statuses.size() > 0)
        {
            for (int i = 0; i < statuses.size(); ++i)
            {
                HintStatus hs = statuses.get(i);
                if (hs.system.trim().equals(hintSystemName))
                {
                    String value = hs.status.trim().toLowerCase();
                    if (!value.equals("done") && !value.equals("deleted"))
                        return i;
                    else
                        return -2;
                }
            }
        }
        return -1;
    }

    public String setStatus(String system, String status)
    {
        boolean isNew = false;
        HintStatus stat = null;

        if (statuses == null)
            statuses = new ArrayList<HintStatus>();

        stat = findStatus(system);
        if (stat != null)
        {
            if (!stat.status.toLowerCase().startsWith("deleted"))
                stat.status = status;
        }

        if (stat == null)
        {
            stat = new HintStatus(system, status);
            statuses.add(stat);
        }

        return status;
    }

    @Override
    public String toString()
    {
        Gson gson = new GsonBuilder().create();
        String json = gson.toJson(this);
        return json;
    }

}
