package com.corionis.els.gui.browser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

public class NavTreeModel extends DefaultTreeModel
{
    protected boolean filterIsActive;
    private transient Logger logger = LogManager.getLogger("applog");

    public NavTreeModel(TreeNode root, boolean asksAllowsChildren)
    {
        this(root, asksAllowsChildren, true);
    }

    public NavTreeModel(TreeNode root, boolean asksAllowsChildren, boolean filterIsActive)
    {
        super(root, asksAllowsChildren);
        this.filterIsActive = filterIsActive;
    }

    public void activateFilter(boolean newValue)
    {
        filterIsActive = newValue;
    }

    @Override
    public Object getChild(Object parent, int index)
    {
        Object child;
        if (parent instanceof NavTreeNode)
        {
            child = ((NavTreeNode) parent).getChildAt(index, true, true);
            assert(child != null);
            return child;
        }
        child = ((NavTreeNode) parent).getChildAt(index);
        assert(child != null);
        return child;
    }

    @Override
    public int getChildCount(Object parent)
    {
        if (parent instanceof NavTreeNode)
        {
            return ((NavTreeNode) parent).getChildCount(true, true);
        }
        return ((NavTreeNode) parent).getChildCount();
    }

    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        return ((NavTreeNode)parent).findChildIndex((NavTreeNode) child, true, true);
    }

/* Does not work due to ELS lazy-loading of content
    @Override
    public boolean isLeaf(Object node)
    {
        boolean hasSubDirs = false;
        for (int i = 0; i < ((NavTreeNode)node).getChildCount(); i++)
        {
            NavTreeNode ntn = (NavTreeNode) ((NavTreeNode)node).getChildAt(i);
            if (ntn.getUserObject().isDir)
            {
                hasSubDirs = true;
                break;
            }
        }
        return !hasSubDirs;
    }
*/

    public boolean isActivatedFilter()
    {
        return filterIsActive;
    }

}
