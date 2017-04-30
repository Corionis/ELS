package com.groksoft;

// see https://logging.apache.org/log4j/2.x/

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;

/**
 * VolMonger - main program
 */
public class VolMonger
{
    private Configuration cfg = null;
    private Logger logger = null;

    private Collection publisher = null;
    private Collection subscriber = null;

    /**
     * Main entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        VolMonger volmonger = new VolMonger();
        int returnValue = volmonger.process(args);
        System.exit(returnValue);
    } // main

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
            org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            // get the named logger
            logger = LogManager.getLogger("applog");

            // the + makes searching for the beginning of a run easier
            logger.info("+ VolMonger begin, version " + cfg.getVOLMONGER_VERSION());

            // dump the settings
            logger.info("cfg: -c Validation run = " + Boolean.toString(cfg.isValidationRun()));
            logger.info("cfg: -d Debug level = " + cfg.getDebugLevel());
            logger.info("cfg: -e Export filename = " + cfg.getExportFilename());
            logger.info("cfg: -f Log filename = " + cfg.getLogFilename());
            logger.info("cfg: -i Import filename = " + cfg.getImportFilename());
            logger.info("cfg: -k Keep .volmonger files = " + Boolean.toString(cfg.isKeepVolMongerFiles()));
            logger.info("cfg: -l Publisher library name = " + cfg.getPublisherLibraryName());
            logger.info("cfg: -m Mismatch output filename = " + cfg.getMismatchFilename());
            logger.info("cfg: -n Subscriber's name = " + cfg.getSubscriberName());
            logger.info("cfg: -p Publisher's collection filename = " + cfg.getPublisherFileName());
            logger.info("cfg: -s Subscriber's collection filename = " + cfg.getSubscriberFileName());
            logger.info("cfg: -t Test run = " + Boolean.toString(cfg.isTestRun()));

            publisher = new Collection();
            subscriber = new Collection();

            try {
                scanCollection(cfg.getPublisherFileName(), publisher);
                if (cfg.getExportFilename().length() > 0) {                     // -e export publisher (only)
                    publisher.exportCollection();
                } else {
                    if (cfg.getImportFilename().length() > 0) {                 // -i import if specified
                        subscriber.importItems();
                    } else {
                        if (cfg.getSubscriberFileName().length() > 0) {         // else -s subscriber scan
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
            System.out.println(Utils.getStackTrace(e));
            returnValue = 1;
            cfg = null;
        }

        // the - makes searching for the ending of a run easier
        logger.info("- VolMonger end");
        return returnValue;
    } // run

    /**
     * Scan a collection to find Items
     *
     * @param collectionFile The JSON file containing the collection
     * @param collection     The collection object
     */
    private void scanCollection(String collectionFile, Collection collection) throws MongerException {
        collection.readControl(collectionFile);
        collection.validateControl();
        collection.scanAll();
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
        String header = "Monging " + publisher.getControl().metadata.name + " to " + subscriber.getControl().metadata.name;
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

        Iterator<Item> publisherIterator = publisher.getItems().iterator();
        while (publisherIterator.hasNext()) {
            Item publisherItem = publisherIterator.next();

            boolean has = subscriber.has(publisherItem.getItemPath());
            if (has) {
                logger.info("  + Subscriber " + subscriber.getControl().metadata.name + " has " + publisherItem.getItemPath());
            } else {
                logger.info("  - Subscriber " + subscriber.getControl().metadata.name + " missing " + publisherItem.getItemPath());
                if (cfg.getMismatchFilename().length() > 0) {
                    mismatchFile.println(publisherItem.getItemPath());
                }
                if (cfg.isTestRun()) {          // -t Test run option

                    logger.info("    Would copy " + publisherItem.getFullPath());

                    // todo should a test run do more?  e,g, check space, or?

                } else {


                    // todo below:
                    // may need the notion of a "group of items" that is the collection of directories and files
                    // we consider to be the minimum set that could be copied to a different target
                    // AND whether individual items exist or not on the subscriber.


                    // get total size of non-existent item(s), recursively, in bytes
                    long size = totalItemSize(publisherItem);

                    // get a subscriber target where the publisher item(s) will fit
                    String target = getTarget(subscriber, publisherItem.getLibrary(), size);
                    if (target.length() > 0) {


                        // copy item(s) to target
                        logger.info("    Copied " + publisherItem.getFullPath());


                    } else {
                        logger.error("    No space on any subscriber " + publisherItem.getLibrary() + " target for " +
                            publisherItem.getFullPath() + " that is " + size / (1024 * 1024) + " MB");
                    }
                }
            }
        }
        if (mismatchFile != null) {
            mismatchFile.close();
        }
    } // mongeCollections

    /**
     * Gets a subscriber target.
     *
     * Will return one of the subscriber targets for the library of the item that is
     * large enough to hold the size specified, otherwise an empty string is returned.
     *
     * @param subscriber    the subscriber collection object
     * @param library       the publisher library.definition.name
     * @param size          the total size of item(s) to be copied
     * @return the target
     */
    public String getTarget(Collection subscriber, String library, long size) throws MongerException {
        String target = "";
        boolean allFull = true;
        boolean notFound = true;

        for (int i = 0; i < subscriber.getControl().libraries.length; ++i) {

            // find the matching subscriber library
            if (library.equalsIgnoreCase(subscriber.getControl().libraries[i].definition.name)) {
                notFound = false;

                for (int j = 0; j < subscriber.getControl().libraries[i].targets.length; ++j) {

                    // check space on the candidate target
                    String candidate = subscriber.getControl().libraries[i].targets[j];
                    long space = availableSpace(candidate);
                    long minimum = Utils.getScaledValue(subscriber.getControl().libraries[i].definition.minimum);

                    if (space > minimum) {                  // check target space minimum
                        allFull = false;

                        if (space > size) {                 // check size of item(s) to be copied
                            target = candidate;             // has space, use it
                            break;
                        }
                    }
                }
                if (allFull) {
                    logger.error("All targets for library " + library + " are below definition.minimum of " + subscriber.getControl().libraries[i].definition.minimum);
                }
                break;  // can match only one library name
            }
        }
        if (notFound) {

            // todo should this be an exception?

            logger.error("No subscriber library match found for publisher library " + library);
        }
        return target;
    }

    /**
     * Available space on target.
     *
     * @param target  the target
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

} // VolMonger
