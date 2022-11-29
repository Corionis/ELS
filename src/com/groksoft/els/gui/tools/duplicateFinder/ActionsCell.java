package com.groksoft.els.gui.tools.duplicateFinder;

import com.groksoft.els.gui.GuiContext;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.EventObject;

public class ActionsCell extends AbstractCellEditor implements TableCellEditor, TableCellRenderer
{
    GuiContext guiContext;
    DuplicateFinderUI ui;
    JButton buttonGoto = new JButton();

    public ActionsCell(GuiContext guiContext, DuplicateFinderUI ui)
    {
        super();
        this.guiContext = guiContext;
        this.ui = ui;

        //---- buttonGoto ----
        buttonGoto.setText(guiContext.cfg.gs("DuplicateFinder.goto"));
        buttonGoto.setToolTipText(guiContext.cfg.gs("DuplicateFinder.go.to.this.item.in.the.browser"));
        buttonGoto.setActionCommand("goto");
        buttonGoto.setFont(buttonGoto.getFont().deriveFont(buttonGoto.getFont().getSize() - 2f));
        buttonGoto.setMaximumSize(new Dimension(68, 24));
        buttonGoto.setMinimumSize(new Dimension(68, 24));
        buttonGoto.setPreferredSize(new Dimension(68, 24));
        buttonGoto.addActionListener(e -> ui.gotoItem(e));
    }

    @Override
    public Object getCellEditorValue()
    {
        return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable jTable, Object value, boolean isSelected, int row, int column)
    {
        Dupe dupe = (Dupe)value;
        if (dupe == null || dupe.item == null || dupe.isTop)
            return null;
        return buttonGoto;
    }

    @Override
    public Component getTableCellRendererComponent(JTable jTable, Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        Dupe dupe = (Dupe)value;
        if (dupe == null || dupe.item == null || dupe.isTop)
            return null;
        return buttonGoto;
    }

    @Override
    public boolean shouldSelectCell(EventObject e)
    {
        return true;
    }

}
