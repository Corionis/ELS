package com.groksoft;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

// see https://github.com/google/gson
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.Utils;
import com.groksoft.Item;

/**
 * The type Collection.
 */
public class Collection
{
    private Logger logger = LogManager.getLogger("applog");
    private String collectionFile = "";
    private Configuration cfg = null;
    private Control control = null;
    private List<Item> items = new ArrayList<>();

// Methods:
    // A load method to read a collection.control file
    // A validate method to check the syntax and existence of the elements in a collection.control file
    // A scanAll method to scanAll and generate the set of Item objects
    // A sort method, by context
    // A duplicates method to check for duplicate contexts in the Collection - possibly enforced by the selected Java collection requiring a unique key

    /**
     * Instantiates a new Collection.
     */
    public Collection() {
        cfg = Configuration.getInstance();
    }

    /**
     * Read control.
     *
     * @param filename the filename
     * @throws MongerException the monger exception
     */
    public void readControl(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading collection file " + filename);
            setCollectionFile(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            control = gson.fromJson(json, Control.class);

        } catch (Exception e) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(e));
        }
    }

    /**
     * Validate control.
     *
     * @throws MongerException the monger exception
     */
    public void validateControl() throws MongerException {
        String s;
        boolean b;
        String itemName = "";
        long minimumSize = -1;

        if (getControl() == null) {
            throw new MongerException("Control is null");
        }

        try {
            if (control.metadata.name == null || control.metadata.name.length() == 0) {
                throw new MongerException("metadata.name must be defined");
            }
            if (control.metadata.case_sensitive == null) {
                throw new MongerException("metadata.case_sensitive true/false must be defined");
            }

            for (int i = 0; i < control.libraries.length; i++) {
                if (control.libraries[i].definition == null) {
                    throw new MongerException("libraries.definition[" + i + "] must be defined");
                }
                if (control.libraries[i].definition.name == null || control.libraries[i].definition.name.length() == 0) {
                    throw new MongerException("library[" + i + "].name must be defined");
                }
                // minimum is optional
                if (control.libraries[i].definition.minimum != null && control.libraries[i].definition.minimum.length() > 0) {
                    minimumSize = Utils.getScaledValue(control.libraries[i].definition.minimum);
                    if (minimumSize == -1) {
                        throw new MongerException("control.libraries[" + i + "].definition.minimum is invalid");
                    }
                }
                if (minimumSize < (1024 * 1024)) {
                    minimumSize = 1024 * 1024; // a proper 1 megabyte value
                }

                if (control.libraries[i].sources == null || control.libraries[i].sources.length == 0) {
                    throw new MongerException("libraries[" + i + "].sources must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < control.libraries[i].sources.length; j++) {
                        if (control.libraries[i].sources[j].length() == 0) {
                            throw new MongerException("libraries[" + i + "].sources[" + j + "] must be defined");
                        }
                        if (Files.notExists(Paths.get(control.libraries[i].sources[j]))) {
                            throw new MongerException("control.libraries[" + i + "].sources[" + j + "]: " + control.libraries[i].sources[j] + " does not exist");
                        }
                        logger.debug("DIR: " + control.libraries[i].sources[j]);
                    }
                }
                if (control.libraries[i].targets == null || control.libraries[i].targets.length == 0) {
                    throw new MongerException("libraries.sources[" + i + "] must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < control.libraries[i].targets.length; j++) {
                        if (control.libraries[i].targets[j].length() == 0) {
                            throw new MongerException("libraries[" + i + "].targets[" + j + "] must be defined");
                        }
                        if (Files.notExists(Paths.get(control.libraries[i].targets[j]))) {
                            throw new MongerException("control.libraries[" + i + "].targets[" + j + "]: " + control.libraries[i].targets[j] + " does not exist");
                        }
                        logger.debug("DIR: " + control.libraries[i].targets[j]);
                    }
                }
            }
//
// EXCEPTION HANDLING point/question: Right now all exceptions are caught and generically re-thrown as MongerExceptions.
// Should discrete exceptions be detected so CODE exceptions can include getStackTrace() and simple message exceptions
// do not need to dump out the stack?  It is not consistent yet.  And a stack trace is confusing with a simple text
// text like "such 'n such must be defined".
//
        } catch (Exception e) {
            throw new MongerException("Exception while validating " + getCollectionFile() + " item " + itemName + " trace: " + Utils.getStackTrace(e));
        }

        logger.info("Validation successful");
    }

    /**
     * Scan All libraries.
     *
     * @throws MongerException the monger exception
     */
    public void scanAll() throws MongerException {
        // Traverse the library and get the media directories
        for (int i = 0; i < control.libraries.length; i++) {

            // todo decide if a single library was specified

            for (int j = 0; j < control.libraries[i].sources.length; j++) {
                scanDirectory(control.libraries[i].sources[j]);
            }
        }

        // todo sort items

        if (cfg.getExportFilename().length() < 1) {
            // todo write out to file
            // Idea: Export to a JSON file; then a load of that file creates an ArrayList of Items
        }
    }

    /**
     * Scan a specific directory, recursively.
     *
     * @param directory the directory
     * @throws MongerException the monger exception
     */
    private void scanDirectory(String directory) throws MongerException {
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
                itemPath = fullPath.substring(directory.length() + 1);  // item path
                item.setItemPath(itemPath);
                isSym = Files.isSymbolicLink(path);                     // is symbolic link check
                item.setSymLink(isSym);
                this.items.add(item);
                logger.debug(entry.toString());
                if (isDir) {
                    scanDirectory(item.getFullPath());
                }
            }
        } catch (Exception e) {
            throw new MongerException("Error Reading Directory " + directory);
        }
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
     * Gets control.
     *
     * @return the control
     */
    public Control getControl() {
        return control;
    }

    //==================================================================================================================

    /**
     * Classes used in the JSON to Object translations
     */
    public class Control
    {
        /**
         * The Metadata.
         */
        Metadata metadata;
        /**
         * The Libraries.
         */
        Libraries[] libraries;
    }

    /**
     * The type Metadata.
     */
    public class Metadata
    {
        /**
         * The Name.
         */
        public String name;
        /**
         * The Case sensitive.
         */
        public Boolean case_sensitive;
    }

    /**
     * The type Libraries.
     */
    public class Libraries
    {
        /**
         * The Definition.
         */
        public Definition definition;
        /**
         * The Sources.
         */
        public String[] sources;
        /**
         * The Targets.
         */
        public String[] targets;

    }

    /**
     * The type Definition.
     */
    public class Definition
    {
        /**
         * The Name.
         */
        public String name;
        /**
         * The Minimum.
         */
        public String minimum;
    }

}
