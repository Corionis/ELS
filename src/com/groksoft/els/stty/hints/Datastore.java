package com.groksoft.els.stty.hints;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * ELS Hints Datastore class
 */

public class Datastore
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Configuration cfg;
    private Context context;
    private String statDirectory;
    private Library statLibrary;

    public Datastore(Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    private Item add(String itemLib, String itemPath, String systemName, String defaultStatus) throws Exception
    {
        Item item = new Item();
        item.setLibrary(itemLib);
        item.setItemPath(itemLib + "--" + itemPath);
        item.setFullPath(statDirectory + context.statusRepo.getSeparator() + item.getItemPath());
        item.setSize(42);
        statLibrary.items.add(item); // add to the in-memory collection

        File stat = new File(item.getFullPath()); // create an empty file
        stat.createNewFile();
        return item;
    }

    private Item findItem(String itemLib, String itemPath, String systemName, String defaultStatus) throws Exception
    {
        String path = itemLib + "--" + itemPath;
        Item item = statLibrary.get(path);
        if (item == null)
        {
            item = add(itemLib, itemPath, systemName, defaultStatus);
        }
        return item;
    }

    private String findSystem(List<String> lines, String systemName)
    {
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            if (line.toLowerCase().startsWith(systemName.toLowerCase() + " "))
            {
                return line;
            }
        }
        return null;
    }

    public synchronized String getStatus(String itemLib, String itemPath, String systemName, String defaultStatus) throws Exception
    {
        String status = "";

        Item item = findItem(itemLib, itemPath, systemName, defaultStatus);
        List<String> lines = Files.readAllLines(Paths.get(item.getFullPath()));
        String line = findSystem(lines, systemName);
        if (line == null)
        {
            status = defaultStatus;
            updateDatastore(item, lines, systemName, status);
        }
        else
        {
            String[] parts = line.split("[\\s]+");
            if (parts.length == 2)
            {
                status = parts[1];
            }
            else
                throw new MungeException("Malformed datastore status line in: " + item.getFullPath());
        }
        return status;
    }

    public void initialize() throws MungeException
    {
        if (context.statusRepo.getLibraryData() != null &&
                context.statusRepo.getLibraryData().libraries != null &&
                context.statusRepo.getLibraryData().libraries.bibliography != null &&
                context.statusRepo.getLibraryData().libraries.bibliography.length > 0)
        {
            statLibrary = context.statusRepo.getLibraryData().libraries.bibliography[0];
        }
        else
            throw new MungeException("Hint Status Server repo contains no library for status datastore");

        statDirectory = "";
        if (statLibrary.sources != null &&
                statLibrary.sources.length > 0)
        {
            statDirectory = statLibrary.sources[0];
        }
        else
            throw new MungeException("Hint Status Server repo first library contains no sources: " + statLibrary.name);

        File dir = new File(statDirectory);
        if (dir.exists())
        {
            if (!dir.isDirectory())
                throw new MungeException("Status directory is not a directory: " + statDirectory);
            logger.info("Using library \'" + statLibrary.name + "\" source directory \"" + statDirectory + "\" for status datastore");
        }
        else
        {
            logger.info("Creating new library \'" + statLibrary.name + "\" source directory \"" + statDirectory + "\" for status datastore");
            dir.mkdirs();
        }

        // scan the status datastore (repository)
        context.statusRepo.scan(statLibrary.name);
    }

    public synchronized String setStatus(String itemLib, String itemPath, String systemName, String status) throws Exception
    {
        String path = itemLib + "--" + itemPath;
        Item item = statLibrary.get(path);
        if (item == null) // if a get() was done first this shouldn't happen
        {
            item = add(itemLib, itemPath, systemName, status);
        }

        List<String> lines = Files.readAllLines(Paths.get(item.getFullPath()));
        lines = updateDatastore(item, lines, systemName, status);
        return status;
    }

    private List<String> updateDatastore(Item item, List<String> lines, String systemName, String status) throws Exception
    {
        boolean found = false;
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            if (line.toLowerCase().startsWith(systemName.toLowerCase() + " "))
            {
                String[] parts = line.split("[\\s]+");
                line = parts[0] + " " + status;
                lines.set(i, line);
                found = true;
                break;
            }
        }
        if (!found)
        {
            lines.add(systemName + " " + status);
        }
        Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
        return lines;
    }

}
