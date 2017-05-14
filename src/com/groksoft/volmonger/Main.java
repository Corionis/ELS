package com.groksoft.volmonger;

// see https://logging.apache.org/log4j/2.x/

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Main - main program
 */
public class Main
{
    private Configuration cfg = null;
    private Logger logger = null;

    private Collection publisher = null;
    private Collection subscriber = null;
    private String currentGroupName = "";
    private String lastGroupName = "";

    /**
     * Main entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Main volmonger = new Main();
        int returnValue = volmonger.process(args);
        System.exit(returnValue);
    } // main

    /**
     * Instantiates a new Main application.
     */
    public Main() {
    }

    /**
     * Process everything
     * <p>
     * This is the primary mainline.
     *
     * @param args Command Line args
     */
    private int process(String[] args) {
        int returnValue = 0;

        try {
            cfg = Configuration.getInstance();
            cfg.parseCommandLine(args);

            // setup the logger
            System.setProperty("logFilename", cfg.getLogFilename());
            System.setProperty("debugLevel", cfg.getDebugLevel());
            System.setProperty("consoleLevel", cfg.getConsoleLevel());
            org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            // get the named logger
            logger = LogManager.getLogger("applog");

            // the + makes searching for the beginning of a run easier
            logger.info("+ Main begin, version " + cfg.getVOLMONGER_VERSION() + " ------------------------------------------");

            // dump the settings
            logger.info("cfg: -c Console level = " + cfg.getConsoleLevel());
            logger.info("cfg: -d Debug level = " + cfg.getDebugLevel());
            logger.info("cfg: -D Dry run = " + Boolean.toString(cfg.isTestRun()));
            logger.info("cfg: -e Export filename = " + cfg.getExportFilename());
            logger.info("cfg: -f Log filename = " + cfg.getLogFilename());
            logger.info("cfg: -k Keep .volmonger files = " + Boolean.toString(cfg.isKeepVolMongerFiles()));
            logger.info("cfg: -l Publisher library name = " + cfg.getPublisherLibraryName());
            logger.info("cfg: -m Mismatch output filename = " + cfg.getMismatchFilename());
            logger.info("cfg: -n What's new output filename = " + cfg.getWhatsNewFilename());
            logger.info("cfg: -p Publisher's libraries filename = " + cfg.getPublisherFileName());
            logger.info("cfg: -P Publisher's collection import filename = " + cfg.getPublisherFileName());
            logger.info("cfg: -s Subscriber's libraries filename = " + cfg.getSubscriberFileName());
            logger.info("cfg: -S Subscriber collection import filename = " + cfg.getSubscriberImportFilename());
            logger.info("cfg: -t Targets for mismatches to be copied too = " + cfg.getTargetsFilename());
            logger.info("cfg: -v Validation run = " + Boolean.toString(cfg.isValidationRun()));

            publisher = new Collection();
            subscriber = new Collection();

            try {
                if (cfg.getPublisherImportFilename().length() > 0) {                // -P import publisher if specified
                    publisher.importItems(cfg.getPublisherImportFilename());
                } else {
                    if (cfg.getPublisherFileName().length() > 0) {              // else -p publisher library scan
                        scanCollection(cfg.getPublisherFileName(), publisher);
                    }
                }
                if (cfg.getExportFilename().length() > 0) {                     // -e export publisher (only)
                    publisher.exportCollection();
                } else {
                    if (cfg.getSubscriberImportFilename().length() > 0) {       // -S import subscriber if specified
                        subscriber.importItems(cfg.getSubscriberImportFilename());
                    } else {
                        if (cfg.getSubscriberFileName().length() > 0) {         // else -s subscriber library scan
                            scanCollection(cfg.getSubscriberFileName(), subscriber);
                        }
                    }
                    mongeCollections(publisher, subscriber);                    // monge publisher-to-subscriber
                }
            } catch (Exception e) {
                // the methods above throw pre-formatted messages, just use that
                logger.error(e.getMessage());
                returnValue = 2;
            }
        } catch (MongerException e) {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
            returnValue = 1;
            cfg = null;
        }

        // the - makes searching for the ending of a run easier
        if (logger != null) {
            logger.info("- Main end" + " ------------------------------------------");
        }

        return returnValue;
    } // process

