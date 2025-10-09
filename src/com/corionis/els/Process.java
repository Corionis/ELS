package com.corionis.els;

import com.corionis.els.repository.Item;
import com.corionis.els.repository.Library;
import com.corionis.els.tools.Tools;
import com.corionis.els.tools.email.EmailHandler;
import com.corionis.els.tools.email.EmailTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Process class where the primary operationsUI are executed.
 */
public class Process
{
    private Marker SHORT = MarkerManager.getMarker("SHORT");
    private Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Context context;
    private String currentWhatsNew = "";
    private int differentSizes = 0;
    private int errorCount = 0;
    private boolean fault = false;
    private ArrayList<String> ignoredList = new ArrayList<>();
    private boolean isInitialized = false;
    private boolean justScannedPublisher = false;
    private PrintWriter mismatchesFile = null;
    private PrintWriter mismatchesFileHtml = null;
    private long totalDirectories = 0;
    private long totalItems = 0;
    private int warnings = 0;
    private PrintWriter whatsNewFile = null;
    private PrintWriter whatsNewFileHtml = null;
    private long whatsNewTotal = 0;
    private transient Logger logger = LogManager.getLogger("applog");

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
    public Process(Context context)
    {
        this.context = context;
    }

    private void createOutputFiles(String filename, boolean isMismatches) throws MungeException
    {
        if (filename.length() > 0)
        {
            String where = Utils.getFullPathLocal(filename);
            where = Utils.pipe(where);
            where = Utils.unpipe(context.publisherRepo, where);
            try
            {
                if (isMismatches)
                    mismatchesFile = new PrintWriter(where);
                else
                    whatsNewFile = new PrintWriter(where);
                logger.info(context.cfg.gs("Process.writing.to.file") + where);

                where += ".html";
                if (isMismatches)
                    mismatchesFileHtml = new PrintWriter(where);
                else
                    whatsNewFileHtml = new PrintWriter(where);
                logger.info(context.cfg.gs("Process.writing.to.file") + where);
            }
            catch (FileNotFoundException fnf)
            {
                fault = true;
                String s = context.cfg.gs("Process.file.not.found.exception.for.output.file") + where;
                logger.error(s);
                throw new MungeException(s);
            }
        }
    }

    /**
     * Check publisher collection data for duplicates
     */
    private void duplicatesCheck() throws Exception
    {
        // scan the collection if library file specified
        if (context.cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }

        totalDirectories = 0;
        totalItems = 0;
        for (Library pubLib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.analyzing.library.for.duplicates"), pubLib.name));
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

        logger.info(SIMPLE, context.cfg.gs("Process.total.files") + totalItems);
        logger.info(SIMPLE, context.cfg.gs("Process.total.directories") + totalDirectories);
        logger.info(SIMPLE, context.cfg.gs("Process.total.items") + (totalItems + totalDirectories));
        logger.info(SIMPLE, context.cfg.gs("Process.total.duplicates") + duplicates);
        logger.info(SIMPLE, context.cfg.gs("Process.total.empty.directories") + empties);
    }

    /**
     * Export publisher collection data to configured file as JSON
     */
    private void exportCollection() throws Exception
    {
        // scan the collection if library file specified
        if (context.cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
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
        if (context.cfg.getPublisherLibrariesFileName().length() > 0 && !justScannedPublisher)
        {
            context.publisherRepo.scan();
            justScannedPublisher = true;
        }
        context.publisherRepo.exportText();
    }

    public int getWarnings()
    {
        return warnings;
    }

    /**
     * Munge two collections
     * <p>
     * This is the full back-up process.
     *
     * @throws MungeException the els exception
     */
    private void munge() throws Exception
    {
        String currLibMismatch = "";
        String currLibWhat = "";
        ArrayList<Item> group = new ArrayList<>();
        long totalSize = 0;

        String intro = context.cfg.gs("Process.munging.back.up") + context.publisherRepo.getLibraryData().libraries.description +
                context.cfg.gs("Process.to") + context.subscriberRepo.getLibraryData().libraries.description +
                (context.cfg.isDryRun() ? " (--dry-run)" : "");

        if (mismatchesFile != null)
        {
            mismatchesFile.println(context.cfg.gs("Process.mismatches") + "\n\n" + intro);
            mismatchesFile.println("");

            mismatchesFileHtml.println("<h3>" + context.cfg.gs("Process.mismatches") + "</h3>\n" + intro);
            mismatchesFileHtml.println("<br/><br/>");
        }

        if (whatsNewFile != null)
        {
            whatsNewFile.println(context.cfg.gs("Process.whats.new") + "\n\n" + intro);
            whatsNewFile.println("");

            whatsNewFileHtml.println("<h3>" + context.cfg.gs("Process.whats.new") + "</h3>\n" + intro);
            whatsNewFileHtml.println("<br/><br/>");
        }

        boolean rescan = false;
        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            if (subLib.rescanNeeded)
            {
                rescan = true;
                break;
            }
        }
        if (rescan)
        {
            logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.hints.executed.on.subscriber.updated.data.required"),
                    context.subscriberRepo.getLibraryData().libraries.description));
            if (context.cfg.isRemoteSubscriber())
                context.transfer.requestCollection();
            //else is local, handled below
        }

