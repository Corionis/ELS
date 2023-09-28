package com.corionis.els_updater;

import com.corionis.els.Configuration;
import com.corionis.els.Utils;
import com.corionis.els.gui.Preferences;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LoggerContext;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;

/**
 * ELS Updater
 *
 * <p>Java main() for processing updates.</p>
 * <p>ELS uses an embedded JRE from the OpenJDK project.<br/>
 * * https://openjdk.org/<br/>
 * * https://github.com/AdoptOpenJDK<br/>
 * * https://wiki.openjdk.org/display/jdk8u/Main<br/>
 */
public class Main
{
    public Configuration cfg = null;
    private String commandLine = "";
    private boolean mainFault = false;
    private String infoFile = "";
    private String installedPath = "";
    public Logger logger = null; // log4j2 logger singleton
    private Main main;
    private Preferences preferences = null;
    private String prefix;
    private Marker SHORT = MarkerManager.getMarker("SHORT");
    private boolean testMode = false;
    private String updaterInfoFile = "";
    private ArrayList<String> version = new ArrayList<>();
    private String versionFile = "";

    private Main(String[] args)
    {
        this.main = this;
        init(args);
        process();
    }

    /**
     * Main application command line constructor
     */
    public static void main(String[] args)
    {
        new Main(args);
    }

    private String getPreferencesPath()
    {
        String path = installedPath + System.getProperty("file.separator") + "local" +
                System.getProperty("file.separator") + "preferences.json";
        return path;
    }

    private void init(String[] args)
    {
        try
        {
            if (args.length > 0 && args[0].equals("--dump-system"))
            {
                Properties p = System.getProperties();
                p.list(System.out);
                return;
            }

            String logFilename = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") +
                    "ELS_Updater" + System.getProperty("file.separator") + "ELS-Updater.log";
            File delLog = new File(logFilename);
            if (delLog.exists())
                delLog.delete();
            System.setProperty("logFilename", logFilename);
            System.setProperty("consoleLevel", "Debug");
            System.setProperty("debugLevel", "Debug");
            System.setProperty("pattern", "\"%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m [%t]:%C.%M:%L%n\"");
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            loggerContext.reconfigure();
            loggerContext.updateLoggers();
            logger = LogManager.getLogger("applog");

            logger.info(SHORT, "+------------------------------------------");
            logger.info(SHORT, "ELS Updater, version " + Configuration.getBuildVersionName() + ", " + Configuration.getBuildDate());
        }
        catch (Exception e)
        {
            mainFault = true;
            System.out.print(Utils.getStackTrace(e));
        }
    }

