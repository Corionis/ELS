package com.groksoft.volmonger.repository;

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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmonger.Configuration;
import com.groksoft.volmonger.MongerException;
import com.groksoft.volmonger.Utils;

/**
 * The type Repository.
 */
public class Repository
{
    private transient Logger logger = LogManager.getLogger("applog");
    private transient Configuration cfg = null;

    // LibraryData members
    private LibraryData libraryData = null;
    private String jsonFilename = "";

    /**
     * Instantiates a new Collection.
     */
    public Repository() {
        cfg = Configuration.getInstance();
    }

    /**
     * Dump collection.
     */
    public void dump() {
        System.out.println("  Libraries from " + getJsonFilename());
        System.out.println("    Description: " + libraryData.libraries.description);
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
     * Export libraries to JSON.
     *
     * @throws MongerException the monger exception
     */
    public void export() throws MongerException {
        String json;
        Gson gson = new Gson();
        logger.info("Writing item file " + cfg.getExportFilename());
        json = gson.toJson(libraryData);
        try {
            PrintWriter outputStream = new PrintWriter(cfg.getExportFilename());
            outputStream.println(json);
            outputStream.close();
        } catch (FileNotFoundException fnf) {
            throw new MongerException("Exception while writing item file " + cfg.getExportFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

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
        boolean has = false;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                for (Item item : lib.items) {
                    if (libraryData.libraries.case_sensitive) {
                        if (item.getItemPath().equals(match)) {
                            has = true;
                            break;
                        }
                    } else {
                        if (item.getItemPath().equalsIgnoreCase(match)) {
                            has = true;
                            break;
                        }
                    }
                }
                if (has) {
                    break;  // break outer loop also
                }
            }
        }
        return has;
    }

    /**
     * Has specific library
     * <p>
     * Do these Libraries have a particular Library?
     *
     * @param libraryName the library name
     * @return the boolean
     */
    public boolean hasLibrary(String libraryName) throws MongerException {
        boolean has = false;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                if (has) {
                    throw new MongerException("Library " + lib.name + " found more than once in " + getJsonFilename());
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
    public Library getLibrary(String libraryName) throws MongerException {
        boolean has = false;
        Library retLib = null;
        for (Library lib : libraryData.libraries.bibliography) {
            if (lib.name.equalsIgnoreCase(libraryName)) {
                if (has) {
                    throw new MongerException("Library " + lib.name + " found more than once in " + getJsonFilename());
                }
                has = true;
                retLib = lib;
            }
        }
        return retLib;
    }

    /**
     * Read library.
     *
     * @param filename The JSON Libraries filename
     * @throws MongerException the monger exception
     */
    public void read(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading Libraries file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            libraryData = gson.fromJson(json, LibraryData.class);
        } catch (IOException ioe) {
            throw new MongerException("Exception while reading libraries " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Scan All LibraryData.
     *
     * @throws MongerException the monger exception
     */
    public void scan(String libraryName) throws MongerException {

        for (Library lib : libraryData.libraries.bibliography) {
            if (libraryName.length() > 0) {
                if (!libraryName.equalsIgnoreCase(lib.name))
                    continue;
            }
            for (String src : lib.sources) {
                scanDirectory(lib, src, src);
            }
            sort(lib);
        }
    }

    /**
     * Scan a specific directory, recursively.
     *
     * @param directory the directory
     * @throws MongerException the monger exception
     */
    private void scanDirectory(Library library, String base, String directory) throws MongerException {
        Item item = null;
        String fullPath = "";
        String itemPath = "";
        boolean isDir = false;
        boolean isSym = false;
        Path path = Paths.get(directory);

        if (library.items == null) {
            library.items = new ArrayList<>();
        }

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
            throw new MongerException("Exception reading directory " + directory + " trace: " + Utils.getStackTrace(ioe));
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
     * @throws MongerException the monger exception
     */
    public void validate() throws MongerException {
        long minimumSize;

        if (getLibraryData() == null) {
            throw new MongerException("Libraries are null");
        }

        Libraries lbs = libraryData.libraries;

        if (lbs.description == null || lbs.description.length() == 0) {
            throw new MongerException("libraries.description must be defined");
        }
        if (lbs.case_sensitive == null) {
            throw new MongerException("libraries.case_sensitive true/false must be defined");
        }

        if (lbs.ignore_patterns.length > 0) {
            Pattern patt = null;
            try {
                for (String s : lbs.ignore_patterns) {
                    patt = Pattern.compile(s);
                    lbs.compiledPatterns.add(patt);
                }
            } catch (PatternSyntaxException pe) {
                throw new MongerException("Pattern " + patt + " has bad regular expression (regex) syntax");
            } catch (IllegalArgumentException iae) {
                throw new MongerException("Pattern " + patt + " has bad flags");
            }
        }

        for (int i = 0; i < lbs.bibliography.length; i++) {
            Library lib = lbs.bibliography[i];
            if (lib.name == null || lib.name.length() == 0) {
                throw new MongerException("bibliography.name " + i + " must be defined");
            }
            if (lib.items == null || lib.items.size() == 0) {
                if (lib.sources == null || lib.sources.length == 0) {
                    throw new MongerException("bibliography.sources " + i + " must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < lib.sources.length; j++) {
                        if (lib.sources[j].length() == 0) {
                            throw new MongerException("bibliography[" + i + "].sources[" + j + "] must be defined");
                        }
                        if (Files.notExists(Paths.get(lib.sources[j]))) {
                            throw new MongerException("bibliography[" + i + "].sources[" + j + "]: " + lib.sources[j] + " does not exist");
                        }
                        logger.debug("src: " + lib.sources[j]);
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
     * @param jsonFilename the LibraryData file
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
