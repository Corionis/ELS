package com.groksoft.els.gui.bookmarks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class Bookmarks implements Serializable
{
    ArrayList<Bookmark> bookmarks;

    public Bookmarks()
    {
        bookmarks = new ArrayList<Bookmark>();
    }

    public void add(Bookmark bookmark)
    {
        if (find(bookmark) > -1)
        {
            delete(bookmark);
        }
        bookmarks.add(bookmark);
    }

    public void delete(Bookmark bookmark)
    {
        bookmarks.remove(bookmark);
    }

    public void delete(int index)
    {
        bookmarks.remove(index);
    }

    public int find(Bookmark bookmark)
    {
        for (int i = 0; i < bookmarks.size(); ++i)
        {
            Bookmark candidate = bookmarks.get(i);
            if (candidate.name.equals(bookmark.name))
                return i;
        }
        return -1;
    }

    public Bookmark find(String name)
    {
        for (int i = 0; i < bookmarks.size(); ++i)
        {
            Bookmark candidate = bookmarks.get(i);
            if (candidate.name.equals(name))
                return candidate;
        }
        return null;
    }

    public Bookmark get(int index)
    {
        return bookmarks.get(index);
    }

    public String getFullPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els"  + System.getProperty("file.separator") +
                "bookmarks.json";
        return path;
    }

    public void sort()
    {
        Collections.sort(bookmarks, new Comparator<Bookmark>()
        {
            @Override
            public int compare(Bookmark bookmark, Bookmark t1)
            {
                return bookmark.name.compareToIgnoreCase(t1.name);
            }
        });
    }

    public int size()
    {
        return bookmarks.size();
    }

    public void write() throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException("Error writing: " + getFullPath() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

}
