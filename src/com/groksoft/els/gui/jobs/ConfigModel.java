package com.groksoft.els.gui.jobs;

import com.groksoft.els.Context;
import com.groksoft.els.jobs.Conflict;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.jobs.Jobs;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.util.ArrayList;

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
                    success = false;
                    JOptionPane.showMessageDialog(myDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else if (context.navigator.dialogJobs != null &&
                        context.navigator.dialogJobs.checkForChanges() == true) // todo ??????????????????
                {
                    success = false;
                    JOptionPane.showMessageDialog(myDialog,
                            context.cfg.gs(("Please save Jobs changes before renaming this Job")),
                            context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed ...
                    if (!job.getConfigName().equals(name))
                    {
                        // see if anything depends on the old name
                        Jobs jobsHandler = new Jobs(context);
                        int condition = jobsHandler.checkConflicts(myDialog, true, job);
                        if (condition == 1)
                        {
                            // handle conflicts
                            ArrayList<Conflict> conflicts = jobsHandler.getConflicts();

                        }

                        if (condition >= 0)
                        {
                            // see if that name is to be deleted
                            boolean restored = false;
                            for (Job dj : myDialog.getDeletedJobs())
                            {
                                if (dj.getConfigName().equals(name))
                                {
                                    // remove from delete list
                                    myDialog.getDeletedJobs().remove(dj);
                                    restored = true;
                                    break;
                                }
                            }

                            if (!restored)
                            {
                                // add to delete list if file exists
                                File file = new File(job.getFullPath());
                                if (file.exists())
                                {
                                    myDialog.getDeletedJobs().add(job.clone());
                                }
                            }

                            job.setConfigName(name);
                            job.setDataHasChanged();
                            context.mainFrame.labelStatusMiddle.setText("");
                            success = true;
                        }
                        else
                        {
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.rename") + context.cfg.gs("Z.cancelled"));
                            success = false;
                        }
                    }
                }
            }
        }

        myDialog.getConfigItems().requestFocus();
        myDialog.getConfigItems().changeSelection(index, 0, false, false);

        return success;
    }

}
