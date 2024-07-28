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

            // Brute-force adding paths from keys
/*
             Repositories repositories = new Repositories();
             repositories.loadList(localContext);
*/

            File[] files = FileSystemView.getFileSystemView().getFiles(jobDir, true);
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

/* Brute-force adding paths from keys
                        if (job != null)
                        {
                            boolean change = false;
                            for (Task task : job.getTasks())
                            {
                                if (task.getPublisherKey() != null && task.getPublisherKey().length() > 0)
                                {
                                    if (task.getPublisherPath() == null || task.getPublisherPath().length() == 0)
                                    {
                                        Repository r = repositories.findRepo(task.getPublisherKey());
                                        if (r != null)
                                            task.setPublisherPath(r.getJsonFilename());
                                        change = true;
                                    }
                                }

                                if (task.getSubscriberKey() != null && task.getSubscriberKey().length() > 0)
                                {
                                    if (task.getSubscriberPath() == null || task.getSubscriberPath().length() == 0)
                                    {
                                        Repository r = repositories.findRepo(task.getSubscriberKey());
                                        if (r != null)
                                            task.setSubscriberPath(r.getJsonFilename());
                                        change = true;
                                    }
                                }

                                if (task.getHintsKey() != null && task.getHintsKey().length() > 0)
                                {
                                    if (task.getHintsPath() == null || task.getHintsPath().length() == 0)
                                    {
                                        Repository r = repositories.findRepo(task.getHintsKey());
                                        if (r != null)
                                            task.setHintsPath(r.getJsonFilename());
                                        change = true;
                                    }
                                }
                            }

//                            if (change)
                            {
                                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                String j = gson.toJson(job);
                                String path = job.getDirectoryPath() + System.getProperty("file.separator") +
                                        Utils.scrubFilename(job.getConfigName()) + ".json";
                                try
                                {
                                    File f = new File((path));
                                    PrintWriter outputStream = new PrintWriter((path));
                                    outputStream.println(j);
                                    outputStream.close();
                                }
                                catch (FileNotFoundException fnf)
                                {
                                    throw new MungeException(localContext.cfg.gs("Z.error.writing") + (path) + ": " + Utils.getStackTrace(fnf));
                                }

                            }
                        }
*/

                    }
                }
            }
        }
        return jobList;
    }

}
