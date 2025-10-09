package com.corionis.els.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonWriter;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The type Repository
 */
public class Repository implements Comparable
{
    public static final int HINT_SERVER = 3;
    public static final boolean NO_VALIDATE = false;
    public static final int PUBLISHER = 1;
    public static final int SUBSCRIBER = 2;
    public static final boolean VALIDATE = true;
    public final String SUB_EXCLUDE = "ELS-SUBSCRIBER-SKIP_";

    private String jsonFilename = "";
    private LibraryData libraryData = null;
    private int purpose = -1;

    private transient Context context;
    private transient boolean dynamic = false;
    private transient Logger logger = LogManager.getLogger("applog");

    /**
     * Instantiate a Repository with a purpose
     *
     * @param context The Context
     * @param purpose One of PUBLISHER, SUBSCRIBER, HINT_SERVER
     */
    public Repository(Context context, int purpose)
    {
        this.context = context;
        this.purpose = purpose;
    }

    /**
     * Close this repository but do not include individual items
     *
     * @return Cloned Repository
     */
    public Repository cloneNoItems()
    {
        Repository noItems = new Repository(context, purpose);
        noItems.setJsonFilename(getJsonFilename());
        noItems.createStructure();
        noItems.libraryData.libraries.description = getLibraryData().libraries.description;
        noItems.libraryData.libraries.host = getLibraryData().libraries.host;
        noItems.libraryData.libraries.listen = getLibraryData().libraries.listen;
        noItems.libraryData.libraries.timeout = getLibraryData().libraries.timeout;
        noItems.libraryData.libraries.flavor = getLibraryData().libraries.flavor;
        noItems.libraryData.libraries.case_sensitive = getLibraryData().libraries.case_sensitive;
        noItems.libraryData.libraries.temp_dated = getLibraryData().libraries.temp_dated;
        noItems.libraryData.libraries.temp_location = getLibraryData().libraries.temp_location;
        noItems.libraryData.libraries.terminal_allowed = getLibraryData().libraries.terminal_allowed;
        noItems.libraryData.libraries.key = getLibraryData().libraries.key;
        if (getLibraryData().libraries.ignore_patterns != null)
            noItems.libraryData.libraries.ignore_patterns = getLibraryData().libraries.ignore_patterns.clone();
        else
            noItems.libraryData.libraries.ignore_patterns = null;
        noItems.libraryData.libraries.email = getLibraryData().libraries.email;
        noItems.libraryData.libraries.format = getLibraryData().libraries.format;
        noItems.libraryData.libraries.mismatches = getLibraryData().libraries.mismatches;
        noItems.libraryData.libraries.whatsNew = getLibraryData().libraries.whatsNew;
        noItems.libraryData.libraries.skipOffline = getLibraryData().libraries.skipOffline;
        if (getLibraryData().libraries.locations != null)
            noItems.libraryData.libraries.locations = getLibraryData().libraries.locations.clone();
        else
            noItems.libraryData.libraries.locations = null;
        noItems.libraryData.libraries.bibliography = new Library[getLibraryData().libraries.bibliography.length];
        for (int i = 0; i < getLibraryData().libraries.bibliography.length; ++i)
        {
            Library lib = new Library();
            lib.name = getLibraryData().libraries.bibliography[i].name;
            lib.sources = getLibraryData().libraries.bibliography[i].sources.clone();
            noItems.libraryData.libraries.bibliography[i] = lib;
        }
        return noItems;
    }
    // @formatter:on

    @Override
    public int compareTo(Object o)
    {
        return this.getLibraryData().libraries.description.compareTo(((Repository) o).getLibraryData().libraries.description);
    }

    public void createLibrary(String name)
    {
        Library lib = new Library();
        lib.name = name;
        expandBibliography(lib);
    }

    public void createStructure()
    {
        libraryData = new LibraryData();
        libraryData.libraries = new Libraries();
    }

    private void expandBibliography(Library lib)
    {
        Library[] expanded = new Library[libraryData.libraries.bibliography.length + 1];
        System.arraycopy(libraryData.libraries.bibliography, 0, expanded, 0, libraryData.libraries.bibliography.length);
        libraryData.libraries.bibliography = expanded;
        libraryData.libraries.bibliography[expanded.length - 1] = lib;
    }

