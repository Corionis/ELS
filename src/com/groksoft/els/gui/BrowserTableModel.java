package com.groksoft.els.gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public class BrowserTableModel extends AbstractTableModel
{
//    NavTreeModel model;
    NavTreeNode node;
//    JTree tree;

    public BrowserTableModel(NavTreeNode start)
    {
        super();
//        tree = tr;
        node = start;
//        model = (NavTreeModel) tree.getModel();
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return Icon.class;
            case 1:
                return String.class;
            case 2:
                return Long.class;
            case 3:
                return FileTime.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 4;
    }

    @Override
    public String getColumnName(int column)
    {
        switch (column)
        {
            case 0:
                return "";
            case 1:
                return "Name";
            case 2:
                return "Size";
            case 3:
                return "Modified";
        }
        return "unknown";
    }

    @Override
    public int getRowCount()
    {
        return node.getChildCount(false);
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        TreeNode child = node.getChildAt(row, false);
        Object userObject = ((DefaultMutableTreeNode) child).getUserObject();
        if (userObject != null)
        {
            if (userObject instanceof String)
            {
                if (column == 0)
                    return UIManager.getIcon("FileChooser.homeFolderIcon");
                if (column == 1)
                    return userObject;
                if (column == 2)
                    return Long.valueOf(child.getChildCount());
            }
            else if (userObject instanceof NavTreeUserObject)
            {
                if (column == 0) // icon
                {
                    switch (((NavTreeUserObject) userObject).type)
                    {
                        case NavTreeUserObject.BOOKMARKS:
                            return UIManager.getIcon("FileView.floppyDriveIcon");
                        case NavTreeUserObject.BOX:
                            break;
                        case NavTreeUserObject.COMPUTER:
                            return UIManager.getIcon("FileView.computerIcon");
                        case NavTreeUserObject.DRIVE:
                            return UIManager.getIcon("FileView.hardDriveIcon");
                        case NavTreeUserObject.HOME:
                            return UIManager.getIcon("FileChooser.homeFolderIcon");
                        case NavTreeUserObject.LIBRARY:
                            return UIManager.getIcon("FileView.directoryIcon");
                        case NavTreeUserObject.REAL:
                            if (((NavTreeUserObject) userObject).file != null && ((NavTreeUserObject) userObject).file.isDirectory())
                                return UIManager.getIcon("FileView.directoryIcon");
                            else
                                return UIManager.getIcon("FileView.fileIcon");
                        case NavTreeUserObject.REMOTE:
                            if (((NavTreeUserObject) userObject).isDir)
                                return UIManager.getIcon("FileView.directoryIcon");
                            else
                                return UIManager.getIcon("FileView.fileIcon");
                        default:
                            return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error
                    }
                }
                if (column == 1) // name
                    return ((NavTreeUserObject) userObject).name;
                if (column == 2) // size
                {
                    switch (((NavTreeUserObject) userObject).type)
                    {
                        case NavTreeUserObject.BOOKMARKS:
                            return Long.valueOf(child.getChildCount());
                        case NavTreeUserObject.BOX:
                            break;
                        case NavTreeUserObject.COMPUTER:
                            return Long.valueOf(child.getChildCount());
                        case NavTreeUserObject.DRIVE:
                            return Long.valueOf(child.getChildCount());
                        case NavTreeUserObject.HOME:
                            return Long.valueOf(child.getChildCount());
                        case NavTreeUserObject.LIBRARY:
                            return Long.valueOf(child.getChildCount());
                        case NavTreeUserObject.REAL:
                            if (((NavTreeUserObject) userObject).file != null)
                            {
                                if (((NavTreeUserObject) userObject).file.isDirectory())
                                    return Long.valueOf(child.getChildCount());
                                try
                                {
                                    long size = Files.size(((NavTreeUserObject) userObject).file.toPath());
                                    return size;
                                }
                                catch (Exception e)
                                {
                                }
                            }
                            break;
                        case NavTreeUserObject.REMOTE:
                            if (((NavTreeUserObject) userObject).isDir)
                                return null;
                            return ((NavTreeUserObject) userObject).size;
                        default:
                            return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error

                    }
                }
                if (column == 3) // date
                {
                    switch (((NavTreeUserObject) userObject).type)
                    {
                        case NavTreeUserObject.BOOKMARKS:
                            break;
                        case NavTreeUserObject.BOX:
                            break;
                        case NavTreeUserObject.COMPUTER:
                            break;
                        case NavTreeUserObject.DRIVE:
                            break;
                        case NavTreeUserObject.HOME:
                            break;
                        case NavTreeUserObject.LIBRARY:
                            break;
                        case NavTreeUserObject.REAL:
                            if (((NavTreeUserObject) userObject).file != null)
                            {
                                try
                                {
                                    return Files.getLastModifiedTime(((NavTreeUserObject) userObject).file.toPath());
                                }
                                catch (Exception e)
                                {
                                }
                            }
                            break;
                        case NavTreeUserObject.REMOTE:
                            return ((NavTreeUserObject) userObject).fileTime;
                        default:
                            return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error

                    }
                }
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

}
