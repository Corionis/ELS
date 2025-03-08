package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.jobs.Conflict;
import com.corionis.els.gui.util.DisableJListSelectionModel;
import com.corionis.els.jobs.Job;
import com.corionis.els.jobs.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class ConfigModel extends DefaultTableModel
{
    private ArrayList<Conflict> conflicts = null;
    private Context context;
    private String displayName;
    private LibrariesUI librariesUI;
    private Logger logger = LogManager.getLogger("applog");

    public ConfigModel(Context context, String displayName, LibrariesUI librariesUI)
    {
        super();
        this.context = context;
        this.displayName = displayName;
        this.librariesUI = librariesUI;
    }

    /**
     * Check for tool conflicts in Jobs before rename or delete
     *
     * @param oldName       The old name of the Job configuration
     * @param newName       The new name, null for delete
     * @param isRename      Is a rename operation = true, else false
     * @return int -1 = cancel, 0 = no conflicts, 1 = rename/delete
     */
    public int checkJobConflicts(String oldName, String newName, boolean isRename)
    {
        int answer = 0;
        JList<String> conflictJList = new JList<String>();

        try
        {
            // get list of conflicts
            int count = getJobPathReferences(oldName, newName);
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

                String message = java.text.MessageFormat.format(context.cfg.gs("LibraryUI.references.for.library.found.in.jobs"), oldName);
                JScrollPane pane = new JScrollPane();
                pane.setViewportView(conflictJList);
                String question = (isRename ? context.cfg.gs("LibraryUI.rename") : context.cfg.gs("LibraryUI.delete")) +
                        context.cfg.gs("LibraryUI.the.listed.references");
                Object[] params = {message, pane, question};

                int opt = JOptionPane.showConfirmDialog(context.mainFrame, params, librariesUI.displayName, JOptionPane.OK_CANCEL_OPTION);
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
            JOptionPane.showMessageDialog(context.mainFrame, msg, librariesUI.displayName, JOptionPane.ERROR_MESSAGE);
        }

        return answer;
    }

    /**
     * Find a LibMeta library in the table
     *
     * @param configName Library configuration name to find
     * @param myMeta Library Meta, if not null that LibMeta is skipped (a duplicate check)
     * @return LibMeta found, or null if not found or skipped
     */
    public LibrariesUI.LibMeta findMeta(String configName, LibrariesUI.LibMeta myMeta)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) getValueAt(i, 0);
            if (libMeta.description.equalsIgnoreCase(configName))
            {
                if (libMeta == null || libMeta != myMeta)
                {
                    return libMeta;
                }
            }
        }
        return null;
    }

    /**
     * Find a index library in the table
     *
     * @param myMeta Library Meta, if not null that LibMeta is skipped (a duplicate check)
     * @return Index in the table if founnd, otherwise -1
     */
    public int findIndex(LibrariesUI.LibMeta myMeta)
    {
        for (int i = 0; i < getRowCount(); ++i)
        {
            LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) getValueAt(i, 0);
            if (libMeta.path.equals(myMeta.path))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Find all Job path references for the old name and internal name & create a Conflict for each
     *
     * @param oldName      The old name
     * @param newName      The new name, may be null
     * @return int         The count of references
     */
    private int getJobPathReferences(String oldName, String newName)
    {
        int count = 0;
        conflicts = new ArrayList<>();
        String oldCompare = oldName + ".json";

        for (int i = 0; i < librariesUI.getJobsList().size(); ++i)
        {
            Job job = (Job) librariesUI.getJobsList().get(i);
            for (int j = 0; j < job.getTasks().size(); ++j)
            {
                Task task = job.getTasks().get(j);
                if (task.getPublisherPath().endsWith(oldCompare) ||
                    task.getSubscriberPath().endsWith(oldCompare) ||
                    task.getHintsPath().endsWith(oldCompare))
                {
                    Conflict conflict = new Conflict();
                    conflict.job = job;
                    conflict.oldName = oldName;
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
     * Process the conflicts by renaming or deleting Job tasks
     */
    private void processJobConflicts()
    {
        for (Conflict conflict : conflicts)
        {
            Job job = (Job) conflict.job;
            if (conflict.newName == null) // delete operation
            {
                Task task = job.getTasks().get(conflict.taskNumber);
                task.setContext(context);
                String oldCompare = conflict.oldName + ".json";
                if (task.getPublisherPath().endsWith(oldCompare))
                {
                    task.setPublisherKey("");
                    task.setPublisherPath("");
                }
                if (task.getSubscriberPath().endsWith(oldCompare))
                {
                    task.setSubscriberKey("");
                    task.setSubscriberPath("");
                    task.setSubscriberRemote(false);
                    task.setSubscriberOverride("");
                }
                if (task.getHintsPath().endsWith(oldCompare))
                {
                    task.setHintsKey("");
                    task.setHintsPath("");
                    task.setHintsRemote(false);
                    task.setHintsOverrideHost(false);
                }
            }
            else // rename operation
            {
                Task task = job.getTasks().get(conflict.taskNumber);
                task.setContext(context);
                String oldCompare = conflict.oldName + ".json";
                if (task.getPublisherPath().endsWith(oldCompare))
                {
                    task.setPublisherPath(swapNames(task.getPublisherPath(), oldCompare, conflict.newName));
                }
                if (task.getSubscriberPath().endsWith(oldCompare))
                {
                    task.setSubscriberPath(swapNames(task.getSubscriberPath(), oldCompare, conflict.newName));
                }
                if (task.getHintsPath().endsWith(oldCompare))
                {
                    task.setHintsPath(swapNames(task.getHintsPath(), oldCompare, conflict.newName));
                }
            }
            job.setDataHasChanged();
        }
    }

    @Override
    public void setValueAt(Object object, int row, int column)
    {
        updateListName((String) object, row);
    }

    private String swapNames(String path, String oldCompare, String newName)
    {
        int pos = path.length() - oldCompare.length();
        path = path.substring(0, pos) + newName + ".json";
        return path;
    }

    /**
     * Update or set a library JSON file path and name
     *
     * @param name New name
     * @param index Index in the configModel of library
     */
    private void updateListName(String name, int index)
    {
        if (index >= 0 && index < getRowCount())
        {
            LibrariesUI.LibMeta libMeta = (LibrariesUI.LibMeta) getValueAt(index, 0);
            if (libMeta != null)
            {
                LibrariesUI.LibMeta tmp = findMeta(name, libMeta);
                if (tmp != null && !tmp.repo.isDynamic())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs(("Z.that.configuration.already.exists")),
                            displayName, JOptionPane.WARNING_MESSAGE);
                }
                else
                {
                    // if the name changed add any existing file to deleted list
                    if (!libMeta.description.equals(name))
                    {
                        // see if anything depends on the old name
                        int answer = checkJobConflicts(libMeta.description, name, true);
                        if (answer >= 0)
                        {
                            // see if that name is to be deleted
                            boolean restored = false;
                            for (LibrariesUI.LibMeta deletedLib : librariesUI.getDeletedLibraries())
                            {
                                if (deletedLib.description.equals(name))
                                {
                                    // remove from delete list
                                    librariesUI.getDeletedLibraries().remove(deletedLib);
                                    restored = true;
                                    break;
                                }
                            }

                            if (!restored)
                            {
                                File file = new File(libMeta.path);
                                if (file.exists())
                                {
                                    librariesUI.getDeletedLibraries().add(libMeta.clone());
                                }
                            }

                            libMeta.description = name;
                            libMeta.repo.getLibraryData().libraries.description = name;

                            // create or rename filename
                            String jfn;
                            if (libMeta.repo.getJsonFilename() != null && libMeta.repo.getJsonFilename().length() > 0)
                            {
                                jfn = libMeta.repo.getJsonFilename();
                                int sepPos = jfn.lastIndexOf("/");
                                if (sepPos < 0)
                                    sepPos = jfn.lastIndexOf("\\");
                                if (sepPos >= 0)
                                    jfn = jfn.substring(0, sepPos + 1) + name + ".json";
                                else
                                    jfn = librariesUI.getDirectoryPath() + System.getProperty("file.separator") + libMeta.description + ".json";
                            }
                            else
                                jfn = librariesUI.getDirectoryPath() + System.getProperty("file.separator") + libMeta.description + ".json";
                            libMeta.repo.setJsonFilename(jfn);
                            libMeta.path = jfn;

                            libMeta.setDataHasChanged();
                        }
                        else
                        {
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.rename") + context.cfg.gs("Z.cancelled"));
                        }
                    }
                }
            }
        }

        context.mainFrame.librariesConfigItems.requestFocus();
        context.mainFrame.librariesConfigItems.changeSelection(index, 0, false, false);
    }

}
