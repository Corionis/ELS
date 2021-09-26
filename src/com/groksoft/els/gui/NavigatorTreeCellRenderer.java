package com.groksoft.els.gui;

import com.groksoft.els.repository.Item;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Tree Cell Renderer class for System tree
 */
public class NavigatorTreeCellRenderer extends DefaultTreeCellRenderer
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
        if (value instanceof NavigatorNode)
        {
            NavigatorNode node = (NavigatorNode) value;
            if (node.getUserObject() instanceof String)
            {
                setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
            }
            else if (node.getUserObject() instanceof TreeUserObject)
            {
                TreeUserObject tso = (TreeUserObject) node.getUserObject();
                switch (tso.type)
                {
                    case TreeUserObject.BOOKMARKS:
                        setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                        break;
                    case TreeUserObject.BOX:
                        break;
                    case TreeUserObject.COMPUTER:
                        setIcon(UIManager.getIcon("FileView.computerIcon"));
                        break;
                    case TreeUserObject.DRIVE:
                        setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
                        break;
                    case TreeUserObject.HOME:
                        setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
                        break;
                    case TreeUserObject.LIBRARY:
                        setIcon(UIManager.getIcon("FileChooser.directoryIcon"));
                        break;
                    case TreeUserObject.REAL:
                        if (tso.file != null && tso.file.isDirectory())
                            setIcon(UIManager.getIcon("FileChooser.directoryIcon"));
                        else
                            setIcon(UIManager.getIcon("FileView.fileIcon"));
                        break;
                    case TreeUserObject.REMOTE:
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
