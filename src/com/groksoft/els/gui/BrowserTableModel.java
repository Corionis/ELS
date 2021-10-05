package com.groksoft.els.gui;

import com.groksoft.els.repository.Item;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;
import java.nio.file.attribute.FileTime;

public class BrowserTableModel extends AbstractTableModel
{
    TreeModel model;
    DefaultMutableTreeNode node;
    JTree tree;

    public BrowserTableModel(JTree tr, DefaultMutableTreeNode start)
    {
        tree = tr;
        node = start;
        model = tree.getModel();
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
        return node.getChildCount();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        TreeNode child = node.getChildAt(row);
        Object userObject = ((DefaultMutableTreeNode) child).getUserObject();
        if (userObject != null)
        {
            if (userObject instanceof String)
            {
                if (column == 0)
                    return UIManager.getIcon("FileView.computerIcon");
                if (column == 1)
                    return userObject;
                if (column == 2)
                    return Long.valueOf(child.getChildCount());
            }
            else if (userObject instanceof NavTreeUserObject)
            {
                if (column == 0)
                {
                    switch (((NavTreeUserObject)userObject).type)
                    {
                        case NavTreeUserObject.BOX:
                            break;
                        case NavTreeUserObject.COMPUTER:
                            return UIManager.getIcon("FileView.computerIcon");
                        case NavTreeUserObject.DRIVE:
                            return UIManager.getIcon("FileView.hardDriveIcon");
                        case NavTreeUserObject.HOME:
                            return UIManager.getIcon("FileChooser.homeFolderIcon");
                        case NavTreeUserObject.BOOKMARKS:
                            return UIManager.getIcon("FileView.floppyDriveIcon");
                    }
                }
                if (column == 1)
                    return ((NavTreeUserObject) userObject).name;
                if (column == 2)
                    return Long.valueOf(child.getChildCount());
            }
            else if (userObject instanceof Item)
            {
                if (column == 0)
                {
                    if (((Item)userObject).isDirectory())
                        return UIManager.getIcon("FileView.directoryIcon");
                    else
                        return UIManager.getIcon("FileView.fileIcon");
                }
                if (column == 1)
                    return ((Item) userObject).getItemShortName();
                if (column == 2)
                    return Long.valueOf(((Item) userObject).getSize());
                if (column == 3)
                    return ((Item) userObject).getModifiedDate();
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int col)
    {
        return false;
    }

} // private class BrowserTableModel
