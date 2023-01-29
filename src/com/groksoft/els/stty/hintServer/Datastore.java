package com.groksoft.els.stty.hintServer;

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
import java.util.ArrayList;
import java.util.List;

/**
 * ELS Hint Status Tracker/Server Datastore class
 */

public class Datastore
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Configuration cfg;
    private Context context;
    private String statusDirectory;
    private Library statusLibrary;

    /**
     * Constructor
     *
     * @param config Configuration
     * @param ctx    Context
     */
    public Datastore(Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    /**
     * Add a hint status tracker collection Item and empty file to track a hint's status
     *
     * @param itemLib       The library of the hint
     * @param itemPath      The path to the new hint file
     * @param backupName    The back-up name for the status
     * @param defaultStatus The default status for the back-up
     * @return The new Item for the hint
     * @throws Exception
     */
    private synchronized Item add(String itemLib, String itemPath, String backupName, String defaultStatus) throws Exception
    {
        Item item = new Item();
        item.setLibrary(itemLib);
        item.setItemPath(itemLib + "--" + itemPath.replaceAll(context.statusRepo.getWriteSeparator(), "--"));
        item.setFullPath(statusDirectory + context.statusRepo.getSeparator() + item.getItemPath());
        item.setSize(42);
        statusLibrary.items.add(item); // add to the in-memory collection

        File stat = new File(item.getFullPath()); // create an empty file
        stat.createNewFile();
        logger.info("  + Added hint status file: " + item.getFullPath());
        return item;
    }

    /**
     * Find a back-up name in a hint status file
     *
     * @param lines      The lines of the hint status file
     * @param backupName The back-up name to find
     * @return String line found, or null
     */
    private String findBackup(List<String> lines, String backupName)
    {
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            if (line.toLowerCase().startsWith(backupName.toLowerCase() + " "))
            {
                return line;
            }
        }
        return null;
    }

    /**
     * Find a hint Item in the hint status tracker collection
     * <p>
     * If not found a new Item is added.
     *
     * @param itemLib       The library of the hint
     * @param itemPath      The path to the new hint file
     * @param backupName    The back-up name for the status
     * @param defaultStatus The default status for the back-up
     * @return The Item of the hint
     * @throws Exception
     */
    private Item findItem(String itemLib, String itemPath, String backupName, String defaultStatus) throws Exception
    {
        String path = itemLib + "--" + itemPath;
        Item item = statusLibrary.get(path);
        if (item == null)
        {
            item = add(itemLib, itemPath, backupName, defaultStatus);
        }
        return item;
    }

    /**
     * Command "get" to return the status of a back-up
     *
     * @param itemLib       The library of the hint
     * @param itemPath      The item path of the hint
     * @param backupName    The back-up name to find
     * @param defaultStatus The default status if not found
     * @return The current status of the hint
     * @throws Exception
     */
    public synchronized String getStatus(String itemLib, String itemPath, String backupName, String defaultStatus) throws Exception
    {
        String status = "";

        itemPath = itemPath.replaceAll("/", "--").replaceAll("\\\\", "--");
        Item item = findItem(itemLib, itemPath, backupName, defaultStatus);
        List<String> lines = Files.readAllLines(Paths.get(item.getFullPath()));
        String line = findBackup(lines, backupName);
        if (line == null)
        {
            status = defaultStatus;
            updateDatastore(item, lines, backupName, status);
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

    /**
     * Initialize the hint tracker.
     * <p>
     * Uses the first source from the first library defined in the
     * hint tracker/server JSON file as the datastore directory for
     * tracking hint status.
     *
     * @throws MungeException
     */
    public void initialize() throws MungeException
    {
        if (context.statusRepo.getLibraryData() != null &&
                context.statusRepo.getLibraryData().libraries != null &&
                context.statusRepo.getLibraryData().libraries.bibliography != null &&
                context.statusRepo.getLibraryData().libraries.bibliography.length > 0)
        {
            statusLibrary = context.statusRepo.getLibraryData().libraries.bibliography[0];
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

        File dir = new File(statusDirectory);
        if (dir.exists())
        {
            if (!dir.isDirectory())
                throw new MungeException("Status directory is not a directory: " + statusDirectory);
            logger.info("Using library \'" + statusLibrary.name + "\" source directory \"" + statusDirectory + "\" for status datastore");
        }
        else
        {
            logger.info("Creating new library \'" + statusLibrary.name + "\" source directory \"" + statusDirectory + "\" for status datastore");
            dir.mkdirs();
        }

        // scan the status datastore (repository)
        context.statusRepo.scan(statusLibrary.name);
    }

    /**
     * Command "set" to change the status of a back-up
     *
     * @param itemLib    The library of the hint
     * @param itemPath   The item path of the hint
     * @param backupName The back-up name to find
     * @param status     The status to use
     * @return The updated status of the hint; should be the same as that passed
     * @throws Exception
     */
    public synchronized String setStatus(String itemLib, String itemPath, String backupName, String status) throws Exception
    {
        String path = itemLib + "--" + itemPath;
        path = path.replaceAll("/", "--").replaceAll("\\\\", "--");
        Item item = statusLibrary.get(path);
        if (item == null)
        {
            item = add(itemLib, itemPath, backupName, status);
        }

        List<String> lines = new ArrayList<String>();
        lines = Files.readAllLines(Paths.get(item.getFullPath()));
        lines = updateDatastore(item, lines, backupName, status);
        return status;
    }

    /**
     * Update the datastore.
     * <p>
     * If the hint has been Deleted by all participants then the
     * hint status tracker/server file is deleted for automatic
     * hint file maintenance.
     *
     * @param item       The Item to be updated
     * @param lines      The lines of the item
     * @param backupName The back-up name to be updated
     * @param status     The status value to be used
     * @return The updates lines of the hint status tracker/server
     * @throws Exception
     */
    private synchronized List<String> updateDatastore(Item item, List<String> lines, String backupName, String status) throws Exception
    {
        int count = 0;
        int deleted = 0;
        boolean found = false;
        for (int i = 0; i < lines.size(); ++i)
        {
            String update = "";
            String line = lines.get(i).trim().toLowerCase();
            if (line.length() == 0)
                continue;
            ++count;
            if (line.startsWith(backupName.toLowerCase() + " "))
            {
                update = backupName + " " + status;
                lines.set(i, update);
                line = update.toLowerCase();
                found = true;
            }
            if (line.endsWith(" deleted"))
                ++deleted;
        }
        if (!found)
        {
            lines.add(backupName + " " + status);
            ++count;
            if (status.trim().equalsIgnoreCase("deleted"))
                ++deleted;
        }
        if (deleted == count)
        {
            Files.delete(Paths.get(item.getFullPath()));
            logger.info("  $ Hint finished by all participants, deleted hint status file: " + item.getFullPath());
        }
        else
            Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
        return lines;
    }

}
