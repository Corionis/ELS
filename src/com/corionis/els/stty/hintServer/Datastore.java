package com.corionis.els.stty.hintServer;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintStatus;
import com.corionis.els.repository.Item;
import com.corionis.els.repository.Library;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.swing.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * ELS Hint Status Tracker/Server Datastore class
 */

public class Datastore
{
    public List<Hint> hints;

    private transient Context context;
    private transient Library statusLibrary;
    private transient String statusDirectory;
    private transient String statusFullPath;

    private final transient String DATASTORE_NAME = "hint-datastore.json";
    private final transient Logger logger = LogManager.getLogger("applog");
    private final transient Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private final transient Marker SHORT = MarkerManager.getMarker("SHORT");

    /**
     * Constructor
     *
     * @param context Context
     */
    public Datastore(Context context)
    {
        this.context = context;
    }

    public synchronized void add(Hint hint)
    {
        if (this.hints == null)
            this.hints = new ArrayList<Hint>();

        this.hints.add(hint);
    }

    public synchronized int count(String system)
    {
        int count = 0;
        if (hints != null && hints.size() > 0)
        {
            for (int i = 0; i < hints.size(); ++i)
            {
                HintStatus hs = hints.get(i).findStatus(system);
                if (hs != null && !hs.status.toLowerCase().equals("done"))
                    ++count;
                else if (hs == null)
                    ++count;
            }
        }
        return count;
    }

    public synchronized Hint get(Hint hint, String mode)
    {
        ArrayList<Hint> list = getAll(hint, mode);
        if (list != null && list.size() == 1)
            return list.get(0);

        if (list.size() > 1)
            logger.warn("!!! Datastore get singleton Hint returned " + list.size() + " Hints");

        return null;
    }

