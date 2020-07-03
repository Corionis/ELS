package com.groksoft.volmunger.repository;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.google.gson.Gson;                    // see https://github.com/google/gson

// see https://logging.apache.org/log4j/2.x/
import com.google.gson.GsonBuilder;
import com.groksoft.volmunger.MungerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.Utils;

/**
 * The type Repository.
 */
public class Repository
{
    private transient Logger logger = LogManager.getLogger("applog");
    private transient Configuration cfg = null;

    public static final boolean PUBLISHER = true;
    public static final boolean SUBSCRIBER = false;
    public static final boolean VALIDATE = true;
    public static final boolean NO_VALIDATE = false;

    // LibraryData members
    private LibraryData libraryData = null;
    private String jsonFilename = "";

    /**
     * Instantiates a new Collection.
     *
     * @param config Configuration
     */
    public Repository(Configuration config) {
        cfg = config;
    }

    /**
     * Dump collection.
     */
    public void dump() {
        System.out.println("  Libraries from " + getJsonFilename());
        System.out.println("    Description: " + libraryData.libraries.description);
        System.out.println("           Site: " + libraryData.libraries.site);
        System.out.println("            Key: " + libraryData.libraries.key);
        System.out.println("    Case-sensitive: " + libraryData.libraries.case_sensitive);
        System.out.println("    Ignore patterns:");
        for (String patt : libraryData.libraries.ignore_patterns) {
            System.out.println("      " + patt);
        }
        System.out.println("    Bibliography:");
        for (Library lib : libraryData.libraries.bibliography) {
            System.out.println("      Name: " + lib.name);
            System.out.println("      Sources:");
            for (String src : lib.sources) {
                System.out.println("        " + src);
            }
        }
    }

