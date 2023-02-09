package com.groksoft.els.gui.jobs;

import com.groksoft.els.Context;
import com.groksoft.els.jobs.Job;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;

public class ConfigModel extends DefaultTableModel
{
    private Context context;
    private JobsUI myDialog;

    public ConfigModel(Context context, JobsUI ui)
    {
        super();
        this.context = context;
        this.myDialog = ui;
    }

    /**
     * Find a Job in the table
     *
     * @param configName Job configuration name to find
     * @param jrt        If not null that Job is skipped (a duplicate check)
     * @return Job found, or null if not found
     */
    public Job find(String configName, Job jrt)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((Job) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                Job value = (Job) getValueAt(i, 0);
                if (jrt == null || jrt != value)
                {
                    return value;
                }
            }
        }
        return null;
    }


    @Override
    public void setValueAt(Object object, int row, int column)
    {
        updateListName((String) object, row);
    }

    private boolean updateListName(String name, int index)
    {
        boolean success = false;
        if (index >= 0)
        {
            Job job = (Job) getValueAt(index, 0);
            if (job != null)
            {
                Job tmp = find(name, job);
                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(myDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!job.getConfigName().equals(name))
                    {
                        File file = new File(job.getFullPath());
                        if (file.exists())
                        {
                            myDialog.getDeletedJobs().add(job.clone());
                        }
                        job.setConfigName(name);
                        job.setDataHasChanged();
                    }
                    success = true;
                }
            }
        }

        myDialog.getConfigItems().requestFocus();
        myDialog.getConfigItems().changeSelection(index, 0, false, false);

        return success;
    }

}
