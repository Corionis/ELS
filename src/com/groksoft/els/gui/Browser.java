package com.groksoft.els.gui;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.Vector;

public class Browser
{
    private transient Logger logger = LogManager.getLogger("applog");
    private ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private Configuration cfg;
    private Context context;
    private Navigator navigator;
    private Navigator.GuiContext guiContext;

    // styles of tree display
    private static final int STYLECOLLECTION_ALL = 0;
    private static final int STYLECOLLECTION_AZ = 1;
    private static final int STYLECOLLECTION_SOURCES = 2;
    private static final int STYLESYSTEM_TREE = 0;
    private static final int STYLESYSTEM_BOOKMARKS = 1;

    int styleOne = STYLECOLLECTION_ALL;
    int styleTwo = STYLESYSTEM_TREE;

    public Browser(Navigator nav, Configuration config, Context ctx, Navigator.GuiContext gctx)
    {
        navigator = nav;
        cfg = config;
        context = ctx;
        guiContext = gctx;

        initialize();
    }

    private boolean initialize()
    {
        // treeCollectionOne
        if (context.publisherRepo != null && context.publisherRepo.isInitialized())
            loadCollectionTree(guiContext.form.treeCollectionOne, context.publisherRepo);
        else
            loadTreeRoot(guiContext.form.treeCollectionOne, "--Open a publisher--");

        // treeSystemOne
        loadSystemTree(guiContext.form.treeSystemOne, System.getProperty("user.name"));

        // treeCollectionTwo
        if (context.publisherRepo != null && context.publisherRepo.isInitialized())
            loadCollectionTree(guiContext.form.treeCollectionTwo, context.publisherRepo);
        else
            loadTreeRoot(guiContext.form.treeCollectionTwo, "--Open a profile--");

        // treeSystemTwo
        loadSystemTree(guiContext.form.treeSystemTwo, System.getProperty("user.name"));

        return true;
    }

    public void loadCollectionTree(JTree tree, Repository repo)
    {
        try
        {
            loadTreeRoot(tree, repo.getLibraryData().libraries.description);
            switch (styleOne)
            {
                case STYLECOLLECTION_ALL:
                    styleAll(tree, repo);
                    break;
                case STYLECOLLECTION_AZ:
                    break;
                case STYLECOLLECTION_SOURCES:
                    break;
                default:
                    break;
            }
            ((DefaultTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
        }
    }

    public void loadSystemTree(JTree tree, String initialLocation)
    {
        try
        {
            switch (styleTwo)
            {
                case STYLESYSTEM_TREE:
                    styleTree(tree, initialLocation);
                    break;
                case STYLESYSTEM_BOOKMARKS:
                    break;
                default:
                    break;
            }
            ((DefaultTreeModel) tree.getModel()).reload();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
        }
    }

    private void loadTreeRoot(JTree tree, String title)
    {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) new DefaultMutableTreeNode(title);
        DefaultTreeModel model = (DefaultTreeModel) new DefaultTreeModel(root, true);
        tree.setModel(model);
        tree.setShowsRootHandles(true);
        tree.setLargeModel(true);
    }

    private void styleAll(JTree tree, Repository repo) throws Exception
    {
        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) model.getRoot();
        for (Library lib : repo.getLibraryData().libraries.bibliography)
        {
            if (lib.items == null || lib.items.size() < 1)
                repo.scan(lib.name);

            DefaultMutableTreeNode node = (DefaultMutableTreeNode) new DefaultMutableTreeNode(lib.name);
            root.add(node);
            styleAllNodes(node, lib, 0);
            if (context.fault)
                break;
        }
    }

