package com.groksoft.els.gui;

import java.io.File;
import java.io.Serializable;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * TreeUserObject for tree user objects
 */
public class NavTreeUserObject implements Comparable, Serializable
{
    public static final int BOOKMARKS = 0;
    public static final int COLLECTION = 1; // root of libraries
    public static final int COMPUTER = 2;
    public static final int DRIVE = 3;
    public static final int HOME = 4;
    public static final int LIBRARY = 5; // ELS library node
    public static final int REAL = 6; // use File
    public static final int REMOTE = 7; // use
    public static final int SYSTEM = 8; // hidden; holds System tab Computer, Bookmarks, etc.

    public File file;
    public FileTime fileTime;
    public boolean isDir = false;
    public int mtime;
    public String name = "";
    public NavTreeNode node;
    public String path = "";
    public long size = -1L;
    public String[] sources = null;
    public int type = REAL;

    // logical entries: BOOKMARKS, COLLECTION, COMPUTER, SYSTEM
    public NavTreeUserObject(NavTreeNode ntn, String aName, int aType)
    {
        this.node = ntn;
        this.name = aName;
        this.isDir = true;
        this.type = aType;
    }

    // A REAL physical file or directory
    public NavTreeUserObject(NavTreeNode ntn, String name, File file)
    {
        this.node = ntn;
        this.name = name;
        this.file = file;
        this.path = file.getAbsolutePath();
        this.isDir = file.isDirectory();
        this.type = REAL;
    }

    // A collection of libraries
    public NavTreeUserObject(NavTreeNode ntn, String name, String[] sources)
    {
        this.node = ntn;
        this.name = name;
        this.sources = sources.clone();
        this.isDir = true;
        this.type = LIBRARY;
    }

    // A DRIVE or HOME
    public NavTreeUserObject(NavTreeNode ntn, String name, String path, int type)
    {
        this.node = ntn;
        this.name = name;
        this.path = path;
        this.isDir = true;
        this.type = type;
    }

    // A REMOTE file or directory
    public NavTreeUserObject(NavTreeNode ntn, String name, String path, long size, int mtime, boolean isDir)
    {
        this.node = ntn;
        this.name = name;
        this.path = (path.startsWith("//") ? path.substring(1) : path);
        this.size = size;
        this.mtime = mtime;
        this.fileTime = FileTime.from(mtime, TimeUnit.SECONDS);
        this.isDir = isDir;
        this.type = REMOTE;
    }

    @Override
    public int compareTo(Object o)
    {
        boolean fbf = Navigator.guiContext.preferences.isSortFoldersBeforeFiles();
        boolean thatDir = ((NavTreeUserObject) o).isDir;
        if (Navigator.guiContext.preferences.isSortFoldersBeforeFiles())
        {
            if (isDir && !thatDir)
                return -1;
            if (thatDir && !isDir)
                return 1;
        }
        if (Navigator.guiContext.preferences.isSortCaseInsensitive())
        {
            return name.compareToIgnoreCase(((NavTreeUserObject) o).name);
        }
        return name.compareTo(((NavTreeUserObject) o).name);
    }

    public String getPath()
    {
        switch (type)
        {
            case BOOKMARKS:
                return this.name;
            case COLLECTION:
                return this.name;
            case COMPUTER:
                return this.name;
            case DRIVE:
                return this.path;
            case HOME:
                return this.path;
            case LIBRARY:
                return this.name;
            case REAL:
                return this.file.getPath();
            case REMOTE:
                return this.path;
            case SYSTEM:
                return this.name;
            default:
                return "Unknown";
        }
    }

    public String getType()
    {
        switch (type)
        {
            case BOOKMARKS:
                return "Bookmark";
            case COLLECTION:
                return "Collection";
            case COMPUTER:
                return "Computer";
            case DRIVE:
                return "Drive";
            case HOME:
                return "Home";
            case LIBRARY:
                return "Library";
            case REAL:
                return "Local";
            case REMOTE:
                return "Remote";
            case SYSTEM:
                return "System";
            default:
                return "Unknown";
        }
    }

    public String toString()
    {
        return name;
    }
}
