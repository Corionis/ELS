package com.groksoft.els.gui;

import com.groksoft.els.repository.Item;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Tree Cell Renderer class for System tree
 */
public class NavTreeCellRenderer extends DefaultTreeCellRenderer
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
        if (value instanceof NavTreeNode)
        {
            NavTreeNode node = (NavTreeNode) value;
            if (node.getUserObject() instanceof String)
            {
                setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
            }
            else if (node.getUserObject() instanceof NavTreeUserObject)
            {
                NavTreeUserObject tso = (NavTreeUserObject) node.getUserObject();
                switch (tso.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                        break;
                    case NavTreeUserObject.BOX:
                        break;
                    case NavTreeUserObject.COMPUTER:
                        setIcon(UIManager.getIcon("FileView.computerIcon"));
                        break;
                    case NavTreeUserObject.DRIVE:
                        setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
                        break;
                    case NavTreeUserObject.HOME:
                        setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
                        break;
                    case NavTreeUserObject.LIBRARY:
                        setIcon(UIManager.getIcon("FileChooser.directoryIcon"));
                        break;
                    case NavTreeUserObject.REAL:
                        if (tso.file != null && tso.file.isDirectory())
                            setIcon(UIManager.getIcon("FileChooser.directoryIcon"));
                        else
                            setIcon(UIManager.getIcon("FileView.fileIcon"));
                        break;
                    case NavTreeUserObject.REMOTE:
                        if (tso.isDir)
                            setIcon(UIManager.getIcon("FileChooser.directoryIcon"));
                        else
                            setIcon(UIManager.getIcon("FileView.fileIcon"));
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
                    setIcon(UIManager.getIcon("FileView.fileIcon"));
                }
            }
        }
        return this;
    }
}
