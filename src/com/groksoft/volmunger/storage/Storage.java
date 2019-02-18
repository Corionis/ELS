package com.groksoft.volmunger.storage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson;                    // see https://github.com/google/gson

// see https://logging.apache.org/log4j/2.x/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.MungerException;
import com.groksoft.volmunger.Utils;

/**
 * The type Storage.
 */
public class Storage
{
    private transient Logger logger = LogManager.getLogger("applog");

    // TargetData members
    private TargetData targetData = null;
    private String jsonFilename = "";

    public static final long minimumBytes = 1073741824L;      // minimum minimum bytes (1GB)

    /**
     * Instantiates a new Storage instance.
     */
    public Storage() {
    }

    /**
     * Get target for a specific library
     * <p>
     * Do these Targets have a particular Library?
     *
     * @param libraryName the library name
     * @return the Target
     */
    public Target getTarget(String libraryName) throws MungerException {
        boolean has = false;
        Target retTarget = null;
        for (Target tar : targetData.targets.storage) {
            if (tar.name.equalsIgnoreCase(libraryName)) {
                if (has) {
                    throw new MungerException("Storage name " + tar.name + " found more than once in " + getJsonFilename());
                }
                has = true;
                retTarget = tar;
            }
        }
        return retTarget;
    }

    /**
     * Read Targets.
     *
     * @param filename The JSON Libraries filename
     * @throws MungerException the volmunger exception
     */
    public void read(String filename) throws MungerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading Targets file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            targetData = gson.fromJson(json, TargetData.class);
            String p = targetData.targets.storage[0].locations[0];
        } catch (IOException ioe) {
            throw new MungerException("Exception while reading targets: " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Validate the Targets data.
     *
     * @throws MungerException the volmunger exception
     */
    public void validate() throws MungerException {
        long minimumSize;

        if (getTargetData() == null) {
            throw new MungerException("TargetData are null");
        }

        Targets targets = targetData.targets;

        if (targets.description == null || targets.description.length() == 0) {
            throw new MungerException("targets.description must be defined");
        }

        for (int i = 0; i < targets.storage.length; ++i) {
            Target t = targets.storage[i];
            if (t.name == null || t.name.length() == 0) {
                throw new MungerException("storage.name [" + i + "] must be defined");
            }
            if (t.minimum == null || t.minimum.length() == 0) {
                throw new MungerException("storage.minimum [" + i + "] must be defined");
            }
            long min = Utils.getScaledValue(t.minimum);
            if (min < minimumBytes) {               // non-fatal warning
                logger.warn("storage.minimum [" + i + "] " + t.name + " of " + t.minimum + " is less than minimum of " + (minimumBytes / 1024 / 1024) + "MB. Using minimum.");
            }
            if (t.locations == null || t.locations.length == 0) {
                throw new MungerException("storage.locations [" + i + "] " + t.name + " must be defined");
            }
            for (int j = 0; j < t.locations.length; ++j) {
                if (t.locations[j].length() == 0) {
                    throw new MungerException("storage[" + i + "].locations[" + j + "] " + t.name + " must be defined");
                }
                if (Files.notExists(Paths.get(t.locations[j]))) {
                    throw new MungerException("storage[" + i + "].locations[" + j + "]: " + t.locations[j] + " does not exist");
                }
                logger.debug("  loc: " + t.locations[j]);
            }
        }
        logger.info("Targets validation successful: " + getJsonFilename());
    }

    /**
     * Gets Storage filename.
     *
     * @return the TargetData filename
     */
    public String getJsonFilename() {
        return jsonFilename;
    }

    /**
     * Sets Storage file.
     *
     * @param jsonFilename the TargetData file
     */
    public void setJsonFilename(String jsonFilename) {
        this.jsonFilename = jsonFilename;
    }

    /**
     * Gets targetData.
     *
     * @return the target data
     */
    public TargetData getTargetData() {
        return targetData;
    }

}
