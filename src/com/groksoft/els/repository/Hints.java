package com.groksoft.els.repository;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Hints class to handle writing, updating, finding and executing ELS Hints and their commands.
 */
public class Hints
{
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Context context;
    private int deletedHints = 0;
    private int doneHints = 0;
    private int executedHints = 0;
    private String hintItemPath = "";
    private String hintItemSubdirectory = "";
    private HintKeys keys;
    private int seenHints = 0;
    private int skippedHints = 0;
    private int validatedHints = 0;
    private final transient Logger logger = LogManager.getLogger("applog");

    /**
     * Constructor
     *
     * @param context  The Context
     * @param hintKeys HintKeys if enabled, else null
     */
    public Hints(Context context, HintKeys hintKeys)
    {
        this.context = context;
        keys = hintKeys;
    }

    /**
     * Dump the statistics of an ELS Hints runs
     */
    private void dumpStats()
    {
        logger.info(SHORT, "+------------------------------------------");
        if (validatedHints > 0)
        {
            logger.info(SHORT, "# Validated hints    : " + validatedHints + " (--dry-run)");
        }
        else
        {
            logger.info(SHORT, "# Executed hints     : " + executedHints + (context.cfg.isDryRun() ? " (--dry-run)" : ""));
            logger.info(SHORT, "# Seen Hints         : " + seenHints);
            logger.info(SHORT, "# Done hints         : " + doneHints);
            logger.info(SHORT, "# Deleted hints      : " + deletedHints);
            logger.info(SHORT, "# Skipped hints      : " + skippedHints);
            logger.info(SHORT, "# Moved directories  : " + context.transfer.getMovedDirectories());
            logger.info(SHORT, "# Moved files        : " + context.transfer.getMovedFiles());
            logger.info(SHORT, "# Removed directories: " + context.transfer.getRemovedDirectories());
            logger.info(SHORT, "# Removed files      : " + context.transfer.getRemovedFiles());
            logger.info(SHORT, "# Skipped missing    : " + context.transfer.getSkippedMissing());
        }
        if (!context.cfg.isHintSkipMainProcess())
            logger.info(SHORT, "-------------------------------------------");
    }

