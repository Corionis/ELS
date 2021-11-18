package com.groksoft.els.gui;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
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
    public static final int SYSTEM = 7; // hidden; holds System tab Computer, Bookmarks, etc.

    public File file;
    public FileTime fileTime;
    public boolean isDir = false;
    public int mtime;
    public String name = "";
    public NavTreeNode node;
    public String path = "";
    public boolean isRemote = false;
    public long size = -1L;
    public String[] sources = null;
    public int type = REAL;

    // logical entries: BOOKMARKS, COLLECTION, COMPUTER, SYSTEM
    public NavTreeUserObject(NavTreeNode ntn, String aName, int type, boolean remote)
    {
        this.node = ntn;
        this.name = aName;
        this.isDir = true;
        this.isRemote = remote;
        this.type = type;
    }

    // A local file or directory
    public NavTreeUserObject(NavTreeNode ntn, String name, File file)
    {
        this.node = ntn;
        this.name = name;
        this.file = file;
        this.path = file.getAbsolutePath();
        this.isDir = file.isDirectory();
        this.isRemote = false;
        this.type = REAL;
        try
        {
            this.size = Files.size(file.toPath());
        }
        catch (Exception e)
        {
            this.size = -1L;
        }
    }

    // A collection of libraries
    public NavTreeUserObject(NavTreeNode ntn, String name, String[] sources, boolean remote)
    {
        this.node = ntn;
        this.name = name;
        this.sources = sources.clone();
        this.isDir = true;
        this.isRemote = remote;
        this.type = LIBRARY;
    }

    // A DRIVE or HOME
    public NavTreeUserObject(NavTreeNode ntn, String name, String path, int type, boolean remote)
    {
        this.node = ntn;
        this.name = name;
        this.path = path;
        this.isDir = true;
        this.isRemote = remote;
        this.type = type;
    }

    // A remote file or directory
    public NavTreeUserObject(NavTreeNode ntn, String name, String path, long size, int mtime, boolean isDir)
    {
        this.node = ntn;
        this.name = name;
        this.path = (path.startsWith("//") ? path.substring(1) : path);
        this.size = size;
        this.mtime = mtime;
        this.fileTime = FileTime.from(mtime, TimeUnit.SECONDS);
        this.isDir = isDir;
        this.isRemote = true;
        this.type = REAL;
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
                if (isRemote)
                    return this.path;
                return this.file.getPath();
            case SYSTEM:
                return this.name;
            default:
                return "Unknown";
        }
    }

    public String getType()
    {
        String label = (isRemote ? "Remote " : "Local ");
        switch (type)
        {
            case BOOKMARKS:
                return label + "Bookmark";
            case COLLECTION:
                return label + "Collection";
            case COMPUTER:
                return label + "Computer";
            case DRIVE:
                return label + "Drive";
            case HOME:
                return label + "Home";
            case LIBRARY:
                return label + "Library";
            case REAL:
                return label + (isDir ? "Directory" : "File");
            case SYSTEM:
                return label + "System";
            default:
                return label + "Unknown";
        }
    }

    public String toString()
    {
        return name;
    }
}
