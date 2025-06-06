package com.corionis.els.gui.browser;

import com.corionis.els.Utils;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;

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
    public NavTreeUserObject(NavTreeNode ntn, String name, String path, File file)
    {
        this.node = ntn;
        this.name = name;
        this.file = file;
        this.path = file.getPath();
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
        if (mtime < 0)
            this.fileTime = FileTime.fromMillis(System.currentTimeMillis());
        else
            this.fileTime = FileTime.from(mtime, TimeUnit.SECONDS);
        this.isDir = isDir;
        this.isHidden = name.startsWith(".");
        this.isRemote = true;
        this.type = REAL;
    }

    @Override
    public int compareTo(Object o)
    {
        NavTreeUserObject nto = (NavTreeUserObject) o;
        boolean thatDir = ((NavTreeUserObject) o).isDir;
        if (nto.node.context.preferences.isSortFoldersBeforeFiles())
        {
            if (isDir && !thatDir)
                return -1;
            if (thatDir && !isDir)
                return 1;
        }
        if (nto.node.context.preferences.isSortCaseInsensitive())
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
        tuo.node = this.node;
        tuo.path = this.path;
        tuo.isRemote = this.isRemote;
        tuo.size = this.size;
        tuo.sources = this.sources;
        tuo.type = this.type;
        return tuo;
    }

    public String getDisplayPath()
    {
        String path = getPath();
        if (node != null)
        {
            String os = node.getMyRepo().getLibraryData().libraries.flavor;
            boolean isWindows = os.equalsIgnoreCase("windows") ? true : false;
            if (isWindows && path.length() > 1 && path.startsWith("/"))
            {
                path = path.substring(1);
                path = path.replaceAll("/", "\\\\");
                path = path.replaceAll("\\\\\\\\", "\\\\");
            }
        }
        return path;
    }

    public String getItemPath(String library, String path)
    {
        String itemPath = "";
        try
        {
            Library lib = getRepo().getLibrary(library);
            if (lib != null)
            {
                for (String source : lib.sources)
                {
                    String sourcePath;
                    if (Utils.isRelativePath(path))
                        sourcePath = source;
                    else
                    {
                        if (getRepo().getPurpose() == Repository.PUBLISHER)
                            sourcePath = Utils.getFullPathLocal(source);
                        else
                            sourcePath = node.context.cfg.getFullPathSubscriber(source);

                        if (sourcePath.matches("^\\\\[a-zA-Z]:.*") || sourcePath.matches("^/[a-zA-Z]:.*"))
                            sourcePath = sourcePath.substring(1);
                    }

                    if (path.startsWith(sourcePath))
                    {
                        itemPath = path.substring(sourcePath.length() + 1);
                        break;
                    }
                }
            }
        }
        catch (Exception e)
        {
        }
        return itemPath;
    }

    public NavTreeNode getParentLibrary()
    {
        boolean found = false;
        NavTreeNode node = this.node;
        while (true)
        {
            if (node.getUserObject().type == LIBRARY)
            {
                found = true;
                break;
            }
            node = (NavTreeNode) node.getParent();
            if (node == null)
                break;
        }
        if (!found)
            node = null;
        return node;
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
        return node.context.cfg.gs("NavTreeNode.unknown");
    }

    public String getRelativePath()
    {
        String path = getPath();
        if (type == REAL)
        {
            if (node.getMyRepo().isPublisher())
                path = node.context.cfg.makeRelativePath(path);
            else
                path = node.context.cfg.makeRelativePathSubscriber(path);
        }
        return path;
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
                    repo = node.context.publisherRepo;
                    break;
                case "treeCollectionTwo":
                case "treeSystemTwo":
                    repo = node.context.subscriberRepo;
                    break;
            }
        }
        return repo;
    }

    public String getType()
    {
        String label = (isRemote ? node.context.cfg.gs("Z.remote.uppercase") : node.context.cfg.gs("NavTreeNode.local"));
        switch (type)
        {
            case BOOKMARKS:
                return label + node.context.cfg.gs("NavTreeNode.bookmark");
            case COLLECTION:
                return label + node.context.cfg.gs("NavTreeNode.collection");
            case COMPUTER:
                return label + node.context.cfg.gs("NavTreeNode.computer");
            case DRIVE:
                return label + node.context.cfg.gs("NavTreeNode.drive");
            case HOME:
                return label + node.context.cfg.gs("NavTreeNode.home");
            case LIBRARY:
                return label + node.context.cfg.gs("NavTreeNode.library");
            case REAL:
                return label + (isDir ? node.context.cfg.gs("NavTreeNode.directory") : node.context.cfg.gs("NavTreeNode.file"));
            case SYSTEM:
                return label + node.context.cfg.gs("NavTreeNode.system");
            default:
                return label + node.context.cfg.gs("NavTreeNode.unknown");
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
