package com.corionis.els.tools.operations;

import com.corionis.els.*;
import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.gui.util.ArgumentTokenizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class OperationsTool extends AbstractTool
{
    // @formatter:off

    // Elements match card names in lowercase
    // Jobs PubSub handling and Origins always disabled for Operations
    //  * Publisher & Subscriber
    //     + Publisher
    //     + Listener
    //     + Terminal
    //     + SubscriberQuit
    //  * Publisher
    //     + StatusQuit
    //  * None, PubSub disabled
    //     + HintServer
    //
    // @see JobsUI.getOriginWant()
    
    public enum Cards
    {
        Publisher, Listener, HintServer, Terminal, SubscriberQuit, StatusQuit
    }

    public enum Operations
    {
        PublisherOperation, SubscriberListener, PublisherManual, PublisherListener,
        SubscriberTerminal, StatusServer, StatusServerQuit, SubscriberListenerQuit
    }

    public static final String INTERNAL_NAME = "Operations";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user-specified name for this instance
    private String internalName = INTERNAL_NAME;
    private Operations operation = Operations.PublisherOperation;
    private Cards card = Cards.Publisher;
    private char[] optAuthorize = null; // -a | --authorize
    private String optAuthKeys = ""; // -A | --auth-keys
    private String optBlacklist = ""; // -B | --blacklist
    private boolean optCrossCheck = false; // -x | --cross-check
    private boolean optDecimalScale = false; // -z | --decimal-scale
    private boolean optDuplicates = false; // -u | --duplicates
    private boolean optEmptyDirectories = false; // -E | --empty-directories
    private String[] optExclude; // -L | --exclude
    private String optExportItems = ""; // -i | --export-items
    private String optExportText = ""; // -e | --export-text
    private boolean optForceQuit = false; // -Q | --force-quit
    private boolean optIgnored = false; // -N | --ignored
    private String optIpWhitelist = ""; // -I | --ip-whitelist
    private String optKeys = ""; // -k | --keys (Hint keys)
    private String optKeysOnly = ""; // -K | --key-only
    private String[] optLibrary; // -l | --library
    private boolean optListenerKeepGoing = false; // -g | --listener-keep-going
    private boolean optListenerQuit = false; // -G | --listener-quit
    private String optMismatches = ""; // -m | --mismatches
    private boolean optNavigator = false; // -n | --navigator
    private boolean optNoBackFill = false; // -b | --no-back-fill
    private boolean optOverwrite = false; // -o | --overwrite
    private boolean optPreserveDates = false; // -y | --preserve-dates
    private boolean optQuitStatus = false; // -q | --quit-status
    private String optTargets = ""; // -t | --targets
    private boolean optValidate = false; // -v | --validate
    private String optWhatsNew = ""; // -w | --whatsnew
    private String optWhatsNewAll = ""; // -W | --whatsnew-all

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private Logger logger = LogManager.getLogger("applog");
    transient private String hintPath;
    transient private String pubPath;
    transient private String subPath;
    transient private Task task = null;
    transient boolean stop = false;
    // @formatter-on

    public OperationsTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("Operations.displayName"));
    }

    public OperationsTool clone()
    {
        OperationsTool tool = new OperationsTool(this.context);
        tool.setConfigName(getConfigName());
        tool.internalName = INTERNAL_NAME;
        tool.setOperation(getOperation());
        tool.setCard(getCard());
        tool.setOptAuthorize(getOptAuthorize());
        tool.setOptAuthKeys(getOptAuthKeys());
        tool.setOptNoBackFill(isOptNoBackFill());
        tool.setOptBlacklist(getOptBlacklist());
        tool.setOptExportText(getOptExportText());
        tool.setOptEmptyDirectories(isOptEmptyDirectories());
        tool.setOptListenerKeepGoing(isOptListenerKeepGoing());
        tool.setOptListenerQuit(isOptListenerQuit());
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
        return tool;
    }

    private String generateCommandLine(boolean dryRun)
    {
        // generate-commandline
        Configuration defCfg = new Configuration(context);
        boolean glo = context.preferences != null ? context.preferences.isGenerateLongOptions() : false;
        StringBuilder sb = new StringBuilder();

        String conf = (glo ? "--config \"" : "-C \"") + context.cfg.getWorkingDirectory() + "\"";
        sb.append(" " + conf);
        if (context.cfg.isLoggerView())
            sb.append(" --logger");

        if (dryRun)
            sb.append(" " + (glo ? "--dry-run" : "-D"));

        // --- non-munging actions
        if (isOptNavigator() != defCfg.isNavigator())
            sb.append(" " + (glo ? "--navigator" : "-n"));
        if (isOptForceQuit() || operation == Operations.StatusServerQuit)
            sb.append(" " + (glo ? "--force-quit" : "-Q"));
        if (isOptQuitStatus())
            sb.append(" " + (glo ? "--quit-status" : "-q"));

        // --- hint keys
        if (!getOptKeys().isEmpty())
            sb.append(" " + (glo ? "--keys" : "-k") + " \"" + getOptKeys() + "\"");
        if (!getOptKeysOnly().isEmpty())
            sb.append(" " + (glo ? "--keys-only" : "-K") + " \"" + getOptKeysOnly() + "\"");

        // --- hints & hint server
        if (!hintPath.isEmpty())
        {
            hintPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), hintPath);
            if (context.cfg.isOverrideHintsHost() ) // && operation != Operations.StatusServer)
                sb.append((" " + (glo ? "--override-hints-host" : "-J")));
            if (task.isHintsRemote() || isToolHintServer())
                sb.append(" " + (glo ? "--hint-server" : "-H") + " \"" + hintPath + "\"");
            else
                sb.append(" " + (glo ? "--hints" : "-h") + " \"" + hintPath + "\"");
        }

        // --- remote mode
        switch (operation)
        {
            case PublisherOperation:
                if (task.isSubscriberRemote())
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
            case StatusServer:
            case StatusServerQuit:
                break;
            case SubscriberListenerQuit:
                sb.append(" " + (glo ? "--listener-quit" : "-G"));
                break;
        }

        // --- libraries
        if (operation != Operations.StatusServer)
        {
            if (!pubPath.isEmpty())
            {
                pubPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), pubPath);
                sb.append(" " + (glo ? "--publisher-libraries" : "-p") + " \"" + pubPath + "\"");
            }
            if (operation != Operations.StatusServerQuit)
            {
                subPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), subPath);
                if (!subPath.isEmpty())
                {
                    if (!context.cfg.getOverrideSubscriberHost().isEmpty())
                        sb.append(" " + (glo ? "--override-host" : "-O ") + context.cfg.getOverrideSubscriberHost());
                    sb.append(" " + (glo ? "--subscriber-libraries" : "-s") + " \"" + subPath + "\"");
                }
            }
        }

        // --- targets
        switch (operation)
        {
            case PublisherOperation:
            case PublisherListener:
            case SubscriberListener:
                sb.append(" " + (glo ? "--targets" : "-t"));
                if (!getOptTargets().isEmpty())
                    sb.append(" \"" + getOptTargets() + "\"");
                break;
            case PublisherManual:
            case StatusServer:
            case StatusServerQuit:
            case SubscriberListenerQuit:
            case SubscriberTerminal:
                break;
        }

        // --- security
        if (getOptAuthorize() != null && getOptAuthorize().length > 0)
            sb.append(" " + (glo ? "--authorize" : "-a") + " \"" + getOptAuthorizeString() + "\"");
        if (!getOptAuthKeys().isEmpty())
            sb.append(" " + (glo ? "--auth-keys" : "-A") + " \"" + getOptAuthKeys() + "\"");
        if (!getOptBlacklist().isEmpty())
            sb.append(" " + (glo ? "--blacklist" : "-B") + " \"" + getOptBlacklist() + "\"");
        if (!getOptIpWhitelist().isEmpty())
            sb.append(" " + (glo ? "--ip-whitelist" : "-I") + " \"" + getOptIpWhitelist() + "\"");

        // --- exports
        if (!getOptExportText().isEmpty())
            sb.append(" " + (glo ? "--export-text" : "-e") + " \"" + getOptExportText() + "\"");
        if (!getOptExportItems().isEmpty())
            sb.append(" " + (glo ? "--export-items" : "-i") + " \"" + getOptExportItems() + "\"");

        // --- include/exclude libraries
        String[] libs = getOptLibrary();
        if (libs != null && libs.length > 0)
        {
            for (int i = 0; i < libs.length; ++i)
            {
                sb.append(" " + (glo ? "--library" : "-l") + " \"" + libs[i] + "\"");
            }
        }
        libs = getOptExclude();
        if (libs != null && libs.length > 0)
        {
            for (int i = 0; i < libs.length; ++i)
            {
                sb.append(" " + (glo ? "--exclude" : "-L") + " \"" + libs[i] + "\"");
            }
        }

        // --- differences
        if (!getOptMismatches().isEmpty())
            sb.append(" " + (glo ? "--mismatches" : "-m") + " \"" + getOptMismatches() + "\"");
        if (!getOptWhatsNew().isEmpty())
            sb.append(" " + (glo ? "--whatsnew" : "-w") + " \"" + getOptWhatsNew() + "\"");
        if (!getOptWhatsNewAll().isEmpty())
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

