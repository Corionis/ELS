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
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
    private boolean recursive = false;
    private String text1 = "";
    private String text2 = "";
    private String text3 = "";
    private boolean option1 = false;
    private boolean option2 = false;
    private boolean option3 = false;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private int renameCount = 0;
    transient private final boolean dualRepositories = false; // used by GUI, always false for this tool
    transient private GuiContext guiContext = null;
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private final boolean realOnly = true;
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
        renamer.setConfigName(this.getConfigName());
        renamer.setDisplayName(this.getDisplayName());
        renamer.setDataHasChanged();
        renamer.setIncludeInToolsList(this.isIncludeInToolsList());
        renamer.isDryRun = this.isDryRun;
        renamer.setIsRemote(this.isRemote());
        renamer.setType(this.getType());
        renamer.setSegment(this.getSegment());
        renamer.setIsRecursive(this.isRecursive());
        renamer.setText1(this.getText1());
        renamer.setText2(this.getText2());
        renamer.setText3(this.getText3());
        renamer.setOption1(this.isOption1());
        renamer.setOption2(this.isOption2());
        renamer.setOption3(this.isOption3());
        return renamer;
    }

    public String exec(String wholeName)
    {
        String change = "";
        String value = "";

        if (wholeName != null && wholeName.length() > 0)
        {
            String name = FilenameUtils.getBaseName(wholeName);
            String ext = FilenameUtils.getExtension(wholeName);

            switch (getSegment())
            {
                case 0: // Name only
                    value = name;
                    break;
                case 1: // Extension only
                    value = ext;
                    break;
                case 2: // Whole filename
                    value = wholeName;
                    break;
            }

            if (value.length() > 0)
            {
                switch (getType())
                {
                    case 0: // Case change
                        change = execCaseChange(value);
                        break;
                    case 1: // Insert
                        change = execInsert(value);
                        break;
                    case 2: // Numbering
                        change = execNumbering(value);
                        break;
                    case 3: // Remove
                        change = execRemove(value);
                        break;
                    case 4: // Replace
                        change = execReplace(value);
                        break;
                }
            }

            switch (getSegment())
            {
                case 0: // Name only
                    value = change + (ext.length() > 0 ? "." + ext : "");
                    break;
                case 1: // Extension only
                    value = name + "." + change;
                    break;
                case 2: // Whole filename
                    value = change;
                    break;
            }
        }
        return value;
    }

    private String execCaseChange(String value)
    {
        switch (getText1())
        {
            case "firstupper":
                value = value.toLowerCase(cfg.bundle().getLocale());
                String first = value.substring(0, 1);
                first = first.toUpperCase(cfg.bundle().getLocale());
                value = first + ((value.length() > 1) ? value.substring(1) : "");
                break;
            case "lower":
                value = value.toLowerCase(cfg.bundle().getLocale());
                break;
            case "titlecase":
                // use space or period as split separator?
                int sc = StringUtils.countMatches(value, " ");
                int pc = StringUtils.countMatches(value, ".");
                //  Which has more?
                int m = (getSegment() == 2 ? 2 : 1);
                String sep = (pc > m && pc > sc) ? "\\." : " ";

                String[] split = value.split(sep);
                value = "";
                for (int i = 0; i < split.length; ++i)
                {
                    String word = split[i];
                    word = word.toLowerCase(cfg.bundle().getLocale());
                    String fc;
                    if (word.length() == 1)
                        fc = word;
                    else
                        fc = word.substring(0, 1);
                    fc = fc.toUpperCase(cfg.bundle().getLocale());
                    word = fc + ((word.length() > 1) ? word.substring(1) : "");
                    value = value + (value.length() > 0 ? (sep != " " ? "." : " ") : "") + word;
                }
                break;
            case "upper":
                value = value.toUpperCase(cfg.bundle().getLocale());
                break;
        }
        return value;
    }

    private String execInsert(String value)
    {
        if (getText1().length() > 0)
        {
            if (isOption3())
                value = value + getText1();
            else
            {
                int pos = -1;
                try
                {
                    pos = Integer.parseInt(getText2());
                }
                catch (NumberFormatException e)
                {
                    pos = -1;
                }
                String left = value;
                String right = "";
                if (pos >= 0 && pos < value.length() - 1)
                {
                    // LEFTOFF debug change for At End and From End
                    if (isOption1())
                    {
                        left = value.substring(0, value.length() - pos - 1);
                        if (isOption2())
                            right = value.substring(value.length() - pos - 1 + getText1().length());
                        else
                            right = value.substring(value.length() - pos - 1);
                    }
                    else
                    {
                        left = value.substring(0, pos - 1);
                        if (isOption2())
                            right = value.substring(pos - 1 + getText1().length());
                        else
                            right = value.substring(pos - 1);
                    }
                    value = left + getText1() + right;

                }
            }
        }
        return value;
    }

    private String execNumbering(String value)
    {

        return value;
    }

    private String execRemove(String value)
    {

        return value;
    }

    private String execReplace(String value)
    {

        return value;
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

    public boolean isOption3()
    {
        return option3;
    }

    @Override
    public boolean isRealOnly()
    {
        return realOnly;
    }

    public boolean isRecursive()
    {
        return recursive;
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
            throw new MungeException(java.text.MessageFormat.format(cfg.gs("Renamer.uses.only.one.repository"), getInternalName()));

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

            scanForRenames(path, true);
        }

        if (guiContext != null)
        {
            guiContext.browser.printLog(getDisplayName() + ", " + getConfigName() + ": " + renameCount);

            // reset and reload relevant trees
            if (!isDryRun && renameCount > 0)
            {
                if (!repo.isSubscriber())
                {
                    guiContext.browser.deepScanCollectionTree(guiContext.mainFrame.treeCollectionOne, guiContext.context.publisherRepo, false, false);
                    guiContext.browser.deepScanSystemTree(guiContext.mainFrame.treeSystemOne, guiContext.context.publisherRepo, false, false);
                }
                else
                {
                    guiContext.browser.deepScanCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, isRemote(), false);
                    guiContext.browser.deepScanSystemTree(guiContext.mainFrame.treeSystemTwo, guiContext.context.subscriberRepo, isRemote(), false);
                }
            }
        }
        else
        {
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + renameCount);
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

    private String rename(String fullpath, String filename) throws Exception
    {
        boolean hidden = false;
        if (filename.startsWith(".") && filename.length() > 1)
        {
            filename = filename.substring(1);
            hidden = true;
        }
        String change = exec(filename); // execute the renamer sub-tool
        if (hidden)
        {
            change = "." + change;
            filename = "." + filename;
        }

        String newPath = "";
        if (!filename.equals(change))
        {
            String left = Utils.getLeftPath(fullpath, repo.getSeparator());
            newPath = left + repo.getSeparator() + change;
            if (!isDryRun)
            {
                getContext().transfer.rename(fullpath, newPath, isRemote());
                ++renameCount;
                fullpath = newPath;
                if (guiContext != null)
                    guiContext.browser.printLog(cfg.gs("Z.renamed") + filename + cfg.gs("Z.to") + change);
                else
                    logger.info(cfg.gs("Z.renamed") + filename + cfg.gs("Z.to") + change);
            }
            else
            {
                if (guiContext != null)
                    guiContext.browser.printLog(cfg.gs("Z.would.rename") + filename + cfg.gs("Z.to") + change);
                else
                    logger.info(cfg.gs("Z.would.rename") + filename + cfg.gs("Z.to") + change);
            }
        }
        return fullpath;
    }

    public void reset()
    {
        renameCount = 0;
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
        option3 = false;
    }

    private void scanForRenames(String path, boolean topLevel)
    {
        boolean firstDir = false;

        if (isRemote())
        {
            try
            {
                boolean pathIsDir = false;
                SftpATTRS attrs = getContext().clientSftp.stat(path);
                if (attrs.isDir() && topLevel)
                {
                    pathIsDir = true;
                    String filename = FilenameUtils.getName(path);
                    if (guiContext != null && guiContext.progress != null)
                    {
                        guiContext.progress.update(" " + filename);
                    }
                    String change = rename(path, filename);
                    boolean changed = !path.equals(change);
                    path = change;
                    if (!isRecursive())
                        return;
                }
                else if (attrs.isDir())
                    pathIsDir = true;

                Vector listing = getContext().clientSftp.listDirectory(path);
                for (int i = 0; i < listing.size(); ++i)
                {
                    if (isRequestStop())
                        break;
                    ChannelSftp.LsEntry entry = (ChannelSftp.LsEntry) listing.get(i);
                    if (!entry.getFilename().equals(".") && !entry.getFilename().equals(".."))
                    {
                        attrs = entry.getAttrs();
                        String filename = entry.getFilename();
                        String fullpath = path + (pathIsDir ? repo.getSeparator() + entry.getFilename() : "");
                        if (guiContext != null && guiContext.progress != null)
                        {
                            guiContext.progress.update(" " + fullpath);
                        }

                        String change = rename(fullpath, filename); // change & rename
                        boolean changed = !fullpath.equals(change);
                        fullpath = change;
                        if (attrs.isDir() && isRecursive())
                        {
                            scanForRenames(fullpath, false);
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
                if (file.isDirectory() && topLevel)
                {
                    firstDir = true;
                    File[] more = FileSystemView.getFileSystemView().getFiles(file.getAbsoluteFile(), false);
                    files = new File[more.length + 1];
                    files[0] = file;
                    for (int i = 0; i < more.length; ++i)
                    {
                        files[i + 1] = more[i];
                    }
                }
                else if (file.isDirectory())
                {
                    files = FileSystemView.getFileSystemView().getFiles(file.getAbsoluteFile(), false);
                }
                else
                {
                    files = new File[1];
                    files[0] = file;
                }

                String fullpath = "";
                boolean rescan = true;
                while (rescan)
                {
                    for (int i = 0; i < files.length; ++i)
                    {
                        rescan = false;
                        if (isRequestStop())
                            break;

                        File entry = files[i];
                        String filename = entry.getName();
                        boolean isDir = entry.isDirectory();
                        if (guiContext != null && guiContext.progress != null)
                        {
                            guiContext.progress.update(" " + filename);
                        }
                        fullpath = entry.getAbsolutePath();

                        String change = rename(fullpath, filename); // change & rename
                        boolean changed = !fullpath.equals(change);
                        fullpath = change;
                        if (isDir && !firstDir && isRecursive())
                        {
                            scanForRenames(fullpath, false);
                        }
                        else if (isDir && firstDir && changed)
                        {
                            rescan = true;
                            break;
                        }
                        firstDir = false;
                    }
                    if (rescan)
                    {
                        if (!isRecursive())
                            return;
                        firstDir = false;
                        File dir = new File(fullpath);
                        files = FileSystemView.getFileSystemView().getFiles(dir, false);
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

    public void setOption3(boolean option3)
    {
        if (this.option3 != option3)
        {
            this.option3 = option3;
            setDataHasChanged();
        }
    }

    public void setIsRecursive(boolean recursive)
    {
        this.recursive = recursive;
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