    /**
     * Scan a collection to find Items
     *
     * @param collectionFile The JSON file containing the collection
     * @param collection     The collection object
     */
    private void scanCollection(String collectionFile, Collection collection) throws MongerException {
        collection.readLibrary(collectionFile);
        collection.validateLibrary();
        collection.scanAllLibraries();
    } // scanCollection

    /**
     * Monge two collections
     *
     * @param publisher  Publishes new media
     * @param subscriber Subscribes to a Publisher to receive new media
     */
    private void mongeCollections(Collection publisher, Collection subscriber) throws MongerException {
        boolean iWin = false;
        PrintWriter mismatchFile = null;
        PrintWriter whatsNewFile = null;
        String currentWhatsNew = "";
        ArrayList<Item> group = new ArrayList<>();
        long totalSize = 0;
        boolean importedPublisher = false;

        if (cfg.getPublisherImportFilename().length() > 0) {                // -P import publisher if specified
            importedPublisher = true;
        }

        String header = "Monging " + publisher.getLibrary().metadata.name + " to " + subscriber.getLibrary().metadata.name;
        logger.info(header);

        // todo IDEA: Add some counters for the various situations: Copied, Skipped, Not found, WTF, etc. for metrics
        
        // setup the -m mismatch output file
        if (cfg.getMismatchFilename().length() > 0) {
            try {
                mismatchFile = new PrintWriter(cfg.getMismatchFilename());
                mismatchFile.println(header);
                logger.info("Writing to mismatch file " + cfg.getMismatchFilename());
            } catch (FileNotFoundException fnf) {
                String s = "File not found exception for mismatch output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        // setup the -n What's New output file
        if (cfg.getWhatsNewFilename().length() > 0) {
            try {
                whatsNewFile = new PrintWriter(cfg.getWhatsNewFilename());
                whatsNewFile.println("What's New");
                logger.info("Writing to What's New file " + cfg.getWhatsNewFilename());
            } catch (FileNotFoundException fnf) {
                String s = "File not found exception for What's New output file " + cfg.getWhatsNewFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        // Make sure we close the mismatch and whatsnew files if anything throws an exception....

        // todo Rearrange this logic to be subscriber-centric.
        //      The outer loop should iterate over the "subscribed" libraries.
        //      There should be an option to specify one (or more?) libraries to process.
        //      QUESTION Should there be an option to scan all, or scan as-subscribed in the loop below dynamically?
        //               That is - only scan publisher libraries the subscriber has listed (subscribed to).

        try {
            for (Item publisherItem : publisher.getItems()) {
                boolean has = subscriber.has(publisherItem.getItemPath());

                // Ignore thumbs.db files
                // QUESTION Are there more files like thumbs.db we should ignore? If so make an array of them....
                // ANSWER Extend library metadata to include an array of files to ignore.
                // QUESTION Should we implement regex patterns too?
                if (publisherItem.getItemPath().equalsIgnoreCase("Thumbs.db"))
                    continue;

                if (has) {
                    logger.info("  + Subscriber " + subscriber.getLibrary().metadata.name + " has " + publisherItem.getItemPath());
                } else {

                    if (!publisherItem.isDirectory()) {
                        logger.info("  - Subscriber " + subscriber.getLibrary().metadata.name + " missing " + publisherItem.getItemPath());
                        if (cfg.getMismatchFilename().length() > 0) {
                            mismatchFile.println(publisherItem.getItemPath());
                        }

                        if (cfg.getWhatsNewFilename().length() > 0) {
                            /**
                             * Only show the left side of mismatched file. And Only show it once.
                             * So if you have 10 new episodes of Lucifer only the following will show in the what's new file
                             * Big Bang Theory
                             * Lucifer
                             * Legion
                             */
                            String path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().indexOf("\\"));
                            if (path.length() < 1) {
                                path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().indexOf("/"));
                            }

                            if (!currentWhatsNew.equalsIgnoreCase(path)) {
                                whatsNewFile.println(path);
                                currentWhatsNew = path;
                            }
                        }

                        if (isNewGrouping(publisherItem)) {
                            // if there is a group - process it

                            if (group.size() > 0) {
                                // get a subscriber target where the publisher item(s) will fit
                                String target = getTarget(subscriber, group.get(0).getLibrary(), totalSize);
                                if (target.length() > 0) {
                                    for (Item groupItem : group) {
                                        if (cfg.isTestRun()) {          // -t Test run option
                                            logger.info("    Would copy " + groupItem.getFullPath());
                                        } else {
                                            // copy item(s) to target
                                            logger.info("    Copied " + groupItem.getFullPath());
                                        }
                                    }
                                } else {
                                    logger.error("    No space on any subscriber " + group.get(0).getLibrary() + " target for " +
                                            lastGroupName + " that is " + totalSize / (1024 * 1024) + " MB");
                                }
                            }
                            logger.info("Switching groups from '" + lastGroupName + "' to '" + currentGroupName + "'");
                            group.clear();
                            totalSize = 0;
                            lastGroupName = currentGroupName;
                        }
                        long size = 0;
                        try {
                            size = Files.size(Paths.get(publisherItem.getFullPath()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        publisherItem.setSize(size);
                        totalSize += size;
                        group.add(publisherItem);
                    }
                }
            }
        } finally {
            if (mismatchFile != null) {
                mismatchFile.close();
            }
            if (whatsNewFile != null) {
                whatsNewFile.close();
            }
        }
    }

    /**
     * Is new grouping boolean.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    private boolean isNewGrouping(Item publisherItem) {
        boolean ret = true;
        String path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf("\\"));
        if (path.length() < 1) {
            path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf("/"));
        }
        if (currentGroupName.equalsIgnoreCase(path)) {
            ret = false;
        } else {
            currentGroupName = path;
        }
        return ret;
    }

    /**
     * Gets a subscriber target.
     * <p>
     * Will return one of the subscriber targets for the library of the item that is
     * large enough to hold the size specified, otherwise an empty string is returned.
     *
     * @param subscriber the subscriber collection object
     * @param library    the publisher library.definition.name
     * @param size       the total size of item(s) to be copied
     * @return the target
     * @throws MongerException the monger exception
     */
    public String getTarget(Collection subscriber, String library, long size) throws MongerException {
        String target = "";
        boolean allFull = true;
        boolean notFound = true;

        // setup the -m mismatch output file
        if (cfg.getTargetsFilename().length() > 0) {
            try {
                logger.info("Using Targets from file " + cfg.getTargetsFilename());
                readTargets();
            } catch (FileNotFoundException fnf) {
                String s = "File not found exception for mismatch output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        for (int i = 0; i < subscriber.getLibrary().libraries.length; ++i) {

            // find the matching subscriber library
            if (library.equalsIgnoreCase(subscriber.getLibrary().libraries[i].definition.name)) {
                notFound = false;

                // fixme Change targets to it's own file and use the -t option to define it.
                for (int j = 0; j < subscriber.getLibrary().libraries[i].targets.length; ++j) {

                    // check space on the candidate target
                    String candidate = subscriber.getLibrary().libraries[i].targets[j];
                    long space = availableSpace(candidate);
                    long minimum = Utils.getScaledValue(subscriber.getLibrary().libraries[i].definition.minimum);

                    if (space > minimum) {                  // check target space minimum
                        allFull = false;

                        if (space > size) {                 // check size of item(s) to be copied
                            target = candidate;             // has space, use it
                            break;
                        }
                    }
                }
                if (allFull) {
                    logger.error("All targets for library " + library + " are below definition.minimum of " + subscriber.getLibrary().libraries[i].definition.minimum);
                }
                break;  // can match only one library name
            }
        }
        if (notFound) {

            // QUESTION should this be an exception?

            logger.error("No subscriber library match found for publisher library " + library);
        }
        return target;
    }

    /**
     * Read targets.
     * todo Not sure if this is the place for this......
     */
    public void readTargets() {
    }

    /**
     * Available space on target.
     *
     * @param target the target
     * @return the long space available on target in bytes
     */
    public long availableSpace(String target) {
        long space = Utils.getScaledValue("4TB");

        // todo if local just get the disk free space of the target

        // todo if target transport is a Socket make the size request to the client end

        return space;
    }

    /**
     * Total item(s) size long.
     *
     * @param item the item root node
     * @return the long size of the item(s) in bytes
     */
    public long totalItemSize(Item item) {
        long size = 1024 * 1024;

        // todo get size of item

        // todo item may be a directory - if so get total size of all objects in it recursively

        return size;
    }

} // Main