    /**
     * Export library items to JSON collection file.
     *
     * @throws MungeException the els exception
     */
    public void exportItems(boolean isCollection) throws MungeException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String where = Utils.getFullPathLocal(context.cfg.getExportCollectionFilename());
        File outFile = new File(where);
        outFile.getParentFile().mkdirs();
        logger.info(MessageFormat.format(context.cfg.gs("Repository.writing.choice.collection.library.file"), isCollection ? 0 : 1) + where);
        String json = gson.toJson(getLibraryData());
        try
        {
            PrintWriter outputStream = new PrintWriter(where);
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.exception.while.writing.choice.collection.library.file.trace"), isCollection ? 0 : 1, where) + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Export library items to text file.
     *
     * @throws MungeException the els exception
     */
    public void exportText() throws MungeException
    {
        String where = Utils.getFullPathLocal(context.cfg.getExportTextFilename());
        File outFile = new File(where);
        outFile.getParentFile().mkdirs();
        logger.info(context.cfg.gs("Repository.writing.text.file") + where);
        try
        {
            PrintWriter outputStream = new PrintWriter(where);
            for (Library lib : getLibraryData().libraries.bibliography)
            {
                if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                        (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
                {
                    for (Item item : lib.items)
                    {
                        if (!item.isDirectory())
                        {
                            if (!ignore(item))
                            {
                                outputStream.println(item.getItemPath());
                            }
                        }
                    }
                }
            }
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.exception.while.writing.text.file.0.trace"), where) + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Get the right-side item name.
     *
     * @param item The Item
     * @return String of the right-side of the itemPath
     * @throws MungeException
     */
    public String getItemName(Item item) throws MungeException
    {
        String path = item.getItemPath();
        String sep = getSeparator();
        String name = path.substring(path.lastIndexOf(sep) + 1, path.length());
        return name;
    }

    /**
     * Gets LibraryData filename.
     *
     * @return the LibraryData filename
     */
    public String getJsonFilename()
    {
        return jsonFilename;
    }

    /**
     * Get specific library
     * <p>
     * Do these Libraries have a particular Library?
     *
     * @param libraryName the library name
     * @return the Library, or null if not found
     */
    public Library getLibrary(String libraryName) throws MungeException
    {
        boolean has = false;
        Library retLib = null;
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if (lib.name.equalsIgnoreCase(libraryName))
            {
                if (has) // check for a duplicate library name
                {
                    throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.library.found.more.than.once.in"), lib.name) + getJsonFilename());
                }
                has = true;
                retLib = lib;
            }
        }
        return retLib;
    }

    /**
     * Gets LibraryData.
     *
     * @return the library
     */
    public LibraryData getLibraryData()
    {
        return libraryData;
    }

    /**
     * Gets an item collection from the itemMap hash map
     */
    public Collection getMapItem(Library lib, String itemPath) throws MungeException
    {
        Collection collection = null;
        try
        {
            String key = itemPath;
            if (!getLibraryData().libraries.case_sensitive)
            {
                key = key.toLowerCase();
            }
            collection = lib.itemMap.get(key);
        }
        catch (Exception e)
        {
            throw new MungeException("itemMap.get '" + itemPath + "' failed");
        }
        return collection;
    }

    /**
     * Get the purpose of this repository
     *
     * @return 1 = publisher, 2 = subscriber, 3 = hint server
     */
    public int getPurpose()
    {
        return purpose;
    }

    /**
     * Get file separator
     *
     * @return File separator string single character
     * @throws MungeException
     */
    public String getSeparator()
    {
        String sep = getWriteSeparator();
        return sep;
    }

    /**
     * Get file separator for writing
     *
     * @return file separator string, may be multiple characters, e.g. \\
     * @throws MungeException
     */
    public String getWriteSeparator()
    {
        return Utils.getFileSeparator(getLibraryData().libraries.flavor);
    }

    /**
     * Does this repository have any items?
     *
     * @return true = repository not empty, a collection not library; otherwise false
     */
    public boolean hasContent()
    {
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if (lib.items != null && lib.items.size() > 0)
                return true;
        }
        return false;
    }

    /**
     * Has directory true/false.
     * <p>
     * String itemPath is expected to have been converted to pipe character file separators using Utils.pipe().
     *
     * @param libraryName the library name
     * @param itemPath    the match
     * @return the string, null if not found
     */
    public String hasDirectory(String libraryName, String itemPath) throws MungeException
    {
        String match = itemPath;
        Item foundItem = null;
        int i = match.lastIndexOf("|");
        if (i < 0)
        {
            return null;
        }
        String path = match.substring(0, i);
        if (path.length() < 1)
        {
            path = match.substring(0, match.lastIndexOf("|"));
        }
        match = path;
        path = null;
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if (lib.name.equalsIgnoreCase(libraryName))
            {
                foundItem = null;
                if (lib.items != null)
                {
                    // has to be a linear search because directories are not placed in the itemMap hash map
                    for (Item item : lib.items)
                    {
                        if (getLibraryData().libraries.case_sensitive)
                        {
                            if (Utils.pipe(this, item.getItemPath()).equals(match))
                            {
                                foundItem = item;
                                break;
                            }
                        }
                        else
                        {
                            if (Utils.pipe(this, item.getItemPath()).equalsIgnoreCase(match))
                            {
                                foundItem = item;
                                break;
                            }
                        }
                    }
                }
                else
                {
                    logger.warn(MessageFormat.format(context.cfg.gs("Repository.subscriber.library.has.no.items.is.command.line.configured.correctly"), lib.name));
                }
                if (foundItem != null && foundItem.isDirectory())
                {
                    path = foundItem.getFullPath().substring(0, foundItem.getFullPath().lastIndexOf(foundItem.getItemPath()) - 1);
                }
                break;
            }
        }
        return path;
    }

    /**
     * Has specific item
     * <p>
     * String match is expected to have been converted to pipe character file separators using Utils.pipe().
     * The item "has" member contains all instances including self.
     *
     * @param pubItem  the publisher item being found, for adding 'has' items
     * @param itemPath the itemPath() of the item to find
     * @return the Item, or null
     */
    public Item hasItem(Item pubItem, String libName, String itemPath) throws MungeException
    {
        Item has = null;

        if (pubItem == null || !pubItem.isDirectory())
        {
            for (Library lib : getLibraryData().libraries.bibliography)
            {
                if (context.cfg.isCrossCheck() || lib.name.equalsIgnoreCase(libName))
                {
                    if (lib.itemMap != null)
                    {
                        // hash map technique
                        Collection collection = getMapItem(lib, itemPath);
                        if (collection != null)
                        {
                            Iterator it = collection.iterator();
                            for (int i = 0; i < collection.size(); ++i)
                            {
                                Integer j = (Integer) it.next();
                                Item item = lib.items.elementAt(j);
                                if (!item.isDirectory())
                                {
                                    if (pubItem != null)
                                        pubItem.addHas(item); // add match and any duplicate for cross-reference

                                    // is it a duplicate?
                                    if (has != null)
                                    {
                                        logger.warn(MessageFormat.format(context.cfg.gs("Repository.duplicate.of.found.at"), itemPath, item.getFullPath()));
                                    }
                                    else
                                    {
                                        has = item; // return first match
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return has;
    }

    /**
     * Has duplicate true/false.
     * <p>
     * String match is expected to have been converted to pipe character file separators using Utils.pipe().
     * The item "has" member contains only duplicates and -not- self.
     *
     * @param pubItem  the publisher item being found, for adding 'has' items
     * @param itemPath the itemPath() of the item to find
     * @return the boolean
     */
    public void hasPublisherDuplicate(Item pubItem, String itemPath) throws MungeException
    {
        String key;
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if (context.cfg.isCrossCheck() || lib.name.equalsIgnoreCase(pubItem.getLibrary()))
            {
                if (lib.itemMap != null)
                {
                    // hash map technique
                    Collection collection = getMapItem(lib, itemPath);
                    if (collection != null)
                    {
                        Iterator it = collection.iterator();
                        for (int i = 0; i < collection.size(); ++i)
                        {
                            Integer j = (Integer) it.next();
                            Item item = lib.items.elementAt(j);
                            if (item != pubItem && !item.isDirectory())
                            {
                                pubItem.addHas(item); // add match and any duplicate for cross-reference
                                logger.warn(MessageFormat.format(context.cfg.gs("Repository.duplicate.of.found.at"), pubItem.getFullPath() , item.getFullPath()));
                            }
                        }
                    }
                }
                else
                {
                    throw new MungeException(context.cfg.gs("Repository.itemmap.is.null.for.library") + lib.name);
                }
            }
        }
    }

    /**
     * Determine if item should be ignored
     * <br/>
     * Examples:
     * <ul>
     *   <li>Ignore any case of "desktop.ini" = "(?i)desktop\\.ini"</li>
     *   <li>Ignore "*.srt" files = ".*\\.srt"</li>
     *   <li>Ignore directory "/Plex Versions" = ".*\\/Plex Versions.*"</li>
     *   <li>Ignore directory "Plex Versions/" = ".*Plex Versions\\/.*"</li>
     * </ul>
     *
     * @param item The item to check
     * @return true/false
     */
    public boolean ignore(Item item) throws MungeException
    {
        String str = "";
        String strPatt = "";
        boolean ret = false;
        String name = getItemName(item);

        if (name.toLowerCase().endsWith(".els")) // automatically exclude .els files
        {
            ret = true;
        }
        else
        {
            int i = 0;
            for (Pattern patt : getLibraryData().libraries.compiledPatterns)
            {
                str = getLibraryData().libraries.ignore_patterns[i++];
                strPatt = patt.toString();
                if (str.startsWith(".*\\" + getSeparator()) || str.endsWith("\\" + getSeparator() + ".*"))
                {
                    if (item.getFullPath().matches(strPatt))
                    {
                        ret = true;
                        break;
                    }
                }
                else if (name.matches(strPatt))
                {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    /**
     * Was this Repository loaded dynamically?
     *
     * @return true if dynamic
     */
    public boolean isDynamic()
    {
        return dynamic;
    }

    /**
     * Is Initialized indicator
     *
     * @returns boolean true/false
     */
    public boolean isInitialized()
    {
        if (getLibraryData() != null && (this.jsonFilename != null && this.jsonFilename.length() > 0))
            return true;
        else
            return false;
    }

    public boolean isPublisher()
    {
        return getPurpose() == PUBLISHER;
    }

    public boolean isSubscriber()
    {
        return getPurpose() == SUBSCRIBER;
    }

    /**
     * Normalize all JSON paths based on "flavor"
     */
    public void normalize() throws MungeException
    {
        if (getLibraryData() != null)
        {
            // if listen is empty use host
/*
            if (getLibraryData().libraries.listen == null ||
                    getLibraryData().libraries.listen.length() < 1)
            {
                getLibraryData().libraries.listen = getLibraryData().libraries.host;
            }
*/

            // set default timeout
            if (getLibraryData().libraries.timeout < 0)
                getLibraryData().libraries.timeout = 15; // default connection time-out if not defined

            String flavor = getLibraryData().libraries.flavor.toLowerCase();
            if (!flavor.equalsIgnoreCase(Libraries.LINUX) && !flavor.equalsIgnoreCase(Libraries.MAC) && !flavor.equalsIgnoreCase(Libraries.WINDOWS))
                throw new MungeException(context.cfg.gs("Repository.flavor.is.not.linux.mac.or.windows") + flavor);
            String from = "";
            String to = "";
            if (flavor.equalsIgnoreCase(Libraries.LINUX) || flavor.equalsIgnoreCase(Libraries.MAC))
            {
                from = "\\\\";
                to = "/";
            }
            else if (flavor.equalsIgnoreCase(Libraries.WINDOWS))
            {
                from = "/";
                to = "\\\\";
            }

            // temporary files location
            if (getLibraryData().libraries.temp_location != null)
            {
                String path = getLibraryData().libraries.temp_location;
                if (path.startsWith("~")) // is it relative to the user's home directory?
                {
                    path = System.getProperty("user.home") + path.substring(1);
                    getLibraryData().libraries.temp_location = path;
                }
            }

            if (getLibraryData().libraries.bibliography != null)
            {
                for (Library lib : getLibraryData().libraries.bibliography)
                {
                    if (lib.sources != null)
                    {
                        for (int i = 0; i < lib.sources.length; ++i)
                        {
                            if (lib.sources[i] != null && lib.sources[i].length() > 0)
                                lib.sources[i] = normalizeSubst(lib.sources[i], from, to);
                            else
                                throw new MungeException("Malformed JSON");
                        }
                    }
                    if (lib.items != null)
                    {
                        // setup the hash map for this library
                        if (lib.itemMap == null)
                            lib.itemMap = ArrayListMultimap.create();
                        else
                            lib.itemMap.clear();

                        for (int i = 0; i < lib.items.size(); ++i)
                        {
                            Item item = lib.items.elementAt(i);
                            item.setItemPath(normalizeSubst(item.getItemPath(), from, to));
                            item.setFullPath(normalizeSubst(item.getFullPath(), from, to));

                            // add itemPath & the item's index in the Vector to the hash map
                            String key = item.getItemPath();
                            if (!getLibraryData().libraries.case_sensitive)
                            {
                                key = key.toLowerCase();
                            }
                            lib.itemMap.put(Utils.pipe(this, key), i);
                        }
                    }
                }
            }
        }
    }

    /**
     * Normalize a path
     * <p>
     *
     * @param toFlavor Desired flavor of separators
     * @param path     Path to normalize
     * @return path Normalized path for desired flavor
     * @throws MungeException
     */
    public String normalizePath(String toFlavor, String path) throws MungeException
    {
        String to = Utils.getFileSeparator(toFlavor);
        path = normalizeSubst(path, Utils.getFileSeparator(getLibraryData().libraries.flavor), to);
        return path;
    }

    /**
     * Normalize a path with a specific path separator character.
     *
     * @param path The path to normalize
     * @param from The previous path separator character
     * @param to   The new path separator character
     * @return String normalized path
     */
    private String normalizeSubst(String path, String from, String to)
    {
//        if (from.equals("\\"))
//            from = "\\\\";
//        if (to.equals("\\"))
//            to = "\\\\";
//        return path.replaceAll(from, to).replaceAll("\\|", to);
        path = Utils.unpipe(Utils.pipe(path), to);
        return path;
    }

    /**
     * Read library.
     *
     * @param filename The JSON Libraries filename
     * @return boolean True if file is a valid ELS repository, false if not a repository
     * @throws MungeException the els exception
     */
    public boolean read(String filename, String type, boolean printLog) throws Exception
    {
        boolean valid = false;
        try
        {
            String json;
            if (getLibraryData() != null)
                libraryData = null;
            Gson gson = new Gson();
            filename = Utils.getFullPathLocal(filename);
            if (printLog)
                logger.info(context.cfg.gs("Repository.reading.library.file") + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            libraryData = gson.fromJson(json, LibraryData.class);
            if (libraryData != null && libraryData.libraries != null)
            {
                normalize();
                if (printLog)
                    logger.info(MessageFormat.format(context.cfg.gs("Repository.read.successfully"), libraryData.libraries.description));
                valid = true;
            }
        }
        catch (Exception ioe)
        {
            String msg = MessageFormat.format(context.cfg.gs("Repository.exception.while.reading.library"), type) +
                    filename + System.getProperty("line.separator");
            if (context.main.isStartupActive())
            {
                logger.error(msg);
                int opt = JOptionPane.showConfirmDialog(context.guiLogAppender.getStartup(),
                        msg + "Continue?",
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (opt == JOptionPane.YES_OPTION)
                {
                    context.fault = false;
                    return false;
                }
            }
            throw new MungeException(msg);
        }
        return valid;
    }

    /**
     * Scan all or libraries selected with -l.
     *
     * @throws Exception
     */
    public void scan() throws Exception
    {
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                    (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
            {
                scanSources(lib);
                sort(lib);
            }
        }
        normalize();
    }

    /**
     * Scan a specific library name.
     *
     * @throws MungeException the els exception
     */
    public void scan(String libraryName) throws MungeException
    {
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            if (libraryName.length() > 0 && libraryName.equalsIgnoreCase(lib.name))
            {
                scanSources(lib);
                sort(lib);
            }
        }
        normalize();
    }

    /**
     * Scan a specific directory, recursively.
     * <p>
     * Used by the public scan methods.
     * <p>
     * This is a local-only method
     *
     * @param directory the directory
     * @throws MungeException the els exception
     */
    private int scanDirectory(Library library, String base, String directory) throws MungeException
    {
        int count = 0;
        Item item = null;
        String fullPath = "";
        String itemPath = "";
        long size = 0;
        boolean isDir = false;
        boolean isSym = false;

        if (isSubscriber() && context.cfg.isRemoteSubscriber())
            directory = context.cfg.getFullPathSubscriber(directory);
        else
            directory = Utils.getFullPathLocal(directory);
        Path path = Paths.get(directory);

        if (library.items == null)
        {
            library.items = new Vector<>();
        }

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path))
        {
            for (Path entry : directoryStream)
            {
                ++count;
                item = new Item();
                fullPath = entry.toString();                            // full path
                item.setFullPath(fullPath);
                path = Paths.get(fullPath);
                isDir = Files.isDirectory(path);                        // is directory check
                item.setDirectory(isDir);
                size = (isDir ? 0L : Files.size(path));                 // size
                item.setSize(size);

                int p;
                String full;
                if (isSubscriber() && context.cfg.isRemoteSubscriber())
                    full = context.cfg.getFullPathSubscriber(base);
                else
                    full = Utils.getFullPathLocal(base);
                p = full.length() + 1;

                itemPath = fullPath.substring(p);                       // item path
                item.setItemPath(itemPath);
                isSym = Files.isSymbolicLink(path);                     // is symbolic link check
                item.setSymLink(isSym);
                item.setLibrary(library.name);                          // the library name
                item.setModifiedDate(Files.getLastModifiedTime(path));  // modified date

                if (!Utils.isFileOnly(item.getItemPath()))
                {
                    item.setItemSubdirectory(Utils.pipe(this, Utils.getLeftPath(item.getItemPath(), getSeparator())));
                }

                item.setItemShortName(Utils.getShortPath(item.getItemPath(), getSeparator()));
                library.items.add(item);

                if (isDir)
                {
                    // track item count in a directory item's size
                    item.setSize(scanDirectory(library, base, item.getFullPath()));
                }

            }
        }
        catch (IOException ioe)
        {
            throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.exception.reading.directory.trace"), directory) + Utils.getStackTrace(ioe));
        }
        library.rescanNeeded = false;
        return count;
    }

    /**
     * Scan the sources of a library.
     * <p>
     * Used by the public scan methods.
     *
     * @param lib Library to scan
     * @throws MungeException From scanDirectory()
     */
    private void scanSources(Library lib) throws MungeException
    {
        logger.info((lib.rescanNeeded ? context.cfg.gs("Repository.rescan.required") : context.cfg.gs("Repository.scanning"))
                + getLibraryData().libraries.description + ": " + lib.name);
        lib.items = null;
        for (String src : lib.sources)
        {
            logger.info("  src: " + src);
            scanDirectory(lib, src, src);
        }
    }

    /**
     * Set this Repository as dynamically loaded
     *
     * @param sense
     */
    public void setDynamic(boolean sense)
    {
        this.dynamic = sense;
    }

    /**
     * Sets LibraryData file.
     *
     * @param jsonFilename of the LibraryData file
     */
    public void setJsonFilename(String jsonFilename)
    {
        this.jsonFilename = jsonFilename;
    }

    /**
     * Sort a specific library's items.
     */
    public void sort(Library lib)
    {
        lib.items.sort((item1, item2) -> item1.getItemPath().compareToIgnoreCase(item2.getItemPath()));
    }

    /**
     * Sort all libraries in collection.
     */
    public void sortAll()
    {
        for (Library lib : getLibraryData().libraries.bibliography)
        {
            sort(lib);
        }
    }

    @Override
    public String toString()
    {
        return (libraryData != null && libraryData.libraries != null) ? libraryData.libraries.description : "";
    }

    /**
     * Validate LibraryData.
     *
     * @throws MungeException the els exception
     */
    public void validate() throws Exception
    {
        if (getLibraryData() == null)
        {
            throw new MungeException(context.cfg.gs("Repository.libraries.are.null"));
        }

        Libraries lbs = getLibraryData().libraries;
        if (lbs == null)
        {
            throw new MungeException(context.cfg.gs("Repository.libraries.must.be.defined"));
        }

        if (lbs.description == null || lbs.description.length() == 0)
        {
            throw new MungeException(context.cfg.gs("Repository.libraries.description.must.be.defined"));
        }
        if (lbs.case_sensitive == null)
        {
            throw new MungeException(context.cfg.gs("Repository.libraries.case.sensitive.true.false.must.be.defined"));
        }

        if (lbs.ignore_patterns != null && lbs.ignore_patterns.length > 0)
        {
            Pattern patt = null;
            String src = null;
            try
            {
                for (String s : lbs.ignore_patterns)
                {
                    src = s;
                    patt = Pattern.compile(src);
                    lbs.compiledPatterns.add(patt);
                }
            }
            catch (PatternSyntaxException pe)
            {
                throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.ignore.pattern.has.bad.regular.expression.regex.syntax"), src));
            }
            catch (IllegalArgumentException iae)
            {
                throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.ignore.pattern.has.bad.flags"), src));
            }
        }

        if (getLibraryData().libraries.bibliography != null)
        {
            logger.info(MessageFormat.format(context.cfg.gs("Repository.validating.libraries.in"), lbs.description) + getJsonFilename());
            for (int i = 0; i < lbs.bibliography.length; i++)
            {
                Library lib = lbs.bibliography[i];
                if (lib.name == null || lib.name.length() == 0)
                {
                    throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.bibliography.name.must.be.defined"), i));
                }
                if (lib.sources == null || lib.sources.length == 0)
                {
                    throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.bibliography.sources.must.be.defined"), i));
                }
                else
                {
                    if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(lib.name)) &&
                            (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(lib.name)))
                    {
                        logger.info("  library: " + lib.name +
                                ", " + lib.sources.length + " source" + ((lib.sources.length > 1) ? "s" : "") +
                                (lib.items != null && lib.items.size() > 0 ? ", " + lib.items.size() + " item" + (lib.items.size() > 0 ? "s" : "") : ""));
                        // validate sources paths
                        for (int j = 0; j < lib.sources.length; j++)
                        {
                            if (lib.sources[j].length() == 0)
                            {
                                throw new MungeException(MessageFormat.format(context.cfg.gs("Repository.bibliography.source.path.must.be.defined"), i,j));
                            }
                            if (Files.notExists(Paths.get(Utils.getFullPathLocal(lib.sources[j]))))
                            {
                                String msg = MessageFormat.format(context.cfg.gs("Repository.bibliography.sources.does.not.exist"), i,j,lib.sources[j]);
                                if (context.cfg.isGui())
                                    logger.error(msg);
                                else
                                    throw new MungeException(msg);
                            }
                            logger.info("    src: " + lib.sources[j]);

                            // validate item path
                            if (lib.items != null && lib.items.size() > 0)
                            {
                                for (Item item : lib.items)
                                {
                                    if (Files.notExists(Paths.get(item.getFullPath())))
                                    {
                                        logger.error(context.cfg.gs("Repository.file.does.not.exist") + item.getFullPath());
                                    }
                                    else
                                    {
                                        if (!item.isDirectory() && Files.size(Paths.get(item.getFullPath())) != item.getSize())
                                        {
                                            logger.error(context.cfg.gs("Repository.file.size.does.not.match.file.is") + Files.size(Paths.get(item.getFullPath())) + ", data has " + item.getSize() + ": " + item.getFullPath());
                                        }
                                    }
                                    if (!item.getLibrary().equals(lib.name))
                                    {
                                        logger.error(context.cfg.gs("Repository.file.library.does.not.match.file.is.in") + lib.name + ", data has " + item.getLibrary() + ": " + item.getFullPath());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void write() throws Exception
    {
        try
        {
            File f = new File(Utils.getFullPathLocal(getJsonFilename()));
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }

            Gson gson = new GsonBuilder().serializeNulls().create();
            JsonWriter jsonWriter = new JsonWriter(new FileWriter(f.getPath()));
            jsonWriter.setIndent("    ");
            gson.toJson(libraryData, LibraryData.class, jsonWriter);
            jsonWriter.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(context.cfg.gs("Z.error.writing") + getJsonFilename() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
