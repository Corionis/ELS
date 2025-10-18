package com.corionis.els.tools.cleanup;

import com.corionis.els.Context;
import com.corionis.els.Persistent;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Task;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;

public class CleanupTool extends AbstractTool
{
    // @formatter:off
    private String configName;
    public static final String INTERNAL_NAME = "Cleanup";
    public static final String SUBSYSTEM = "tools";
    private final String internalName = INTERNAL_NAME;
    private int age = 0;

    transient private int deleteCount = 0;
    transient private ArrayList<String> filesToDelete = null;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private long compareUtc = 0L;
    transient private Repository pubRepo = null;
    transient private ArrayList<String> toolPaths;

    /**
     * Constructor when used from the command line
     *
     * @param context Context
     */
    public CleanupTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("Cleanup.displayName"));
        this.context = context;
        this.dataHasChanged = false;
    }

    public CleanupTool clone()
    {
        assert context != null;
        CleanupTool tool = new CleanupTool(context);
        tool.setConfigName(getConfigName());
        tool.setDisplayName(getDisplayName());
        tool.setAge(getAge());
        tool.setDataHasChanged();
        tool.setRemote(this.isRemote());
        return tool;
    }
    // @formatter:on

    private void deleteFiles()
    {
        String msg = "";
        for (String path : filesToDelete)
        {
            File file = new File(path);
            if (file.exists() && isOutOfDate(file))
            {
                file.delete();
                msg = "  - " + context.cfg.gs("Z.deleted") + path;
                logger.info(msg);
                ++deleteCount;
            }
        }
    }

    private void expandOrigins(ArrayList<Origin> origins) throws MungeException
    {
        // this tool only uses one repository
        if (pubRepo == null)
            return;

        for (Origin origin : origins)
        {
            if (origin.getType() == NavTreeUserObject.COLLECTION)
            {
                if (origin.getLocation().length() > 0)
                {
                    if (!pubRepo.getLibraryData().libraries.description.equalsIgnoreCase(origin.getLocation()))
                        throw new MungeException((context.cfg.gs("Cleanup.task.definition.and.loaded.repository.do.not.match")));
                }
                // process in the order defined in the JSON
                for (Library lib : pubRepo.getLibraryData().libraries.bibliography)
                {
                    Collections.addAll(toolPaths, lib.sources);
                }
            }
            else if (origin.getType() == NavTreeUserObject.LIBRARY)
            {
                for (Library lib : pubRepo.getLibraryData().libraries.bibliography)
                {
                    if (lib.name.equalsIgnoreCase(origin.getLocation()))
                    {
                        Collections.addAll(toolPaths, lib.sources);
                    }
                }
            }
            else if (origin.getType() == NavTreeUserObject.REAL)
            {
                toolPaths.add(origin.getLocation());
            }
        }
    }

    public int getAge()
    {
        return age;
    }

    public String getConfigName()
    {
        return configName;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    @Override
    public boolean isToolOriginsUsed()
    {
        return true;
    }

    public boolean isToolPublisher()
    {
        return false;
    }

    @Override
    public boolean isToolSubscriber()
    {
        return false;
    }

    @Override
    public void processTool(Task task) throws Exception
    {
        reset();

        if (pubRepo == null)
            pubRepo = context.publisherRepo;
        if (pubRepo == null)
            pubRepo = Persistent.lastPublisherRepo;

        if (pubRepo == null)
        {
            logger.error(java.text.MessageFormat.format(context.cfg.gs("Cleanup.has.no.repository.defined"), getConfigName()));
            return;
        }

        compareUtc = Instant.now().toEpochMilli();
        compareUtc = compareUtc - (getAge() * 86400000L);

        // expand origins into physical toolPaths
        expandOrigins(task.origins);
        if (toolPaths == null || toolPaths.size() == 0)
            return;

        for (String path : toolPaths)
        {
            if (isRequestStop())
                break;
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + path);

            // get fully-qualified list of filesToDelete
            scanForFiles(path);
        }

        if (!filesToDelete.isEmpty())
        {
            // delete based on age
            deleteFiles();

            String msg = getDisplayName() + ", " + getConfigName() + context.cfg.gs("Cleanup.deleted") + deleteCount;
            logger.info(msg);
            if (context != null && context.cfg.isNavigator() && !context.cfg.isLoggerView())
            {
                if (context.navigator.dialogCleanup != null && context.navigator.dialogCleanup.isShowing())
                    context.navigator.dialogCleanup.labelStatus.setText(msg);

                // reset and reload relevant trees
                if (deleteCount > 0)
                {
                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false);
                    context.browser.loadSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false);
                }
            }
        }
        else
            logger.warn(context.cfg.gs("Cleanup.no.files.found.to.compress"));
    }

    private boolean isOutOfDate(File file)
    {
        long mod = file.lastModified();
        if (mod < compareUtc)
            return true;
        return false;
    }

    public void reset()
    {
        deleteCount = 0;
        resetStop();
        toolPaths = new ArrayList<>();
        filesToDelete = new ArrayList<>();
        if (logger == null)
            logger = LogManager.getLogger("applog");
    }

    private boolean scanForFiles(String path)
    {
        boolean hadError = false;

        // is local
        try
        {
            File file = new File(Utils.getFullPathLocal(path));
            if (!file.isDirectory())
            {
                filesToDelete.add(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getAbsoluteFile().toString()));
                return true;
            }

            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(path));
            for (Path entry : directoryStream)
            {
                if (isRequestStop())
                    break;

                String filename = entry.toAbsolutePath().toString();
                boolean isDir = Files.isDirectory(entry);
                if (isDir)
                {
                    scanForFiles(filename);
                }
                else
                {
                    filesToDelete.add(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), filename));
                }
            }
        }
        catch (Exception e)
        {
            hadError = true;
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            logger.error(msg);
            if (context != null)
            {
                msg = context.cfg.gs("Z.exception") + " " + e.getMessage();
                if (context.navigator != null)
                {
                    int reply = JOptionPane.showConfirmDialog(context.navigator.dialogCleanup, msg, context.cfg.gs("CleanupUI.title"),
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        requestStop();
                }
            }
        }

        return hadError;
    }

    public void setAge(int age)
    {
        this.age = age;
    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void write() throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(getCfg().gs("Z.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