        logger.info(intro);

        boolean firstLibraryMismatch = true;
        boolean firstLibraryWhats = true;
        try
        {
            for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
            {
                if (fault)
                    break;

                Library pubLib = null;

                // if processing all libraries, or this one was specified on the command line with -l,
                // and it has not been excluded with -L
                if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(subLib.name)) &&
                        (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(subLib.name)))
                {
                    // if the subscriber has included and not excluded this library
                    if (subLib.name.startsWith(context.subscriberRepo.SUB_EXCLUDE))
                    {
                        String n = subLib.name.replaceFirst(context.subscriberRepo.SUB_EXCLUDE, "");
                        logger.info(context.cfg.gs("Process.skipping.subscriber.library") + n);
                        continue;
                    }

                    // if the publisher has a matching library
                    if ((pubLib = context.publisherRepo.getLibrary(subLib.name)) != null)
                    {
                        // do the libraries have items or do they need to be scanned?
                        if (pubLib.items == null || pubLib.items.size() < 1 || pubLib.rescanNeeded)
                        {
                            context.publisherRepo.scan(pubLib.name);
                        }
                        if (subLib.items == null || subLib.items.size() < 1 || subLib.rescanNeeded) // remote rescan handled above
                        {
                            context.subscriberRepo.scan(subLib.name);
                        }

                        logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.munge.publisher.items.with.subscriber.items"),
                                subLib.name, pubLib.items.size(), subLib.items.size(), subLib.items != null ? 0 : 1));

                        // iterate the publisher's items
                        for (Item item : pubLib.items)
                        {
                            if (fault)
                                break;

                            if (context.publisherRepo.ignore(item))
                            {
                                logger.debug(context.cfg.gs("Process.ignoring") + item.getItemPath());
                                ignoredList.add(item.getFullPath());
                            }
                            else
                            {
                                if (!item.isDirectory())
                                {
                                    boolean diffDates = false;
                                    ++totalItems;

                                    // does the subscriber have a matching item?
                                    Item has = context.subscriberRepo.hasItem(item, item.getLibrary(), Utils.pipe(context.publisherRepo, item.getItemPath()));
                                    if (has != null)
                                    {
                                        if (item.getHas().size() == 1) // no duplicates?
                                        {
                                            // match dates?
                                            if (subLib.matchDates && item.getModifiedDate().compareTo(has.getModifiedDate()) > 0)
                                            {
                                                diffDates = true;
                                                has = null;
                                            }
                                            else if (item.getSize() != has.getSize())
                                            {
                                                logger.warn(java.text.MessageFormat.format(context.cfg.gs("Process.subscriber.has.different.size"),
                                                        subLib.name,item.getItemPath()));
                                                ++differentSizes;
                                            }
                                            else
                                                logger.debug(java.text.MessageFormat.format(context.cfg.gs("Process.subscriber.has"),
                                                        subLib.name,item.getItemPath()));
                                        } // otherwise duplicates were logged in hasItem(), do not log again
                                    }

                                    if (has == null)
                                    {
                                        if (diffDates)
                                            logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.subscriber.out.of.date"),
                                                    subLib.name,item.getItemPath()));
                                        else
                                            logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.subscriber.missing"),
                                                    subLib.name,item.getItemPath()));

