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


public class VolMonger
{
    Logger logger = null;
    Collection publisher = null;
    Collection subscriber = null;

    // command-line option flags & values
    String publisherFile = "";
    String subscriberFile = "";
    String logFilename = "VolMonger.log";

    // ----------------------------------------------------------------------
    /**
     * Main entry point
     *
     * @param args
     */
    public static void main(String[] args) {
        VolMonger volmonger = new VolMonger();
        volmonger.run(args);
    } // main

    // ----------------------------------------------------------------------
    /**
     * Run the process
     *
     * @param args
     */
    public void run(String[] args) {
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
     * @param collectionFile
     * @param collection
     * @return boolean True == success, otherwise error
     */
    public boolean scanCollection(String collectionFile, Collection collection) {
        try {
            logger.info("Reading JSON file");
//            FileReader fileReader = new FileReader("");
//            Object o = Jsoner.deserialize(fileReader);
            //JSONObject json = (JSONObject) parser.parse(fileReader);

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
     * @param publisher
     * @param subscriber
     * @return boolean True == success, otherwise error
     */
    public boolean mongeCollections(Collection publisher, Collection subscriber) {
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
