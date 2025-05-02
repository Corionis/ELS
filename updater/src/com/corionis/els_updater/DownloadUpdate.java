package com.corionis.els_updater;

import com.corionis.els.Configuration;
import com.corionis.els.Utils;
import com.corionis.els.gui.Preferences;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.Set;

public class DownloadUpdate extends JFrame
{
    private boolean fault = false;
    private String installedPath;
    private Logger logger = LogManager.getLogger("applog");
    private String message;
    private Main main;
    private DownloadUpdate me;
    private boolean mockMode;
    private String outFile;
    private Preferences preferences;
    private String prefix;
    private boolean requestStop = false;
    private Marker SHORT = MarkerManager.getMarker("SHORT");
    private String updateFile;
    private URL url;
    private ArrayList<String> version = new ArrayList<>();
    private Worker worker;

    public DownloadUpdate(Main main, Preferences preferences, String installedPath, ArrayList<String> version, String prefix)
    {
        super();
        this.me = this;
        this.main = main;
        this.mockMode = main.mockMode;
        this.preferences = preferences;
        this.installedPath = installedPath;
        this.version = version;
        this.prefix = prefix;
        if (!main.isInstallUpdate())
            initComponents();

        process();
    }

    private void actionCancel(ActionEvent e)
    {
        logger.info(main.cfg.gs("Updater.action.cancelled"));
        requestStop = true;
    }

    private void actionWindowClosed(WindowEvent e)
    {
        actionCancel(null);
    }

    private void process()
    {
        if (!main.isInstallUpdate())
        {
            setIconImage(new ImageIcon(getClass().getResource("/els-logo-48px.png")).getImage());
            if (preferences != null)
            {
                int x = preferences.getAppXpos() + (preferences.getAppWidth() / 2) - (getWidth() / 2);
                int y = preferences.getAppYpos() + (preferences.getAppHeight() / 2) - (getHeight() / 2);
                setLocation(x, y);
            }
            setVisible(true);
            buttonCancel.setSelected(false);
        }

        worker = new Worker();
        worker.execute();

        if (main.isInstallUpdate()) // -Y command line, wait for worker thread to finish
        {
            try
            {
                while (!worker.isDone())
                {
                    Thread.sleep(100);
                }
            }
            catch (Exception e)
            {
                logger.info("THREAD INTERRUPTED");
            }
        }
    }

    // ==========================================

    class Worker extends SwingWorker<Void, Void>
    {
        @Override
        protected Void doInBackground() throws Exception
        {
            if (!main.isInstallUpdate())
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                progressBar.setMinimum(0);
            }

            if (!requestStop && preprocess())
            {
                if (!requestStop && download())
                {
                    // give the download a chance to flush buffers and close the file
                    Thread.sleep(2000);

                    if (!requestStop && unpack())
                    {
                        if (!requestStop && postprocess())
                        {
                            logger.info(SHORT, main.cfg.gs("Updater.download.and.unpack.of.els.successful"));
                            if (main.isInstallUpdate())
                                System.out.println(main.cfg.gs("Updater.download.and.unpack.of.els.successful"));
                        }
                        else
                            fault = true;
                    }
                    else
                        fault = true;
                }
                else
                    fault = true;
            }
            else
                fault = true;

            if (fault)
            {
                if (!main.isInstallUpdate())
                {
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
            }
            return null;
        }

        @Override
        public void done()
        {
            if (!main.isInstallUpdate())
            {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                setVisible(false);
            }

            if (requestStop)
            {
                logger.warn(SHORT, main.cfg.gs("Navigator.els.update.process.cancelled"));
            }
            else
            {
                if (!fault)
                {
                    if (!main.isInstallUpdate())
                    {
                        // double-check the ELS.jar exists before Swing is ended
                        String exe = installedPath + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + main.cfg.ELS_JAR;
                        File els = new File(exe);
                        if (!els.exists())
                        {
                            fault = true;
                            message = main.cfg.gs("Navigator.cannot.find.executable") + exe;
                            Object[] opts = {main.cfg.gs("Z.ok")};
                            JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                                    JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                        }
                    }
                }
            }

            // polish-off the updates, delete or restore back-up directories
            polish();

            main.stop(fault, requestStop);
        }

