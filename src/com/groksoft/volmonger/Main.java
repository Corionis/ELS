package com.groksoft.volmonger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Pattern;

// see https://logging.apache.org/log4j/2.x/
import com.groksoft.volmonger.storage.Target;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmonger.repository.Item;
import com.groksoft.volmonger.repository.Library;
import com.groksoft.volmonger.repository.Repository;
import com.groksoft.volmonger.storage.Storage;

/**
 * Main - VolMonger program
 */
public class Main {
    private Configuration cfg = null;
    private Logger logger = null;

    private Repository publisherRepository = null;
    private Repository subscriberRepository = null;

    private Storage storageTargets = null;
    private long grandTotalItems = 0L;
    private long grandTotalSize = 0L;

    private String currentGroupName = "";
    private String lastGroupName = "";
    private long whatsNewTotal = 0;
    private int ignoreTotal = 0;
    private int errorCount = 0;
    private ArrayList<String> ignoredList = new ArrayList<>();

    /**
     * The Formatter.
     * Setup formatter for number
     */
    DecimalFormat formatter = new DecimalFormat("#,###");

    /**
     * Instantiates the Main application.
     */
    public Main() {
    }

    /**
     * Main entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Main volmonger = new Main();
        int returnValue = volmonger.process(args);
        System.exit(returnValue);
    } // main

    private void exportPaths() {
        int returnValue = 0;
        try {
            for (Library pubLib : publisherRepository.getLibraryData().libraries.bibliography) {
                publisherRepository.scan(pubLib.name);
            }
            publisherRepository.exportPaths();
        } catch (MongerException e) {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
            returnValue = 1;
        }
    }

    private void export() {
        int returnValue = 0;
        try {
            for (Library pubLib : publisherRepository.getLibraryData().libraries.bibliography) {
                publisherRepository.scan(pubLib.name);
            }
            publisherRepository.export();
        } catch (MongerException e) {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
            returnValue = 1;
        }
    }

    /**
     * Process everything
     * <p>
     * This is the primary mainline.
     *
     * @param args Command Line args
     */
    private int process(String[] args) {
        int returnValue = 0;

        System.out.println("STARTING");
        try {
            cfg = Configuration.getInstance();
            cfg.parseCommandLine(args);

            // setup the logger
            System.setProperty("logFilename", cfg.getLogFilename());
            System.setProperty("consoleLevel", cfg.getConsoleLevel());
            System.setProperty("debugLevel", cfg.getDebugLevel());
            org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            // get the named logger
            logger = LogManager.getLogger("applog");

            // the + makes searching for the beginning of a run easier
            logger.info("+ Main begin, version " + cfg.getVOLMONGER_VERSION() + " ------------------------------------------");
            cfg.dump();

            // todo Add sanity checks for option combinations that do not make sense

            // create primary objects
            publisherRepository = new Repository();
            subscriberRepository = new Repository();
            storageTargets = new Storage();

            try {
                // get -p Publisher metadata
                if (cfg.getPublisherFileName().length() > 0) {
                    readRepository(cfg.getPublisherFileName(), publisherRepository, true);
                }

                // get -P Publisher import metadata
                if (cfg.getPublisherImportFilename().length() > 0) {
                    readRepository(cfg.getPublisherImportFilename(), publisherRepository, false);
                }

                // get -s Subscriber metadata
                if (cfg.getSubscriberFileName().length() > 0) {
                    readRepository(cfg.getSubscriberFileName(), subscriberRepository, true);
                }

                // get -S Subscriber import metadata
                if (cfg.getSubscriberImportFilename().length() > 0) {
                    readRepository(cfg.getSubscriberImportFilename(), subscriberRepository, false);
                }

                // get -t Targets
                if (cfg.getTargetsFilename().length() > 0) {
                    readTargets(cfg.getTargetsFilename(), storageTargets);
                } else {
                    logger.warn("NOTE: No targets file was specified - performing a dry run");
                    cfg.setDryRun(true);
                }

                // handle -e export publisher (only)
                if (cfg.getExportPathFilename().length() > 0) {
                    if (cfg.getPublisherFileName().length() > 0 ||
                            cfg.getPublisherImportFilename().length() > 0) {
                        exportPaths();
                    } else {
                        throw new MongerException("-e option requires the -p and/or -P options");
                    }
                }

                // handle -i export publisher (only)
                if (cfg.getExportFilename().length() > 0) {
                    if (cfg.getPublisherFileName().length() > 0 ||
                            cfg.getPublisherImportFilename().length() > 0) {
                        export();
                    } else {
                        throw new MongerException("-i option requires the -p and/or -P options");
                    }
                }

                // if all the pieces are specified monge the collections
                if (cfg.getPublisherFileName().length() > 0 &&
                        cfg.getSubscriberFileName().length() > 0 ||
                        cfg.getSubscriberImportFilename().length() > 0) {
                    mongeCollections();
                }

            } catch (Exception ex) {
                logger.error(ex.getMessage() + " toString=" + ex.toString());
                returnValue = 2;
            }
        } catch (MongerException e) {
            // no logger yet to just print to the screen
            System.out.println(e.getMessage());
            returnValue = 1;
            cfg = null;
        }

        // the - makes searching for the ending of a run easier
        if (logger != null) {
            logger.info("- Main end" + " ------------------------------------------");
        }

        return returnValue;
    } // process

