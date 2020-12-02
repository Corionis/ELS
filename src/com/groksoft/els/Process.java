package com.groksoft.els;

import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.storage.Storage;
import com.groksoft.els.storage.Target;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
    private int errorCount = 0;
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
     * @param from the from
     * @param to   the to
     * @return the boolean
     */
    private boolean copyFile(String from, String to, boolean overwrite)
    {
        try
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
        catch (UnsupportedOperationException e)
        {
            logger.error("Copy problem UnsupportedOperationException: " + e.getMessage());
            return false;
        }
        catch (FileAlreadyExistsException e)
        {
            logger.error("Copy problem FileAlreadyExistsException: " + e.getMessage());
            return false;
        }
        catch (DirectoryNotEmptyException e)
        {
            logger.error("Copy problem DirectoryNotEmptyException: " + e.getMessage());
            return false;
        }
        catch (IOException e)
        {
            logger.error("Copy problem IOException: " + e.getMessage());
            return false;
        }
        return true;
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
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws MungerException
    {
        String response = "";
        if (cfg.getTargetsFilename().length() < 1)
        {
            throw new MungerException("-t or -T target are required for this operation");
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
                        logger.info("  > Would copy #" + copyCount + " " + groupItem.getFullPath());
                    }
                    else
                    {
                        String targetPath = getTarget(groupItem, groupItem.getLibrary(), totalSize);
                        if (targetPath != null)
                        {
                            // copy item(s) to targetPath
                            ++copyCount;

                            String to = targetPath + context.subscriberRepo.getWriteSeparator();
                            to += context.publisherRepo.normalize(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                            String msg = "  > Copying #" + copyCount + " " + groupItem.getFullPath() + " to " + to;
                            logger.info(msg);
                            response += (msg + "\r\n");

                            if (!copyFile(groupItem.getFullPath(), to, overwrite))
                            {
                                ++errorCount;
                            }
                        }
                        else
                        {
                            logger.error("    No space on any targetPath " + group.get(0).getLibrary() + " for " +
                                    lastGroupName + " that is " + totalSize / (1024 * 1024) + " MB");
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
            throw new MungerException(e.getMessage() + " trace: " + Utils.getStackTrace(e));
        }

        return response;
    }

    /**
     * Check publisher collection data for duplicates
     */
    private void duplicatesCheck() throws Exception
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");

        if (!justScannedPublisher)
        {
            context.publisherRepo.scan();
        }
        justScannedPublisher = true;

        logger.info("Analyzing for duplicates" + (cfg.isRenaming() ? " and performing any substitution renames" : ""));
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            for (Item item : pubLib.items)
            {
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

        if (duplicates > 0)
            logger.info(SIMPLE, "Total duplicates: " + duplicates);
        if (empties > 0)
            logger.info(SIMPLE, "Total empty directories: " + empties);
    }

    /**
     * Export publisher collection data to configured file as JSON
     */
    private void exportCollection() throws Exception
    {
        if (!justScannedPublisher)
        {
            context.publisherRepo.scan();
        }
        context.publisherRepo.exportCollection();
        justScannedPublisher = true;
    }

    /**
     * Export publisher collection data to configured file as plain text
     */
    private void exportText() throws Exception
    {
        if (!justScannedPublisher)
        {
            context.publisherRepo.scan();
        }
        context.publisherRepo.exportText();
        justScannedPublisher = true;
    }

    public int getCopyCount()
    {
        return copyCount;
    }

    public void getStorageTargets() throws MungerException
    {
        String location = cfg.getTargetsFilename();

        if (cfg.isRemoteSession() && cfg.isRequestTargets())
        {
            // request target data from remote subscriber
            location = context.clientStty.retrieveRemoteData(location, "targets");
            cfg.setTargetsFilename(location);
        }

        if (storageTargets == null)
            storageTargets = new Storage();

        storageTargets.read(location, context.subscriberRepo.getLibraryData().libraries.flavor);
        if (!cfg.isRemoteSession())
            storageTargets.validate();
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
    public String getTarget(Item item, String library, long size) throws MungerException
    {
        String target = null;
        boolean allFull = true;
        boolean notFound = true;
        long space = 0L;
        long minimum = 0L;

        Target tar = storageTargets.getLibraryTarget(library);
        if (tar != null)
        {
            minimum = Utils.getScaledValue(tar.minimum);
        }
        else
        {
            minimum = Storage.minimumBytes;
        }

        // see if there is an "original" directory the new content will fit in
        if (!cfg.isNoBackFill())
        {
            String path = context.subscriberRepo.hasDirectory(library, Utils.pipe(context.publisherRepo, item.getItemPath()));
            if (path != null)
            {
                if (cfg.isRemoteSession())
                {
                    // remote subscriber
                    space = context.clientStty.availableSpace(path);
                }
                else
                {
                    space = Utils.availableSpace(path);
                }
                logger.info("Checking space on " + (cfg.isRemoteSession() ? "remote" : "local") +
                        " path " + path + " = (" + (Utils.formatLong(space)) +
                        ") for " + (Utils.formatLong(size)) +
                        ", minimum " + Utils.formatLong(minimum));
                if (space > (size + minimum))
                {
                    logger.info("Using original storage location for " + item.getItemPath() + " at " + path);
                    //
                    // inline return
                    //
                    ++grandTotalOriginalLocation;
                    return path;
                }
                else
                {
                    logger.info("Original storage location too full for " + item.getItemPath() + " (" + size + ") at " + path);
                }
            }
        }

        // find a matching target
        if (tar != null)
        {
            notFound = false;
            for (int j = 0; j < tar.locations.length; ++j)
            {
                // check space on the candidate target
                String candidate = tar.locations[j];
                if (cfg.isRemoteSession())
                {
                    // remote subscriber
                    space = context.clientStty.availableSpace(candidate);
                }
                else
                {
                    space = Utils.availableSpace(candidate);
                }
                if (space > minimum)
                {
                    // check target space minimum
                    allFull = false;
                    if (space > (size + minimum))
                    {
                        // check size of item(s) to be copied
                        target = candidate;             // has space, use it
                        break;
                    }
                }
            }
            if (allFull)
            {
                logger.error("All locations for library " + library + " are below specified minimum of " + tar.minimum);

                // todo Should this be a throw ??
                System.exit(2);     // EXIT the program
            }
        }
        if (notFound)
        {
            logger.error("No target library match found for publisher library " + library);
        }
        return target;
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

            if (isInitialized)
            {
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
                if (cfg.getTargetsFilename().length() > 0)
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
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
        }
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
                if (!cfg.isSpecificPublisherLibrary() || cfg.isSelectedPublisherLibrary(subLib.name))
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
                            if (cfg.isRemoteSession())
                            {
                                throw new MungerException("Subscriber collection missing data for subscriber library " + subLib.name);
                            }
                            context.subscriberRepo.scan(subLib.name);
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
                                                logger.warn("  ! Subscriber " + subLib.name + " has different size " + item.getItemPath());
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
            logger.error("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
        }
        finally
        {
            if (group.size() > 0)
            {
                // Process the last group
                logger.info("Processing last group " + currentGroupName);
                copyGroup(group, totalSize, cfg.isOverwrite());
                totalSize = 0L;
            }

            // Close all the files and show the results
            if (mismatchFile != null)
            {
                mismatchFile.println("----------------------------------------------------");
                mismatchFile.println("Total items: " + grandTotalItems);
                mismatchFile.println("Total size : " + Utils.formatLong(grandTotalSize));
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
            logger.info(SIMPLE, "-----------------------------------------------------");
            logger.info(SIMPLE, "Ignored " + ignoredList.size() + " files:");
            for (String s : ignoredList)
            {
                logger.info(SIMPLE, "    " + s);
            }
        }

        int duplicates = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            for (Item item : pubLib.items)
            {
                if (item.getHas().size() > 1)
                {
                    duplicates = reportDuplicates("Subscriber", item, duplicates);
                }
            }
        }

        int empties = 0;
        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            for (Item item : subLib.items)
            {
                if (item.isDirectory() && item.getSize() == 0)
                {
                    empties = reportEmpties("Subscriber", item, empties);
                }
            }
        }

        if (duplicates > 0)
            logger.info(SHORT, "# Duplicates       : " + duplicates);
        if (empties > 0)
            logger.info(SHORT, "# Empty directories: " + empties);
        if (ignoredList.size() > 0)
            logger.info(SHORT, "# Ignored files    : " + ignoredList.size());
        logger.info(SHORT, "# Directories      : " + totalDirectories);
        logger.info(SHORT, "# Files            : " + totalItems);
        logger.info(SHORT, "# Copies           : " + copyCount + ((!cfg.isDryRun()) ? ", " + grandTotalOriginalLocation + " of which went to original locations" : ""));
        logger.info(SHORT, "# Errors           : " + errorCount);
        logger.info(SHORT, "# Items processed  : " + grandTotalItems);
        logger.info(SHORT, "# Total size       : " + Utils.formatLong(grandTotalSize));
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
        int returnValue = 0;

        try
        {
            try
            {
                if (!isInitialized)
                {
                    initialize();
                }

                if (isInitialized)
                {
                    // if all the pieces are specified munge the collections
                    if ((cfg.getPublisherLibrariesFileName().length() > 0 ||
                            cfg.getPublisherCollectionFilename().length() > 0) &&
                            (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                                    cfg.getSubscriberCollectionFilename().length() > 0) &&
                            cfg.getTargetsFilename().length() > 0)
                    {
                        munge(); // this is the full munge process
                    }
                }
            }
            catch (Exception ex)
            {
                logger.error("Inner: " + Utils.getStackTrace(ex));
                returnValue = 2;
            }
        }
        catch (Exception e)
        {
            logger.error("Outer: " + Utils.getStackTrace(e));
            returnValue = 1;
            cfg = null;
        }
        finally
        {
            // the - makes searching for the ending of a run easier
            if (logger != null)
            {
                logger.info("- Process end" + " ------------------------------------------");

                if (context.clientStty != null)
                {
                    String resp = context.clientStty.roundTrip("quit");
                    if (resp != null && !resp.equalsIgnoreCase("End-Execution"))
                    {
                        logger.warn("Remote subscriber might not have quit");
                    }
                }
                //LogManager.shutdown();
            }
        }

        return returnValue;
    } // process

    /**
     * Search publisher collection for string substitutions for renaming items
     */
    private void rename() throws Exception
    {
        // scan the collection
        if (!justScannedPublisher)
        {
            context.publisherRepo.scan();
        }

        // see if there are any renames performed
        if (context.publisherRepo.renameContent())
        {
            // reset and rescan so JSON data reflects reality
            context.publisherRepo.resetItems();
            context.publisherRepo.scan();
        }
        justScannedPublisher = true;
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
                    logger.info(SIMPLE, "-----------------------------------------------------");
                    logger.info(SIMPLE, type + " duplicate filenames found:");
                }
                ++duplicates;
                logger.info(SIMPLE, "  " + dupe.getFullPath());
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
            logger.info(SIMPLE, "-----------------------------------------------------");
            logger.info(SIMPLE, type + " empty directories found:");
        }
        ++empties;
        logger.info(SIMPLE, "  " + item.getFullPath());
        return empties;
    }

} // Process
