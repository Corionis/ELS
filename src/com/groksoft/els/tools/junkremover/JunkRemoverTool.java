package com.groksoft.els.tools.junkremover;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.Progress;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
    transient private boolean dualRepositories = false; // used by GUI, always false for this tool
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private final boolean realOnly = false;
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
                if (origin.getName().length() > 0)
                {
                    if (!repo.getLibraryData().libraries.description.equalsIgnoreCase(origin.getName()))
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
                    if (lib.name.equalsIgnoreCase(origin.getName()))
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
                toolPaths.add(origin.getName());
                ++count;
            }
        }

        return count;
    }

    public JunkRemoverTool clone()
    {
        assert context != null;
        JunkRemoverTool jrt = new JunkRemoverTool(context);
        jrt.setConfigName(getConfigName());
        jrt.setDisplayName(getDisplayName());
        jrt.setDataHasChanged();
        jrt.setIncludeInToolsList(this.isIncludeInToolsList());
        jrt.isDryRun = this.isDryRun;
        jrt.setIsRemote(this.isRemote());
        jrt.setJunkList(getJunkList());
        jrt.setIncludeInToolsList(isIncludeInToolsList());
        return jrt;
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

    public ArrayList<String> getForwardPaths()
    {
        return null;
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

    public boolean isCachedLastTask()
    {
        return false;
    }

    @Override
    public boolean isDualRepositories()
    {
        return dualRepositories;
    }

    @Override
    public boolean isRealOnly()
    {
        return realOnly;
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

    /**
     * Process the tool with the metadata provided
     * <br/>
     * Uses the junkList across the toolPaths added by addToolPaths() and the dryRun setting.
     * The addToolPaths() method must be called first.
     * <br/>
     * Used by a Job & the Run button of the tool
     */
    @Override
    public void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception
    {
        reset();
        isDryRun = dryRun;

        if (publisherRepo != null && subscriberRepo != null)
            throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("JunkRemover.uses.only.one.repository"), getInternalName()));

        // this tool only uses one repository
        repo = (publisherRepo != null) ? publisherRepo : subscriberRepo;

        // expand origins into physical toolPaths
        int count = expandOrigins(origins);
        if (toolPaths == null || toolPaths.size() == 0)
            return;

        // only subscribers can be remote
        if (subscriberRepo != null && getCfg().isRemoteSession())
            setIsRemote(true);

        for (String path : toolPaths)
        {
            if (isRequestStop())
                break;
            String rem = isRemote() ? context.cfg.gs("Z.remote.uppercase") : "";
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + rem + path);

            scanForJunk(path);
        }

        logger.info(getDisplayName() + ", " + getConfigName() + ": " + deleteCount + (isDryRun ? " (dry-run)" : ""));
        if (context != null)
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

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param context     The context
     * @param publisherRepo  Publisher repo, or null
     * @param subscriberRepo Subscriber repo, or null
     * @param origins        List of origins to process
     * @param dryRun         Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        // create a fresh dialog
        if (context != null)
        {
            if (context.progress == null || !context.progress.isBeingUsed())
            {
                ActionListener cancel = new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        requestStop();
                    }
                };
                context.progress = new Progress(context, context.mainFrame, cancel, isDryRun);
                context.progress = context.progress;
                context.progress.display();
            }
            else
            {
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                return null;
            }
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                try
                {
                    processTool(context, publisherRepo, subscriberRepo, origins, dryRun, null);
                }
                catch (Exception e)
                {
                    String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                    if (context != null)
                    {
                        logger.error(msg);
                        JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        logger.error(msg);
                }
                return null;
            }
        };
        worker.execute();
        return worker;
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
                                        getContext().transfer.remove(fullpath, attrs.isDir(), isRemote());
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
                File file = new File(path);
                if (file.isDirectory())
                {
                    files = FileSystemView.getFileSystemView().getFiles(file.getAbsoluteFile(), false);
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
                    String fullpath = entry.getAbsolutePath();
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
                                if (context != null)
                                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.count") + deleteCount);
                                if (!isDryRun)
                                    getContext().transfer.remove(fullpath, entry.isDirectory(), isRemote());
                                logDeletion(fullpath);
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

    public void setForwardPaths(ArrayList<String> forwardPaths)
    {
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
