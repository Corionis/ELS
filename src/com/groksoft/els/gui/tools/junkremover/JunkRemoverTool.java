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

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

public class JunkRemoverTool extends AbstractTool
{
    private ArrayList<JunkItem> junkList;
    transient boolean dataHasChanged = false;
    transient ArrayList<String> operationPaths;
     transient private GuiContext guiContext = null;

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

        if (operationPaths == null)
            operationPaths = new ArrayList<String>();

        if (tuo.type == NavTreeUserObject.COLLECTION)
        {
            Repository repo = guiContext.context.transfer.getRepo(tuo);
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
        jrt.setDataHasChanged(isDataChanged());
        jrt.setDisplayName(getDisplayName());
        jrt.setDryRun(isDryRun());
        jrt.setInternalName(getInternalName());
        jrt.setJunkList(getJunkList());
//        jrt.operationPaths = operationPaths;
//        jrt.isRemote = isRemote;
//        jrt.isSubscriber = isSubscriber;
        return jrt;
    }

    public ArrayList<JunkItem> getJunkList()
    {
        return junkList;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    private boolean match(String filename, JunkItem junk)
    {
        // https://commons.apache.org/proper/commons-io/
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html
        // https://commons.apache.org/proper/commons-io/javadocs/api-2.5/org/apache/commons/io/FilenameUtils.html#wildcardMatch(java.lang.String,%20java.lang.String)
        return FilenameUtils.wildcardMatch(filename, junk.wildcard, (junk.caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE));
    }

    /**
     * Process the tool with the arguments provided
     */
    @Override
    public boolean process() throws Exception
    {
        // TODO
        //  * Iterate "thing" list HERE
        //  * Either interactive or command line have to:
        //     + Iterate an entire Collection
        //     + Iterate the sources of a Library
        //     + Process an individual path - detect if the String contains any path character
        //     + Ultimately iterate a directory or individual file
        //  * Use the Repository either way
        //  * Interactive refreshes the selected items when done
        //  * Command line rescans when done

        if (operationPaths == null || operationPaths.size() == 0)
            return false;

//        if (isSubscriber())
//            repo = guiContext.context.subscriberRepo;
//        else
//            repo = guiContext.context.publisherRepo;
//
//        if (repo == null)
//            throw new MungeException("Repository does not appear to be loaded");

        for (String path : operationPaths)
        {
            if (isRemote())
            {
                if (guiContext != null)
                {
                    guiContext.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + path);
                }
                try
                {
                    Vector listing = guiContext.context.clientSftp.listDirectory(path);
                    //logger.info(Utils.formatInteger(listing.size()) + guiContext.cfg.gs("NavTreeNode.received.entries.from") + path);
                    for (int i = 0; i < listing.size(); ++i)
                    {
                        ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                        if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                        {
                            SftpATTRS a = entry.getAttrs();
                        }
                    }
                }
                catch (Exception e)
                {

                }

            }
            else
            {

            }
        }

        return false;
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
            throw new MungeException("Error writing: " + getFullPath() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    // ================================================================================================================

    public class JunkItem implements Serializable
    {
        boolean caseSensitive = false;
        String wildcard;
    }

}
