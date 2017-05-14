package com.groksoft.volmonger;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

// see https://github.com/google/gson

/**
 * The type Collection.
 */
public class Libraries
{
    private transient Logger logger = LogManager.getLogger("applog");
    private transient Configuration cfg = null;

    // data members
    private Data data = null;                           // internal classes
    private String collectionFile = "";


    // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    // todo Remove items here. Use Libraries to create a Collection that contains Items

    private List<Item> items = new ArrayList<>();

// Methods:
    // A load method to read a collection.library file
    // A validate method to check the syntax and existence of the elements in a collection.library file
    // A scanAllLibraries method to scanAllLibraries and generate the set of Item objects
    // A sort method, by context
    // A duplicates method to check for duplicate contexts in the Collection - possibly enforced by the selected Java collection requiring a unique key

    /**
     * Instantiates a new Collection.
     */
    public Libraries() {
        cfg = Configuration.getInstance();
    }

    /**
     * Read library.
     *
     * @param filename the filename
     * @throws MongerException the monger exception
     */
    public void readLibrary(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading libraries file " + filename);
            setCollectionFile(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            data = gson.fromJson(json, Data.class);
        } catch (IOException ioe) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Validate library.
     *
     * @throws MongerException the monger exception
     */
    public void validateLibrary() throws MongerException {
        long minimumSize;

        if (getData() == null) {
            throw new MongerException("Library is null");
        }

        if (data.metadata.name == null || data.metadata.name.length() == 0) {
            throw new MongerException("metadata.name must be defined");
        }
        if (data.metadata.case_sensitive == null) {
            throw new MongerException("metadata.case_sensitive true/false must be defined");
        }

        for (int i = 0; i < data.libraries.length; i++) {
            if (data.libraries[i].definition == null) {
                throw new MongerException("libraries.definition[" + i + "] must be defined");
            }
            if (data.libraries[i].definition.name == null || data.libraries[i].definition.name.length() == 0) {
                throw new MongerException("library[" + i + "].name must be defined");
            }
            // minimum is optional
            if (data.libraries[i].definition.minimum != null && data.libraries[i].definition.minimum.length() > 0) {
                minimumSize = Utils.getScaledValue(data.libraries[i].definition.minimum);
                if (minimumSize == -1) {
                    throw new MongerException("data.libraries[" + i + "].definition.minimum is invalid");
                }
            }
            // genre is optional

            if (data.libraries[i].sources == null || data.libraries[i].sources.length == 0) {
                throw new MongerException("libraries[" + i + "].sources must be defined");
            } else {
                // Verify paths
                for (int j = 0; j < data.libraries[i].sources.length; j++) {
                    if (data.libraries[i].sources[j].length() == 0) {
                        throw new MongerException("libraries[" + i + "].sources[" + j + "] must be defined");
                    }
                    if (Files.notExists(Paths.get(data.libraries[i].sources[j]))) {
                        throw new MongerException("data.libraries[" + i + "].sources[" + j + "]: " + data.libraries[i].sources[j] + " does not exist");
                    }
                    logger.debug("src: " + data.libraries[i].sources[j]);
                }
            }
        }
        logger.info("Validation successful");
    }

    /**
     * Scan All libraries.
     *
     * @throws MongerException the monger exception
     */
    public void scanAllLibraries() throws MongerException {
        for (int i = 0; i < data.libraries.lengtht; i++) {

            // todo decide if a single library was specified

            for (int j = 0; j < data.libraries[i].sources.length; j++) {
                scanDirectory(data.libraries[i].definition.name, data.libraries[i].sources[j], data.libraries[i].sources[j]);
            }
        }

        if (!cfg.getConsoleLevel().equalsIgnoreCase("off")) {
            System.out.println("PRESORT:");
            dumpCollection();
        }

        sortCollection();

        if (!cfg.getConsoleLevel().equalsIgnoreCase("off")) {
            System.out.println("\r\nSORTED:");
            dumpCollection();
        }
    }t

    /**
     * Scan a specific directory, recursively.
     *
     * @param directory the directory
     * @throws MongerException the monger exception
     */
    private void scanDirectory(String library, String base, String directory) throws MongerException {
        Item item = null;
        String fullPath = "";
        String itemPath = "";
        boolean isDir = false;
        boolean isSym = false;
        Path path = Paths.get(directory);

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
                item.setLibrary(library);                               // the library name
                this.items.add(item);
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
    public void sortCollection() {
        Collections.sort(items, new Comparator<Item>()
        {
            @Override
            public int compare(Item item1, Item item2) {
                return item1.getItemPath().compareToIgnoreCase(item2.getItemPath());
            }
        });
    }

    /**
     * Dump collection.
     */
    public void dumpCollection() {
        Iterator<Item> itemIterator = items.iterator();
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            System.out.println("    " + item.getItemPath());
        }
    }

    /**
     * Export collection.
     *
     * @throws MongerException the monger exception
     */
    public void exportCollection() throws MongerException {
        String json;
        Gson gson = new Gson();



        // todo Use the Collection class


        logger.info("Writing item file " + cfg.getExportFilename());
        ItemExport export = new ItemExport();
        export.library = library;
        export.items = items;
        json = gson.toJson(export);
        try {
            PrintWriter outputStream = new PrintWriter(cfg.getExportFilename());
            outputStream.println(json);
            outputStream.close();
        } catch (FileNotFoundException fnf) {
            throw new MongerException("Exception while writing item file " + cfg.getExportFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    /**
     * Import items.
     *
     * @param filename the filename
     * @throws MongerException the monger exception
     */
    public void importItems(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading item file " + filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            ItemExport itemExport = gson.fromJson(json, ItemExport.class);
            setCollectionFile(filename);
        } catch (IOException ioe) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Has boolean.
     * <p>
     * Does this Collection have an item with an itemPath the same as the passed itemPath?
     *
     * @param path the path
     * @return the boolean
     */
    public boolean has(String path) {
        boolean has = false;
        Iterator<Item> iterator = getItems().iterator();
        while (iterator.hasNext()) {
            Item item = iterator.next();
            if (getLibrary().metadata.case_sensitive) {
                if (path.equals(item.getItemPath())) {
                    has = true;
                    break;
                }
            } else {
                if (path.equalsIgnoreCase(item.getItemPath())) {
                    has = true;
                    break;
                }
            }
        }
        return has;
    }

    /**
     * Gets collection file.
     *
     * @return the collection file
     */
    public String getCollectionFile() {
        return collectionFile;
    }

    /**
     * Sets collection file.
     *
     * @param collectionFile the collection file
     */
    public void setCollectionFile(String collectionFile) {
        this.collectionFile = collectionFile;
    }

    /**
     * Gets data.
     *
     * @return the library
     */
    public Data getData() {
        return data;
    }

    /**
     * Gets items.
     *
     * @return the items
     */
    public List<Item> getItems() {
        return items;
    }

    /**
     * Sets items.
     *
     * @param items the items
     */
    public void setItems(List<Item> items) {
        this.items = items;
    }

    //==================================================================================================================
    // Inner classes for Gson libraries file

    public class Data
    {
        /**
         * The Libraries Metadata.
         */
        public Metadata metadata;

        /**
         * The libraries.
         */
        public Library[] libraries;
    }

    /**
     * The type Metadata for Libraries.
     */
    public class Metadata
    {
        /**
         * The Name.
         */
        public String name;

        /**
         * If case-sensitive.
         */
        public Boolean case_sensitive;

        /**
         * Ignore patterns.
         */
        public String[] ignore_patterns;
    }

}
