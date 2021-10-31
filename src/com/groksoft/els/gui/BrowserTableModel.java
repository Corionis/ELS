package com.groksoft.els.gui;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;

public class BrowserTableModel extends AbstractTableModel
{
    NavTreeNode node;

    public BrowserTableModel(NavTreeNode start)
    {
        super();
        node = start;
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
        NavTreeNode child = (NavTreeNode) node.getChildAt(row, false);
        NavTreeUserObject tuo = child.getUserObject();
        if (tuo != null)
        {
            if (column == 0) // icon
            {
                switch (((NavTreeUserObject) tuo).type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        return UIManager.getIcon("FileView.floppyDriveIcon");
                    case NavTreeUserObject.COLLECTION:
                        return UIManager.getIcon("FileChooser.homeFolderIcon");
                    case NavTreeUserObject.COMPUTER:
                        return UIManager.getIcon("FileView.computerIcon");
                    case NavTreeUserObject.DRIVE:
                        return UIManager.getIcon("FileView.hardDriveIcon");
                    case NavTreeUserObject.HOME:
                        return UIManager.getIcon("FileChooser.homeFolderIcon");
                    case NavTreeUserObject.LIBRARY:
                        return UIManager.getIcon("FileView.directoryIcon");
                    case NavTreeUserObject.REAL:
                        if (((NavTreeUserObject) tuo).file != null && ((NavTreeUserObject) tuo).file.isDirectory())
                            return UIManager.getIcon("FileView.directoryIcon");
                        else
                            return UIManager.getIcon("FileView.fileIcon");
                    case NavTreeUserObject.REMOTE:
                        if (((NavTreeUserObject) tuo).isDir)
                            return UIManager.getIcon("FileView.directoryIcon");
                        else
                            return UIManager.getIcon("FileView.fileIcon");
                    case NavTreeUserObject.SYSTEM:
                        break;
                    default:
                        return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error
                }
            }
            if (column == 1) // name
                return ((NavTreeUserObject) tuo).name;
            if (column == 2) // size
            {
                switch (((NavTreeUserObject) tuo).type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        return Long.valueOf(child.getChildCount(false));
                    case NavTreeUserObject.COLLECTION:
                        return Long.valueOf(child.getChildCount(false));
                    case NavTreeUserObject.COMPUTER:
                        return Long.valueOf(child.getChildCount(false));
                    case NavTreeUserObject.DRIVE:
                        return null; // Long.valueOf(child.getChildCount());
                    case NavTreeUserObject.HOME:
                        return Long.valueOf(child.getChildCount(false));
                    case NavTreeUserObject.LIBRARY:
                        return Long.valueOf(child.getChildCount(false));
                    case NavTreeUserObject.REAL:
                        if (((NavTreeUserObject) tuo).file != null)
                        {
                            if (((NavTreeUserObject) tuo).file.isDirectory())
                                return null; // Long.valueOf(child.getChildCount());
                            try
                            {
                                long size = Files.size(((NavTreeUserObject) tuo).file.toPath());
                                return size;
                            }
                            catch (Exception e)
                            {
                                return -1L;
                            }
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        if (((NavTreeUserObject) tuo).isDir)
                            return null;
                        return ((NavTreeUserObject) tuo).size;
                    case NavTreeUserObject.SYSTEM:
                        return null;
                    default:
                        return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error

                }
            }
            if (column == 3) // date
            {
                switch (((NavTreeUserObject) tuo).type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        break;
                    case NavTreeUserObject.COLLECTION:
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
                        if (((NavTreeUserObject) tuo).file != null)
                        {
                            try
                            {
                                return Files.getLastModifiedTime(((NavTreeUserObject) tuo).file.toPath());
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        return ((NavTreeUserObject) tuo).fileTime;
                    case NavTreeUserObject.SYSTEM:
                        break;
                    default:
                        return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error

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
