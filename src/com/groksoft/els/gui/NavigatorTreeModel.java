package com.groksoft.els.gui;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;

class NavigatorTreeModel extends DefaultTreeModel
{
    protected boolean filterIsActive;

    public NavigatorTreeModel(TreeNode root)
    {
        this(root, false);
    }

    public NavigatorTreeModel(TreeNode root, boolean asksAllowsChildren)
    {
        this(root, asksAllowsChildren, false);
    }

    public NavigatorTreeModel(TreeNode root, boolean asksAllowsChildren, boolean filterIsActive)
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
        if (filterIsActive)
        {
            if (parent instanceof NavigatorNode)
            {
                return ((NavigatorNode) parent).getChildAt(index, filterIsActive);
            }
        }
        return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent)
    {
        if (filterIsActive)
        {
            if (parent instanceof NavigatorNode)
            {
                return ((NavigatorNode) parent).getChildCount(filterIsActive);
            }
        }
        return ((TreeNode) parent).getChildCount();
    }

    public boolean isActivatedFilter()
    {
        return filterIsActive;
    }

}
