package com.groksoft.els.gui;

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

    public boolean isActivatedFilter()
    {
        return filterIsActive;
    }

}
