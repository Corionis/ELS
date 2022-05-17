package com.groksoft.els.gui.tools.junkremover;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.gui.tools.AbstractTool;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class JunkRemoverTool extends AbstractTool
{
    private ArrayList<JunkItem> junkList;

    transient private boolean dataHasChanged = false;
    transient int deleteCount = 0;
    transient private ArrayList<String> operationPaths;
    transient private GuiContext guiContext = null;
    transient private Repository repo;

    public JunkRemoverTool(Configuration config, Context ctxt)
    {
        super(config, ctxt, "JunkRemover");
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
        this.junkList = new ArrayList<JunkItem>();

    }

    public JunkRemoverTool(GuiContext guiContext)
    {
        super(guiContext.cfg, guiContext.context, "JunkRemover");
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
        this.guiContext = guiContext;
        this.junkList = new ArrayList<JunkItem>();
    }

    public JunkItem addJunkItem()
    {
        JunkItem ji = new JunkItem();
        junkList.add(ji);
        setDataHasChanged(true);
        return ji;
    }

    public int addTuoPaths(NavTreeUserObject tuo)
    {
        int count = 0;

        if (tuo.type == NavTreeUserObject.COLLECTION)
        {
            Repository repo = getContext().transfer.getRepo(tuo);
            if (repo != null)
            {
                Arrays.sort(repo.getLibraryData().libraries.bibliography);
                for (Library lib : repo.getLibraryData().libraries.bibliography)
                {
                    for (String source : lib.sources)
                    {
                        operationPaths.add(source);
                        ++count;
                    }
                }
            }
        }
        else if (tuo.type == NavTreeUserObject.LIBRARY)
        {
            for (String source : tuo.sources)
            {
                operationPaths.add(source);
                ++count;
            }
        }
        else if (tuo.type == NavTreeUserObject.REAL)
        {
            operationPaths.add(tuo.getPath());
            ++count;
        }

        if (count > 0)
        {
            if (tuo.isSubscriber())
                setIsSubscriber(true);
            if (tuo.isRemote)
                setIsRemote(true);
        }
        return count;
    }

    public JunkRemoverTool clone()
    {
        assert guiContext != null;
        JunkRemoverTool jrt = new JunkRemoverTool(guiContext);
        jrt.setConfigName(getConfigName());
        jrt.setInternalName(getInternalName());

        jrt.setDataHasChanged(true);
        jrt.setDisplayName(getDisplayName());
        jrt.setJunkList(getJunkList());

        // runtime transient items
        // jrt.operationPaths = operationPaths;
        // jrt.setDryRun(isDryRun());
        // jrt.isRemote = isRemote;
        // jrt.isSubscriber = isSubscriber;
        return jrt;
    }

    public ArrayList<JunkItem> getJunkList()
    {
        return junkList;
    }

    public Repository getRepo()
    {
        return repo;
    }

    public SwingWorker<Void, Void> guiProcessTool()
    {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                processTool();
                return null;
            }
        };
        worker.execute();
        return worker;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

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
                msg += getCfg().gs("NavTreeNode.remote");
            else
                msg += getCfg().gs("NavTreeNode.local");
            msg += MessageFormat.format(getCfg().gs("NavTransferHandler.delete.file.message"), isDryRun() ? 0 : 1, fullpath);
            if (guiContext != null)
                guiContext.browser.printLog(msg);
            else
                logger.info(msg);
        }
        return isMatch;
    }

    /**
     * Process the tool with the metadata provided
     * <br/>
     * Uses the junkList across the operationPaths added by addTuoPaths() and the dryRun setting.
     */
    @Override
    public void processTool() throws Exception
    {
        deleteCount = 0;
        resetRequestStop();

        if (operationPaths == null || operationPaths.size() == 0)
            return;

        if (isSubscriber())
            repo = getContext().subscriberRepo;
        else
            repo = getContext().publisherRepo;

        if (repo == null)
            throw new MungeException(getCfg().gs("JunkRemover.repository.does.not.appear.to.be.loaded"));

        for (String path : operationPaths)
        {
            if (isRequestStop())
                break;
            if (guiContext != null)
                guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + path);
            else
                logger.info(getDisplayName() + ", " + getConfigName() + ": " + path);

            scanForJunk(path);
        }

        if (guiContext != null)
        {
            guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + deleteCount);

            // reset and reload relevant trees
            if (!isDryRun() && deleteCount > 0)
            {
                if (!isSubscriber())
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

            // TODO Work-out how to refresh command-line data
        }
    }

    public void resetOperationPaths()
    {
        operationPaths = new ArrayList<String>();
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
                                    if (!isDryRun())
                                    {
                                        getContext().transfer.remove(fullpath, attrs.isDir(), isRemote());
                                        ++deleteCount;
                                    }
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                String msg = getCfg().gs("JunkRemover.exception") + Utils.getStackTrace(e);
                if (guiContext != null)
                    guiContext.browser.printLog(msg, true);
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
                    if (entry.isDirectory())
                    {
                        scanForJunk(entry.getAbsolutePath());
                    }
                    else
                    {
                        for (JunkItem ji : junkList)
                        {
                            if (isRequestStop())
                                break;
                            if (match(filename, entry.getAbsolutePath(), ji))
                            {
                                if (!isDryRun())
                                {
                                    getContext().transfer.remove(entry.getAbsolutePath(), entry.isDirectory(), isRemote());
                                    ++deleteCount;
                                }
                            }
                        }
                    }
                }
            }
            catch (Exception e)
            {
                String msg = getCfg().gs("JunkRemover.exception") + Utils.getStackTrace(e);
                if (guiContext != null)
                    guiContext.browser.printLog(msg, true);
                else
                    logger.error(msg);
            }
        }

        return hadError;
    }

    public void setContext(Configuration config, Context context)
    {
        this.guiContext = null;
        this.cfg = config;
        this.context = context;
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
    }

    public void setDataHasChanged(boolean sense)
    {
        dataHasChanged = sense;
    }

    public void setGuiContext(GuiContext guicontext)
    {
        this.guiContext = guicontext;
        this.cfg = guicontext.cfg;
        this.context = guicontext.context;
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
    }

    public void setJunkList(ArrayList<JunkItem> junkList)
    {
        this.junkList = junkList;
    }

    public void setOperationPaths(ArrayList<String> names)
    {
        this.operationPaths = names;
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
        boolean caseSensitive = false;
        String wildcard;
    }

}
