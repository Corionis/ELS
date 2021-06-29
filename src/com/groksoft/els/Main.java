package com.groksoft.els;

import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.Date;

import static com.groksoft.els.Configuration.*;

/**
 * ELS main program
 */
public class Main
{
    public boolean isListening = false;
    Context context = new Context();
    private Logger logger = null;

    /**
     * Instantiates the Main application
     */
    public Main()
    {
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        Main els = new Main();
        int returnValue = els.process(args);
    } // main

    /**
     * execute the process
     *
     * @param args the input arguments
     * @return
     */
    public int process(String[] args)
    {
        int returnValue = 0;
        ThreadGroup sessionThreads = null;
        Configuration cfg = new Configuration();
        Process proc;
        Date stamp = new Date();

        try
        {
            cfg.parseCommandLine(args);

            // setup the logger based on configuration
            System.setProperty("logFilename", cfg.getLogFilename());
            System.setProperty("consoleLevel", cfg.getConsoleLevel());
            System.setProperty("debugLevel", cfg.getDebugLevel());
            System.setProperty("pattern", cfg.getPattern());
            org.apache.logging.log4j.core.LoggerContext lctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            lctx.reconfigure();

            org.apache.logging.log4j.core.config.Configuration ccfg = lctx.getConfiguration();
            LoggerConfig lcfg = ccfg.getLoggerConfig("Console");
            lcfg.setLevel(Level.toLevel(cfg.getConsoleLevel()));
            lcfg = ccfg.getLoggerConfig("applog");
            lcfg.setLevel(Level.toLevel(cfg.getDebugLevel()));
            lctx.updateLoggers();

            // get the named logger
            logger = LogManager.getLogger("applog");

            // an execution of this program can only be configured as one of these
            logger.info("+------------------------------------------");
            switch (cfg.getRemoteFlag())
            {
                // handle standard local execution, no -r option
                case NOT_REMOTE:
                    logger.info("ELS Local Process begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    if (!cfg.isValidation()) // only publisher needed for a JSON file validation
                    {
                        context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);
                    }

                    // the Process class handles the ELS process
                    proc = new Process(cfg, context);
                    returnValue = proc.process();
                    break;

                // handle -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    logger.info("ELS Publisher Listener begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start servers for -r T & clients for get command in stty.publisher.Daemon
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("PServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, true);
                        context.serveStty.startListening(context.publisherRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.publisherRepo, context.subscriberRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungerException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // handle -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    logger.info("ELS Publisher Manual Terminal begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true, true);
                        if (context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungerException("Publisher manual console failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungerException("Publisher sftp client failed to connect");
                        }
                    }
                    break;

                // handle -r P execute the automated process to remote subscriber -r S
                case REMOTE_PUBLISH:
                    logger.info("ELS Publish Process to Remote Subscriber begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveStty client for automation
                        context.clientStty = new ClientStty(cfg, false, true);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungerException("Publisher remote failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungerException("Publisher sftp client failed to connect");
                        }

                        // the Process class handles the ELS process
                        proc = new Process(cfg, context);
                        returnValue = proc.process();
                    }
                    else
                    {
                        throw new MungerException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // handle -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    logger.info("ELS Subscriber Listener begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungerException("Targets -t | -T required");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start servers
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, true);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.subscriberRepo, context.publisherRepo, true);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungerException("Subscriber and publisher options are required for -r S");
                    }
                    break;

                // handle -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    logger.info("ELS Subscriber Manual Terminal begin, version " + cfg.getProgramVersionN());
                    cfg.dump();

                    if (!cfg.isTargetsEnabled())
                        throw new MungerException("Targets -t | -T required");

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients & servers for -r L for get command
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true, true);
                        if (context.clientStty.connect(context.subscriberRepo, context.publisherRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungerException("Subscriber terminal console failed to connect");
                        }

                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.subscriberRepo, context.publisherRepo, true);
                        if (!context.clientSftp.startClient())
                        {
                            throw new MungerException("Publisher sftp client failed to connect");
                        }

                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context, false);
                        context.serveStty.startListening(context.subscriberRepo);
                        isListening = true;

                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.subscriberRepo, context.publisherRepo, false);
                        context.serveSftp.startServer();
                    }
                    else
                    {
                        throw new MungerException("A subscriber -s or -S file and publisher -p or -P) is required for -r T");
                    }
                    break;

                default:
                    throw new MungerException("Unknown type of remote");
            }

        }
        catch (Exception e)
        {
            if (logger != null)
            {
                logger.error(e.getMessage());
            }
            else
            {
                System.out.println(e.getMessage());
            }
            isListening = false; // force stop
            returnValue = 1;
        }
        finally
        {
            if ( ! isListening)
            {
                stopServices();
                Date done = new Date();
                long millis = Math.abs(done.getTime() - stamp.getTime());
                logger.fatal("Runtime: " + Utils.getDuration(millis));
            }
            else
            {
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            if (context.clientStty != null)
                            {
                                context.clientStty.disconnect();
                            }

                            Date done = new Date();
                            long millis = Math.abs(done.getTime() - stamp.getTime());
                            logger.fatal("Runtime: " + Utils.getDuration(millis));

                            logger.info("stopping services");
                            Thread.sleep(10000L);
                            stopServices();
                        }
                        catch (Exception e)
                        {
                            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
                        }
                    }
                });
            }
        }

        return returnValue;
    } // process

    /**
     * Read either publisher or subscriber repository
     *
     * @param cfg         Loaded configuration
     * @param isPublisher Is this the publisher? true/false
     * @param validate    Validate repository against actual directories and files true/false
     * @return Repository object
     * @throws Exception
     */
    private Repository readRepo(Configuration cfg, boolean isPublisher, boolean validate) throws Exception
    {
        Repository repo = new Repository(cfg);
        if (isPublisher)
        {
            if (cfg.getPublisherLibrariesFileName().length() > 0 &&                     // both
                    cfg.getPublisherCollectionFilename().length() > 0)
            {
                throw new MungerException("Cannot use both -p and -P");
            }
            else if (cfg.getPublisherLibrariesFileName().length() == 0 &&               // neither
                    cfg.getPublisherCollectionFilename().length() == 0)
            {
                if (cfg.isRemoteSession())
                {
                    throw new MungerException("A -p publisher library or -P collection file is required for -r P");
                }
                else
                {
                    throw new MungerException("A -p publisher library or -P collection file is required, or the filename missing from -p or -P");
                }
            }

            // get -p Publisher libraries
            if (cfg.getPublisherLibrariesFileName().length() > 0)
            {
                repo.read(cfg.getPublisherLibrariesFileName());

            }
            // get -P Publisher collection
            if (cfg.getPublisherCollectionFilename().length() > 0)
            {
                repo.read(cfg.getPublisherCollectionFilename());
            }
        }
        else
        {
            if (cfg.getSubscriberLibrariesFileName().length() > 0 &&                    // both
                    cfg.getSubscriberCollectionFilename().length() > 0)
            {
                throw new MungerException("Cannot use both -s and -S");
            }
            else if (cfg.getSubscriberLibrariesFileName().length() == 0 &&              // neither
                    cfg.getSubscriberCollectionFilename().length() == 0)
            {
                if (cfg.isRemoteSession())
                {
                    throw new MungerException("A -s subscriber library or -S collection file is required for -r S");
                }
                else
                {
                    if (cfg.isPublishOperation())
                    {
                        throw new MungerException("A -s subscriber library or -S collection file is required, or the filename missing for -s or -S");
                    }
                    return null;
                }
            }

            // get -s Subscriber libraries
            if (cfg.getSubscriberLibrariesFileName().length() > 0)
            {
                repo.read(cfg.getSubscriberLibrariesFileName());
            }

            // get -S Subscriber collection
            if (cfg.getSubscriberCollectionFilename().length() > 0)
            {
                repo.read(cfg.getSubscriberCollectionFilename());
            }
        }

        // -v | --validate option
        if (validate && repo.isInitialized())
        {
            repo.validate();
        }

        return repo;
    }

    /**
     * Stop all service that are in use
     */
    public void stopServices()
    {
        if (context.clientStty != null)
        {
            context.clientStty.disconnect();
        }
        if (context.serveStty != null)
        {
            context.serveStty.stopServer();
        }
        if (context.clientSftp != null)
        {
            context.clientSftp.stopClient();
        }
        if (context.serveSftp != null)
        {
            context.serveSftp.stopServer();
        }
    }

    /**
     * Class to make passing these data easier
     */
    public class Context
    {
        public ClientSftp clientSftp;
        public ClientStty clientStty;
        public Repository publisherRepo;
        public ServeSftp serveSftp;
        public ServeStty serveStty;
        public Repository subscriberRepo;
    }

} // Main
