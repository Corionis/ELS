package com.groksoft.els.gui;

import java.io.File;
import java.io.Serializable;
import java.nio.file.attribute.FileTime;

/**
 * TreeUserObject for tree user objects
 */
public class NavTreeUserObject implements Serializable
{
    public static final int BOOKMARKS = 0;
    public static final int BOX = 1; // holds System tab Computer, Bookmarks, etc.
    public static final int COMPUTER = 2;
    public static final int DRIVE = 3;
    public static final int HOME = 4;
    public static final int LIBRARY = 5; // ELS library node
    public static final int REAL = 6; // use File
    public static final int REMOTE = 7; // use

    public File file;
    public FileTime fileTime;
    public boolean isDir = false;
    public int mtime;
    public String name = "";
    public String path = "";
    public long size = -1L;
    public String[] sources;
    public int type = REAL;

    // A remote file or directory
    public NavTreeUserObject(String name, String path, long size, int mtime, boolean isDir)
    {
        this.name = name;
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.fileTime = FileTime.fromMillis(mtime);
        this.isDir = isDir;
        this.type = REMOTE;
    }

    public NavTreeUserObject(String name, String path, int type)
    {
        this.name = name;
        this.path = path;
        this.type = type;
    }

    public NavTreeUserObject(String name, String[] sources)
    {
        this.name = name;
        this.sources = sources.clone();
        this.type = LIBRARY;
    }

    // A physical file or directory
    public NavTreeUserObject(String name, File file)
    {
        this.name = name;
        this.file = file;
        this.isDir = file.isDirectory();
        this.type = REAL;
    }

    public NavTreeUserObject(String aName, int aType)
    {
        this.name = aName;
        this.type = aType;
    }

    public String getType()
    {
        switch (type)
        {
            case BOOKMARKS:
                return "Bookmark";
            case BOX:
                return "Box";
            case DRIVE:
                return "Drive";
            case HOME:
                return "Home";
            case LIBRARY:
                return "Library";
            case REAL:
                return "Real";
            case REMOTE:
                return "Remote";
            default:
                return "Unknown";
        }
    }

    public String toString()
    {
        return name;
    }
}