/*
    public String generateCommandLine(String pubPath, String subPath, boolean dryRun)
    {
        this.pubPath = pubPath;
        this.subPath = subPath;
        return generateCommandLine(dryRun);
    }
*/

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

    public Operations getOperation()
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

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    public boolean isOptCrossCheck()
    {
        return optCrossCheck;
    }

    public boolean isOptDecimalScale()
    {
        return optDecimalScale;
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
    public boolean isToolHintServer()
    {
        if (card.equals(Cards.HintServer))
            return true;
        return false;
    }

    @Override
    public boolean isToolPublisher()
    {
        if (card.equals(Cards.Publisher) || card.equals(Cards.Terminal) || card.equals(Cards.SubscriberQuit) || card.equals(Cards.StatusQuit))
            return true;
        return false;
    }

    @Override
    public boolean isToolSubscriber()
    {
        if (card.equals(Cards.Listener))
            return true;
        return false;
    }

    @Override
    public void processTool(Task task)
    {
        hintPath = task.hintsPath;
        pubPath = task.publisherPath;
        subPath = task.subscriberPath;
        this.task = task;

        // construct the arguments
        String cmd = generateCommandLine(task.dryRun);
        List<String> list = ArgumentTokenizer.tokenize(cmd);
        String[] args = list.toArray(new String[0]);

        // run the Operation
        logger.info(context.cfg.gs("Z.launching") + getConfigName());
        Main main = new Main(args, context, getConfigName());
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

    public void setOperation(Operations operation)
    {
        this.operation = operation;
    }

    public void write() throws Exception
    {
        setOptAuthKeys(context.cfg.makeRelativePath(getOptAuthKeys()));
        setOptBlacklist(context.cfg.makeRelativePath(getOptBlacklist()));
        setOptExportItems(context.cfg.makeRelativePath(getOptExportItems()));
        setOptExportText(context.cfg.makeRelativePath(getOptExportText()));
        setOptIpWhitelist(context.cfg.makeRelativePath(getOptIpWhitelist()));
        setOptKeys(context.cfg.makeRelativePath(getOptKeys()));
        setOptKeysOnly(context.cfg.makeRelativePath(getOptKeysOnly()));
        setOptMismatches(context.cfg.makeRelativePath(getOptMismatches()));
        setOptTargets(context.cfg.makeRelativePath(getOptTargets()));
        setOptWhatsNew(context.cfg.makeRelativePath(getOptWhatsNew()));
        setOptWhatsNewAll(context.cfg.makeRelativePath(getOptWhatsNewAll()));

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
