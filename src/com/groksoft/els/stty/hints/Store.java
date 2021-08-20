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
import java.nio.file.StandardOpenOption;

public class Store
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Configuration cfg;
    private Context context;
    String statDirectory;
    private Library statLib;

    public Store(Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    private void add(String itemLib, String itemPath, String systemName, String defaultStatus) throws Exception
    {
        Item item = new Item();
        item.setLibrary(itemLib);
        item.setItemPath(itemLib + "--" + itemPath);
        item.setFullPath(statDirectory + context.statusRepo.getSeparator() + item.getItemPath());
        item.setSize(42);
        statLib.items.add(item);

        File stat = new File(item.getFullPath());
        byte[] buff = (systemName + " " + defaultStatus + "\r\n").getBytes(StandardCharsets.UTF_8);
        Files.write(stat.toPath(), buff, StandardOpenOption.CREATE);
    }

    public synchronized String getStatus(String itemLib, String itemPath, String systemName, String defaultStatus) throws Exception
    {
        String status = "";
        String path = itemLib + "--" + itemPath;
        Item item = statLib.get(path);
        if (item == null)
        {
            add(itemLib, itemPath, systemName, defaultStatus);
            status = defaultStatus;
        }
        else
        {

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
            statLib = context.statusRepo.getLibraryData().libraries.bibliography[0];
        }
        else
            throw new MungeException("Hint Status Server repo contains no library for status");

        statDirectory = "";
        if (statLib.sources != null &&
            statLib.sources.length > 0)
        {
            statDirectory = statLib.sources[0];
        }
        else
            throw new MungeException("Hint Status Server repo first library contains no sources: " + statLib.name);

        File dir = new File(statDirectory);
        if (dir.exists())
        {
            if (!dir.isDirectory())
                throw new MungeException("Status directory is not a directory: " + statDirectory);
            logger.info("Using library \'" + statLib.name + "\" source directory \"" + statDirectory + "\" for status store");
        }
        else
        {
            logger.info("Creating new library \'" + statLib.name + "\" source directory \"" + statDirectory + "\" for status store");
            dir.mkdirs();
        }

        // scan the status store (repository)
        context.statusRepo.scan(statLib.name);
    }

    public synchronized void setStatus(Item item, String systemName, String status)
    {
    }

}
