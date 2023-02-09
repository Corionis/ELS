package com.groksoft.els.gui.browser;

import com.groksoft.els.Context;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

/**
 * Tree Cell Renderer class for System tree
 */
public class NavTreeCellRenderer extends DefaultTreeCellRenderer
{
    Context context;
    
    public NavTreeCellRenderer(Context context)
    {
        this.context = context;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);

        if (tree.isEnabled() && value instanceof NavTreeNode)
        {
            NavTreeNode node = (NavTreeNode) value;
            if (node.getUserObject() instanceof NavTreeUserObject)
            {
                NavTreeUserObject tuo = (NavTreeUserObject) node.getUserObject();

                switch (tuo.type)
                {
                    case NavTreeUserObject.BOOKMARKS:
                        setIcon(UIManager.getIcon("FileView.floppyDriveIcon"));
                        break;
                    case NavTreeUserObject.COLLECTION:
                        setIcon(UIManager.getIcon("FileChooser.homeFolderIcon")); // collection root
                        setToolTipText((tuo.isRemote ? context.cfg.gs("Z.remote.uppercase") : context.cfg.gs("NavTreeNode.local")) +
                                (!tuo.isSubscriber() && context.preferences.isLastIsWorkstation() ? context.cfg.gs("Navigator.workstation") : context.cfg.gs("NavTreeNode.collection")) +
                                ", " + tuo.node.getChildCount() + context.cfg.gs("Navigator.libraries"));
                        break;
                    case NavTreeUserObject.COMPUTER:
                        setIcon(UIManager.getIcon("FileView.computerIcon"));
                        setToolTipText((tuo.isRemote ? context.cfg.gs("Z.remote.uppercase") : context.cfg.gs("NavTreeNode.local")));
                        break;
                    case NavTreeUserObject.DRIVE:
                        setIcon(UIManager.getIcon("FileView.hardDriveIcon"));
                        setToolTipText(tuo.path);
                        break;
                    case NavTreeUserObject.HOME:
                        setIcon(UIManager.getIcon("FileChooser.homeFolderIcon"));
                        setToolTipText(tuo.path);
                        break;
                    case NavTreeUserObject.LIBRARY:
                        setIcon(UIManager.getIcon("FileView.directoryIcon"));
                        setToolTipText(context.cfg.gs("NavTreeNode.library") + ", " + tuo.sources.length + (tuo.sources.length == 1 ? context.cfg.gs("NavTreeNode.source") : context.cfg.gs("NavTreeNode.sources")));
                        break;
                    case NavTreeUserObject.REAL:
                        setToolTipText(tuo.path);
                        if (tuo.isDir)
                            setIcon(UIManager.getIcon("FileView.directoryIcon"));
                        else
                            setIcon(UIManager.getIcon("FileView.fileIcon"));
                        break;
                    case NavTreeUserObject.SYSTEM:
                        // hidden node
                        break;
                    default:
                        setIcon(UIManager.getIcon("InternalFrame.closeIcon")); // something that looks like an error
                        break;
                }
            }
            else
            {
                setIcon(UIManager.getIcon("InternalFrame.closeIcon")); // something that looks like an error
            }
        }
        else
        {
            setIcon(UIManager.getIcon("InternalFrame.closeIcon")); // something that looks like an error
        }
        return this;
    }
}
