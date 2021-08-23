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
import java.nio.charset.StandardCharsets;
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
        statLibrary.items.add(item);

        File stat = new File(item.getFullPath());
        byte[] buff = (systemName + " " + defaultStatus + "\r\n").getBytes(StandardCharsets.UTF_8);
        Files.write(stat.toPath(), buff, StandardOpenOption.CREATE);
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
        String path = itemLib + "--" + itemPath;
        Item item = statLibrary.get(path);
        if (item == null)
        {
            item = add(itemLib, itemPath, systemName, defaultStatus);
            status = defaultStatus;
        }
        else
        {
            List<String> lines = Files.readAllLines(Paths.get(item.getFullPath()));
            String line = findSystem(lines, systemName);
            if (line == null)
            {
                status = defaultStatus;
            }
            else
            {
                String[] parts = line.split("[\\s]+");
                if (parts.length == 2)
                {
                    status = parts[1];
                }
                else
                    throw new MungeException("Malformed status line in: " + item.getFullPath());
            }
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
            throw new MungeException("Hint Status Server repo contains no library for status");

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
            logger.info("Using library \'" + statLibrary.name + "\" source directory \"" + statDirectory + "\" for status store");
        }
        else
        {
            logger.info("Creating new library \'" + statLibrary.name + "\" source directory \"" + statDirectory + "\" for status store");
            dir.mkdirs();
        }

        // scan the status store (repository)
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
        else
        {
            List<String> lines = Files.readAllLines(Paths.get(item.getFullPath()));
            lines = updateSystem(lines, systemName, status);
            Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
        }
        return status;
    }

    private List<String> updateSystem(List<String> lines, String systemName, String status)
    {
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            if (line.toLowerCase().startsWith(systemName.toLowerCase() + " "))
            {
                String[] parts = line.split("[\\s]+");
                line = parts[0] + " " + status;
                lines.set(i, line);
                break;
            }
        }
        return lines;
    }

}
