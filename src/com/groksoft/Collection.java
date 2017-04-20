package com.groksoft;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// see https://github.com/google/gson
import com.google.gson.Gson;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sound.midi.MetaEventListener;

/**
 * The type Collection.
 */
public class Collection {
    private Logger logger = LogManager.getLogger("applog");
    private String collectionFile = "";
    private Control control = null;

// Methods:
    // A load method to read a collection.control file
    // A validate method to check the syntax and existence of the elements in a collection.control file
    // A scan method to scan and generate the set of Item objects
    // A sort method, by context
    // A duplicates method to check for duplicate contexts in the Collection - possibly enforced by the selected Java collection requiring a unique key

    public Collection() {
    }

    public void readControl(String filename) throws MongerException {
        try {
            String json;
            Gson gson = new Gson();
            logger.info("Reading collection file " + filename);
            setCollectionFile(filename);
            json = new String(Files.readAllBytes(Paths.get(filename)));
            json = json.replaceAll("[\n\r]", "");
            control = gson.fromJson(json, Control.class);

        } catch (Exception e) {
            throw new MongerException("Exception while reading " + filename + " trace: " + Utils.getStackTrace(e));
        }
    }

    public void validateControl() throws MongerException {
        String s;
        boolean b;
        String itemName = "";
        if (getControl() == null) {
            throw new MongerException("Control is null");
        }

        try {
            if (control.metadata.name == null || control.metadata.name.length() == 0) {
                throw new MongerException("metadata.name must be defined");
            }
            if (control.metadata.case_sensitive == null) {
                throw new MongerException("metadata.case_sensitive true/false must be defined");
            }

            for (int i = 0; i < control.libraries.length; i++) {
                if (control.libraries[i].definition == null) {
                    throw new MongerException("libraries.definition[" + i + "] must be defined");
                }
                if (control.libraries[i].sources == null || control.libraries[i].sources.length == 0) {
                    throw new MongerException("libraries[" + i + "].sources must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < control.libraries[i].sources.length; j++) {

                        if (control.libraries[i].sources[j].length() == 0) {
                            throw new MongerException("libraries[" + i + "].sources[" + j + "] must be defined");
                        }
                        System.out.println("DIR: "+control.libraries[i].sources[j]);
                        // Travers the current directory and get the media directories
                        // todo This wants the main dir to be TestRun but the other code needs it to be mock??????
                        // todo Got to tired to figure it out so just added TestRun to the path manually here!
                        Path path = Paths.get("TestRun/"+control.libraries[i].sources[j]);
                        List<String> fileNames = new ArrayList<>();

                        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
                            for (Path entry : directoryStream) {
                                fileNames.add(entry.toString());
                                System.out.println(entry.toString());
                            }
                        } catch (IOException ex) {
                            throw new MongerException("Error Reading Directory " + control.libraries[i].sources[j]);
                        }
                        //System.out.println(fileNames);
                    }
                }
                if (control.libraries[i].targets == null || control.libraries[i].targets.length == 0) {
                    throw new MongerException("libraries.sources[" + i + "] must be defined");
                } else {
                    // Verify paths
                    for (int j = 0; j < control.libraries[i].targets.length; j++) {
                        if (control.libraries[i].targets[j].length() == 0) {
                            throw new MongerException("libraries[" + i + "].targets[" + j + "] must be defined");
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new MongerException("Exception while validating " + getCollectionFile() + " item " + itemName + " trace: " + Utils.getStackTrace(e));
        }

        logger.info("Validation successful");
    }

    public String getCollectionFile() {
        return collectionFile;
    }

    public void setCollectionFile(String collectionFile) {
        this.collectionFile = collectionFile;
    }

    public Control getControl() {
        return control;
    }

    //==================================================================================================================

    /**
     * Classes used in the JSON to Object translations
     */

    public class Control {
        Metadata metadata;
        Libraries[] libraries;
    }

    public class Metadata {
        public String name;
        public Boolean case_sensitive;
    }

    public class Libraries {
        public Definition definition;
        public String[] sources;
        public String[] targets;

    }

    public class Definition {
        public String name;
        public String minimum;
    }

}