    /**
     * Execute the commands of an ELS Hint
     * <br/>
     * Paths in a hint are normalized for the repository flavor when executed.
     * So the paths may "look wrong" in the file.
     *
     * @param repo  The Repository where the hint is executing
     * @param item  The hint Item
     * @param lines The lines that have been read from the hints file
     * @return true if the item's library was altered
     * @throws Exception
     */
    private boolean execute(Repository repo, Item item, List<String> lines) throws Exception
    {
        HintKeys.HintKey hintKey;
        boolean libAltered = false;
        String statusLine;

        logger.info("* Executing " + item.getFullPath() + " on " + repo.getLibraryData().libraries.description);

        // find the ELS key for this repo
        hintKey = findHintKey(repo);

        hintItemSubdirectory = item.getItemSubdirectory();

        // find the actor name in the .els file
        statusLine = findNameLine(lines, hintKey.name);
        if (statusLine == null || !statusLine.toLowerCase().startsWith("for "))
        {
            logger.info("  Skipping execution, not For " + hintKey.name);
            ++skippedHints;
            return libAltered;
        }

        // process the command(s)
        int lineNo = 0;
        for (String line : lines)
        {
            ++lineNo;
            String lowLine = line.toLowerCase();

            // skip blank lines
            if (line.length() < 1)
                continue;

            // log & skip comments
            if (line.startsWith("#"))
            {
                logger.info("  " + line);
                continue;
            }

            if (lowLine.startsWith("for ") || lowLine.startsWith("done ") || lowLine.startsWith("seen ") || lowLine.startsWith("deleted "))
            {
                continue;
            }

            // mv move
            if (lowLine.startsWith("mv "))
            {
                String[] parts = parseCommand(line, lineNo, 3); // null never returned
                //dumpTerms(parts);

                String fromLib = parseLibrary(parts[1], lineNo);
                if (fromLib == null)
                    fromLib = item.getLibrary(); // use the library of the .els item

                String fromName = parseFilename(parts[1], lineNo);
                if (fromName.length() < 1)
                    throw new MungeException("Malformed from filename on line " + lineNo);
                if (hintItemSubdirectory != null && Utils.isFileOnly(fromName))
                {
                    hintItemSubdirectory = Utils.pipe(hintItemSubdirectory); // clean-up any mixed path separators
                    hintItemSubdirectory = Utils.unpipe(hintItemSubdirectory, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));
                    fromName = hintItemSubdirectory + "|" + fromName;
                }
                fromName = Utils.pipe(fromName); // clean-up any mixed path separators
                fromName = Utils.unpipe(fromName, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));

                String toLib = parseLibrary(parts[2], lineNo);
                if (toLib == null)
                    toLib = item.getLibrary(); // use the library of the .els item

                String toName = parseFilename(parts[2], lineNo);
                if (toName.length() < 1)
                    throw new MungeException("Malformed to filename on line " + lineNo);
                if (hintItemSubdirectory != null && Utils.isFileOnly(toName))
                {
                    hintItemSubdirectory = Utils.pipe(hintItemSubdirectory); // clean-up any mixed path separators
                    hintItemSubdirectory = Utils.unpipe(hintItemSubdirectory, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));
                    toName = hintItemSubdirectory + "|" + toName;
                }
                toName = Utils.pipe(toName); // clean-up any mixed path separators
                toName = Utils.unpipe(toName, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));

                context.localMode = true;
                if (context.transfer.move(repo, fromLib.trim(), fromName.trim(), toLib.trim(), toName.trim()))
                    libAltered = true;
                context.localMode = false;

                item.setHintExecuted(true);
            }
            else if (lowLine.startsWith("rm ")) // rm remove
            {
                String[] parts = parseCommand(line, lineNo, 2); // null never returned
                //dumpTerms(parts);

                String fromLib = parseLibrary(parts[1], lineNo);
                if (fromLib == null)
                    fromLib = item.getLibrary(); // use the library of the .els item

                String fromName = parseFilename(parts[1], lineNo);
                if (fromName.length() < 1)
                    throw new MungeException("Malformed from filename on line " + lineNo);
                fromName = Utils.pipe(fromName); // clean-up any mixed path separators
                fromName = Utils.unpipe(fromName, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));

                if (hintItemSubdirectory != null && Utils.isFileOnly(fromName))
                {
                    hintItemSubdirectory = Utils.pipe(hintItemSubdirectory); // clean-up any mixed path separators
                    hintItemSubdirectory = Utils.unpipe(hintItemSubdirectory, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));
                    fromName = hintItemSubdirectory + "|" + fromName;
                }

                if (context.transfer.remove(repo, fromLib.trim(), fromName.trim()))
                    libAltered = true;

                item.setHintExecuted(true);
            }
        }

        // update hint status
        if (item.isHintExecuted())
        {
            if (!context.cfg.isDryRun())
            {
                updateStatus(item, lines, hintKey.name, "Done");
            }
            ++executedHints;
        }

        return libAltered;
    }

    /**
     * Find the hint UUID key for a Repository
     *
     * @param repo Repository containing the UUID key to find
     * @return Hints.Hintkey of matching UUID
     * @throws Exception if not found
     */
    private HintKeys.HintKey findHintKey(Repository repo) throws Exception
    {
        // find the ELS key for this repo
        HintKeys.HintKey hintKey = keys.findKey(repo.getLibraryData().libraries.key);
        if (hintKey == null)
        {
            throw new MungeException("Repository not found in ELS keys " + keys.getFilename() + " matching key in " + repo.getLibraryData().libraries.description);
        }
        return hintKey;
    }

    /**
     * Find the line in a hint .els file the matching the name
     *
     * @param lines Lines of the hint .els file
     * @param name  Name line to find
     * @return String of matching name line or null if not found
     */
    private String findNameLine(List<String> lines, String name)
    {
        for (String line : lines)
        {
            String parts[] = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                if (word.equals("for") || word.equals("done") || word.equals("seen") || word.equals("deleted"))
                {
                    if (parts[1].equalsIgnoreCase(name))
                        return line.trim();
                }
            }
        }
        return null;
    }

    /**
     * Get the target location to place a incoming hint file
     *
     * @param item Item to place
     * @return String path of appropriate location
     * @throws Exception
     */
    private String getHintTarget(Item item) throws Exception
    {
        String target = null;

        // does the target already have this hint?
        Item entry = context.subscriberRepo.hasItem(item, item.getLibrary(), Utils.pipe(context.publisherRepo, item.getItemPath()));
        if (entry != null)
        {
            target = entry.getFullPath(); // yes, overwrite it
        }
        else
        {
            // does the target have this directory?
            String path = context.subscriberRepo.hasDirectory(item.getLibrary(), Utils.pipe(context.publisherRepo, item.getItemPath()));
            if (path != null)
            {
                target = path + Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor) + item.getItemPath(); // yes, use it
            }

            if (target == null)
            {
                Library lib = context.subscriberRepo.getLibrary(item.getLibrary());
                if (lib != null)
                {
                    String candidate;
                    for (int j = 0; j < lib.sources.length; ++j)
                    {
                        candidate = lib.sources[j];
                        // check size of item(s) to be copied
                        long space = context.transfer.getFreespace(candidate, context.cfg.isRemoteSession());
                        if (space > (item.getSize() + (1024 * 1024 * 10)))
                        {
                            target = candidate + Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor) + item.getItemPath();
                            break;
                        }
                    }
                }
            }
        }

        if (target == null)
        {
            // subscriber does not have the item or directory??? Must be a new item???
            throw new MungeException("Target for ELS hint file cannot be found: " + item.getFullPath());
        }

        target = Utils.pipe(target); // clean-up any mixed path separators
        target = Utils.unpipe(target, Utils.getFileSeparator(context.subscriberRepo.getLibraryData().libraries.flavor));
        return target;
    }

    /**
     * Get the String matching a given status rank
     *
     * @param rank Rank to match
     * @return String of integer rank
     */
    private String getStatusString(int rank)
    {
        return ((rank == 0) ? "Unknown" : ((rank == 1) ? "For" : ((rank == 2) ? "Done" : (rank == 3 ? "Seen" : "Deleted"))));
    }

    /**
     * Run a hint on a subscriber after merging status with an incoming .merge file.
     * <p>
     * Used by the subscriber/Daemon when the command is sent by the publisher
     * after the hint file has been copied to the subscriber.
     *
     * @param libName  Library name of incoming item
     * @param itemPath ItemPath of incoming item
     * @param toPath   Path of hint
     * @return true if hint was executed
     * @throws Exception
     */
    public boolean hintRun(String libName, String itemPath, String toPath) throws Exception
    {
        boolean sense = false;
        Item toItem = null;

        File toFile = new File(toPath + ".merge");
        if (toFile.exists())
        {
            Item existingItem = context.subscriberRepo.hasItem(null, libName, itemPath);
            if (existingItem != null)
            {
                toItem = SerializationUtils.clone(existingItem);
            }
        }

        // merge, might create the file
        mergeHints(toPath + ".merge", toPath);

        // execute
        if (toItem == null)
        {
            toItem = new Item();
            toItem.setDirectory(false);
            toItem.setItemPath(itemPath);
            toItem.setSymLink(false);
            toItem.setLibrary(libName);
        }
        Path entry = Paths.get(toPath);
        toItem.setSize(Files.size(entry));
        toItem.setFullPath(toPath);
        if (!Utils.isFileOnly(toItem.getItemPath()))
        {
            toItem.setItemSubdirectory(Utils.pipe(context.subscriberRepo, Utils.getLeftPath(toItem.getItemPath(), context.subscriberRepo.getSeparator())));
        }

        List<String> lines = readHintUpdated(toItem);
        execute(context.subscriberRepo, toItem, lines);
        if (toItem.isHintExecuted())
            sense = true;

        postprocessHintFile(context.subscriberRepo, toItem);

        return sense;
    }

    /**
     * Run all the local hints on the publisher.
     * <p>
     * If not done manually and the publisher's hint status set to Done, a hint
     * must be executed on the publisher before a operationsUI operationsUI to a subscriber
     * so the two ends match.
     *
     * @throws Exception
     */
    public void hintsLocal() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Processing local ELS Hints for " + context.publisherRepo.getLibraryData().libraries.description + (context.cfg.isDryRun() ? " (--dry-run)" : ""));

        for (Library lib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                    (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
            {
                // does the library have items?
                if (lib.items == null || lib.items.size() < 1)
                {
                    logger.info("  ! " + lib.name + " is empty, skipping");
                    continue;
                }

                for (Item item : lib.items)
                {
                    // only ELS Hints that have not been executed already
                    // the hintExecuted boolean is runtime only (transient)
                    if (!item.getItemPath().toLowerCase().endsWith(".els") || item.isHintExecuted())
                    {
                        continue;
                    }

                    // check if it needs to be done locally
                    hintsFound = true;
                    List<String> lines = readHintUpdated(item);
                    boolean libAltered = execute(context.publisherRepo, item, lines);
                    lib.rescanNeeded = true;

                    postprocessHintUpdated(context.subscriberRepo, item);
                }
            }
        }

        if (hintsFound)
        {
            dumpStats();

            if (!context.cfg.isDryRun())
            {
                for (Library lib : context.publisherRepo.getLibraryData().libraries.bibliography)
                {
                    // if processing all libraries, or this one was specified on the command line with -l,
                    // and it has not been excluded with -L
                    if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                            (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
                    {
                        if (lib.rescanNeeded)
                        {
                            context.publisherRepo.scan(lib.name);
                        }
                    }
                }
            }
        }
        else
        {
            logger.info("  - No .els hint files found");
        }
    }

    /**
     * Copy each publisher hint to a subscriber and have it execute the hint.
     * <p>
     * Copies individual hints from the publisher to the subscriber, either
     * locally or remotely, then commands the subscriber to execute the hint
     * using hintrun().
     *
     * @throws Exception
     */
    public void hintsMunge() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Munging ELS Hints from " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description + (context.cfg.isDryRun() ? " (--dry-run)" : ""));

        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            Library pubLib = null;
            Item toItem;

            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(subLib.name)) &&
                    (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(subLib.name)))
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
                    if (pubLib.items == null || pubLib.items.size() < 1)
                    {
                        context.publisherRepo.scan(pubLib.name);
                    }

                    // iterate the publisher's items
                    for (Item item : pubLib.items)
                    {
                        // only ELS Hints
                        if (!item.getItemPath().toLowerCase().endsWith(".els"))
                        {
                            continue;
                        }

                        hintsFound = true;

                        // do the libraries have items or do they need to be scanned?
                        if (subLib.items == null || subLib.items.size() < 1)
                        {
                            if (!context.cfg.isRemoteSession()) // remote collection already loaded and may be empty
                            {
                                context.subscriberRepo.scan(subLib.name);
                            }
                        }

                        // Hints are intended to be copied and processed immediately
                        // So the hint cannot be copied during a --dry-run
                        // Validate the syntax instead
                        if (context.cfg.isDryRun())
                        {
                            logger.info("* Validating syntax for dry run: " + item.getFullPath());
                            ++validatedHints;
                            List<String> lines = readHintUpdated(item);  // reads & validates hint
                        }
                        else
                        {
                            // read the ELS hint file
                            List<String> lines = readHint(item.getFullPath());

                            // check if the publisher has Done or Seen this hint
                            // this is important prior to a operationsUI run to avoid duplicates, etc.
                            HintKeys.HintKey hintKey = findHintKey(context.publisherRepo);
                            String statusLine = findNameLine(lines, hintKey.name);
                            if (statusLine == null || (!statusLine.toLowerCase().startsWith("done ") && !statusLine.toLowerCase().startsWith("seen ")))
                                throw new MungeException("Publisher must execute hints locally; Status has not Done or Seen hint: " + item.getFullPath());

                            if (statusLine.toLowerCase().startsWith("done "))
                                ++doneHints;
                            else if (statusLine.toLowerCase().startsWith("seen "))
                                ++seenHints;

                            // transfer the hint to the subscriber
                            String toPath = getHintTarget(item); // never null
                            String tmpPath = toPath + ".merge";
                            context.transfer.copyFile(item.getFullPath(), item.getModifiedDate(), tmpPath, context.cfg.isRemoteSession(), true);

                            toItem = SerializationUtils.clone(item);
                            toItem.setFullPath(toPath);
                            if (!Utils.isFileOnly(toItem.getItemPath()))
                            {
                                toItem.setItemSubdirectory(Utils.pipe(context.subscriberRepo, Utils.getLeftPath(toItem.getItemPath(), context.subscriberRepo.getSeparator())));
                            }

                            boolean updatePubSide = false;
                            if (context.cfg.isRemoteSession())
                            {
                                if (context.clientStty.isConnected())
                                {
                                    // Send command to merge & execute
                                    String msg = "* Executing " + item.getFullPath() + " remotely on " + context.subscriberRepo.getLibraryData().libraries.description;
                                    String response = context.clientStty.roundTrip("execute \"" +
                                            toItem.getLibrary() + "\" \"" + toItem.getItemPath() + "\" \"" + toPath + "\"", msg, -1);
                                    if (response != null && response.length() > 0)
                                    {
                                        logger.info("  > execute command returned: " + response);
                                        updatePubSide = response.equalsIgnoreCase("true") ? true : false;
                                        if (updatePubSide)
                                        {
                                            subLib.rescanNeeded = true;
                                        }
                                    }
                                    else
                                        throw new MungeException(context.cfg.gs("Z.subscriber.disconnected"));
                                }
                                else
                                    throw new MungeException(context.cfg.gs("Z.subscriber.disconnected"));
                            }
                            else
                            {
                                // merge
                                mergeHints(tmpPath, toPath);

                                // execute
                                lines = readHintUpdated(toItem);
                                execute(context.subscriberRepo, toItem, lines);
                                if (toItem.isHintExecuted())
                                {
                                    subLib.rescanNeeded = true;
                                    updatePubSide = true;
                                }
                            }

                            updateSubscriberOnPublisher(item);

                            if (!context.cfg.isRemoteSession()) // subscriber-side does this itself
                            {
                                postprocessHintFile(context.subscriberRepo, toItem);
                            }
                            postprocessHintUpdated(context.publisherRepo, item);
                        }
                    }
                }
                else
                {
                    throw new MungeException("Subscribed Publisher library " + subLib.name + " not found");
                }
            }
            else
            {
                logger.info("Skipping publisher library: " + subLib.name);
            }
        }

        if (hintsFound)
        {
            dumpStats();

            if (!context.cfg.isDryRun())
            {
                boolean request = false;
                for (Library lib : context.subscriberRepo.getLibraryData().libraries.bibliography)
                {
                    // if processing all libraries, or this one was specified on the command line with -l,
                    // and it has not been excluded with -L
                    if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                            (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
                    {
                        if (lib.rescanNeeded)
                        {
                            if (context.cfg.isRemoteSession())
                            {
                                request = true;
                                break;
                            }
                            else
                            {
                                context.subscriberRepo.scan(lib.name);
                            }
                        }
                    }
                }
                // request updated remote subscriber collection
                if (request)
                {
                    context.transfer.requestCollection();
                }
            }
        }
        else
        {
            logger.info("  - No .els hint files found");
        }
    }

    /**
     * Clean-up hint status and files on subscriber.
     * <p>
     * Checks each hint file on a subscriber to promote the status
     * from Done to Seen and potentially deletes the hint file if
     * the status allows it - for automatic hint maintenance.
     *
     * @throws Exception
     */
    public void hintsSubscriberCleanup() throws Exception
    {
        if (context.cfg.isRemoteSession() && !context.localMode)
        {
            if (context.clientStty.isConnected())
            {
                // Send command to clean-up hints
                String msg = "Sending hints cleanup command to remote: " + context.subscriberRepo.getLibraryData().libraries.description;
                String response = context.clientStty.roundTrip("cleanup", msg, -1);
                if (response != null && response.length() > 0)
                {
                    logger.debug("  > cleanup command returned: " + response);
                }
                else
                    throw new MungeException(context.cfg.gs("Z.subscriber.disconnected"));
            }
            else
                throw new MungeException(context.cfg.gs("Z.subscriber.disconnected"));
        }
        else
        {
            subscriberCleanup();
        }
    }

    /**
     * Merge an incoming hint file with any existing hint file.
     * <p>
     * Merges the completion status of each back-up in an existing
     * hint file with that of a hint file coming from the publisher.
     * The highest completion status is used. If no hint file exist
     * a new file is created.
     *
     * @param mergePath The path of the incoming .els.merge file
     * @param toPath    The resulting path of the merged hint
     * @throws Exception
     */
    private void mergeHints(String mergePath, String toPath) throws Exception
    {
        File toFile = new File(toPath);
        if (!toFile.exists())
        {
            File mergeFile = new File(mergePath);
            mergeFile.renameTo(toFile);
            return;
        }

        List<String> mergeLines = readHint(mergePath);
        List<String> toLines = readHint(toPath);
        boolean changed = false;

        if (mergeLines.size() != toLines.size())
            throw new MungeException("Hint merge files are not the same number of lines, merge: " + mergePath + ", to hint: " + toPath);

        for (int i = 0; i < mergeLines.size(); ++i)
        {
            String mergeline = mergeLines.get(i);
            String copy = mergeline.toLowerCase();
            if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen ") || copy.startsWith("deleted "))
            {
                String[] mergeParts = parseNameLine(mergeline, i);
                String mergeStatus = mergeParts[0];
                String mergeName = mergeParts[1];
                int mergeRank = statusToRank(mergeStatus);

                for (int j = 0; j < toLines.size(); ++j)
                {
                    String existing = toLines.get(j);
                    copy = existing.toLowerCase();
                    if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen ") || copy.startsWith("deleted "))
                    {
                        String[] existingParts = parseNameLine(existing, j);
                        String existingStatus = existingParts[0];
                        String existingName = existingParts[1];

                        if (mergeName.equalsIgnoreCase(existingName))
                        {
                            int existingRank = statusToRank(existingStatus);
                            if (mergeRank > existingRank)
                            {
                                mergeStatus = getStatusString(mergeRank);
                                mergeline = mergeStatus + " " + mergeName;
                                mergeLines.set(i, mergeline);
                                changed = true;
                            }
                            break;
                        }
                    }
                }
            }
        }

        // update toPath with content from mergePath
        if (changed)
            Files.write(Paths.get(toPath), mergeLines, StandardOpenOption.CREATE);

        // delete mergePath file
        File mergeFile = new File(mergePath);
        mergeFile.delete();
    }

    /**
     * Merge values from the ELS Hint Tracker with those in a hint file.
     * <p>
     * If hint tracking is being used, where a --hint-server is defined
     * for either a local or remote operationsUI, then it's status values are
     * merged with the hint file.
     *
     * @param item  Item of the hint file
     * @param lines Lines of the hint file
     * @return Merged lines of the hint file
     * @throws Exception
     */
    private List<String> mergeStatusServer(Item item, List<String> lines) throws Exception
    {
        // is a hint status server being used?
        if (context.cfg.isUsingHintTracking())
        {
            boolean changed = false;
            for (int i = 0; i < lines.size(); ++i)
            {
                String existing = lines.get(i);
                String copy = existing.toLowerCase();
                if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen ") || copy.startsWith("deleted "))
                {
                    String[] existingParts = parseNameLine(existing, i);
                    String existingStatus = existingParts[0];
                    String existingName = existingParts[1];
                    int existingRank = statusToRank(existingStatus);
                    int mergeRank;

                    if (context.statusStty != null)
                    {
                        if (context.statusStty.isConnected())
                        {
                            // get the status from the status server
                            String command = "get \"" + item.getLibrary() + "\" " +
                                    "\"" + item.getItemPath() + "\" " +
                                    "\"" + existingName + "\" " +
                                    "\"" + existingStatus + "\"";

                            String response = context.statusStty.roundTrip(command, "", 10000);
                            if (response != null && !response.equalsIgnoreCase("false"))
                            {
                                mergeRank = statusToRank(response);
                            }
                            else
                                throw new MungeException("Status Server " + context.statusRepo.getLibraryData().libraries.description +
                                        " failure during get, line: " + i + " in: " + item.getFullPath());
                        }
                        else
                            throw new MungeException("Status Server " + context.statusRepo.getLibraryData().libraries.description +
                                    " failure during get, line: " + i + " in: " + item.getFullPath());
                    }
                    else
                    {
                        String mergeStatus = context.datastore.getStatus(item.getLibrary(), item.getItemPath(), existingName, existingStatus);
                        mergeRank = statusToRank(mergeStatus);
                    }

                    if (mergeRank > existingRank)
                    {
                        String mergeStatus = getStatusString(mergeRank);
                        lines.set(i, mergeStatus + " " + existingName);
                        changed = true;
                    }
                }
            }

            if (changed)
                Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
        }
        return lines;
    }

    /**
     * Parse a hint file command line
     *
     * @param line     Line to parse
     * @param lineNo   The number of the line in the file
     * @param expected The number of expected values
     * @return String[] of arguments
     * @throws Exception
     */
    private String[] parseCommand(String line, int lineNo, int expected) throws Exception
    {
        int MAX_TERMS = 4;
        String[] cmd = new String[MAX_TERMS]; // maximum number of terms in any command
        StringTokenizer t = new StringTokenizer(line, "'\"");
        if (!t.hasMoreTokens())
            return null;

        int i = 0;
        while (t.hasMoreTokens())
        {
            String term = t.nextToken().trim();
            if (term.length() > 0)
            {
                cmd[i++] = term;
                if (i >= MAX_TERMS)
                    throw new MungeException("Too many terms");
            }
        }
        if (i != expected)
            throw new MungeException("Malformed command, " + i + " is wrong number of terms, expecting " + expected + " for line " + lineNo);
        return cmd;
    }

    /**
     * Parse the right-side filename portion of a hint command line
     *
     * @param line   The line to parse
     * @param lineNo The number of the line in the file
     * @return String the parsed filename or null
     * @throws MungeException
     */
    private String parseFilename(String line, int lineNo) throws MungeException
    {
        String name = null;
        String[] parts = line.split("\\|");
        if (parts.length > 2)
            throw new MungeException("Malformed library|file term on line " + lineNo + ": " + line);
        if (parts.length == 1)
            name = parts[0];
        else if (parts.length == 2)
            name = parts[1];
        return name;
    }

    /**
     * Parse the left-side library name portion of a hint command line
     *
     * @param line   The line to parse
     * @param lineNo The number of the line in the file
     * @return String the parsed library or null
     * @throws MungeException
     */
    private String parseLibrary(String line, int lineNo) throws MungeException
    {
        String lib = null;
        String[] parts = line.split("\\|");
        if (parts.length > 2)
            throw new MungeException("Malformed library|file term on line " + lineNo + ": " + line);
        if (parts.length == 2)
            lib = parts[0];
        return lib;
    }

    /**
     * Parse a hint file name/status line
     *
     * @param line   The line to parse
     * @param lineNo The number of the line in the file
     * @return String[2] of back-up name and status
     * @throws Exception
     */
    private String[] parseNameLine(String line, int lineNo) throws Exception
    {
        String[] cmd = new String[2];
        StringTokenizer t = new StringTokenizer(line, " ");
        if (!t.hasMoreTokens())
            return null;

        int i = 0;
        while (t.hasMoreTokens())
        {
            String term = t.nextToken().trim();
            if (term.length() > 0)
            {
                cmd[i++] = term;
                if (i > 2)
                    throw new MungeException("Too many terms");
            }
        }
        return cmd;
    }

    /**
     * Post-process a hint's status.
     * <p>
     * Scans the lines of a hint promoting status as the hint goes through
     * the steps of For, Done, Seen then Deleted. When all back-up's status
     * is either Seen or Deleted the hint file is deleted for automatic
     * hint maintenance.
     *
     * @param repo  The Repository containing the hint file
     * @param item  The Item of the hint file
     * @param lines The lines of the hint file
     * @return String the current/updated status of this hint
     * @throws Exception
     */
    private String postprocessHint(Repository repo, Item item, List<String> lines) throws Exception
    {
        int doneWords = 0;
        int forWords = 0;
        int deletedWords = 0;
        String pubStat = "";
        int seenWords = 0;
        String subStat = "";
        int totalWords = 0;
        String currentStat = "";
        boolean isPub = false;

        // find the ELS keys
        HintKeys.HintKey pubHintKey = findHintKey(context.publisherRepo);
        HintKeys.HintKey subHintKey = findHintKey(context.subscriberRepo);
        HintKeys.HintKey itemHintKey;
        if (repo == context.publisherRepo)
        {
            itemHintKey = pubHintKey;
            isPub = true;
        }
        else if (repo == context.subscriberRepo)
            itemHintKey = subHintKey;
        else
            throw new MungeException("Unknown repo: " + repo.getLibraryData().libraries.description);

        // scan the lines adding-up status values
        for (String line : lines)
        {
            String parts[] = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                String name = parts[1];
                if (name.equalsIgnoreCase(pubHintKey.name))
                {
                    pubStat = word;
                    if (isPub)
                        currentStat = word;
                }
                if (name.equalsIgnoreCase(subHintKey.name))
                {
                    subStat = word;
                    if (!isPub)
                        currentStat = word;
                }
                if (word.equals("for") || word.equals("done") || word.equals("seen") || word.equals("deleted"))
                {
                    ++totalWords;
                    switch (word)
                    {
                        case "for":
                            ++forWords;
                            break;
                        case "done":
                            ++doneWords;
                            break;
                        case "seen":
                            ++seenWords;
                            break;
                        case "deleted":
                            ++deletedWords;
                            break;
                    }
                }
            }
        }

        if (forWords == 0) // if it is still "For" any back-up don't do anything
        {
            if (doneWords > 0) // if some for "Done" ...
            {
                if (pubStat.equals("done")) // if the publisher is "Done" promote to "Seen"
                {
                    updateStatus(item, lines, pubHintKey.name, "Seen");
                    if (isPub)
                        currentStat = "Seen";
                    --doneWords;
                    ++seenWords;
                }

                // if the subscriber is "Done" promote to "Seen"
                // skip if this is hintsLocal() where they are the same Repository
                if (context.publisherRepo != context.subscriberRepo)
                {
                    if (subStat.equals("done"))
                    {
                        updateStatus(item, lines, subHintKey.name, "Seen");
                        if (!isPub)
                            currentStat = "Seen";
                        --doneWords;
                        ++seenWords;
                    }
                }
            }

            // if all the back-ups have either "Seen" or "Deleted" the hint then
            // all back-ups have "Done" it so delete the hint file
            if (seenWords + deletedWords == totalWords)
            {
                File prevFile = new File(item.getFullPath());
                if (prevFile.exists())
                {
                    if (context.cfg.isDryRun())
                    {
                        logger.info("  > Hint done and seen, would delete hint file: " + item.getFullPath());
                    }
                    else
                    {
                        if (prevFile.delete())
                        {
                            logger.info("  > Hint done and seen, deleted hint file: " + item.getFullPath());
                            ++deletedHints;
                            currentStat = "Deleted";
                            updateStatusTracking(item, itemHintKey.name, "Deleted");
                            repo.getLibrary(item.getLibrary()).rescanNeeded = true;
                        }
                    }
                }
            }
        }
        return currentStat;
    }

    /**
     * Post-process a hint file.
     * <p>
     * If a hint file still exists use postprocessHint() to update
     * any appropriate status values.
     *
     * @param repo The Repository of the hint
     * @param item The Item of the hint
     * @return String the current/updated status of this hint
     * @throws Exception
     */
    private String postprocessHintFile(Repository repo, Item item) throws Exception
    {
        // read the ELS hint file
        if (Files.notExists(Paths.get(item.getFullPath())))
            return "Deleted";
        List<String> lines = readHint(item.getFullPath()); // hint not validated
        return postprocessHint(repo, item, lines);
    }

    /**
     * Post-process a hint file updated from the Hint Tracker/Server.
     * <p>
     * If a hint file still exists updated it with values from the Hint Tracker
     * or Hint Server, if defined, then use postprocessHint() to update
     * any appropriate status values.
     *
     * @param repo The Repository of the hint
     * @param item The Item of the hint
     * @return String the current/updated status of this hint
     * @throws Exception
     */
    private String postprocessHintUpdated(Repository repo, Item item) throws Exception
    {
        // read the ELS hint file
        if (Files.notExists(Paths.get(item.getFullPath())))
            return "Deleted";
        List<String> lines = readHintUpdated(item);
        return postprocessHint(repo, item, lines);
    }

    /**
     * Read and cleanup lines from a hint file
     *
     * @param path Full path to hint file
     * @return String lines that have tabs replaced with a space and trimmed
     */
    private List<String> readHint(String path) throws Exception
    {
        List<String> lines = Files.readAllLines(Paths.get(path));

        // cleanup the lines
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            line = line.replaceAll("\\t", " ").trim();
            lines.set(i, line);
        }
        return lines;
    }

    /**
     * Read, cleanup, merge with status tracker/server & validate lines from a hint file
     *
     * @param item Item to be read
     * @return String lines
     * @throws Exception
     */
    private List<String> readHintUpdated(Item item) throws Exception
    {
        List<String> lines = readHint(item.getFullPath());
        lines = mergeStatusServer(item, lines);
        return validate(item.getFullPath(), lines);
    }

    public String reduceCollectionPath(NavTreeUserObject tuo)
    {
        String path = null;
        if (tuo.node.getMyTree().getName().contains("Collection"))
        {
            Repository repo = tuo.getRepo();
            if (repo != null)
            {
                String tuoPath = (repo.getLibraryData().libraries.case_sensitive) ? tuo.path : tuo.path.toLowerCase();
                if (tuoPath.length() == 0)
                {
                    path = tuo.name + " | ";
                }
                else
                {
                    for (Library lib : repo.getLibraryData().libraries.bibliography)
                    {
                        for (String source : lib.sources)
                        {
                            String srcPath = source;
                            if (!tuo.isRemote)
                            {
                                File srcDir = new File(source);
                                srcPath = srcDir.getAbsolutePath();
                            }
                            srcPath = (repo.getLibraryData().libraries.case_sensitive) ? srcPath : srcPath.toLowerCase();
                            if (tuoPath.startsWith(srcPath))
                            {
                                path = lib.name + " | " + tuo.path.substring(srcPath.length() + 1);
                                break;
                            }
                        }
                        if (path != null)
                            break;
                    }
                }
            }
        }
        if (path == null)
            path = tuo.path;
        return path;
    }

    /**
     * Return the numeric rank of a status String value
     *
     * @param status The value to rank
     * @return int The numeric rank of the status String, or 0 if no match
     */
    private int statusToRank(String status)
    {
        if (status.equalsIgnoreCase("for"))
            return 1;
        else if (status.equalsIgnoreCase("done"))
            return 2;
        else if (status.equalsIgnoreCase("seen"))
            return 3;
        else if (status.equalsIgnoreCase("deleted"))
            return 4;
        return 0;
    }

    /**
     * Clean-up a local subscriber's hint files.
     * <p>
     * Used at the end of an operationsUI either locally or by the
     * subscriber/Daemon when the command is received from the
     * publisher. Scans the subscriber for .els files then runs
     * postprocessHintFile() on each.
     *
     * @throws Exception
     */
    private void subscriberCleanup() throws Exception
    {
        logger.info("Cleaning-up ELS Hints on " + context.subscriberRepo.getLibraryData().libraries.description);

        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(subLib.name)) &&
                    (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(subLib.name)))
            {
                // if the subscriber has included and not excluded this library
                if (subLib.name.startsWith(context.subscriberRepo.SUB_EXCLUDE))
                {
                    String n = subLib.name.replaceFirst(context.subscriberRepo.SUB_EXCLUDE, "");
                    logger.info("Skipping subscriber library: " + n);
                    continue;
                }

                if (subLib.rescanNeeded || subLib.items == null)
                {
                    context.subscriberRepo.scan(subLib.name);
                }

                // iterate the subscriber's items
                if (!context.cfg.isDryRun())
                {
                    for (Item item : subLib.items)
                    {
                        // only ELS Hints
                        if (!item.getItemPath().toLowerCase().endsWith(".els"))
                        {
                            continue;
                        }
                        File itemFile = new File(item.getFullPath());
                        if (itemFile.exists())
                        {
                            List<String> lines = readHintUpdated(item); // merge with status server if in use
                            postprocessHintFile(context.subscriberRepo, item);
                        }
                    }
                }
            }
            else
            {
                logger.info("Skipping subscriber library: " + subLib.name);
            }
        }
    }

    /**
     * Update hint status for a specific back-up name.
     * <p>
     * Updates and saves the status for the operationsUI name in the hint
     * file and updates the hint status tracker/server if defined.
     * <p>
     * The higher value of either the existing or the new value is saved.
     *
     * @param item       The Item of the hint
     * @param lines      The lines of the hint
     * @param backupName The name of the back-up from the Hint Keys file
     * @param status     The new status for the hint
     * @return String of the value actually saved
     * @throws Exception
     */
    private String updateStatus(Item item, List<String> lines, String backupName, String status) throws Exception
    {
        boolean changed = false;
        int lineNo = 0;

        int mergeRank = statusToRank(status);
        for (String line : lines)
        {
            String[] parts = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                if (word.equals("for") || word.equals("done") || word.equals("seen") || word.equals("deleted"))
                {
                    if (parts[1].equalsIgnoreCase(backupName))
                    {
                        int existingRank = statusToRank(word);
                        if (mergeRank > existingRank)
                        {
                            line = status + " " + backupName;
                            lines.set(lineNo, line);
                            changed = true;
                            break;
                        }
                        else
                        {
                            status = word;
                            break;
                        }
                    }
                }
            }
            ++lineNo;
        }

        if (changed)
        {
            Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
            updateStatusTracking(item, backupName, status);
        }
        return status;
    }

    /**
     * Update the hint status tracker/server.
     * <p>
     * Used during postprocess.
     * <p>
     * If defined on the command line with -H | --hint-server the tracker
     * is updated either locally or remotely when the -r | --remote option
     * is used.
     * <p>
     * No further processing of the status is done by the tracker/server,
     * i.e. the status is not changed.
     *
     * @param item       The Item of the hint
     * @param backupName The name of the back-up from the ELS Hint Keys file
     * @param status     The desired status String
     * @throws Exception
     */
    private void updateStatusTracking(Item item, String backupName, String status) throws Exception
    {
        updateStatusTracking(item.getLibrary(), item.getItemPath(), backupName, status);
    }

    /**
     * Update the hint status tracker/server.
     * <p>
     * If defined on the command line with -H | --hint-server the tracker
     * is updated either locally or remotely when the -r | --remote option
     * is used.
     * <p>
     * No further processing of the status is done by the tracker/server,
     * i.e. the status is not changed.
     *
     * @param libraryName Name of library of Hint
     * @param itemPath    The ItemPath of the hint
     * @param backupName  The name of the back-up from the ELS Hint Keys file
     * @param status      The desired status String
     * @throws Exception
     */
    public void updateStatusTracking(String libraryName, String itemPath, String backupName, String status) throws Exception
    {
        if (context.statusStty != null) // is the Hint Status Server being used?
        {
            if (context.statusStty.isConnected())
            {
                // set the status on the status server
                String command = "set \"" + libraryName + "\" " +
                        "\"" + itemPath + "\" " +
                        "\"" + backupName + "\" " +
                        "\"" + status + "\"";

                String response = context.statusStty.roundTrip(command, "", 10000);
                if (response == null || !response.equalsIgnoreCase(status))
                    throw new MungeException("Status Server " + context.statusRepo.getLibraryData().libraries.description + " returned a failure during set");
            }
            else
                throw new MungeException(context.cfg.gs("Z.subscriber.disconnected"));
        }
        else // no, local
        {
            if (context.datastore != null) // is the Hint Tracker being used?
            {
                String result = context.datastore.setStatus(libraryName, itemPath, backupName, status);
                if (result == null || !result.equalsIgnoreCase(status))
                    throw new MungeException("Hint setStatus() for " + context.statusRepo.getLibraryData().libraries.description + " returned a failure during set");
            }
        }
    }

    /**
     * Update the subscriber's status in the publisher's hint file.
     * <p>
     * Merges any existing status with "Done". The highest value is saved.
     *
     * @param item The Item of the hint
     * @return Resulting String status
     * @throws Exception
     */
    private String updateSubscriberOnPublisher(Item item) throws Exception
    {
        String currentStat = "";
        List<String> lines = readHintUpdated(item);
        HintKeys.HintKey hintKey = findHintKey(context.subscriberRepo);
        String line = findNameLine(lines, hintKey.name);
        if (line != null)
        {
            String[] parts = line.split("[\\s]+"); // two parts guaranteed
            int rank = statusToRank(parts[0]);
            int toRank = statusToRank("Done");
            if (rank > toRank)
                toRank = rank;
            return updateStatus(item, lines, hintKey.name, getStatusString(toRank));
        }
        return currentStat;
    }

    /**
     * Validate the syntax of a hint file
     *
     * @param filename The file path of the hint file
     * @param lines    The lines of the hint file
     * @return The validated lines of the hint file
     * @throws Exception Any problem found throws an exception
     */
    private List<String> validate(String filename, List<String> lines) throws Exception
    {
        int lineNo = 0;
        String lowered;
        for (String line : lines)
        {
            ++lineNo;

            // skip blank lines
            if (line.length() < 1)
                continue;

            // skip comments
            if (line.startsWith("#"))
            {
                continue;
            }

            lowered = line.toLowerCase();

            if (lowered.startsWith("for ") || lowered.startsWith("done ") || lowered.startsWith("seen ") || lowered.startsWith("deleted "))
            {
                String[] parts = line.split("[\\s]+");
                if (parts != null && parts.length == 2)
                {
                    if (parts[0].length() > 0 && parts[1].length() > 0)
                        continue;
                }
            }

            if (lowered.startsWith("mv "))
            {
                String[] parts = parseCommand(line, lineNo, 3);

                String fromName = "";
                if (parts != null && parts.length > 2)
                {
                    fromName = parseFilename(parts[1], lineNo);
                    String fromLib = parseLibrary(parts[1], lineNo);
                }
                if (!(fromName.length() > 0))
                    throw new MungeException("Malformed from filename on line " + lineNo + " in " + filename);

                String toLib = "";
                String toName = "";
                if (parts.length > 3)
                {
                    toLib = parseLibrary(parts[2], lineNo);
                    toName = parseFilename(parts[2], lineNo);
                }
                if (!(toName.length() > 0))
                    throw new MungeException("Malformed to filename on line " + lineNo + " in " + filename);

                continue;
            }

            if (lowered.startsWith("rm "))
            {
                String[] parts = parseCommand(line, lineNo, 2);

                if (parts != null && parts.length > 2)
                    parseLibrary(parts[1], lineNo);
                String fromName = parseFilename(parts[1], lineNo);
                if (!(fromName.length() > 0))
                    throw new MungeException("Malformed from filename on line " + lineNo + " in " + filename);

                continue;
            }

            throw new MungeException("Unknown keyword on malformed line " + lineNo + " in " + filename);
        }
        return lines;
    }

    public String writeHint(String action, boolean isWorkstation, NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        String hintPath = "";

        // if a workstation and source is publisher then it is local or a basic add and there is no hint
        if (isWorkstation && !sourceTuo.isSubscriber())
            return "";

        boolean sourceIsCollection = sourceTuo.node.getMyTree().getName().toLowerCase().contains("collection");
        boolean targetIsCollection = (targetTuo != null) ? targetTuo.node.getMyTree().getName().toLowerCase().contains("collection") : false;

        // if source is subscriber system tab this it is a basic add, no hint
        if (sourceTuo.isSubscriber() && !sourceIsCollection)
            return "";

        // if either the source or target are not a collection there is no hint
        if (sourceIsCollection || targetIsCollection)
        {
            String command = "";
            String act = action.trim().toLowerCase();

            if (act.equals("mv"))
            {
                String moveTo;
                moveTo = reduceCollectionPath(targetTuo);
                // do not append right-side target path if the nodes are the same
                if (sourceTuo.node != targetTuo.node)
                {
                    if (!moveTo.trim().endsWith("|"))
                        moveTo += targetTuo.getRepo().getSeparator();
                    moveTo += Utils.getRightPath(sourceTuo.getPath(), targetTuo.getRepo().getSeparator());
                }

                command = "mv \"" + reduceCollectionPath(sourceTuo) + "\" \"" + moveTo + "\"";
            }
            else if (act.equals("rm"))
            {
                command = "rm \"" + reduceCollectionPath(sourceTuo) + "\"";
            }
            else
                throw new MungeException("Action must be 'mv' or 'rm'");

            hintPath = Utils.getLeftPath(sourceTuo.path, null);
            String hintName = Utils.getRightPath(sourceTuo.path, null);
            hintPath = hintPath + Utils.getSeparatorFromPath(hintPath) + hintName + ".els";

            // do not write a Hint about the same Hint
            if (Utils.getRightPath(sourceTuo.path, null).equals(hintName + ".els"))
                return "";

            if (!sourceTuo.isSubscriber())
                context.localMode = true;

            String sourceKey = sourceTuo.isSubscriber() ? context.subscriberRepo.getLibraryData().libraries.key : context.publisherRepo.getLibraryData().libraries.key;
            writeOrUpdateHint(hintPath, command, sourceKey);
            context.localMode = false;
        }
        return hintPath;
    }

    public void writeOrUpdateHint(String hintPath, String command, String sourceKey) throws Exception
    {
        if (context.cfg.isRemoteSession() && !context.localMode)
        {
            String line = "hint \"" + hintPath + "\" " + command;
            context.clientStty.roundTrip(line + "\n", "Sending remote: " + line, 10000);
        }
        else // local operation
        {
            File hintFile = new File(hintPath);
            command = command + "\n";

            if (Files.exists(hintFile.toPath()))
            {
                Files.write(hintFile.toPath(), command.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                logger.info(context.cfg.gs("Transfer.updated.hint.file") + hintFile.getAbsolutePath());
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append("# Created " + new Date().toString() + "\n");

                ArrayList<HintKeys.HintKey> keys = context.hintKeys.get();
                for (HintKeys.HintKey key : keys)
                {
                    if (key.uuid.equalsIgnoreCase(sourceKey))
                        sb.append("Done " + key.name + "\n");
                    else
                        sb.append("For " + key.name + "\n");
                }
                sb.append(command);
                Files.write(hintFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                logger.info(context.cfg.gs("Transfer.created.hint.file") + hintFile.getAbsolutePath());
            }
        }
    }

}
