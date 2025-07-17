package com.corionis.els.gui.bookmarks;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.hints.HintsUI;

import javax.swing.table.DefaultTableModel;

public class BookmarksTableModel  extends DefaultTableModel
{
    Context context;
    Bookmarks bookmarks;

    private BookmarksTableModel()
    {

    }

    public BookmarksTableModel(Context context, Bookmarks bookmarks)
    {
        super();
        this.context = context;
        this.bookmarks = bookmarks;
    }

    @Override
    public Class getColumnClass(int column)
    {
        switch (column)
        {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
        }
        return String.class;
    }

    @Override
    public int getColumnCount()
    {
        return 3;
    }

    @Override
    public String getColumnName(int column)
    {
        String name = "";
        switch (column)
        {
            case 0:
                name = context.cfg.gs("BookmarksUI.header.tab");
                break;
            case 1:
                name = context.cfg.gs("BookmarksUI.header.name");
                break;
            case 2:
                name = context.cfg.gs("BookmarksUI.header.path");
                break;
        }
        return name;
    }

    @Override
    public int getRowCount()
    {
        return (bookmarks == null) ? 0 : bookmarks.size();
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        Bookmark bm = null;
        if (row < bookmarks.size())
        {
             bm = (Bookmark) bookmarks.get(row);
            switch (column)
            {
                case 0:
                    switch (bm.panel) // tab
                    {
                        case "treeCollectionOne":
                        case "tableCollectionOne":
                            return "   1";
                        case "treeSystemOne":
                        case "tableSystemOne":
                            return "   2";
                        case "treeCollectionTwo":
                        case "tableCollectionTwo":
                            return "   3";
                        case "treeSystemTwo":
                        case "tableSystemTwo":
                            return "   4";
                    }
                    break;
                case 1: // name
                    return bm.name;
                case 2: // path
                    return Utils.concatStringArray(bm.pathElements, "/");
            }
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int row, int column)
    {
        return column == 1;
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (column == 1)
        {
            Bookmark bm = (Bookmark) bookmarks.get(row);
            bm.name = (String) object;
        }
    }

}
