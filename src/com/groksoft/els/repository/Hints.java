package com.groksoft.els.repository;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.StringTokenizer;

/**
 * The Hints class handles finding and executing ELS Hints.
 */
public class Hints
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private final Marker SHORT = MarkerManager.getMarker("SHORT");
    private final Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Configuration cfg;
    private Context context;
    private int deletedHints = 0;
    private int doneHints = 0;
    private int executedHints = 0;
    private HintKeys keys;
    private int seenHints = 0;
    private int skippedHints = 0;
    private int validatedHints = 0;

    public Hints(Configuration config, Context ctx, HintKeys hintKeys)
    {
        cfg = config;
        context = ctx;
        keys = hintKeys;
    }

    private void dumpStats()
    {
        logger.info(SHORT, "+------------------------------------------");
        if (validatedHints > 0)
        {
            logger.info(SHORT, "# Validated hints    : " + validatedHints + " (--dry-run)");
        }
        else
        {
            logger.info(SHORT, "# Executed hints     : " + executedHints);
            logger.info(SHORT, "# Done hints         : " + doneHints);  // TODO Reconsider metrics
            logger.info(SHORT, "# Seen hints         : " + seenHints);
            logger.info(SHORT, "# Skipped hints      : " + skippedHints);
            logger.info(SHORT, "# Deleted hints      : " + deletedHints);
            logger.info(SHORT, "# Moved directories  : " + context.transfer.getMovedDirectories());
            logger.info(SHORT, "# Moved files        : " + context.transfer.getMovedFiles());
            logger.info(SHORT, "# Removed directories: " + context.transfer.getRemovedDirectories());
            logger.info(SHORT, "# Removed files      : " + context.transfer.getRemovedFiles());
            logger.info(SHORT, "# Skipped missing    : " + context.transfer.getSkippedMissing());
        }
        if (!cfg.isHintSkipMainProcess())
            logger.info(SHORT, "-------------------------------------------");
    }

    private void dumpTerms(String[] parts)
    {
        for (int i = 0; i < parts.length; ++i)
        {
            if (parts[i] != null && parts[i].length() > 0)
            {
                logger.debug("    " + parts[i]);
            }
        }
    }

    private boolean execute(Repository repo, Item item, List<String> lines) throws Exception
    {
        HintKeys.HintKey hintKey;
        boolean libAltered = false;
        String statusLine;

        logger.info("* Executing " + item.getFullPath() + " on " + repo.getLibraryData().libraries.description);

        // find the ELS key for this repo
        hintKey = findHintKey(repo);

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

            // skip blank lines
            if (line.length() < 1)
                continue;

            // log & skip comments
            if (line.startsWith("#"))
            {
                logger.info("  " + line);
                continue;
            }

            if (line.startsWith("for ") || line.startsWith("done ") || line.startsWith("seen "))
            {
                continue;
            }

            // mv move
            if (line.toLowerCase().startsWith("mv "))
            {
                String[] parts = parseCommand(line, lineNo, 3); // null never returned
                //dumpTerms(parts);

                String fromLib = parseLibrary(parts[1], lineNo);
                if (fromLib == null)
                    fromLib = item.getLibrary(); // use the library of the .els item

                String fromName = parseFile(parts[1], lineNo);
                if (fromName.length() < 1)
                    throw new MungeException("Malformed from filename on line " + lineNo);

                String toLib = parseLibrary(parts[2], lineNo);
                if (toLib == null)
                    toLib = item.getLibrary(); // use the library of the .els item

                String toName = parseFile(parts[2], lineNo);
                if (toName.length() < 1)
                    throw new MungeException("Malformed to filename on line " + lineNo);

                context.hintMode = true;
                if (context.transfer.move(repo, fromLib.trim(), fromName.trim(), toLib.trim(), toName.trim()))
                    libAltered = true;
                context.hintMode = false;

                item.setHintExecuted(true);
            }
            else if (line.toLowerCase().startsWith("rm ")) // rm remove
            {
                String[] parts = parseCommand(line, lineNo, 2); // null never returned
                //dumpTerms(parts);

                String fromLib = parseLibrary(parts[1], lineNo);
                if (fromLib == null)
                    fromLib = item.getLibrary(); // use the library of the .els item

                String fromName = parseFile(parts[1], lineNo);
                if (fromName.length() < 1)
                    throw new MungeException("Malformed from filename on line " + lineNo);

                if (context.transfer.remove(repo, fromLib.trim(), fromName.trim()))
                    libAltered = true;

                item.setHintExecuted(true);
            }
        }

        // update hint status
        if (item.isHintExecuted())
        {
            if (!cfg.isDryRun())
            {
                updateHintStatus(repo, item, lines, hintKey.name, "Done");
            }
            ++executedHints;
        }

        return libAltered;
    }

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

    private String findNameLine(List<String> lines, String name)
    {
        for (String line : lines)
        {
            String parts[] = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                if (word.equals("for") || word.equals("done") || word.equals("seen"))
                {
                    if (parts[1].equalsIgnoreCase(name))
                        return line.trim();
                }
            }
        }
        return null;
    }

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
                        long space = context.transfer.getFreespace(candidate);
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
        return target;
    }

    private String getStatusString(int rank)
    {
        return ((rank == 0) ? "For" : ((rank == 1) ? "Done" : "Seen"));
    }

    public boolean hintExecute(String libName, String itemPath, String toPath) throws Exception
    {
        boolean sense = false;
        Item toItem = null;

        File toFile = new File(toPath + ".merge");
        if (toFile.exists())
        {
            Item existingItem = context.subscriberRepo.hasItem(null, libName, itemPath);
            if (existingItem != null)
                toItem = SerializationUtils.clone(existingItem);
        }

        // merge
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

        List<String> lines = readHint(context.subscriberRepo, toItem);
        execute(context.subscriberRepo, toItem, lines);
        if (toItem.isHintExecuted())
            sense = true;

        postprocessHint(context.subscriberRepo, toItem);

        return sense;
    }

    public void hintsLocal() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Processing local ELS Hints for " + context.publisherRepo.getLibraryData().libraries.description + (cfg.isDryRun() ? " (--dry-run)" : ""));

        for (Library lib : context.publisherRepo.getLibraryData().libraries.bibliography)
        {
            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                    (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
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
                    List<String> lines = readHint(context.publisherRepo, item);
                    boolean libAltered = execute(context.publisherRepo, item, lines);
                    lib.rescanNeeded = true;
                }
            }
        }

        if (hintsFound)
        {
            dumpStats();

            if (!cfg.isDryRun())
            {
                for (Library lib : context.publisherRepo.getLibraryData().libraries.bibliography)
                {
                    // if processing all libraries, or this one was specified on the command line with -l,
                    // and it has not been excluded with -L
                    if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                            (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
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

    public void hintsMunge() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Munging ELS Hints from " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description + (cfg.isDryRun() ? " (--dry-run)" : ""));

        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            Library pubLib = null;
            Item toItem;

            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name)) &&
                    (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(subLib.name))) // v3.0.0
            {
                // if the subscriber has included and not excluded this library
                if (subLib.name.startsWith(context.subscriberRepo.SUB_EXCLUDE)) // v3.0.0
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
                            if (!cfg.isRemoteSession()) // remote collection already loaded and may be empty
                            {
                                context.subscriberRepo.scan(subLib.name);
                            }
                        }

                        // Hints are intended to be copied and processed immediately
                        // So the hint cannot be copied during a --dry-run
                        // Validate the syntax instead
                        if (cfg.isDryRun())
                        {
                            logger.info("* Validating syntax for dry run: " + item.getFullPath());
                            ++validatedHints;
                            List<String> lines = readHint(null, item);  // reads & validates hint
                        }
                        else
                        {
                            // read the ELS hint file
                            List<String> lines = readHint(null, item);

                            // check if the publisher has Done or Seen this hint
                            // this is important prior to a backup run to avoid duplicates, etc.
                            HintKeys.HintKey hintKey = findHintKey(context.publisherRepo);
                            String statusLine = findNameLine(lines, hintKey.name);
                            if (statusLine == null || (!statusLine.toLowerCase().startsWith("done ") && !statusLine.toLowerCase().startsWith("seen ")))
                                throw new MungeException("Publisher must execute hints locally; Status is not Done or Seen in hint: " + item.getFullPath());

                            String toPath = getHintTarget(item); // never null
                            String tmpPath = toPath + ".merge";
                            context.transfer.copyFile(item.getFullPath(), tmpPath, true);

                            boolean updatePubSide = false;
                            if (cfg.isRemoteSession())
                            {
                                toItem = SerializationUtils.clone(item);
                                toItem.setFullPath(toPath);
                                logger.info("* Executing " + item.getFullPath() + " remotely on " + context.subscriberRepo.getLibraryData().libraries.description);

                                // Send command to merge & execute
                                String response = context.clientStty.roundTrip("execute \"" +
                                        toItem.getLibrary() + "\" \"" + toItem.getItemPath() + "\" \"" + toPath + "\"");
                                if (response != null && response.length() > 0)
                                {
                                    logger.debug("  > execute command returned: " + response);
                                    updatePubSide = response.equalsIgnoreCase("true") ? true : false;
                                    if (updatePubSide)
                                    {
                                        subLib.rescanNeeded = true;
                                        ++executedHints;
                                    }
                                }
                                else
                                    throw new MungeException("Subscriber disconnected");
                            }
                            else
                            {
                                // merge
                                mergeHints(tmpPath, toPath);

                                // execute
                                toItem = SerializationUtils.clone(item);
                                toItem.setFullPath(toPath);

                                lines = readHint(context.subscriberRepo, toItem);
                                execute(context.subscriberRepo, toItem, lines);
                                if (toItem.isHintExecuted())
                                    updatePubSide = true;
                            }

                            updateHintSubscriberOnPublisher(context.subscriberRepo, item);

                            if (!cfg.isRemoteSession()) // subscriber-side does this itself
                            {
                                postprocessHint(context.subscriberRepo, toItem);
                            }
                            postprocessHint(context.publisherRepo, item);
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

            if (!cfg.isDryRun())
            {
                boolean request = false;
                for (Library lib : context.subscriberRepo.getLibraryData().libraries.bibliography)
                {
                    // if processing all libraries, or this one was specified on the command line with -l,
                    // and it has not been excluded with -L
                    if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                            (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
                    {
                        if (lib.rescanNeeded)
                        {
                            if (cfg.isRemoteSession())
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
                    logger.info("Requesting updated subscriber collection from: " + context.subscriberRepo.getLibraryData().libraries.description);
                    context.transfer.requestCollection();
                }
            }
        }
        else
        {
            logger.info("  - No .els hint files found");
        }
    }

    public void hintsSubscriberCleanup() throws Exception
    {
        if (cfg.isRemoteSession() && !context.hintMode)
        {
            logger.info("Sending hints cleanup command to remote " + context.subscriberRepo.getLibraryData().libraries.description);

            // Send command to merge & execute
            String response = context.clientStty.roundTrip("cleanup");
            if (response != null && response.length() > 0)
            {
                logger.debug("  > cleanup command returned: " + response);
            }
            else
            {
                throw new MungeException("Subscriber disconnected");
            }
        }
        else
        {
            subscriberCleanup();
        }
    }

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
            if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen "))
            {
                String[] mergeParts = parseNameLine(mergeline, i);
                String mergeStatus = mergeParts[0];
                String mergeName = mergeParts[1];
                int mergeRank = statusToInt(mergeStatus);

                for (int j = 0; j < toLines.size(); ++j)
                {
                    String existing = toLines.get(j);
                    copy = existing.toLowerCase();
                    if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen "))
                    {
                        String[] existingParts = parseNameLine(existing, j);
                        String existingStatus = existingParts[0];
                        String existingName = existingParts[1];

                        if (mergeName.equalsIgnoreCase(existingName))
                        {
                            int existingRank = statusToInt(existingStatus);
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

    private List<String> mergeStatusServer(Repository repo, Item item, List<String> lines) throws Exception
    {
        // is a hint status server being used?
        if (repo != null && context.statusStty != null)
        {
            boolean changed = false;
            HintKeys.HintKey hintKey = findHintKey(repo);

            // get the status from the status server
            String command = "get \"" + item.getLibrary() + "\" " +
                    "\"" + item.getItemPath() + "\" " +
                    "\"" + hintKey.name + "\" " +
                    "\"" + "For" + "\"";        // QUESTION Is this necessary?

            String response = context.statusStty.roundTrip(command);
            if (response != null && !response.equalsIgnoreCase("false"))
            {
                int mergeRank = statusToInt(response);
                for (int i = 0; i < lines.size(); ++i)
                {
                    String existing = lines.get(i);
                    String copy = existing.toLowerCase();
                    if (copy.startsWith("for ") || copy.startsWith("done ") || copy.startsWith("seen "))
                    {
                        String[] existingParts = parseNameLine(existing, i);
                        String existingStatus = existingParts[0];
                        String existingName = existingParts[1];

                        if (existingName.equalsIgnoreCase(hintKey.name))
                        {
                            int existingRank = statusToInt(existingStatus);
                            if (mergeRank > existingRank)
                            {
                                String mergeStatus = getStatusString(mergeRank);
                                String mergeline = mergeStatus + " " + hintKey.name;
                                lines.set(i, mergeline);
                                changed = true;
                            }
                            break;
                        }
                    }
                }

                if (changed)
                    Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
            }
            else
                throw new MungeException("Status Server " + context.statusRepo.getLibraryData().libraries.description + " returned a failure during get");
        }
        return lines;
    }

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

    private String parseFile(String term, int lineNo) throws MungeException
    {
        String name = null;
        String[] parts = term.split("\\|");
        if (parts.length > 2)
            throw new MungeException("Malformed library|file term on line " + lineNo + ": " + term);
        if (parts.length == 1)
            name = parts[0];
        else if (parts.length == 2)
            name = parts[1];
        return name;
    }

    private String parseLibrary(String term, int lineNo) throws MungeException
    {
        String lib = null;
        String[] parts = term.split("\\|");
        if (parts.length > 2)
            throw new MungeException("Malformed library|file term on line " + lineNo + ": " + term);
        if (parts.length == 2)
            lib = parts[0];
        return lib;
    }

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

    private void postprocessHint(Repository repo, Item item) throws Exception
    {
        int doneWords = 0;
        int forWords = 0;
        int seenWords = 0;
        String pubStat = "";
        String subStat = "";
        int totalWords = 0;

        // read the ELS hint file
        if (Files.notExists(Paths.get(item.getFullPath())))
            return;
        List<String> lines = readHint(item.getFullPath()); // hint not validated

        // find the ELS keys
        HintKeys.HintKey pubHintKey = findHintKey(context.publisherRepo);
        HintKeys.HintKey subHintKey = findHintKey(context.subscriberRepo);

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
                }
                if (name.equalsIgnoreCase(subHintKey.name))
                {
                    subStat = word;
                }
                if (word.equals("for") || word.equals("done") || word.equals("seen"))
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
                    }
                }
            }
        }

        if (forWords == 0)
        {
            if (doneWords > 0)
            {
                if (pubStat.equals("done"))
                {
                    updateHintStatus(repo, item, lines, pubHintKey.name, "Seen");
                    --doneWords;
                    ++seenWords;
                }
                if (subStat.equals("done"))
                {
                    updateHintStatus(repo, item, lines, subHintKey.name, "Seen");
                    --doneWords;
                    ++seenWords;
                }
            }
            else if (seenWords == totalWords)
            {
                File prevFile = new File(item.getFullPath());
                if (prevFile.exists())
                {
                    if (cfg.isDryRun())
                    {
                        logger.info("  > hint done and seen, would remove: " + item.getFullPath());
                    }
                    else
                    {
                        if (prevFile.delete())
                        {
                            logger.info("  > hint done and seen, removing: " + item.getFullPath());
                            ++deletedHints;
                        }
                    }
                }
            }
        }
    }

    /**
     * Read, cleanup & validate lines from a hint file
     *
     * @param item Item to be read
     * @return String lines that have tabs replaced with a space, trimmed and are validated
     * @throws Exception
     */
    private List<String> readHint(Repository repo, Item item) throws Exception
    {
        List<String> lines = readHint(item.getFullPath());
        lines = mergeStatusServer(repo, item, lines);
        return validate(item.getFullPath(), lines);
    }

    /**
     * Read and cleanup lines from a hint file
     *
     * @param path Full path to hint file
     * @return String lines that have tabs replaced with a space and trimmed
     * @throws Exception
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

    private int statusToInt(String status)
    {
        if (status.equalsIgnoreCase("for"))
            return 0;
        else if (status.equalsIgnoreCase("done"))
            return 1;
        return 2;
    }

    private void subscriberCleanup() throws Exception
    {
        logger.info("Cleaning-up ELS Hints on " + context.subscriberRepo.getLibraryData().libraries.description);

        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            // if processing all libraries, or this one was specified on the command line with -l,
            // and it has not been excluded with -L
            if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name)) &&
                    (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(subLib.name))) // v3.0.0
            {
                // if the subscriber has included and not excluded this library
                if (subLib.name.startsWith(context.subscriberRepo.SUB_EXCLUDE)) // v3.0.0
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
                if (!cfg.isDryRun())
                {
                    for (Item item : subLib.items)
                    {
                        // only ELS Hints
                        if (!item.getItemPath().toLowerCase().endsWith(".els"))
                        {
                            continue;
                        }
                        postprocessHint(context.subscriberRepo, item);
                    }
                }
            }
            else
            {
                logger.info("Skipping subscriber library: " + subLib.name);
            }
        }
    }

    private void updateHintStatus(Repository repo, Item item, List<String> lines, String systemName, String status) throws Exception
    {
        boolean changed = false;
        int lineNo = 0;

        for (String line : lines)
        {
            String[] parts = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                if (word.equals("for") || word.equals("done") || word.equals("seen"))
                {
                    int mergeRank = statusToInt(word);
                    int existingRank = statusToInt(status);
                    if (existingRank > mergeRank)
                    {
                        if (parts[1].equalsIgnoreCase(systemName))
                        {
                            line = status + " " + parts[1];
                            lines.set(lineNo, line);
                            changed = true;
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

            // is a hint status server being used?
            if (repo != null && context.statusStty != null)
            {
                HintKeys.HintKey hintKey = findHintKey(repo);

                // set the status on the status server
                String command = "set \"" + item.getLibrary() + "\" " +
                        "\"" + item.getItemPath() + "\" " +
                        "\"" + hintKey.name + "\" " +
                        "\"" + status + "\"";

                String response = context.statusStty.roundTrip(command);
                if (response == null || !response.equalsIgnoreCase(status))
                    throw new MungeException("Status Server " + context.statusRepo.getLibraryData().libraries.description + " returned a failure during set");
            }
        }
    }

    private void updateHintSubscriberOnPublisher(Repository repo, Item item) throws Exception
    {
        List<String> lines = readHint(item.getFullPath());  // hint not validated
        HintKeys.HintKey hintKey = findHintKey(context.subscriberRepo);
        updateHintStatus(repo, item, lines, hintKey.name, "Done");
    }

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

            if (lowered.startsWith("for ") || lowered.startsWith("done ") || lowered.startsWith("seen "))
            {
                if (lowered.startsWith("done "))
                    ++doneHints;
                else if (lowered.startsWith("seen "))
                    ++seenHints;

                continue;
            }

            if (lowered.startsWith("mv "))
            {
                String[] parts = parseCommand(line, lineNo, 3);

                String fromName = "";
                if (parts != null && parts.length > 2)
                {
                    fromName = parseFile(parts[1], lineNo);
                    String fromLib = parseLibrary(parts[1], lineNo);
                }
                if (!(fromName.length() > 0))
                    throw new MungeException("Malformed from filename on line " + lineNo + " in " + filename);

                String toLib = "";
                String toName = "";
                if (parts.length > 3)
                {
                    toLib = parseLibrary(parts[2], lineNo);
                    toName = parseFile(parts[2], lineNo);
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
                String fromName = parseFile(parts[1], lineNo);
                if (!(fromName.length() > 0))
                    throw new MungeException("Malformed from filename on line " + lineNo + " in " + filename);

                continue;
            }

            throw new MungeException("Unknown keyword on malformed line " + lineNo + " in " + filename);
        }
        return lines;
    }

}
