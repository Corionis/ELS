package com.corionis.els.gui.jobs;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.jobs.Job;
import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.corionis.els.gui.util.DisableJListSelectionModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

/**
 * ConfigModel class
 * <p></p>
 * <p>Used by Tools, including Jobs, to manage Job references</p>
 */
public class ConfigModel extends DefaultTableModel
{
    private ArrayList<Conflict> conflicts = null;
    private Context context;
    private ConfigModel jobsConfigModel;
    private Logger logger = LogManager.getLogger("applog");
    private AbstractToolDialog theDialog;
    private ArrayList<AbstractTool> toolList = null;

    /**
     * Constructor
     *
     * @param context    The Context
     * @param toolDialog The Tool dialog
     */
    public ConfigModel(Context context, AbstractToolDialog toolDialog)
    {
        super();
        this.context = context;
        this.theDialog = toolDialog;
    }

    /**
     * Check for tool conflicts in Jobs before rename or delete
     *
     * @param oldName       The old name of the Job configuration
     * @param newName       The new name, null for delete
     * @param internalName, The Tool/Job internal name
     * @param isRename      Is a rename operation = true, else false
     * @return int -1 = cancel, 0 = no conflicts, 1 = rename/delete
     */
    public int checkJobConflicts(String oldName, String newName, String internalName, boolean isRename)
    {
        int answer = 0;
        JList<String> conflictJList = new JList<String>();

        try
        {
            // get list of conflicts
            int count = getJobReferences(oldName, newName, internalName);
            if (count > 0)
            {
                answer = -1;

                // make list of displayable conflict names
                ArrayList<String> conflictNames = new ArrayList<>();
                for (Conflict conflict : conflicts)
                {
                    conflictNames.add(conflict.toString(context));
                }
                Collections.sort(conflictNames);

                // add the Strings to the JList model
                DefaultListModel<String> dialogList = new DefaultListModel<String>();
                for (String name : conflictNames)
                {
                    dialogList.addElement(name);
                }
                conflictJList.setModel(dialogList);
                conflictJList.setSelectionModel(new DisableJListSelectionModel());

                String message = context.cfg.gs("References for \"" + oldName + "\" found in Jobs:       ");
                JScrollPane pane = new JScrollPane();
                pane.setViewportView(conflictJList);
                String question = context.cfg.gs(isRename ? "Rename" : "Delete") + context.cfg.gs("LibraryUI.the.listed.references");
                Object[] params = {message, pane, question};

                int opt = JOptionPane.showConfirmDialog(theDialog, params, theDialog.getTitle(), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    answer = 1;
                    processJobConflicts();
                }
            }
        }
        catch (Exception e)
        {
            answer = -1;
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(theDialog, msg, theDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
        }

        return answer;
    }

    /**
     * Find a config tool in the table
     *
     * @param configName Configuration name to find
     * @param tool       If not null that OperationsUI is skipped (a duplicate check)
     * @return Config tools if found, or null if not found
     */
    public AbstractTool find(String configName, AbstractTool tool)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            if (((AbstractTool) getValueAt(i, 0)).getConfigName().equalsIgnoreCase(configName))
            {
                AbstractTool value = (AbstractTool) getValueAt(i, 0);
                if (tool == null || tool != value)
                {
                    return value;
                }
            }
        }
        return null;
    }

    /**
     * Get the full path to the Job JSON file
     *
     * @param job
     * @return Full path string
     */
    private String getFullJobPath(Job job)
    {
        String path = job.getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(job.getConfigName()) + ".json";
        return path;
    }

    public ConfigModel getJobsConfigModel()
    {
        return jobsConfigModel;
    }

    /**
     * Find all Job references for the old name and internal name & create a Conflict for each
     *
     * @param oldName      The old name
     * @param newName      The new name, may be null
     * @param internalName The Tool/Job internal name
     * @return
     */
    private int getJobReferences(String oldName, String newName, String internalName)
    {
        int count = 0;
        conflicts = new ArrayList<>();

        for (int i = 0; i < jobsConfigModel.getRowCount(); ++i)
        {
            Job job = (Job) jobsConfigModel.getValueAt(i, 0);

            for (int j = 0; j < job.getTasks().size(); ++j)
            {
                Task task = job.getTasks().get(j);
                if (task.getConfigName().equals(oldName) && task.getInternalName().equals(internalName))
                {
                    Conflict conflict = new Conflict();
                    conflict.job = job;
                    conflict.newName = newName;
                    conflict.taskNumber = j;
                    conflicts.add(conflict);
                    ++count;
                }
            }
        }
        return count;
    }

    /**
     * Load Jobs configurations used in parallel with the Tool ConfigModel
     *
     * @param theDialog       The Tool/Job dialog
     * @param configJobsModel The Jobs ConfigModel for JobsUI, otherwise null
     * @return The Jobs ConfigModel, used in JobsUI
     */
    public ConfigModel loadJobsConfigurations(AbstractToolDialog theDialog, ConfigModel configJobsModel)
    {
        if (configJobsModel == null)
        {
            configJobsModel = new ConfigModel(context, theDialog);
            configJobsModel.setColumnCount(1);
            this.jobsConfigModel = configJobsModel; // set internal
        }

        Job tmpJob = new Job(context, "temp");
        File jobsDir = new File(tmpJob.getDirectoryPath());
        if (jobsDir.exists())
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(java.lang.reflect.Type type)
                {
                    return new Job(context, "");
                }
            }
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Job.class, new objInstanceCreator());

            ArrayList<Job> jobsArray = new ArrayList<>();
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, true);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null && json.length() > 0)
                        {
                            Job job = builder.create().fromJson(json, Job.class);
                            if (job != null)
                            {
                                if (toolList != null)
                                {
                                    ArrayList<Task> tasks = job.getTasks();
                                    for (int i = 0; i < tasks.size(); ++i)
                                    {
                                        Task task = tasks.get(i);
                                        AbstractTool tool = context.tools.getTool(task.getInternalName(), task.getConfigName());
                                        if (tool != null)
                                        {

                                            task.setContext(context);
                                            tasks.set(i, task);
                                        }
                                    }
                                    jobsArray.add(job);
                                }
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        String msg = context.cfg.gs("Z.exception") + entry.getName() + " " + Utils.getStackTrace(e);
                        logger.error(msg);
                        JOptionPane.showMessageDialog(theDialog, msg,
                                theDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            Collections.sort(jobsArray);
            for (Job job : jobsArray)
            {
                configJobsModel.addRow(new Object[]{job});
            }
        }
        return configJobsModel;
    }

    /**
     * Process the conflicts by renaming or deleting Job tasks
     */
    private void processJobConflicts()
    {
        for (Conflict conflict : conflicts)
        {
            Job job = (Job) conflict.job;
            if (conflict.newName == null) // delete operation
            {
                job.getTasks().remove(conflict.taskNumber);
            }
            else // rename operation
            {
                Task task = job.getTasks().get(conflict.taskNumber);
                task.setContext(context);
                task.setConfigName(conflict.newName);
            }
            job.setDataHasChanged();
        }
    }

    /***
     * Save Jobs configurations JSON files
     *
     * @param configJobsModel The JobsUI ConfigModel, otherwise null in other Tools
     * @return true if changes detected, otherwise false
     */
    public boolean saveJobsConfigurations(ConfigModel configJobsModel)
    {
        boolean changed = false;
        Job job = null;
        try
        {
            if (configJobsModel == null)
                configJobsModel = jobsConfigModel;

            // write/update changed tool JSON configuration files
            for (int i = 0; i < configJobsModel.getRowCount(); ++i)
            {
                job = (Job) configJobsModel.getValueAt(i, 0);
                if (job.isDataChanged())
                {
                    String status = job.validate(context.cfg);
                    if (status.length() > 0)
                    {
                        JOptionPane.showMessageDialog(theDialog, status, theDialog.getTitle(), JOptionPane.WARNING_MESSAGE);
                    }
                    writeJob(job);
                    changed = true;
                    job.setDataHasChanged(false);
                }
            }

            if (changed)
            {
                context.navigator.loadJobsMenu();
                context.libraries.loadJobs();
            }
        }
        catch (Exception e)
        {
            String name = (job != null) ? job.getConfigName() + " " : " ";
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(theDialog,
                    context.cfg.gs("Z.error.writing") + name + e.getMessage(),
                    theDialog.getTitle(), JOptionPane.ERROR_MESSAGE);
        }
        return true;
    }

    /**
     * Used by JobsUI (only) so the model used by Conflict management is the same as it's model
     *
     * @param jobsConfigModel JobsUI ConfigModel
     */
    public void setJobsConfigModel(ConfigModel jobsConfigModel)
    {
        this.jobsConfigModel = jobsConfigModel;
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
            AbstractTool tool = (AbstractTool) getValueAt(index, 0);
            if (tool != null)
            {
                Job tmp = (Job) find(newName, tool);
                if (tmp != null)
                {
                    JOptionPane.showMessageDialog(theDialog,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            theDialog.getTitle(), JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed ...
                    if (!tool.getConfigName().equals(newName))
                    {
                        // see if anything depends on the old name
                        int answer = checkJobConflicts(tool.getConfigName(), newName, tool.getInternalName(), true);
                        if (answer >= 0)
                        {
                            // see if that name is to be deleted
                            boolean restored = false;
                            for (AbstractTool deletedTool : theDialog.getDeletedTools())
                            {
                                if (deletedTool.getConfigName().equals(newName))
                                {
                                    // remove from delete list
                                    theDialog.getDeletedTools().remove(deletedTool);
                                    restored = true;
                                    break;
                                }
                            }

                            if (!restored)
                            {
                                // add to delete list if file exists
                                File file = new File(tool.getFullPath());
                                if (file.exists())
                                {
                                    theDialog.getDeletedTools().add((AbstractTool) tool.clone());
                                }
                            }

                            tool.setConfigName(newName);
                            tool.setDataHasChanged();
                            context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                        }
                        else
                        {
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.rename") + context.cfg.gs("Z.cancelled"));
                        }
                    }
                }
            }
        }

        theDialog.getConfigItems().requestFocus();
        theDialog.getConfigItems().changeSelection(index, 0, false, false);
    }

    /**
     * Set the list of available Jobs and Tools
     *
     * @param toolList
     */
    public void setToolList(ArrayList<AbstractTool> toolList)
    {
        this.toolList = toolList;
    }

    /**
     * Write a job JSON file to the standard location
     *
     * @param job The Job
     * @throws Exception
     */
    private void writeJob(Job job) throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(job);
        try
        {
            File f = new File(getFullJobPath(job));
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullJobPath(job));
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(context.cfg.gs("Z.error.writing") + getFullJobPath(job) + ": " + Utils.getStackTrace(fnf));
        }
    }

}
