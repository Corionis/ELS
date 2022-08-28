package com.groksoft.els.tools.junkremover;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.Progress;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.jobs.Origin;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Vector;

public class JunkRemoverTool extends AbstractTool
{
    public static final String INTERNAL_NAME = "JunkRemover";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user name for this instance
    private String internalName = INTERNAL_NAME;
    private ArrayList<JunkItem> junkList;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private int deleteCount = 0;
    transient private boolean dualRepositories = false; // used by GUI, always false for this tool
    transient private GuiContext guiContext = null;
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private final boolean realOnly = false;
    transient private Repository repo; // this tool only uses one repo
    transient private ArrayList<String> toolPaths;

    /**
     * Constructor when used from the command line
     *
     * @param config Configuration
     * @param ctxt   Context
     */
    public JunkRemoverTool(GuiContext guiContext, Configuration config, Context ctxt)
    {
        super(config, ctxt);
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
        this.guiContext = guiContext;
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
                        throw new MungeException((cfg.gs("JunkRemover.task.definition.and.loaded.repository.do.not.match")));
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
        assert guiContext != null;
        JunkRemoverTool jrt = new JunkRemoverTool(guiContext, guiContext.cfg, guiContext.context);
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
        return Utils.getCfg().gs("JunkRemover.displayName");
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
            msg = "  - " + cfg.gs("Z.deleted") + fullpath;
        else
            msg = "  > " + cfg.gs("Z.would.delete") + fullpath;

        if (guiContext != null)
            guiContext.browser.printLog(msg);
        else
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
    public void processTool(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun) throws Exception
    {
        reset();
        isDryRun = dryRun;

        if (publisherRepo != null && subscriberRepo != null)
            throw new MungeException(java.text.MessageFormat.format(cfg.gs("JunkRemover.uses.only.one.repository"), getInternalName()));

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
            String rem = isRemote() ? cfg.gs("Z.remote.uppercase") : "";
            if (guiContext != null)
                guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + rem + path);
            else
                logger.info(getDisplayName() + ", " + getConfigName() + ": " + path);

            scanForJunk(path);
        }

        if (guiContext != null)
        {
            guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + deleteCount + (isDryRun ? " (dry-run)" : ""));

            // reset and reload relevant trees
            if (!isDryRun && deleteCount > 0)
            {
                if (!repo.isSubscriber())
                {
                    guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionOne, guiContext.context.publisherRepo, false);
                    guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemOne, guiContext.context.publisherRepo, false);
                }
                else
                {
                    guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, isRemote());
                    guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemTwo, guiContext.context.subscriberRepo, isRemote());
                }
            }
        }
        else
        {
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + deleteCount + (isDryRun ? " (dry-run)" : ""));
        }
    }

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param guiContext     The GuiContext
     * @param publisherRepo  Publisher repo, or null
     * @param subscriberRepo Subscriber repo, or null
     * @param origins        List of origins to process
     * @param dryRun         Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    @Override
    public SwingWorker<Void, Void> processToolThread(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        // create a fresh dialog
        if (guiContext != null)
        {
            if (guiContext.progress == null || !guiContext.progress.isBeingUsed())
            {
                ActionListener cancel = new ActionListener()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        requestStop();
                    }
                };
                guiContext.progress = new Progress(guiContext, guiContext.mainFrame, cancel, isDryRun);
            }
            else
            {
                JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                return null;
            }

            guiContext.progress.display();
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                try
                {
                    processTool(guiContext, publisherRepo, subscriberRepo, origins, dryRun);
                }
                catch (Exception e)
                {
                    String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                    if (guiContext != null)
                    {
                        guiContext.browser.printLog(msg, true);
                        JOptionPane.showMessageDialog(guiContext.navigator.dialogJunkRemover, msg, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
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
                        if (guiContext != null && guiContext.progress != null)
                        {
                            guiContext.progress.update(" " + fullpath);
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
                                    if (guiContext != null)
                                    {
                                        guiContext.mainFrame.labelStatusMiddle.setText(cfg.gs("Z.count") + deleteCount);
                                        guiContext.mainFrame.labelStatusMiddle.updateUI();
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
                String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (guiContext != null)
                {
                    guiContext.browser.printLog(msg, true);
                    int reply = JOptionPane.showConfirmDialog(guiContext.navigator.dialogJunkRemover, guiContext.cfg.gs("JunkRemover.title"),
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
                    if (guiContext != null && guiContext.progress != null)
                    {
                        guiContext.progress.update(" " + filename);
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
                                if (guiContext != null)
                                    guiContext.mainFrame.labelStatusMiddle.setText(cfg.gs("Z.count") + deleteCount);
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
                String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (guiContext != null)
                {
                    guiContext.browser.printLog(msg, true);
                    int reply = JOptionPane.showConfirmDialog(guiContext.navigator.dialogJunkRemover, guiContext.cfg.gs("JunkRemover.title"),
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
