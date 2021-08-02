package com.groksoft.els;

import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Location;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.storage.Storage;
import com.groksoft.els.storage.Target;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * The Transfer class handles copying content to the appropriate location and
 * the local-only operations needed for ELS Hints.
 */
public class Transfer
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private Configuration cfg = null;
    private Main.Context context;
    private int copyCount = 0;
    private String currentGroupName = "";
    private long grandTotalItems = 0L;
    private long grandTotalOriginalLocation = 0L;
    private long grandTotalSize = 0L;
    private boolean isInitialized = false;
    private String lastGroupName = "";
    private int movedDirectories = 0;
    private int movedFiles = 0;
    private int removedDirectories = 0;
    private int removedFiles = 0;
    private int skippedMissing = 0;
    private Storage storageTargets = null;
    private boolean toIsNew = false;

    private Transfer()
    {
        // hide default constructor
    }

    public Transfer(Configuration config, Main.Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    /**
     * Copy a file, local or remote
     *
     * @param from      the full from path
     * @param to        the full to path
     * @param overwrite true/false
     */
    public void copyFile(String from, String to, boolean overwrite) throws Exception
    {
        if (cfg.isRemoteSession())
        {
            context.clientSftp.transmitFile(from, to, overwrite);
        }
        else
        {
            Path fromPath = Paths.get(from).toRealPath();
            Path toPath = Paths.get(to);
            File f = new File(to);
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            Files.copy(fromPath, toPath, StandardCopyOption.COPY_ATTRIBUTES, REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        }
    }

    /**
     * Copy group of files
     * <p>
     * The overwrite parameter is false for normal Process munge operations, and
     * true for Subscriber terminal (-r T) to Publisher listener (-r L) operations.
     *
     * @param group     the group
     * @param totalSize the total size
     * @param overwrite whether to overwrite any existing target file
     * @throws MungerException the els exception
     */
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws Exception
    {
        String response = "";
        if (!cfg.isTargetsEnabled())
        {
            throw new MungerException("-t or -T target is required for this operation");
        }

        if (group.size() > 0)
        {
            for (Item groupItem : group)
            {
                if (cfg.isDryRun())
                {
                    // -D Dry run option
                    ++copyCount;
                    logger.info("  > Would copy #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) + ", " + groupItem.getFullPath());
                }
                else
                {
                    String targetPath = getTarget(groupItem.getLibrary(), totalSize, groupItem);
                    if (targetPath != null)
                    {
                        // copy item(s) to targetPath
                        ++copyCount;

                        String to = targetPath + context.subscriberRepo.getWriteSeparator();
                        to += context.publisherRepo.normalizePath(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                        String msg = "  > Copying #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) + ", " + groupItem.getFullPath() + " to " + to;
                        logger.info(msg);
                        response += (msg + "\r\n");

                        copyFile(groupItem.getFullPath(), to, overwrite);
                    }
                    else
                    {
                        throw new MungerException("No space on any target location of " + group.get(0).getLibrary() + " for " +
                                lastGroupName + " that is " + Utils.formatLong(totalSize, false));
                    }
                }
            }
        }

        grandTotalItems = grandTotalItems + group.size();
        grandTotalSize = grandTotalSize + totalSize;
        lastGroupName = currentGroupName;
        group.clear();

        return response;
    }

    public int getCopyCount()
    {
        return copyCount;
    }

    public String getCurrentGroupName()
    {
        return currentGroupName;
    }

    /**
     * Get free space of a storage location either local or remote
     *
     * @param path
     * @return available space
     * @throws Exception
     */
    public long getFreespace(String path) throws Exception
    {
        long space;
        if (cfg.isRemoteSession())
        {
            // remote subscriber
            space = context.clientStty.availableSpace(path);
        }
        else
        {
            space = Utils.availableSpace(path);
        }
        return space;
    }

    public long getGrandTotalItems()
    {
        return grandTotalItems;
    }

    public long getGrandTotalOriginalLocation()
    {
        return grandTotalOriginalLocation;
    }

    public long getGrandTotalSize()
    {
        return grandTotalSize;
    }

    public String getLastGroupName()
    {
        return lastGroupName;
    }

    /**
     * Get the minimum available space constraint from subscriber file
     *
     * @param path
     * @return
     */
    private long getLocationMinimum(String path)
    {
        long minimum = 0L;
        for (Location loc : context.subscriberRepo.getLibraryData().libraries.locations)
        {
            if (path.startsWith(loc.location))
            {
                minimum = Utils.getScaledValue(loc.minimum);
                break;
            }
        }
        if (minimum < 1L)
        {
            minimum = Storage.MINIMUM_BYTES;
        }
        return minimum;
    }

    public int getMovedDirectories()
    {
        return movedDirectories;
    }

    public int getMovedFiles()
    {
        return movedFiles;
    }

    public int getRemovedDirectories()
    {
        return removedDirectories;
    }

    public int getRemovedFiles()
    {
        return removedFiles;
    }

    public int getSkippedMissing()
    {
        return skippedMissing;
    }

    /**
     * Get the storage targets either local or remote
     *
     * @throws Exception
     */
    private void getStorageTargets() throws Exception
    {
        String location = cfg.getTargetsFilename();

        if (cfg.isRemoteSession() && cfg.isRequestTargets())
        {
            if (location == null || location.length() < 1)
            {
                location = "subscriber-targets";
            }
            // request target data from remote subscriber
            location = context.clientStty.retrieveRemoteData(location, "targets");
            cfg.setTargetsFilename(location);
        }

        if (location != null) // v3.0.0 allow targets to be empty to use sources as target locations
        {
            if (storageTargets == null)
                storageTargets = new Storage();

            storageTargets.read(location, context.subscriberRepo.getLibraryData().libraries.flavor);
            if (!cfg.isRemoteSession())
                storageTargets.validate();
        }
    }

    /**
     * Gets a subscriber target
     * <p>
     * Will return the original directory where existing files are located if one
     * exists and that location has enough space.
     * <p>
     * Otherwise will return one of the subscriber targets for the library of the item
     * that has enough space to hold the item, otherwise an empty string is returned.
     *
     * @param item    the item
     * @param library the publisher library.definition.name
     * @param size    the total size of item(s) to be copied
     * @return the target
     * @throws MungerException the els exception
     */
    private String getTarget(String library, long size, Item item) throws Exception
    {
        String path = null;
        boolean notFound = true;
        long minimum = 0L;
        Target target = null;

        if (storageTargets != null)
        {
            target = storageTargets.getLibraryTarget(library);
        }
        if (target != null)
        {
            minimum = Utils.getScaledValue(target.minimum);
        }
        else
        {
            minimum = Storage.MINIMUM_BYTES;
        }

        // see if there is an "original" directory the new content will fit in
        if (!cfg.isNoBackFill())
        {
            path = context.subscriberRepo.hasDirectory(library, Utils.pipe(context.publisherRepo, item.getItemPath()));
            if (path != null)
            {
                // check size of item(s) to be copied
                if (itFits(path, size, minimum, target != null))
                {
                    logger.info("Using original storage location for " + item.getItemPath() + " at " + path);
                    //
                    // inline return
                    //
                    ++grandTotalOriginalLocation;
                    return path;
                }
                logger.info("Original storage location too full for " + item.getItemPath() + " " + Utils.formatLong(size, false) + " at " + path);
                path = null;
            }
        }

        String candidate;
        if (target != null) // a defined target is the default
        {
            notFound = false;
            for (int j = 0; j < target.locations.length; ++j)
            {
                candidate = target.locations[j];
                // check size of item(s) to be copied
                if (itFits(candidate, size, minimum, true))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        else // v3.0.0, use sources for target locations
        {
            Library lib = context.subscriberRepo.getLibrary(library);
            if (lib != null)
            {
                notFound = false;
            }
            for (int j = 0; j < lib.sources.length; ++j)
            {
                candidate = lib.sources[j];
                // check size of item(s) to be copied
                if (itFits(candidate, size, minimum, false))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        if (notFound)
        {
            logger.error("No target library match found for library " + library);
        }
        return path;
    }

    /**
     * Initialize the configured data structures
     */
    public void initialize() throws Exception
    {
        if (!isInitialized)
        {
            isInitialized = true;

            // For -r P connect to remote subscriber -r S
            if (cfg.isRemotePublish() || cfg.isPublisherListener())
            {
                // sanity checks
                if (context.publisherRepo.getLibraryData().libraries.flavor == null ||
                        context.publisherRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungerException("Publisher data incomplete, missing 'flavor'");
                }

                if (context.subscriberRepo.getLibraryData().libraries.flavor == null ||
                        context.subscriberRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungerException("Subscriber data incomplete, missing 'flavor'");
                }

                // check for opening commands from Subscriber
                // *** might change cfg options for subscriber and targets that are handled below ***
                if (context.clientStty.checkBannerCommands())
                {
                    logger.info("Received subscriber commands:" + (cfg.isRequestCollection() ? " RequestCollection " : "") + (cfg.isRequestTargets() ? "RequestTargets" : ""));
                }
            }

            // get -s Subscriber libraries
            if (cfg.getSubscriberLibrariesFileName().length() > 0)
            {
                if (cfg.isRemoteSession() && cfg.isRequestCollection())
                {
                    // request complete collection data from remote subscriber
                    String location = context.clientStty.retrieveRemoteData(cfg.getSubscriberLibrariesFileName(), "collection");
                    if (location == null || location.length() < 1)
                        throw new MungerException("Could not retrieve remote collections file");
                    cfg.setSubscriberLibrariesFileName(""); // clear so the collection file will be used
                    cfg.setSubscriberCollectionFilename(location);

                    context.subscriberRepo.read(cfg.getSubscriberCollectionFilename());
                }
            }

            // get -t|T Targets
            if (cfg.isTargetsEnabled())
            {
                getStorageTargets();
            }
            else
            {
                cfg.setDryRun(true);
            }
        }
    }

    /**
     * Is new grouping boolean
     * <p>
     * True if the item "group" is different than the current "group".
     * A group is a set of files within the same movie directory or
     * television season.
     *
     * @param publisherItem the publisher item
     * @return the boolean
     */
    public boolean isNewGrouping(Item publisherItem) throws MungerException
    {
        boolean ret = true;
        String p = publisherItem.getItemPath();
        String s = context.publisherRepo.getSeparator();
        int i = publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator());
        if (i < 0)
        {
            logger.warn("No subdirectory in path : " + publisherItem.getItemPath());
            return true;
        }
        String path = publisherItem.getItemPath().substring(0, i);
        if (path.length() < 1)
        {
            path = publisherItem.getItemPath().substring(0, publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator()));
        }
        if (currentGroupName.equalsIgnoreCase(path))
        {
            ret = false;
        }
        else
        {
            currentGroupName = path;
        }
        return ret;
    }

    /**
     * Will the needed size fit?
     */
    private boolean itFits(String path, long size, long minimum, boolean hasTarget) throws Exception
    {
        boolean fits = false;
        long space = getFreespace(path);

        if (!hasTarget) // provided targets file overrides subscriber file locations minimum values
        {
            if (context.subscriberRepo.getLibraryData().libraries.locations != null &&
                    context.subscriberRepo.getLibraryData().libraries.locations.length > 0) // v3.0.0
            {
                minimum = getLocationMinimum(path);
            }
        }

        logger.info("Checking " + (hasTarget ? "target location" : "library source") +
                " for " + (Utils.formatLong(size, false)) +
                " with minimum " + Utils.formatLong(minimum, false) +
                " on " + (cfg.isRemoteSession() ? "remote" : "local") +
                " path " + path + " has " + (Utils.formatLong(space, false)));

        if (space > (size + minimum))
        {
            fits = true;
        }
        return fits;
    }

    /**
     * Perform move on either a file or directory
     * <p>
     * This is a local-only method, v3.0.0
     */
    public boolean move(Repository repo, String fromLibName, String fromName, String toLibName, String toName) throws Exception
    {
        boolean libAltered = false;

        Library fromLib = repo.getLibrary(fromLibName);
        if (fromLib == null)
        {
            logger.info("  ! From library not found: " + fromLibName);
            return false;
        }
        Library toLib = repo.getLibrary(toLibName);
        if (toLib == null)
        {
            logger.info("  ! To library not found: " + toLibName);
            return false;
        }

        // scan the toLib if necessary
        if (!toLib.name.equalsIgnoreCase(fromLib.name) && toLib.items == null)
        {
            repo.scan(toLib.name);
        }

        Collection collection = repo.getMapItem(fromLib, fromName);
        if (collection != null)
        {
            Iterator it = collection.iterator();
            if (collection.size() > 0)
            {
                for (int i = 0; i < collection.size(); ++i) // generally there is only one
                {
                    Integer j = (Integer) it.next();

                    Item fromItem = fromLib.items.elementAt(j);

                    if (fromItem.isDirectory())
                    {
                        // move to a different library
                        if (!toLib.name.equalsIgnoreCase(fromLib.name))
                        {

                            // move directory's items
                            for (Item nextItem : fromLib.items)
                            {
                                if (nextItem.isDirectory())
                                    continue;

                                if (nextItem.getItemPath().startsWith(fromItem.getItemPath() + repo.getSeparator()))
                                {
                                    String nextName = nextItem.getItemPath().substring(fromItem.getItemPath().length() + 1);
                                    String nextPath = toName + repo.getSeparator() + nextName;
                                    if (moveItem(repo, fromLib, nextItem, toLib, nextPath))
                                    {
                                        libAltered = true;
                                    }
                                }
                            }

                            // remove the physical directory; should be empty at this point
                            if (!cfg.isDryRun())
                            {
                                File prevDir = new File(fromItem.getFullPath());
                                if (Utils.removeDirectoryTree(prevDir))
                                {
                                    logger.warn("  ! Previous directory was not empty: " + fromItem.getFullPath());
                                }
                                ++movedDirectories;
                            }
                        }
                        else // logically it is a rename within same library
                        {
                            // rename the directory
                            if (moveItem(repo, fromLib, fromItem, toLib, toName))
                                libAltered = true;
                        }
                    }
                    else // it is a file
                    {
                        if (moveItem(repo, fromLib, fromItem, toLib, toName))
                            libAltered = true;
                    }
                }
            }
            else
            {
                logger.info("  ! Does not exist (A), skipping: " + fromLibName + "|" + fromName);
                ++skippedMissing;
            }
        }
        else
        {
            logger.info("  ! Does not exist (B), skipping: " + fromLibName + "|" + fromName);
            ++skippedMissing;
        }

        return libAltered;
    }

    /**
     * Move an individual item, directory or file
     * <p>
     * This is a local-only method, v3.0.0
     */
    private boolean moveItem(Repository repo, Library fromLib, Item fromItem, Library toLib, String toName) throws Exception
    {
        boolean libAltered = false;

        // setup the toItem
        Item toItem = setupToItem(repo, fromLib, fromItem, toLib, toName);

        // see if it still exists
        File fromFile = new File(fromItem.getFullPath());
        if (fromFile.exists())
        {
            if (cfg.isDryRun())
            {
                logger.info("  > Would mv " + (fromItem.isDirectory() ? "directory " : "file ") + fromLib.name + "|" + fromItem.getItemPath() + " to " + toLib.name + "|" + toName);
                return false;
            }

            // perform move / rename
            File toFile = new File(toItem.getFullPath());
            if (toFile.exists())
            {
                logger.info("  ! Target exists, will overwrite: " + toItem.getFullPath());
            }
            else if (toIsNew)
            {
                if (toFile.getParentFile().mkdirs())
                {
                    toLib.rescanNeeded = true;
                    libAltered = true;
                }
            }

            logger.info("  > mv " + (fromItem.isDirectory() ? "directory " : "file ") + fromLib.name + "|" + fromItem.getItemPath() + " to " + toLib.name + "|" + toName);
            Files.move(fromFile.toPath(), toFile.toPath(), REPLACE_EXISTING);

            // no exception thrown
            if (toFile.isDirectory()) // directories should not reach here
            {
                ++movedDirectories;
            }
            else
            {
                ++movedFiles;
            }
        }
        else
        {
            logger.info("  ! Does not exist (C), skipping: " + fromItem.getFullPath());
            ++skippedMissing;
        }
        return libAltered;
    }

    /**
     * Remove an item, directory or file
     * <p>
     * If a directory ALL contents are deleted recursively.
     * <p>
     * This is a local-only method, v3.0.0
     */
    public boolean remove(Repository repo, String fromLibName, String fromName) throws Exception
    {
        boolean libAltered = false;

        Library fromLib = repo.getLibrary(fromLibName);
        if (fromLib == null)
        {
            logger.info("  ! From library not found: " + fromLibName);
            return false;
        }

        Collection collection = repo.getMapItem(fromLib, fromName);
        if (collection != null)
        {
            Iterator it = collection.iterator();
            if (collection.size() > 0)
            {
                for (int i = 0; i < collection.size(); ++i) // generally there is only one
                {
                    Integer j = (Integer) it.next();

                    Item fromItem = fromLib.items.elementAt(j);

                    if (fromItem.isDirectory())
                    {
                        if (cfg.isDryRun())
                        {
                            logger.info("  > Would rm directory: " + fromItem.getFullPath());
                        }
                        else
                        {
                            // remove the physical directory
                            File prevDir = new File(fromItem.getFullPath());
                            if (Utils.removeDirectoryTree(prevDir))
                            {
                                logger.warn("  ! Previous directory was not empty: " + fromItem.getFullPath());
                            }
                            logger.info("  > rm directory: " + fromItem.getFullPath());
                            fromLib.rescanNeeded = true;
                            libAltered = true;
                            ++removedDirectories;
                        }
                    }
                    else // it is a file
                    {
                        if (cfg.isDryRun())
                        {
                            logger.info("  > Would rm file: " + fromItem.getFullPath());
                        }
                        else
                        {
                            File prevFile = new File(fromItem.getFullPath());
                            if (prevFile.delete())
                            {
                                logger.info("  > rm file: " + fromItem.getFullPath());
                                fromLib.rescanNeeded = true;
                                libAltered = true;
                                ++removedFiles;
                            }
                        }
                    }
                }
            }
            else
            {
                logger.info("  ! Does not exist (A), skipping: " + fromLibName + "|" + fromName);
                ++skippedMissing;
            }
        }
        else
        {
            logger.info("  ! Does not exist (B), skipping: " + fromLibName + "|" + fromName);
            ++skippedMissing;
        }

        return libAltered;
    }

    /**
     * Setup a To Item either new or a copy of an existing Item
     * <p>
     * This is a local-only method, v3.0.0
     */
    private Item setupToItem(Repository repo, Library fromLib, Item fromItem, Library toLib, String toName) throws Exception
    {
        String path;
        Item toItem;
        toIsNew = false;

        // move to a different library
        if (!toLib.name.equalsIgnoreCase(fromLib.name))
        {
            toItem = repo.hasItem(fromItem, toLib.name, Utils.pipe(repo, toName));
            if (toItem == null) // does not exist
            {
                toIsNew = true;
                toItem = SerializationUtils.clone(fromItem);
                toItem.setLibrary(toLib.name);
                toItem.setItemPath(toName);
                path = getTarget(toLib.name, toItem.getSize(), toItem);
                path = path + repo.getSeparator() + toName;
            }
            else // exists, use same object
            {
                toItem.setLibrary(toLib.name);
                toItem.setItemPath(toName);
                String base = toItem.getFullPath().substring(0, toItem.getFullPath().length() - fromItem.getItemPath().length());
                path = base + toName;
            }
        }
        else // logically it is a rename within same library
        {
            toItem = SerializationUtils.clone(fromItem);
            toItem.setItemPath(toName);
            String base = toItem.getFullPath().substring(0, toItem.getFullPath().length() - fromItem.getItemPath().length());
            path = base + toName;
        }
        toItem.setFullPath(path);
        return toItem;
    }

}
