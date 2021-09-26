package com.groksoft.els.gui;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class NavigatorNode extends DefaultMutableTreeNode
{
    private boolean isVisible = true;
    private boolean refresh = true;

    public NavigatorNode()
    {
        this(null);
    }

    public NavigatorNode(Object userObject)
    {
        this(userObject, true, true);
    }

    public NavigatorNode(Object userObject, boolean allowsChildren, boolean isVisible)
    {
        super(userObject, allowsChildren);
        this.isVisible = isVisible;
    }

    public TreeNode getChildAt(int index, boolean filterIsActive)
    {
        if (!filterIsActive)
        {
            return super.getChildAt(index);
        }

        if (children == null)
        {
            throw new ArrayIndexOutOfBoundsException("node has no children");
        }

        int realIndex = -1;
        int visibleIndex = -1;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            NavigatorNode node = (NavigatorNode) e.nextElement();
            if (node.isVisible())
            {
                visibleIndex++;
            }

            realIndex++;
            if (visibleIndex == index)
            {
                return (TreeNode) children.elementAt(realIndex);
            }
        }

        throw new ArrayIndexOutOfBoundsException("index unmatched");
        //return (TreeNode)children.elementAt(index);
    }

    public int getChildCount(boolean filterIsActive)
    {
        if (!filterIsActive)
        {
            return super.getChildCount();
        }

        if (children == null)
        {
            return 0;
        }

        int count = 0;
        Enumeration e = children.elements();
        while (e.hasMoreElements())
        {
            NavigatorNode node = (NavigatorNode) e.nextElement();
            if (node.isVisible())
            {
                count++;
            }
        }

        return count;
    }

    public TreePath getTreePath()
    {
        List<Object> nodes = new ArrayList<Object>();
        NavigatorNode treeNode;
        nodes.add(this);
        treeNode = (NavigatorNode) this.getParent();
        while (treeNode != null)
        {
            nodes.add(0, treeNode);
            treeNode = (NavigatorNode) treeNode.getParent();
        }
        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

    public boolean isRefresh()
    {
        return refresh;
    }

    public boolean isVisible()
    {
        return isVisible;
    }

    public void setRefresh(boolean refresh)
    {
        this.refresh = refresh;
    }

    public void setVisible(boolean visible)
    {
        this.isVisible = visible;
    }

}
