package com.corionis.els.gui.tools.email;

import com.corionis.els.Context;
import com.corionis.els.Utils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class EmailTemplatesModel extends DefaultTableModel
{
    Context context;
    EmailTemplates theDialog;

    public EmailTemplatesModel(Context context, EmailTemplates theDialog)
    {
        this.context = context;
        this.theDialog = theDialog;
    }

    public Template find(String configName)
    {
        Template value = null;
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((Template) getValueAt(i, 0)).getFileName().equalsIgnoreCase(configName))
            {
                value = (Template) getValueAt(i, 0);
                break;
            }
        }
        return value;
    }

    @Override
    public Object getValueAt(int row, int column)
    {
        if (column == 0)
            return super.getValueAt(row, column);
        else
        {
            Template template = (Template) getValueAt(row, 0);
            if (template != null && template.isChanged())
                return "*";
        }
        return null;
    }

    /**
     * Override: Set a config name, check for conflicts & handle deletions
     *
     * @param object
     * @param row
     * @param column
     */
    @Override
    public void setValueAt(Object object, int row, int column)
    {
        if (column == 0)
            updateConfigName((String) object, row);
    }

    /**
     * Check for Tool already existing, conflicts with Jobs, and handle deletions
     *
     * @param newName New name for Tool
     * @param index   ConfigModel row index
     */
    protected void updateConfigName(String newName, int index)
    {
        if (index >= 0)
        {
            String scrubbed = Utils.scrubFilename(newName);
            if (!newName.equals(scrubbed))
            {
                JOptionPane.showMessageDialog(theDialog,
                        context.cfg.gs((context.cfg.gs("JobsUI.job.name.may.not.contain.these.characters"))),
                        theDialog.getTitle(), JOptionPane.WARNING_MESSAGE);
                theDialog.getConfigItems().requestFocus();
                theDialog.getConfigItems().changeSelection(index, 0, false, false);
                return;
            }

            Template template = (Template) getValueAt(index, 0);
            if (template != null)
            {
                Template tmp = (Template) find(newName);
                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(theDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            theDialog.getTitle(), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed ...
                    if (!template.getFileName().equals(newName))
                    {
                        // add to delete list if file exists
                        File file = new File(template.getFullPath(template.getFileName()));
                        if (file.exists())
                        {
                            theDialog.getDeletedTemplates().add((Template) template.clone());
                        }
                    }

                    template.setFileName(newName);
                    template.setChanged();

                    context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                }
            }
        }

        theDialog.getConfigItems().requestFocus();
        theDialog.getConfigItems().changeSelection(index, 0, false, false);
    }

}
