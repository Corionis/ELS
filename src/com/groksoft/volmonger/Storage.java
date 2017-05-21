package com.groksoft.volmonger;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// see https://github.com/google/gson
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.monitor.MonitorSettingException;

/**
 * The type Storage.
 */
public class Storage
{
    private transient Logger logger = LogManager.getLogger("applog");
    private transient Configuration cfg = null;

    // TargetData members
    private TargetData targetData = null;
    private String jsonFilename = "";

    /**
     * Instantiates a new Storage instance.
     */
    public Storage() {
        cfg = Configuration.getInstance();
    }

    /**
     * Read Targets.
     *
     * @param filename The JSON Libraries filename
     * @throws MongerException the monger exception
     */
    public void read(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading Targets file " + filename);
            setJsonFilename(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            targetData = gson.fromJson(json, TargetData.class);
        } catch (IOException ioe) {
            throw new MongerException("Exception while reading targets: " + filename + " trace: " + Utils.getStackTrace(ioe));
        }
    }

    /**
     * Gets TargetData filename.
     *
     * @return the TargetData filename
     */
    public String getJsonFilename() {
        return jsonFilename;
    }

    /**
     * Sets TargetData file.
     *
     * @param jsonFilename the TargetData file
     */
    public void setJsonFilename(String jsonFilename) {
        this.jsonFilename = jsonFilename;
    }


    // todo Add validate and other methods


}