                                        /* If the group is switching, process the current one. */
                                        if (context.transfer.isNewGrouping(item))
                                        {
                                            // There is a new group - process the previous group
                                            logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.switching.groups.from.to"),
                                                    context.transfer.getLastGroupName(),context.transfer.getCurrentGroupName()));
                                            context.transfer.copyGroup(group, totalSize, context.cfg.isOverwrite(), whatsNewFile, whatsNewFileHtml, mismatchesFile, mismatchesFileHtml);
                                            totalSize = 0L;

                                            if (mismatchesFile != null)
                                            {
                                                if (!item.getLibrary().equals(currLibWhat))
                                                {
                                                    currLibMismatch = item.getLibrary();
                                                    if (!firstLibraryMismatch)
                                                        mismatchesFile.println("");
                                                    mismatchesFile.println(currLibMismatch);

                                                    if (!firstLibraryMismatch)
                                                        mismatchesFileHtml.println("</span><br/>");
                                                    mismatchesFileHtml.println("<span style=\"font-family: Arial, Helvetica, sans-serif;\"><b>" + currLibMismatch + "</b><br/>");
                                                    mismatchesFileHtml.println("</span><span style=\"font-family: monospace; font-size: 110%;\">");
                                                    firstLibraryMismatch = false;
                                                }
                                            }

                                            if (whatsNewFile != null)
                                            {
                                                /*
                                                 * Unless the -W or --whatsnew-all option is used:
                                                 * Only show the left side of mismatches file. And Only show it once.
                                                 * So if you have 10 new episodes of Lucifer only the following will show in the what's new file
                                                 * Big Bang Theory
                                                 * Lucifer
                                                 * Legion
                                                 */
                                                if (!item.getLibrary().equals(currLibWhat))
                                                {
                                                    // If not first time display and reset the whatsNewTotal
                                                    if (!currLibWhat.equals(""))
                                                    {
                                                        whatsNewFile.println("    ------------------------------------------");
                                                        whatsNewFile.println(java.text.MessageFormat.format(context.cfg.gs("Process.total.for"), currLibWhat, whatsNewTotal));
                                                        whatsNewFile.println("");

                                                        whatsNewFileHtml.println("</span><span style=\"font-family: Arial, Helvetica, sans-serif;\">");
                                                        whatsNewFileHtml.println("<hr style=\"margin-left: 30px; margin-right: auto; width: 50%;\">");
                                                        whatsNewFileHtml.println("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + java.text.MessageFormat.format(context.cfg.gs("Process.total.for"), currLibWhat, whatsNewTotal));
                                                        whatsNewFileHtml.println("<br/><br/>");
                                                        whatsNewTotal = 0;
                                                    }

                                                    currLibWhat = item.getLibrary();
                                                    whatsNewFile.println(currLibWhat);

                                                    whatsNewFileHtml.println("<b>" + currLibWhat + "</b><br/>");
                                                    if (firstLibraryWhats)
                                                        whatsNewFileHtml.println("<span style=\"font-family: monospace; font-size: 110%;\">");
                                                    else
                                                        whatsNewFileHtml.println("</span><span style=\"font-family: monospace; font-size: 110%;\">");
                                                    firstLibraryWhats = false;
                                                }

                                                String path = Utils.getLastPath(item.getItemPath(), context.publisherRepo.getSeparator());
                                                if (context.cfg.isWhatsNewAll() || !currentWhatsNew.equalsIgnoreCase(path))
                                                {
                                                    whatsNewFile.println("    " + (context.cfg.isWhatsNewAll() ? item.getItemPath() : path));
                                                    whatsNewFileHtml.println("&nbsp;&nbsp;&nbsp;&nbsp;" + (context.cfg.isWhatsNewAll() ? item.getItemPath() : path) + "<br/>");
                                                    currentWhatsNew = path;
                                                    whatsNewTotal++;
                                                }
                                            }

                                            // Flush the output files
                                            if (whatsNewFile != null)
                                            {
                                                whatsNewFile.flush();
                                                whatsNewFileHtml.flush();
                                            }
                                            if (mismatchesFile != null)
                                            {
                                                mismatchesFile.flush();
                                                mismatchesFileHtml.flush();
                                            }
                                        }

                                        totalSize += item.getSize();
                                        group.add(item);
                                        if (!context.cfg.isDryRun())
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
                        logger.warn(java.text.MessageFormat.format(context.cfg.gs("Process.subscribed.publisher.library.not.found"), subLib.name));
                        ++warnings;
                    }
                }
                else
                {
                    logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.skipping.publisher.library"), subLib.name));
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
                    logger.info(java.text.MessageFormat.format(context.cfg.gs("Process.processing.last.group"),
                            context.transfer.getCurrentGroupName()));
                    context.transfer.copyGroup(group, totalSize, context.cfg.isOverwrite(), whatsNewFile, whatsNewFileHtml, mismatchesFile, mismatchesFileHtml);
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
            if (mismatchesFile != null)
            {
                mismatchesFile.println("------------------------------------------");
                if (getWarnings() > 0)
                    mismatchesFile.println(MessageFormat.format(context.cfg.gs("Process.warnings"), getWarnings()));
                if (errorCount > 0)
                    mismatchesFile.println(MessageFormat.format(context.cfg.gs("Process.errors"), errorCount));
                mismatchesFile.println(context.cfg.gs("Process.total.items") + context.transfer.getGrandTotalItems());
                mismatchesFile.println(context.cfg.gs("Process.total.size") + Utils.formatLong(context.transfer.getGrandTotalSize(), true, context.cfg.getLongScale()));
                mismatchesFile.println("\n");
                mismatchesFile.println("------- " + context.cfg .gs("Email.do.not.reply") + " -------\n");
                mismatchesFile.close();

                mismatchesFileHtml.println("</span><span style=\"font-family: Arial, Helvetica, sans-serif;\">");
                mismatchesFileHtml.println("<hr style=\"margin-left: 0; margin-right: auto; width: 50%;\">");
                if (getWarnings() > 0)
                    mismatchesFileHtml.println(MessageFormat.format(context.cfg.gs("Process.warnings"), getWarnings()) + "<br/>");
                if (errorCount > 0)
                    mismatchesFileHtml.println(MessageFormat.format(context.cfg.gs("Process.errors"), errorCount) + "<br/>");
                mismatchesFileHtml.println(context.cfg.gs("Process.total.items") + context.transfer.getGrandTotalItems() + "<br/>");
                mismatchesFileHtml.println(context.cfg.gs("Process.total.size") + Utils.formatLong(context.transfer.getGrandTotalSize(), true, context.cfg.getLongScale()) + "<br/>");
                mismatchesFileHtml.println("</span>");
                mismatchesFileHtml.println("<br/><br/>------- " + context.cfg .gs("Email.do.not.reply") + " -------<br/><br/>");
                mismatchesFileHtml.println("</body></html>");
                mismatchesFileHtml.close();
            }

            if (whatsNewFile != null)
            {
                whatsNewFile.println("    ------------------------------------------");
                whatsNewFile.println(java.text.MessageFormat.format(context.cfg.gs("Process.total.for"), currLibWhat, whatsNewTotal));
                whatsNewFile.println("\n");
                whatsNewFile.println("------- " + context.cfg .gs("Email.do.not.reply") + " -------\n");
                whatsNewFile.close();

                whatsNewFileHtml.println("</span><span style=\"font-family: Arial, Helvetica, sans-serif;\">");
                whatsNewFileHtml.println("<hr style=\"margin-left: 30px; margin-right: auto; width: 50%;\">");
                whatsNewFileHtml.println("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + java.text.MessageFormat.format(context.cfg.gs("Process.total.for"), currLibWhat, whatsNewTotal));
                whatsNewFileHtml.println("</span>");
                whatsNewFileHtml.println("<br/><br/><br/>------- " + context.cfg .gs("Email.do.not.reply") + " -------<br/><br/>");
                whatsNewFileHtml.println("</body></html>");
                whatsNewFileHtml.close();
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
        logger.info(SHORT, context.cfg.gs("Process.different.sizes") + differentSizes);
        logger.info(SHORT, context.cfg.gs("Process.duplicates") + duplicates);
        logger.info(SHORT, context.cfg.gs("Process.empty.directories") + empties);
        logger.info(SHORT, context.cfg.gs("Process.ignored.files") + ignoredList.size());
        logger.info(SHORT, context.cfg.gs("Process.directories") + totalDirectories);
        logger.info(SHORT, context.cfg.gs("Process.files") + totalItems);
        logger.info(SHORT, context.cfg.gs("Process.copies") + context.transfer.getCopyCount() + ((!context.cfg.isDryRun() && context.transfer.getCopyCount() > 0) ? ", " + context.transfer.getGrandTotalOriginalLocation() + " of which went to original locations" : "") + (context.cfg.isDryRun() ? " (--dry-run)" : ""));
        logger.info(SHORT, context.cfg.gs("Process.warnings.fin") + getWarnings());
        logger.info(SHORT, context.cfg.gs("Process.errors.fin") + errorCount);
        logger.info(SHORT, context.cfg.gs("Process.items.processed") + context.transfer.getGrandTotalItems());
        logger.info(SHORT, context.cfg.gs("Process.total.size.fin") + Utils.formatLong(context.transfer.getGrandTotalSize(), true, context.cfg.getLongScale()));
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
        boolean lined = false;
        boolean localHints = false;
        String result = "";

        try
        {
            if (!isInitialized)
            {
                context.transfer = new Transfer(context);
                context.transfer.initialize();
                isInitialized = true;
            }

            String hostname = "";
            try
            {
                hostname = " (" + InetAddress.getLocalHost().getHostName() + ")";
            }
            catch (UnknownHostException e)
            {
                // ignored
            }
            String from = context.publisherRepo.getLibraryData().libraries.description + hostname;

            // setup the -m Mismatches output file
            if (context.cfg.getMismatchFilename().length() > 0)
            {
                createOutputFiles(context.cfg.getMismatchFilename(), true);

                mismatchesFile.println("Automated email from: " + from);
                mismatchesFile.println("------------------------------------------\n");

                mismatchesFileHtml.println("<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">");
                mismatchesFileHtml.println("<div><img src='https://www.elsnavigator.com/assets/images/els-logo-64px.png' style=\"vertical-align: middle;\"/>");
                mismatchesFileHtml.println("<span style=\"font-family: Arial, Helvetica, sans-serif;font-size: 120%;vertical-align: middle;\">");
                mismatchesFileHtml.println("<b>&nbsp;&nbsp;Automated email from: " + from + "</b></span></div>");
                mismatchesFileHtml.println("<hr/>");
            }

            // setup the -w|-W What's New output file
            if (context.cfg.getWhatsNewFilename().length() > 0)
            {
                createOutputFiles(context.cfg.getWhatsNewFilename(), false);

                whatsNewFile.println("Automated email from: " + from);
                whatsNewFile.println("------------------------------------------\n");

                whatsNewFileHtml.println("<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">");
                whatsNewFileHtml.println("<div><img src='https://www.elsnavigator.com/assets/images/els-logo-64px.png' style=\"vertical-align: middle;\"/>");
                whatsNewFileHtml.println("<span style=\"font-family: Arial, Helvetica, sans-serif;font-size: 120%;vertical-align: middle;\">");
                whatsNewFileHtml.println("<b>&nbsp;&nbsp;Automated email from: " + from + "</b></span></div>");
                whatsNewFileHtml.println("<hr/>");
            }

            // process ELS Hints locally, targets and no subscriber
            if (context.hintsHandler != null && context.cfg.isTargetsEnabled() && !context.cfg.isRemoteOperation() &&
                    (context.cfg.getPublisherLibrariesFileName().length() > 0 ||
                            context.cfg.getPublisherCollectionFilename().length() > 0) &&
                    (context.cfg.getSubscriberLibrariesFileName().length() == 0 &&
                            context.cfg.getSubscriberCollectionFilename().length() == 0))
            {
                localHints = true; // skip munge
                result = context.hintsHandler.hintsMunge(true, null, null);
            }

            // process -e export text, publisher only
            if (context.cfg.getExportTextFilename().length() > 0)
            {
                exportText();
            }

            // process -i export collection items, publisher only
            if (context.cfg.getExportCollectionFilename().length() > 0)
            {
                exportCollection();
            }

            // check for publisher duplicates
            if (context.cfg.isDuplicateCheck() || context.cfg.isEmptyDirectoryCheck())
            {
                duplicatesCheck();
            }

            // process ELS Hints to subscriber
            if (context.cfg.isHintTrackingEnabled() && context.cfg.isTargetsEnabled() &&
                    context.cfg.getPublisherFilename().length() > 0 && context.cfg.getSubscriberFilename().length() > 0)
            {
                result = context.hintsHandler.hintsMunge(false, mismatchesFile, mismatchesFileHtml);
            }

            if (!result.toLowerCase().equals("fault"))
            {
                // if all the pieces are specified perform a full munge of the collections
                if (!context.fault && !localHints && !context.cfg.isHintSkipMainProcess())
                {
                    if (context.cfg.isTargetsEnabled() && context.cfg.getPublisherFilename().length() > 0 && context.cfg.getSubscriberFilename().length() > 0)
                    {
                        munge();
                    }
                    else
                    {
                        if (!context.cfg.isDuplicateCheck() && !context.cfg.isEmptyDirectoryCheck() && !context.cfg.isValidation() &&
                                !context.cfg.getPublisherFilename().isEmpty() && !context.cfg.getSubscriberFilename().isEmpty())
                            logger.warn(context.cfg.gs("Process.something.missing.make.sure"));
                    }
                }
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

                if (context.cfg.isHintSkipMainProcess() && !localHints)
                {
                    logger.info(context.cfg.gs("Process.skipping.main.process"));
                }
            }

            // Disconnect Subscriber
            if (context.clientStty != null)
            {
                try
                {
                    if (!context.cfg.isKeepGoing())
                        context.clientStty.send("quit", context.cfg.gs("Process.sending.quit.command.to.remote.subscriber"));
                    else
                        context.clientStty.send("bye", context.cfg.gs("Process.sending.bye.command.to.remote.subscriber"));
                    Thread.sleep(1500);
                }
                catch (Exception e)
                {
                    //
                }
            }

            // Disconnect Hint Server
            if (context.hintsStty != null)
            {
                try
                {
                    if (context.cfg.isQuitStatusServer())
                        context.hintsStty.send("quit", context.cfg.gs("Process.sending.quit.command.to.remote.hint.status.server"));
                    else
                        context.hintsStty.send("bye", context.cfg.gs("Process.sending.bye.command.to.remote.hint.status.server"));
                    Thread.sleep(1500);
                    context.hintsStty.disconnect();
                    context.hintsStty = null;
                }
                catch (Exception e)
                {
                    //
                }
            }
        }

        context.fault = fault;

        if (!fault &&
                (context.transfer.getGrandTotalItems() > 0 || context.hintsHandler.getPerformed() > 0) &&
                (mismatchesFile != null || whatsNewFile != null))
        {
            sendCompletionEmail();
        }
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
        for (Item dupe : item.getHas())
        {
            if (!dupe.isReported())
            {
                if (duplicates == 0)
                {
                    if (context.cfg.isDuplicateCheck())
                    {
                        logger.debug(SIMPLE, "+------------------------------------------");
                        logger.debug(SIMPLE, type + context.cfg.gs("Process.duplicate.filenames.found"));
                    }
                }
                ++duplicates;
                if (context.cfg.isDuplicateCheck())
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
        if (empties == 0)
        {
            if (context.cfg.isEmptyDirectoryCheck())
            {
                logger.debug(SIMPLE, "+------------------------------------------");
                logger.debug(SIMPLE, type + context.cfg.gs("Process.empty.directories.found"));
            }
        }
        ++empties;
        if (context.cfg.isEmptyDirectoryCheck())
            logger.debug(SIMPLE, "  " + item.getFullPath());
        return empties;
    }

    public void sendCompletionEmail()
    {
        String toolName = "";
        if (context.cfg.getEmailServer().length() > 0)
            toolName = context.cfg.getEmailServer();
        //else
            //toolName = context.preferences.getDefaultEmailServer();

        if  (toolName == null || toolName.length() == 0)
        {
            String msg = context.cfg.gs(context.cfg.gs(context.cfg.gs("Process.cannot.send.completion.email")));
            logger.warn(msg);
            //if ((context.navigator != null || context.cfg.isLoggerView()) && context.preferences.isAskSendEmail())
            //    JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            return;
        }

        Tools emailTools = new Tools();
        try
        {
            emailTools.loadAllTools(context, EmailTool.INTERNAL_NAME);
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            return;
        }

        EmailTool tool = (EmailTool) emailTools.getTool(EmailTool.INTERNAL_NAME, toolName);
        if (tool != null)
        {
            if ((context.navigator != null || context.cfg.isLoggerView()) && context.preferences.isAskSendEmail())
            {
                int opt = JOptionPane.showConfirmDialog(context.mainFrame,
                        context.cfg.gs("Process.back.up.completed.successfully.send.emails"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (opt != JOptionPane.YES_OPTION)
                {
                    return;
                }
            }

            EmailHandler emailHandler = new EmailHandler(context, null, tool, EmailHandler.Function.SEND);
            emailHandler.start();

            try
            {
                while (emailHandler.isAlive())
                {
                    Thread.sleep(500);
                }
            }
            catch (InterruptedException ie)
            {
            }
        }
        else
        {
            String msg = context.cfg.gs("Process.email.tool.not.found" + toolName);
            logger.error(msg);
            if ((context.navigator != null || context.cfg.isLoggerView()) && context.preferences.isAskSendEmail())
                JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }


    /**
     * Dump the list of ignored files
     */
    private void reportIgnored()
    {
        if (context.cfg.isIgnoredReported())
        {
            if (ignoredList.size() > 0)
            {
                logger.debug(SHORT, "+------------------------------------------");
                logger.debug(SIMPLE, MessageFormat.format(context.cfg.gs("Process.ignored.files.fin"), ignoredList.size()));
                for (String s : ignoredList)
                {
                    logger.debug(SIMPLE, "    " + s);
                }
            }
        }
    }

    public void setWarnings(int warnings)
    {
        this.warnings = warnings;
    }

} // Process
