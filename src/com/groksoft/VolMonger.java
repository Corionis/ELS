package com.groksoft;

// see https://logging.apache.org/log4j/2.x/

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;

import static java.awt.SystemColor.text;

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
            logger.info("cfg: -k Keep .volmonger files = " + Boolean.toString(cfg.isKeepVolMongerFiles()));
            logger.info("cfg: -l Publisher library name = " + cfg.getPublisherLibraryName());
            logger.info("cfg: -n Subscriber's name = " + cfg.getSubscriberName());
            logger.info("cfg: -p Publisher's collection filename = " + cfg.getPublisherFileName());
            logger.info("cfg: -s Subscriber's collection filename = " + cfg.getSubscriberFileName());
            logger.info("cfg: -t Test run = " + Boolean.toString(cfg.isTestRun()));

            publisher = new Collection();
            subscriber = new Collection();

            try {
                scanCollection(cfg.getPublisherFileName(), publisher);
                if (cfg.getSubscriberFileName().length() > 0) {
                    scanCollection(cfg.getSubscriberFileName(), subscriber);
                }
                if (cfg.getExportFilename().length() > 0) {
                    publisher.exportCollection();
                } else {
                    if (cfg.getImportFilename().length() > 0) {
                        subscriber.importItems();
                    }
                    mongeCollections(publisher, subscriber);
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

        if (cfg.getMismatchFilename().length() > 0) {
            try {
                mismatchFile = new PrintWriter(cfg.getMismatchFilename());
                mismatchFile.println(header);
            } catch (FileNotFoundException fnf) {
                String s = "File not found error for mismatch output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        logger.info(header);
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
                if (cfg.isTestRun()) {
                    logger.info("    Would copy " + publisherItem.getFullPath());
                } else {
                    // copy it

                    logger.info("    Copied " + publisherItem.getFullPath());
                }
            }
        }
        if (mismatchFile != null) {
            mismatchFile.close();
        }
    } // mongeCollections

} // VolMonger