    public synchronized ArrayList<Hint> getAll(Hint hint, String mode)
    {
        ArrayList<Hint> results = new ArrayList<Hint>();
        if (hints != null && hints.size() > 0)
        {
            boolean all = mode.toLowerCase().equals("all") ? true : false;
            boolean exact = mode.toLowerCase().equals("exact") ? true : false;
            boolean full = mode.toLowerCase().equals("full") ? true : false;
            boolean conflict = mode.toLowerCase().equals("conflict") ? true : false;

            if (all)
            {
                if (hints != null)
                {
                    results.addAll(hints);
                }
                return results;
            }

            if (exact || full || conflict)
            {
                for (int i = 0; i < hints.size(); ++i)
                {
                    Hint entry = hints.get(i);
                    if (conflict)
                    {
                        if (hint.fromLibrary != null && hint.fromLibrary.length() > 0 && entry.fromLibrary.compareTo(hint.fromLibrary) != 0 ? false : true)
                        {
                            if (hint.fromItemPath != null && hint.fromItemPath.length() > 0 && entry.fromItemPath.compareTo(hint.fromItemPath) != 0 ? false : true)
                            {
                                results.add(entry);
                            }
                        }
                    }
                    else
                    {
                        if (exact && (hint.utc != 0 && entry.utc != hint.utc) ? false : true)
                        {
                            if (exact && (hint.author != null && hint.author.length() > 0 && entry.author.compareTo(hint.author) != 0) ? false : true)
                            {
                                if (hint.system != null && hint.system.length() > 0 && entry.system.compareTo(hint.system) != 0 ? false : true)
                                {
                                    if (hint.action != null && hint.action.length() > 0 && entry.action.compareTo(hint.action) != 0 ? false : true)
                                    {
                                        if (hint.fromLibrary != null && hint.fromLibrary.length() > 0 && entry.fromLibrary.compareTo(hint.fromLibrary) != 0 ? false : true)
                                        {
                                            if (hint.fromItemPath != null && hint.fromItemPath.length() > 0 && entry.fromItemPath.compareTo(hint.fromItemPath) != 0 ? false : true)
                                            {
                                                if (hint.toLibrary != null && hint.toLibrary.length() > 0 && entry.toLibrary.compareTo(hint.toLibrary) != 0 ? false : true)
                                                {
                                                    if (hint.toItemPath != null && hint.toItemPath.length() > 0 && entry.toItemPath.compareTo(hint.toItemPath) != 0 ? false : true)
                                                    {
                                                        results.add(entry);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    public ArrayList<Hint> getFor(String hintSystemName)
    {
        ArrayList<Hint> results = null;
        if (hints != null && hints.size() > 0)
        {
            for (int i = 0; i < hints.size(); ++i)
            {
                Hint hint = hints.get(i);
                if (hint.isFor(hintSystemName) >= -1)
                {
                    if (results == null)
                        results = new ArrayList<Hint>();
                    results.add(hint);
                }
            }
        }
        return results;
    }

    /**
     * Initialize the hint tracker.
     * <p>
     * Uses the first source from the first library defined in the
     * hint tracker/server JSON file as the datastore directory for
     * tracking hint status.
     *
     * @throws MungeException
     */
    public boolean initialize() throws Exception
    {
        if (context.hintsRepo.getLibraryData() != null &&
                context.hintsRepo.getLibraryData().libraries != null &&
                context.hintsRepo.getLibraryData().libraries.bibliography != null &&
                context.hintsRepo.getLibraryData().libraries.bibliography.length > 0)
        {
            statusLibrary = context.hintsRepo.getLibraryData().libraries.bibliography[0];
        }
        else
            throw new MungeException("Hint Status Tracker/Server repo contains no library for status datastore");

        statusDirectory = "";
        if (statusLibrary.sources != null && statusLibrary.sources.length > 0)
        {
            statusDirectory = statusLibrary.sources[0];
        }
        else
            throw new MungeException("Hint Status Tracker/Server repo first library contains no sources: " + statusLibrary.name);

        File dir = new File(Utils.getFullPathLocal(statusDirectory));
        if (dir.exists())
        {
            if (!dir.isDirectory())
                throw new MungeException("Status directory is not a directory: " + statusDirectory);
            logger.info("Using library '" + statusLibrary.name + "\" source directory \"" + dir + "' for Hint tracking datastore");
        }
        else
        {
            logger.info("Creating new library '" + statusLibrary.name + "\" source directory \"" + dir + "' for Hint tracking datastore");
            dir.mkdirs();
        }

        // scan the status repository
        context.hintsRepo.scan(statusLibrary.name);

        int count = 0;
        if (statusLibrary.items != null)
            count = statusLibrary.items.size();

        if (count == 1)
        {
            Item item = statusLibrary.items.get(0);
            statusFullPath = item.getFullPath();
            if (!read())
                return false;
        }
        else
        {
            if (count != 0)
                throw new MungeException("Multiple status files are not supported in " + statusDirectory);

            statusFullPath = statusLibrary.sources[0] + System.getProperty("file.separator") + DATASTORE_NAME;
        }
        return true;
    }

    private synchronized void normalize()
    {
        sort("utc");
    }

    public boolean read() throws Exception
    {
        boolean valid = false;
        try
        {
            String json;
            String path = statusFullPath;
            if (Utils.isRelativePath(path))
            {
                path = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + path;
            }

            Gson gson = new Gson();
            logger.info("Reading Hint datastore " + path);
            if (Utils.isRelativePath(path))
            {
                path = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + path;
            }

            File ds = new File(path);
            if (ds.exists())
                json = new String(Files.readAllBytes(Paths.get(path)));
            else
                json = "[]";

            Type listType = new TypeToken<ArrayList<Hint>>()
            {
            }.getType();
            hints = gson.fromJson(json, listType);
            if (hints != null)
            {
                normalize();
                logger.info("Read \"" + path + "\" successfully, " + hints.size() + " Hints");
                valid = true;
            }
            else
            {
                logger.info("Read \"" + path + "\", content is empty");
            }
        }
        catch (IOException ioe)
        {
            String msg = "Exception while reading Hint datastore: " + ioe.toString();
            if (context.main.isStartupActive())
            {
                logger.error(msg);
                int opt = JOptionPane.showConfirmDialog(context.main.guiLogAppender.getStartup(),
                        "<html><body>" + msg + "<br/><br/>Continue?</body></html>",
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    context.fault = false;
                    return false;
                }
            }
            throw new MungeException(msg);
        }
        return valid;
    }

    public boolean reload() throws Exception
    {
        if (!read())
            return false;
        return true;
    }

    public void sort(String element)
    {
        switch (element.toLowerCase())
        {
            case "utc":
                Collections.sort(hints, sortByUtc());
                break;
        }
    }

    public static synchronized Comparator<Hint> sortByUtc()
    {
        Comparator comp = new Comparator<Hint>()
        {
            @Override
            public int compare(Hint hint1, Hint hint2)
            {
                return (hint1.utc < hint2.utc ? -1 : (hint1.utc > hint2.utc ? 1 : 0));
            }
        };
        return comp;
    }

    public void write() throws Exception
    {
        String path = Utils.getFullPathLocal(statusFullPath);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(hints);
        try
        {
            PrintWriter outputStream = new PrintWriter(path);
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(context.cfg.gs("Z.error.writing") + path + ": " + Utils.getStackTrace(fnf));
        }
    }

}
