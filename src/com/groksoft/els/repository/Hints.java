package com.groksoft.els.repository;

import com.groksoft.els.Configuration;
import com.groksoft.els.Main;
import com.groksoft.els.MungerException;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.StringTokenizer;

public class Hints
{
    public final int TO_PUBLISHER = 1;
    public final int TO_SUBSCRIBER = 2;
    Configuration cfg;
    Main.Context context;
    HintKeys keys;
    Repository toRepo;
    Repository fromRepo;
    private final transient Logger logger = LogManager.getLogger("applog");

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

    private void copy(String from, String to, boolean overwrite) throws Exception
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

    public void execute(Repository repo, Item item) throws Exception
    {
        String file = item.getFullPath();
        List<String> lines = Files.readAllLines(Paths.get(file));
        for (int i = 0; i < lines.size(); ++i)
        {
            lines.set(i, lines.get(i).replaceAll("\\t", " ").trim());
        }

        // find the ELS key for this repo
        HintKeys.HintKey hintKey = keys.findKey(repo.getLibraryData().libraries.key);
        if (hintKey == null)
        {
            logger.info("Library not found in ELS keys " + keys.getFilename() + " matching key in " + repo.getLibraryData().libraries.description);
            return;
        }

        // find the actor name in the .els file
        String statusLine = findNameLine(lines, hintKey.name);
        if (statusLine == null || (!statusLine.toLowerCase().startsWith("for")))
        {
            logger.info("    > Skipping execution, not For " + hintKey.name);
            return;
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
                logger.info("    " + line);
                continue;
            }

            // mv command, move command
            if (line.toLowerCase().startsWith("mv "))
            {
                logger.info("    " + line);
                String[] parts = parseCommand(line, lineNo, 3);
                dumpTerms(parts);

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

                repo.move(fromLib, fromName, toLib, toName);
                int i = 1;


            }

        }

/*
        for (String line : lines)
        {
        }
*/

    }

    private void dumpTerms(String[] parts)
    {
        for (int i = 0; i < parts.length; ++i)
        {
            if (parts[i] != null && parts[i].length() > 0)
            {
                logger.debug("      " + parts[i]);
            }
        }
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
                        return line;
                }
            }
        }
        return null;
    }

    private long getFreespace(String path) throws Exception
    {
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
        return space;
    }

    private String getHintTarget(Item item, int direction) throws Exception
    {
        String target = null;
        Repository toRepo = getRepository(direction, true);
        Repository fromRepo = getRepository(direction, false);

        // does the target already have this hint?
        Item entry = toRepo.hasItem(item, Utils.pipe(fromRepo, item.getItemPath()));
        if (entry != null)
        {
            // FixMe ! What about collision of changes?
            target = entry.getFullPath(); // yes, overwrite it
        }
        else
        {
            // does the target have this directory?
            String path = context.subscriberRepo.hasDirectory(item.getLibrary(), Utils.pipe(fromRepo, item.getItemPath()));
            if (path != null)
            {
                target = path + Utils.getFileSeparator(toRepo.getLibraryData().libraries.flavor) + item.getItemPath(); // yes, use it
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
                        long space = getFreespace(candidate);
                        if (space > (item.getSize() + (1024 * 1024 * 10)))
                        {
                            target = candidate + Utils.getFileSeparator(toRepo.getLibraryData().libraries.flavor) + item.getItemPath();
                            break;
                        }
                    }
                }
            }
        }
        if (target == null)
        {
            // target does not have the item or directory??? Must be a new item???
            throw new MungerException("Target for ELS hint file cannot be found: " + item.getFullPath());
        }
        return target;
    }

    private Repository getRepository(int direction, boolean wantTo) throws MungerException
    {
        Repository repo = null;
        if (wantTo)
        {
            if (direction == TO_PUBLISHER)
                repo = context.publisherRepo;
            else if (direction == TO_SUBSCRIBER)
                repo = context.subscriberRepo;
        }
        else
        {
            if (direction == TO_PUBLISHER)
                repo = context.subscriberRepo;
            else if (direction == TO_SUBSCRIBER)
                repo = context.publisherRepo;
        }
        if (repo == null)
            throw new MungerException("unknown directory " + direction);
        return repo;
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
            String term =  t.nextToken().trim();
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

    public void process(Library library, int direction) throws Exception
    {
        logger.info("Processing ELS Hints " + ((direction == TO_SUBSCRIBER) ? "to" : "from") + " subscriber");
        toRepo = getRepository(direction, true);
        fromRepo = getRepository(direction, false);

        // copy .els files
        for (Item item : library.items)
        {
            if (!item.getItemPath().toLowerCase().endsWith(".els"))
            {
                continue;
            }

            // check if it needs to be done locally
            run(fromRepo, item);

/*
            String path = getHintTarget(item, direction);
            logger.info("  > Copying " + item.getFullPath() + " to " + path);
            copy(item.getFullPath(), path, true);
*/


        }
    }

    private void run(Repository repo, Item item) throws Exception
    {
        if (cfg.isRemoteSession())
        {
        }
        else
        {
            logger.info("  * Executing " + item.getFullPath() + " on " + repo.getLibraryData().libraries.description);
            execute(repo, item);
        }
    }

}
