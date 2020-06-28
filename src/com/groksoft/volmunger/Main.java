package com.groksoft.volmunger;

// see https://logging.apache.org/log4j/2.x/

import com.groksoft.volmunger.sftp.Client;
import com.groksoft.volmunger.sftp.Server;
import com.groksoft.volmunger.stty.Stty;
import com.groksoft.volmunger.stty.SttyClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmunger.repository.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.groksoft.volmunger.Configuration.*;

/**
 * Main VolMunger program
 */
public class Main
{
    private boolean isListening = false;
    private Logger logger = null;

    public Repository publisherRepo;
    public Client sftpClient;
    public Server sftp;
    public Stty stty;
    public SttyClient sttyClient;
    public Repository subscriberRepo;

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

            // todo Add sanity checks for option combinations that do not make sense

            // an execution of this program can only be configured as one of these
            switch (cfg.getRemoteFlag())
            {
                // handle standard local execution, no -r option
                case NOT_REMOTE:
                    logger.info("+ VolMunger Local Process begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    // the Process class handles the VolMunger process
                    proc = new Process();
                    returnValue = proc.process(cfg);
                    break;

                // handle -r S subscriber listener
                case SUBSCRIBER_LISTENER:
                    logger.info("+ VolMunger Subscriber Listener begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    if (cfg.isRequestTargets() && Files.notExists(Paths.get(cfg.getTargetsFilename())))
                        throw new MungerException("-t file not found: " + cfg.getTargetsFilename());

                    publisherRepo = readRepo(cfg, false);
                    subscriberRepo = readRepo(cfg, true);

                    if (subscriberRepo.getJsonFilename() != null && !subscriberRepo.getJsonFilename().isEmpty())
                    {
                        // start stty server
                        sessionThreads = new ThreadGroup("SServer");
                        stty = new Stty(sessionThreads, 10, cfg, publisherRepo, subscriberRepo);
                        stty.startListening(subscriberRepo);

                        // start sftp server
                        sftp = new Server(publisherRepo, subscriberRepo);
                        sftp.startServer();

                        isListening = true;
                    }
                    else
                    {
                        throw new MungerException("A subscriber library (-s) or collection file (-S) is required for -r S");
                    }
                    break;

                // handle -r P execute the process
                case PUBLISHER_PROCESS:
                    logger.info("+ VolMunger Publisher Process Remote Subscriber begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    if (cfg.getSubscriberCollectionFilename().length() > 0 || cfg.getSubscriberLibrariesFileName().length() > 0)
                    {
                        // the Process class handles the VolMunger process
                        proc = new Process();
                        returnValue = proc.process(cfg);
                        if (returnValue == 0)
                        {
                            Thread.sleep(Long.MAX_VALUE);
                        }
                    }
                    else
                    {
                        throw new MungerException("A subscriber library or collection file is required for -r P");
                    }
                    break;

                // handle -r M publisher terminal
                case PUBLISHER_TERMINAL:
                    logger.info("+ VolMunger Publisher SttyClient begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    // start the sftp client
                    sftpClient = new Client(publisherRepo, subscriberRepo);
                    sftpClient.startClient();

                    // start the stty client interactively
                    sttyClient = new SttyClient(cfg, true);
                    if (sttyClient.connect(publisherRepo, subscriberRepo))
                    {
                        sttyClient.guiSession();
                    }
                    else
                    {
                        throw new MungerException("Publisher SttyClient failed to connect");
                    }
                    break;

                // handle -r L publisher listener
                case PUBLISHER_LISTENER:
                    logger.info("+ VolMunger Publisher Listener begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    if (publisherRepo.getJsonFilename() != null && !publisherRepo.getJsonFilename().isEmpty())
                    {
                        // start stty server
                        sessionThreads = new ThreadGroup("PServer");
                        stty = new Stty(sessionThreads, 10, cfg, subscriberRepo, publisherRepo);
                        stty.startListening(publisherRepo);

                        // start sftp server
                        sftp = new Server(subscriberRepo, publisherRepo);
                        sftp.startServer();

                        isListening = true;
                    }
                    else
                    {
                        throw new MungerException("A publisher library (-p) or collection file (-P) is required for -r L");
                    }
                    break;

                // handle -r T subscriber terminal
                case SUBSCRIBER_TERMINAL:
                    logger.info("+ VolMunger Subscriber SttyClient begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, false);
                    subscriberRepo = readRepo(cfg, true);

                    // start the sftp client
                    sftpClient = new Client(subscriberRepo, publisherRepo);
                    sftpClient.startClient();

                    // start the stty client interactively
                    sttyClient = new SttyClient(cfg, true);
                    if (sttyClient.connect(subscriberRepo, publisherRepo))
                    {
                        sttyClient.guiSession();
                    }
                    else
                    {
                        throw new MungerException("Subscriber SttyClient failed to connect");
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
            returnValue = 1;
        }
        finally
        {
            if (stty != null)       // LEFTOFF
            {
                if (!isListening)
                {
                    stty.stopServer();
                }
                else
                {
                    Runtime.getRuntime().addShutdownHook(new Thread()
                    {
                        public void run()
                        {
                            try
                            {
                                Thread.sleep(200);
                                logger.info("Shutting down communications ...");

                                // some clean up code...
                                stty.stopServer();
                            }
                            catch (InterruptedException e)
                            {
                                logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
                            }
                        }
                    });
                }
            }

            // stop the stty client
            if (sttyClient != null)
            {
                sttyClient.disconnect();
            }

            // stop the sftp client
            if (sftpClient != null)
            {
                sftpClient.stopClient();
            }

            // stop the sftp server
            if (sftp != null)
            {
                try
                {
                    sftp.stopServer();
                }
                catch (IOException e)
                {
                    // ignore any exception
                }
            }
        }

        return returnValue;
    } // process

    private Repository readRepo(Configuration cfg, boolean isPublisher) throws Exception
    {
        Repository repo = new Repository(cfg);
        if (isPublisher)
        {
            if (cfg.getPublisherLibrariesFileName().length() > 0 &&
                    cfg.getPublisherCollectionFilename().length() > 0)
            {
                System.out.println("\r\nCannot use both -p and -P, exiting");
                return null;
            }
            else if (cfg.getPublisherLibrariesFileName().length() == 0 &&
                    cfg.getPublisherCollectionFilename().length() == 0)
            {
                System.out.println("\r\nMust use -p or -P with a filename to use -r");
                return null;
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
            if (cfg.getSubscriberLibrariesFileName().length() > 0 &&
                    cfg.getSubscriberCollectionFilename().length() > 0)
            {
                System.out.println("\r\nCannot use both -s and -S, exiting");
                return null;
            }
            else if (cfg.getSubscriberLibrariesFileName().length() == 0 &&
                    cfg.getSubscriberCollectionFilename().length() == 0)
            {
                System.out.println("\r\nMust use -s or -S with a filename to use -r");
                return null;
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
        return repo;
    }

} // Main
