package com.corionis.els.gui.jobs;

import com.corionis.els.Context;
import com.corionis.els.tools.AbstractTool;

public class Conflict
{
    public AbstractTool job;
    public int taskNumber;
    public String oldName;
    public String newName;

    public String toString(Context context)
    {
        return job.getConfigName() + context.cfg.gs(", Task #") + (taskNumber + 1);
    }
}
