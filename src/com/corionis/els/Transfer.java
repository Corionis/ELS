package com.corionis.els;

import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.hints.Hint;
import com.corionis.els.repository.Item;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Location;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.storage.Storage;
import com.corionis.els.storage.Target;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * Transfer class to handle content operations to the appropriate location
 */
public class Transfer
{
    private final transient Logger logger = LogManager.getLogger("applog");
    private Context context;
    private int copyCount = 0;
    private String currentGroupName = "";
    private long grandTotalItems = 0L;
    private long grandTotalOriginalLocation = 0L;
    private long grandTotalSize = 0L;
    private boolean isInitialized = false;
    private String lastGroupName = "";
    private Storage storageTargets = null;
    private boolean toIsNew = false;

    /**
     * Constructor for Navigator with selectable locale
     *
     * @param context    Context
     */
    public Transfer(Context context)
    {
        this.context = context;
    }

    /**
     * Copy a file, local or remote
     *
     * @param from      the full from path
     * @param to        the full to path
     * @param isRemote  true/false
     * @param overwrite true/false
     */
    public synchronized void copyFile(ClientSftp sftp, String from, FileTime filetime, String to, boolean isRemote, boolean overwrite) throws Exception
    {
        if (isRemote)
        {
            sftp.transmitFile(from, to, overwrite);
            if (context.cfg.isPreserveDates() && filetime != null)
                sftp.setDate(to, (int) filetime.to(TimeUnit.SECONDS));
        }
        else
        {
            if (to.matches("^\\\\[a-zA-Z]:.*"))
                to = to.substring(1);
            File f = new File(to);
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }

            File fileIn  = new File(from);
            File fileOut = new File(to);
            long size  = fileIn.length();
            byte[] buffer = new byte[32755];
            int received = 0;
            if (context.progress != null)
                context.progress.init(2, from, to, size);

            FileInputStream fin  = new FileInputStream(fileIn);
            FileOutputStream fout = new FileOutputStream(fileOut);
            while ((received = fin.read(buffer)) != -1)
            {
                if (context.progress != null)
                {
                    if (!context.progress.count(received))
                        break;
                }
                fout.write(buffer, 0, received);
            }
            fin.close();
            fout.close();

            if (context.progress != null)
                context.progress.end();

            if (context.cfg.isPreserveDates() && filetime != null)
                fileOut.setLastModified(filetime.toMillis());
        }
    }

    /**
     * Copy group of files
     * <p>
     * The overwrite parameter is false for normal Process munge operationsUI, and
     * true for Subscriber terminal (-r T) to Publisher listener (-r L) operationsUI.
     * <p>
     * Only used in Process() and publisher Daemon.
     *
     * @param group     the group
     * @param totalSize the total size
     * @param overwrite whether to overwrite any existing target file
     * @throws MungeException the els exception
     */
    public String copyGroup(ArrayList<Item> group, long totalSize, boolean overwrite) throws Exception
    {
        String response = "";
        if (!context.cfg.isTargetsEnabled())
        {
            throw new MungeException(context.cfg.gs("Transfer.t.target.is.required.for.this.operation"));
        }

        if (group.size() > 0)
        {
            for (Item groupItem : group)
            {
                if (context.cfg.isDryRun())
                {
                    // -D Dryrun option
                    ++copyCount;
                    grandTotalItems = grandTotalItems + 1;
                    grandTotalSize = grandTotalSize + groupItem.getSize();
                    logger.info("  > " + context.cfg.gs("Transfer.would.copy") + " #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false, context.cfg.getLongScale()) + ", " + groupItem.getFullPath());
                }
                else
                {
                    String targetPath = getTarget(groupItem.getLibrary(), context.cfg.isRemoteOperation(), totalSize, groupItem.getItemPath());
                    if (targetPath != null)
                    {
                        // copy item(s) to targetPath
                        ++copyCount;
                        totalSize = totalSize - groupItem.getSize();

                        String to = targetPath + context.subscriberRepo.getWriteSeparator();
                        to += context.publisherRepo.normalizePath(context.subscriberRepo.getLibraryData().libraries.flavor, groupItem.getItemPath());

                        String msg = "  > " + context.cfg.gs("Transfer.copying") + " #" + copyCount + ", " + Utils.formatLong(groupItem.getSize(), false, context.cfg.getLongScale()) +
                                ", " + groupItem.getFullPath() + context.cfg.gs("NavTransferHandler.transfer.file.to") + to;
                        logger.info(msg);
                        response += (msg + "\r\n");

                        // only used in Process() and publisher Daemon
                        copyFile(context.clientSftp, groupItem.getFullPath(), groupItem.getModifiedDate(), to, context.cfg.isRemoteOperation(), overwrite);

                        grandTotalItems = grandTotalItems + 1;
                        grandTotalSize = grandTotalSize + groupItem.getSize();
                    }
                    else
                    {
                        throw new MungeException(MessageFormat.format(context.cfg.gs("Transfer.no.space.on.any.target.location"),
                                group.get(0).getLibrary(), lastGroupName, Utils.formatLong(totalSize, false, context.cfg.getLongScale())));
                    }
                }
            }
        }
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
    private long getLocationMinimum(Repository targetRepo, String path)
    {
        long minimum = 0L;
        if (targetRepo.getLibraryData().libraries.locations != null)
        {
            for (Location loc : targetRepo.getLibraryData().libraries.locations)
            {
                boolean match = false;
                if (Utils.isRelativePath(loc.location))
                    match = path.contains(loc.location);
                else
                    match = path.startsWith(loc.location);
                if (match)
                {
                    minimum = Utils.getScaledValue(loc.minimum);
                    break;
                }
            }
        }
        if (minimum < 1L)
        {
            minimum = Storage.MINIMUM_BYTES;
        }
        return minimum;
    }

    /**
     * Get the storage targets either local or remote
     *
     * @throws Exception
     */
    private void getStorageTargets() throws Exception
    {
        String location = null;

        if (context.cfg.isRemoteOperation() && context.cfg.isRequestTargets())
        {
            // request target data from remote subscriber
            location = context.clientStty.retrieveRemoteData("targets", "Requesting targets", 20000);
            context.cfg.setTargetsFilename(location);
        }

        if (location != null && location.length() > 0) 
        {
            if (storageTargets == null)
                storageTargets = new Storage();

            storageTargets.read(location, context.subscriberRepo.getLibraryData().libraries.flavor);
            if (!context.cfg.isRemoteOperation())
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
     * @param library    the publisher library.definition.name
     * @param totalSize  the total size of item(s) to be copied
     * @param itemPath   the getItemPath() value
     * @return the target
     * @throws MungeException the els exception
     */
    private String getTarget(String library, boolean isRemote, long totalSize, String itemPath) throws Exception
    {
        String path = getTarget(context.publisherRepo, library, totalSize, context.subscriberRepo, isRemote, itemPath);
        return path;
    }

    /**
     * Gets a target
     * <p>
     * Will return the original directory where existing files are located if one
     * exists, that location has enough space and back-fill is enabled.
     * <p>
     * Otherwise will return one of the subscriber targets for the library of the item
     * that has enough space to hold the item, otherwise an empty string is returned.
     *
     * @param sourceRepo the source publisher or subscriber repo
     * @param library    the publisher library name
     * @param totalSize  the total size of item(s) to be copied
     * @param targetRepo the target publisher or subscriber repo
     * @param itemPath   the getItemPath() value
     * @return the target
     * @throws MungeException the els exception
     */
    public synchronized String getTarget(Repository sourceRepo, String library, long totalSize, Repository targetRepo, boolean isRemote, String itemPath) throws Exception
    {
        String path = null;
        boolean notFound = true;
        long minimum = 0L;
        Target target = null;

        if (storageTargets != null)
        {
            target = storageTargets.getLibraryTarget(library); // storage targets override locations
        }
        if (target != null)
        {
            minimum = Utils.getScaledValue(target.minimum);
        }

        // see if there is an "original" directory the new content will fit in
        if (!context.cfg.isNoBackFill())
        {
            path = targetRepo.hasDirectory(library, Utils.pipe(sourceRepo, itemPath));
            if (path != null)
            {
                // check size of item(s) to be copied
                if (itFits(targetRepo, path, isRemote, totalSize, minimum, target != null))
                {
                    logger.info(MessageFormat.format(context.cfg.gs("Transfer.using.original.storage.location"), itemPath, path));
                    //
                    // inline return
                    //
                    ++grandTotalOriginalLocation;
                    return path;
                }
                logger.info(MessageFormat.format(context.cfg.gs("Transfer.original.storage.location.too.full"), itemPath, Utils.formatLong(totalSize, false, context.cfg.getLongScale()), path));
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
                if (itFits(targetRepo, candidate, isRemote, totalSize, minimum, true))
                {
                    path = candidate;             // has space, use it
                    break;
                }
            }
        }
        else 
        {
            Library lib = targetRepo.getLibrary(library);
            if (lib != null)
            {
                notFound = false;
                for (int j = 0; j < lib.sources.length; ++j)
                {
                    candidate = lib.sources[j];
                    // check size of item(s) to be copied
                    if (itFits(targetRepo, candidate, isRemote, totalSize, minimum, false))
                    {
                        path = candidate;             // has space, use it
                        break;
                    }
                }
            }
        }
        if (notFound)
        {
            logger.error(context.cfg.gs("Transfer.no.target.library.match.found.for.library") + library);
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
            if (context.cfg.isRemotePublishOperation() || context.cfg.isPublisherListener())
            {
                // sanity checks
                if (context.publisherRepo.getLibraryData().libraries.flavor == null ||
                        context.publisherRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungeException(context.cfg.gs("Transfer.publisher.data.incomplete.missing.flavor"));
                }

                if (context.subscriberRepo.getLibraryData().libraries.flavor == null ||
                        context.subscriberRepo.getLibraryData().libraries.flavor.length() < 1)
                {
                    throw new MungeException(context.cfg.gs("Transfer.subscriber.data.incomplete.missing.flavor"));
                }

                // check for opening commands from Subscriber
                // *** might change localContext.cfg options for subscriber and targets that are handled below ***
                if (context.clientStty.checkBannerCommands())
                {
                    logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
                }
            }

            if (!context.cfg.isLoggerView())
            {
                if (context.cfg.isNavigator())
                {
                    if (context.cfg.isRemoteOperation())
                    {
                        requestLibrary();
                    }
                }
                else
                {
                    // get -s Subscriber libraries
                    if (context.cfg.getSubscriberLibrariesFileName().length() > 0)
                    {
                        if (context.cfg.isRemoteOperation() && context.cfg.isRequestCollection())
                        {
                            requestCollection();
                        }
                    }

                    // get -t|T Targets
                    if (context.cfg.isTargetsEnabled())
                    {
                        logger.info(context.cfg.gs("Transfer.requesting.subscriber.targets"));
                        getStorageTargets();
                    }
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
            logger.warn(context.cfg.gs("Transfer.no.subdirectory.in.path") + publisherItem.getItemPath());
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
     * <br/>
     * Each directory level is checked because drives may be mounted anywhere
     */
    public boolean itFits(Repository targetRepo, String path, boolean isRemote, long totalSize, long minimum, boolean hasTarget) throws Exception
    {
        boolean fits = false;
        long space = getFreespace(path, isRemote);

        if (!hasTarget) // provided targets file overrides subscriber file locations minimum values
        {
            if (targetRepo.getLibraryData().libraries.locations != null &&
                    targetRepo.getLibraryData().libraries.locations.length > 0)
            {
                minimum = getLocationMinimum(targetRepo, path);
            }
            else
                minimum = Storage.MINIMUM_BYTES;
        }

        logger.info(MessageFormat.format(context.cfg.gs("Transfer.checking"), hasTarget ? 0 : 1, Utils.formatLong(totalSize, false, context.cfg.getLongScale()),
                Utils.formatLong(minimum, false, context.cfg.getLongScale()), isRemote ? 0 : 1, path) + (Utils.formatLong(space, false, context.cfg.getLongScale())));

        if (space > (totalSize + minimum))
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
            context.clientSftp.makeDirectory(create); // only used in Navigator
        }
        else
        {
            if (create.matches("^\\\\[a-zA-Z]:.*"))
                create = create.substring(1);
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
     * This is a local-only method
     */
    public boolean move(Repository repo, Hint hint) throws Exception
    {
        boolean libAltered = false;

        logger.info(MessageFormat.format(context.cfg.gs("Transfer.mv.directory.file"), hint.directory ? 0 : 1) +
                "\"" + hint.fromLibrary + "|" + hint.fromItemPath + "\"" + context.cfg.gs("NavTransferHandler.transfer.file.to") + "\"" +
                hint.toLibrary + "|" + hint.toItemPath + "\"");

        Library fromLib = repo.getLibrary(hint.fromLibrary);
        if (fromLib == null)
        {
            logger.info(context.cfg.gs("Transfer.from.library.not.found") + hint.fromLibrary);
            return false;
        }
        Library toLib = repo.getLibrary(hint.toLibrary);
        if (toLib == null)
        {
            logger.info(context.cfg.gs("Transfer.to.library.not.found") + hint.toLibrary);
            return false;
        }

        // scan the toLib if necessary
        if (!toLib.name.equalsIgnoreCase(fromLib.name) && toLib.items == null)
        {
            repo.scan(toLib.name);
        }

        Collection collection = repo.getMapItem(fromLib, Utils.pipe(repo, hint.fromItemPath));
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
                                    String nextPath = hint.toItemPath + repo.getSeparator() + nextName;
                                    if (moveItem(repo, fromLib, nextItem, toLib, nextPath))
                                    {
                                        fromLib.rescanNeeded = true;
                                        toLib.rescanNeeded = true;
                                        libAltered = true;
                                    }
                                }
                            }

                            // remove the physical directory; should be empty at this point
                            if (!context.cfg.isDryRun())
                            {
                                File prevDir = new File(fromItem.getFullPath());
                                if (Utils.removeDirectoryTree(prevDir))
                                {
                                    logger.warn(context.cfg.gs("Transfer.previous.directory.was.not.empty") + fromItem.getFullPath());
                                }
                                fromLib.rescanNeeded = true;
                                toLib.rescanNeeded = true;
                                libAltered = true;
                            }
                        }
                        else // logically it is a rename within same library
                        {
                            // rename the directory
                            if (moveItem(repo, fromLib, fromItem, toLib, hint.toItemPath))
                            {
                                fromLib.rescanNeeded = true;
                                toLib.rescanNeeded = true;
                                libAltered = true;
                            }
                        }
                    }
                    else // it is a file
                    {
                        if (moveItem(repo, fromLib, fromItem, toLib, hint.toItemPath))
                        {
                            fromLib.rescanNeeded = true;
                            toLib.rescanNeeded = true;
                            libAltered = true;
                        }
                    }
                }
            }
            else
            {
                logger.info(context.cfg.gs("Transfer.does.not.exist.skipping") + hint.fromLibrary + "|" + hint.fromItemPath);
            }
        }
        else
        {
            logger.info(context.cfg.gs("Transfer.does.not.exist.skipping") + hint.fromLibrary + "|" + hint.fromItemPath);
        }

        return libAltered;
    }

    /**
     * Move a local file
     * <p>
     * This is a local-only method
     *
     * @param from      the full from path
     * @param to        the full to path
     * @param overwrite true/false
     */
    public synchronized void moveFile(String from, FileTime filetime, String to, boolean overwrite) throws Exception
    {
        Path fromPath = Paths.get(from).toRealPath();
        Path toPath = Paths.get(to);
        File f = new File(to);
        if (f != null)
        {
            f.getParentFile().mkdirs();
        }
        if (context.cfg.isPreserveDates() && filetime != null)
            Files.move(fromPath, toPath, (overwrite) ? REPLACE_EXISTING : null);
        else
        {
            Files.move(fromPath, toPath, (overwrite) ? REPLACE_EXISTING : null);
            touch(to, false);
        }
    }

    /**
     * Move an individual collection item, directory or file
     * <p>
     * This is a local-only method
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

            if (context.cfg.isDryRun())
            {
                logger.info(MessageFormat.format(context.cfg.gs("Transfer.would.mv.directory.file"), fromItem.isDirectory() ? 0 : 1) +
                        "\"" + fromLib.name + "|" + fromPath + "\"" + context.cfg.gs("NavTransferHandler.transfer.file.to") + "\"" +
                        toLib.name + "|" + toPath + "\"");
                return false;
            }

            // perform move / rename
            File toFile = new File(toPath);
            if (toFile.exists())
            {
                logger.info(context.cfg.gs("Transfer.target.exists.will.overwrite") + toItem.getFullPath());
            }

            // make sure the parent directories exist
            if (toFile.getParentFile().mkdirs())
            {
                toLib.rescanNeeded = true;
                libAltered = true;
            }

            Files.move(fromFile.toPath(), toFile.toPath(), REPLACE_EXISTING);
            libAltered = true;
        }
        else
        {
            logger.info(context.cfg.gs("Transfer.does.not.exist.skipping") + fromItem.getFullPath());
        }
        return libAltered;
    }

    public String readTextFile(NavTreeUserObject tuo) throws Exception
    {
        String content = "";
        List<String> lines = null;
        if (tuo.isRemote)
        {
            String path = "\"" + tuo.path + "\"";
            context.clientStty.send("read " + path, "Read text file " + path);
            content = context.clientStty.receive("Reading", 10000);
            if (content.equalsIgnoreCase("false"))
                content = "";
        }
        else
        {
            content = Utils.readString(tuo.path);
        }
        return content;
    }

    /**
     * Remove a file, local or remote
     *
     * @param sftp
     * @param path     the full from path
     * @param isDir    if this specific path is a directory
     * @param isRemote if this specific path is remote
     */
    public void remove(ClientSftp sftp, String path, boolean isDir, boolean isRemote) throws Exception
    {
        if (isRemote)
        {
            sftp.remove(path, isDir);
        }
        else
        {
            if (path.matches("^\\\\[a-zA-Z]:.*"))
                path = path.substring(1);
            Path fromPath = Paths.get(path).toRealPath();
            Files.delete(fromPath);
        }
    }

    /**
     * Remove a collection item, directory or file
     * <p>
     * If a directory ALL contents are deleted recursively.
     * <p>
     * This is a local-only method
     */
    public boolean remove(Repository repo, Hint hint) throws Exception
    {
        boolean libAltered = false;

        Library fromLib = repo.getLibrary(hint.fromLibrary);
        if (fromLib == null)
        {
            logger.info(context.cfg.gs("Transfer.from.library.not.found") + hint.fromLibrary);
            return false;
        }

        Collection collection = repo.getMapItem(fromLib, Utils.pipe(repo, hint.fromItemPath));
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
                        if (context.cfg.isDryRun())
                        {
                            logger.info(context.cfg.gs("Transfer.would.rm.directory") + "\"" + hint.fromLibrary + "|" + fromItem.getFullPath() + "\"");
                        }
                        else
                        {
                            // remove the physical directory
                            String rmPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, fromItem.getFullPath());
                            File rmdir = new File(rmPath);
                            if (Utils.removeDirectoryTree(rmdir))
                            {
                                logger.warn(context.cfg.gs("Transfer.previous.directory.was.not.empty") + fromItem.getFullPath());
                            }
                            logger.info(context.cfg.gs("Transfer.rm.directory") + "\"" + fromItem.getFullPath() + "\"");
                            fromLib.rescanNeeded = true;
                            libAltered = true;
                        }
                    }
                    else // it is a file
                    {
                        if (context.cfg.isDryRun())
                        {
                            logger.info(context.cfg.gs("Transfer.would.rm.file") + hint.fromLibrary + "|" + fromItem.getFullPath());
                        }
                        else
                        {
                            String rmPath = repo.normalizePath(repo.getLibraryData().libraries.flavor, fromItem.getFullPath());
                            File rmFile = new File(rmPath);
                            if (rmFile.delete())
                            {
                                logger.info(context.cfg.gs("Transfer.rm.file") + "\"" + fromItem.getFullPath() + "\"");
                                fromLib.rescanNeeded = true;
                                libAltered = true;
                            }
                        }
                    }
                }
            }
            else
            {
                logger.info(context.cfg.gs("Transfer.does.not.exist.skipping") + hint.fromLibrary + "|" + hint.fromItemPath);
            }
        }
        else
        {
            logger.info(context.cfg.gs("Transfer.does.not.exist.skipping") + hint.fromLibrary + "|" + hint.fromItemPath);
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
            context.clientSftp.rename(from, to); // only used in Navigator
        }
        else
        {
            if (to.matches("^\\\\[a-zA-Z]:.*"))
                to = to.substring(1);
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
        if (context.cfg.isRemoteOperation())
        {
            // request collection data from remote subscriber
            String location = context.clientStty.retrieveRemoteData("collection",
                    context.cfg.gs("Transfer.requesting.subscriber.collection"), -1);
            if (location == null || location.length() < 1)
                throw new MungeException(context.cfg.gs("Transfer.could.not.retrieve.remote.collection.file"));
            context.cfg.setSubscriberLibrariesFileName(""); // clear so the collection file will be used
            context.cfg.setSubscriberCollectionFilename(location);
            context.subscriberRepo.read(context.cfg.getSubscriberCollectionFilename(), "Subscriber", true);
        }
    }

    /**
     * Request the remote end send it's library JSON for the Navigator
     *
     * @throws Exception
     */
    public void requestLibrary() throws Exception
    {
        if (context.cfg.isRemoteOperation())
        {
            // request collection data from remote subscriber
            String location = context.clientStty.retrieveRemoteData("library",
                    context.cfg.gs("Transfer.requesting.subscriber.library"), 20000);
            if (location == null || location.length() < 1)
                throw new MungeException(context.cfg.gs("Transfer.could.not.retrieve.remote.library.file"));
            context.cfg.setSubscriberCollectionFilename(""); // clear so the library file will be used
            context.cfg.setSubscriberLibrariesFileName(location);
            context.subscriberRepo.read(context.cfg.getSubscriberLibrariesFileName(), "Subscriber", true);
            context.subscriberRepo.setDynamic(true);
        }
    }

    /**
     * Setup a To Item either new or a copy of an existing Item
     * <p>
     * This is a local-only method
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
                path = getTarget(repo, toItem.getLibrary(), toItem.getSize(), repo, false, toItem.getItemPath());
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
        long millis = System.currentTimeMillis();
        if (isRemote)
        {
            context.clientSftp.setDate(path, (int) (millis / 1000l));
        }
        else
        {
            if (path.matches("^\\\\[a-zA-Z]:.*"))
                path = path.substring(1);
            Path file = Paths.get(path);
            FileTime ft = FileTime.fromMillis(millis);
            Files.setLastModifiedTime(file, ft);
        }
        millis = millis / 1000l;
        return millis;
    }

}
