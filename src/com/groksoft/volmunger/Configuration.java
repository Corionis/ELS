package com.groksoft.volmunger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

// see https://logging.apache.org/log4j/2.x/

/**
 * Configuration
 * <p>
 * Contains all command-line options and any other application-level configuration.
 */
public class Configuration
{
    public static final int NOT_REMOTE = 0;
    public static final int REMOTE_PUBLISH = 1;
    public static final int SUBSCRIBER_LISTENER = 2;
    public static final int PUBLISHER_MANUAL = 3;
    public static final int PUBLISHER_LISTENER = 4;
    public static final int SUBSCRIBER_TERMINAL = 5;
    private final String VOLMUNGER_VERSION = "2.0.0";

    // debug
    private String consoleLevel = "debug";  // Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF
    private String debugLevel = "info";

    // files
    private String exportCollectionFilename = "";
    private String exportTextFilename = "";
    private String mismatchFilename = "";
    private String publisherCollectionFilename = "";
    private String subscriberCollectionFilename = "";
    private String targetsFilename = "";
    private String whatsNewFilename = "";

    // publisher & subscriber
    private String publisherLibrariesFileName = "";
    private ArrayList<String> publisherLibraryName = new ArrayList<>();
    private boolean specificPublisherLibrary = false;
    private String subscriberLibrariesFileName = "";

    // other
    private boolean dryRun = false;
    private boolean keepVolMungerFiles = false;
    private String logFilename = "volmunger.log";
    private String[] originalArgs;

    private boolean publishOperation = true;
    private boolean validationRun = false;

    // remote
    private String authorizedPassword = "";
    private int remoteFlag = NOT_REMOTE;
    private String remoteType = "-";
    private boolean requestCollection = false;
    private boolean requestTargets = false;

    /**
     * Instantiates a new Configuration.
     */
    public Configuration()
    {
    }