    /**
     * Is the current system Windows?
     *
     * @return false if not Windows
     */
    private boolean isWindows()
    {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("windows"))
            return true;
        return false;
    }

    private void process()
    {
        try
        {
            javax.swing.SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    cfg = new Configuration(null);
                    boolean fault = false;
                    String message = "";

                    if (!readElsUpdaterInfo())
                    {
                        message = cfg.gs("Updater.missing.or.malformed.updater.info.parameters.cannot.continue") + updaterInfoFile;
                        fault = true;
                    }
                    else
                    {
                        String path = "";
                        try
                        {
                            path = readPreferences();
                            if (path.length() > 0)
                            {
                                preferences.initLookAndFeel();
                                cfg.loadLocale(preferences.getLocale(), cfg);
                            }
                            else
                            {
                                preferences = null;
                                cfg.loadLocale("en_US", cfg);
                            }
                        }
                        catch (Exception e)
                        {
                            // ignore any exception and use default LaF if necessary
                        }

                        logger.info(SHORT, cfg.gs("Updater.preferences") + path);
                        logger.info(SHORT, cfg.gs("Updater.commandline") + commandLine);
                        logger.info(SHORT, cfg.gs("Updater.installedpath") + installedPath);
                    }

                    if (!fault && !readUpdateInfo())
                    {
                        message = cfg.gs("Updater.missing.or.malformed.update.info.cannot.continue") + infoFile;
                        fault = true;
                    }

                    if (!fault && !readVersionInfo())
                    {
                        message = cfg.gs("Updater.missing.or.malformed.version.info.file.cannot.continue") + versionFile;
                        fault = true;
                    }

                    if (fault)
                    {
                        logger.fatal(message);
                        Object[] opts = {cfg.gs("Z.ok")};
                        JOptionPane.showOptionDialog(null, message, cfg.gs("Updater.title"),
                                JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                        System.exit(1);
                    }

                    DownloadUpdate downloadUpdate = new DownloadUpdate(main, preferences, installedPath, version, prefix);

                    if (fault)
                    {
                        // display message & exit
                    }
                }
            });
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    private boolean readElsUpdaterInfo()
    {
        try
        {
            updaterInfoFile = Utils.getSystemTempDirectory() + System.getProperty("file.separator") +
                    "ELS_Updater" + System.getProperty("file.separator") +
                    "ELS_Updater.info";
            BufferedReader br = new BufferedReader(new FileReader(updaterInfoFile));
            installedPath = br.readLine();
            commandLine = br.readLine();
            br.close();

            if (commandLine == null || commandLine.length() == 0 ||
                    installedPath == null || installedPath.length() == 0)
                return false;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            return false;
        }
        return true;
    }

    private String readPreferences()
    {
        Path path = null;
        try
        {
            Gson gson = new Gson();
            preferences = new Preferences();
            path = Paths.get(getPreferencesPath());
            String json = new String(Files.readAllBytes(path));
            preferences = gson.fromJson(json, Preferences.class); // preferences.getClass());
        }
        catch (IOException e)
        {
            logger.error(Utils.getStackTrace(e));
            return "";
        }
        return path == null ? "" : path.toString();
    }

    private boolean readUpdateInfo()
    {
        try
        {
            infoFile = Utils.getSystemTempDirectory() + System.getProperty("file.separator") +
                    "ELS_Updater" + System.getProperty("file.separator") +
                    "bin" + System.getProperty("file.separator") +
                    "update.info";
            File updateInfo = new File(infoFile);
            if (updateInfo.exists())
            {
                prefix = new String(Files.readAllBytes(Paths.get(infoFile)));
                prefix = prefix.trim();
            }
            else
            {
                prefix = cfg.getUrlPrefix(); // use the hardcoded URL
                logger.info(SHORT, java.text.MessageFormat.format(cfg.gs("Updater.update.info.not.found.0.using.coded.url.1"), infoFile, prefix));
            }

            logger.info(SHORT, cfg.gs("Updater.update.url") + prefix);

            if (prefix.length() == 0)
                return false;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            return false;
        }
        return true;
    }

    public boolean readVersionInfo()
    {
        // download (or read in testMode) the latest version.info
        String versionPath;
        BufferedReader bufferedReader = null;
        try
        {
            if (!testMode)
            {
                versionPath = prefix + "/version.info";
                URL url = new URL(versionPath);
                bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            else // assume mock working directory
            {
                versionPath = "bin/version.info";
                bufferedReader = new BufferedReader(new FileReader(versionPath));
            }
            String buf;
            while ((buf = bufferedReader.readLine()) != null)
            {
                version.add(buf.trim());
            }
            bufferedReader.close();

            if (version.size() < Configuration.VERSION_SIZE)
                return false;
        }

        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            return false;
        }
        return true;
    }

    public void stop(boolean fault, boolean requestStop)
    {
        try
        {
            // handle fault and mainFault
            String status;
            if (requestStop)
                status = "";
            else
                status = fault ? " --update-failed" : " --update-successful";
            String[] args = Utils.parseCommandLIne(commandLine + status);
            Process proc = Runtime.getRuntime().exec(args, null, new File(installedPath));
            Thread.sleep(5000);
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }

        System.exit(fault ? 1 : 0);
    }

    private Set<PosixFilePermission> translateModeToPosix(int mode)
    {
        Set<PosixFilePermission> perms = new HashSet<>();
        if ((mode & 0001) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        if ((mode & 0002) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((mode & 0004) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 0010) > 0)
        {
            perms.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((mode & 0020) > 0)
        {
            perms.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((mode & 0040) > 0)
        {
            perms.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 0100) > 0)
        {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((mode & 0200) > 0)
        {
            perms.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((mode & 0400) > 0)
        {
            perms.add(PosixFilePermission.OWNER_READ);
        }
        return perms;
    }

}
