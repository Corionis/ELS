package com.groksoft.volmonger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;

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
public class Main
{
    private Configuration cfg = null;
    private Logger logger = null;

    private Repository publisherRepository = null;
    private Repository subscriberRepository = null;

    private Storage storageTargets = null;

    private String currentGroupName = "";
    private String lastGroupName = "";

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
                if (cfg.getExportFilename().length() > 0) {
                    if (cfg.getPublisherFileName().length() > 0 ||
                            cfg.getPublisherImportFilename().length() > 0) {
                        export();
                    } else {
                        throw new MongerException("-e option requires the -p and/or -P options");
                    }
                }

                // if all the pieces are specified monge the collections
                if (cfg.getPublisherFileName().length() > 0 &&
                        cfg.getSubscriberFileName().length() > 0 &&
                        cfg.getTargetsFilename().length() > 0) {
                    mongeCollections();
                }

            } catch (Exception ex) {
                logger.error(ex.getMessage());
                returnValue = 2;
            }
        }
        catch (MongerException e) {
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
        int errorCount = 0;
        boolean iWin = false;
        Item lastDirectoryItem = null;
        PrintWriter mismatchFile = null;
        PrintWriter whatsNewFile = null;
        PrintWriter targetFile = null;
        String currentWhatsNew = "";
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
                                String path;
                                path = Utils.getLastPath(item.getItemPath());
                                if (!currentWhatsNew.equalsIgnoreCase(path)) {
                                    assert whatsNewFile != null;
                                    whatsNewFile.println(path);
                                    currentWhatsNew = path;
                                }
                            }

                            logger.info("  + Subscriber " + subLib.name + " missing " + item.getItemPath());

                            if (!item.isDirectory()) {
                                if (cfg.getMismatchFilename().length() > 0) {
                                    assert mismatchFile != null;
                                    mismatchFile.println(item.getItemPath());
                                }
                                if (isNewGrouping(item)) {
                                    logger.info("Switching groups from '" + lastGroupName + "' to '" + currentGroupName + "'");
                                    // if there is a new group - process it
                                    if (group.size() > 0) {
                                        for (Item groupItem : group) {
                                            if (cfg.isDryRun()) {          // -t Test run option
                                                logger.info("    Would copy " + groupItem.getFullPath());
                                            } else {
                                                String targetPath = getTarget(subscriberRepository, subLib.name, totalSize);
                                                if (targetPath != null) {
                                                    // copy item(s) to targetPath
                                                    String to = targetPath + "/" + groupItem.getItemPath();
                                                    if (copyFile(groupItem.getFullPath(), to)) {
                                                        logger.info("    Copied " + groupItem.getFullPath());
                                                    } else {
                                                        ++errorCount;       // todo should there be an error count threshold?
                                                    }
                                                } else {
                                                    logger.error("    No space on any targetPath " + group.get(0).getLibrary() + " for " +
                                                            lastGroupName + " that is " + totalSize / (1024 * 1024) + " MB");
                                                }
                                            }
                                        }
                                    }

                                    // QUESTION Is this setLastDirectory() right???
                                    subscriberRepository.setLastDirectory(null);    // reset lastDirectory

                                    group.clear();
                                    totalSize = 0;
                                    lastGroupName = currentGroupName;
                                }
                                long size = 0;
                                size = getItemSize(item);
                                item.setSize(size);
                                totalSize += size;
                                group.add(item);
                            }
                        }
                    }
                } else {
                    throw new MongerException("Subscribed Publisher library " + subLib.name + " not found");
                }
            }
        } catch (Exception e) {
            logger.error("Exception " + e.getMessage() + " trace: " + Utils.getStackTrace(e));
        } finally {
            if (mismatchFile != null) {
                mismatchFile.close();
            }
            if (whatsNewFile != null) {
                whatsNewFile.close();
            }
        }
    }

    /**
     * Is new grouping boolean.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    private boolean isNewGrouping(Item publisherItem) {
        boolean ret = true;
        String path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf("\\"));
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
     * @param subRepo the subscriber Repository object
     * @param library the publisher library.definition.name
     * @param size    the total size of item(s) to be copied
     * @return the target
     * @throws MongerException the monger exception
     */
    public String getTarget(Repository subRepo, String library, long size) throws MongerException {
        String target = null;
        boolean allFull = true;
        boolean notFound = true;
        long space = 0l;

        if (subRepo.getLastDirectory() != null) {

            // todo Check available space on lastDirectory volume

            String ldfp = subRepo.getLastDirectory().getFullPath();
            String path = ldfp.substring(0, ldfp.lastIndexOf("\\"));        // the item already has the last directory name
            return path;
        }

        // find the matching library
        Target tar = storageTargets.getTarget(library);
        if (tar != null) {
            notFound = false;
            for (int j = 0; j < tar.locations.length; ++j) {

                // check space on the candidate target
                String candidate = tar.locations[j];
                space = availableSpace(candidate);     // todo Move to Transport class
                long minimum = Utils.getScaledValue(tar.minimum);
                if (space > minimum) {                  // check target space minimum
                    allFull = false;
                    if (space > size) {                 // check size of item(s) to be copied
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
