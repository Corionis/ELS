package com.groksoft.els.jobs;

import com.groksoft.els.gui.browser.NavTreeUserObject;

import javax.swing.*;
import javax.swing.tree.TreePath;

public class Origin
{
    private String name;
    private int type;

    transient NavTreeUserObject tuo;
    
    transient JTree sourceTree = null;
    transient TreePath treePath = null;
    
    transient JTable sourceTable = null;
    transient int tableRow = -1;

    private Origin()
    {
    }

    public Origin(JTree sourceTree, TreePath treePath, NavTreeUserObject tuo)
    {
        this.sourceTree = sourceTree;
        this.treePath = treePath;
        this.tuo = tuo;
        this.name = tuo.getPath();
        this.type = tuo.type;
    }

    public Origin(JTable sourceTable, int tableRow, NavTreeUserObject tuo)
    {
        this.sourceTable = sourceTable;
        this.tableRow = tableRow;
        this.tuo = tuo;
        this.name = tuo.getPath();
        this.type = tuo.type;
    }

/*
    public Origin(String name, int type)
    {
        this.name = name;
        this.type = type;
    }
*/

    public Origin clone()
    {
        Origin o = new Origin();
        o.sourceTree = this.sourceTree;
        o.treePath = this.treePath;
        o.sourceTable = this.sourceTable;
        o.tableRow = this.tableRow;
        o.tuo = this.tuo;
        o.name = this.name;
        o.type = this.type;
        return o;
    }

    public String getName()
    {
        return name;
    }

    public int getType()
    {
        return type;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public void setType(int type)
    {
        this.type = type;
    }

}
