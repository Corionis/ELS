package com.groksoft;
// http://javarevisited.blogspot.com/2014/12/how-to-read-write-json-string-to-file.html

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

// see https://logging.apache.org/log4j/2.x/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// see https://github.com/cliftonlabs/json-simple/
import org.json.simple.*;


/**
 * The type Vol monger.
 */
public class VolMonger
{
    private Logger logger = null;
    private Collection publisher = null;
    private Collection subscriber = null;

    // command-line option flags & values
    private String publisherFile = "";
    private String subscriberFile = "";
    private String logFilename = "VolMonger.log";

    // ----------------------------------------------------------------------

    /**
     * Main entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        VolMonger volmonger = new VolMonger();
        volmonger.run(args);
    } // main

    // ----------------------------------------------------------------------
    /**
     * Run the process
     *
     * @param args Command Line args
     */
    private void run(String[] args) {
        String y;

        // TODO Process command-line arguments here - with NO logging enabled yet - just spit out System.println() messages if the command-line has errors
        // xxx

        // setup the log file with a filename optionally defined on the command-line
        System.setProperty("logFilename", logFilename); // set the system property used in log4j2.xml
        org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
        ctx.reconfigure();

        // get the named logger
        logger = LogManager.getLogger("applog");

        logger.info("Begin VolMonger");

        if (scanCollection(publisherFile, publisher)) {
            if (scanCollection(subscriberFile, subscriber)) {
                if (mongeCollections(publisher, subscriber)) {
                    logger.info("Success");
                } else {
                    logger.error("Error occurred");
                }
            }
        }

        logger.info("End VolMonger");
    } // run

    // ----------------------------------------------------------------------
    /**
     * Scan a collection to find Items
     *
     * @param collectionFile The JSON file containing the collection
     * @param collection The collection object
     * @return boolean True == success, otherwise error
     */
    private boolean scanCollection(String collectionFile, Collection collection) {
        try {
            logger.info("Reading JSON file");
//            FileReader fileReader = new FileReader("");
//            Object o = Jsoner.deserialize(fileReader);
//            JSONObject json = (JSONObject) parser.parse(fileReader);

        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return true;
    } // scanCollection

    // ----------------------------------------------------------------------
    /**
     * Monge two collections
     *
     * @param publisher Publishes new media
     * @param subscriber Subscribes to a Publisher to recive new media
     * @return boolean True == success, otherwise error
     */
    private boolean mongeCollections(Collection publisher, Collection subscriber) {
        boolean iWin = false;
        try {
            logger.info("Monging ");

        } catch (Exception e) {
            logger.error(e);
            return false;
        }
        return true;
    } // mongeCollections

}
