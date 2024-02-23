package com.corionis.els.gui.util;

import com.corionis.els.Context;

import javax.swing.table.DefaultTableModel;

public abstract class TooltipsTableModel extends DefaultTableModel
{
    Context context = null;

    private TooltipsTableModel()
    {
        // hide default constructor
    }

    public TooltipsTableModel(Context context)
    {
        this.context = context;
    }

    public abstract int ttGetRowCount();

    public abstract Object ttGetValueAt(int row, int column);

}
