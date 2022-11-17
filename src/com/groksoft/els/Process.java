package com.groksoft.els;

import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Process class where the primary operations are executed.
 */
public class Process
{
    private Configuration cfg = null;
    private Context context;
    private int differentSizes = 0;
    private int errorCount = 0;
    private boolean fault = false;
    private Hints hints = null;
    private ArrayList<String> ignoredList = new ArrayList<>();
    private boolean isInitialized = false;
    private boolean justScannedPublisher = false;
    private transient Logger logger = LogManager.getLogger("applog");
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
    public Process(Configuration config, Context ctxt)
    {
        this.cfg = config;
        this.context = ctxt;
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
            logger.info("Analyzing library '" + pubLib.name + "' for duplicates");
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

        reportIgnored();

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
        context.publisherRepo.exportItems(true);
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

    /**
     * Process ELS Hints for the local system
     */
    private void hintsLocal() throws Exception
    {
        // scan the collection if library file specified
        if (cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }
        hints.hintsLocal();
    }

    /**
     * Munge two collections
     * <p>
     * This is the full munge process.
     *
     * @throws MungeException the els exception
     */
    private void munge() throws Exception
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

        String header = "Munging collections " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description + (cfg.isDryRun() ? " (--dry-run)" : "");

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
                throw new MungeException(s);
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
                throw new MungeException(s);
            }
        }

        logger.info(header);
        try
        {
            for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
            {
                if (fault)
                    break;

                boolean scanned = false;
                Library pubLib = null;

                // if processing all libraries, or this one was specified on the command line with -l,
                // and it has not been excluded with -L
                if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name)) &&
                        (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(subLib.name))) 
                {
                    // if the subscriber has included and not excluded this library
                    if (subLib.name.startsWith(context.subscriberRepo.SUB_EXCLUDE)) 
                    {
                        String n = subLib.name.replaceFirst(context.subscriberRepo.SUB_EXCLUDE, "");
                        logger.info("Skipping subscriber library: " + n);
                        continue;
                    }

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
                                (subLib.items != null ? subLib.items.size() : 0) + " subscriber items");

                        // iterate the publisher's items
                        for (Item item : pubLib.items)
                        {
                            if (fault)
                                break;

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
                                    Item has = context.subscriberRepo.hasItem(item, item.getLibrary(), Utils.pipe(context.publisherRepo, item.getItemPath()));
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
                                        if (context.transfer.isNewGrouping(item))
                                        {
                                            // There is a new group - process the old group
                                            logger.info("Switching groups from " + context.transfer.getLastGroupName() + " to " + context.transfer.getCurrentGroupName());
                                            context.transfer.copyGroup(group, totalSize, cfg.isOverwrite());
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

                                        totalSize += item.getSize();
                                        group.add(item);
                                        if (!cfg.isDryRun())
                                            subLib.rescanNeeded = true;
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
                        logger.warn("Subscribed Publisher library " + subLib.name + " not found");
                    }
                }
                else
                {
                    logger.info("Skipping publisher library: " + subLib.name);
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            ++errorCount;
            //logger.error(Utils.getStackTrace(e));
            throw (e);
        }
        finally
        {
            if (!fault && group.size() > 0)
            {
                try
                {
                    // Process the last group
                    logger.info("Processing last group " + context.transfer.getCurrentGroupName());
                    context.transfer.copyGroup(group, totalSize, cfg.isOverwrite());
                }
                catch (Exception e)
                {
                    fault = true;
                    ++errorCount;
                    //logger.error(Utils.getStackTrace(e));
                    throw (e);
                }
                totalSize = 0L;
            }

            // Close all the files and show the results
            if (mismatchFile != null)
            {
                mismatchFile.println("----------------------------------------------------");
                mismatchFile.println("Total items: " + context.transfer.getGrandTotalItems());
                mismatchFile.println("Total size : " + Utils.formatLong(context.transfer.getGrandTotalSize(), true));
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

        reportIgnored();

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
        logger.info(SHORT, "# Copies           : " + context.transfer.getCopyCount() + ((!cfg.isDryRun() && context.transfer.getCopyCount() > 0) ? ", " + context.transfer.getGrandTotalOriginalLocation() + " of which went to original locations" : "") + (cfg.isDryRun() ? " (--dry-run)" : ""));
        logger.info(SHORT, "# Errors           : " + errorCount);
        logger.info(SHORT, "# Items processed  : " + context.transfer.getGrandTotalItems());
        logger.info(SHORT, "# Total size       : " + Utils.formatLong(context.transfer.getGrandTotalSize(), true));
    }

    /**
     * Process everything
     * <p>
     * This is the where a munge run starts and ends based on configuration.
     * <p>
     * What is done depends on the combination of options specified on the command line.
     */
    public void process()
    {
        Marker SHORT = MarkerManager.getMarker("SHORT");
        boolean lined = false;
        boolean localHints = false;

        try
        {
            if (!isInitialized)
            {
                context.transfer = new Transfer(cfg, context);
                context.transfer.initialize();
                isInitialized = true;
            }

            // Get ELS hints keys if specified
            if (cfg.getHintKeysFile().length() > 0) 
            {
                context.hintKeys = new HintKeys(cfg, context);
                context.hintKeys.read(cfg.getHintKeysFile());
                hints = new Hints(cfg, context, context.hintKeys);
            }

            // process ELS Hints locally, no subscriber, publisher's targets
            if (hints != null && cfg.isTargetsEnabled() && !cfg.isRemoteSession() &&
                    (cfg.getPublisherLibrariesFileName().length() > 0 ||
                            cfg.getPublisherCollectionFilename().length() > 0) &&
                    (cfg.getSubscriberLibrariesFileName().length() == 0 &&
                            cfg.getSubscriberCollectionFilename().length() == 0))
            {
                localHints = true; // skip munge, the targets are wrong for that
                hintsLocal();
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

            // check for publisher duplicates
            if (cfg.isDuplicateCheck())
            {
                duplicatesCheck();
            }

            // process ELS Hints to subscriber
            if (hints != null && cfg.isTargetsEnabled() &&
                    (cfg.getPublisherLibrariesFileName().length() > 0 ||
                            cfg.getPublisherCollectionFilename().length() > 0) &&
                    (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                            cfg.getSubscriberCollectionFilename().length() > 0))
            {
                hints.hintsMunge();
            }

            // if all the pieces are specified perform a full munge of the collections
            if (!context.fault && !localHints && !cfg.isHintSkipMainProcess())
            {
                if (cfg.isTargetsEnabled() &&
                        (cfg.getPublisherLibrariesFileName().length() > 0 ||
                                cfg.getPublisherCollectionFilename().length() > 0) &&
                        (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                                cfg.getSubscriberCollectionFilename().length() > 0))
                {
                    munge();
                }
            }

            // clean-up ELS Hints on subscriber
            if (!context.fault && hints != null && !localHints && !cfg.isDryRun() && cfg.isTargetsEnabled() &&
                    (cfg.getPublisherLibrariesFileName().length() > 0 ||
                            cfg.getPublisherCollectionFilename().length() > 0) &&
                    (cfg.getSubscriberLibrariesFileName().length() > 0 ||
                            cfg.getSubscriberCollectionFilename().length() > 0))
            {
                logger.info(SHORT, "-------------------------------------------");
                lined = true;
                hints.hintsSubscriberCleanup();
            }

        }
        catch (Exception ex)
        {
            fault = true;
            ++errorCount;
            logger.error(Utils.getStackTrace(ex));
        }
        finally
        {
            if (logger != null)
            {
                if (!lined)
                    logger.info(SHORT, "-------------------------------------------");

                if (cfg.isHintSkipMainProcess() && !localHints)
                {
                    logger.info("! Skipping main process, hint processing with -K | --keys-only enabled");
                }

                // tell remote end to exit
                if (context.clientStty != null)
                {
                    String resp = "";
                    try
                    {
                        // if a fault occurred tell any listener
                        if (fault && context.clientStty.isConnected())
                        {
                            // ... this is done in Main.process finally clause ...
                            //logger.warn("Sending remote fault command (2)");
                            //resp = context.clientStty.roundTrip("fault");
                        }
                        else
                        {
                            if (!cfg.isKeepGoing())
                                resp = context.clientStty.roundTrip("quit", "Sending quit command to remote", 1000);
                            else
                            {
                                context.clientStty.send("bye", "Sending bye command to remote");
                                resp = "End-Execution";
                            }
                            Thread.sleep(1000);
                        }
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
            }
        }
        context.fault = fault;
    } // process

    /**
     * Dump any duplicates found to the log
     *
     * @param type       Publisher or Subscriber, description
     * @param item       The item with duplicates
     * @param duplicates The count of duplicated
     * @return New count of duplicates
     */
    private int reportDuplicates(String type, Item item, int duplicates)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
        for (Item dupe : item.getHas())
        {
            if (!dupe.isReported())
            {
                if (duplicates == 0)
                {
                    if (cfg.isDuplicateCheck())
                    {
                        logger.debug(SIMPLE, "+------------------------------------------");
                        logger.debug(SIMPLE, type + " duplicate filenames found:");
                    }
                }
                ++duplicates;
                if (cfg.isDuplicateCheck())
                    logger.debug(SIMPLE, "  " + dupe.getFullPath());
                dupe.setReported(true);
            }
        }
        return duplicates;
    }

    /**
     * Dump any empty directories to the log
     *
     * @param type    Publisher or Subscriber, description
     * @param item    The item with empties
     * @param empties The count of empties
     * @return The new count of empties
     */
    private int reportEmpties(String type, Item item, int empties)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
        if (empties == 0)
        {
            if (cfg.isEmptyDirectoryCheck())
            {
                logger.debug(SIMPLE, "+------------------------------------------");
                logger.debug(SIMPLE, type + " empty directories found:");
            }
        }
        ++empties;
        if (cfg.isEmptyDirectoryCheck())
            logger.debug(SIMPLE, "  " + item.getFullPath());
        return empties;
    }

    /**
     * Dump the list of ignored files
     */
    private void reportIgnored()
    {
        if (cfg.isIgnoredReported())
        {
            Marker SHORT = MarkerManager.getMarker("SHORT");
            Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
            if (ignoredList.size() > 0)
            {
                logger.debug(SHORT, "+------------------------------------------");
                logger.debug(SIMPLE, "Ignored " + ignoredList.size() + " files:");
                for (String s : ignoredList)
                {
                    logger.debug(SIMPLE, "    " + s);
                }
            }
        }
    }

} // Process
