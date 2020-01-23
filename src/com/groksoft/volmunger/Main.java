package com.groksoft.volmunger;

// see https://logging.apache.org/log4j/2.x/
import com.groksoft.volmunger.comm.subscriber.CommManager;
import com.groksoft.volmunger.comm.subscriber.Transfer;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        if (volmunger.isListening)
        {
            volmunger.logger.info("VolMunger is operating in remote subscriber mode");
        }
        //System.exit(returnValue);
    } // main

    public int process(String[] args) {
        int returnValue = 0;
        ThreadGroup sessionThreads = null;
        Configuration cfg = new Configuration();

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

            // handle -r S or --remote-subscriber session ... listen
            if (cfg.amRemoteSubscriber()) {
                // the + makes searching for the beginning of a run easier
                logger.info("+ VolMunger Remote Subscriber begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                cfg.dump();

                Repository publisherRepo = readRepo(cfg, true);
                Repository subscriberRepo = readRepo(cfg, false);

                //sessionThreads = new ThreadGroup("Server");
                //commManager = new CommManager(sessionThreads, 10, cfg, publisherRepo, subscriberRepo);

                if (subscriberRepo.getJsonFilename() != null && !subscriberRepo.getJsonFilename().isEmpty()) {
                    //commManager.startListening(subscriberRepo);
                    transfer = new Transfer();
                    transfer.start();
                    isListening = true;
                }
            }
            else {
                // -r P or --remote-publisher, execute the process
                //
                // the Process class handles the entire VolMunger process
                Process proc = new Process();
                //
                // cfg can be null so a new configuration is built based on args
                returnValue = proc.process(cfg, args);

                // then ask if the subscriber has anything else to be done
                // .. if so a new command line & process can be executed
                // .. if not end the program


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

                if (cfg.amRemotePublisher())
                {
                    Thread.sleep(Long.MAX_VALUE);
                }
            }

        }
        catch (Exception e) {
            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
        }
        finally {
            if (commManager != null && !isListening) {
                commManager.stopCommManager();
            } else {
                if (commManager != null) {
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
            if (cfg.getPublisherLibrariesFileName().length() > 0) {
                repo.read(cfg.getPublisherLibrariesFileName());
            }
            // get -P Publisher collection
            if (cfg.getPublisherCollectionFilename().length() > 0) {
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
