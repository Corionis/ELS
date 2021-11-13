package com.groksoft.els.gui;

import com.groksoft.els.Utils;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;

public class BrowserTableModel extends AbstractTableModel
{
    private SimpleDateFormat dateFormatter;
    private NavTreeNode node;

    public BrowserTableModel(NavTreeNode treeNode)
    {
        super();
        node = treeNode;
        dateFormatter = new SimpleDateFormat(Navigator.guiContext.preferences.getDateFormat());
    }

    private String formatFileTime(FileTime stamp)
    {
        if (stamp != null)
            return dateFormatter.format(stamp.toMillis());
        return "";
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return Icon.class;
            case 1:
                return NavTreeUserObject.class;
            case 2:
                return String.class;  // Long.class;
            case 3:
                return String.class;  // return formatted FileTime
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
                switch (tuo.type)
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
                        if (tuo.file != null && tuo.file.isDirectory())
                            return UIManager.getIcon("FileView.directoryIcon");
                        else
                            return UIManager.getIcon("FileView.fileIcon");
                    case NavTreeUserObject.REMOTE:
                        if (tuo.isDir)
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
                return tuo;

            if (column == 2) // size
            {
                switch (tuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                    case NavTreeUserObject.COLLECTION:
                    case NavTreeUserObject.COMPUTER:
                    case NavTreeUserObject.HOME:
                    case NavTreeUserObject.LIBRARY:
                        return Long.valueOf(child.getChildCount(false)) + " items";
                    case NavTreeUserObject.DRIVE:
                        return null;
                    case NavTreeUserObject.REAL:
                        if (tuo.file != null)
                        {
                            if (tuo.file.isDirectory())
                                return null; // Long.valueOf(child.getChildCount());
                            try
                            {
                                long size = Files.size(tuo.file.toPath());
                                return Utils.formatLong(size, false);
                            }
                            catch (Exception e)
                            {
                                return -1L;
                            }
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        if (tuo.isDir)
                            return null;
                        return Utils.formatLong(tuo.size, false);
                    case NavTreeUserObject.SYSTEM:
                        return null;
                    default:
                        return UIManager.getIcon("InternalFrame.closeIcon"); // something that looks like an error

                }
            }

            if (column == 3) // date
            {
                switch (tuo.type)
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
                        if (tuo.file != null)
                        {
                            try
                            {
                                return formatFileTime(Files.getLastModifiedTime(tuo.file.toPath()));
                            }
                            catch (Exception e)
                            {
                            }
                        }
                        break;
                    case NavTreeUserObject.REMOTE:
                        return formatFileTime(tuo.fileTime);
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
