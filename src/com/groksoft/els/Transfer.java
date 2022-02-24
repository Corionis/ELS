package com.groksoft.els;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavTreeUserObject;
import com.groksoft.els.repository.*;
import com.groksoft.els.storage.Storage;
import com.groksoft.els.storage.Target;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Transfer class to handle copying content to the appropriate location and
 * the local-only operations needed for ELS Hints.
 */
public class Transfer
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private Configuration cfg = null;
    private Context context;
    private int copyCount = 0;
    private String currentGroupName = "";
    private long grandTotalItems = 0L;
    private long grandTotalOriginalLocation = 0L;
    private long grandTotalSize = 0L;
    private GuiContext guiContext = null;
    private boolean isInitialized = false;
    private String lastGroupName = "";
    private int movedDirectories = 0;
    private int movedFiles = 0;
    private int removedDirectories = 0;
    private int removedFiles = 0;
    private int skippedMissing = 0;
    private Storage storageTargets = null;
    private boolean toIsNew = false;

    /**
     * Constructor with fixed en_US locale
     *
     * @param config Configuration
     * @param ctx    Context
     */
    public Transfer(Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    /**
     * Constructor for Navigator with selectable locale
     *
     * @param config Configuration
     * @param ctx    Context
     * @param gtxt   GuiContext
     */
    public Transfer(Configuration config, Context ctx, GuiContext gtxt)
    {
        cfg = config;
        context = ctx;
        guiContext = gtxt;
    }

    /**
     * Copy a file, local or remote
     *
     * @param from      the full from path
     * @param to        the full to path
     * @param isRemote  true/false
     * @param overwrite true/false
     */
    public void copyFile(String from, FileTime filetime, String to, boolean isRemote, boolean overwrite) throws Exception
    {
        if (isRemote)
        {
            context.clientSftp.transmitFile(from, to, overwrite);
            if (cfg.isPreserveDates() && filetime != null)
                context.clientSftp.setDate(to, (int) filetime.to(TimeUnit.SECONDS));
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
            if (cfg.isPreserveDates() && filetime != null)
                Files.copy(fromPath, toPath, StandardCopyOption.COPY_ATTRIBUTES, REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            else
                Files.copy(fromPath, toPath, REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
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
     * @throws MungeException the els exception
     */
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws Exception
    {
        String response = "";
        if (!cfg.isTargetsEnabled())
        {
            throw new MungeException(cfg.gs("Transfer.t.target.is.required.for.this.operation"));
        }

        if (group.size() > 0)
        {
            for (Item groupItem : group)
            {
                if (cfg.isDryRun())
                {
                    // -D Dry run option
                    ++copyCount;
                    logger.info("  > " + cfg.gs("Transfer.would.copy") + " #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) + ", " + groupItem.getFullPath());
                }
                else
                {
                    String targetPath = getTarget(groupItem.getLibrary(), cfg.isRemoteSession(), totalSize, groupItem.getItemPath());
                    if (targetPath != null)
                    {
                        // copy item(s) to targetPath
                        ++copyCount;

                        String to = targetPath + context.subscriberRepo.getWriteSeparator();
                        to += context.publisherRepo.normalizePath(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                        String msg = "  > " + cfg.gs("Transfer.copying") + " #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false) +
                                ", " + groupItem.getFullPath() + cfg.gs("NavTransferHandler.transfer.file.to") + to;
                        logger.info(msg);
                        response += (msg + "\r\n");

                        copyFile(groupItem.getFullPath(), groupItem.getModifiedDate(), to, cfg.isRemoteSession(), overwrite);
                    }
                    else
                    {
                        throw new MungeException(MessageFormat.format(cfg.gs("Transfer.no.space.on.any.target.location"),
                                group.get(0).getLibrary(), lastGroupName, Utils.formatLong(totalSize, false)));
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

    /**
     * Return the count of copies
     *
     * @return int count
     */
    public int getCopyCount()
    {
        return copyCount;
    }

    /**
     * Return the current "group" name
     *
     * @return String group name
     */
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
    public long getFreespace(String path, boolean isRemote) throws Exception
    {
        long space;
        if (isRemote && !context.localMode)
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

    /**
     * Get the grand total count of items
     *
     * @return int count
     */
    public long getGrandTotalItems()
    {
        return grandTotalItems;
    }

    /**
     * Get the grand total of items copied to original locations
     *
     * @return int count
     */
    public long getGrandTotalOriginalLocation()
    {
        return grandTotalOriginalLocation;
    }

    /**
     * Get the grand total of copied size
     *
     * @return long size in bytes
     */
    public long getGrandTotalSize()
    {
        return grandTotalSize;
    }

    /**
     * Get the last group name
     *
     * @return String last group name
     */
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

    /**
     * Get the count of moved directories
     *
     * @return int count
     */
    public int getMovedDirectories()
    {
        return movedDirectories;
    }

    /**
     * Get the count of moved files
     *
     * @return int count
     */
    public int getMovedFiles()
    {
        return movedFiles;
    }

    /**
     * Get the count of removed directories
     *
     * @return int count
     */
    public int getRemovedDirectories()
    {
        return removedDirectories;
    }

    /**
     * Get the count of removed files
     *
     * @return int count
     */
    public int getRemovedFiles()
    {
        return removedFiles;
    }

    public Repository getRepo(NavTreeUserObject tuo)
    {
        Repository repo = null;
        switch (tuo.node.getMyTree().getName())
        {
            case "treeCollectionOne":
            case "treeSystemOne":
                repo = context.publisherRepo;
                break;
            case "treeCollectionTwo":
            case "treeSystemTwo":
                repo = context.subscriberRepo;
                break;
        }
        return repo;
    }

    /**
     * Get the count of items skipped because they are missing
     *
     * @return int count
     */
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

        if (location != null && location.length() > 0) // v3.0.0 allow targets to be empty to use sources as target locations
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
     * @param library  the publisher library.definition.name
     * @param size     the total size of item(s) to be copied
     * @param itemPath the getItemPath() value
     * @return the target
     * @throws MungeException the els exception
     */
    private String getTarget(String library, boolean isRemote, long size, String itemPath) throws Exception
    {
        String path = getTarget(context.publisherRepo, library, size, context.subscriberRepo, isRemote, itemPath);
        return path;
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
     * @param sourceRepo the source publisher or subscriber repo
     * @param library    the publisher library.definition.name
     * @param size       the total size of item(s) to be copied
     * @param targetRepo the target publisher or subscriber repo
     * @param itemPath   the getItemPath() value
     * @return the target
     * @throws MungeException the els exception
     */
    public String getTarget(Repository sourceRepo, String library, long size, Repository targetRepo, boolean isRemote, String itemPath) throws Exception
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
            path = targetRepo.hasDirectory(library, Utils.pipe(sourceRepo, itemPath));
            if (path != null)
            {
                // check size of item(s) to be copied
                if (itFits(path, isRemote, size, minimum, target != null))
                {
                    logger.info(MessageFormat.format(cfg.gs("Transfer.using.original.storage.location"), itemPath, path));
                    //
                    // inline return
                    //
                    ++grandTotalOriginalLocation;
                    return path;
                }
                logger.info(MessageFormat.format(cfg.gs("Transfer.original.storage.location.too.full"), itemPath, Utils.formatLong(size, false), path));
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
                if (itFits(candidate, isRemote, size, minimum, true))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        else // v3.0.0, use sources for target locations
        {
            Library lib = targetRepo.getLibrary(library);
            if (lib != null)
            {
                notFound = false;
            }
            for (int j = 0; j < lib.sources.length; ++j)
            {
                candidate = lib.sources[j];
                // check size of item(s) to be copied
                if (itFits(candidate, isRemote, size, minimum, false))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        if (notFound)
        {
            logger.error(cfg.gs("Transfer.no.target.library.match.found.for.library") + library);
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
                    throw new MungeException(cfg.gs("Transfer.publisher.data.incomplete.missing.flavor"));
                }

                if (context.subscriberRepo.getLibraryData().libraries.flavor == null ||
                        context.subscriberRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungeException(cfg.gs("Transfer.subscriber.data.incomplete.missing.flavor"));
                }

                // check for opening commands from Subscriber
                // *** might change cfg options for subscriber and targets that are handled below ***
                if (context.clientStty.checkBannerCommands())
                {
                    logger.info(cfg.gs("Transfer.received.subscriber.commands") + (cfg.isRequestCollection() ? " RequestCollection " : "") + (cfg.isRequestTargets() ? "RequestTargets" : ""));
                }
            }

            if (cfg.isNavigator())
            {
                if (cfg.isRemoteSession())
                {
                    logger.info(cfg.gs("Transfer.requesting.subscriber.library"));
                    requestLibrary();
                }
            }
            else
            {
                // get -s Subscriber libraries
                if (cfg.getSubscriberLibrariesFileName().length() > 0)
                {
                    if (cfg.isRemoteSession() && cfg.isRequestCollection())
                    {
                        requestCollection();
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
    public boolean isNewGrouping(Item publisherItem) throws MungeException
    {
        boolean ret = true;
        String p = publisherItem.getItemPath();
        String s = context.publisherRepo.getSeparator();
        int i = publisherItem.getItemPath().lastIndexOf(context.publisherRepo.getSeparator());
        if (i < 0)
        {
            logger.warn(cfg.gs("Transfer.no.subdirectory.in.path") + publisherItem.getItemPath());
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
    private boolean itFits(String path, boolean isRemote, long size, long minimum, boolean hasTarget) throws Exception
    {
        boolean fits = false;
        long space = getFreespace(path, isRemote);

        if (!hasTarget) // provided targets file overrides subscriber file locations minimum values
        {
            if (context.subscriberRepo.getLibraryData().libraries.locations != null &&
                    context.subscriberRepo.getLibraryData().libraries.locations.length > 0) // v3.0.0
            {
                minimum = getLocationMinimum(path);
            }
        }

        logger.info(MessageFormat.format(cfg.gs("Transfer.checking"), hasTarget ? 0 : 1, Utils.formatLong(size, false),
                Utils.formatLong(minimum, false), cfg.isRemoteSession() ? 0 : 1, path) + (Utils.formatLong(space, false)));

        if (space > (size + minimum))
        {
            fits = true;
        }
        return fits;
    }

    public boolean makeDirs(String path, boolean isDir, boolean isRemote) throws Exception
    {
        boolean sense = true;
        String create = (isDir) ? path : Utils.getLeftPath(path, null);
        if (isRemote)
        {
            context.clientSftp.makeDirectory(create);
        }
        else
        {
            File f = new File(create);
            if (f != null)
            {
                sense = f.mkdirs();
            }
        }
        return sense;
    }

    /**
     * Perform collection move on either a file or directory
     * <p>
     * This is a local-only method, v3.0.0
     */
    public boolean move(Repository repo, String fromLibName, String fromName, String toLibName, String toName) throws Exception
    {
        boolean libAltered = false;

        Library fromLib = repo.getLibrary(fromLibName);
        if (fromLib == null)
        {
            logger.info(cfg.gs("Transfer.from.library.not.found") + fromLibName);
            return false;
        }
        Library toLib = repo.getLibrary(toLibName);
        if (toLib == null)
        {
            logger.info(cfg.gs("Transfer.to.library.not.found") + toLibName);
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
                                        fromLib.rescanNeeded = true;
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
                                    logger.warn(cfg.gs("Transfer.previous.directory.was.not.empty") + fromItem.getFullPath());
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
                logger.info(cfg.gs("Transfer.does.not.exist.skipping") + fromLibName + "|" + fromName);
                ++skippedMissing;
            }
        }
        else
        {
            logger.info(cfg.gs("Transfer.does.not.exist.skipping") + fromLibName + "|" + fromName);
            ++skippedMissing;
        }

        return libAltered;
    }

    /**
     * Move a local file
     *
     * @param from      the full from path
     * @param to        the full to path
     * @param overwrite true/false
     */
    public void moveFile(String from, FileTime filetime, String to, boolean overwrite) throws Exception
    {
        Path fromPath = Paths.get(from).toRealPath();
        Path toPath = Paths.get(to);
        File f = new File(to);
        if (f != null)
        {
            f.getParentFile().mkdirs();
        }
        if (cfg.isPreserveDates() && filetime != null) // TODO Fix file time handling here
            Files.move(fromPath, toPath, (overwrite) ? REPLACE_EXISTING : null);
        else
            Files.move(fromPath, toPath, (overwrite) ? REPLACE_EXISTING : null);
    }

    /**
     * Move an individual collection item, directory or file
     * <p>
     * This is a local-only method, v3.0.0
     */
    private boolean moveItem(Repository repo, Library fromLib, Item fromItem, Library toLib, String toName) throws Exception
    {
        boolean libAltered = false;

        // setup the toItem
        Item toItem = setupToItem(repo, fromLib, fromItem, toLib, toName);

        // see if it still exists
        String fromPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, fromItem.getFullPath());
        File fromFile = new File(fromPath);
        if (fromFile.exists())
        {
            String toPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, toItem.getFullPath());

            if (cfg.isDryRun())
            {
                logger.info(MessageFormat.format(cfg.gs("Transfer.would.mv.directory.file"), fromItem.isDirectory() ? 0 : 1) +
                        "\"" + fromLib.name + "|" + fromPath + "\"" + cfg.gs("NavTransferHandler.transfer.file.to") + "\"" +
                        toLib.name + "|" + toPath + "\"");
                return false;
            }

            // perform move / rename
            File toFile = new File(toPath);
            if (toFile.exists())
            {
                logger.info(cfg.gs("Transfer.target.exists.will.overwrite") + toItem.getFullPath());
            }

            // make sure the parent directories exist
            if (toFile.getParentFile().mkdirs())
            {
                toLib.rescanNeeded = true;
                libAltered = true;
            }

            logger.info(MessageFormat.format(cfg.gs("Transfer.mv.directory.file"), fromItem.isDirectory() ? 0 : 1) +
                    "\"" + fromLib.name + "|" + fromPath + "\"" + cfg.gs("NavTransferHandler.transfer.file.to") + "\"" +
                    toLib.name + "|" + toPath + "\"");
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
            logger.info(cfg.gs("Transfer.does.not.exist.skipping") + fromItem.getFullPath());
            ++skippedMissing;
        }
        return libAltered;
    }

    public String readTextFile(NavTreeUserObject tuo) throws Exception
    {
        String content = "";
        List<String> lines = null;
        if (tuo.isRemote)
        {
            context.clientStty.send("read \"" + tuo.path + "\"");
            content = context.clientStty.receive();
            if (content.equalsIgnoreCase("false"))
                content = "";
        }
        else
        {
            content = Utils.readString(tuo.path);
        }
        return content;
    }

    public String reduceCollectionPath(NavTreeUserObject tuo)
    {
        String path = null;
        if (tuo.node.getMyTree().getName().contains("Collection"))
        {
            Repository repo = getRepo(tuo);
            if (repo != null)
            {
                String tuoPath = (repo.getLibraryData().libraries.case_sensitive) ? tuo.path : tuo.path.toLowerCase();
                if (tuoPath.length() == 0)
                {
                    path = tuo.name + " | ";
                }
                else
                {
                    for (Library lib : repo.getLibraryData().libraries.bibliography)
                    {
                        for (String source : lib.sources)
                        {
                            String srcPath = source;
                            if (!tuo.isRemote)
                            {
                                File srcDir = new File(source);
                                srcPath = srcDir.getAbsolutePath();
                            }
                            srcPath = (repo.getLibraryData().libraries.case_sensitive) ? srcPath : srcPath.toLowerCase();
                            if (tuoPath.startsWith(srcPath))
                            {
                                path = lib.name + " | " + tuo.path.substring(srcPath.length() + 1);
                                break;
                            }
                        }
                        if (path != null)
                            break;
                    }
                }
            }
        }
        if (path == null)
            path = tuo.path;
        return path;
    }

    /**
     * Remove a file, local or remote
     *
     * @param path     the full from path
     * @param isDir    if this specific path is a directory
     * @param isRemote if this specific path is remote
     */
    public void remove(String path, boolean isDir, boolean isRemote) throws Exception
    {
        if (isRemote)
        {
            context.clientSftp.remove(path, isDir);
        }
        else
        {
            Path fromPath = Paths.get(path).toRealPath();
            Files.delete(fromPath);
        }
    }

    /**
     * Remove a collection item, directory or file
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
            logger.info(cfg.gs("Transfer.from.library.not.found") + fromLibName);
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
                            logger.info(cfg.gs("Transfer.would.rm.directory") + "\"" + fromLibName + "|" + fromItem.getFullPath() + "\"");
                        }
                        else
                        {
                            // remove the physical directory
                            String rmPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, fromItem.getFullPath());
                            File rmdir = new File(rmPath);
                            if (Utils.removeDirectoryTree(rmdir))
                            {
                                logger.warn("  ! Previous directory was not empty: " + fromItem.getFullPath());
                            }
                            logger.info(cfg.gs("Transfer.rm.directory") + "\"" + fromItem.getFullPath() + "\"");
                            fromLib.rescanNeeded = true;
                            libAltered = true;
                            ++removedDirectories;
                        }
                    }
                    else // it is a file
                    {
                        if (cfg.isDryRun())
                        {
                            logger.info(cfg.gs("Transfer.would.rm.file") + fromLibName + "|" + fromItem.getFullPath());
                        }
                        else
                        {
                            String rmPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, fromItem.getFullPath());
                            File rmFile = new File(rmPath);
                            if (rmFile.delete())
                            {
                                logger.info(cfg.gs("Transfer.rm.file") + "\"" + fromItem.getFullPath() + "\"");
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
                logger.info(cfg.gs("Transfer.does.not.exist.skipping") + fromLibName + "|" + fromName);
                ++skippedMissing;
            }
        }
        else
        {
            logger.info(cfg.gs("Transfer.does.not.exist.skipping") + fromLibName + "|" + fromName);
            ++skippedMissing;
        }

        return libAltered;
    }

    /**
     * Rename a file or directory, local or remote
     *
     * @param from     the full from path
     * @param to       the full to path
     * @param isRemote if this specific path is remote
     */
    public void rename(String from, String to, boolean isRemote) throws Exception
    {
        if (isRemote)
        {
            context.clientSftp.rename(from, to);
        }
        else
        {
            Files.move(Paths.get(from), Paths.get(to), StandardCopyOption.ATOMIC_MOVE);
        }
    }

    /**
     * Request the remote end re-scan and send it's collection JSON based on parameters
     * <p>
     * Any -l | -L parameter is handled.
     *
     * @throws Exception
     */
    public void requestCollection() throws Exception
    {
        if (cfg.isRemoteSession())
        {
            // request collection data from remote subscriber
            String location = context.clientStty.retrieveRemoteData(cfg.getSubscriberFilename(), "collection");
            if (location == null || location.length() < 1)
                throw new MungeException(cfg.gs("Transfer.could.not.retrieve.remote.collection.file"));
            cfg.setSubscriberLibrariesFileName(""); // clear so the collection file will be used
            cfg.setSubscriberCollectionFilename(location);
            context.subscriberRepo.read(cfg.getSubscriberCollectionFilename());
        }
    }

    /**
     * Request the remote end send it's library JSON for the Navigator
     *
     * @throws Exception
     */
    public void requestLibrary() throws Exception
    {
        if (cfg.isRemoteSession())
        {
            // request collection data from remote subscriber
            String location = context.clientStty.retrieveRemoteData(cfg.getSubscriberFilename(), "library");
            if (location == null || location.length() < 1)
                throw new MungeException(cfg.gs("Transfer.could.not.retrieve.remote.library.file"));
            cfg.setSubscriberLibrariesFileName(location);
            cfg.setSubscriberCollectionFilename(""); // clear so the library file will be used
            context.subscriberRepo.read(cfg.getSubscriberLibrariesFileName());
        }
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
                path = getTarget(toLib.name, false, toItem.getSize(), toItem.getItemPath());
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

    public long touch(String path, boolean isRemote) throws Exception
    {
        long seconds = (int) (System.currentTimeMillis() / 1000l);
        if (isRemote)
        {
            context.clientSftp.setDate(path, seconds);
        }
        else
        {
            File touchFile = new File(path);
            touchFile.setLastModified(seconds);
        }
        return seconds;
    }

    public String writeHint(String action, boolean isWorkstation, NavTreeUserObject sourceTuo, NavTreeUserObject targetTuo) throws Exception
    {
        /*
         * Publisher Workstation & Subscriber Collection
         *      Source                  Target                  Action
         *      ----------------------  ----------------------  ----------------------
         *      Workstation *           Workstation *           none
         *      Workstation *           Subscriber Collection   basic add, none
         *      Workstation *           Subscriber System       none
         *      Subscriber Collection   Workstation *           hint rm
         *      Subscriber System       Workstation *           none
         *
         *      Subscriber Collection   Subscriber Collection   hint
         *      Subscriber System       Subscriber Collection   basic add, none
         *      Subscriber Collection   Subscriber System       hint rm
         *      Subscriber System       Subscriber System       none
         *
         * Publisher Collection & Subscriber Collection
         *      Source                  Target                  Action
         *      ----------------------  ----------------------  ----------------------
         *      Publisher Collection    Publisher Collection    hint
         *      Publisher System        Publisher Collection    basic add, none
         *      Publisher Collection    Publisher System        hint rm
         *      Publisher System        Publisher System        none
         *
         *      Publisher Collection    Subscriber Collection
         *      Publisher System        Subscriber Collection
         *      Publisher Collection    Subscriber System       hint rm
         *      Publisher System        Subscriber System       none
         *
         *      Subscriber Collection    Publisher Collection
         *      Subscriber System        Publisher Collection
         *      Subscriber Collection    Publisher System       hint rm
         *      Subscriber System        Publisher System       none
         *
         *      Subscriber Collection    Subscriber Collection
         *      Subscriber System        Subscriber Collection
         *      Subscriber Collection    Subscriber System
         *      Subscriber System        Subscriber System
         *
         */
        String hintPath = "";

        // if a workstation and source is publisher then it is local or a basic add and there is no hint
        if (isWorkstation && !sourceTuo.isSubscriber())
            return "";

        boolean sourceIsCollection = sourceTuo.node.getMyTree().getName().toLowerCase().contains("collection");
        boolean targetIsCollection = (targetTuo != null) ? targetTuo.node.getMyTree().getName().toLowerCase().contains("collection") : false;

        // if source is subscriber system tab this it is a basic add, no hint
        if (sourceTuo.isSubscriber() && !sourceIsCollection)
            return "";

        // if either the source or target are not a collection there is no hint
        if (sourceIsCollection || targetIsCollection)
        {
            String command = "";
            String act = action.trim().toLowerCase();

            // a move out of a collection is an rm from the collection's point of view
            if (!targetIsCollection || !targetTuo.isSubscriber())
                act = "rm";

            if (act.equals("mv"))
            {
                command = "mv \"" + reduceCollectionPath(sourceTuo) + "\" \"" + reduceCollectionPath(targetTuo) + "\"";
            }
            else if (act.equals("rm"))
            {
                command = "rm \"" + reduceCollectionPath(sourceTuo) + "\"";
            }
            else
                throw new MungeException("Action must be 'mv' or 'rm'");

            hintPath = Utils.getLeftPath(sourceTuo.path, null);
            String hintName = Utils.getRightPath(hintPath, null);
            hintPath = hintPath + Utils.getSeparatorFromPath(hintPath) + hintName + ".els";

            // do not write a Hint about the same Hint
            if (Utils.getRightPath(sourceTuo.path, null).equals(hintName + ".els"))
                return "";

            if (!sourceTuo.isSubscriber())
                context.localMode = true;
            hintPath = writeUpdateHint(hintPath, command);
            context.localMode = false;
        }
        return hintPath;
    }

    public String writeUpdateHint(String hintPath, String command) throws Exception
    {
        if (cfg.isRemoteSession() && !context.localMode)
        {
            String line = "hint \"" + hintPath + "\" " + command;
            logger.info("Sending remote: " + line);
            hintPath = context.clientStty.roundTrip(line + "\n");
        }
        else // local operation
        {
            File hintFile = new File(hintPath);
            command = command + "\n";
            if (Files.exists(hintFile.toPath()))
            {
                Files.write(hintFile.toPath(), command.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
                logger.info(cfg.gs("Transfer.updated.hint.file") + hintFile.getAbsolutePath());
                hintPath = "";
            }
            else
            {
                StringBuilder sb = new StringBuilder();
                sb.append("# Created " + new Date().toString() + "\n");

                ArrayList<HintKeys.HintKey> keys = context.hintKeys.get();
                for (HintKeys.HintKey key : keys)
                {
                    if (!key.uuid.equalsIgnoreCase(context.publisherRepo.getLibraryData().libraries.key))
                    {
                        sb.append("For " + key.name + "\n");
                    }
                }
                sb.append(command);
                Files.write(hintFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
                logger.info(cfg.gs("Transfer.created.hint.file") + hintFile.getAbsolutePath());
            }
        }
        return hintPath;
    }

}
