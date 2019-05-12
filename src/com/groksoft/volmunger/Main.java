package com.groksoft.volmunger;

// see https://logging.apache.org/log4j/2.x/
import com.groksoft.volmunger.comm.CommManager;
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
            volmunger.logger.info("VolMunger is operating in remote mode");
        }
        //System.exit(returnValue);
    } // main

    public int process(String[] args) {
        Repository repo = null;
        int returnValue = 0;
        ThreadGroup sessionThreads = null;

        try {
            Configuration cfg = new Configuration();
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

            // handle -r remote session option as Subscriber ... listen
            if (cfg.amRemoteSubscriber()) {
                // the + makes searching for the beginning of a run easier
                logger.info("+ VolMunger Remote Subscriber begin, version " + cfg.getVOLMUNGER_VERSION() + " ------------------------------------------");
                cfg.dump();
                repo = readRepo(cfg); // returns either the Publisher or Subscriber repo based on args

                sessionThreads = new ThreadGroup("Server");
                commManager = new CommManager(sessionThreads, 10, cfg);

                if (repo.getJsonFilename() != null && !repo.getJsonFilename().isEmpty()) {
                    commManager.startListening(repo);
                    isListening = true;
                }
            }
            else {
                // PUSH MODE ---------------------------------------------------------
                // -r or --remote-subscriber execute the process
                //
                // the Process class handles the entire VolMunger munger process
                Process proc = new Process();
                //
                // cfg can be null so a new configuration is built based on args
                returnValue = proc.process(cfg, args);

                // then ask if the subscriber has anything else to be done
                // .. if so a new command line & process can be executed
                // .. if not end the program


                // PULL MODE --------------------------------------------------------
                // if -R or --request-server (or whatever) start a listener and
                // wait for matching Subscriber to tell Publisher what to do

                // ... to be coded ...

                // HAVE CHANGED:
                // -t and added -T where existing scripts and batch files need to be edited changing -t to -T

                // need to add a command line option to request a specific item
                //
                // ideas for adds & changes --
                // 1. change export collection -i, to -E
                // a. add -i to request a specific item
                // 3. add -R P|S for --request-server PULL mode
                // 3. add "double-dash" equivalent options to make command lines more readable
                //    examples: -E and --export-collection, -r and --remote-subscriber
                //    It's just adding more case statements in Configuration.parseCommandLine()
                //
                // With -l to spec the library, and -i to spec the item we have a way to make a request.
                // An "item" is the granularity of a movie (directory) or a tv season (directory).
                //
            }

        }
        catch (Exception e) {
            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
        }
        finally {
            if (commManager != null && !isListening) {
                commManager.stopCommManager();
            } else {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    public void run() {
                        try {
                            Thread.sleep(200);
                            logger.info("Shutting down CommManager ...");
                            //some cleaning up code...
                            commManager.stopCommManager();

                        }
                        catch (InterruptedException e) {
                            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
                        }
                    }
                });
            }
        }

        return returnValue;
    } // process

    private Repository readRepo(Configuration cfg) throws Exception {
        Repository repo = new Repository(cfg);
        if (cfg.amRemotePublisher())
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
                System.out.println("\r\nMust use -p or -P with a filename to use -r P");
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

        if (cfg.amRemoteSubscriber())
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
                System.out.println("\r\nMust use -s or -S with a filename to use -r S");
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