    private int styleAllNodes(DefaultMutableTreeNode node, Library lib, int index)
    {
        String nodePath;
        if (node.getUserObject() instanceof String)
        {
            nodePath = "";
        }
        else if (node.getUserObject() instanceof TreeSystemObject)
        {
            nodePath = "";
        }
        else // must be an item
            nodePath = ((Item)node.getUserObject()).getFullPath();

        while (index < lib.items.size())
        {
            Item item = lib.items.elementAt(index);

            // return out of recursion if not same part of tree
            if (nodePath.length() > 0 && !item.getFullPath().startsWith(nodePath))
                return index;

            ++index;
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) new DefaultMutableTreeNode(item);
            if (item.isDirectory())
            {
                treeNode.setAllowsChildren(true);
                node.add(treeNode);
                index = styleAllNodes(treeNode, lib, index);
            }
/*
            else
            {
                treeNode.setAllowsChildren(false);
                node.add(treeNode);
            }
*/
        }
        return index;
    }

    public void styleTree(JTree tree, String initialLocation) throws Exception
    {
        /*
         * BookMarks
         *   Entry
         * Computer
         *   Drive /
         * Home
         *   Directory trh
         */
        // TODO Change to externalized strings

        // setup new empty tree & System tree cell renderer
        TreeSystemObject tso = new TreeSystemObject("Box", TreeSystemObject.BOX);
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) new DefaultMutableTreeNode(tso);
        root.setAllowsChildren(true);
        DefaultTreeModel model = (DefaultTreeModel) new DefaultTreeModel(root, true);
        tree.setShowsRootHandles(true);
        tree.setRootVisible(false);
        tree.setLargeModel(true);
        tree.setModel(model);
        tree.setCellRenderer(new SystemTreeCellRenderer());

        // add Computer root node
        tso = new TreeSystemObject("Computer", TreeSystemObject.COMPUTER);
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) new DefaultMutableTreeNode(tso);
        rootNode.setAllowsChildren(true);
        root.add(rootNode);

        // get all available storage drives
        File[] rootPaths;
        FileSystemView fsv = FileSystemView.getFileSystemView();
        rootPaths = File.listRoots();

        // create an empty Repository with one library per hard drive
        Repository repoComputer = Repository.createEmptyRepository(cfg, rootPaths.length + 2);

        for (int i = 0; i < rootPaths.length; ++i)
        {
            File drive = rootPaths[i];
            repoComputer.getLibraryData().libraries.bibliography[i].name = drive.getPath();
            repoComputer.getLibraryData().libraries.bibliography[i].sources = new String[1];
            repoComputer.getLibraryData().libraries.bibliography[i].sources[0] = drive.getAbsolutePath();

            tso = new TreeSystemObject(drive.getPath(), TreeSystemObject.DRIVE);
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) new DefaultMutableTreeNode(tso);
            rootNode.add(treeNode);

            styleTreeScan(repoComputer.getLibraryData().libraries.bibliography[i], "", drive.getAbsolutePath());

            repoComputer.sort(repoComputer.getLibraryData().libraries.bibliography[i]);
            styleTreeNodes(treeNode, repoComputer.getLibraryData().libraries.bibliography[i], 0);
        }

