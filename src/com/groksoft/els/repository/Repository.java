package com.groksoft.els.repository;

import com.google.common.collect.ArrayListMultimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * The type Repository
 */
public class Repository
{
    public static final boolean NO_VALIDATE = false;
    public static final int PUBLISHER = 1;
    public static final int SUBSCRIBER = 2;
    public static final int HINT_SERVER = 3;
    public static final boolean VALIDATE = true;

    public final String SUB_EXCLUDE = "ELS-SUBSCRIBER-SKIP_";
    private transient Configuration cfg = null;
    private String jsonFilename = "";
    private LibraryData libraryData = null;
    private transient Logger logger = LogManager.getLogger("applog");
    private int purpose = -1;

    /**
     * Instantiate a new empty Repository
     *
     * @param config Configuration
     */
//    public Repository(Configuration config)
//    {
//        this.cfg = config;
//    }

    /**
     * Instantiate a Repository with a purpose
     * @param config Configuration
     * @param purpose One of PUBLISHER, SUBSCRIBER, HINT_SERVER
     */
    public Repository(Configuration config, int purpose)
    {
        this.cfg = config;
        this.purpose = purpose;
    }

    /**
     * Create an empty repository object.
     * <p>
     * Description and key are empty strings.<br>
     * Libraries are created but are empty/null.
     *
     * @param cfg Configuration
     * @param numOfLibraries The number of empty libraries to create
     * @return Empty Repository object
     */
//    public static Repository createEmptyRepository(Configuration cfg, int numOfLibraries)
//    {
//        Repository repo = new Repository(cfg);
//        repo.libraryData = new LibraryData();
//        repo.libraryData.libraries = new Libraries();
//        repo.libraryData.libraries.description = "";
//        repo.libraryData.libraries.key = "";
//        repo.libraryData.libraries.bibliography = new Library[numOfLibraries];
//        for (int i = 0; i < numOfLibraries; ++i)
//        {
//            repo.libraryData.libraries.bibliography[i] = new Library();
//        }
//        return repo;
//    }

