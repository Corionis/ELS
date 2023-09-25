package com.corionis.els.jobs;

import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.corionis.els.Context;
import com.corionis.els.gui.jobs.Conflict;
import com.corionis.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")
public class Jobs
{
    private Context context;
    ArrayList<Conflict> conflicts = null;
    private Logger logger = LogManager.getLogger("applog");

    private Jobs()
    {
        // hide default constructor
    }

    public Jobs(Context context)
    {
        this.context = context;
    }

    public ArrayList<AbstractTool> loadAllJobs() throws Exception
    {
        ArrayList<AbstractTool> jobList = new ArrayList<>();
        jobList = scanJobs(jobList);
        Collections.sort(jobList);
        return jobList;
    }

    private ArrayList<AbstractTool> scanJobs(ArrayList<AbstractTool> jobList) throws Exception
    {
        Job tmpJob = new Job(context, "temp");
        String dir = tmpJob.getDirectoryPath();
        File jobDir = new File(dir);
        if (jobDir.exists() && jobDir.isDirectory())
        {
            class objInstanceCreator implements InstanceCreator
            {
                @Override
                public Object createInstance(Type type)
                {
                    return new Job(context, "");
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
