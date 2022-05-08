package com.groksoft.els.gui.bookmarks;

import java.io.Serializable;
import java.util.ArrayList;

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

    public boolean load()
    {

        return true;
    }

    public void write()
    {

    }

}
