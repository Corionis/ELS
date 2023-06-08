package com.groksoft.els.jobs;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")
public class Jobs
{
    private Context context;
    ArrayList<Conflict> conflicts = null;
    private Logger logger = LogManager.getLogger("applog");

    private Jobs()
    {
        // hide default constructor
    }

    public Jobs(Context context)
    {
        this.context = context;
    }

    /**
     * Check for tool conflicts in Jobs before rename or delete
     * <br/><br/>
     * Use getConflicts() to retrieve list to be renamed or deleted
     *
     * @param myDialog The dialog associated with the tool
     * @param isRename Rename = true, delete = false
     * @param tool The tool being manipulated
     * @return -1 = cancel rename or delete, 0 = no conflicts, 1 = perform rename or delete
     */
    public int checkConflicts(JDialog myDialog, boolean isRename, AbstractTool tool)
    {
        int condition = 0;
        ArrayList<AbstractTool> jobList;
        ArrayList<AbstractTool> toolList;
        JList<String> conflictJList = new JList<String>();

        try
        {
            // get list of Jobs
            jobList = loadAllJobs();

            // get list of tools including Jobs
            toolList = loadAllJobs(); // creates the ArrayList
            Tools toolsHandler = new Tools();
            toolList = toolsHandler.loadAllTools(context, null, toolList); // add the other tools

            // get list of conflicts
            conflicts = getReferences(jobList, toolsHandler, toolList, tool);
            if (conflicts != null && conflicts.size() > 0)
            {
                condition = -1;

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
                conflictJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                conflictJList.setSelectedIndex(0);

                String message = context.cfg.gs("References found in Jobs:");
                JScrollPane pane = new JScrollPane();
                pane.setViewportView(conflictJList);
                conflictJList.setEnabled(false);
                String question = context.cfg.gs(isRename ? "Rename" : "Delete") + context.cfg.gs(" the references listed?");
                Object[] params = {message, pane, question};

                int opt = JOptionPane.showConfirmDialog(myDialog, params, context.cfg.gs("JobsUI.title"), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                    condition = 1;
            }
        }
        catch (Exception e)
        {
            condition = -1;
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(myDialog, msg, context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
        }

        return condition;
    }

    public ArrayList<Conflict> getConflicts()
    {
        return this.conflicts;
    }

    private ArrayList<Conflict> getReferences(ArrayList<AbstractTool> jobList, Tools tools, ArrayList<AbstractTool> toolList, AbstractTool tool)
    {
        ArrayList<Conflict> conflicts = new ArrayList<>();
        for (AbstractTool job : jobList)
        {
            int num = 0;
            for (Task jobTask : ((Job)job).getTasks())
            {
                ++num;
                if (jobTask.getConfigName().equals(tool.getConfigName()) && jobTask.getInternalName().equals(tool.getInternalName()))
                {
                    AbstractTool conflictTool = tools.getTool(toolList, tool.getInternalName(), tool.getConfigName());
                    if (tool != conflictTool) // conflictTool should never be null
                    {
                        Conflict conflict = new Conflict();
                        conflict.job = job;
                        conflict.taskNumber = num;
                        conflict.tool = conflictTool;
                        conflict.newName = tool.getConfigName();
                        conflicts.add(conflict);
                    }
                }
            }
        }
        return conflicts;
    }


    public ArrayList<AbstractTool> loadAllJobs() throws Exception
    {
        ArrayList<AbstractTool> jobList = new ArrayList<>();
        jobList = scanJobs(jobList);
        Collections.sort(jobList);
        return jobList;
    }

    private ArrayList<AbstractTool> scanJobs(ArrayList<AbstractTool> jobList) throws Exception
    {
        Job tmpJob = new Job(context, "temp");
        String dir = tmpJob.getDirectoryPath();
        File jobDir = new File(dir);
        if (jobDir.exists() && jobDir.isDirectory())
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new Job(context, "");
                }
            }
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Job.class, new objInstanceCreator());

            File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                    if (json != null)
                    {
                        Job job = builder.create().fromJson(json, Job.class);
                        if (job != null)
                            jobList.add(job);
                    }
                }
            }
        }
        return jobList;
    }

}