    /**
     * @param item
     * @return
     */
    private boolean ignore(Item item) {
        String str = "";
        String str1 = "";
        boolean ret = false;

        for (Pattern patt : getLibraryData().libraries.compiledPatterns) {

            str = patt.toString();
            str1 = str.replace("?", ".?").replace("*", ".*?");
            if (item.getName().matches(str1)) {
                //logger.info(">>>>>>Ignoring '" + item.getName());
                //ignoreTotal++;
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * Export libraries to text.
     *
     * @throws MungerException the volmunger exception
     */
    public void exportText() throws MungerException {
        String path;
        logger.info("Writing paths file " + cfg.getExportTextFilename());


        try {
            PrintWriter outputStream = new PrintWriter(cfg.getExportTextFilename());
            for (Library lib : libraryData.libraries.bibliography) {
                for (Item item : lib.items) {
                    if( !item.isDirectory() ) {
                        if( !ignore(item) ) {
                            outputStream.println(item.getItemPath());
                        }
                    }
                }
            }

            outputStream.close();
        } catch (FileNotFoundException fnf) {
            throw new MungerException("Exception while writing text file " + cfg.getExportTextFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Export libraries to JSON.
     *
     * @throws MungerException the volmunger exception
     */
    public void exportCollection() throws MungerException {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;
        logger.info("Writing collection file " + cfg.getExportCollectionFilename());
        json = gson.toJson(libraryData);
        try {
            PrintWriter outputStream = new PrintWriter(cfg.getExportCollectionFilename());
            outputStream.println(json);
            outputStream.close();
        } catch (FileNotFoundException fnf) {
            throw new MungerException("Exception while writing collection file " + cfg.getExportCollectionFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Has directory string.
     *
     * @param libraryName the library name
     * @param itemPath       the match
     * @return the string
     */
    public String hasDirectory(String libraryName, String itemPath) {
        String match = itemPath;
        Item foundItem = null;
        int i = match.lastIndexOf(File.separator);
        if (i < 0) {
            return null;
        }
        String path = match.substring(0, i);
        if (path.length() < 1) {
            path = match.substring(0, match.lastIndexOf(File.separator));
        }
        match = path;
        path = null;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                foundItem = null;
                for (Item item : lib.items) {
                    if (libraryData.libraries.case_sensitive) {
                        if (item.getItemPath().equals(match)) {
                            foundItem = item;
                            break;
                        }
                    } else {
                        if (item.getItemPath().equalsIgnoreCase(match)) {
                            foundItem = item;
                            break;
                        }
                    }
                }
                if (foundItem != null && foundItem.isDirectory()) {
                    path = foundItem.getFullPath().substring(0, foundItem.getFullPath().lastIndexOf(foundItem.getItemPath()) - 1);
                }
                break;
            }
        }
        return path;
    }

/*
    public String hasDirectory(String libraryName, String match) {
        Item foundItem = null;
        int i = match.lastIndexOf(File.separator);
        if (i < 0) {
            return null;
        }
        String path = match.substring(0, i);
        if (path.length() < 1) {
            path = match.substring(0, match.lastIndexOf(File.separator));
        }
        match = path;
        path = null;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                for (Item item : lib.items) {
                    if (libraryData.libraries.case_sensitive) {
                        if (item.getItemPath().equals(match)) {
                            foundItem = item;
                            break;
                        }
                    } else {
                        if (item.getItemPath().equalsIgnoreCase(match)) {
                            foundItem = item;
                            break;
                        }
                    }
                }
                if (foundItem != null) {
                    if (foundItem.isDirectory()) {
                        path = foundItem.getFullPath();
                        String segment;
                        while(true) {
                            // logger.info(">>>>>>>> Checking hasDirectory for "+path);
                            try {
                                segment = path.substring(0, path.lastIndexOf(File.separator));
                            } catch (Exception e) {
                                logger.info("Library name error. No library '"+ libraryName +"' found in path '"+ foundItem.getFullPath() +"'" );
                                throw e;
                            }
                            // logger.info(">>>>>>>> segment is: '"+segment + "'");
                            if (segment !=  "" && segment.length() < 1) {
                                segment = foundItem.getFullPath().substring(0, foundItem.getFullPath().lastIndexOf(File.separator));
                            }
                            path = segment;
                            String s = segment.substring(segment.lastIndexOf(File.separator) + 1, segment.length());
                            if (segment.substring(segment.lastIndexOf(File.separator) + 1, segment.length()).equalsIgnoreCase(libraryName) ) {
                                break;
                            }
                        }
                    }
                    break;  // break outer loop also
                }
            }
        }
        return path;
    }
*/

    /**
     * Has specific item
     * <p>
     * Does this Library have a particular item?
     *
     * @param libraryName the library name
     * @param match       the match
     * @return the boolean
     */
    public boolean hasItem(String libraryName, String match) {
        Item has = null;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                for (Item item : lib.items) {
                    if (libraryData.libraries.case_sensitive) {
                        if (item.getItemPath().equals(match)) {
                            has = item;
                            break;
                        }
                    } else {
                        if (item.getItemPath().equalsIgnoreCase(match)) {
                            has = item;
                            break;
                        }
                    }
                }
                if (has != null) {
                    break;  // break outer loop also
                }
            }
        }
        return has != null;
    }

    /**
     * Has specific library
     * <p>
     * Do these Libraries have a particular one?
     *
     * @param libraryName the library name
     * @return the boolean
     */
    public boolean hasLibrary(String libraryName) throws MungerException {
        boolean has = false;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                if (has) {
                    throw new MungerException("Library " + lib.name + " found more than once in " + getJsonFilename());
                }
                has = true;
            }
        }
        return has;
    }

    /**
     * Get specific library
     * <p>
     * Do these Libraries have a particular Library?
     *
     * @param libraryName the library name
     * @return the Library
     */
    public Library getLibrary(String libraryName) throws MungerException {
        boolean has = false;
        Library retLib = null;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                if (has) {
                    throw new MungerException("Library " + lib.name + " found more than once in " + getJsonFilename());
                }
                has = true;
                retLib = lib;
            }
        }
        return retLib;
    }

    /**
     * Is Initialized indicator
     *
     * @returns boolean true/false
     */
    public boolean isInitialized()
    {
        if (this.libraryData != null &&
                (this.jsonFilename != null && this.jsonFilename.length() > 0))
            return true;
        else
            return false;
    }


    /**
     * Read library.
     *
     * @param filename The JSON Libraries filename
     * @throws MungerException the volmunger exception
     */
    public void read(String filename) throws MungerException {
        try {
            String json;
            if (libraryData != null)
                libraryData = null;
            Gson gson = new Gson();
            logger.info("Reading Libraries file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            libraryData = gson.fromJson(json, LibraryData.class);
        } catch (IOException ioe) {
            throw new MungerException("Exception while reading libraries " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Scan a specific library name.
     *
     * @throws MungerException the volmunger exception
     */
    public void scan(String libraryName) throws MungerException {

        for (Library lib : libraryData.libraries.bibliography) {
            if (libraryName.length() > 0) {
                if (!libraryName.equalsIgnoreCase(lib.name))
                    continue;
            }
            logger.info("Scanning " + getLibraryData().libraries.description + ": " + lib.name);
            for (String src : lib.sources) {
                logger.info("  " + src);
                scanDirectory(lib, src, src);
            }
            sort(lib);
        }
    }

    /**
     * Scan a specific directory, recursively.
     *
     * @param directory the directory
     * @throws MungerException the volmunger exception
     */
    private void scanDirectory(Library library, String base, String directory) throws MungerException {
        Item item = null;
        String fullPath = "";
        String itemPath = "";
        boolean isDir = false;
        boolean isSym = false;
        Path path = Paths.get(directory);

        if (library.items != null) {
            library.items = null; // clear any existing data
        }
        library.items = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            for (Path entry : directoryStream) {
                item = new Item();
                fullPath = entry.toString();                            // full path
                item.setFullPath(fullPath);
                path = Paths.get(fullPath);
                isDir = Files.isDirectory(path);                        // is directory check
                item.setDirectory(isDir);
                itemPath = fullPath.substring(base.length() + 1);       // item path
                item.setItemPath(itemPath);
                isSym = Files.isSymbolicLink(path);                     // is symbolic link check
                item.setSymLink(isSym);
                item.setLibrary(library.name);                          // the library name
                library.items.add(item);
                //logger.debug(entry.toString());
                if (isDir) {
                    scanDirectory(library, base, item.getFullPath());
                }
            }
        } catch (IOException ioe) {
            throw new MungerException("Exception reading directory " + directory + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Sort collection.
     */
    public void sort(Library lib) {
        lib.items.sort((item1, item2) -> item1.getItemPath().compareToIgnoreCase(item2.getItemPath()));
    }


    /**
     * Validate LibraryData.
     *
     * @throws MungerException the volmunger exception
     */
    public void validate() throws MungerException {
        long minimumSize;

        if (libraryData == null) {
            throw new MungerException("Libraries are null");
        }

        Libraries lbs = libraryData.libraries;
        if (lbs == null) {
            throw new MungerException("libraries must be defined");
        }

        if (lbs.description == null || lbs.description.length() == 0) {
            throw new MungerException("libraries.description must be defined");
        }
        if (lbs.case_sensitive == null) {
            throw new MungerException("libraries.case_sensitive true/false must be defined");
        }

        if (lbs.ignore_patterns.length > 0) {
            Pattern patt = null;
            try {
                for (String s : lbs.ignore_patterns) {
                    patt = Pattern.compile(s);
                    lbs.compiledPatterns.add(patt);
                }
            } catch (PatternSyntaxException pe) {
                throw new MungerException("Pattern " + patt + " has bad regular expression (regex) syntax");
            } catch (IllegalArgumentException iae) {
                throw new MungerException("Pattern " + patt + " has bad flags");
            }
        }

        if (lbs.bibliography == null) {
            throw new MungerException("libraries.bibliography must be defined");
        }
        for (int i = 0; i < lbs.bibliography.length; i++) {
            Library lib = lbs.bibliography[i];
            if (lib.name == null || lib.name.length() == 0) {
                throw new MungerException("bibliography.name " + i + " must be defined");
            }
            if (lib.items == null || lib.items.size() == 0) {
                if (lib.sources == null || lib.sources.length == 0) {
                    throw new MungerException("bibliography.sources " + i + " must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < lib.sources.length; j++) {
                        if (lib.sources[j].length() == 0) {
                            throw new MungerException("bibliography[" + i + "].sources[" + j + "] must be defined");
                        }
                        if (Files.notExists(Paths.get(lib.sources[j]))) {
                            throw new MungerException("bibliography[" + i + "].sources[" + j + "]: " + lib.sources[j] + " does not exist");
                        }
                        logger.debug("  src: " + lib.sources[j]);
                    }
                }
            }
        }
        logger.info("Library validation successful: " + getJsonFilename());
    }

    /**
     * Gets LibraryData filename.
     *
     * @return the LibraryData filename
     */
    public String getJsonFilename() {
        return jsonFilename;
    }

    /**
     * Sets LibraryData file.
     *
     * @param jsonFilename of the LibraryData file
     */
    public void setJsonFilename(String jsonFilename) {
        this.jsonFilename = jsonFilename;
    }

    /**
     * Gets LibraryData.
     *
     * @return the library
     */
    public LibraryData getLibraryData() {
        return libraryData;
    }

}
