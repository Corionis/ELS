package com.groksoft.els;

import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Location;
import com.groksoft.els.storage.Storage;
import com.groksoft.els.storage.Target;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;

/**
 * ELS Process
 */
public class Process
{
    private Configuration cfg = null;
    private Main.Context context;
    private int copyCount = 0;
    private String currentGroupName = "";
    private int differentSizes = 0;
    private int errorCount = 0;
    private boolean fault = false;
    private long grandTotalItems = 0L;
    private long grandTotalOriginalLocation = 0L;
    private long grandTotalSize = 0L;
    private ArrayList<String> ignoredList = new ArrayList<>();
    private boolean isInitialized = false;
    private boolean justScannedPublisher = false;
    private String lastGroupName = "";
    private transient Logger logger = LogManager.getLogger("applog");
    private Storage storageTargets = null;
    private long totalDirectories = 0;
    private long totalItems = 0;
    private long whatsNewTotal = 0;

    /**
     * Hide default constructor
     */
    private Process()
    {
        // hide default constructor
    }

    /**
     * Instantiates the class
     */
    public Process(Configuration config, Main.Context ctxt)
    {
        this.cfg = config;
        this.context = ctxt;
    }

    /**
     * Copy a file, local or remote
     *
     * @param from the full from path
     * @param to   the full to path
     * @return success boolean
     */
    private void copyFile(String from, String to, boolean overwrite) throws Exception
    {
        if (cfg.isRemoteSession())
        {
            context.clientSftp.transmitFile(from, to, overwrite);
        }
        else
        {
            Path fromPath = Paths.get(from).toRealPath();
            Path toPath = Paths.get(to);
            File f = new File(to);
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            Files.copy(fromPath, toPath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        }
    }

    /**
     * Copy group of files
     * <p>
     * The overwrite parameter is false for normal Process munge operations, and
     * true for Subscriber terminal (-r T) to Publisher listener (-r L) operations.
     *
     * @param group     the group
     * @param totalSize the total size
     * @param overwrite whether to overwrite any existing target file
     * @throws MungerException the els exception
     */
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws Exception
    {
        String response = "";
        if (!cfg.isTargetsEnabled())
        {
            throw new MungerException("-t or -T target is required for this operation");
        }
        if (!isInitialized)
        {
            initialize();
            if (!isInitialized)
                throw new MungerException("initialize() failed");
        }
        try
        {
            if (group.size() > 0)
            {
                for (Item groupItem : group)
                {
                    if (cfg.isDryRun())
                    {
                        // -D Dry run option
                        ++copyCount;
                        logger.info("  > Would copy #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) + ", " + groupItem.getFullPath());
                    }
                    else
                    {
                        String targetPath = getTarget(groupItem.getLibrary(), totalSize, groupItem);
                        if (targetPath != null)
                        {
                            // copy item(s) to targetPath
                            ++copyCount;

                            String to = targetPath + context.subscriberRepo.getWriteSeparator();
                            to += context.publisherRepo.normalize(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                            String msg = "  > Copying #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) + ", " + groupItem.getFullPath() + " to " + to;
                            logger.info(msg);
                            response += (msg + "\r\n");

                            copyFile(groupItem.getFullPath(), to, overwrite);
                        }
                        else
                        {
                            throw new MungerException("No space on any target location of " + group.get(0).getLibrary() + " for " +
                                    lastGroupName + " that is " + Utils.formatLong(totalSize, false));
                        }
                    }
                }
            }
            grandTotalItems = grandTotalItems + group.size();
            group.clear();
            grandTotalSize = grandTotalSize + totalSize;
            totalSize = 0L;
            lastGroupName = currentGroupName;
        }
        catch (Exception e)
        {
            fault = true;
            throw e;
        }

        return response;
    }

    /**
     * Check publisher collection data for duplicates
     */
    private void duplicatesCheck() throws Exception
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");

        // scan the collection if library file specified
        if (cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }

        totalDirectories = 0;
        totalItems = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            logger.info("Analyzing library '" + pubLib.name + "' for duplicates" + (cfg.isRenaming() ? " and performing any substitution renames" : ""));
            for (Item item : pubLib.items)
            {
                if (item.isDirectory())
                    ++totalDirectories;
                else
                    ++totalItems;

                // populate the item.hasList
                context.publisherRepo.hasPublisherDuplicate(item, Utils.pipe(context.publisherRepo, item.getItemPath()));
            }
        }

        int duplicates = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            for (Item item : pubLib.items)
            {
                if (item.getHas().size() > 0)
                {
                    duplicates = reportDuplicates("Publisher", item, duplicates);
                }
            }
        }

        int empties = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            for (Item item : pubLib.items)
            {
                if (item.isDirectory() && item.getSize() == 0)
                {
                    empties = reportEmpties("Publisher", item, empties);
                }
            }
        }

