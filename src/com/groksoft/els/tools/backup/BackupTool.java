package com.groksoft.els.tools.backup;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.ArrayList;

public class BackupTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Backup";
    public static final String SUBSYSTEM = "tools";

    private String configName; // user name for this instance
    private String internalName = INTERNAL_NAME;

    transient private boolean dataHasChanged = false; // used by GUI, dynamic
    transient private GuiContext guiContext = null;
    transient private boolean isDryRun = false;
    transient private Logger logger = LogManager.getLogger("applog");
    transient boolean stop = false;
    // @formatter:on

    public BackupTool(GuiContext guiContext, Configuration config, Context ctxt)
    {
        super(config, ctxt);
        setDisplayName(getCfg().gs("Navigator.splitPane.Browser.tab.title"));
        this.guiContext = guiContext;
    }

    public BackupTool clone()
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

}
