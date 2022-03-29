package com.groksoft.els.gui.tools.junkremover;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.gui.tools.AbstractTool;
import com.groksoft.els.gui.util.ArgumentTokenizer;
import com.groksoft.els.repository.Repository;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOCase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class JunkRemoverTool extends AbstractTool
{
    // @formatter:off

    // data members
    private ArrayList<JunkItem> junkList;

    // runtime data
    transient static final String ALL = "#ALL_LIBRARIES#";
    transient boolean dataHasChanged = false;
    transient private GuiContext guiContext = null;
    transient private NavTreeNode node;
    transient private boolean onPublisher = false;
    transient private boolean onSubscriber = false;
    transient private Repository repo;
    transient private NavTreeUserObject tuo;

    // @formatter:on

    public JunkRemoverTool(Configuration config, Context ctxt)
    {
        super(config, ctxt, "JunkRemover");
        setDisplayName(getCfg().gs("JunkRemover.displayName"));
    }

    public JunkRemoverTool(GuiContext guiContext)
    {
        super(guiContext.cfg, guiContext.context, "JunkRemover");
        setDisplayName(getCfg().gs("JunkRemover.displayName"));

        this.guiContext = guiContext;
    }

    public JunkItem addJunkItem()
    {
        if (junkList == null)
            createJunkList();
        JunkItem ji = new JunkItem();
        junkList.add(ji);
        setDataHasChanged(true);
        return ji;
    }

    /**
     * Parse defined arguments for this tool
     *
     *  -D | --dry-run
     *
     *  -l | --library [name] : Library to process
     *
     *  -L | --exclude [name] : Library to exclude
     *
     *  -p | --publisher : Run on publisher
     *
     *  -s | --subscriber : Run on subscriber
     *
     *
     * @throws Exception
     */
    @Override
    protected void argsParse() throws Exception
    {
        int index;
        // https://sourceforge.net/p/drjava/git_repo/ci/master/tree/drjava/src/edu/rice/cs/util/ArgumentTokenizer.java
        List<String> args = ArgumentTokenizer.tokenize(getArguments());

        for (index = 0; index < args.size(); ++index)
        {
            String arg = args.get(index);
            switch (arg)
            {
                case "-D":                                              // dry run
                    setDryRun(true);
                    break;
                case "-l":                                              // library
                    if (index < args.size() - 1)
                        guiContext.cfg.addPublisherLibraryName(args.get(++index));
                    else
                        throw new MungeException("-l | --library requires a library name");
                    break;
                case "-p":                                              // on publisher
                    onPublisher = true;
                    break;
                case "-s":                                              // on subscriber
                    onSubscriber = true;
                    break;
                default:
                    throw new MungeException(getInternalName() + ": " + "Unknown argument " + arg);
            }
        }
    }

    /**
     * Test values of arguments for sanity
     */
    @Override
    protected void argsTest() throws Exception
    {
        if (onPublisher == false && onSubscriber == false)
            throw new MungeException("Either -p or -s is required");
        if (onPublisher == true && onSubscriber == true)
            throw new MungeException("Both -p and -s cannot be used");
    }

    public JunkRemoverTool clone()
    {
        assert guiContext != null;
        JunkRemoverTool jrt = new JunkRemoverTool(guiContext);
        jrt.setArguments(getArguments());
        jrt.setConfigName(getConfigName());
        jrt.setDataHasChanged(isDataChanged());
        jrt.setDisplayName(getDisplayName());
        jrt.setDryRun(isDryRun());
        jrt.setInternalName(getInternalName());
        jrt.setJunkList(getJunkList());
        return jrt;
    }

    public void createJunkList()
    {
        junkList = new ArrayList<JunkItem>();
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
        // validate the arguments, no news is good news
        String msg;
        if ((msg = validate()).length() == 0)
        {
            if (guiContext != null)
            {
                // interactive

            }
            else
            {
                // command line
            }
        }
        else
        {
            // validation error, report for interactive or command line
        }

        if (guiContext != null)
        {
            // deep scan node and update tree
        }

        return false;
    }

    public void setDataHasChanged(boolean sense)
    {
        dataHasChanged = sense;
    }

    public void setGuiContext(GuiContext gctxt)
    {
        guiContext = gctxt;
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
            throw new MungeException("Error writing: " + getFullPath() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    // ================================================================================================================

    public class JunkItem implements Serializable
    {
        String wildcard;
        boolean caseSensitive = false;
    }

}
