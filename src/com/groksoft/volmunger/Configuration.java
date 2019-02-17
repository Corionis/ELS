package com.groksoft.volmunger;

import java.util.ArrayList;

// see https://logging.apache.org/log4j/2.x/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configuration
 * <p>
 * Contains all command-line options and any other application-level configuration.
 */
public class Configuration {
    private final String VOLMUNGER_VERSION = "1.0.0";
    private static Configuration instance = null;

    // flags & names
    private String consoleLevel = "debug";  // Levels: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, and OFF
    private String debugLevel = "info";

    private boolean keepVolMungerFiles = false;
    private String logFilename = "volmunger.log";
    private boolean dryRun = false;
    private boolean validationRun = false;

    private String exportFilename = "";
    private String exportPathsFilename = "";
    private String mismatchFilename = "";
    private String publisherImportFilename = "";
    private String subscriberImportFilename = "";
    private String targetsFilename = "";
    private String whatsNewFilename = "";

    // publisher & subscriber
    private String publisherFileName = "";
    private ArrayList<String> publisherLibraryName = new ArrayList<>();
    private boolean specificPublisherLibrary = false;
    private String subscriberFileName = "";

    /**
     * Instantiates a new Configuration.
     */
    private Configuration() {
        // singleton pattern
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static Configuration getInstance() {
        if (instance == null) {
            instance = new Configuration();
        }
        return instance;
    }

    /**
     * Dump the configuration
     */
    public void dump(String[] argsCopy) {
        Logger logger = LogManager.getLogger("applog");

        String msg = "Arguments: ";
        for (int index = 0; index < argsCopy.length; ++index) {
            msg = msg + argsCopy[index] + " ";
        }
        logger.info(msg);

        logger.info("  cfg: -c Session logging level = " + getConsoleLevel());
        logger.info("  cfg: -d Debug logging level = " + getDebugLevel());
        logger.info("  cfg: -D Dry run = " + Boolean.toString(isDryRun()));
        logger.info("  cfg: -e Export paths filename = " + getExportPathsFilename());
        logger.info("  cfg: -f Log filename = " + getLogFilename());
        logger.info("  cfg: -i Export JSON filename = " + getExportFilename());
        logger.info("  cfg: -k Keep .volmunger files = " + Boolean.toString(isKeepVolMungerFiles()));
        logger.info("  cfg: -l Publisher library name(s):");
        for (String ln : getPublisherLibraryNames()) {
            logger.info("  cfg:     " + ln);
        }
        logger.info("  cfg: -m Mismatches output filename = " + getMismatchFilename());
        logger.info("  cfg: -n What's New output filename = " + getWhatsNewFilename());
        logger.info("  cfg: -p Publisher Library filename = " + getPublisherFileName());
        logger.info("  cfg: -P Publisher Collection import filename = " + getPublisherImportFilename());
        logger.info("  cfg: -s Session Library filename = " + getSubscriberFileName());
        logger.info("  cfg: -S Session Collection import filename = " + getSubscriberImportFilename());
        logger.info("  cfg: -t Targets filename = " + getTargetsFilename());
        logger.info("  cfg: -v Validation run = " + Boolean.toString(isValidationRun()));
    }

    /**
     * Gets console level.
     *
     * @return the console level
     */
    public String getConsoleLevel() {
        return consoleLevel;
    }

    /**
     * Sets console level.
     *
     * @param consoleLevel the console level
     */
    public void setConsoleLevel(String consoleLevel) {
        this.consoleLevel = consoleLevel;
    }

    /**
     * Gets export filename.
     *
     * @return the export filename
     */
    public String getExportFilename() {
        return exportFilename;
    }

    /**
     * Sets export filename.
     *
     * @param exportFilename the export filename
     */
    public void setExportFilename(String exportFilename) {
        this.exportFilename = exportFilename;
    }

    /**
     * Gets log filename.
     *
     * @return the log filename
     */
    public String getLogFilename() {
        return logFilename;
    }

    /**
     * Sets log filename.
     *
     * @param logFilename the log filename
     */
    public void setLogFilename(String logFilename) {
        this.logFilename = logFilename;
    }

    /**
     * Parse command line.
     *
     * @param args the args
     * @return the boolean
     * @throws MungerException the volmunger exception
     */
    public void parseCommandLine(String[] args) throws MungerException {
        int index;
        boolean success = true;

        for (index = 0; index < args.length; ++index) {
            switch (args[index]) {
                case "-c":                                             // console level
                    if (index <= args.length - 2) {
                        setConsoleLevel(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -c requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "-D":                                             // Dry run
                    setDryRun(true);
                    break;
                case "-d":                                             // debug level
                    if (index <= args.length - 2) {
                        setDebugLevel(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -d requires a level, trace, debug, info, warn, error, fatal, or off");
                    }
                    break;
                case "-e":                                             // export paths filename
                    if (index <= args.length - 2) {
                        setExportPathsFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -e requires an export paths output filename");
                    }
                    break;
                case "-f":                                             // log filename
                    if (index <= args.length - 2) {
                        setLogFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -f requires a log filename");
                    }
                    break;
                case "-i":                                             // export filename
                    if (index <= args.length - 2) {
                        setExportFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -i requires an export JSON output filename");
                    }
                    break;
                case "-k":                                             // keep .volmunger files
                    setKeepVolMungerFiles(true);
                    break;
                case "-l":                                             // publisher library to process
                    if (index <= args.length - 2) {
                        addPublisherLibraryName(args[index + 1]);
                        setSpecificPublisherLibrary(true);
                        ++index;
                    } else {
                        throw new MungerException("Error: -l requires a publisher library name");
                    }
                    break;
                case "-m":                                             // Mismatch output filename
                    if (index <= args.length - 2) {
                        setMismatchFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -m requires a mismatches output filename");
                    }
                    break;
                case "-n":                                             // What's New output filename
                    if (index <= args.length - 2) {
                        setWhatsNewFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -n requires a What's New output filename");
                    }
                    break;
                case "-p":                                             // publisher collection filename
                    if (index <= args.length - 2) {
                        setPublisherFileName(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -p requires a publisher collection filename");
                    }
                    break;
                case "-P":                                             // import publisher filename
                    if (index <= args.length - 2) {
                        setPublisherImportFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -P requires an publisher import JSON filename");
                    }
                    break;
                case "-s":                                             // subscriber collection filename
                    if (index <= args.length - 2) {
                        setSubscriberFileName(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -s requires a subscriber collection filename");
                    }
                    break;
                case "-S":                                             // import subscriber filename
                    if (index <= args.length - 2) {
                        setSubscriberImportFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -S requires an subscriber import JSON filename");
                    }
                    break;
                case "-t":                                             // targets filename
                    if (index <= args.length - 2) {
                        setTargetsFilename(args[index + 1]);
                        ++index;
                    } else {
                        throw new MungerException("Error: -t requires a targets filename");
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
     * Gets Main version.
     *
     * @return the Main version
     */
    public String getVOLMUNGER_VERSION() {
        return VOLMUNGER_VERSION;
    }

    /**
     * Gets debug level.
     *
     * @return the debug level
     */
    public String getDebugLevel() {
        return debugLevel;
    }

    /**
     * Sets debug level.
     *
     * @param debugLevel the debug level
     */
    public void setDebugLevel(String debugLevel) {
        this.debugLevel = debugLevel;
    }

    /**
     * Gets subscriber import filename.
     *
     * @return the import filename
     */
    public String getSubscriberImportFilename() {
        return subscriberImportFilename;
    }

    /**
     * Sets subscriber import filename.
     *
     * @param subscriberImportFilename the import filename
     */
    public void setSubscriberImportFilename(String subscriberImportFilename) {
        this.subscriberImportFilename = subscriberImportFilename;
    }

    /**
     * Is keep vol volmunger files boolean.
     *
     * @return the boolean
     */
    public boolean isKeepVolMungerFiles() {
        return keepVolMungerFiles;
    }

    /**
     * Sets keep vol volmunger files.
     *
     * @param keepVolMungerFiles the keep vol volmunger files
     */
    public void setKeepVolMungerFiles(boolean keepVolMungerFiles) {
        this.keepVolMungerFiles = keepVolMungerFiles;
    }

    /**
     * Gets mismatch filename.
     *
     * @return the mismatch filename
     */
    public String getMismatchFilename() {
        return mismatchFilename;
    }

    /**
     * Sets mismatch filename.
     *
     * @param mismatchFilename the mismatch filename
     */
    public void setMismatchFilename(String mismatchFilename) {
        this.mismatchFilename = mismatchFilename;
    }

    /**
     * Gets whats new filename.
     *
     * @return the whats new filename
     */
    public String getWhatsNewFilename() {
        return whatsNewFilename;
    }

    /**
     * Sets whats new filename.
     *
     * @param whatsNewFilename the whats new filename
     */
    public void setWhatsNewFilename(String whatsNewFilename) {
        this.whatsNewFilename = whatsNewFilename;
    }

    /**
     * Gets PatternLayout for log4j2.
     *
     * Call this method AFTER setDebugLevel() has been called.
     *
     * @return the PatternLayout to use
     */
    public String getPattern() {
        String withMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m [%t]:%C.%M:%L%n";
        String withoutMethod = "%-5p %d{MM/dd/yyyy HH:mm:ss.SSS} %m%n";
        if (getDebugLevel().trim().equalsIgnoreCase("info")) {
            return withoutMethod;
        }
        return withMethod;
    }

    /**
     * Gets publisher configuration file name.
     *
     * @return the publisher configuration file name
     */
    public String getPublisherFileName() {
        return publisherFileName;
    }

    /**
     * Sets publisher configuration file name.
     *
     * @param publisherFileName the publisher configuration file name
     */
    public void setPublisherFileName(String publisherFileName) {
        this.publisherFileName = publisherFileName;
    }

    /**
     * Gets publisher library name.
     *
     * @return the publisher library name
     */
    public ArrayList<String> getPublisherLibraryNames() {
        return publisherLibraryName;
    }

    /**
     * Add a publisher library name.
     *
     * @param publisherLibraryName the publisher library name
     */
    public void addPublisherLibraryName(String publisherLibraryName) {
        this.publisherLibraryName.add(publisherLibraryName);
    }

    /**
     * Is specific publisher library boolean.
     *
     * @return the boolean
     */
    public boolean isSpecificPublisherLibrary() {
        return specificPublisherLibrary;
    }

    /**
     * Sets specific publisher library.
     *
     * @param specificPublisherLibrary the specific publisher library
     */
    public void setSpecificPublisherLibrary(boolean specificPublisherLibrary) {
        this.specificPublisherLibrary = specificPublisherLibrary;
    }

    /**
     * Gets publisher import filename.
     *
     * @return the publisher import filename
     */
    public String getPublisherImportFilename() {
        return publisherImportFilename;
    }

    /**
     * Sets publisher import filename.
     *
     * @param publisherImportFilename the publisher import filename
     */
    public void setPublisherImportFilename(String publisherImportFilename) {
        this.publisherImportFilename = publisherImportFilename;
    }

    /**
     * Gets subscriber configuration file name.
     *
     * @return the subscriber configuration file name
     */
    public String getSubscriberFileName() {
        return subscriberFileName;
    }

    /**
     * Sets subscriber configuration file name.
     *
     * @param subscriberFileName the subscriber configuration file name
     */
    public void setSubscriberFileName(String subscriberFileName) {
        this.subscriberFileName = subscriberFileName;
    }

    /**
     * Gets targets filename.
     *
     * @return the targets filename
     */
    public String getTargetsFilename() {
        return targetsFilename;
    }

    /**
     * Sets targets filename.
     *
     * @param targetsFilename the targets filename
     */
    public void setTargetsFilename(String targetsFilename) {
        this.targetsFilename = targetsFilename;
    }

    /**
     * Is dry run boolean.
     *
     * @return the boolean
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * Sets dry run.
     *
     * @param dryRun true/false boolean
     */
    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    /**
     * Is validation run boolean.
     *
     * @return the boolean
     */
    public boolean isValidationRun() {
        return validationRun;
    }

    /**
     * Sets validation run.
     *
     * @param validationRun the validation run
     */
    public void setValidationRun(boolean validationRun) {
        this.validationRun = validationRun;
    }


    /**
     * @return exportPathsFilename
     */
    public String getExportPathsFilename() {
        return exportPathsFilename;
    }

    /**
     * @param exportPathsFilename
     */
    public void setExportPathsFilename(String exportPathsFilename) {
        this.exportPathsFilename = exportPathsFilename;
    }

}
