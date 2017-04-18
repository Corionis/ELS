package com.groksoft;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// see https://github.com/cliftonlabs/json-simple/
import org.json.simple.*;

/**
 * The type Collection.
 */
public class Collection
{
    private Logger logger = LogManager.getLogger("applog");
    private String collectionFile = "";
    private JsonObject control = new JsonObject();
    ;

// Methods:
    // A load method to read a collection.control file
    // A validate method to check the syntax and existence of the elements in a collection.control file
    // A scan method to scan and generate the set of Item objects
    // A sort method, by context
    // A duplicates method to check for duplicate contexts in the Collection - possibly enforced by the selected Java collection requiring a unique key

    public Collection() {
    }

    public void readCollectionFile(String filename) throws MongerException {
        try {
            logger.info("Reading collection file " + filename);
            setCollectionFile(filename);
            String jsonStr = new String(Files.readAllBytes(Paths.get(filename)));
            if (jsonStr.length() == 0) {
                throw new MongerException("Failed to read " + filename + ", no data");
            }
            JsonObject jo = Jsoner.deserialize(jsonStr, getControl());  // there are no defaults, use the empty control object
            if (jo == null || jo.size() == 0) {
                throw new MongerException("Failed to parse " + filename + ", possible syntax error?");
            }
            setControl(jo);
        } catch (Exception e) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(e));
        }
    }

    public void validateCollection() throws MongerException {
        String s;
        boolean b;
        if (getControl() == null) {
            throw new MongerException("JsonObject control is null");
        }

        try {
            if (control.size() == 2) {
                HashMap<String, String> metadata = control.getMap("metadata");
                if (metadata.size() == 2) {
                    s = metadata.get("name");
                    logger.info("metadata.get(name) = " + s);
                    if (s == null || s.length() < 1) {
                        throw new MongerException("metadata.name must be defined");
                    }
                    s = metadata.get("caseSensitive");
                    logger.info("metadata.get(caseSensitive) = " + s);
                    if (s == null || s.length() < 1) {
                        throw new MongerException("metadata.case-sensitive must be defined");
                    }
                    b = s.equalsIgnoreCase("true");
                    logger.info("s.equalsIgnoreCase = " + b);
                }
                HashMap<String, String> libraries = control.getMap("libraries");
                logger.info("libraries = " + libraries);
                s = "42";
            }
        } catch (Exception e) {
            throw new MongerException("Exception while validating " + getCollectionFile() + " trace: " + Utils.getStackTrace(e));
        }
    }

    public String getCollectionFile() {
        return collectionFile;
    }

    public void setCollectionFile(String collectionFile) {
        this.collectionFile = collectionFile;
    }

    public JsonObject getControl() {
        return control;
    }

    public void setControl(JsonObject control) {
        this.control = control;
    }

}