//        repo.getLibraryData().libraries.bibliography[1].name = "Home";
//        repo.getLibraryData().libraries.bibliography[2].name = "Bookmarks";
    }

    private int styleTreeNodes(DefaultMutableTreeNode node, Library lib, int index)
    {
        String nodePath;
        if (node.getUserObject() instanceof TreeSystemObject)
        {
            nodePath = "";
        }
        else
            nodePath = ((Item)node.getUserObject()).getFullPath();

        while (index < lib.items.size())
        {
            Item item = lib.items.elementAt(index);

            // return out of recursion if not same part of tree
            if (nodePath.length() > 0 && !nodePath.startsWith(item.getFullPath()))
                return index;

            ++index;
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) new DefaultMutableTreeNode(item);
            if (item.isDirectory())
            {
                treeNode.setAllowsChildren(true);
                node.add(treeNode);
                index = styleTreeNodes(treeNode, lib, index);
            }
/*
            else
            {
                treeNode.setAllowsChildren(false);
                node.add(treeNode);
            }
*/
        }
        return index;
    }

    /**
     * Scan a specific directory, one level.
     *
     * @param directory the directory
     * @throws MungeException the els exception
     */
    private int styleTreeScan(Library library, String base, String directory) throws MungeException
    {
        Item item = null;
        String fullPath = "";
        String itemPath = "";
        long size = 0;
        boolean isDir = false;
        boolean isSym = false;
        Path path = Paths.get(directory);

        if (library.items == null)
        {
            library.items = new Vector<>();
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path))
        {
            for (Path entry : directoryStream)
            {
                item = new Item();
                fullPath = entry.toString();                            // full path
                item.setFullPath(fullPath);
                path = Paths.get(fullPath);
                isDir = Files.isDirectory(path);                        // is directory check
                item.setDirectory(isDir);
                size = (isDir ? 0L : Files.size(path));                 // size
                item.setSize(size);
                itemPath = fullPath.substring(base.length() + 1);       // item path
                item.setItemPath(itemPath);
                isSym = Files.isSymbolicLink(path);                     // is symbolic link check
                item.setSymLink(isSym);
                item.setLibrary(library.name);                          // the library name

                if (!Utils.isFileOnly(item.getItemPath()))
                {
                    item.setItemSubdirectory(Utils.getLeftPath(item.getItemPath(), System.getProperty("path.separator")));
                }

                item.setItemShortName(Utils.getShortPath(item.getItemPath(), System.getProperty("path.separator")));
                library.items.add(item);

/*
                if (isDir)
                {
                    // track item count in a directory item's size
                    item.setSize(styleTreeScan(library, base, item.getFullPath()));
                }
*/
            }
        }
        catch (IOException ioe)
        {
            throw new MungeException("Exception reading directory " + directory + " trace: " + Utils.getStackTrace(ioe));
        }
        library.rescanNeeded = false;
        return 0;
    }

    // ------------------------------------------------------------------------

    /**
     * Tree Cell Renderer class for System tree
     */
    private class SystemTreeCellRenderer extends DefaultTreeCellRenderer
    {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

            // FileChooser.homeFolderIcon  +
            // FileView.computerIcon  +
            // FileView.directoryIcon
            // FileView.fileIcon
            // FileView.floppyDriveIcon
            // FileView.hardDriveIcon
            if (value instanceof DefaultMutableTreeNode)
            {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                if (node.getUserObject() instanceof String)
                {
                    setIcon(UIManager.getIcon("FileView.computerIcon"));
                }
                else if (node.getUserObject() instanceof TreeSystemObject)
                {
                    TreeSystemObject tso = (TreeSystemObject) node.getUserObject();
                    switch (tso.type)
                    {
                        case TreeSystemObject.BOX:
                            break;
                        case TreeSystemObject.COMPUTER:
                            setIcon(UIManager.getIcon("FileView.computerIcon"));
                            break;
                        case TreeSystemObject.DRIVE:
                            setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
                            break;
                        case TreeSystemObject.HOME:
                            setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
                            break;
                        case TreeSystemObject.BOOKMARKS:
                            setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                            break;
                    }
                }
                else if (node.getUserObject() instanceof Item)
                {
                    Item item = (Item) node.getUserObject();
                    if (item.isDirectory())
                    {
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                    }
                    else
                    {
                        setIcon(UIManager.getIcon("FileChooser.fileIcon"));
                    }
                }
            }
            return this;
        }
    } // private class SystemTreeCellRenderer

    // ------------------------------------------------------------------------

    /**
     * TreeSystemObject for faux System tree entries
     */
    private class TreeSystemObject
    {
        public static final int REAL = 0; // use directory flag
        public static final int BOX = 1;
        public static final int COMPUTER = 2;
        public static final int DRIVE = 3;
        public static final int HOME = 4;
        public static final int BOOKMARKS = 5;

        String name = "";
        int type = REAL;

        public TreeSystemObject(String aName, int aType)
        {
            name = aName;
            type = aType;
        }

        public String toString()
        {
            return name;
        }
    } // private class TreeSystemObject

} // public class Browser
