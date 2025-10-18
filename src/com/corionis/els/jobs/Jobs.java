package com.corionis.els.jobs;

import com.corionis.els.Utils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.corionis.els.Context;
import com.corionis.els.tools.AbstractTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")
public class Jobs
{
    private Context context;
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

    public void saveAllJobs(ArrayList<AbstractTool> jobList) throws Exception
    {
        boolean changed = false;
        Job job = null;
        if (jobList != null)
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < jobList.size(); ++i)
            {
                job = (Job) jobList.get(i);
                if (job.isDataChanged())
                {
                    String status = job.validate(context.cfg, false);
                    if (status.length() > 0)
                    {
                        JOptionPane.showMessageDialog(context.mainFrame, status, context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                    }

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    String json = gson.toJson(job);
                    String path = job.getDirectoryPath() + System.getProperty("file.separator") +
                            Utils.scrubFilename(job.getConfigName()) + ".json";

                    File f = new File(path);
                    if (f != null)
                    {
                        f.getParentFile().mkdirs();
                    }
                    PrintWriter outputStream = new PrintWriter(path);
                    outputStream.println(json);
                    outputStream.close();

                    changed = true;
                    job.setDataHasChanged(false);
                }
            }

            if (changed)
            {
                context.navigator.loadJobsMenu();
                context.libraries.loadJobs();
            }
        }
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

            // Brute-force adding paths from keys
/*
             Repositories repositories = new Repositories();
             repositories.loadList(localContext);
*/

            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(jobDir.toPath());
            for (Path entry : directoryStream)
            {
                boolean isDir = Files.isDirectory(entry);
                if (!isDir)
                {
                    String json = new String(Files.readAllBytes(Paths.get(entry.toString())));
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