        private boolean download()
        {
            try
            {
                // extention is based on the operating system
                // there is no DMG download because of Apple
                String ext = Utils.isOsWindows() ? ".zip" : (Utils.isOsMac() ? ".mac.tar.gz" : ".tar.gz");
                updateFile = version.get(Configuration.BUILD_ELS_DISTRO) + ext;
                outFile = main.getUpdaterPath() + System.getProperty("file.separator") + updateFile;

                if (!main.isInstallUpdate())
                {
                    labelVersion.setText(version.get(Configuration.BUILD_VERSION_NAME));
                    labelStatus.setText(main.cfg.gs("Z.update") + version.get(Configuration.BUILD_DATE));
                }

                if (!mockMode)
                {
                    // download the ELS Updater
                    String downloadUrl = prefix + "/" + updateFile;
                    logger.info(SHORT, main.cfg.gs("Updater.downloading") + " " + downloadUrl);
                    if (main.isInstallUpdate())
                        System.out.print(main.cfg.gs("Navigator.install.downloading"));

                    url = new URL(downloadUrl);
                    URLConnection connection = url.openConnection();

                    int contentLength = connection.getContentLength();

                    if (!main.isInstallUpdate())
                    {
                        progressBar.setMaximum(contentLength);
                        progressBar.setValue(0);
                    }

                    InputStream raw = connection.getInputStream();
                    InputStream in = new BufferedInputStream(raw);
                    byte[] data = new byte[contentLength];
                    int count = 0;
                    int offset = 0;
                    int tickLast = 0;
                    int tickMax = contentLength / 43;
                    while (offset < contentLength && !requestStop)
                    {
                        count = in.read(data, offset, data.length - offset);
                        if (count == -1)
                            break;
                        offset += count;
                        tickLast += count;

                        if (!main.isInstallUpdate())
                            progressBar.setValue(offset);
                        else
                        {
                            if (tickLast >= tickMax)
                            {
                                System.out.print("#");
                                tickLast = 0;
                            }
                        }
                    }
                    in.close();
                    if (main.isInstallUpdate())
                        System.out.println();

                    if (!requestStop)
                    {
                        try
                        {
                            FileOutputStream out = new FileOutputStream(outFile);
                            out.write(data);
                            out.flush();
                            out.close();
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            message = main.cfg.gs("Z.error.writing") + outFile + ", " + e.getMessage();
                            if (!main.isInstallUpdate())
                            {
                                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                Object[] opts = {main.cfg.gs("Z.ok")};
                                JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                            }
                            else
                                System.out.println(message);
                            fault = true;
                            return false;
                        }
                    }

                    if (requestStop)
                    {
                        // remove partial download
                        File dl = new File(outFile);
                        if (dl.exists())
                            dl.delete();
                        return false;
                    }
                }
                else
                {
                    // in mockMode copy ELS from the build directory
                    File dl = new File(outFile);
                    if (!dl.exists())
                    {
                        String copy = installedPath + System.getProperty("file.separator") + ".." + System.getProperty("file.separator") + "build" + System.getProperty("file.separator") + updateFile;
                        File cp = new File (copy);
                        if (cp.exists())
                        {
                            if (main.isInstallUpdate())
                                System.out.println(main.cfg.gs("Updater.copying") + copy);
                            Files.copy(Paths.get(copy), Paths.get(outFile));
                        }
                        else
                        {
                            message = java.text.MessageFormat.format(main.cfg.gs("Navigator.update.not.found"), outFile);
                            logger.error(message);
                            if (!main.isInstallUpdate())
                            {
                                Object[] opts = {main.cfg.gs("Z.ok")};
                                JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                            }
                            else
                                System.out.println(message);
                            fault = true;
                            return false;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                message = main.cfg.gs("Z.error.downloading") + outFile + ", " + e.getMessage();
                if (!main.isInstallUpdate())
                {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                {
                    System.out.println();
                    System.out.println(message);
                }
                fault = true;
                return false;
            }
            return true;
        }

        private boolean polish()
        {
            // if successful remove back-ups
            if (!fault && !requestStop)
            {
                logger.info(SHORT, main.cfg.gs("Updater.removing.original.backups"));
                if (main.isInstallUpdate())
                    System.out.println(main.cfg.gs("Updater.removing.original.backups"));
                String directory = installedPath + System.getProperty("file.separator") + "bin_back";
                if (removeDirectory(directory))
                {
                    directory = installedPath + System.getProperty("file.separator") + "rt_back";
                    if (!removeDirectory(directory))
                        return false;
                }
                else
                    return false;
            }
            else // otherwise remove any failed directory and restore originals from back-ups
            {
                logger.info(SHORT, (requestStop ? main.cfg.gs("Updater.removing.cancelled.directories") :
                        main.cfg.gs("Updater.removing.failed.directories")));
                String directory = installedPath + System.getProperty("file.separator") + "bin";
                if (removeDirectory(directory))
                {
                    directory = installedPath + System.getProperty("file.separator") + "rt";
                    if (removeDirectory(directory))
                    {
                        logger.info(SHORT, main.cfg.gs("Updater.renaming.backups.to.original"));
                        String to = installedPath + System.getProperty("file.separator") + "bin";
                        String from = to + "_back";
                        if (renameDirectory(from, to))
                        {
                            to = installedPath + System.getProperty("file.separator") + "rt";
                            from = to + "_back";
                            if (!renameDirectory(from, to))
                                return false;
                        }
                    }
                    else
                        return false;
                }
            }
            return true;
        }

        private boolean postprocess()
        {
            // other things after updates
            // ...

            return true;
        }

        private boolean preprocess()
        {
            // handle changes to the installed ELS before the upgrade
            // ...

            // rename directories for back-ups
            logger.info(SHORT, main.cfg.gs("Updater.renaming.original.for.backup"));
            if (main.isInstallUpdate())
                System.out.println(main.cfg.gs("Updater.renaming.original.for.backup"));
            String from = installedPath + System.getProperty("file.separator") + "bin";
            String to = from + "_back";
            if (renameDirectory(from, to))
            {
                from = installedPath + System.getProperty("file.separator") + "rt";
                to = from + "_back";
                if (!renameDirectory(from, to))
                    return false;
            }
            else
                return false;

            return true;
        }

        private boolean removeDirectory(String directory)
        {
            try
            {
                File dir = new File(directory);
                if (dir.exists())
                {
                    FileUtils.deleteDirectory(dir);
                }
            }
            catch (Exception e)
            {
                // ignore any fault, it might not be there
            }
            return true;
        }

        private boolean renameDirectory(String from, String to)
        {
            try
            {
                File dir = new File(from);
                if (dir.exists())
                {
                    File renameTo = new File(to);
                    dir.renameTo(renameTo);
                }
            }
            catch (Exception e)
            {
                fault = true;
                logger.error(Utils.getStackTrace(e));
                message = main.cfg.gs("Z.exception" + e.getMessage());
                if (!main.isInstallUpdate())
                {
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                    System.out.println(message);
                fault = true;
                return false;
            }
            return true;
        }

        private boolean unpack()
        {
            boolean success = false;
            if (!main.isInstallUpdate())
            {
                setTitle(main.cfg.gs("Navigator.unpacking"));
                buttonCancel.setEnabled(false);
            }

            logger.info(SHORT, main.cfg.gs("Updater.unpacking") + outFile);
            if (main.isInstallUpdate())
                System.out.println(main.cfg.gs("Updater.unpacking") + outFile);
            if (updateFile.endsWith(".zip"))
                success = unpackZip(outFile, installedPath);
            else
                if (updateFile.endsWith(".dmg"))
                    success = unpackDmg(outFile, installedPath);
                else
                    success = unpackTar(outFile, installedPath);

            if (!main.isInstallUpdate())
                buttonCancel.setEnabled(true);
            return success;
        }

        private boolean unpackDmg(String from, String to)
        {
            boolean success = false;

            if (!main.isInstallUpdate())
            {
                progressBar.setMaximum(4);
                progressBar.setValue(0);
            }
            else
                System.out.print(main.cfg.gs("Updater.unpacking"));

            String outPath = main.getUpdaterPath();

            try
            {
                logger.info(main.cfg.gs("Updater.mounting.update") + from);
                String[] parms = new String[]{"/usr/bin/hdiutil", "attach", from, "-mountroot", outPath};
                if (main.execExternalExe(me, main.cfg, parms))
                {
                    if (!main.isInstallUpdate())
                        progressBar.setValue(1);
                    else
                        System.out.print("#");

                    // copy Java/ directory files
                    logger.info(main.cfg.gs("Updater.copy") + "ELS.app/Contents/Java");
                    File fromDir = new File(outPath + "/ELS - Entertainment Library Synchronizer/ELS.app/Contents/Java");
                    File toDir = new File(to + "/Contents/Java");
                    FileUtils.copyDirectory(fromDir, toDir, true);

                    if (!main.isInstallUpdate())
                        progressBar.setValue(2);
                    else
                        System.out.print("#");

                    // copy Plugins/rt/ directory files
                    logger.info(main.cfg.gs("Updater.copy") + "ELS.app/Contents/Plugins/rt");
                    fromDir = new File(outPath + "/ELS - Entertainment Library Synchronizer/ELS.app/Contents/Plugins/rt");
                    toDir = new File(to + "/Contents/Plugins/rt");
                    FileUtils.copyDirectory(fromDir, toDir, true);

                    if (!main.isInstallUpdate())
                        progressBar.setValue(3);
                    else
                        System.out.print("#");

                    Thread.sleep(2000);

                    // detach ELS
                    logger.info(main.cfg.gs("Updater.unmounting") + from);
                    parms = new String[]{"/usr/bin/hdiutil", "detach", outPath + "/ELS - Entertainment Library Synchronizer", "-force", "-verbose"};
                    success = main.execExternalExe(me, main.cfg, parms);

                    if (!main.isInstallUpdate())
                        progressBar.setValue(4);
                    else
                        System.out.println("#");
                }
            }
            catch (Exception e)
            {
                fault = true;
                logger.error(Utils.getStackTrace(e));
                message = main.cfg.gs("Z.exception" + Utils.getStackTrace(e));
                if (!main.isInstallUpdate())
                {
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                {
                    System.out.println();
                    System.out.println(message);
                }
            }
            return success;
        }

        private boolean unpackTar(String from, String to)
        {
            // unpack well-defined archive .tar.gz to use same download as users
            try
            {
                // Apache Commons Compress
                // https://commons.apache.org/proper/commons-compress
                Path path = Paths.get(from);
                Path outPath = Paths.get(to);
                long size = Files.size(path);
                if (!main.isInstallUpdate())
                {
                    progressBar.setMaximum((int) size);
                    progressBar.setValue(0);
                }
                else
                    System.out.print(main.cfg.gs("Updater.unpacking"));

                GzipCompressorInputStream gin = new GzipCompressorInputStream(new BufferedInputStream(Files.newInputStream(path)));
                TarArchiveInputStream in = new TarArchiveInputStream(gin);
                TarArchiveEntry entry = null;
                long gcount = 0L;
                long ticklast = 0L;
                long tickMax = size / 42L;
                while ((entry = in.getNextTarEntry()) != null)
                {
                    gcount = gin.getCompressedCount();
                    long gread = gin.getBytesRead();
                    long bread = in.getBytesRead();
                    if (gread != bread)
                    {
                        logger.fatal(java.text.MessageFormat.format(main.cfg.gs("Updater.bytes.read.and.bytes.decompressed"),
                                gread, bread, entry.getName()));
                        System.exit(1);
                    }

                    String name = entry.getName();
                    if (name.length() == 0)
                        continue;
                    if (name.startsWith("ELS"))
                    {
                        if (name.equals("ELS/"))
                            continue;
                        name = name.substring(4, name.length());
                    }

                    Path entryPath = outPath.resolve(name);
                    logger.info(SHORT, "  " + entryPath.toString());
                    if (entry.isDirectory())
                    {
                        if (!Files.exists(entryPath))
                            Files.createDirectories(entryPath);
                        Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                    }
                    else
                    {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());

                        int mode = entry.getMode();
                        Set<PosixFilePermission> perms = Utils.translateModeToPosix(mode);
                        Files.setPosixFilePermissions(entryPath, perms);

                        if (!main.isInstallUpdate())
                            progressBar.setValue((int)gcount);
                        else
                        {
                            if (gread - ticklast > tickMax)
                            {
                                System.out.print("#");
                                ticklast = gread;
                            }
                        }
                    }
                }
                in.close();
                if (main.isInstallUpdate())
                    System.out.println();
                return true;
            }
            catch (Exception e)
            {
                fault = true;
                logger.error(Utils.getStackTrace(e));
                message = main.cfg.gs("Z.exception" + Utils.getStackTrace(e));
                if (!main.isInstallUpdate())
                {
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                {
                    System.out.println();
                    System.out.println(message);
                }
            }
            return false;
        }

        private boolean unpackZip(String from, String to)
        {
            // unpack well-defined archive .zip to use same download as users
            try
            {
                // Apache Commons Compress
                // https://commons.apache.org/proper/commons-compress
                Path path = Paths.get(from);
                Path outPath = Paths.get(to);
                long size = Files.size(path);
                if (!main.isInstallUpdate())
                {
                    progressBar.setMaximum((int) size);
                    progressBar.setValue(0);
                }
                else
                    System.out.print(main.cfg.gs("Updater.unpacking"));

                ZipArchiveInputStream in = new ZipArchiveInputStream(new BufferedInputStream(Files.newInputStream(path)));
                ZipArchiveEntry entry = null;
                long zcount = 0L;
                long ticklast = 0L;
                long tickMax = size / 42L;
                while ((entry = in.getNextZipEntry()) != null)
                {
                    zcount = in.getBytesRead();
                    String name = entry.getName();
                    if (name.length() == 0)
                        continue;
                    if (name.startsWith("ELS"))
                    {
                        if (name.equals("ELS/"))
                            continue;
                        name = name.substring(4, name.length());
                    }
                    Path entryPath = outPath.resolve(name);
                    logger.info(SHORT, "  " + entryPath.toString());
                    if (entry.isDirectory())
                    {
                        if (!Files.exists(entryPath))
                            Files.createDirectories(entryPath);
                        Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                    }
                    else
                    {
                        Files.createDirectories(entryPath.getParent());
                        Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                        Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());

                        if (!main.isInstallUpdate())
                            progressBar.setValue((int)zcount);
                        else
                        {
                            if (zcount - ticklast > tickMax)
                            {
                                System.out.print("#");
                                ticklast = zcount;
                            }
                        }
                    }
                }
                in.close();
                if (main.isInstallUpdate())
                    System.out.println();
                return true;
            }
            catch (Exception e)
            {
                fault = true;
                logger.error(Utils.getStackTrace(e));
                message = main.cfg.gs("Z.exception" + Utils.getStackTrace(e));
                if (!main.isInstallUpdate())
                {
                    Object[] opts = {main.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(me, message, main.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                {
                    System.out.println();
                    System.out.println(message);
                }
            }
            return false;
        }
    }

    // ==========================================

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.corionis.els.locales.bundle");
        panelTopSpacer = new JPanel();
        panelBanner = new JPanel();
        labelLogo = new JLabel();
        labelVersion = new JLabel();
        panelControls = new JPanel();
        panelProgress = new JPanel();
        labelStatus = new JLabel();
        progressBar = new JProgressBar();
        vSpacer1 = new JPanel(null);
        buttonCancel = new JButton();

        //======== this ========
        setTitle(bundle.getString("Updater.downloading"));
        setPreferredSize(new Dimension(400, 134));
        setMinimumSize(new Dimension(400, 134));
        setMaximumSize(new Dimension(400, 134));
        setResizable(false);
        setAlwaysOnTop(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                actionWindowClosed(e);
            }
            @Override
            public void windowClosing(WindowEvent e) {
                actionWindowClosed(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== panelTopSpacer ========
        {
            panelTopSpacer.setPreferredSize(new Dimension(100, 8));
            panelTopSpacer.setMinimumSize(new Dimension(100, 8));
            panelTopSpacer.setMaximumSize(new Dimension(100, 8));
            panelTopSpacer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        }
        contentPane.add(panelTopSpacer, BorderLayout.NORTH);

        //======== panelBanner ========
        {
            panelBanner.setMaximumSize(new Dimension(376, 48));
            panelBanner.setMinimumSize(new Dimension(376, 48));
            panelBanner.setPreferredSize(new Dimension(376, 48));
            panelBanner.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

            //---- labelLogo ----
            labelLogo.setIcon(new ImageIcon(getClass().getResource("/els-logo-48px.png")));
            labelLogo.setPreferredSize(new Dimension(48, 48));
            labelLogo.setHorizontalAlignment(SwingConstants.CENTER);
            panelBanner.add(labelLogo);

            //---- labelVersion ----
            labelVersion.setText("Version 4.0.0-development");
            panelBanner.add(labelVersion);
        }
        contentPane.add(panelBanner, BorderLayout.CENTER);

        //======== panelControls ========
        {
            panelControls.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

            //======== panelProgress ========
            {
                panelProgress.setMaximumSize(new Dimension(314, 36));
                panelProgress.setMinimumSize(new Dimension(314, 36));
                panelProgress.setPreferredSize(new Dimension(314, 36));
                panelProgress.setBorder(new EmptyBorder(0, 4, 4, 4));
                panelProgress.setLayout(new BorderLayout());

                //---- labelStatus ----
                labelStatus.setText("Status shown here");
                labelStatus.setHorizontalTextPosition(SwingConstants.CENTER);
                labelStatus.setHorizontalAlignment(SwingConstants.LEFT);
                labelStatus.setPreferredSize(new Dimension(314, 20));
                labelStatus.setMinimumSize(new Dimension(314, 20));
                labelStatus.setMaximumSize(new Dimension(314, 20));
                labelStatus.setVerticalAlignment(SwingConstants.TOP);
                panelProgress.add(labelStatus, BorderLayout.NORTH);

                //---- progressBar ----
                progressBar.setForeground(Color.lightGray);
                progressBar.setPreferredSize(new Dimension(314, 8));
                progressBar.setMinimumSize(new Dimension(314, 8));
                progressBar.setMaximumSize(new Dimension(314, 8));
                progressBar.setMaximum(1000);
                progressBar.setFocusable(false);
                progressBar.setAlignmentY(1.5F);
                panelProgress.add(progressBar, BorderLayout.CENTER);

                //---- vSpacer1 ----
                vSpacer1.setPreferredSize(new Dimension(10, 3));
                vSpacer1.setMinimumSize(new Dimension(10, 3));
                vSpacer1.setMaximumSize(new Dimension(10, 3));
                panelProgress.add(vSpacer1, BorderLayout.SOUTH);
            }
            panelControls.add(panelProgress);

            //---- buttonCancel ----
            buttonCancel.setText(bundle.getString("Z.cancel"));
            buttonCancel.setToolTipText(bundle.getString("Z.cancel.download"));
            buttonCancel.addActionListener(e -> actionCancel(e));
            panelControls.add(buttonCancel);
        }
        contentPane.add(panelControls, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    public JPanel panelTopSpacer;
    public JPanel panelBanner;
    public JLabel labelLogo;
    public JLabel labelVersion;
    public JPanel panelControls;
    public JPanel panelProgress;
    public JLabel labelStatus;
    public JProgressBar progressBar;
    public JPanel vSpacer1;
    public JButton buttonCancel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
