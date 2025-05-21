package com.corionis.els_updater;

// See els.xml target "updater-compile" where these classes are copied during builds

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
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.corionis.els.Configuration.APPLICATION_NAME;

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
    public Logger logger = null; // log4j2 logger singleton
    private Marker SHORT = MarkerManager.getMarker("SHORT");
    private String commandLine = "";
    private String configPath = "";
    private String infoFile = "";
    private boolean installUpdates = false;
    private String installedPath = "";
    private Main main;
    private boolean mainFault = false;
    public boolean mockMode = false; // local mock without downloading version.info, get from bin/version.info; see els/Main
    private boolean pathFault = false;
    private Preferences preferences = null;
    private String prefix;
    private String updaterInfoFile = "";
    private String updaterPath = "";
    private ArrayList<String> version = new ArrayList<>();
    private String versionFile = "";

    //
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

    public boolean execExternalExe(Component comp, Configuration cfg, String[] parms)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");

        String cl = "";
        for (int i = 0; i < parms.length; ++i)
        {
            cl = cl + parms[i] + " ";
        }
        final String cmdline = cl;

        final int MAX_TRIES = 3;
        int tries = 0;
        while (tries < MAX_TRIES)
        {
            try
            {
                final java.lang.Process proc = Runtime.getRuntime().exec(parms);
                Thread thread = new Thread()
                {
                    public void run()
                    {
                        String line;
                        BufferedReader input = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                        try
                        {
                            while ((line = input.readLine()) != null)
                                logger.info(SIMPLE, line);
                            input.close();
                        }
                        catch (IOException e)
                        {
                            logger.error(cfg.gs("Z.exception") + cmdline + System.getProperty("line.separator") + Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(comp, cfg.gs("Z.exception") + Utils.getStackTrace(e), cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                };

                // run it
                thread.start();
                int result = proc.waitFor();
                thread.join();
                if (result != 0)
                {
                    ++tries;
                    String message = cfg.gs("Z.process.failed") + result + ", : " + cmdline;
                    logger.error(message);
                    JOptionPane.showMessageDialog(comp, message, cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                else
                    return true;
            }
            catch (Exception e)
            {
                ++tries;
                logger.warn(cfg.gs("Z.exception") + parms[0] + ", #" + tries + ", " + e.getMessage());
                // give the OS a little more time
                try
                {
                    Thread.sleep(2000);
                }
                catch (Exception e1)
                {
                }
            }

            if (tries >= MAX_TRIES)
            {
                String message = cfg.gs("Z.process.failed") + cmdline;
                logger.error(message);
                JOptionPane.showMessageDialog(comp, message, cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    public String getUpdaterPath()
    {
        if (updaterPath.length() == 0)
        {
            updaterPath = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "ELS_Updater_" + System.getProperty("user.name");
            File up = new File(updaterPath);
            if (!up.exists())
            {
                String old = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "ELS_Updater";
                up = new File(old);
                if (!up.exists())
                {
                    pathFault = true;
                }
                updaterPath = old;
            }
        }
        return updaterPath;
    }

    private void init(String[] args)
    {
        try
        {
            if (args.length > 0)
            {
                if (args[0].equals("--dump-system"))
                {
                    Properties p = System.getProperties();
                    p.list(System.out);
                    System.exit(1);
                }
                if (args[0].equals("-Y"))
                    setInstallUpdates(true);
            }

            System.setProperty("jdk.lang.Process.launchMechanism", "POSIX_SPAWN");

            // must be set before any AWT classes are loaded
            if (System.getProperty("os.name").toLowerCase().startsWith("mac"))
            {
                System.setProperty("apple.laf.useScreenMenuBar", "true");
                System.setProperty("apple.awt.application.name", APPLICATION_NAME);
                System.setProperty("apple.awt.application.appearance", "system");
            }

            String logFilename = getUpdaterPath() + System.getProperty("file.separator") + "ELS-Updater.log";
            File delLog = new File(logFilename);
            if (delLog.exists())
                delLog.delete();
            System.setProperty("logFilename", logFilename);
            System.setProperty("consoleLevel", (isInstallUpdate() ? "OFF" : "DEBUG"));
            System.setProperty("debugLevel", "DEBUG");
            System.setProperty("pattern", "\"%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m [%t]:%C.%M:%L%n\"");
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            loggerContext.reconfigure();
            loggerContext.updateLoggers();
            logger = LogManager.getLogger("applog");

            logger.info(SHORT, "+------------------------------------------");
            logger.info(SHORT, "ELS Updater, version " + Configuration.getBuildVersionName() + ", " + Configuration.getBuildDate());
            if (isInstallUpdate())
            {
                System.out.println();
                System.out.println("ELS Updater, version " + Configuration.getBuildVersionName() + ", " + Configuration.getBuildDate());
            }
        }
        catch (Exception e)
        {
            mainFault = true;
            System.out.print(Utils.getStackTrace(e));
        }
    }

    public boolean isInstallUpdate()
    {
        return installUpdates;
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
                        message = java.text.MessageFormat.format(cfg.gs("Updater.missing.or.malformed.els.updater.info.file.cannot.continue"), updaterInfoFile);
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
                                if (!isInstallUpdate())
                                    preferences.initLookAndFeel("ELS Updater", true);
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
                            logger.error(Utils.getStackTrace(e));
                            // ignore any exception and use default LaF if necessary
                        }

                        logger.info(SHORT, cfg.gs("Updater.preferences") + path);
                        logger.info(SHORT, cfg.gs("Updater.commandline") + commandLine);
                        logger.info(SHORT, cfg.gs("Updater.installed.path") + installedPath);
                        if (isInstallUpdate())
                            System.out.println("  " + cfg.gs("Updater.installed.path") + installedPath);
                    }

                    if (!fault && !readUpdateInfo())
                    {
                        message = java.text.MessageFormat.format(cfg.gs("Updater.missing.or.malformed.els.updater.info.file.cannot.continue"), updaterInfoFile);
                        fault = true;
                    }

                    if (!fault && !readVersionInfo())
                    {
                        message = java.text.MessageFormat.format(cfg.gs("Navigator.version.info.missing.or.malformed"), versionFile);
                        fault = true;
                    }

                    if (fault)
                    {
                        logger.fatal(message);
                        if (!isInstallUpdate())
                        {
                            Object[] opts = {"Ok"};
                            JOptionPane.showOptionDialog(null, message, "ELS Updater",
                                    JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                        }
                        else
                            System.out.println(message);

                        System.exit(1); // <<<<<<<-------------- Exit
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

    /**
     * Read parameters for Updater
     *
     * @return
     */
    private boolean readElsUpdaterInfo()
    {
        try
        {
            updaterInfoFile = getUpdaterPath() + System.getProperty("file.separator") + "ELS_Updater.info";
            BufferedReader br = new BufferedReader(new FileReader(updaterInfoFile));
            installedPath = br.readLine();
            configPath = br.readLine();
            commandLine = br.readLine();
            br.close();

            if (commandLine == null || commandLine.length() == 0) // upgrade from a 2-line file
            {
                commandLine = configPath;
                Pattern p = Pattern.compile(" -C \\\"([^\\\"]*)\\\"");
                Matcher m = p.matcher(commandLine);
                if (m.find())
                {
                    configPath = m.group(1);
                }
            }

            if (commandLine == null || commandLine.length() == 0 ||
                    configPath == null || configPath.length() == 0 ||
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

    /**
     * Read user Preferences from -C configuration directory
     *
     * @return
     */
    private String readPreferences()
    {
        Path path = null;
        try
        {
            Gson gson = new Gson();
            preferences = new Preferences();
            path = Paths.get(preferences.getFullPath(configPath));
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

    /**
     * Read update.info with URL to version.info
     *
     * @return
     */
    private boolean readUpdateInfo()
    {
        try
        {
            infoFile = getUpdaterPath() + System.getProperty("file.separator") + "bin" + System.getProperty("file.separator") + "update.info";
            File updateInfo = new File(infoFile);
            if (updateInfo.exists())
            {
                prefix = new String(Files.readAllBytes(Paths.get(infoFile)));
                prefix = prefix.trim();
            }
            else
            {
                prefix = cfg.getUrlPrefix(); // use the hardcoded URL
                logger.info(SHORT, java.text.MessageFormat.format(cfg.gs("Updater.update.info.not.found.using.coded.url"), infoFile, prefix));
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

    /**
     * Read version.info with build dates and update filenames
     *
     * @return
     */
    public boolean readVersionInfo()
    {
        // download (or read in testMode) the latest version.info
        String versionPath;
        BufferedReader bufferedReader = null;
        try
        {
            if (!mockMode)
            {
                versionPath = prefix + "/version.info";
                URL url = new URL(versionPath);
                bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            }
            else // assume mock working directory
            {
                versionPath = configPath + "/bin/version.info";
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

    public void setInstallUpdates(boolean installUpdates)
    {
        this.installUpdates = installUpdates;
    }

    public void stop(boolean fault, boolean requestStop)
    {
        int code = fault ? 1 : 0;
        String status;
        if (!isInstallUpdate())
        {
            try
            {
                // handle fault and mainFault
                if (requestStop)
                    status = "";
                else
                    status = fault ? " --update-failed" : " --update-successful";
                commandLine = commandLine + status;
                String[] args = Utils.parseCommandLIne(commandLine);
                logger.info(cfg.gs("Updater.restarting.els") + commandLine);
                Process proc = Runtime.getRuntime().exec(args, null, new File(installedPath));
                Thread.sleep(1000);
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
        }
        else
        {
            status = fault ? cfg.gs("Navigator.download.unpack.failed") : cfg.gs("Navigator.updated");
            logger.info(SHORT, status);
            System.out.println(status);
        }
        System.exit(code);
    }

}
