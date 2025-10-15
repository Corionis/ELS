package com.corionis.els.tools.archiver;

import com.corionis.els.Context;
import com.corionis.els.Persistent;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Task;
import com.corionis.els.repository.Library;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class ArchiverTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Archiver";
    public static final String SUBSYSTEM = "tools";

    private String configName;
    private final String internalName = INTERNAL_NAME;
    private boolean appendPubSub = true;
    private boolean appendDate = true;
    private boolean deleteFiles = true;
    private String format = "zip";
    private String target = "";

    transient private int count = 0;
    transient private int deleteCount = 0;
    transient private ArrayList<String> filesToCompress = null;
    transient private Logger logger = LogManager.getLogger("applog");
    transient private String outputFilename = "";
    transient private Repository pubRepo = null;
    transient private Repository subRepo = null;
    transient private ArrayList<String> toolPaths;
    // @formatter:on

    /**
     * Constructor when used from the command line
     *
     * @param context Context
     */
    public ArchiverTool(Context context)
    {
        super(context);
        setDisplayName(getCfg().gs("Archiver.displayName"));
        this.context = context;
        this.dataHasChanged = false;
    }

    public ArchiverTool clone()
    {
        assert context != null;
        ArchiverTool tool = new ArchiverTool(context);
        tool.setConfigName(getConfigName());
        tool.setDisplayName(getDisplayName());
        tool.setAppendPubSub(isAppendPubSub());
        tool.setAppendDate(isAppendDate());
        tool.setDeleteFiles(isDeleteFiles());
        tool.setFormat(getFormat());
        tool.setTarget(getTarget());
        tool.setDataHasChanged();
        tool.setRemote(this.isRemote());
        return tool;
    }

    private void deleteFiles()
    {
        String msg = "";
        for  (String path : filesToCompress)
        {
            File file = new File(path);
            if (file.exists())
            {
                file.delete();
                msg = "  - " + context.cfg.gs("Z.deleted") + path;
                logger.info(msg);
                ++deleteCount;
            }
            else
            {
                logger.warn(context.cfg.gs("Archiver.file.disappeared") + path);
            }
        }
    }

    private void expandOrigins(ArrayList<Origin> origins) throws MungeException
    {
        // this tool only uses one repository
        if (pubRepo == null)
            return;

        for (Origin origin : origins)
        {
            if (origin.getType() == NavTreeUserObject.COLLECTION)
            {
                if (origin.getLocation().length() > 0)
                {
                    if (!pubRepo.getLibraryData().libraries.description.equalsIgnoreCase(origin.getLocation()))
                        throw new MungeException((context.cfg.gs("Archiver.task.definition.and.loaded.repository.do.not.match")));
                }
                // process in the order defined in the JSON
                for (Library lib : pubRepo.getLibraryData().libraries.bibliography)
                {
                    for (String source : lib.sources)
                    {
                        toolPaths.add(source);
                    }
                }
            }
            else if (origin.getType() == NavTreeUserObject.LIBRARY)
            {
                for (Library lib : pubRepo.getLibraryData().libraries.bibliography)
                {
                    if (lib.name.equalsIgnoreCase(origin.getLocation()))
                    {
                        for (String source : lib.sources)
                        {
                            toolPaths.add(source);
                        }
                    }
                }
            }
            else if (origin.getType() == NavTreeUserObject.REAL)
            {
                toolPaths.add(origin.getLocation());
            }
        }
    }

    public String getConfigName()
    {
        return configName;
    }

     @Override
    public String getDisplayName()
    {
        return displayName;
    }

    public String formatArchiveFilename()
    {
        String archiveName = "";
        String name = "";
        if (getTarget().length() > 0)
        {
            // remove any left-side path
            String sep = Utils.getSeparatorFromPath(getTarget());
            if (sep.length() > 0)
            {
                name = Utils.pipe(getTarget(), sep);
                int p = name.lastIndexOf("|");
                if (p >= 0 && p < name.length() - 1)
                    name = name.substring(p + 1);
                else
                    name = "";
            }
            else
                name = getTarget();
        }
        archiveName = name;

        if (isAppendPubSub())
        {
            if (pubRepo != null)
                archiveName += pubRepo.getLibraryData().libraries.description;
            if (pubRepo != null && subRepo != null)
                archiveName += "-";
            if (subRepo != null)
                archiveName += subRepo.getLibraryData().libraries.description;
        }

        if (isAppendDate())
        {
            long utc = Instant.now().toEpochMilli();
            Instant instant = Instant.ofEpochMilli(utc);
            ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
            if (archiveName.length() > 0)
                archiveName += " ";
            archiveName += zdt.format(DateTimeFormatter.ofPattern(context.preferences.getDateFormat()));
        }

        if (archiveName.length() > 0)
        {
            if (getFormat().equals("tar"))
                archiveName += ".tar";
            else
                archiveName += ".zip";
        }

        return archiveName;
    }

    public String formatArchiveFilename(Repository pubRepo, Repository subRepo)
    {
        this.pubRepo = pubRepo;
        this.subRepo = subRepo;
        return formatArchiveFilename();
    }

    public String getFormat()
    {
        return format;
    }

    @Override
    public String getInternalName()
    {
        return internalName;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    public String getTarget()
    {
        return target;
    }

    public String getTargetDirectory()
    {
        String directory = "";
        String sep = Utils.getSeparatorFromPath(getTarget());
        if (sep.length() > 0)
        {
            directory = Utils.getLeftPath(getTarget(), sep);
            if (Utils.isRelativePath(directory))
                directory = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + directory;
        }
        else
        {
            directory = context.cfg.getWorkingDirectory();
        }
        return directory;
    }

    public boolean isAppendDate()
    {
        return appendDate;
    }

    public boolean isAppendPubSub()
    {
    return appendPubSub;
    }

    public boolean isDataChanged()
    {
        return dataHasChanged; // used by the GUI
    }

    public boolean isDeleteFiles()
    {
        return deleteFiles;
    }

    @Override
    public boolean isToolOriginsUsed()
    {
        return true;
    }

    @Override
    public boolean isToolPubOrSub()
    {
        return true;
    }

    @Override
    public boolean isToolSubscriber()
    {
        return false;
    }

    @Override
    public void processTool(Task task) throws Exception
    {
        reset();

        if (pubRepo == null)
            pubRepo = context.publisherRepo;
        if (pubRepo == null)
            pubRepo = Persistent.lastPublisherRepo;

        if (subRepo == null)
            subRepo = context.subscriberRepo;
        if (subRepo == null)
            subRepo = Persistent.lastSubscriberRepo;

        if (pubRepo == null)
        {
            logger.error(java.text.MessageFormat.format(context.cfg.gs("Archiver.has.no.repository.defined"), getConfigName()));
            return;
        }

        // expand origins into physical toolPaths
        expandOrigins(task.origins);
        if (toolPaths == null || toolPaths.size() == 0)
            return;

        for (String path : toolPaths)
        {
            if (isRequestStop())
                break;
            logger.info(getDisplayName() + ", " + getConfigName() + ": " + path);

            // get fully-qualified list of filesToCompress
            scanForFiles(path);
        }

        if (!filesToCompress.isEmpty())
        {
            // zip/tar
            if (getFormat().equalsIgnoreCase("zip"))
                zipFiles();
            else if (getFormat().equalsIgnoreCase("tar"))
                tarFiles();
            else
                throw new MungeException(context.cfg.gs("Archiver.unknown.archiver.format") + getFormat());

            // delete
            if (isDeleteFiles())
                deleteFiles();

            String msg = getDisplayName() + ", " + getConfigName() + context.cfg.gs("Archiver.compressed") +
                    count + context.cfg.gs("Archiver.deleted") + deleteCount  + context.cfg.gs("Archiver.files.successfully.to") + outputFilename;
            logger.info(msg);
            if (context != null && context.cfg.isNavigator() && !context.cfg.isLoggerView())
            {
                if (context.navigator.dialogArchiver != null && context.navigator.dialogArchiver.isShowing())
                    context.navigator.dialogArchiver.labelStatus.setText(msg);

                // reset and reload relevant trees
                if (deleteCount > 0)
                {
                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false);
                    context.browser.loadSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false);
                }
            }
        }
        else
            logger.warn(context.cfg.gs("Archiver.no.files.found.to.compress"));
    }

    public boolean isToolPublisher()
    {
        return false;
    }

    public void reset()
    {
        count = 0;
        deleteCount = 0;
        resetStop();
        toolPaths = new ArrayList<>();
        filesToCompress = new ArrayList<>();
        if (logger == null)
            logger = LogManager.getLogger("applog");
    }

    private boolean scanForFiles(String path)
    {
        boolean hadError = false;

        // is local
        try
        {
            File file = new File(Utils.getFullPathLocal(path));
            if (!file.isDirectory())
            {
                filesToCompress.add(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getAbsoluteFile().toString()));
                return true;
            }

            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(path));
            for (Path entry : directoryStream)
            {
                if (isRequestStop())
                    break;

                String filename = entry.toAbsolutePath().toString();
                boolean isDir = Files.isDirectory(entry);
                if (isDir)
                {
                    scanForFiles(filename);
                }
                else
                {
                    filesToCompress.add(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), filename));
                }
            }
        }
        catch (Exception e)
        {
            hadError = true;
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            logger.error(msg);
            if (context != null)
            {
                msg = context.cfg.gs("Z.exception") + " " + e.getMessage();
                if (context.navigator != null)
                {
                    int reply = JOptionPane.showConfirmDialog(context.navigator.dialogArchiver, msg, context.cfg.gs("ArchiverUI.title"),
                            JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                        requestStop();
                }
            }
        }

        return hadError;
    }

    public void setAppendDate(boolean appendDate)
    {
        this.appendDate = appendDate;
    }

    public void setAppendPubSub(boolean appendPubSub)
    {
        this.appendPubSub = appendPubSub;
    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setDeleteFiles(boolean deleteFiles)
    {
        this.deleteFiles = deleteFiles;
    }

    public void setFormat(String format)
    {
        this.format = format;
    }

    public void setTarget(String target)
    {
        this.target = target;
    }

    private void tarFiles() throws Exception
    {
        // Apache Commons Compress
        // https://commons.apache.org/proper/commons-compress
        if (filesToCompress != null && !filesToCompress.isEmpty())
        {
            String directory = getTargetDirectory();
            String tarFilePath = formatArchiveFilename();
            tarFilePath = directory + System.getProperty("file.separator") + tarFilePath;
            tarFilePath = Utils.pipe(tarFilePath);
            tarFilePath = Utils.unpipe(tarFilePath, System.getProperty("file.separator"));

            File parent = new File(directory);

            if (!parent.exists())
                parent.mkdirs();

            File outputFile = new File(tarFilePath);
            tarFilePath = outputFile.getAbsolutePath();
            outputFilename = tarFilePath;

            TarArchiveOutputStream taos = new TarArchiveOutputStream(new FileOutputStream(tarFilePath));

            for (String path : filesToCompress)
            {
                File input = new File(path);
                if (!input.exists())
                {
                    logger.warn(context.cfg.gs("Repository.file.does.not.exist") + path);
                    continue;
                }

                TarArchiveEntry entry = new TarArchiveEntry(input, path);
                entry.setModTime(input.lastModified());
                taos.putArchiveEntry(entry);

                try (FileInputStream fis = new FileInputStream(path))
                {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0)
                    {
                        taos.write(buffer, 0, len);
                    }
                    ++count;
                    logger.info("  + " + context.cfg.gs("Archiver.compressed.file") + path);
                }
                taos.closeArchiveEntry();
            }
            taos.close();
            logger.info(context.cfg.gs("Archiver.compressed") + count + context.cfg.gs("Archiver.files.successfully.to") + tarFilePath);
        }
    }

    public void write() throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(getCfg().gs("Z.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

    private void zipFiles() throws Exception
    {
        // Apache Commons Compress
        // https://commons.apache.org/proper/commons-compress
        if (filesToCompress != null && !filesToCompress.isEmpty())
        {
            String directory = getTargetDirectory();
            String zipFilePath = formatArchiveFilename();
            zipFilePath = directory + System.getProperty("file.separator") + zipFilePath;
            zipFilePath = Utils.pipe(zipFilePath);
            zipFilePath = Utils.unpipe(zipFilePath, System.getProperty("file.separator"));

            File parent = new File(directory);

            if (!parent.exists())
                parent.mkdirs();

            File outputFile = new File(zipFilePath);
            zipFilePath = outputFile.getAbsolutePath();
            outputFilename = zipFilePath;

            ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(new FileOutputStream(zipFilePath));

            for (String path : filesToCompress)
            {
                File input = new File(path);
                if (!input.exists())
                {
                    logger.warn(context.cfg.gs("Repository.file.does.not.exist") + path);
                    continue;
                }

                ZipArchiveEntry entry = new ZipArchiveEntry(path);
                entry.setLastModifiedTime(FileTime.fromMillis(input.lastModified()));
                zaos.putArchiveEntry(entry);

                try (FileInputStream fis = new FileInputStream(path))
                {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0)
                    {
                        zaos.write(buffer, 0, len);
                    }
                    ++count;
                    logger.info("  + " + context.cfg.gs("Archiver.compressed.file") + path);
                }
                zaos.closeArchiveEntry();
            }
            zaos.close();
        }
    }
}