    /**
     * Monge two collections
     *
     * @throws MongerException the monger exception
     */
    private void mongeCollections() throws MongerException {
        boolean iWin = false;
        Item lastDirectoryItem = null;
        PrintWriter mismatchFile = null;
        PrintWriter whatsNewFile = null;
        PrintWriter targetFile = null;
        String currentWhatsNew = "";
        String currLib = "";
        ArrayList<Item> group = new ArrayList<>();
        long totalSize = 0;

        String header = "Monging " + publisherRepository.getLibraryData().libraries.description + " to " + subscriberRepository.getLibraryData().libraries.description;
        logger.info(header);

        // todo IDEA: Add some counters for the various situations: Copied, Skipped, Not found, WTF, etc. for metrics

        // setup the -m mismatch output file
        if (cfg.getMismatchFilename().length() > 0) {
            try {
                mismatchFile = new PrintWriter(cfg.getMismatchFilename());
                mismatchFile.println(header);
                logger.info("Writing to mismatch file " + cfg.getMismatchFilename());
            } catch (FileNotFoundException fnf) {
                String s = "File not found exception for mismatch output file " + cfg.getMismatchFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        // setup the -n What's New output file
        if (cfg.getWhatsNewFilename().length() > 0) {
            try {
                whatsNewFile = new PrintWriter(cfg.getWhatsNewFilename());
                whatsNewFile.println("What's New");
                logger.info("Writing to What's New file " + cfg.getWhatsNewFilename());
            } catch (FileNotFoundException fnf) {
                String s = "File not found exception for What's New output file " + cfg.getWhatsNewFilename();
                logger.error(s);
                throw new MongerException(s);
            }
        }

        logger.info("                          ***** Starting Monge *****");
        try {
            for (Library subLib : subscriberRepository.getLibraryData().libraries.bibliography) {
                Library pubLib = null;
                if ((pubLib = publisherRepository.getLibrary(subLib.name)) != null) {
                    // Do the libraries have items or do we need to scan?
                    if (pubLib.items == null || pubLib.items.size() < 1) {
                        publisherRepository.scan(pubLib.name);
                    }
                    if (subLib.items == null || subLib.items.size() < 1) {
                        subscriberRepository.scan(subLib.name);
                    }

                    for (Item item : pubLib.items) {
                        if (ignore(item)) {
                            logger.info("  ! Ignoring '" + item.getItemPath() + "'");
                            ignoredList.add("\n"+item.getFullPath());
                        } else {
                            boolean has = subscriberRepository.hasItem(subLib.name, item.getItemPath());
                            if (has) {
                                logger.info("  = Subscriber " + subLib.name + " has " + item.getItemPath());
                            } else {

                                if (cfg.getWhatsNewFilename().length() > 0) {
                                    /*
                                     * Only show the left side of mismatched file. And Only show it once.
                                     * So if you have 10 new episodes of Lucifer only the following will show in the what's new file
                                     * Big Bang Theory
                                     * Lucifer
                                     * Legion
                                     */
                                    if (!item.getLibrary().equals(currLib)) {
                                        // If not first time display and reset the whatsNewTotal
                                        if (!currLib.equals("")) {
                                            whatsNewFile.println("--------------------------------");
                                            whatsNewFile.println("Total for " + currLib + " = " + whatsNewTotal);
                                            whatsNewFile.println("--------------------------------");
                                            whatsNewTotal = 0;
                                        }
                                        // Display the Library
                                        currLib = item.getLibrary();
                                        whatsNewFile.println("");
                                        whatsNewFile.println(currLib);
                                        whatsNewFile.println(new String(new char[currLib.length()]).replace('\0', '='));
                                        whatsNewFile.println("");
                                    }
                                    String path;
                                    path = Utils.getLastPath(item.getItemPath());
                                    if (!currentWhatsNew.equalsIgnoreCase(path)) {
                                        assert whatsNewFile != null;
                                        whatsNewFile.println("    " + path);
                                        currentWhatsNew = path;
                                        whatsNewTotal++;
                                    }
                                }

                                logger.info("  + Subscriber " + subLib.name + " missing " + item.getItemPath());

                                if (!item.isDirectory()) {
                                    if (cfg.getMismatchFilename().length() > 0) {
                                        assert mismatchFile != null;
                                        mismatchFile.println(item.getFullPath());
                                    }
                                    /* If the group is switching, process the current one. */
                                    if (isNewGrouping(item)) {
                                        logger.info("Switching groups from '" + lastGroupName + "' to '" + currentGroupName + "'");
                                        // There is a new group - process it
                                        processGroup(group, totalSize);
                                    }
                                    long size = 0;
                                    size = getItemSize(item);
                                    item.setSize(size);
                                    totalSize += size;
                                    group.add(item);
                                }
                            }
                        }
                    }
                } else {
                    throw new MongerException("Subscribed Publisher library " + subLib.name + " not found");
                }
            }
        } catch (Exception e)

        {
            logger.error("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
        } finally

        {
            // Process the last group
            logger.info("Processing last group '" + currentGroupName + "'");
            // There is a new group - process it
            processGroup(group, totalSize);

            // Close asl the files and show the results
            if (mismatchFile != null) {
                mismatchFile.println("----------------------------------------------------");
                mismatchFile.println("Grand total items: " + grandTotalItems);
                double gb = grandTotalSize / (1024 * 1024 * 1024);
                mismatchFile.println("Grand total size : " + formatter.format(grandTotalSize) + " bytes, " + gb + " GB");
                mismatchFile.close();
            }
            if (whatsNewFile != null) {
                whatsNewFile.println("--------------------------------");
                whatsNewFile.println("Total for " + currLib + " = " + whatsNewTotal);
                whatsNewFile.println("--------------------------------");
                whatsNewFile.close();
            }
        }
        logger.info("-----------------------------------------------------");
        if( ignoredList.size() > 0 ) {
            logger.info("Ignore List " + ignoredList.toString()+"\n");
            /*for (String s : ignoredList) {
                // todo is there a way to get the logger NOT to show the line number stuff on the end.
                logger.info(s+"        ");
            }*/
        }
        logger.info("Grand total errors: " + errorCount);
        logger.info("Grand total ignored: " + ignoreTotal);
        logger.info("Grand total items: " + grandTotalItems);
        double gb = grandTotalSize / (1024 * 1024 * 1024);
        logger.info("Grand total size : " + formatter.format(grandTotalSize) + " bytes, " + gb + " GB");
    }


    /**
     * Process group.
     *
     * @param group     the group
     * @param totalSize the total size
     * @throws MongerException the monger exception
     */
    private void processGroup(ArrayList<Item> group, long totalSize) throws MongerException{
        try {
            if (group.size() > 0) {
                for (Item groupItem : group) {
                    if (cfg.isDryRun()) {          // -t Test run option
                        logger.info("    Would copy " + groupItem.getFullPath());
                    } else {
                        String targetPath = getTarget(groupItem, groupItem.getLibrary(), totalSize);
                        if (targetPath != null) {
                            // copy item(s) to targetPath
                            String to = targetPath + "\\" + groupItem.getItemPath();
                            logger.info("  > Copying " + groupItem.getFullPath() + " to " + to);
                            if (!copyFile(groupItem.getFullPath(), to)) {
                                logger.error("    Copy failed");
                                ++errorCount;
                            }
                        } else {
                            logger.error("    No space on any targetPath " + group.get(0).getLibrary() + " for " +
                                    lastGroupName + " that is " + totalSize / (1024 * 1024) + " MB");
                        }
                    }
                }
            }
            grandTotalItems = grandTotalItems + group.size();
            group.clear();
            grandTotalSize = grandTotalSize + totalSize;
            totalSize = 0L;
            lastGroupName = currentGroupName;
        } catch (Exception e) {
            throw new MongerException("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
        }
    }

    /**
     * @param item
     * @return
     */
    private boolean ignore(Item item) {
        String str = "";
        String str1 = "";
        boolean ret = false;

        for (Pattern patt : publisherRepository.getLibraryData().libraries.compiledPatterns) {

            str = patt.toString();
            str1 = str.replace("?", ".?").replace("*", ".*?");
            if (item.getName().matches(str1)) {
                //logger.info(">>>>>>Ignoring '" + item.getName());
                ignoreTotal++;
                ret = true;
                break;
            }
        }
        return ret;
    }

    /**
     * Is new grouping boolean.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    private boolean isNewGrouping(Item publisherItem) {
        boolean ret = true;
        int i = publisherItem.getItemPath().lastIndexOf("\\");
        if (i < 0) {
            logger.warn("No subdirectory in path : " + publisherItem.getItemPath());
            return true;
        }
        String path = publisherItem.getItemPath().substring(0, i);
        if (path.length() < 1) {
            path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf("/"));
        }
        if (currentGroupName.equalsIgnoreCase(path)) {
            ret = false;
        } else {
            currentGroupName = path;
        }
        return ret;
    }

    /**
     * Gets a subscriber target.
     * <p>
     * Will return one of the subscriber targets for the library of the item that is
     * large enough to hold the size specified, otherwise an empty string is returned.
     *
     * @param item    the item
     * @param library the publisher library.definition.name
     * @param size    the total size of item(s) to be copied
     * @return the target
     * @throws MongerException the monger exception
     */
    public String getTarget(Item item, String library, long size) throws MongerException {
        String target = null;
        boolean allFull = true;
        boolean notFound = true;
        long space = 0L;
        long minimum = 0L;

        Target tar = storageTargets.getTarget(library);
        if (tar != null) {
            minimum = Utils.getScaledValue(tar.minimum);
        } else {
            minimum = Storage.minimumBytes;
        }

        // see if there is an "original" directory the new content will fit in
        String path = subscriberRepository.hasDirectory(library, item.getItemPath());
        if (path != null) {
            space = availableSpace(path);
            logger.info("Checking space " + path + " == " + (space / (1024 * 1024)) + " for " + (size / (1024 * 1024)) + " minimum " + (minimum / (1024 * 1024)));
            if (space > (size + minimum)) {
                logger.info("Using original storage location for " + item.getItemPath() + " to " + path);
                return path;
            } else {
                logger.info("Original storage location too full for " + item.getItemPath() + " (" + size + ") on " + path);
            }
        }

        // find the matching target
        if (tar != null) {
            notFound = false;
            for (int j = 0; j < tar.locations.length; ++j) {
                // check space on the candidate target
                String candidate = tar.locations[j];
                space = availableSpace(candidate);     // todo Move to Transport class
                if (space > minimum) {                  // check target space minimum
                    allFull = false;
                    if (space > (size + minimum)) {     // check size of item(s) to be copied
                        target = candidate;             // has space, use it
                        break;
                    }
                }
            }
            if (allFull) {
                logger.error("All locations for library " + library + " are below definition.minimum of " + tar.minimum);
            }
        }
        if (notFound) {
            logger.error("No target library match found for publisher library " + library);
        }
        return target;
    }

    /**
     * Read repository.
     *
     * @param filename the filename
     * @param repo     the repo
     * @throws MongerException the monger exception
     */
    private void readRepository(String filename, Repository repo, boolean validate) throws MongerException {
        repo.read(filename);
        if (validate) {
            repo.validate();
        }
    } // readRepository

    /**
     * Read targets.
     *
     * @param filename the filename
     * @param storage  the storage
     * @throws MongerException the monger exception
     */
    public void readTargets(String filename, Storage storage) throws MongerException {
        storage.read(filename);
        storage.validate();
    }

    /**
     * Available space on target.
     *
     * @param location the path to the target
     * @return the long space available on target in bytes
     */
    public long availableSpace(String location) {

        // todo Move to Transport class
        long space = 0;
        try {
            File f = new File(location);
            space = f.getFreeSpace();
        } catch (SecurityException e) {
            logger.error("Exception '" + e.getMessage() + "' getting available space from " + location);
        }
        return space;
    }

    /**
     * Get item size long.
     *
     * @param item the item root node
     * @return the long size of the item(s) in bytes
     */
    public long getItemSize(Item item) {
        long size = 0;
        try {
            size = Files.size(Paths.get(item.getFullPath()));    /// todo Move to Transport class
        } catch (IOException e) {
            logger.error("Exception '" + e.getMessage() + "' getting size of item " + item.getFullPath());
        }
        return size;
    }

    /**
     * Copy file.
     *
     * @param from the from
     * @param to   the to
     * @return the boolean
     */
    public boolean copyFile(String from, String to) {
        try {
            File f = new File(to);
            if (f != null) {
                f.getParentFile().mkdirs();
            }
            Path fromPath = Paths.get(from).toRealPath();
            Path toPath = Paths.get(to);  //.toRealPath();
            //Files.copy(fromPath, toPath.resolve(fromPath.getFileName()), StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
            Files.copy(fromPath, toPath, StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
        } catch (Exception e) {
            logger.error("Exception copying " + from + " to " + to + " msg: " + e.getMessage());
            return false;
        }
        return true;
    }

} // Main
