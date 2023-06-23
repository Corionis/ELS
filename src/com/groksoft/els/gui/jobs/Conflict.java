package com.groksoft.els.gui.jobs;

import com.groksoft.els.Context;
import com.groksoft.els.tools.AbstractTool;

public class Conflict
{
    public AbstractTool job;
    public int taskNumber;
    public String newName;

    public String toString(Context context)
    {
        return job.getConfigName() + context.cfg.gs(", task #") + (taskNumber + 1);
    }
}
