package com.corionis.els.tools.junkremover;

import com.corionis.els.Context;
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
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class JunkRemoverTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "JunkRemover";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user name for this instance
    private String internalName = INTERNAL_NAME;
    private ArrayList<JunkItem> junkList;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private int deleteCount = 0;
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private Repository repo; // this tool only uses one repo
    transient private ArrayList<String> toolPaths;
    // @formatter:on

    /**
     * Constructor when used from the command line
     *
     * @param context   Context
     */
    public JunkRemoverTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
        this.context = context;
        this.junkList = new ArrayList<JunkItem>();
        this.dataHasChanged = false;
    }

    public JunkItem addJunkItem()
    {
        JunkItem ji = new JunkItem();
        junkList.add(ji);
        setDataHasChanged();
        return ji;
    }

    public JunkRemoverTool clone()
    {
        assert context != null;
        JunkRemoverTool jrt = new JunkRemoverTool(context);
        jrt.setConfigName(getConfigName());
        jrt.setDisplayName(getDisplayName());
        jrt.setDataHasChanged();
        jrt.isDryRun = this.isDryRun;
        jrt.setRemote(this.isRemote());
        jrt.setJunkList(getJunkList());
        return jrt;
    }

    private int expandOrigins(ArrayList<Origin> origins) throws MungeException
    {
        int count = 0;

        // this tool only uses one repository
        if (repo == null)
            return -1;

        for (Origin origin : origins)
        {
            if (origin.getType() == NavTreeUserObject.COLLECTION)
            {
                if (origin.getLocation().length() > 0)
                {
                    if (!repo.getLibraryData().libraries.description.equalsIgnoreCase(origin.getLocation()))
                        throw new MungeException((context.cfg.gs("JunkRemover.task.definition.and.loaded.repository.do.not.match")));
                }
                // process in the order defined in the JSON
                for (Library lib : repo.getLibraryData().libraries.bibliography)
                {
                    for (String source : lib.sources)
                    {
                        toolPaths.add(source);
                        ++count;
                    }
                }
            }
            else if (origin.getType() == NavTreeUserObject.LIBRARY)
            {
                for (Library lib : repo.getLibraryData().libraries.bibliography)
                {
                    if (lib.name.equalsIgnoreCase(origin.getLocation()))
                    {
                        for (String source : lib.sources)
                        {
                            toolPaths.add(source);
                            ++count;
                        }
                    }
                }
            }
            else if (origin.getType() == NavTreeUserObject.REAL)
            {
                toolPaths.add(origin.getLocation());
                ++count;
            }
        }

        return count;
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

    public ArrayList<JunkItem> getJunkList()
    {
        return junkList;
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

    @Override
    public boolean isToolPubOrSub()
    {
        return true;
    }

    @Override
    public boolean isToolSubscriber()
    {
        return false;
    }

    private void logDeletion(String fullpath)
    {
        String msg = "";
        if (!isDryRun)
            msg = "  - " + context.cfg.gs("Z.deleted") + fullpath;
        else
            msg = "  > " + context.cfg.gs("Z.would.delete") + fullpath;

        logger.info(msg);
    }

    private boolean match(String filename, String fullpath, JunkItem junk)
    {
        // https://commons.apache.org/proper/commons-io/
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)
        boolean isMatch = FilenameUtils.wildcardMatch(filename, junk.wildcard, (junk.caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE));
        return isMatch;
    }

    @Override
    public void processTool(Task task) throws Exception
    {
        reset();
        isDryRun = task.dryRun;

        if (task.publisherRepo != null && task.subscriberRepo != null)
            throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("JunkRemover.uses.only.one.repository"), getInternalName()));

        // this tool only uses one repository
        repo = (task.publisherRepo != null) ? task.publisherRepo : task.subscriberRepo;
        if (repo == null)
        {
            logger.error(getConfigName() + " has no repository defined");
            return;
        }

        // expand origins into physical toolPaths
        int count = expandOrigins(task.origins);
        if (toolPaths == null || toolPaths.size() == 0)
            return;

        // only subscribers can be remote
        if (task.subscriberRepo != null && getCfg().isRemoteOperation())
            setRemote(true);

        for (String path : toolPaths)
        {
            if (isRequestStop())
                break;
            String rem = isRemote() ? context.cfg.gs("Z.remote.uppercase") : "";
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + rem + path);

            scanForJunk(path);
        }

        logger.info(getDisplayName() + ", " + getConfigName() + ": " + deleteCount + (isDryRun ? " (dry-run)" : ""));
        if (context != null && context.cfg.isNavigator() && !context.cfg.isLoggerView())
        {
            // reset and reload relevant trees
            if (!isDryRun && deleteCount > 0)
            {
                if (!repo.isSubscriber())
                {
                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false);
                    context.browser.loadSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false);
                }
                else
                {
                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, isRemote());
                    context.browser.loadSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, isRemote());
                }
            }
        }
    }

    public void reset()
    {
        deleteCount = 0;
        resetStop();
        toolPaths = new ArrayList<>();
        if (logger == null)
            logger = LogManager.getLogger("applog");
    }

    private boolean scanForJunk(String path)
    {
        boolean hadError = false;

        if (isRemote())
        {
            try
            {
                path = context.cfg.getFullPathSubscriber(path);
                Vector listing = getContext().clientSftp.listDirectory(path);
                for (int i = 0; i < listing.size(); ++i)
                {
                    if (isRequestStop())
                        break;
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                    if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                    {
                        SftpATTRS attrs = entry.getAttrs();
                        String filename = entry.getFilename();
                        String fullpath = path + repo.getSeparator() + entry.getFilename();
                        if (context != null && context.progress != null)
                        {
                            context.progress.update(" " + fullpath);
                        }
                        if (attrs.isDir())
                        {
                            scanForJunk(fullpath);
                        }
                        else
                        {
                            for (JunkItem ji : junkList)
                            {
                                if (isRequestStop())
                                    break;
                                if (match(filename, fullpath, ji))
                                {
                                    ++deleteCount;
                                    if (context != null)
                                    {
                                        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.count") + deleteCount);
                                        context.mainFrame.labelStatusMiddle.updateUI();
                                    }
                                    if (!isDryRun)
                                        getContext().transfer.remove(fullpath, isRemote());
                                    logDeletion(fullpath);
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (context != null)
                {
                    logger.error(msg);
                    int reply = JOptionPane.showConfirmDialog(context.navigator.dialogJunkRemover, context.cfg.gs("JunkRemover.title"),
                            "Z.cancel.run", JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        requestStop();
                }
                else
                    logger.error(msg);
            }
        }
        else // is local
        {
            try
            {
                File[] files;
                File file = new File(Utils.getFullPathLocal(path));
                if (file.isDirectory())
                {
                    files = FileSystemView.getFileSystemView().getFiles(file, true);
                }
                else
                {
                    files = new File[1];
                    files[0] = file;
                }
                for (File entry : files)
                {
                    if (isRequestStop())
                        break;
                    String filename = entry.getName();
                    if (context != null && context.progress != null)
                    {
                        context.progress.update(" " + filename);
                    }
                    String fullpath = entry.getPath();
                    if (entry.isDirectory())
                    {
                        scanForJunk(fullpath);
                    }
                    else
                    {
                        for (JunkItem ji : junkList)
                        {
                            if (isRequestStop())
                                break;
                            if (match(filename, fullpath, ji))
                            {
                                ++deleteCount;
                                if (context.mainFrame != null)
                                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.count") + deleteCount);
                                if (!isDryRun)
                                    getContext().transfer.remove(fullpath, isRemote());
                                logDeletion(fullpath);
                                break;
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (context != null)
                {
                    logger.error(msg);
                    int reply = JOptionPane.showConfirmDialog(context.navigator.dialogJunkRemover, context.cfg.gs("JunkRemover.title"),
                            "Z.cancel.run", JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        requestStop();
                }
                else
                    logger.error(msg);
            }
        }

        return hadError;
    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    public void setDataHasChanged(boolean state)
    {
        dataHasChanged = state;
    }

    public void setJunkList(ArrayList<JunkItem> junkList)
    {
        this.junkList = junkList;
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
            throw new MungeException(getCfg().gs("JunkRemover.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

    // ================================================================================================================

    public class JunkItem implements Serializable
    {
        public boolean caseSensitive = false;
        public String wildcard;
    }

}
