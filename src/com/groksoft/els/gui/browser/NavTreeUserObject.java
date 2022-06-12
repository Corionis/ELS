package com.groksoft.els.gui.browser;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.Navigator;
import com.groksoft.els.repository.Repository;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.TimeUnit;

/**
 * NavTreeUserObject for tree objects
 */
public class NavTreeUserObject implements Comparable
{
    public static final int BOOKMARKS = 0;
    public static final int COLLECTION = 1; // root of libraries
    public static final int COMPUTER = 2;
    public static final int DRIVE = 3;
    public static final int HOME = 4;
    public static final int LIBRARY = 5; // ELS library node
    public static final int REAL = 6; // physical file or directory
    public static final int SYSTEM = 7; // hidden; holds System tab Computer, Bookmarks, etc.

    public String name = "";
    public String path = "";
    public int type = REAL;
    public boolean isDir = false;
    public boolean isRemote = false;

    public File file;
    public FileTime fileTime;
    public boolean isHidden = false;
    public int mtime;
    public NavTreeNode node;
    public long size = -1L;
    public String[] sources = null;

    private NavTreeUserObject()
    {
    }

    // logical entries: BOOKMARKS, COLLECTION, COMPUTER, SYSTEM
    public NavTreeUserObject(NavTreeNode ntn, String name, int type, boolean remote)
    {
        this.node = ntn;
        this.name = name;
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
        this.fileTime = Utils.getLocalFileTime(this.path);
        this.isDir = file.isDirectory();
        this.isHidden = file.isHidden();
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
        this.isHidden = name.startsWith(".");
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

    /**
     * Clone this NavTreeUserObject. NOTE: node must be set after cloning
     *
     * @return Object A clone of this object with node = null
     */
    @Override
    public Object clone()
    {
        NavTreeUserObject tuo = new NavTreeUserObject();
        tuo.file = this.file;
        tuo.fileTime = this.fileTime;
        tuo.isDir = this.isDir;
        tuo.isHidden = this.isHidden;
        tuo.mtime = this.mtime;
        tuo.name = this.name;
        tuo.node = null;
        tuo.path = this.path;
        tuo.isRemote = this.isRemote;
        tuo.size = this.size;
        tuo.sources = this.sources;
        tuo.type = this.type;
        return tuo;
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
        }
        return node.guiContext.cfg.gs("NavTreeNode.unknown");
    }

    public synchronized Repository getRepo()
    {
        Repository repo = null;
        if (node != null)
        {
            switch (node.getMyTree().getName())
            {
                case "treeCollectionOne":
                case "treeSystemOne":
                    repo = node.guiContext.context.publisherRepo;
                    break;
                case "treeCollectionTwo":
                case "treeSystemTwo":
                    repo = node.guiContext.context.subscriberRepo;
                    break;
            }
        }
        return repo;
    }

    public String getType()
    {
        String label = (isRemote ? node.guiContext.cfg.gs("Z.remote.uppercase") : node.guiContext.cfg.gs("NavTreeNode.local"));
        switch (type)
        {
            case BOOKMARKS:
                return label + node.guiContext.cfg.gs("NavTreeNode.bookmark");
            case COLLECTION:
                return label + node.guiContext.cfg.gs("NavTreeNode.collection");
            case COMPUTER:
                return label + node.guiContext.cfg.gs("NavTreeNode.computer");
            case DRIVE:
                return label + node.guiContext.cfg.gs("NavTreeNode.drive");
            case HOME:
                return label + node.guiContext.cfg.gs("NavTreeNode.home");
            case LIBRARY:
                return label + node.guiContext.cfg.gs("NavTreeNode.library");
            case REAL:
                return label + (isDir ? node.guiContext.cfg.gs("NavTreeNode.directory") : node.guiContext.cfg.gs("NavTreeNode.file"));
            case SYSTEM:
                return label + node.guiContext.cfg.gs("NavTreeNode.system");
            default:
                return label + node.guiContext.cfg.gs("NavTreeNode.unknown");
        }
    }

    public String toString()
    {
        return name;
    }

    public boolean isSubscriber()
    {
        if (node.getMyTree().getName().toLowerCase().endsWith("two"))
            return true;
        return false;
    }

}
