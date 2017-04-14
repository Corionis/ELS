package com.groksoft;

import java.nio.file.Files;
import java.nio.file.Paths;

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
    private JsonObject json = new JsonObject();;

// Methods:
    // A load method to read a collection.json file
    // A validate method to check the syntax and existence of the elements in a collection.json file
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
            setJson(Jsoner.deserialize(jsonStr, getJson()));  // there are no defaults, use the empty json object
        } catch (Exception e) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(e));
        }
    }

    public void validateCollection() throws MongerException {
        if (getJson() == null) {
            throw new MongerException("JsonObject json is null");
        }
        JsonObject o = getJson();
    }

    public String getCollectionFile() {
        return collectionFile;
    }

    public void setCollectionFile(String collectionFile) {
        this.collectionFile = collectionFile;
    }

    public JsonObject getJson() {
        return json;
    }

    public void setJson(JsonObject json) {
        this.json = json;
    }

}
