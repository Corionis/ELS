package com.groksoft.els.tools.operations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.*;
import com.groksoft.els.gui.util.ArgumentTokenizer;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.groksoft.els.Configuration.Operations.StatusServer;

public class OperationsTool extends AbstractTool implements Comparable, Serializable
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Operations";
    public static final String SUBSYSTEM = "tools";
    public static enum Cards { Publisher, Listener, HintServer, Terminal, Quitter }

    private String configName; // user-specified name for this instance
    private String internalName = INTERNAL_NAME;
    private Configuration.Operations operation = Configuration.Operations.NotRemote;
    private Cards card = Cards.Publisher;
    private char[] optAuthorize = null; // -a | --authorize
    private String optAuthKeys = ""; // -A | --auth-keys
    private boolean optNoBackFill = false; // -b | --no-back-fill
    private String optBlacklist = ""; // -B | --blacklist
    private boolean optDryRun = false; // -D --dry-run
    private String optExportText = ""; // -e | --export-text
    private boolean optEmptyDirectories = false; // -E | --empty-directories
    private boolean optListenerKeepGoing = false; // -g | --listener-keep-going
    private boolean optListenerQuit = false; // -G | --listener-quit
    private String optHints = ""; // -h | --hints
    private String optHintServer = ""; // -H | --hint-server
    private String optExportItems = ""; // -i | --export-items
    private String optIpWhitelist = ""; // -I | --ip-whitelist
    private String optKeys = ""; // -k | --keys (Hint keys)
    private String optKeysOnly = ""; // -K | --key-only
    private String[] optLibrary; // -l | --library
    private String[] optExclude; // -L | --exclude
    private String optMismatches = ""; // -m | --mismatches
    private boolean optNavigator = false; // -n | --navigator
    private boolean optIgnored = false; // -N | --ignored
    private boolean optOverwrite = false; // -o | --overwrite
    private boolean optQuitStatus = false; // -q | --quit-status
    private boolean optForceQuit = false; // -Q | --force-quit
    private String optTargets = ""; // -t | --targets
    private boolean optDuplicates = false; // -u | --duplicates
    private boolean optValidate = false; // -v | --validate
    private String optWhatsNew = ""; // -w | --whatsnew
    private String optWhatsNewAll = ""; // -W | --whatsnew-all
    private boolean optCrossCheck = false; // -x | --cross-check
    private boolean optPreserveDates = false; // -y | --preserve-dates
    private boolean optDecimalScale = false; // -z | --decimal-scale

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private Logger logger = LogManager.getLogger("applog");
    transient private String pubPath;
    transient private String subPath;
    transient boolean stop = false;
    // @formatter-on

    public OperationsTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("Navigator.splitPane.Operations.tab.title"));
        this.originPathsAllowed = false;
    }

    public OperationsTool clone()
    {
        OperationsTool tool = new OperationsTool(this.context);
        tool.setConfigName(getConfigName());
        tool.internalName = INTERNAL_NAME;
        tool.setOperation(getOperation());
        tool.setOptAuthorize(getOptAuthorize());
        tool.setOptAuthKeys(getOptAuthKeys());
        tool.setOptNoBackFill(isOptNoBackFill());
        tool.setOptBlacklist(getOptBlacklist());
        tool.setOptDryRun(isOptDryRun());
        tool.setOptExportText(getOptExportText());
        tool.setOptEmptyDirectories(isOptEmptyDirectories());
        tool.setOptListenerKeepGoing(isOptListenerKeepGoing());
        tool.setOptListenerQuit(isOptListenerQuit());
        tool.setOptHints(getOptHints());
        tool.setOptHintServer(getOptHintServer());
        tool.setOptExportItems(getOptExportItems());
        tool.setOptIpWhitelist(getOptIpWhitelist());
        tool.setOptKeys(getOptKeys());
        tool.setOptKeysOnly(getOptKeysOnly());
//        tool.setOptLibrary(getOptLibrary().clone());
//        tool.setOptExclude(getOptExclude().clone());
        tool.setOptMismatches(getOptMismatches());
        tool.setOptNavigator(isOptNavigator());
        tool.setOptIgnored(isOptIgnored());
        tool.setOptOverwrite(isOptOverwrite());
        tool.setOptQuitStatus(isOptQuitStatus());
        tool.setOptForceQuit(isOptForceQuit());
        tool.setOptTargets(getOptTargets());
        tool.setOptDuplicates(isOptDuplicates());
        tool.setOptValidate(isOptValidate());
        tool.setOptWhatsNew(getOptWhatsNew());
        tool.setOptWhatsNewAll(getOptWhatsNewAll());
        tool.setOptCrossCheck(isOptCrossCheck());
        tool.setOptPreserveDates(isOptPreserveDates());
        tool.setOptDecimalScale(isOptDecimalScale());

        tool.setIncludeInToolsList(isIncludeInToolsList());
        return tool;
    }

    public String generateCommandLine()
    {
        Configuration defCfg = new Configuration(context);
        boolean glo = context.preferences != null ? context.preferences.isGenerateLongOptions() : false;
        StringBuilder sb = new StringBuilder();

        // --- non-munging actions
        if (isOptNavigator() != defCfg.isNavigator())
            sb.append(" " + (glo ? "--navigator" : "-n"));
        if (isOptForceQuit())
            sb.append(" " + (glo ? "--force-quit" : "-Q"));
        if (isOptQuitStatus())
            sb.append(" " + (glo ? "--quit-status" : "-q"));

        if (isOptDryRun() != defCfg.isDryRun())
            sb.append(" " + (glo ? "--dry-run" : "-D"));

        // --- remote mode
        switch (operation)
        {
            case PublishRemote:
                sb.append(" " + (glo ? "--remote" : "-r") + " P");
                break;
            case SubscriberListener:
                sb.append(" " + (glo ? "--remote" : "-r") + " S");
                break;
            case PublisherListener:
                sb.append(" " + (glo ? "--remote" : "-r") + " L");
                break;
            case PublisherManual:
                sb.append(" " + (glo ? "--remote" : "-r") + " M");
                break;
            case SubscriberTerminal:
                sb.append(" " + (glo ? "--remote" : "-r") + " T");
                break;
            case NotRemote:
            case StatusServer:
            case StatusServerForceQuit:
            case SubscriberListenerForceQuit:
                break;
        }

        // --- libraries
        if (operation != StatusServer)
        {
            if (pubPath != null && pubPath.length() > 0)
                sb.append(" " + (glo ? "--publisher-libraries" : "-p") + " \"" + pubPath + "\"");
            if (subPath != null && subPath.length() > 0)
                sb.append(" " + (glo ? "--subscriber-libraries" : "-s") + " \"" + subPath + "\"");
        }

        // --- targets
        switch (operation)
        {
            case NotRemote:
            case PublisherListener:
            case PublishRemote:
            case SubscriberListener:
            case SubscriberListenerForceQuit:
                sb.append(" " + (glo ? "--targets" : "-t"));
                if (getOptTargets().length() > 0)
                    sb.append(" \"" + getOptTargets() + "\"");
                break;
            case PublisherManual:
            case StatusServer:
            case StatusServerForceQuit:
            case SubscriberTerminal:
                break;
        }

        // --- hint keys
        if (getOptKeys().length() > 0)
            sb.append(" " + (glo ? "--keys" : "-k") + " \"" + getOptKeys() + "\"");
        if (getOptKeysOnly().length() > 0)
            sb.append(" " + (glo ? "--keys-only" : "-K") + " \"" + getOptKeysOnly() + "\"");

        // --- hints & hint server
        if (getOptHints().length() > 0)
            sb.append(" " + (glo ? "--hints" : "-h") + " \"" + getOptHints() + "\"");
        if (getOptHintServer().length() > 0)
            sb.append(" " + (glo ? "--hint-server" : "-H") + " \"" + getOptHintServer() + "\"");

        // --- security
        if (getOptAuthorize() != null && getOptAuthorize().length > 0)
            sb.append(" " + (glo ? "--authorize" : "-a") + " \"" + getOptAuthorizeString() + "\"");
        if (getOptAuthKeys().length() > 0)
            sb.append(" " + (glo ? "--auth-keys" : "-A") + " \"" + getOptAuthKeys() + "\"");
        if (getOptBlacklist().length() > 0)
            sb.append(" " + (glo ? "--blacklist" : "-B") + " \"" + getOptBlacklist() + "\"");
        if (getOptIpWhitelist().length() > 0)
            sb.append(" " + (glo ? "--ip-whitelist" : "-I") + " \"" + getOptIpWhitelist() + "\"");

        // --- exports
        if (getOptExportText().length() > 0)
            sb.append(" " + (glo ? "--export-text" : "-e") + " \"" + getOptExportText() + "\"");
        if (getOptExportItems().length() > 0)
            sb.append(" " + (glo ? "--export-items" : "-i") + " \"" + getOptExportItems() + "\"");

        // --- include/exclude libraries
        // TODO add include/exclude libraries

        // --- differences
        if (getOptMismatches().length() > 0)
            sb.append(" " + (glo ? "--mismatches" : "-m") + " \"" + getOptMismatches() + "\"");
        if (getOptWhatsNew().length() > 0)
            sb.append(" " + (glo ? "--whatsnew" : "-w") + " \"" + getOptWhatsNew() + "\"");
        if (getOptWhatsNewAll().length() > 0)
            sb.append(" " + (glo ? "--whatsnew-all" : "-W") + " \"" + getOptWhatsNewAll() + "\"");

        // --- options
        if (isOptDecimalScale() != !defCfg.isBinaryScale())
            sb.append(" " + (glo ? "--decimal-scale" : "-z"));
        if (isOptDuplicates() != defCfg.isDuplicateCheck())
            sb.append(" " + (glo ? "--duplicates" : "-u"));
        if (isOptEmptyDirectories() != defCfg.isEmptyDirectoryCheck())
            sb.append(" " + (glo ? "--empty-directories" : "-E"));
        if (isOptCrossCheck() != defCfg.isCrossCheck())
            sb.append(" " + (glo ? "--cross-check" : "-x"));
        if (isOptIgnored() != defCfg.isIgnoredReported())
            sb.append(" " + (glo ? "--ignored" : "-N"));
        if (isOptListenerQuit() != defCfg.isQuitSubscriberListener())
            sb.append(" " + (glo ? "--listener-quit" : "-G"));
        if (isOptListenerKeepGoing() != defCfg.isKeepGoing())
            sb.append(" " + (glo ? "--listener-keep-going" : "-g"));
        if (isOptNoBackFill() != defCfg.isNoBackFill())
            sb.append(" " + (glo ? "--no-back-fill" : "-B"));
        if (isOptOverwrite() != defCfg.isOverwrite())
            sb.append(" " + (glo ? "--overwrite" : "-o"));
        if (isOptPreserveDates() != defCfg.isPreserveDates())
            sb.append(" " + (glo ? "--preserve-dates" : "-y"));
        if (isOptValidate() != defCfg.isValidation())
            sb.append(" " + (glo ? "--validate" : "-v"));

        return sb.toString().trim();
    }

    public String generateCommandLine(String pubPath, String subPath)
    {
        this.pubPath = pubPath;
        this.subPath = subPath;
        return generateCommandLine();
    }

    @Override
    public String getConfigName()
    {
        return configName;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    public Cards getCard()
    {
        return card;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    public Configuration.Operations getOperation()
    {
        return operation;
    }

    public String getOptAuthKeys()
    {
        return optAuthKeys;
    }

    public char[] getOptAuthorize()
    {
        return optAuthorize;
    }

    public String getOptAuthorizeString()
    {
        return new String(optAuthorize);
    }

    public String getOptBlacklist()
    {
        return optBlacklist;
    }

    public String[] getOptExclude()
    {
        return optExclude;
    }

    public String getOptExportItems()
    {
        return optExportItems;
    }

    public String getOptExportText()
    {
        return optExportText;
    }

    public String getOptHints()
    {
        return optHints;
    }

    public String getOptHintServer()
    {
        return optHintServer;
    }

    public String getOptIpWhitelist()
    {
        return optIpWhitelist;
    }

    public String getOptKeys()
    {
        return optKeys;
    }

    public String getOptKeysOnly()
    {
        return optKeysOnly;
    }

    public String[] getOptLibrary()
    {
        return optLibrary;
    }

    public String getOptMismatches()
    {
        return optMismatches;
    }

    public String getOptTargets()
    {
        return optTargets;
    }

    public String getOptWhatsNew()
    {
        return optWhatsNew;
    }

    public String getOptWhatsNewAll()
    {
        return optWhatsNewAll;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    @Override
    public boolean isCachedLastTask()
    {
        return false;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    @Override
    public boolean isDualRepositories()
    {
        return true;
    }

    public boolean isOptCrossCheck()
    {
        return optCrossCheck;
    }

    public boolean isOptDecimalScale()
    {
        return optDecimalScale;
    }

    public boolean isOptDryRun()
    {
        return optDryRun;
    }

    public boolean isOptDuplicates()
    {
        return optDuplicates;
    }

    public boolean isOptEmptyDirectories()
    {
        return optEmptyDirectories;
    }

    public boolean isOptForceQuit()
    {
        return optForceQuit;
    }

    public boolean isOptIgnored()
    {
        return optIgnored;
    }

    public boolean isOptListenerKeepGoing()
    {
        return optListenerKeepGoing;
    }

    public boolean isOptListenerQuit()
    {
        return optListenerQuit;
    }

    public boolean isOptNavigator()
    {
        return optNavigator;
    }

    public boolean isOptNoBackFill()
    {
        return optNoBackFill;
    }

    public boolean isOptOverwrite()
    {
        return optOverwrite;
    }

    public boolean isOptPreserveDates()
    {
        return optPreserveDates;
    }

    public boolean isOptQuitStatus()
    {
        return optQuitStatus;
    }

    public boolean isOptValidate()
    {
        return optValidate;
    }

    @Override
    public boolean isRealOnly()
    {
        return false;
    }

    /**
     * Process an Operations tool
     * <br/>
     * Used by by a Job when executing an Operations task
     *
     * @param context The runtime Context
     * @param publisherPath Repository of the publisher or null
     * @param subscriberPath Repository of the subscriber or null
     * @throws Exception
     */
    @Override
    public void processTool(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        pubPath = publisherPath;
        subPath = subscriberPath;

        // construct the arguments
        String cmd = generateCommandLine();
        if (dryRun && !cmd.contains(" -D") && !cmd.contains(" --dry-run"))
        {
            boolean glo = context.preferences != null ? context.preferences.isGenerateLongOptions() : false;
            cmd += (" " + (glo ? "--dry-run" : "-D"));
        }
        List<String> list = ArgumentTokenizer.tokenize(cmd);
        String[] args = list.toArray(new String[0]);

        // run the Operation
        logger.info(context.cfg.gs("Operations.launching") + getConfigName());
        Main main = new Main(args, context, getConfigName());
        logger.info(getConfigName() + context.cfg.gs("Z.completed"));
    }

    @Override
    public void processTool(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception
    {
        // to satisfy AbstractTool, not used
    }

    /**
     * Process an Operations tool on a SwingWorker thread
     * <br/>
     * Used by the Run button of the tool
     *
     * @param context The runtime Context
     * @param publisherPath Repository of the publisher or null
     * @param subscriberPath Repository of the subscriber or null
     * @return SwingWorker<Void, Void> of thread
     */
    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, String publisherPath, String subscriberPath, boolean dryRun) throws Exception
    {
        // create a fresh dialog
        if (context.progress == null || !context.progress.isBeingUsed())
        {
/*
            ActionListener cancel = new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    requestStop();
                }
            };
            context.progress = new Progress(context, context.mainFrame.panelOperationTop, cancel, ((dryRun) ? dryRun : optDryRun));
            context.progress.display();
*/
        }
        else
        {
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.please.wait.for.the.current.operation.to.finish"), context.cfg.gs("Navigator.splitPane.Operations.tab.title"), JOptionPane.WARNING_MESSAGE);
            return null;
        }

        // using currently-loaded repositories means there is no change in connection

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>()
        {
            @Override
            protected Void doInBackground() throws Exception
            {
                try
                {
                    processTool(context, publisherPath, subscriberPath, ((dryRun) ? dryRun : optDryRun));
                }
                catch (Exception e)
                {
                    String msg = context.cfg.gs("Z.exception") + e.getMessage() + "; " + Utils.getStackTrace(e);
                    logger.error(msg);
                    if (context.navigator != null)
                        JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("Navigator.splitPane.Operations.tab.title"), JOptionPane.ERROR_MESSAGE);
                }
                return null;
            }
        };
        return worker;
    }

    @Override
    public SwingWorker<Void, Void> processToolThread(Context context, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
        // to satisfy AbstractTool, not used
        return null;
    }

    public void requestStop()
    {
        this.stop = true;
    }

    public void resetStop()
    {
        this.stop = false;
    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setCard(Cards card)
    {
        this.card = card;
    }

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    public void setDataHasChanged(boolean sense)
    {
        dataHasChanged = sense;
    }

    public void setOptAuthKeys(String optAuthKeys)
    {
        this.optAuthKeys = optAuthKeys;
    }

    public void setOptAuthorize(char[] optAuthorize)
    {
        this.optAuthorize = optAuthorize;
    }

    public void setOptBlacklist(String optBlacklist)
    {
        this.optBlacklist = optBlacklist;
    }

    public void setOptCrossCheck(boolean optCrossCheck)
    {
        this.optCrossCheck = optCrossCheck;
    }

    public void setOptDecimalScale(boolean optDecimalScale)
    {
        this.optDecimalScale = optDecimalScale;
    }

    public void setOptDryRun(boolean optDryRun)
    {
        this.optDryRun = optDryRun;
    }

    public void setOptDuplicates(boolean optDuplicates)
    {
        this.optDuplicates = optDuplicates;
    }

    public void setOptEmptyDirectories(boolean optEmptyDirectories)
    {
        this.optEmptyDirectories = optEmptyDirectories;
    }

    public void setOptExclude(String[] optExclude)
    {
        this.optExclude = optExclude;
    }

    public void setOptExportItems(String optExportItems)
    {
        this.optExportItems = optExportItems;
    }

    public void setOptExportText(String optExportText)
    {
        this.optExportText = optExportText;
    }

    public void setOptForceQuit(boolean optForceQuit)
    {
        this.optForceQuit = optForceQuit;
    }

    public void setOptHintServer(String optHintServer)
    {
        this.optHintServer = optHintServer;
    }

    public void setOptHints(String optHints)
    {
        this.optHints = optHints;
    }

    public void setOptIgnored(boolean optIgnored)
    {
        this.optIgnored = optIgnored;
    }

    public void setOptIpWhitelist(String optIpWhitelist)
    {
        this.optIpWhitelist = optIpWhitelist;
    }

    public void setOptKeys(String optKeys)
    {
        this.optKeys = optKeys;
    }

    public void setOptKeysOnly(String optKeysOnly)
    {
        this.optKeysOnly = optKeysOnly;
    }

    public void setOptLibrary(String[] optLibrary)
    {
        this.optLibrary = optLibrary;
    }

    public void setOptListenerKeepGoing(boolean optListenerKeepGoing)
    {
        this.optListenerKeepGoing = optListenerKeepGoing;
    }

    public void setOptListenerQuit(boolean optListenerQuit)
    {
        this.optListenerQuit = optListenerQuit;
    }

    public void setOptMismatches(String optMismatches)
    {
        this.optMismatches = optMismatches;
    }

    public void setOptNavigator(boolean optNavigator)
    {
        this.optNavigator = optNavigator;
    }

    public void setOptNoBackFill(boolean optNoBackFill)
    {
        this.optNoBackFill = optNoBackFill;
    }

    public void setOptOverwrite(boolean optOverwrite)
    {
        this.optOverwrite = optOverwrite;
    }

    public void setOptPreserveDates(boolean optPreserveDates)
    {
        this.optPreserveDates = optPreserveDates;
    }

    public void setOptQuitStatus(boolean optQuitStatus)
    {
        this.optQuitStatus = optQuitStatus;
    }

    public void setOptTargets(String optTargets)
    {
        this.optTargets = optTargets;
    }

    public void setOptValidate(boolean optValidate)
    {
        this.optValidate = optValidate;
    }

    public void setOptWhatsNew(String optWhatsNew)
    {
        this.optWhatsNew = optWhatsNew;
    }

    public void setOptWhatsNewAll(String optWhatsNewAll)
    {
        this.optWhatsNewAll = optWhatsNewAll;
    }

    public void setOperation(Configuration.Operations operation)
    {
        this.operation = operation;
    }

    public void write() throws Exception
    {
        setOptAuthKeys(context.main.makeRelativeWorkingPath(getOptAuthKeys()));
        setOptBlacklist(context.main.makeRelativeWorkingPath(getOptBlacklist()));
        setOptExportItems(context.main.makeRelativeWorkingPath(getOptExportItems()));
        setOptExportText(context.main.makeRelativeWorkingPath(getOptExportText()));
        setOptHints(context.main.makeRelativeWorkingPath(getOptHints()));
        setOptHintServer(context.main.makeRelativeWorkingPath(getOptHintServer()));
        setOptIpWhitelist(context.main.makeRelativeWorkingPath(getOptIpWhitelist()));
        setOptKeys(context.main.makeRelativeWorkingPath(getOptKeys()));
        setOptKeysOnly(context.main.makeRelativeWorkingPath(getOptKeysOnly()));
        setOptMismatches(context.main.makeRelativeWorkingPath(getOptMismatches()));
        setOptTargets(context.main.makeRelativeWorkingPath(getOptTargets()));
        setOptWhatsNew(context.main.makeRelativeWorkingPath(getOptWhatsNew()));
        setOptWhatsNewAll(context.main.makeRelativeWorkingPath(getOptWhatsNewAll()));

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
