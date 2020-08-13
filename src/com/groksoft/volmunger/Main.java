package com.groksoft.volmunger;

// see https://logging.apache.org/log4j/2.x/

import com.groksoft.volmunger.repository.Repository;
import com.groksoft.volmunger.sftp.ClientSftp;
import com.groksoft.volmunger.sftp.ServeSftp;
import com.groksoft.volmunger.stty.ClientStty;
import com.groksoft.volmunger.stty.ServeStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Paths;

import static com.groksoft.volmunger.Configuration.*;

/**
 * Main VolMunger program
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
        Main volmunger = new Main();
        int returnValue = volmunger.process(args);
    } // main

    public int process(String[] args)
    {
        int returnValue = 0;
        ThreadGroup sessionThreads = null;
        Configuration cfg = new Configuration();
        Process proc;

        // HAVE CHANGED:
        // -t and added -T where existing scripts and batch files need to be edited changing -t to -T

        // ideas for adds & changes --
        // 1. add -g to get/request a specific item
        // 2. add "double-dash" equivalent options to make command lines more readable
        //    examples: -E and --export-collection, -r and --remote-subscriber
        //    It's just adding more case statements in Configuration.parseCommandLine()
        //
        // With -l to spec the library, and -g to get the item we have a way to make a request.
        // An "item" is the granularity of a movie (directory) or a tv season (directory).
        //

        try
        {
            cfg.parseCommandLine(args);

            // setup the logger based on configuration
            System.setProperty("logFilename", cfg.getLogFilename());
            System.setProperty("consoleLevel", cfg.getConsoleLevel());
            System.setProperty("debugLevel", cfg.getDebugLevel());
            System.setProperty("pattern", cfg.getPattern());
            org.apache.logging.log4j.core.LoggerContext ctx = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();

            // get the named logger
            logger = LogManager.getLogger("applog");

            // an execution of this program can only be configured as one of these
            switch (cfg.getRemoteFlag())
            {
                // handle standard local execution, no -r option
                case NOT_REMOTE:
                    logger.info("+ VolMunger Local Process begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // the Process class handles the VolMunger process
                    proc = new Process(cfg, context);
                    returnValue = proc.process();
                    break;

                // handle -r L publisher listener for remote subscriber -r T connections
                case PUBLISHER_LISTENER:
                    logger.info("+ VolMunger Publisher Listener begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start servers
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.subscriberRepo, context.publisherRepo);
                        context.serveSftp.startServer();

                        // start serveStty server
                        sessionThreads = new ThreadGroup("PServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context.subscriberRepo, context.publisherRepo);
                        context.serveStty.startListening(context.publisherRepo);

                        isListening = true;
                    }
                    else
                    {
                        throw new MungerException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // handle -r M publisher manual terminal to remote subscriber -r S
                case PUBLISHER_MANUAL:
                    logger.info("+ VolMunger Publisher Manual Terminal begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo);
                        context.clientSftp.startClient();

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true);
                        if (context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungerException("Publisher console failed to connect");
                        }
                    }
                    break;

                // handle -r P execute the automated process to remote subscriber -r S
                case REMOTE_PUBLISH:
                    logger.info("+ VolMunger Publish Process to Remote Subscriber begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.NO_VALIDATE);

                    // start clients
                    if (context.publisherRepo.isInitialized() && context.subscriberRepo.isInitialized())
                    {
                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.publisherRepo, context.subscriberRepo);
                        context.clientSftp.startClient();

                        // start the serveStty client for automation
                        context.clientStty = new ClientStty(cfg, false);
                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                        {
                            throw new MungerException("Publisher remote failed to connect");
                        }

                        // the Process class handles the VolMunger process
                        proc = new Process(cfg, context);
                        returnValue = proc.process();
                        if (returnValue == 0)
                        {
//                            logger.info("sleeping (1)");
//                            Thread.sleep(Long.MAX_VALUE);
                        }
                    }
                    else
                    {
                        throw new MungerException("Publisher and subscriber options are required for -r P");
                    }
                    break;

                // handle -r S subscriber listener for publisher -r P|M connections
                case SUBSCRIBER_LISTENER:
                    logger.info("+ VolMunger Subscriber Listener begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    if (cfg.isRequestTargets() && Files.notExists(Paths.get(cfg.getTargetsFilename())))
                        throw new MungerException("Targets -t file not found: " + cfg.getTargetsFilename());

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start servers
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // start serveSftp server
                        context.serveSftp = new ServeSftp(context.publisherRepo, context.subscriberRepo);
                        context.serveSftp.startServer();

                        // start serveStty server
                        sessionThreads = new ThreadGroup("SServer");
                        context.serveStty = new ServeStty(sessionThreads, 10, cfg, context.publisherRepo, context.subscriberRepo);
                        context.serveStty.startListening(context.subscriberRepo);

                        isListening = true;
                    }
                    else
                    {
                        throw new MungerException("Subscriber and publisher options are required for -r S");
                    }
                    break;

                // handle -r T subscriber manual terminal to publisher -r L
                case SUBSCRIBER_TERMINAL:
                    logger.info("+ VolMunger Subscriber Manual Terminal begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    context.publisherRepo = readRepo(cfg, Repository.PUBLISHER, Repository.NO_VALIDATE);
                    context.subscriberRepo = readRepo(cfg, Repository.SUBSCRIBER, Repository.VALIDATE);

                    // start clients
                    if (context.subscriberRepo.isInitialized() && context.publisherRepo.isInitialized())
                    {
                        // start the serveSftp client
                        context.clientSftp = new ClientSftp(context.subscriberRepo, context.publisherRepo);
                        context.clientSftp.startClient();

                        // start the serveStty client interactively
                        context.clientStty = new ClientStty(cfg, true);
                        if (context.clientStty.connect(context.subscriberRepo, context.publisherRepo))
                        {
                            context.clientStty.guiSession();
                            isListening = true; // fake listener to wait for shutdown
                        }
                        else
                        {
                            throw new MungerException("Subscriber remote failed to connect");
                        }
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
            if (!isListening)
            {
                stopServices();
            }
            else
            {
                Runtime.getRuntime().addShutdownHook(new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            //logger.info("sleeping 10 seconds");
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
        if (validate && repo.isInitialized())
        {
            repo.validate();
        }
        return repo;
    }

    public void stopServices()
    {
//        logger.info("stopping services");
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

    // easier way to pass these data
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
