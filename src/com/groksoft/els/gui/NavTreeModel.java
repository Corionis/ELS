package com.groksoft.els.gui;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

class NavTreeModel extends DefaultTreeModel
{
    protected boolean filterIsActive;

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

    public Object getChild(Object parent, int index)
    {
        if (parent instanceof NavTreeNode)
        {
            return ((NavTreeNode) parent).getChildAt(index, filterIsActive, true);
        }
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent)
    {
        if (parent instanceof NavTreeNode)
        {
            return ((NavTreeNode) parent).getChildCount(filterIsActive, true);
        }
        return ((TreeNode) parent).getChildCount();
    }

    public boolean isActivatedFilter()
    {
        return filterIsActive;
    }

}
