package com.groksoft.els.tools.renamer;

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

public class RenamerTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Renamer";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user name for this instance
    private String internalName = INTERNAL_NAME;
    private int type = 0;
    private int segment = 0; // 0 = name only, 1 = extension only, 2 = whole filename
    private String text1 = "";
    private String text2 = "";
    private String text3 = "";
    private boolean option1 = false;
    private boolean option2 = false;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private int deleteCount = 0;
    transient private boolean dualRepositories = true; // used by GUI, always false for this tool
    transient private GuiContext guiContext = null;
    transient private boolean isDryRun = false;
    transient private boolean isRemote;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private Repository repo; // this tool only uses one repo
    transient private ArrayList<String> toolPaths;
    // @formatter:on

    /**
     * Constructor when used from the command line
     *
     * @param config Configuration
     * @param ctxt   Context
     */
    public RenamerTool(GuiContext guiContext, Configuration config, Context ctxt)
    {
        super(config, ctxt);
        setDisplayName(getCfg().gs("Renamer.displayName"));
        this.guiContext = guiContext;
    }

    public RenamerTool clone()
    {
        assert guiContext != null;
        RenamerTool renamer = new RenamerTool(guiContext, guiContext.cfg, guiContext.context);
        renamer.setConfigName(getConfigName());
        renamer.setDisplayName(getDisplayName());
        renamer.setDataHasChanged();
        renamer.setIncludeInToolsList(isIncludeInToolsList());

        return renamer;
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
                        throw new MungeException((cfg.gs("Renamer.task.definition.and.loaded.repository.do.not.match")));
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

    public String getConfigName()
    {
        return configName;
    }

    @Override
    public String getDisplayName()
    {
        return Utils.getCfg().gs("Renamer.displayName");
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    public int getSegment()
    {
        return segment;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    public String getText1()
    {
        return text1;
    }

    public String getText2()
    {
        return text2;
    }

    public String getText3()
    {
        return text3;
    }

    public int getType()
    {
        return type;
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

    public boolean isOption1()
    {
        return option1;
    }

    public boolean isOption2()
    {
        return option2;
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

        if (publisherRepo != null && subscriberRepo != null)
            throw new MungeException(java.text.MessageFormat.format(cfg.gs("Renamer.uses.only.one.repository"), getInternalName()));

        // this tool only uses one repository
        repo = (publisherRepo != null) ? publisherRepo : subscriberRepo;

        if (origins == null || origins.size() == 0)
        {
            Origin o = new Origin(repo.getLibraryData().libraries.description, NavTreeUserObject.COLLECTION);
            origins = new ArrayList<Origin>();
            origins.add(o);
        }

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
            String rem = isRemote ? cfg.gs("Z.remote.uppercase") : "";
            if (guiContext != null)
                guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + rem + path);
            else
                logger.info(getDisplayName() + ", " + getConfigName() + ": " + path);

            scanForJunk(path);
        }

        if (guiContext != null)
        {
            guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + deleteCount);

            // reset and reload relevant trees
            if (!isDryRun && deleteCount > 0)
            {
                if (!repo.isSubscriber())
                {
                    guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionOne, guiContext.context.publisherRepo, false);
                    guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemOne, false);
                }
                else
                {
                    guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, isRemote());
                    guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemTwo, isRemote());
                }
            }
        }
        else
        {
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + deleteCount);
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
                        JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
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

    public void resetOptions()
    {
        text1 = "";
        text2 = "";
        text3 = "";
        option1 = false;
        option2 = false;

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
/*
                            for (JunkItem ji : junkList)
                            {
                                if (isRequestStop())
                                    break;
                                if (match(filename, fullpath, ji))
                                {
                                    if (!isDryRun)
                                    {
                                        //getContext().transfer.remove(fullpath, attrs.isDir(), isRemote());
                                        ++deleteCount;
                                    }
                                }
                            }
*/
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
                    JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
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
                    if (entry.isDirectory())
                    {
                        scanForJunk(entry.getAbsolutePath());
                    }
                    else
                    {
/*
                        for (JunkItem ji : junkList)
                        {
                            if (isRequestStop())
                                break;
                            if (match(filename, entry.getAbsolutePath(), ji))
                            {
                                if (!isDryRun)
                                {
                                    //getContext().transfer.remove(entry.getAbsolutePath(), entry.isDirectory(), isRemote());
                                    ++deleteCount;
                                }
                            }
                        }
*/
                    }
                }
            }
            catch (Exception e)
            {
                String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (guiContext != null)
                {
                    guiContext.browser.printLog(msg, true);
                    JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
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

    public void setGuiContext(GuiContext guicontext)
    {
        this.guiContext = guicontext;
        this.cfg = guicontext.cfg;
        this.context = guicontext.context;
        setDisplayName(getCfg().gs("Renamer.displayName"));
    }

/*
    private boolean match(String filename, String fullpath, JunkItem junk)
    {
        // https://commons.apache.org/proper/commons-io/
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)
        boolean isMatch = FilenameUtils.wildcardMatch(filename, junk.wildcard, (junk.caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE));
        if (isMatch)
        {
            String msg = "  ";
            if (isRemote())
                msg += getCfg().gs("Z.remote.uppercase");
            else
                msg += getCfg().gs("NavTreeNode.local");
            msg += MessageFormat.format(getCfg().gs("NavTransferHandler.delete.file.message"), isDryRun ? 0 : 1, fullpath);
            if (guiContext != null)
                guiContext.browser.printLog(msg);
            else
                logger.info(msg);
        }
        return isMatch;
    }
*/

    public void setOption1(boolean option1)
    {
        if (this.option1 != option1)
        {
            this.option1 = option1;
            setDataHasChanged();
        }
    }

    public void setOption2(boolean option2)
    {
        if (this.option2 != option2)
        {
            this.option2 = option2;
            setDataHasChanged();
        }
    }

    public void setSegment(int segment)
    {
        if (this.segment != segment)
        {
            this.segment = segment;
            setDataHasChanged();
        }
    }

    public void setText1(String text1)
    {
        if (!this.text1.equals(text1))
        {
            this.text1 = text1;
            setDataHasChanged();
        }
    }

    public void setText2(String text2)
    {
        if (!this.text2.equals(text2))
        {
            this.text2 = text2;
            setDataHasChanged();
        }
    }

    public void setText3(String text3)
    {
        if (!this.text3.equals(text3))
        {
            this.text3 = text3;
            setDataHasChanged();
        }
    }

    public void setType(int type)
    {
        this.type = type;
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
            throw new MungeException(getCfg().gs("Renamer.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
