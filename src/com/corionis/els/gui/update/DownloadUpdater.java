package com.corionis.els.gui.update;

import java.awt.event.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

import com.corionis.els.Configuration;
import com.corionis.els.Context;
import com.corionis.els.gui.Navigator;
import com.corionis.els.Utils;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DownloadUpdater extends JFrame
{
    private Context context;
    private boolean fault = false;
    private String installedPath;
    private boolean launched = false;
    private transient Logger logger = LogManager.getLogger("applog");
    private String message;
    private Navigator navigator;
    private boolean mockMode;
    private String outPath;
    private String prefix;
    private boolean requestStop = false;
    private String updateArchive;
    private String updateFile;
    private String updaterJar;
    private URL url;
    private ArrayList<String> version = new ArrayList<>();
    private Worker worker;

    public DownloadUpdater(Context context, Navigator navigator, ArrayList<String> version, String prefix)
    {
        super();
        this.context = context;
        this.mockMode = context.main.mockMode;
        this.navigator = navigator;
        this.version = version;
        this.prefix = prefix;
        if (navigator != null)
            initComponents();

        process();
        logger.info("Download done");
    }

    private void actionCancel(ActionEvent e)
    {
        requestStop = true;
    }

    private void actionWindowClosed(WindowEvent e)
    {
        actionCancel(null);
    }

    private void process()
    {
        if (navigator != null)
        {
            setIconImage(new ImageIcon(getClass().getResource("/els-logo-48px.png")).getImage());
            int x = context.mainFrame.getX() + (context.mainFrame.getWidth() / 2) - (getWidth() / 2);
            int y = context.mainFrame.getY() + (context.mainFrame.getHeight() / 2) - (getHeight() / 2);
            setLocation(x, y);
            setVisible(true);
            buttonCancel.setSelected(false);
        }

        worker = new Worker();
        worker.execute();

        if (navigator == null) // wait for worker thread to finish
        {
            try
            {
                while (!worker.isDone() || !launched)
                {
                    Thread.sleep(100);
                }
            }
            catch (Exception e)
            {
                logger.info("THREAD INTERRUPTED");
            }

            System.exit(0);
        }
    }

    // ==========================================

    class Worker extends SwingWorker<Void, Void>
    {
        @Override
        protected Void doInBackground() throws Exception
        {
            if (navigator != null)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                progressBar.setMinimum(0);
            }

            installedPath = context.cfg.getInstalledPath();
            outPath = Utils.getTempUpdaterDirectory();
            File out = new File(outPath);
            if (out.exists())
                FileUtils.deleteDirectory(out);
            out.mkdirs();

            updaterJar = outPath + System.getProperty("file.separator") + "bin" +
                    System.getProperty("file.separator") + "ELS_Updater.jar";

            if (!requestStop && download())
            {
                // give the download a chance to flush buffers and close the file
                Thread.sleep(2000);

                if (!requestStop && unpack())
                {
                    if (!requestStop)
                    {
                        writeUpdaterInfo();
                        message = context.cfg.gs("Navigator.download.and.unpack.of.els.updater.successful");
                        logger.info(message);
                        if (navigator != null)
                            context.mainFrame.labelStatusMiddle.setText(message);
                        else
                            System.out.println(message);
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
                message = context.cfg.gs("Navigator.download.unpack.failed");
                logger.info(message);
                if (navigator != null)
                    context.mainFrame.labelStatusMiddle.setText(message);
                else
                    System.out.println(message);
            }
            return null;
        }

        @Override
        public void done()
        {
            if (navigator != null)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                setVisible(false);
            }

            if (requestStop)
            {
                logger.warn(context.cfg.gs("Navigator.els.update.process.cancelled"));
                if (navigator != null)
                   context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.els.update.process.cancelled"));
            }
            else
            {
                if (!fault)
                {
                    File els = new File(updaterJar);
                    if (!els.exists())
                    {
                        message = context.cfg.gs("Navigator.cannot.find.executable") + outPath;
                        if (navigator != null)
                        {
                            Object[] opts = {context.cfg.gs("Z.ok")};
                            JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                                    JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                        }
                        else
                            System.out.println(message);
                        return;
                    }

                    if (navigator != null)
                    {
                        navigator.setUpdaterProcess(updaterJar);
                        navigator.stop();
                    }
                    else // command line -Y
                    {
                        context.main.execUpdater(updaterJar, false);
                        launched = true;
                    }
                }
            }
        }

        private boolean download()
        {
            try
            {
                if (navigator != null)
                    labelVersion.setText(version.get(Configuration.BUILD_VERSION_NAME));

                String ext = Utils.isOsWindows() ? ".zip" : ".tar.gz";
                updateFile = version.get(Configuration.BUILD_UPDATER_DISTRO) + ext;
                updateArchive = Utils.getTempUpdaterDirectory() + System.getProperty("file.separator") + updateFile;

                if (navigator != null)
                    labelStatus.setText(context.cfg.gs("Z.update") + version.get(Configuration.BUILD_DATE));

                if (!mockMode)
                {
                    // download the ELS Updater
                    String downloadUrl = prefix + "/" + updateFile;
                    url = new URL(downloadUrl);
                    URLConnection connection = url.openConnection();

                    int contentLength = connection.getContentLength();
                    if (navigator != null)
                    {
                        progressBar.setMaximum(contentLength);
                        progressBar.setValue(0);
                    }
                    else
                        System.out.print(context.cfg.gs("Navigator.install.downloading"));

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

                        if (navigator != null)
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
                    if (navigator == null)
                        System.out.println();

                    if (!requestStop)
                    {
                        try
                        {
                            FileOutputStream out = new FileOutputStream(updateArchive);
                            out.write(data);
                            out.flush();
                            out.close();
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            message = context.cfg.gs("Z.error.writing") + updateArchive;
                            Object[] opts = {context.cfg.gs("Z.ok")};
                            if (navigator != null)
                            {
                                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                            }
                            else
                                System.out.println(message);
                            return false;
                        }
                    }

                    if (requestStop)
                    {
                        // remove partial download
                        File dl = new File(updateArchive);
                        if (dl.exists())
                            dl.delete();
                        return false;
                    }
                }
                else
                {
                    // in mockMode copy ELS Updater from the build directory
                    File dl = new File(updateArchive);
                    if (!dl.exists())
                    {
                        String copy = ".." + System.getProperty("file.separator") + "build" + System.getProperty("file.separator") + updateFile;
                        File cp = new File (copy);
                        if (cp.exists())
                        {
                            if (context.cfg.isInstallUpdate())
                                System.out.println(context.cfg.gs("Updater.copying") + copy);
                            Files.copy(Paths.get(copy), Paths.get(updateArchive));
                        }
                        else
                        {
                            message = java.text.MessageFormat.format(context.cfg.gs("Navigator.update.not.found"), updateArchive);
                            if (navigator == null)
                                System.out.println(message);
                            return false;
                        }
                    }
                }
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                message = context.cfg.gs("Z.error.downloading") + updateArchive;
                if (navigator != null)
                {
                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                }
                else
                    System.out.println(message);
                return false;
            }
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
            catch (IOException e)
            {
                logger.error(Utils.getStackTrace(e));
                message = context.cfg.gs("Z.exception" + e.getMessage());
                Object[] opts = {context.cfg.gs("Z.ok")};
                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                return false;
            }
            return true;
        }

        private boolean unpack()
        {
            boolean success = false;
            if (navigator != null)
            {
                setTitle(context.cfg.gs("Navigator.unpacking"));
                buttonCancel.setEnabled(false);
            }

            if (Utils.isOsWindows())
                success = unpackZip(updateArchive, outPath); // Windows
            else
                success = unpackTar(updateArchive, outPath); // Linux

            if (navigator != null)
                buttonCancel.setEnabled(true);
            return success;
        }

         private boolean unpackTar(String from, String to)
        {
            // unpack well-defined archive tar.gz to use same download as users
            try
            {
                // Apache Commons Compress
                // https://commons.apache.org/proper/commons-compress
                Path path = Paths.get(from);
                Path outPath = Paths.get(to);
                long size = Files.size(path);
                if (navigator != null)
                {
                    progressBar.setMaximum((int) size);
                    progressBar.setValue(0);
                }
                else
                    System.out.print(context.cfg.gs("Navigator.install.unpacking"));

                GzipCompressorInputStream gin = new GzipCompressorInputStream(new BufferedInputStream(Files.newInputStream(path)));
                TarArchiveInputStream in = new TarArchiveInputStream(gin);

                TarArchiveEntry entry = null;
                long ticklast = 0L;
                long tickMax = size / 42L;
                while ((entry = in.getNextTarEntry()) != null)
                {
                    long gread = gin.getBytesRead();
                    long bread = in.getBytesRead();
                    if (gread != bread)
                    {
                        message = "Bytes read: " + gread + " and bytes decompressed: " + bread + " do not match, archive corrupted";
                        logger.fatal(message);
                        System.out.println(message);
                        System.exit(1);
                    }

                    String name = entry.getName();
                    if (name.length() == 0)
                        continue;
                    if (name.startsWith("ELS_Updater"))
                    {
                        if (name.equals("ELS_Updater/"))
                            continue;
                        name = name.substring(12, name.length());
                    }

                    Path entryPath = outPath.resolve(name);
                    logger.info("  " + entryPath.toString());
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

                        if (navigator != null)
                            progressBar.setValue((int)gread);
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
                if (navigator == null)
                    System.out.println();
                return true;
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
            return false;
        }

        private boolean unpackZip(String from, String to)
        {
            // unpack well-defined archive zip to use same download as users
            try
            {
                // Apache Commons Compress
                // https://commons.apache.org/proper/commons-compress
                Path path = Paths.get(from);
                Path outPath = Paths.get(to);
                long size = Files.size(path);
                if (navigator != null)
                {
                    progressBar.setMaximum((int) size);
                    progressBar.setValue(0);
                }
                else
                    System.out.print(context.cfg.gs("Navigator.install.unpacking"));

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
                    if (name.startsWith("ELS_Updater"))
                    {
                        if (name.equals("ELS_Updater/"))
                            continue;
                        name = name.substring(12, name.length());
                    }
                    Path entryPath = outPath.resolve(name);
                    logger.info("  " + entryPath.toString());
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

                        if (navigator != null)
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
                if (navigator == null)
                    System.out.println();
                return true;
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
            return false;
        }

        /**
         * Write
         *
         * @return
         */
        private boolean writeUpdaterInfo()
        {
            try
            {
                String infoPath = Utils.getTempUpdaterDirectory() + System.getProperty("file.separator") + "ELS_Updater.info";

                // use current values
                String consoleLevel = context.cfg.getConsoleLevel();
                String debugLevel = context.cfg.getDebugLevel();
                boolean overwriteLog = context.cfg.isLogOverwrite();
                String log = context.cfg.getLogFileFullPath();
                String restartCommand = context.cfg.generateCurrentCommandline(consoleLevel, debugLevel, overwriteLog, log);

                FileWriter writer = new FileWriter(infoPath, false);
                writer.write(installedPath + System.getProperty("line.separator"));
                writer.write(context.cfg.getWorkingDirectory() + System.getProperty("line.separator"));
                writer.write(restartCommand + System.getProperty("line.separator"));
                writer.close();
            }
            catch (Exception e)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                logger.error(Utils.getStackTrace(e));
                message = context.cfg.gs("Z.error.writing") + updateArchive;
                Object[] opts = {context.cfg.gs("Z.ok")};
                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                return false;
            }
            return true;
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
        setTitle(bundle.getString("Z.downloading"));
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
