package com.groksoft.els.repository;

import com.groksoft.els.Configuration;
import com.groksoft.els.Main;
import com.groksoft.els.MungerException;
import com.groksoft.els.Utils;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.nio.file.Files;
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
    private final int MAX_TERMS = 4;
    private Marker SHORT = MarkerManager.getMarker("SHORT");
    private Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
    private Configuration cfg;
    private Main.Context context;
    private int deletedHints = 0;
    private int doneHints = 0;
    private int executedHints = 0;
    private Repository fromRepo;
    private HintKeys keys;
    private int seenHints = 0;
    private int skippedHints = 0;
    private Repository toRepo;

    private Hints()
    {
        // hide default constructor
    }

    public Hints(Configuration config, Main.Context ctx, HintKeys hintKeys)
    {
        cfg = config;
        context = ctx;
        keys = hintKeys;
    }

    private void dumpStats()
    {
        logger.info(SHORT, "+------------------------------------------");
        logger.info(SHORT, "# Executed hints     : " + executedHints + (cfg.isDryRun() ? " (--dry-run)" : ""));
        logger.info(SHORT, "# Done hints         : " + doneHints);
        logger.info(SHORT, "# Seen hints         : " + seenHints);
        logger.info(SHORT, "# Skipped hints      : " + skippedHints);
        logger.info(SHORT, "# Deleted hints      : " + deletedHints);
        logger.info(SHORT, "# Moved directories  : " + context.transfer.getMovedDirectories());
        logger.info(SHORT, "# Moved files        : " + context.transfer.getMovedFiles());
        logger.info(SHORT, "# Removed directories: " + context.transfer.getRemovedDirectories());
        logger.info(SHORT, "# Removed files      : " + context.transfer.getRemovedFiles());
        logger.info(SHORT, "# Skipped missing    : " + context.transfer.getSkippedMissing());
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

    private boolean execute(Repository repo, Item item) throws Exception
    {
        boolean libAltered = false;

        logger.info("* Executing " + item.getFullPath() + " on " + repo.getLibraryData().libraries.description);

        // read the ELS hint file
        List<String> lines = readHint(item);

        // find the ELS key for this repo
        HintKeys.HintKey hintKey = findHintKey(repo);

        // find the actor name in the .els file
        String statusLine = findNameLine(lines, hintKey.name);
        if (statusLine != null)
        {
            if (statusLine.toLowerCase().startsWith("done"))
                ++doneHints;
            else if (statusLine.toLowerCase().startsWith("seen"))
                ++seenHints;
        }
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
                    throw new MungerException("Malformed from filename on line " + lineNo);

                String toLib = parseLibrary(parts[2], lineNo);
                if (toLib == null)
                    toLib = item.getLibrary(); // use the library of the .els item

                String toName = parseFile(parts[2], lineNo);
                if (toName.length() < 1)
                    throw new MungerException("Malformed to filename on line " + lineNo);

                if (context.transfer.move(repo, fromLib.trim(), fromName.trim(), toLib.trim(), toName.trim()))
                    libAltered = true;

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
                    throw new MungerException("Malformed from filename on line " + lineNo);

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
                updateHintStatus(item, lines, hintKey.name, "Done");
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
            throw new MungerException("Repository not found in ELS keys " + keys.getFilename() + " matching key in " + repo.getLibraryData().libraries.description);
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
            throw new MungerException("Target for ELS hint file cannot be found: " + item.getFullPath());
        }
        return target;
    }

    public void hintsLocal() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Processing ELS Hints for " + context.publisherRepo.getLibraryData().libraries.description + (cfg.isDryRun() ? " (--dry-run)" : ""));

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
                    boolean libAltered = execute(context.publisherRepo, item);
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
            logger.info("No .els hint files found");
        }
    }

    public void hintsMunge() throws Exception
    {
        boolean hintsFound = false;
        logger.info("Processing ELS Hints from " + context.publisherRepo.getLibraryData().libraries.description + " to " +
                context.subscriberRepo.getLibraryData().libraries.description + (cfg.isDryRun() ? " (--dry-run)" : ""));

        for (Library subLib : context.subscriberRepo.getLibraryData().libraries.bibliography)
        {
            boolean scanned = false;
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
                    // do the libraries have items or do they need to be scanned?
                    if (subLib.items == null || subLib.items.size() < 1)
                    {
                        if (!cfg.isRemoteSession()) // remote collection already loaded and may be empty
                        {
                            context.subscriberRepo.scan(subLib.name);
                        }
                    }
                    if (pubLib.items == null || pubLib.items.size() < 1)
                    {
                        context.publisherRepo.scan(pubLib.name);
                        scanned = true;
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
                        String toPath = getHintTarget(item);
                        context.transfer.copyFile(item.getFullPath(), toPath, true);

                        boolean updatePubSide = false;
                        if (cfg.isRemoteSession())
                        {
                            toItem = SerializationUtils.clone(item);
                            toItem.setFullPath(toPath);
                        }
                        else
                        {
                            toItem = SerializationUtils.clone(item);
                            toItem.setFullPath(toPath);
                            execute(context.subscriberRepo, toItem);
                            if (toItem.isHintExecuted())
                                updatePubSide = true;
                        }

                        if (updatePubSide && !cfg.isDryRun())
                        {
                            updateHintSubscriberOnPublisher(context.publisherRepo, item);
                        }

                        if (cfg.isRemoteSession())
                        {
                            // remotely postprocessHint(toItem);
                            postprocessHint(item);
                        }
                        else
                        {
                            postprocessHint(toItem);
                            postprocessHint(item);
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
                logger.info("Skipping publisher library: " + subLib.name);
            }
        }

        if (hintsFound)
        {
            dumpStats();

            if (!cfg.isDryRun())
            {
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

                            }
                            else
                            {
                                context.subscriberRepo.scan(lib.name);
                            }
                        }
                    }
                }
            }
        }
        else
        {
            logger.info("No .els hint files found");
        }
    }

    private String[] parseCommand(String line, int lineNo, int expected) throws Exception
    {
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
                    throw new MungerException("Too many terms");
            }
        }
        if (i != expected)
            throw new MungerException("Malformed command, " + i + " is wrong number of terms, expecting " + expected + " for line " + lineNo);
        return cmd;
    }

    private String parseFile(String term, int lineNo) throws MungerException
    {
        String name = null;
        String[] parts = term.split("\\|");
        if (parts.length > 2)
            throw new MungerException("Malformed library|file term on line " + lineNo + ": " + term);
        if (parts.length == 1)
            name = parts[0];
        else if (parts.length == 2)
            name = parts[1];
        return name;
    }

    private String parseLibrary(String term, int lineNo) throws MungerException
    {
        String lib = null;
        String[] parts = term.split("\\|");
        if (parts.length > 2)
            throw new MungerException("Malformed library|file term on line " + lineNo + ": " + term);
        if (parts.length == 2)
            lib = parts[0];
        return lib;
    }

    private void postprocessHint(Item item) throws Exception
    {
        int doneWords = 0;
        int forWords = 0;
        int seenWords = 0;
        String pubStat = "";
        String subStat = "";
        int totalWords = 0;

        // read the ELS hint file
        List<String> lines = readHint(item);

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
                    updateHintStatus(item, lines, pubHintKey.name, "Seen");
                    --doneWords;
                    ++seenWords;
                    --doneHints;
                    ++seenHints;
                }
                if (subStat.equals("done"))
                {
                    updateHintStatus(item, lines, subHintKey.name, "Seen");
                    --doneWords;
                    ++seenWords;
                    --doneHints;
                    ++seenHints;
                }
            }

            if (seenWords == totalWords)
            {
                File prevFile = new File(item.getFullPath());
                if (prevFile.delete())
                {
                    logger.info("  > rm hint: " + item.getFullPath());
                    ++deletedHints;
                }
            }
        }
    }

    private List<String> readHint(Item item) throws Exception
    {
        // read the ELS hint file, convert tabs to spaces and trim lines
        String file = item.getFullPath();
        List<String> lines = Files.readAllLines(Paths.get(file));
        for (int i = 0; i < lines.size(); ++i)
        {
            String line = lines.get(i);
            line = line.replaceAll("\\t", " ").trim();
            lines.set(i, line);

            // syntax check
            line = line.toLowerCase();
            if (line.length() > 0 && !line.startsWith("#") &&
                    !line.startsWith("for ") && !line.startsWith("done ") && !line.startsWith("seen ") &&
                    !line.startsWith("mv ") && !line.startsWith("rm "))
                throw new MungerException("malformed line " + (i + 1) + ": " + line);
        }
        return lines;
    }

    private void updateHintStatus(Item item, List<String> lines, String name, String status) throws Exception
    {
        int lineNo = 0;
        for (String line : lines)
        {
            String parts[] = line.split("[\\s]+");
            if (parts.length == 2)
            {
                String word = parts[0].toLowerCase();
                if (word.equals("for") || word.equals("done") || word.equals("seen"))
                {
                    if (parts[1].equalsIgnoreCase(name))
                    {
                        line = status + " " + parts[1];
                        lines.set(lineNo, line);
                    }
                }
            }
            ++lineNo;
        }
        Files.write(Paths.get(item.getFullPath()), lines, StandardOpenOption.CREATE);
    }

    private void updateHintSubscriberOnPublisher(Repository repo, Item item) throws Exception
    {
        // read the ELS hint file
        List<String> lines = readHint(item);

        // find the ELS key for this repo
        HintKeys.HintKey hintKey = findHintKey(context.subscriberRepo);

        updateHintStatus(item, lines, hintKey.name, "Done");
    }

}
