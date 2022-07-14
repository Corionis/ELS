package com.groksoft.els.jobs;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.tools.AbstractTool;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

public class Jobs
{
    private GuiContext guiContext;

    private Jobs()
    {
        // hide default constructor
    }

    public Jobs(GuiContext guiContext)
    {
        this.guiContext = guiContext;
    }

    public ArrayList<AbstractTool> loadAllJobs() throws Exception
    {
        ArrayList<AbstractTool> jobList = new ArrayList<>();
        jobList = scanTools(jobList);
        Collections.sort(jobList);
        return jobList;
    }

    private ArrayList<AbstractTool> scanTools(ArrayList<AbstractTool> jobList) throws Exception
    {
        Job tmpJob = new Job(guiContext.cfg, guiContext.context, "temp");
        String dir = tmpJob.getDirectoryPath();
        File jobDir = new File(dir);
        if (jobDir.exists() && jobDir.isDirectory())
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new Job(guiContext.cfg, guiContext.context, "");
                }
            }
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(Job.class, new objInstanceCreator());

            File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                    if (json != null)
                    {
                        Job job = builder.create().fromJson(json, Job.class);
                        if (job != null)
                            jobList.add(job);
                    }
                }
            }
        }
        return jobList;
    }

}
