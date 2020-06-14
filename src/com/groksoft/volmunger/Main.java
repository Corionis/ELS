package com.groksoft.volmunger;

// see https://logging.apache.org/log4j/2.x/
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.groksoft.volmunger.comm.CommManager;
import com.groksoft.volmunger.comm.Terminal;
import com.groksoft.volmunger.comm.Transfer;
import com.groksoft.volmunger.repository.Repository;

import static com.groksoft.volmunger.Configuration.*;

/**
 * Main VolMunger program
 */
public class Main
{
    private boolean isListening = false;
    private Logger logger = null;
    CommManager commManager = null;
    Transfer transfer = null;

    /**
     * Instantiates the Main application
     */
    public Main() {
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args) {
        Main volmunger = new Main();
        int returnValue = volmunger.process(args);
        //System.exit(returnValue);
    } // main

    public int process(String[] args) {
        int returnValue = 0;
        ThreadGroup sessionThreads = null;
        Configuration cfg = new Configuration();
        Process proc;
        Repository publisherRepo;
        Repository subscriberRepo;

        try {
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

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    sessionThreads = new ThreadGroup("Server");
                    commManager = new CommManager(sessionThreads, 10, cfg, publisherRepo, subscriberRepo);

                    if (subscriberRepo.getJsonFilename() != null && !subscriberRepo.getJsonFilename().isEmpty()) {
                        commManager.startListening(subscriberRepo);
                        isListening = true;
                    }
                    break;

                // handle -r P execute the process
                case PUBLISHER_PROCESS:
                    logger.info("+ VolMunger Publisher Process Remote Subscriber begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    // the Process class handles the VolMunger process
                    proc = new Process();
                    returnValue = proc.process(cfg);
                    Thread.sleep(Long.MAX_VALUE);
                    break;

                // handle -r B publisher terminal
                case PUBLISHER_TERMINAL:
                    logger.info("+ VolMunger Publisher Terminal begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    Terminal pubTerminal = new Terminal(cfg, true);
                    if (pubTerminal.connect(publisherRepo, subscriberRepo)) {
                        pubTerminal.session();
                    } else {
                        throw new MungerException("Publisher Terminal failed to connect");
                    }
                    break;

                // handle -r L publisher listener
                case PUBLISHER_LISTENER:
                    logger.info("+ VolMunger Publisher Listener begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    sessionThreads = new ThreadGroup("Server");
                    commManager = new CommManager(sessionThreads, 10, cfg, publisherRepo, subscriberRepo);

                    if (publisherRepo.getJsonFilename() != null && !publisherRepo.getJsonFilename().isEmpty()) {
                        commManager.startListening(publisherRepo);
                        isListening = true;
                    }
                    break;

                // handle -r T subscriber terminal
                case SUBSCRIBER_TERMINAL:
                    logger.info("+ VolMunger Subscriber Terminal begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                    cfg.dump();

                    publisherRepo = readRepo(cfg, true);
                    subscriberRepo = readRepo(cfg, false);

                    Terminal subTerminal = new Terminal(cfg, true);
                    if (subTerminal.connect(publisherRepo, subscriberRepo)) {
                        subTerminal.session();
                    } else {
                        throw new MungerException("Subscriber Terminal failed to connect");
                    }
                    break;

                default:
                    throw new MungerException("Unknown type of remote");
            }

        } catch (Exception e) {
            logger.error(e.getMessage()); // + "\r\n" + Utils.getStackTrace(e));
            returnValue = 1;
        } finally {
            if (commManager != null) {
                if (!isListening) {
                    commManager.stopCommManager();
                } else {
                    Runtime.getRuntime().addShutdownHook(new Thread()
                    {
                        public void run() {
                            try {
                                Thread.sleep(200);
                                logger.info("Shutting down CommManager ...");
                                // some clean up code...
                                commManager.stopCommManager();

                            } catch (InterruptedException e) {
                                logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
                            }
                        }
                    });
                }
            }

        }

        return returnValue;
    } // process

    private Repository readRepo(Configuration cfg, boolean isPublisher) throws Exception {
        Repository repo = new Repository(cfg);
        if (isPublisher) {
            if (cfg.getPublisherLibrariesFileName().length() > 0 &&
                    cfg.getPublisherCollectionFilename().length() > 0) {
                System.out.println("\r\nCannot use both -p and -P, exiting");
                return null;
            } else if (cfg.getPublisherLibrariesFileName().length() == 0 &&
                    cfg.getPublisherCollectionFilename().length() == 0) {
                System.out.println("\r\nMust use -p or -P with a filename to use -r");
                return null;
            }

            // get -p Publisher libraries
            if (cfg.getPublisherLibrariesFileName().length() > 0) {
                repo.read(cfg.getPublisherLibrariesFileName());
            }
            // get -P Publisher collection
            if (cfg.getPublisherCollectionFilename().length() > 0) {
                repo.read(cfg.getPublisherCollectionFilename());
            }
        } else {
            if (cfg.getSubscriberLibrariesFileName().length() > 0 &&
                    cfg.getSubscriberCollectionFilename().length() > 0) {
                System.out.println("\r\nCannot use both -s and -S, exiting");
                return null;
            } else if (cfg.getSubscriberLibrariesFileName().length() == 0 &&
                    cfg.getSubscriberCollectionFilename().length() == 0) {
                System.out.println("\r\nMust use -s or -S with a filename to use -r");
                return null;
            }

            // get -s Subscriber libraries
            if (cfg.getSubscriberLibrariesFileName().length() > 0) {
                repo.read(cfg.getSubscriberLibrariesFileName());
            }

            // get -S Subscriber collection
            if (cfg.getSubscriberCollectionFilename().length() > 0) {
                repo.read(cfg.getSubscriberCollectionFilename());
            }
        }
        return repo;
    }

} // Main
