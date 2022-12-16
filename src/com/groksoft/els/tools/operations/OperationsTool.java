package com.groksoft.els.tools.operations;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
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
import java.util.ArrayList;

public class OperationsTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Operations";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user name for this instance
    private String internalName = INTERNAL_NAME;
    private int operation = 0;
    private String optAuthorize = ""; // -a | --authorize
    private String optAuthKeys = ""; // -A | --auth-keys
    private boolean optNoBackFill = false; // -b | --no-back-fill
    private String optBlacklist = ""; // -B | --blacklist
    private String optConsoleLevel = "Info"; // -c | --console-level
    private String optDebugLevel = "Debug"; // -d | --debug-level
    private boolean optDryRun = false; // -D --dry-run
    private String optExportText = ""; // -e | --export-text
    private boolean optEmptyDirectories = false; // -E | --empty-directories
    private String optLogFile = ""; // -f | --log-file
    private String optLogFileOverwrite = ""; // -F | --log-overwrite
    private boolean optListenerKeepGoing = false; // -g | --listener-keep-going
    private boolean optListenerQuit = false; // -G | --listener-quit
    private String optHints = ""; // -h | --hints
    private String optHintServer = ""; // -H | --hint-server
    private String optExportItems = ""; // -i | --export-items
    private String optIpWhitelist = ""; // -I | --ip-whitelist
    private String optJob = ""; // -j | --job
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
    private String optTargetsForced = ""; // -T | --force-targets
    private boolean optDuplicates = false; // -u | --duplicates
    private boolean optValidate = false; // -v | --validate
    private String optWhatsNew = ""; // -w | --whatsnew
    private String optWhatsNewAll = ""; // -W | --whatsnew-all
    private boolean optCrossCheck = false; // -x | --cross-check
    private boolean optPreserveDates = false; // -y | --preserve-dates
    private boolean optDecimalScale = false; // -z | --decimal-scale

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private GuiContext guiContext = null;
    transient private Logger logger = LogManager.getLogger("applog");
    transient boolean stop = false;
    // @formatter-on

    public OperationsTool(GuiContext guiContext, Configuration config, Context ctxt)
    {
        super(config, ctxt);
        setDisplayName(getCfg().gs("Navigator.splitPane.Operations.tab.title"));
        this.guiContext = guiContext;
    }

    public OperationsTool clone()
    {
        return null;
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

    @Override
    public ArrayList<String> getForwardPaths()
    {
        return null;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    public String getOptAuthKeys()
    {
        return optAuthKeys;
    }

    public String getOptAuthorize()
    {
        return optAuthorize;
    }

    public String getOptBlacklist()
    {
        return optBlacklist;
    }

    public String getOptConsoleLevel()
    {
        return optConsoleLevel;
    }

    public String getOptDebugLevel()
    {
        return optDebugLevel;
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

    public String getOptJob()
    {
        return optJob;
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

    public String getOptLogFile()
    {
        return optLogFile;
    }

    public String getOptLogFileOverwrite()
    {
        return optLogFileOverwrite;
    }

    public String getOptMismatches()
    {
        return optMismatches;
    }

    public String getOptTargets()
    {
        return optTargets;
    }

    public String getOptTargetsForced()
    {
        return optTargetsForced;
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

    public int getOperation()
    {
        return operation;
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

    @Override
    public void processTool(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun, Task lastTask) throws Exception
    {

    }

    @Override
    public SwingWorker<Void, Void> processToolThread(GuiContext guiContext, Repository publisherRepo, Repository subscriberRepo, ArrayList<Origin> origins, boolean dryRun)
    {
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

    public void setDataHasChanged()
    {
        dataHasChanged = true;
    }

    @Override
    public void setForwardPaths(ArrayList<String> forwardPaths)
    {
        // nop
    }

    public void setOptAuthKeys(String optAuthKeys)
    {
        this.optAuthKeys = optAuthKeys;
    }

    public void setOptAuthorize(String optAuthorize)
    {
        this.optAuthorize = optAuthorize;
    }

    public void setOptBlacklist(String optBlacklist)
    {
        this.optBlacklist = optBlacklist;
    }

    public void setOptConsoleLevel(String optConsoleLevel)
    {
        this.optConsoleLevel = optConsoleLevel;
    }

    public void setOptCrossCheck(boolean optCrossCheck)
    {
        this.optCrossCheck = optCrossCheck;
    }

    public void setOptDebugLevel(String optDebugLevel)
    {
        this.optDebugLevel = optDebugLevel;
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

    public void setOptJob(String optJob)
    {
        this.optJob = optJob;
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

    public void setOptLogFile(String optLogFile)
    {
        this.optLogFile = optLogFile;
    }

    public void setOptLogFileOverwrite(String optLogFileOverwrite)
    {
        this.optLogFileOverwrite = optLogFileOverwrite;
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

    public void setOptTargetsForced(String optTargetsForced)
    {
        this.optTargetsForced = optTargetsForced;
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

    public void setOperation(int operation)
    {
        this.operation =operation;
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
            throw new MungeException(getCfg().gs("Z.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
