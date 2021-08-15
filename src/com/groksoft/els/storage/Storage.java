package com.groksoft.els.storage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

// see https://github.com/google/gson
import com.google.gson.Gson;

// see https://logging.apache.org/log4j/2.x/
import com.groksoft.els.repository.Libraries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;

/**
 * The type Storage.
 */
public class Storage
{
    private transient Logger logger = LogManager.getLogger("applog");

    // TargetData members
    private TargetData targetData = null;
    private String jsonFilename = "";

    public static final long MINIMUM_BYTES = 1073741824L;      // minimum minimum bytes (1GB)

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
    public Target getLibraryTarget(String libraryName) throws MungeException
    {
        Target target = null;
        if (targetData != null)
        {
            boolean has = false;
            for (Target tar : targetData.targets.storage)
            {
                if (tar.name.equalsIgnoreCase(libraryName))
                {
                    if (has) // check for duplicate library
                    {
                        throw new MungeException("Storage name " + tar.name + " found more than once in " + getJsonFilename());
                    }
                    has = true;
                    target = tar;
                }
            }
        }
        return target;
    }

    /**
     * Normalize target paths based on "flavor"
     *
     */
    private void normalize(String flavor)
    {
        if (targetData != null)
        {
            String from = "";
            String to = "";
            switch (flavor)
            {
                case Libraries.WINDOWS:
                    from = "/";
                    to = "\\\\";
                    break;

                case Libraries.LINUX:
                    from = "\\\\";
                    to = "/";
                    break;
            }

            for (Target tar : targetData.targets.storage)
            {
                for (int j = 0; j < tar.locations.length; ++j)
                {
                    tar.locations[j] = tar.locations[j].replaceAll(from, to);
                    if (tar.locations[j].endsWith(to))
                    {
                        tar.locations[j].substring(0, tar.locations[j].length() - 2);
                    }
                }
            }
        }
    }

    /**
     * Read Targets.
     *
     * @param filename The JSON Libraries filename
     * @throws MungeException the els exception
     */
    public void read(String filename, String flavor) throws MungeException
    {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading Targets file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            targetData = gson.fromJson(json, TargetData.class);
            normalize(flavor);
        } catch (IOException ioe) {
            throw new MungeException("Exception while reading targets: " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Validate the Targets data.
     *
     * @throws MungeException the els exception
     */
    public void validate() throws MungeException
    {
        long minimumSize;

        if (targetData == null) {
            throw new MungeException("TargetData are null");
        }

        Targets targets = targetData.targets;

        if (targets.description == null || targets.description.length() == 0) {
            throw new MungeException("targets.description must be defined");
        }

        for (int i = 0; i < targets.storage.length; ++i) {
            Target t = targets.storage[i];
            if (t.name == null || t.name.length() == 0) {
                throw new MungeException("storage.name [" + i + "] must be defined");
            }
            if (t.minimum == null || t.minimum.length() == 0) {
                throw new MungeException("storage.minimum [" + i + "] must be defined");
            }
            long min = Utils.getScaledValue(t.minimum);
            if (min < MINIMUM_BYTES) {               // non-fatal warning
                logger.warn("Storage.minimum [" + i + "] " + t.name + " of " + t.minimum + " is less than allowed minimum of " + (MINIMUM_BYTES / 1024 / 1024) + "MB. Using allowed minimum.");
            }
            if (t.locations == null || t.locations.length == 0) {
                throw new MungeException("storage.locations [" + i + "] " + t.name + " must be defined");
            }
            for (int j = 0; j < t.locations.length; ++j) {
                if (t.locations[j].length() == 0) {
                    throw new MungeException("storage[" + i + "].locations[" + j + "] " + t.name + " must be defined");
                }
                if (Files.notExists(Paths.get(t.locations[j]))) {
                    throw new MungeException("storage[" + i + "].locations[" + j + "]: " + t.locations[j] + " does not exist");
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

}
