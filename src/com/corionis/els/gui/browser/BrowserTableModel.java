package com.corionis.els.gui.browser;

import com.corionis.els.Context;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class BrowserTableModel extends DefaultTableModel
{
    Context context;
    private boolean initialized = false;
    private NavTreeNode node;

    public BrowserTableModel(Context context)
    {
        super();
        this.context = context;
    }

    public int findNavTreeUserObjectIndex(NavTreeNode find)
    {
        int size = node.getChildCount(false, true);
        for (int i = 0; i < size; ++i)
        {
            if ((NavTreeNode) node.getChildAt(i, false, true) == find)
                return i;
        }
        return -1;
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
                return SizeColumn.class;
            case 3:
                return DateColumn.class;
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
                return context.cfg.gs("BrowserTable.column.name");
            case 2:
                return context.cfg.gs("BrowserTable.column.size");
            case 3:
                return context.cfg.gs("BrowserTable.column.modified");
        }
        return context.cfg.gs("NavTreeNode.unknown");
    }

    public NavTreeNode getNode()
    {
        return node;
    }

    @Override
    public int getRowCount()
    {
        if (node != null)
            return node.getChildCount(false, true);
        return 0;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        return getValueAt(row, column, false, true);
    }

    private Object getValueAt(int row, int column, boolean hideFilesInTreeFilterActive, boolean hideHiddenFilterActive)
    {
        NavTreeNode child;
        NavTreeUserObject tuo;

        if (row >= node.getChildCount(hideFilesInTreeFilterActive, hideHiddenFilterActive))
            return null;

        child = (NavTreeNode) node.getChildAt(row, hideFilesInTreeFilterActive, hideHiddenFilterActive);
        if (child == null)
            return null;

        tuo = child.getUserObject();
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
            {
                if (tuo.name.length() == 0)
                    System.out.println("empty name");
                return tuo;
            }

            if (column == 2) // size
            {
                switch (tuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                    case NavTreeUserObject.COLLECTION:
                    case NavTreeUserObject.COMPUTER:
                    case NavTreeUserObject.HOME:
                    case NavTreeUserObject.LIBRARY:
                        return new SizeColumn(Long.valueOf(child.getChildCount(false, true)), context.cfg.getLongScale(), true);
                    case NavTreeUserObject.DRIVE:
                        return null; // with lazy loading there are no children
                    case NavTreeUserObject.REAL:
                        if (tuo.isDir)
                            return null; // with lazy loading there are no children
                        return new SizeColumn(tuo.size, context.cfg.getLongScale());
                    case NavTreeUserObject.SYSTEM:
                        return null;
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
                        return new DateColumn(context, tuo.fileTime);
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

    public boolean isInitialized()
    {
        return initialized;
    }

    public void setInitialized(boolean initialized)
    {
        this.initialized = initialized;
    }

    public void setNode(NavTreeNode treeNode)
    {
        node = treeNode;
    }

}