    /**
     * Export library items to JSON collection file.
     *
     * @throws MungeException the els exception
     */
    public void exportItems(boolean isCollection) throws MungeException
    {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        logger.info("Writing " + (isCollection ? "collection" : "library") + " file " + cfg.getExportCollectionFilename());
        json = gson.toJson(libraryData);
        try
        {
            PrintWriter outputStream = new PrintWriter(cfg.getExportCollectionFilename());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException("Exception while writing " + (isCollection ? "collection" : "library") + " file " + cfg.getExportCollectionFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Export library items to text file.
     *
     * @throws MungeException the els exception
     */
    public void exportText() throws MungeException
    {
        logger.info("Writing text file " + cfg.getExportTextFilename());

        try
        {
            PrintWriter outputStream = new PrintWriter(cfg.getExportTextFilename());
            for (Library lib : libraryData.libraries.bibliography)
            {
                if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                        (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
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
            throw new MungeException("Exception while writing text file " + cfg.getExportTextFilename() + " trace: " + Utils.getStackTrace(fnf));
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
     * Sets LibraryData file.
     *
     * @param jsonFilename of the LibraryData file
     */
    public void setJsonFilename(String jsonFilename)
    {
        this.jsonFilename = jsonFilename;
    }

    /**
     * Get specific library
     * <p>
     * Do these Libraries have a particular Library?
     *
     * @param libraryName the library name
     * @return the Library
     */
    public Library getLibrary(String libraryName) throws MungeException
    {
        boolean has = false;
        Library retLib = null;
        for (Library lib : libraryData.libraries.bibliography)
        {
            if (lib.name.equalsIgnoreCase(libraryName))
            {
                if (has) // check for a duplicate library name
                {
                    throw new MungeException("Library " + lib.name + " found more than once in " + getJsonFilename());
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
            if (!libraryData.libraries.case_sensitive)
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
        if (sep.equalsIgnoreCase("\\\\"))
            sep = "\\";
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
        return Utils.getFileSeparator(libraryData.libraries.flavor);
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
        for (Library lib : libraryData.libraries.bibliography)
        {
            if (lib.name.equalsIgnoreCase(libraryName))
            {
                foundItem = null;
                if (lib.items != null)
                {
                    // has to be a linear search because directories are not placed in the itemMap hash map
                    for (Item item : lib.items)
                    {
                        if (libraryData.libraries.case_sensitive)
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
                    logger.warn("Subscriber library '" + lib.name + "' has no items. Is command-line configured correctly?");
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
     * Has specific item true/false.
     * <p>
     * String match is expected to have been converted to pipe character file separators using Utils.pipe().
     * The item "has" member contains all instances including self.
     *
     * @param pubItem  the publisher item being found, for adding 'has' items
     * @param itemPath the itemPath() of the item to find
     * @return the boolean
     */
    public Item hasItem(Item pubItem, String libName, String itemPath) throws MungeException
    {
        Item has = null;

        if (pubItem == null || !pubItem.isDirectory())
        {
            for (Library lib : libraryData.libraries.bibliography)
            {
                if (cfg.isCrossCheck() || lib.name.equalsIgnoreCase(libName))
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
                                        logger.warn("  ! Duplicate of \"" + itemPath + "\" found at \"" + item.getFullPath() + "\"");
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
        for (Library lib : libraryData.libraries.bibliography)
        {
            if (cfg.isCrossCheck() || lib.name.equalsIgnoreCase(pubItem.getLibrary()))
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
                                logger.warn("  ! Duplicate of \"" + pubItem.getFullPath() + "\" found at \"" + item.getFullPath() + "\"");
                            }
                        }
                    }
                }
                else
                {
                    throw new MungeException("itemMap is null for library " + lib.name);
                }
            }
        }
    }

    /**
     * Determine if item should be ignored
     *<br/>
     * Examples:
     * <ul>
     *   <li>Ignore any case of "desktop.ini" = "(?i)desktop\\.ini"</li>
     *   <li>Ignore "*.srt" files = ".*\\.srt"</li>
     *   <li>Ignore directory "/Plex Versions" = ".*\\/Plex Versions.*"</li>
     *   <li>Ignore directory "Plex Versions/" = ".*Plex Versions\\/.*"</li>
     * </ul>
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
     * Is Initialized indicator
     *
     * @returns boolean true/false
     */
    public boolean isInitialized()
    {
        if (this.libraryData != null && (this.jsonFilename != null && this.jsonFilename.length() > 0))
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
        if (libraryData != null)
        {
            // if listen is empty use host
            if (libraryData.libraries.listen == null ||
                    libraryData.libraries.listen.length() < 1)
            {
                libraryData.libraries.listen = libraryData.libraries.host;
            }

            String flavor = libraryData.libraries.flavor.toLowerCase();
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

            if (libraryData.libraries.temp_location != null)
            {
                String path = libraryData.libraries.temp_location;
                if (path.startsWith("~"))
                {
                    path = System. getProperty("user.home") + path.substring(1);
                    libraryData.libraries.temp_location = path;
                }
            }

            if (libraryData.libraries.bibliography != null)
            {
                for (Library lib : libraryData.libraries.bibliography)
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
                            if (!libraryData.libraries.case_sensitive)
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
        path = normalizeSubst(path, Utils.getFileSeparator(libraryData.libraries.flavor), to);
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
        return path.replaceAll(from, to).replaceAll("\\|", to);
    }

    /**
     * Read library.
     *
     * @param filename The JSON Libraries filename
     * @throws MungeException the els exception
     */
    public void read(String filename, boolean printLog) throws MungeException
    {
        try
        {
            String json;
            if (libraryData != null)
                libraryData = null;
            Gson gson = new Gson();
            if (printLog)
                logger.info("Reading Library file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            libraryData = gson.fromJson(json, LibraryData.class);
            normalize();
            if (printLog)
                logger.info("Read \"" + libraryData.libraries.description + "\" successfully");
        }
        catch (IOException ioe)
        {
            throw new MungeException("Exception while reading library " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
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
            if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                    (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
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
        for (Library lib : libraryData.libraries.bibliography)
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
                itemPath = fullPath.substring(base.length() + 1);       // item path
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
            throw new MungeException("Exception reading directory " + directory + " trace: " + Utils.getStackTrace(ioe));
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
        logger.info("Scanning " + getLibraryData().libraries.description + ": " + lib.name);
        lib.items = null;
        for (String src : lib.sources)
        {
            logger.info("  " + src);
            scanDirectory(lib, src, src);
        }
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
        for (Library lib : libraryData.libraries.bibliography)
        {
            sort(lib);
        }
    }

    /**
     * Validate LibraryData.
     *
     * @throws MungeException the els exception
     */
    public void validate() throws Exception
    {
        if (libraryData == null)
        {
            throw new MungeException("Libraries are null");
        }

        Libraries lbs = libraryData.libraries;
        if (lbs == null)
        {
            throw new MungeException("libraries must be defined");
        }

        if (lbs.description == null || lbs.description.length() == 0)
        {
            throw new MungeException("libraries.description must be defined");
        }
        if (lbs.case_sensitive == null)
        {
            throw new MungeException("libraries.case_sensitive true/false must be defined");
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
                throw new MungeException("Ignore pattern '" + src + "' has bad regular expression (regex) syntax");
            }
            catch (IllegalArgumentException iae)
            {
                throw new MungeException("Ignore pattern '" + src + "' has bad flags");
            }
        }

        if (libraryData.libraries.bibliography != null)
        {
            //if (lbs.bibliography == null)  // TODO does this work?
            //{
            //    throw new MungeException("libraries.bibliography must be defined");
            //}

            logger.info("Validating " + lbs.description + " Libraries in: " + getJsonFilename());
            for (int i = 0; i < lbs.bibliography.length; i++)
            {
                Library lib = lbs.bibliography[i];
                if (lib.name == null || lib.name.length() == 0)
                {
                    throw new MungeException("bibliography.name " + i + " must be defined");
                }
                if (lib.sources == null || lib.sources.length == 0)
                {
                    throw new MungeException("bibliography.sources " + i + " must be defined");
                }
                else
                {
                    if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(lib.name)) &&
                            (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(lib.name)))
                    {
                        logger.info("  library: " + lib.name +
                                ", " + lib.sources.length + " source" + ((lib.sources.length > 1) ? "s" : "") +
                                (lib.items != null && lib.items.size() > 0 ? ", " + lib.items.size() + " item" + (lib.items.size() > 0 ? "s" : "") : ""));
                        // validate sources paths
                        for (int j = 0; j < lib.sources.length; j++)
                        {
                            if (lib.sources[j].length() == 0)
                            {
                                throw new MungeException("bibliography[" + i + "].sources[" + j + "] must be defined");
                            }
                            if (Files.notExists(Paths.get(lib.sources[j])))
                            {
                                throw new MungeException("bibliography[" + i + "].sources[" + j + "]: " + lib.sources[j] + " does not exist");
                            }
                            logger.info("    src: " + lib.sources[j]);

                            // validate item path
                            if (lib.items != null && lib.items.size() > 0)
                            {
                                for (Item item : lib.items)
                                {
                                    if (Files.notExists(Paths.get(item.getFullPath())))
                                    {
                                        logger.error("File does not exist: " + item.getFullPath());
                                    }
                                    else
                                    {
                                        if (!item.isDirectory() && Files.size(Paths.get(item.getFullPath())) != item.getSize())
                                        {
                                            logger.error("File size does not match, file is " + Files.size(Paths.get(item.getFullPath())) + ", data has " + item.getSize() + ": " + item.getFullPath());
                                        }
                                    }
                                    if (!item.getLibrary().equals(lib.name))
                                    {
                                        logger.error("File library does not match, file is in " + lib.name + ", data has " + item.getLibrary() + ": " + item.getFullPath());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