    /**
     * Parse command line.
     * <p>
     * This populates the rest.
     *
     * @param args the args
     * @return the boolean
     * @throws MungerException the volmunger exception
     */
    public void parseCommandLine(String[] args) throws MungerException
    {
        int index;
        boolean success = true;
        originalArgs = args;

        for (index = 0; index < args.length; ++index)
        {
            switch (args[index])
            {
                case "-a":                                             // authorize mode password
                    if (index <= args.length - 2)
                    {
                        setAuthorizedPassword(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -a requires a password value");
                    }
                    break;
                case "-c":                                             // console level
                    if (index <= args.length - 2)
                    {
                        setConsoleLevel(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -c requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "-D":                                             // Dry run
                    setDryRun(true);
                    break;
                case "-d":                                             // debug level
                    if (index <= args.length - 2)
                    {
                        setDebugLevel(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -d requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "-e":                                             // export publisher items to flat text file
                    if (index <= args.length - 2)
                    {
                        setExportTextFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungerException("Error: -e requires an export path output filename");
                    }
                    break;
                case "-f":                                             // log filename
                    if (index <= args.length - 2)
                    {
                        setLogFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -f requires a log filename");
                    }
                    break;
                case "-i":                                             // export publisher items to collection file
                    if (index <= args.length - 2)
                    {
                        setExportCollectionFilename(args[index + 1]);
                        ++index;
                        setPublishOperation(false);
                    }
                    else
                    {
                        throw new MungerException("Error: -i requires a collection output filename");
                    }
                    break;
                case "-k":                                             // keep .volmunger files
                    setKeepVolMungerFiles(true);
                    break;
                case "-l":                                             // publisher library to process
                    if (index <= args.length - 2)
                    {
                        addPublisherLibraryName(args[index + 1]);
                        setSpecificPublisherLibrary(true);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -l requires a publisher library name");
                    }
                    break;
                case "-m":                                             // Mismatch output filename
                    if (index <= args.length - 2)
                    {
                        setMismatchFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -m requires a mismatches output filename");
                    }
                    break;
                case "-n":                                             // What's New output filename
                    if (index <= args.length - 2)
                    {
                        setWhatsNewFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -n requires a What's New output filename");
                    }
                    break;
                case "-p":                                             // publisher JSON libraries file
                    if (index <= args.length - 2)
                    {
                        setPublisherLibrariesFileName(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -p requires a publisher libraries filename");
                    }
                    break;
                case "-P":                                             // publisher JSON collection items file
                    if (index <= args.length - 2)
                    {
                        setPublisherCollectionFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -P requires an publisher collection filename");
                    }
                    break;
                case "-r":                                             // remote session
                    if (index <= args.length - 2)
                    {
                        setRemoteType(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -r must be followed by |P|L|M|S|T, case-insensitive");
                    }
                    break;
                case "-s":                                             // subscriber JSON libraries file
                    if (index <= args.length - 2)
                    {
                        setRequestCollection(true);
                        setSubscriberLibrariesFileName(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -s requires a subscriber libraries filename");
                    }
                    break;
                case "-S":                                             // subscriber JSON collection items file
                    if (index <= args.length - 2)
                    {
                        setSubscriberCollectionFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -S requires an subscriber collection filename");
                    }
                    break;
                case "-t":                                             // targets filename
                    if (index <= args.length - 2)
                    {
                        setRequestTargets(true);
                        setTargetsFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -t requires a targets filename");
                    }
                    break;
                case "-T":                                             // targets filename
                    if (index <= args.length - 2)
                    {
                        setRequestTargets(false);
                        setTargetsFilename(args[index + 1]);
                        ++index;
                    }
                    else
                    {
                        throw new MungerException("Error: -T requires a targets filename");
                    }
                    break;
                case "-v":                                             // validate collections files
                    setValidationRun(true);
                    break;
                default:
                    throw new MungerException("Error: unknown option " + args[index]);
            }
        }
    }

    /**
     * Add a publisher library name.
     *
     * @param publisherLibraryName the publisher library name
     */
    public void addPublisherLibraryName(String publisherLibraryName)
    {
        this.publisherLibraryName.add(publisherLibraryName);
    }

    /**
     * Dump the configuration
     */
    public void dump()
    {
        Logger logger = LogManager.getLogger("applog");

        String msg = "Arguments: ";
        for (int index = 0; index < originalArgs.length; ++index)
        {
            msg = msg + originalArgs[index] + " ";
        }
        logger.info(msg);

        logger.info("  cfg: -c Daemon logging level = " + getConsoleLevel());
        logger.info("  cfg: -d Debug logging level = " + getDebugLevel());
        logger.info("  cfg: -D Dry run = " + Boolean.toString(isDryRun()));
        logger.info("  cfg: -e Export paths filename = " + getExportTextFilename());
        logger.info("  cfg: -f Log filename = " + getLogFilename());
        logger.info("  cfg: -i Export JSON filename = " + getExportCollectionFilename());
        logger.info("  cfg: -k Keep .volmunger files = " + Boolean.toString(isKeepVolMungerFiles()));
        logger.info("  cfg: -l Publisher library name(s):");
        for (String ln : getPublisherLibraryNames())
        {
            logger.info("    cfg:     " + ln);
        }
        logger.info("  cfg: -m Mismatches output filename = " + getMismatchFilename());
        logger.info("  cfg: -n What's New output filename = " + getWhatsNewFilename());
        logger.info("  cfg: -p Publisher Library filename = " + getPublisherLibrariesFileName());
        logger.info("  cfg: -P Publisher Collection import filename = " + getPublisherCollectionFilename());
        logger.info("  cfg: -r ClientStty session = " + getRemoteType());
        logger.info("  cfg: -s Daemon Library filename = " + getSubscriberLibrariesFileName());
        logger.info("  cfg: -S Daemon Collection import filename = " + getSubscriberCollectionFilename());
        logger.info("  cfg: -t Targets filename = " + getTargetsFilename());
        logger.info("  cfg: -v Validation run = " + Boolean.toString(isValidationRun()));
    }

    /**
     * Gets Authorized password.
     *
     * @return the password required to access Authorized mode when using a ClientStty
     */
    public String getAuthorizedPassword()
    {
        return authorizedPassword;
    }

    /**
     * Sets Authorized password.
     *
     * @param password the password required to access Authorized mode with a ClientStty
     */
    public void setAuthorizedPassword(String password)
    {
        this.authorizedPassword = password;
    }

    /**
     * Gets console level.
     *
     * @return the console level
     */
    public String getConsoleLevel()
    {
        return consoleLevel;
    }

    /**
     * Sets console level.
     *
     * @param consoleLevel the console level
     */
    public void setConsoleLevel(String consoleLevel)
    {
        this.consoleLevel = consoleLevel;
    }

    /**
     * Gets debug level.
     *
     * @return the debug level
     */
    public String getDebugLevel()
    {
        return debugLevel;
    }

    /**
     * Sets debug level.
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel)
    {
        this.debugLevel = debugLevel;
    }

    /**
     * Gets retrieveRemoteCollectionExport filename.
     *
     * @return the retrieveRemoteCollectionExport filename
     */
    public String getExportCollectionFilename()
    {
        return exportCollectionFilename;
    }

    /**
     * Sets retrieveRemoteCollectionExport filename.
     *
     * @param exportCollectionFilename the retrieveRemoteCollectionExport filename
     */
    public void setExportCollectionFilename(String exportCollectionFilename)
    {
        this.exportCollectionFilename = exportCollectionFilename;
    }

    /**
     * @return exportTextFilename
     */
    public String getExportTextFilename()
    {
        return exportTextFilename;
    }

    /**
     * @param exportTextFilename
     */
    public void setExportTextFilename(String exportTextFilename)
    {
        this.exportTextFilename = exportTextFilename;
    }

    /**
     * Gets log filename.
     *
     * @return the log filename
     */
    public String getLogFilename()
    {
        return logFilename;
    }

    /**
     * Sets log filename.
     *
     * @param logFilename the log filename
     */
    public void setLogFilename(String logFilename)
    {
        this.logFilename = logFilename;
    }

    /**
     * Gets mismatch filename.
     *
     * @return the mismatch filename
     */
    public String getMismatchFilename()
    {
        return mismatchFilename;
    }

    /**
     * Sets mismatch filename.
     *
     * @param mismatchFilename the mismatch filename
     */
    public void setMismatchFilename(String mismatchFilename)
    {
        this.mismatchFilename = mismatchFilename;
    }

    /**
     * Gets PatternLayout for log4j2.
     * <p>
     * Call this method AFTER setDebugLevel() has been called.
     *
     * @return the PatternLayout to use
     */
    public String getPattern()
    {
        String withMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m [%t]:%C.%M:%L%n";
        String withoutMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m%n";
        if (getDebugLevel().trim().equalsIgnoreCase("info"))
        {
            return withoutMethod;
        }
        return withMethod;
    }

    /**
     * Gets publisher import filename.
     *
     * @return the publisher import filename
     */
    public String getPublisherCollectionFilename()
    {
        return publisherCollectionFilename;
    }

    /**
     * Sets publisher collection filename.
     *
     * @param publisherCollectionFilename the publisher import filename
     */
    public void setPublisherCollectionFilename(String publisherCollectionFilename)
    {
        this.publisherCollectionFilename = publisherCollectionFilename;
    }

    /**
     * Gets publisher configuration file name.
     *
     * @return the publisher configuration file name
     */
    public String getPublisherLibrariesFileName()
    {
        return publisherLibrariesFileName;
    }

    /**
     * Sets publisher libraries file name.
     *
     * @param publisherLibrariesFileName the publisher configuration file name
     */
    public void setPublisherLibrariesFileName(String publisherLibrariesFileName)
    {
        this.publisherLibrariesFileName = publisherLibrariesFileName;
    }

    /**
     * Gets publisher library name.
     *
     * @return the publisher library name
     */
    public ArrayList<String> getPublisherLibraryNames()
    {
        return publisherLibraryName;
    }

    /**
     * Gets remote flag.
     *
     * @return the remote flag, 0 = none, 1 = publisher, 2 = subscriber, 3 = pub terminal, 4 = pub listener, 5 = sub terminal
     */
    public int getRemoteFlag()
    {
        return this.remoteFlag;
    }

    /**
     * Gets remote type.
     *
     * @return the remote type from the command line
     */
    public String getRemoteType()
    {
        return this.remoteType;
    }

    /**
     * Sets remote type.
     *
     * @param type the remote type and remote flag
     */
    public void setRemoteType(String type) throws MungerException
    {
        if (!this.remoteType.equals("-"))
        {
            throw new MungerException("The -r option may only be used once");
        }
        this.remoteType = type;
        this.remoteFlag = NOT_REMOTE;
        if (type.equalsIgnoreCase("P"))
            this.remoteFlag = REMOTE_PUBLISH;
        else if (type.equalsIgnoreCase("S"))
            this.remoteFlag = SUBSCRIBER_LISTENER;
        else if (type.equalsIgnoreCase("M"))
            this.remoteFlag = PUBLISHER_MANUAL;
        else if (type.equalsIgnoreCase("L"))
            this.remoteFlag = PUBLISHER_LISTENER;
        else if (type.equalsIgnoreCase("T"))
            this.remoteFlag = SUBSCRIBER_TERMINAL;
        else
            throw new MungerException("Error: -r must be followed by B|L|P|S|T, case-insensitive");
    }

    /**
     * Gets subscriber import filename.
     *
     * @return the import filename
     */
    public String getSubscriberCollectionFilename()
    {
        return subscriberCollectionFilename;
    }

    /**
     * Sets subscriber collection filename.
     *
     * @param subscriberCollectionFilename the import filename
     */
    public void setSubscriberCollectionFilename(String subscriberCollectionFilename)
    {
        this.subscriberCollectionFilename = subscriberCollectionFilename;
    }

    /**
     * Gets subscriber configuration file name.
     *
     * @return the subscriber configuration file name
     */
    public String getSubscriberLibrariesFileName()
    {
        return subscriberLibrariesFileName;
    }

    /**
     * Sets subscriber libraries file name.
     *
     * @param subscriberLibrariesFileName the subscriber configuration file name
     */
    public void setSubscriberLibrariesFileName(String subscriberLibrariesFileName)
    {
        this.subscriberLibrariesFileName = subscriberLibrariesFileName;
    }

    /**
     * Gets targets filename.
     *
     * @return the targets filename
     */
    public String getTargetsFilename()
    {
        return targetsFilename;
    }

    /**
     * Sets targets filename.
     *
     * @param targetsFilename the targets filename
     */
    public void setTargetsFilename(String targetsFilename)
    {
        this.targetsFilename = targetsFilename;
    }

    /**
     * Gets Main version.
     *
     * @return the Main version
     */
    public String getVOLMUNGER_VERSION()
    {
        return VOLMUNGER_VERSION;
    }

    /**
     * Gets whats new filename.
     *
     * @return the whats new filename
     */
    public String getWhatsNewFilename()
    {
        return whatsNewFilename;
    }

    /**
     * Sets whats new filename.
     *
     * @param whatsNewFilename the whats new filename
     */
    public void setWhatsNewFilename(String whatsNewFilename)
    {
        this.whatsNewFilename = whatsNewFilename;
    }

    /**
     * Is dry run boolean.
     *
     * @return the boolean
     */
    public boolean isDryRun()
    {
        return dryRun;
    }

    /**
     * Sets dry run.
     *
     * @param dryRun true/false boolean
     */
    public void setDryRun(boolean dryRun)
    {
        this.dryRun = dryRun;
    }

    /**
     * Is keep vol volmunger files boolean.
     *
     * @return the boolean
     */
    public boolean isKeepVolMungerFiles()
    {
        return keepVolMungerFiles;
    }

    /**
     * Sets keep vol volmunger files.
     *
     * @param keepVolMungerFiles the keep vol volmunger files
     */
    public void setKeepVolMungerFiles(boolean keepVolMungerFiles)
    {
        this.keepVolMungerFiles = keepVolMungerFiles;
    }

    public boolean isPublishOperation()
    {
        return publishOperation;
    }

    public void setPublishOperation(boolean publishOperation)
    {
        this.publishOperation = publishOperation;
    }

    /**
     * Returns true if this publisher is in listener mode
     */
    public boolean isPublisherListener()
    {
        return (getRemoteFlag() == PUBLISHER_LISTENER);
    }

    /**
     * Returns true if this is a publisher process, automatically execute the process
     */
    public boolean isRemotePublish()
    {
        return (getRemoteFlag() == REMOTE_PUBLISH);
    }

    /**
     * Returns true if this publisher is in terminal mode
     */
    public boolean isPublisherTerminal()
    {
        return (getRemoteFlag() == PUBLISHER_MANUAL);
    }

    /**
     * Returns true if this is any type of remote session
     *
     * @return true/false
     */
    public boolean isRemoteSession()
    {
        return (this.remoteFlag != NOT_REMOTE);
    }

    public boolean isRequestCollection()
    {
        return requestCollection;
    }

    public void setRequestCollection(boolean requestCollection)
    {
        this.requestCollection = requestCollection;
    }

    public boolean isRequestTargets()
    {
        return requestTargets;
    }

    public void setRequestTargets(boolean requestTargets)
    {
        this.requestTargets = requestTargets;
    }

    /**
     * Is specific publisher library boolean.
     *
     * @return the boolean
     */
    public boolean isSpecificPublisherLibrary()
    {
        return specificPublisherLibrary;
    }

    /**
     * Sets specific publisher library.
     *
     * @param specificPublisherLibrary the specific publisher library
     */
    public void setSpecificPublisherLibrary(boolean specificPublisherLibrary)
    {
        this.specificPublisherLibrary = specificPublisherLibrary;
    }

    /**
     * Returns true if subscriber is in listener mode
     */
    public boolean isSubscriberListener()
    {
        return (getRemoteFlag() == SUBSCRIBER_LISTENER);
    }

    /**
     * Returns true if this subscriber is in terminal mode
     */
    public boolean isSubscriberTerminal()
    {
        return (getRemoteFlag() == SUBSCRIBER_TERMINAL);
    }

    /**
     * Is validation run boolean.
     *
     * @return the boolean
     */
    public boolean isValidationRun()
    {
        return validationRun;
    }

    /**
     * Sets validation run.
     *
     * @param validationRun the validation run
     */
    public void setValidationRun(boolean validationRun)
    {
        this.validationRun = validationRun;
    }

}
