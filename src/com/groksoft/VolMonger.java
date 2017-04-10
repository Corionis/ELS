package com.groksoft;
// http://javarevisited.blogspot.com/2014/12/how-to-read-write-json-string-to-file.html

import java.io.FileReader;

// see https://logging.apache.org/log4j/2.x/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// see https://github.com/cliftonlabs/json-simple/
import org.json.simple.*;
import org.json.simple.parser.JSONParser;


/**
 * The type Vol monger.
 * <p>
 * todo: Show Todd the UML stuff. - Is the 1 to 1 correct, or should it be 1 to n????????
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

        cfg = new Configuration();
        try {
            if (cfg.parseCommandLine(args)) {
                // setup the logger
                System.setProperty("logFilename", cfg.getLogFilename());
                org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
                ctx.reconfigure();

                // get the named logger
                logger = LogManager.getLogger("applog");

                // the + makes searching for the beginning of a run easier
                logger.info("+ VolMonger begin, version " + cfg.getVOLMONGER_VERSION());

                try {
                    scanCollection(cfg.getPublisherFileName(), publisher);
                    scanCollection(cfg.getSubscriberFileName(), subscriber);
                    mongeCollections(publisher, subscriber);
                } catch (Exception e) {
                    // the methods above throw pre-formatted messages, just use that
                    logger.error(e.getMessage());
                    returnValue = 2;
                }
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
        JSONParser parser = new JSONParser();
        try {


            // move this to a validate method
            logger.info("Reading JSON file");
            FileReader fileReader = new FileReader(" ");
            Object o = Jsoner.deserialize(fileReader);
            JSONObject json = (JSONObject) parser.parse(fileReader);
            JSONArray jsonArray = (JSONArray) parser.parse(fileReader);
            logger.info("JSONObject = " + json.toJSONString());
            logger.info("JSONArray = " + json.toJSONString());



        } catch (Exception e) {
            throw new MongerException("Exception while monging" + Utils.getStackTrace(e));
        }
    } // scanCollection

    /**
     * Monge two collections
     *
     * @param publisher  Publishes new media
     * @param subscriber Subscribes to a Publisher to recive new media
     */
    private void mongeCollections(Collection publisher, Collection subscriber) throws MongerException {
        boolean iWin = false;
        try {

        } catch (Exception e) {
            throw new MongerException("Exception while monging" + Utils.getStackTrace(e));
        }
        logger.info("Monging ");
    } // mongeCollections

}
