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
    public FolderColumn folderName;
    public String path = "";
    public long size = -1L;
    public String[] sources;
    public int type = REAL;

    // logical entries: BOOKMARKS, COLLECTION, COMPUTER, SYSTEM
    public NavTreeUserObject(String aName, int aType)
    {
        this.name = aName;
        this.isDir = true;
        this.folderName = new FolderColumn(this.name, this.isDir);
        this.type = aType;
    }

    // A physical file or directory
    public NavTreeUserObject(String name, File file)
    {
        this.name = name;
        this.folderName = new FolderColumn(this.name, false);
        this.file = file;
        this.isDir = file.isDirectory();
        this.folderName = new FolderColumn(this.name, this.isDir);
        this.type = REAL;
    }

    // A collection of libraries
    public NavTreeUserObject(String name, String[] sources)
    {
        this.name = name;
        this.sources = sources.clone();
        this.isDir = true;
        this.folderName = new FolderColumn(this.name, this.isDir);
        this.type = LIBRARY;
    }

    // A DRIVE or HOME
    public NavTreeUserObject(String name, String path, int type)
    {
        this.name = name;
        this.path = path;
        this.isDir = true;
        this.folderName = new FolderColumn(this.name, this.isDir);
        this.type = type;
    }

    // A remote file or directory
    public NavTreeUserObject(String name, String path, long size, int mtime, boolean isDir)
    {
        this.name = name;
        this.path = path;
        this.size = size;
        this.mtime = mtime;
        this.fileTime = FileTime.fromMillis(mtime);
        this.isDir = isDir;
        this.folderName = new FolderColumn(this.name, this.isDir);
        this.type = REMOTE;
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
                return "Real";
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
