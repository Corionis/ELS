package com.groksoft.els.tools.renamer;

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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

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
    private boolean filesOnly = true;
    private String text1 = "";
    private String text2 = "";
    private String text3 = "";
    private boolean option1 = false;
    private boolean option2 = false;
    private boolean option3 = false;

    transient private int counter;
    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private int renameCount = 0;
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private Repository repo; // this tool only uses one repo
    transient private ArrayList<String> physicalPaths;
    transient private ArrayList<String> forwardPaths;
    // @formatter:on

    /**
     * Constructor when used from the command line
     *
     * @param context   Context
     */
    public RenamerTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("Renamer.displayName"));
        this.context = context;
    }

    public RenamerTool clone()
    {
        assert context != null;
        RenamerTool renamer = new RenamerTool(context);
        renamer.setConfigName(this.getConfigName());
        renamer.setDisplayName(this.getDisplayName());
        renamer.setDataHasChanged();
        renamer.setIncludeInToolsList(this.isIncludeInToolsList());
        renamer.isDryRun = this.isDryRun;
        renamer.setIsRemote(this.isRemote());
        renamer.setType(this.getType());
        renamer.setSegment(this.getSegment());
        renamer.setIsRecursive(this.isRecursive());
        renamer.setIsFilesOnly(this.isFilesOnly());
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
        // Type of case change = text1
        switch (getText1())
        {
            case "firstupper":
                value = value.toLowerCase(context.cfg.bundle().getLocale());
                String first = value.substring(0, 1);
                first = first.toUpperCase(context.cfg.bundle().getLocale());
                value = first + ((value.length() > 1) ? value.substring(1) : "");
                break;
            case "lower":
                value = value.toLowerCase(context.cfg.bundle().getLocale());
                break;
            case "titlecase":
                // use space or period as split separator?
                int sc = StringUtils.countMatches(value, " ");
                int pc = StringUtils.countMatches(value, ".");
                //  Which has more?
                int m = (getSegment() == 2 ? 2 : 1);
                String sep = (pc > m && pc > sc) ? "\\." : " "; // ????

                String[] split = value.split(sep);
                value = "";
                for (int i = 0; i < split.length; ++i)
                {
                    String word = split[i];
                    word = word.toLowerCase(context.cfg.bundle().getLocale());
                    String fc;
                    if (word.length() == 1)
                        fc = word;
                    else
                        fc = word.substring(0, 1);   // FIXME problem with multiple spaces????????????
                    fc = fc.toUpperCase(context.cfg.bundle().getLocale());
                    word = fc + ((word.length() > 1) ? word.substring(1) : "");
                    value = value + (value.length() > 0 ? (sep != " " ? "." : " ") : "") + word; // ????
                }
                break;
            case "upper":
                value = value.toUpperCase(context.cfg.bundle().getLocale());
                break;
        }
        return value;
    }

    private String execInsert(String value)
    {
        // Text to insert = text1
        // Insert position = text2
        // From end = option1
        // At end = option2
        // Overwrite = option3
        int pos = Integer.parseInt(getText2());
        value = insertString(value, getText1(), pos, isOption1(), isOption2(), isOption3());
        return value;
    }

    private String execNumbering(String value)
    {
        // Start = text1
        // Number of zeros = text2
        // Insert position = text2
        // From end = option1
        // At end = option2
        // Overwrite = option3
        int start = Integer.parseInt(getText1());
        counter = (counter < 0 || counter < start) ? start : ++counter;

        int zeros = Integer.parseInt(getText2());
        String format = StringUtils.repeat('0', zeros);
        DecimalFormat formatter = new DecimalFormat(format);
        String numString = formatter.format(counter);

        int pos = Integer.parseInt(getText3());
        value = insertString(value, numString, pos, isOption1(), isOption2(), isOption3());
        return value;
    }

    private String execRemove(String value)
    {
        // From = text1
        // Length = text2
        // From end = option1
        int from = Integer.parseInt(getText1());
        int length = Integer.parseInt(getText2());
        if (isOption1())
        {
            if (value.length() - length >= 0)
                value = value.substring(0,value.length() - length);
            else
                value = "";
        }
        else
        {
            value = value.substring(0, from) + value.substring(from + length);
        }
        return value;
    }

    private String execReplace(String value)
    {
        // Find = text1
        // Replace = text2
        // Regular expression = option1
        // Case sensitive = option2
        if (isOption1())
        {
            Pattern patt = Pattern.compile(getText1());
            value = value.replaceAll(patt.toString(), getText2());
        }
        else
        {
            if (isOption2())
                value = StringUtils.replaceIgnoreCase(value, getText1(), getText2());
            else
                value = value.replace(getText1(), getText2());
        }
        return value;
    }

    private int expandOrigins(ArrayList<Origin> origins, Task lastTask) throws MungeException
    {
        int count = 0;

        // this tool only uses one repository
        if (repo == null)
            return -1;

        if (lastTask != null)
        {
            physicalPaths = lastTask.getTool().getForwardPaths();
            count = physicalPaths.size();
        }
        else
        {
            for (Origin origin : origins)
            {
                if (origin.getType() == NavTreeUserObject.COLLECTION)
                {
                    if (origin.getName().length() > 0)
                    {
                        if (!repo.getLibraryData().libraries.description.equalsIgnoreCase(origin.getName()))
                            throw new MungeException((getCfg().gs("Renamer.task.definition.and.loaded.repository.do.not.match")));
                    }
                    // process in the order defined in the JSON
                    for (Library lib : repo.getLibraryData().libraries.bibliography)
                    {
                        for (String source : lib.sources)
                        {
                            physicalPaths.add(source);
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
                                physicalPaths.add(source);
                                ++count;
                            }
                        }
                    }
                }
                else if (origin.getType() == NavTreeUserObject.REAL)
                {
                    physicalPaths.add(origin.getName());
                    ++count;
                }
            }
        }

        return count;
    }

    public String getConfigName()
    {
        return configName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public ArrayList<String> getForwardPaths()
    {
        return forwardPaths;
    }

    public String getInternalName()
    {
        return internalName;
    }

    public int getSegment()
    {
        return segment;
    }

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

    private String insertString(String value, String insert, int pos, boolean fromEnd, boolean atEnd, boolean overwrite)
    {
        if (insert.length() > 0)
        {
            if (atEnd) // at end
                value = value + insert;
            else
            {
                String left = value;
                String right = "";
                if (pos < 0)
                    pos = 0;
                if (pos > value.length())
                    pos = value.length();
                if (fromEnd) // from end
                {
                    left = value.substring(0, value.length() - pos);
                    if (overwrite) // overwrite
                    {
                        int x = value.length() - pos + insert.length();
                        if (x < value.length()) // otherwise empty
                            right = value.substring(x);
                    }
                    else
                        right = value.substring(value.length() - pos);
                }
                else // inline
                {
                    left = value.substring(0, pos);
                    if (overwrite) // overwrite
                        right = value.substring(pos + insert.length());
                    else
                        right = value.substring(pos);
                }

                value = left + insert + right;
            }
        }
        return value;
    }

    public boolean isCachedLastTask()
    {
        return true;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    public boolean isDualRepositories()
    {
        return false;
    }

    public boolean isFilesOnly()
    {
        return filesOnly;
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

    public boolean isRealOnly()
    {
        return false;
    }

    public boolean isRecursive()
    {
        return recursive;
    }

    @Override
    public void processTool(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    /**
     * Process the tool with the metadata provided
     * <br/>
     * Uses the junkList across the toolPaths added by addToolPaths() and the dryRun setting.
     * The addToolPaths() method must be called first.
     * <br/>
     * Used by a Job & the Run button of the tool
     */
    public void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception
    {
        reset();
        isDryRun = dryRun;

        if (publisherRepo != null && subscriberRepo != null)
            throw new MungeException(java.text.MessageFormat.format(getCfg().gs("Renamer.uses.only.one.repository"), getInternalName()));

        // this tool only uses one repository
        repo = (publisherRepo != null) ? publisherRepo : subscriberRepo;

        // expand origins into physical physical paths
        int count = expandOrigins(origins, lastTask);
        if (physicalPaths == null || physicalPaths.size() == 0)
            return;

        // only subscribers can be remote
        if (subscriberRepo != null && getCfg().isRemoteSession())
            setIsRemote(true);

        for (String path : physicalPaths)
        {
            if (isRequestStop())
                break;
            String rem = isRemote() ? getCfg().gs("Z.remote.uppercase") : "";
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + rem + "\"" + path + "\"");

            scanForRenames(path, true);
        }

        if (context.navigator != null)
        {
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + renameCount + (isDryRun ? getCfg().gs("Z.dry.run") : ""));

            // reset and reload relevant trees
            if (!isDryRun && renameCount > 0)
            {
                if (!repo.isSubscriber())
                {
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false, false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false, false);
                }
                else
                {
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, isRemote(), false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, isRemote(), false);
                }
            }
        }
        else
        {
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + renameCount + (isDryRun ? getCfg().gs("Z.dry.run") : ""));
        }
        logger.info(getConfigName() + context.cfg.gs("Z.completed"));
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // to satisfy AbstractTool, not used
        return null;
    }

    /**
     * Process the task on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param context        The Context
     * @param publisherRepo  Publisher repo, or null
     * @param subscriberRepo Subscriber repo, or null
     * @param origins        List of origins to process
     * @param dryRun         Boolean for a dry-run
     * @return SwingWorker<Void, Void> of thread
     */
    public SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        // create a fresh dialog
        if (context.navigator != null)
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
                context.progress.display();
            }
            else
            {
                JOptionPane.showMessageDialog(context.mainFrame, getCfg().gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
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
                    String msg = getCfg().gs("Z.exception") + " " + Utils.getStackTrace(e);
                    if (context.navigator != null)
                    {
                        logger.error(msg);
                        JOptionPane.showMessageDialog(context.mainFrame, msg, getCfg().gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
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

    private String rename(String fullpath, String filename, boolean isDirectory) throws Exception
    {
        boolean hidden = false;
        if (!isDirectory || !isFilesOnly())
        {
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
                ++renameCount;
                if (context.mainFrame != null)
                    context.mainFrame.labelStatusMiddle.setText(getCfg().gs("Z.count") + renameCount);
                if (!isDryRun)
                {
                    getContext().transfer.rename(fullpath, newPath, isRemote());
                    fullpath = newPath;
                    logger.info("  " + getCfg().gs("Z.renamed") + "\"" + filename + "\"" + getCfg().gs("Z.to") + "\"" + change + "\"");
                }
                else
                {
                    logger.info("  " + getCfg().gs("Z.would.rename") + "\"" + filename + "\"" + getCfg().gs("Z.to") + "\"" + change + "\"");
                }
            }
        }
        forwardPaths.add(fullpath);
        return fullpath;
    }

    public void reset()
    {
        counter = -1;
        renameCount = 0;
        resetStop();
        physicalPaths = new ArrayList<>();
        forwardPaths = new ArrayList<String>();
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
                    if (context.navigator != null && context.progress != null)
                    {
                        context.progress.update(" " + filename);
                    }
                    String change = rename(path, filename, true);
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
                        if (context.navigator != null && context.progress != null)
                        {
                            context.progress.update(" " + fullpath);
                        }

                        if (!attrs.isDir())
                        {
                            String change = rename(fullpath, filename, false); // change & rename
                            boolean changed = !fullpath.equals(change);
                            fullpath = change;
                        }
                        if (attrs.isDir() && isRecursive())
                        {
                            scanForRenames(fullpath, false);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                String msg = getCfg().gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (context.navigator != null)
                {
                    logger.error(msg);
                    JOptionPane.showMessageDialog(context.mainFrame, msg, getCfg().gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                }
                else
                    logger.error(msg);
                requestStop();
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
                        if (context.navigator != null && context.progress != null)
                        {
                            context.progress.update(" " + filename);
                        }
                        fullpath = entry.getAbsolutePath();

                        boolean changed = false;
                        if (!isDir)
                        {
                            String change = rename(fullpath, filename, isDir); // change & rename
                            changed = !fullpath.equals(change);
                            fullpath = change;
                        }
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
                String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (context.navigator != null)
                {
                    logger.error(msg);
                    JOptionPane.showMessageDialog(context.mainFrame, msg, getCfg().gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                }
                else
                    logger.error(msg);
                requestStop();
            }
        }
    }

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

    public void setIsFilesOnly(boolean filesOnly)
    {
        this.filesOnly = filesOnly;
    }

    public void setForwardPaths(ArrayList<String> forwardPaths)
    {
        this.forwardPaths = forwardPaths;
    }

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
