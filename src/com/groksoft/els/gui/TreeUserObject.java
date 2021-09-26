package com.groksoft.els.gui;

import java.io.File;
import java.io.Serializable;
import java.nio.file.attribute.FileTime;

/**
 * TreeUserObject for tree user objects
 */
public class TreeUserObject implements Serializable
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

    public TreeUserObject(String name, String path, long size, int mtime, boolean isDir)
    {
        this.name = name;
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.fileTime = FileTime.fromMillis(mtime);
        this.isDir = isDir;
        this.type = REMOTE;
    }

    public TreeUserObject(String aName, String path, int aType)
    {
        this.name = aName;
        this.path = path;
        this.type = aType;
    }

    public TreeUserObject(String name, String[] sources)
    {
        this.name = name;
        this.sources = sources.clone();
        this.type = LIBRARY;
    }

    public TreeUserObject(String name, File file)
    {
        this.name = name;
        this.file = file;
        this.isDir = file.isDirectory();
        this.type = REAL;
    }

    public TreeUserObject(String aName, int aType)
    {
        this.name = aName;
        this.type = aType;
    }

    public String toString()
    {
        return name;
    }
}