        logger.info(SIMPLE, "# Total files: " + totalItems);
        logger.info(SIMPLE, "# Total directories: " + totalDirectories);
        logger.info(SIMPLE, "# Total items: " + (totalItems + totalDirectories));
        logger.info(SIMPLE, "# Total duplicates: " + duplicates);
        logger.info(SIMPLE, "# Total empty directories: " + empties);
    }

    /**
     * Export publisher collection data to configured file as JSON
     */
    private void exportCollection() throws Exception
    {
        // scan the collection if library file specified
        if (cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }
        context.publisherRepo.exportItems();
    }

    /**
     * Export publisher collection data to configured file as plain text
     */
    private void exportText() throws Exception
    {
        // scan the collection if library file specified
        if (cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }
        context.publisherRepo.exportText();
    }

    public long getLocationMinimum(String path)
    {
        long minimum = 0L;
        for (Location loc : context.subscriberRepo.getLibraryData().libraries.locations)
        {
            if (path.startsWith(loc.location))
            {
                minimum = Utils.getScaledValue(loc.minimum);
                break;
            }
        }
        if (minimum < 1L)
        {
            minimum = Storage.MINIMUM_BYTES;
        }
        return minimum;
    }

    public void getStorageTargets() throws Exception
    {
        String location = cfg.getTargetsFilename();

        if (cfg.isRemoteSession() && cfg.isRequestTargets())
        {
            if (location == null || location.length() < 1)
            {
                location = "subscriber-targets";
            }
            // request target data from remote subscriber
            location = context.clientStty.retrieveRemoteData(location, "targets");
            cfg.setTargetsFilename(location);
        }

        if (location != null) // v3.00 allow targets to be empty to use sources as target locations
        {
            if (storageTargets == null)
                storageTargets = new Storage();

            storageTargets.read(location, context.subscriberRepo.getLibraryData().libraries.flavor);
            if (!cfg.isRemoteSession())
                storageTargets.validate();
        }
    }

    /**
     * Gets a subscriber target
     * <p>
     * Will return the original directory where existing files are located if one
     * exists and that location has enough space.
     * <p>
     * Otherwise will return one of the subscriber targets for the library of the item
     * that has enough space to hold the item, otherwise an empty string is returned.
     *
     * @param item    the item
     * @param library the publisher library.definition.name
     * @param size    the total size of item(s) to be copied
     * @return the target
     * @throws MungerException the els exception
     */
    public String getTarget(String library, long size, Item item) throws Exception
    {
        String path = null;
        boolean notFound = true;
        long minimum = 0L;
        Target target = null;

        if (storageTargets != null)
        {
            target = storageTargets.getLibraryTarget(library);
        }
        if (target != null)
        {
            minimum = Utils.getScaledValue(target.minimum);
        }
        else
        {
            minimum = Storage.MINIMUM_BYTES;
        }

        // see if there is an "original" directory the new content will fit in
        if (!cfg.isNoBackFill())
        {
            path = context.subscriberRepo.hasDirectory(library, Utils.pipe(context.publisherRepo, item.getItemPath()));
            if (path != null)
            {
                // check size of item(s) to be copied
                if (itFits(path, size, minimum, target != null))
                {
                    logger.info("Using original storage location for " + item.getItemPath() + " at " + path);
                    //
                    // inline return
                    //
                    ++grandTotalOriginalLocation;
                    return path;
                }
                logger.info("Original storage location too full for " + item.getItemPath() + " " + Utils.formatLong(size, false) + " at " + path);
                path = null;
            }
        }

        if (target != null) // a defined target is the default
        {
            notFound = false;
            for (int j = 0; j < target.locations.length; ++j)
            {
                String candidate = target.locations[j];
                // check size of item(s) to be copied
                if (itFits(candidate, size, minimum, true))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        else // v3.00, use sources for target locations
        {
            Library lib = context.subscriberRepo.getLibrary(library);
            if (lib != null)
            {
                notFound = false;
            }
            for (int j = 0; j < lib.sources.length; ++j)
            {
                String candidate = lib.sources[j];
                // check size of item(s) to be copied
                if (itFits(candidate, size, minimum, false))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        if (notFound)
        {
            logger.error("No target library match found for library " + library);
        }
        return path;
    }

    /**
     * Initialize the configured data structures
     */
    private void initialize()
    {
        try
        {
            isInitialized = true;

            // For -r P connect to remote subscriber -r S
            if (cfg.isRemotePublish() || cfg.isPublisherListener())
            {
                // sanity checks
                if (context.publisherRepo.getLibraryData().libraries.flavor == null ||
                        context.publisherRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungerException("Publisher data incomplete, missing 'flavor'");
                }

                if (context.subscriberRepo.getLibraryData().libraries.flavor == null ||
                        context.subscriberRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungerException("Subscriber data incomplete, missing 'flavor'");
                }

                // check for opening commands from Subscriber
                // *** might change cfg options for subscriber and targets that are handled below ***
                if (context.clientStty.checkBannerCommands())
                {
                    logger.info("Received subscriber commands:" + (cfg.isRequestCollection() ? " RequestCollection " : "") + (cfg.isRequestTargets() ? "RequestTargets" : ""));
                }
            }

            // get -s Subscriber libraries
            if (cfg.getSubscriberLibrariesFileName().length() > 0)
            {
                if (cfg.isRemoteSession() && cfg.isRequestCollection())
                {
                    // request complete collection data from remote subscriber
                    String location = context.clientStty.retrieveRemoteData(cfg.getSubscriberLibrariesFileName(), "collection");
                    if (location == null || location.length() < 1)
                        throw new MungerException("Could not retrieve remote collections file");
                    cfg.setSubscriberLibrariesFileName(""); // clear so the collection file will be used
                    cfg.setSubscriberCollectionFilename(location);

                    context.subscriberRepo.read(cfg.getSubscriberCollectionFilename());
                }
            }

            // process renames first
            if (cfg.isRenaming())
            {
                rename();
            }

            // process -e export text, publisher only
            if (cfg.getExportTextFilename().length() > 0)
            {
                exportText();
            }

            // process -i export collection items, publisher only
            if (cfg.getExportCollectionFilename().length() > 0)
            {
                exportCollection();
            }

            // get -t|T Targets
            if (cfg.isTargetsEnabled())
            {
                getStorageTargets();
            }
            else
            {
                cfg.setDryRun(true);
            }

            // check for publisher duplicates
            if (cfg.isDuplicateCheck())
            {
                duplicatesCheck();
            }
        }
        catch (Exception ex)
        {
            fault = true;
            ++errorCount;
            logger.error(Utils.getStackTrace(ex));
        }
    }

    /**
     * Will the needed size fit?
     */
    private boolean itFits(String path, long size, long minimum, boolean hasTarget) throws Exception
    {
        boolean fit = false;
        long space;
        if (cfg.isRemoteSession())
        {
            // remote subscriber
            space = context.clientStty.availableSpace(path);
        }
        else
        {
            space = Utils.availableSpace(path);
        }

        if (!hasTarget) // provided target file overrides subscriber file locations minimum values
        {
            if (context.subscriberRepo.getLibraryData().libraries.locations != null &&
                    context.subscriberRepo.getLibraryData().libraries.locations.length > 0) // v3.00
            {
                minimum = getLocationMinimum(path);
            }
        }

        logger.info("Check available space for " + (Utils.formatLong(size, false)) +
                " with minimum " + Utils.formatLong(minimum, false) +
                " on " + (cfg.isRemoteSession() ? "remote" : "local") +
                " path " + path + " has " + (Utils.formatLong(space, false)));

        if (space > (size + minimum))
        {
            fit = true;
        }
        return fit;
    }

    /**
     * Is new grouping boolean
     * <p>
     * True if the item "group" is different than the current "group".
     * A group is a set of files within the same movie directory or
     * television season.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    private boolean isNewGrouping(Item publisherItem) throws MungerException
    {
        boolean ret = true;
        String p = publisherItem.getItemPath();
        String s = context.publisherRepo.getSeparator();
        int i = publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator());
        if (i < 0)
        {
            logger.warn("No subdirectory in path : " + publisherItem.getItemPath());
            return true;
        }
        String path = publisherItem.getItemPath().substring(0, i);
        if (path.length() < 1)
        {
            path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator()));
        }
        if (currentGroupName.equalsIgnoreCase(path))
        {
            ret = false;
        }
        else
        {
            currentGroupName = path;
        }
        return ret;
    }

    /**
     * Munge two collections
     * <p>
     * This is the full munge process.
     *
     * @throws MungerException the els exception
     */
    private void munge() throws MungerException
    {
        PrintWriter mismatchFile = null;
        PrintWriter whatsNewFile = null;
        PrintWriter targetFile = null;
        String currentWhatsNew = "";
        String currLib = "";
        ArrayList<Item> group = new ArrayList<>();
        long totalSize = 0;
        Marker SHORT = MarkerManager.getMarker("SHORT");
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");

        String header = "Munging " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description;

        // setup the -m mismatch output file
        if (cfg.getMismatchFilename().length() > 0)
        {
            try
            {
                mismatchFile = new PrintWriter(cfg.getMismatchFilename());
                mismatchFile.println(header);
                logger.info("Writing to Mismatches file " + cfg.getMismatchFilename());
            }
            catch (FileNotFoundException fnf)
            {
                fault = true;
                String s = "File not found exception for Mismatches output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MungerException(s);
            }
        }

        // setup the -w What's New output file
        if (cfg.getWhatsNewFilename().length() > 0)
        {
            try
            {
                whatsNewFile = new PrintWriter(cfg.getWhatsNewFilename());
                whatsNewFile.println("What's New");
                logger.info("Writing to What's New file " + cfg.getWhatsNewFilename());
            }
            catch (FileNotFoundException fnf)
            {
                fault = true;
                String s = "File not found exception for What's New output file " + cfg.getWhatsNewFilename();
                logger.error(s);
                throw new MungerException(s);
            }
        }

        logger.info(header);

        try
        {
            for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
            {
                boolean scanned = false;
                Library pubLib = null;

                // if processing all libraries, or this one was specified on the command line with -l
                if (!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name))
                {
                    // if the publisher has a matching library
                    if ((pubLib = context.publisherRepo.getLibrary(subLib.name)) != null)
                    {
                        // do the libraries have items or do they need to be scanned?
                        if (pubLib.items == null || pubLib.items.size() < 1)
                        {
                            context.publisherRepo.scan(pubLib.name);
                            scanned = true;
                        }
                        if (subLib.items == null || subLib.items.size() < 1)
                        {
                            if (!cfg.isRemoteSession()) // remote collection already loaded and may be empty
                            {
                                context.subscriberRepo.scan(subLib.name);
                            }
                        }

                        logger.info("Munge " + subLib.name + ": " + pubLib.items.size() + " publisher items with " +
                                subLib.items.size() + " subscriber items");

                        // iterate the publisher's items
                        for (Item item : pubLib.items)
                        {
                            if (context.publisherRepo.ignore(item))
                            {
                                logger.debug("  ! Ignoring " + item.getItemPath());
                                ignoredList.add(item.getFullPath());
                            }
                            else
                            {
                                if (!item.isDirectory())
                                {
                                    ++totalItems;

                                    // does the subscriber have a matching item?
                                    Item has = context.subscriberRepo.hasItem(item, Utils.pipe(context.publisherRepo, item.getItemPath()));
                                    if (has != null)
                                    {
                                        if (item.getHas().size() == 1) // no duplicates?
                                        {
                                            if (item.getSize() != has.getSize())
                                            {
                                                logger.warn("  ! Subscriber " + subLib.name + " has different size " + item.getItemPath());
                                                ++differentSizes;
                                            }
                                            else
                                                logger.debug("  = Subscriber " + subLib.name + " has " + item.getItemPath());
                                        } // otherwise duplicates were logged in hasItem(), do not log again
                                    }
                                    else
                                    {
                                        if (cfg.getWhatsNewFilename().length() > 0)
                                        {
                                            logger.info("  + Subscriber " + subLib.name + " missing " + item.getItemPath());

                                            /*
                                             * Unless the -W or --whatsnew-all option is used:
                                             * Only show the left side of mismatches file. And Only show it once.
                                             * So if you have 10 new episodes of Lucifer only the following will show in the what's new file
                                             * Big Bang Theory
                                             * Lucifer
                                             * Legion
                                             */
                                            if (!item.getLibrary().equals(currLib))
                                            {
                                                // If not first time display and reset the whatsNewTotal
                                                if (!currLib.equals(""))
                                                {
                                                    whatsNewFile.println("    --------------------------------");
                                                    whatsNewFile.println("    Number of " + currLib + " = " + whatsNewTotal);
                                                    whatsNewFile.println("    ================================");
                                                    whatsNewTotal = 0;
                                                }
                                                currLib = item.getLibrary();
                                                whatsNewFile.println("");
                                                whatsNewFile.println(currLib);
                                                whatsNewFile.println(new String(new char[currLib.length()]).replace('\0', '='));
                                            }
                                            String path = Utils.getLastPath(item.getItemPath(), context.publisherRepo.getSeparator());
                                            if (cfg.isWhatsNewAll() || !currentWhatsNew.equalsIgnoreCase(path))
                                            {
                                                whatsNewFile.println("    " + (cfg.isWhatsNewAll() ? item.getItemPath() : path));
                                                currentWhatsNew = path;
                                                whatsNewTotal++;
                                            }
                                        }

                                        if (cfg.getMismatchFilename().length() > 0)
                                        {
                                            assert mismatchFile != null;
                                            mismatchFile.println(item.getFullPath());
                                        }

                                        /* If the group is switching, process the current one. */
                                        if (isNewGrouping(item))
                                        {
                                            logger.info("Switching groups from " + lastGroupName + " to " + currentGroupName);
                                            // There is a new group - process the old group
                                            copyGroup(group, totalSize, cfg.isOverwrite());
                                            totalSize = 0L;

                                            // Flush the output files
                                            if (cfg.getWhatsNewFilename().length() > 0)
                                            {
                                                whatsNewFile.flush();
                                            }
                                            if (cfg.getMismatchFilename().length() > 0)
                                            {
                                                mismatchFile.flush();
                                            }
                                        }

                                        if (item.getSize() < 0)
                                        {
                                            logger.warn("File size was < 0 during process, getting");
                                            long size = Files.size(Paths.get(item.getFullPath()));
                                            item.setSize(size);
                                            totalSize += size;
                                        }
                                        else
                                        {
                                            totalSize += item.getSize();
                                        }

                                        // add item to current group
                                        group.add(item);
                                    }
                                }
                                else
                                {
                                    ++totalDirectories;
                                }
                            }
                        }
                    }
                    else
                    {
                        throw new MungerException("Subscribed Publisher library " + subLib.name + " not found");
                    }
                }
                else
                {
                    logger.info("Skipping library: " + subLib.name);
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            ++errorCount;
            logger.error(Utils.getStackTrace(e));
        }
        finally
        {
            if (!fault && group.size() > 0)
            {
                try
                {
                    // Process the last group
                    logger.info("Processing last group " + currentGroupName);
                    copyGroup(group, totalSize, cfg.isOverwrite());
                }
                catch (Exception e)
                {
                    fault = true;
                    ++errorCount;
                    logger.error("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
                }
                totalSize = 0L;
            }

            // Close all the files and show the results
            if (mismatchFile != null)
            {
                mismatchFile.println("----------------------------------------------------");
                mismatchFile.println("Total items: " + grandTotalItems);
                mismatchFile.println("Total size : " + Utils.formatLong(grandTotalSize, true));
                mismatchFile.close();
            }
            if (whatsNewFile != null)
            {
                whatsNewFile.println("    --------------------------------");
                whatsNewFile.println("    Total for " + currLib + " = " + whatsNewTotal);
                whatsNewFile.println("    ================================");
                whatsNewFile.close();
            }
        }

        if (ignoredList.size() > 0)
        {
            logger.info(SHORT, "+------------------------------------------");
            logger.debug(SIMPLE, "Ignored " + ignoredList.size() + " files:");
            for (String s : ignoredList)
            {
                logger.debug(SIMPLE, "    " + s);
            }
        }

        int duplicates = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            if (pubLib.items != null)
            {
                for (Item item : pubLib.items)
                {
                    if (item.getHas().size() > 1)
                    {
                        duplicates = reportDuplicates("Subscriber", item, duplicates);
                    }
                }
            }
        }

        int empties = 0;
        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            if (subLib.items != null)
            {
                for (Item item : subLib.items)
                {
                    if (item.isDirectory() && item.getSize() == 0)
                    {
                        empties = reportEmpties("Subscriber", item, empties);
                    }
                }
            }
        }

        logger.info(SHORT, "+------------------------------------------");
        logger.info(SHORT, "# Different sizes  : " + differentSizes);
        logger.info(SHORT, "# Duplicates       : " + duplicates);
        logger.info(SHORT, "# Empty directories: " + empties);
        logger.info(SHORT, "# Ignored files    : " + ignoredList.size());
        logger.info(SHORT, "# Directories      : " + totalDirectories);
        logger.info(SHORT, "# Files            : " + totalItems);
        logger.info(SHORT, "# Copies           : " + copyCount + ((!cfg.isDryRun()) ? ", " + grandTotalOriginalLocation + " of which went to original locations" : ""));
        logger.info(SHORT, "# Errors           : " + errorCount);
        logger.info(SHORT, "# Items processed  : " + grandTotalItems);
        logger.info(SHORT, "# Total size       : " + Utils.formatLong(grandTotalSize, true));
    }

    /**
     * Process everything
     * <p>
     * This is the where a munge run starts and ends based on configuration.
     * <p>
     * What is done depends on the combination of options specified on the command line.
     */
    public int process()
    {
        Marker SHORT = MarkerManager.getMarker("SHORT");
        int returnValue = 0;

        try
        {
            try
            {
                if (!isInitialized)
                {
                    initialize(); // handles actions other than munge()
                }

                if (isInitialized)
                {
                    // if all the pieces are specified munge the collections
                    if (cfg.isTargetsEnabled() &&
                            (cfg.getPublisherLibrariesFileName().length() > 0 ||
                                    cfg.getPublisherCollectionFilename().length() > 0) &&
                            (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                                    cfg.getSubscriberCollectionFilename().length() > 0)
                    )
                    {
                        munge(); // this is the full munge process
                    }
                }
            }
            catch (Exception ex)
            {
                fault = true;
                ++errorCount;
                logger.error("Inner: " + Utils.getStackTrace(ex));
                returnValue = 2;
            }
        }
        catch (Exception e)
        {
            fault = true;
            ++errorCount;
            logger.error("Outer: " + Utils.getStackTrace(e));
            returnValue = 1;
            cfg = null;
        }
        finally
        {
            if (logger != null)
            {
                logger.info(SHORT, "Process end" + " ------------------------------------------");

                // tell remote end to exit
                if (context.clientStty != null)
                {
                    String resp;
                    try
                    {
                        resp = context.clientStty.roundTrip("quit");
                    }
                    catch (Exception e)
                    {
                        resp = null;
                    }
                    if (resp != null && !resp.equalsIgnoreCase("End-Execution"))
                    {
                        logger.warn("Remote subscriber might not have quit");
                    }
                    else if (resp == null)
                    {
                        logger.warn("Remote subscriber is in an unknown state");
                    }
                }

                // mark the process as successful so it may be detected with automation
                if (!fault)
                    logger.fatal(SHORT, "Process completed normally");
            }
        }

        return returnValue;
    } // process

    /**
     * Search publisher collection for string substitutions for renaming items
     */
    private void rename() throws Exception
    {
        // scan the collection if library file specified
        if (cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }
    }

    private int reportDuplicates(String type, Item item, int duplicates)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
        for (Item dupe : item.getHas())
        {
            if (!dupe.isReported())
            {
                if (duplicates == 0)
                {
                    logger.debug(SIMPLE, "+------------------------------------------");
                    logger.debug(SIMPLE, type + " duplicate filenames found:");
                }
                ++duplicates;
                logger.debug(SIMPLE, "  " + dupe.getFullPath());
                dupe.setReported(true);
            }
        }
        return duplicates;
    }

    private int reportEmpties(String type, Item item, int empties)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
        if (empties == 0)
        {
            logger.debug(SIMPLE, "+------------------------------------------");
            logger.debug(SIMPLE, type + " empty directories found:");
        }
        ++empties;
        logger.debug(SIMPLE, "  " + item.getFullPath());
        return empties;
    }

} // Process
