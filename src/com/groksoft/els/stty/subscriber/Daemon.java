package com.groksoft.els.stty.subscriber;

import com.groksoft.els.*;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.stty.DaemonBase;
import com.groksoft.els.stty.ServeStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;

/**
 * Subscriber Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
@SuppressWarnings("Duplicates")
public class Daemon extends DaemonBase
{
    protected static Logger logger = LogManager.getLogger("applog");

    private Context context;
    private boolean fault = false;
    private HintKeys hintKeys = null;
    private Hints hints = null;
    private boolean isTerminal = false;

    /**
     * Instantiate the Daemon service
     *
     * @param config
     * @param ctxt
     */
    public Daemon(Configuration config, Context ctxt, Repository mine, Repository theirs)
    {
        super(config, mine, theirs);
        context = ctxt;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     */
    public synchronized String dumpStatistics()
    {
        String data = "\r\nConsole currently connected: " + ((connected) ? "true" : "false") + "\r\n";
        data += "  Connected on port: " + port + "\r\n";
        data += "  Connected to: " + address + "\r\n";
        return data;
    } // dumpStatistics

    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName()
    {
        return "Daemon";
    } // getName

    private String getNextToken(StringTokenizer t)
    {
        String value = "";
        if (t.hasMoreTokens())
        {
            value = t.nextToken();
            if (value.trim().length() == 0 && t.hasMoreTokens())
                value = t.nextToken();
        }
        return value;
    }

    public String handshake()
    {
        String system = "";
        try
        {
            Utils.writeStream(out, myKey, "HELO");

            String input = Utils.readStream(in, myKey);
            if (input != null && (input.equals("DribNit") || input.equals("DribNlt")))
            {
                isTerminal = input.equals("DribNit");
                if (isTerminal && myRepo.getLibraryData().libraries.terminal_allowed != null &&
                        !myRepo.getLibraryData().libraries.terminal_allowed)
                {
                    Utils.writeStream(out, myKey, "Terminal session not allowed");
                    logger.warn("Attempt made to login interactively but terminal sessions are not allowed");                    return system;
                }
                Utils.writeStream(out, myKey, myKey);

                input = Utils.readStream(in, myKey);
                if (input.equals(theirKey))
                {
                    // send my flavor
                    Utils.writeStream(out, myKey, myRepo.getLibraryData().libraries.flavor);

                    system = theirRepo.getLibraryData().libraries.description;
                    logger.info("Authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            logger.error(e.getMessage());
        }
        return system;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    public boolean process(Socket aSocket) throws Exception
    {
        socket = aSocket;
        port = aSocket.getPort();
        address = aSocket.getInetAddress();
        int attempts = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        boolean tout = false;

        // Get ELS hints keys if specified
        if (cfg.getHintKeysFile().length() > 0) // v3.0.0
        {
            hintKeys = new HintKeys(cfg, context);
            hintKeys.read(cfg.getHintKeysFile());
            hints = new Hints(cfg, context, hintKeys);
            context.transfer = new Transfer(cfg, context);
        }

        // setup i/o
        aSocket.setSoTimeout(120000); // time-out so this thread does not hang server

        in = new DataInputStream(aSocket.getInputStream());
        out = new DataOutputStream(aSocket.getOutputStream());

        connected = true;

        String system = handshake();
        if (system.length() == 0)
        {
            stop = true; // just hang-up on the connection
            logger.error("Connection to " + theirRepo.getLibraryData().libraries.host + " failed handshake");
        }
        else
        {
            if (isTerminal)
            {
                response = "Enter 'help' for information\r\n"; // "Enter " checked in ClientStty.checkBannerCommands()
            }
            else // is automation
            {
                response = "CMD";

                //  -S Subscriber collection file
                if (cfg.isForceCollection())
                {
                    response = response + ":RequestCollection";
                }

                //  -t Subscriber targets
                if (cfg.isForceTargets())
                {
                    response = response + ":RequestTargets";
                }
            }
        }

        // prompt for & process interactive commands
        while (stop == false)
        {
            try
            {
                // prompt the user for a command
                if (!tout)
                {
                    Utils.writeStream(out, myKey, response + (isTerminal ? prompt : ""));
                }
                tout = false;
                response = "";

                line = Utils.readStream(in, myKey);
                if (line == null)
                {
                    logger.info("EOF line. Process ended prematurely.");
                    fault = true;
                    stop = true;
                    break; // exit on EOF
                }

                if (line.trim().length() < 1)
                {
                    response = "\r";
                    continue;
                }

                logger.info("Processing command: " + line + " from: " + system + ", " + Utils.formatAddresses(getSocket()));

                // parse the command
                StringTokenizer t = new StringTokenizer(line, "\"");
                if (!t.hasMoreTokens())
                    continue; // ignore if empty

                String theCommand = t.nextToken().trim();

                // -------------- authorized level password -----------------
                if (theCommand.equalsIgnoreCase("auth"))
                {
                    ++attempts;
                    String pw = "";
                    if (t.hasMoreTokens())
                        pw = t.nextToken(); // get the password
                    if (cfg.getAuthorizedPassword().equals(pw.trim()))
                    {
                        response = "password accepted\r\n";
                        authorized = true;
                        prompt = "$ ";
                        logger.info("Command auth accepted");
                    }
                    else
                    {
                        logger.warn("Auth password attempt failed using: " + pw);
                        if (attempts >= 3) // disconnect on too many attempts
                        {
                            logger.error("Too many failures, disconnecting");
                            break;
                        }
                    }
                    continue;
                }

                // -------------- cleanup -----------------------------------
                if (theCommand.equalsIgnoreCase("cleanup"))
                {
                    if (hints != null)
                    {
                        context.hintMode = true;
                        hints.hintsSubscriberCleanup();
                        context.hintMode = false;
                        response = "true";
                    }
                    continue;
                }

                // -------------- return collection file --------------------
                if (theCommand.equalsIgnoreCase("collection"))
                {
                    try
                    {
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                        LocalDateTime now = LocalDateTime.now();
                        String stamp = dtf.format(now);

                        String location = myRepo.getJsonFilename() + "_collection-generated-" + stamp + ".json";
                        cfg.setExportCollectionFilename(location);

                        for (Library subLib : myRepo.getLibraryData().libraries.bibliography)
                        {
                            if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name)) &&
                                    (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(subLib.name))) // v3.0.0
                            {
                                if (subLib.items != null)
                                {
                                    subLib.items = null; // clear any existing data
                                }
                                myRepo.scan(subLib.name);
                            }
                            else
                            {
                                logger.info("Skipping subscriber library: " + subLib.name);
                                subLib.name = "ELS-SUBSCRIBER-SKIP_" + subLib.name; // v3.0.0
                            }
                        }

                        // otherwise it must be -S so do not scan
                        myRepo.exportItems();

                        response = new String(Files.readAllBytes(Paths.get(location)));
                    }
                    catch (MungeException e)
                    {
                        logger.error(e.getMessage());
                    }
                    continue;
                }

                // -------------- execute hint ------------------------------
                if (theCommand.equalsIgnoreCase("execute"))
                {
                    if (hintKeys == null)
                    {
                        response = (isTerminal ? "execute command requires a --keys file\r\n" : "false");
                    }
                    boolean valid = false;
                    if (t.hasMoreTokens())
                    {
                        String libName = getNextToken(t);
                        String itemPath = getNextToken(t);
                        String toPath = getNextToken(t);
                        if (libName.length() > 0 && itemPath.length() > 0 && toPath.length() > 0)
                        {
                            valid = true;
                            boolean sense = hints.hintRun(libName, itemPath, toPath);
                            response = (isTerminal ? "ok" + (sense ? ", executed" : "") + "\r\n" : Boolean.toString(sense));
                        }
                    }
                    if (!valid)
                    {
                        response = (isTerminal ? "execute command requires a 3 arguments, libName, itemPath, fullPath\r\n" : "false");
                    }
                    continue;
                }

                // -------------- logout ------------------------------------
                if (theCommand.equalsIgnoreCase("logout"))
                {
                    if (authorized)
                    {
                        authorized = false;
                        prompt = basePrompt;
                        continue;
                    }
                    else
                    {
                        theCommand = "quit";
                        // let the logic fall through to the 'quit' handler below
                    }
                }

                // -------------- quit, bye, exit ---------------------------
                if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("bye") || theCommand.equalsIgnoreCase("exit"))
                {
                    Utils.writeStream(out, myKey, "End-Execution");
                    stop = true;
                    break; // break the loop
                }

                // -------------- available disk space ----------------------
                if (theCommand.equalsIgnoreCase("space"))
                {
                    String location = "";
                    if (t.hasMoreTokens())
                    {
                        location = t.nextToken();
                        long space = Utils.availableSpace(location);
                        logger.info("  space: " + Utils.formatLong(space, true) + " at " + location);
                        if (isTerminal)
                        {
                            response = Utils.formatLong(space, true);
                        }
                        else
                        {
                            response = String.valueOf(space);
                        }
                    }
                    else
                    {
                        response = (isTerminal ? "space command requires a location\r\n" : "0");
                    }
                    continue;
                }

                // -------------- status information ------------------------
                if (theCommand.equalsIgnoreCase("status"))
                {
                    if (!authorized)
                    {
                        response = "not authorized\r\n";
                    }
                    else
                    {
                        response = ServeStty.getInstance().dumpStatistics();
                        response += dumpStatistics();
                    }
                    continue;
                }

                // -------------- return targets file -----------------------
                if (theCommand.equalsIgnoreCase("targets"))
                {
                    try
                    {
                        if (cfg.getTargetsFilename().length() > 0)
                        {
                            response = new String(Files.readAllBytes(Paths.get(cfg.getTargetsFilename())));
                        }
                        else
                        {
                            response = ""; // let it default to sources as target locations v3.0.0
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error(e.getMessage());
                    }
                    continue;
                }

                // -------------- help! -------------------------------------
                if (theCommand.equalsIgnoreCase("help") || theCommand.equals("?"))
                {
                    // @formatter:off
                    response = "\r\nAvailable commands, not case sensitive:\r\n";

                    if (authorized)
                    {
                        response += "  status = server and console status information\r\n" +
                                "\r\n" + "" +
                                " And:\r\n";
                    }

                    response += "  auth \"password\" = access Authorized commands, enclose password in quote\r\n" +
                            "  collection = get collection data from remote, can take a few moments to scan\r\n" +
                            "  space [location] = free space at location on remote\r\n" +
                            "  targets = get targets file from remote\r\n" +
                            "\r\n  help or ? = this list\r\n" +
                            "  logout = exit current level\r\n" +
                            "  quit, bye, exit = disconnect\r\n" +
                            "\r\n";
                    // @formatter:on
                    continue;
                }

                response = "\r\nunknown command '" + theCommand + "', use 'help' for information\r\n";

            } // try
            catch (Exception e)
            {
                fault = true;
                connected = false;
                stop = true;
                logger.error(Utils.getStackTrace(e));
                try
                {
                    Utils.writeStream(out, myKey, e.getMessage());
                }
                catch (Exception ex)
                {
                }
                break;
            }
        }
        return stop;
    } // process

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("Requesting stop for stty session on " + socket.getInetAddress().toString() + ":" + socket.getPort());
    } // requestStop

} // Daemon
