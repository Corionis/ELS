package com.groksoft.els.stty.subscriber;

import com.groksoft.els.*;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.stty.ServeStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
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
public class Daemon extends com.groksoft.els.stty.AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

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
        super(config, ctxt, mine, theirs);
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
     * Get the next available token trimmed
     *
     * @param t StringTokenizer
     * @return Next token or an empty string
     */
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

    /**
     * Perform a handshake
     * <br/>
     * If a Hint Keys file is specified (-k|-K) those keys are used for authentication.
     * Otherwise the previous point-to-point authentication is used.
     *
     * @return String name of back-up system
     */
    public String handshake()
    {
        String system = "";
        try
        {
            logger.trace("handshake");
            send("HELO", "");

            String input = receive("", 1000);
            if (input != null && (input.equals("DribNit") || input.equals("DribNlt")))
            {
                isTerminal = input.equals("DribNit");
                if (isTerminal && myRepo.getLibraryData().libraries.terminal_allowed != null &&
                        !myRepo.getLibraryData().libraries.terminal_allowed)
                {
                    send("Terminal session not allowed", "");
                    logger.warn("Attempt made to login interactively but terminal sessions are not allowed");
                    return system;
                }
                send(myKey, "");

                input = receive("", 1000);
                // validate with Authorization Keys if specified
                if (context.authKeys != null)
                {
                    HintKeys.HintKey connectedKey = context.authKeys.findKey(input);  // look for matching key in hints keys file
                    if (connectedKey != null)
                    {
                        // send my flavor
                        send(myRepo.getLibraryData().libraries.flavor, "");

                        system = connectedKey.name;
                        logger.info("Stty server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                    }
                } else if (input.equals(theirRepo.getLibraryData().libraries.key)) // otherwise validate point-to-point
                {
                    // send my flavor
                    send(myRepo.getLibraryData().libraries.flavor, "");

                    system = theirRepo.getLibraryData().libraries.description;
                    logger.info("Stty server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            logger.error(Utils.getStackTrace(e));
        }
        return system;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    public boolean process() throws Exception
    {
        int attempts = 0;
        int commandCount = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        boolean trace = cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? true : false;

        port = getSocket().getPort();
        address = getSocket().getInetAddress();

        // Get ELS Authorization Keys if specified
        try
        {
            if (cfg.getAuthKeysFile().length() > 0)
            {
                context.authKeys = new HintKeys(cfg, context);
                context.authKeys.read(cfg.getAuthKeysFile());
            }

            // Get ELS Hints Keys if specified
            if (cfg.getHintKeysFile().length() > 0)
            {
                context.hintKeys = new HintKeys(cfg, context);
                context.hintKeys.read(cfg.getHintKeysFile());
                hints = new Hints(cfg, context, context.hintKeys);
            }
        }
        catch (Exception e)
        {
            context.fault = true;
            throw e;
        }

        // setup i/o
        context.transfer = new Transfer(cfg, context);

        getSocket().setKeepAlive(true); // keep alive to avoid time-out
        getSocket().setSoTimeout(myRepo.getLibraryData().libraries.timeout * 60 * 1000); // read time-out
        getSocket().setSoLinger(true, 10000); // time-out to linger after transmission completed, 10 seconds

        in = new DataInputStream(getSocket().getInputStream());
        out = new DataOutputStream(getSocket().getOutputStream());

        connected = true;

        String system = handshake();
        if (system.length() == 0)
        {
            if (!context.cfg.isKeepGoing())
                stop = true; // stop this daemon to avoid repeated attacks
            logger.error("Connection to " + Utils.formatAddresses(socket) + " failed handshake");
        }
        else
        {
            if (isTerminal)
            {
                response = "Enter 'help' for information\r\n"; // "Enter " checked with ClientStty.checkBannerCommands()
            }
            else // is automation
            {
                createHeartBeat();

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

            // prompt for & process interactive commands
            try
            {
                while (stop == false)
                {
                    try
                    {
                        // prompt the user for a command
                        if (context.fault || context.timeout)
                        {
                            fault = true;
                            stop = true;
                            logger.warn("Process fault, ending stty");
                            break;
                        }

                        // send response or prompt the user for a command
                        send(response + (isTerminal ? prompt : ""), trace ? "Writing response " + response.length() + " bytes" : "");
                        response = "";

                        // read command
                        line = receive(trace ? "Reading command" : "", -1);
                        if (line == null)
                        {
                            if (!cfg.isKeepGoing())
                            {
                                fault = true; // exit on EOF
                                stop = true;
                                logger.warn("EOF line. Process ended prematurely");
                            }
                            else
                                logger.info("EOF line. --listener-keep-going enabled");
                            break; // break read loop and let the connection be closed
                        }

                        if (line.trim().length() < 1)
                        {
                            response = "\r";
                            continue;
                        }

                        ++commandCount;
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
                                    logger.error("Too many authentication failures, disconnecting");
                                    break;
                                }
                            }
                            continue;
                        }

                        // -------------- bye ---------------------------------------
                        if (theCommand.equalsIgnoreCase("bye"))
                        {
                            break;  // let the connection close
                        }

                        // -------------- cleanup -----------------------------------
                        if (theCommand.equalsIgnoreCase("cleanup"))
                        {
                            if (hints != null)
                            {
                                context.localMode = true;
                                hints.hintsSubscriberCleanup();
                                context.localMode = false;
                                response = "true";
                            }
                            continue;
                        }

                        // -------------- return collection file --------------------
                        if (theCommand.equalsIgnoreCase("collection"))
                        {
                            try
                            {
                                String stamp = "";
                                if (myRepo.getLibraryData().libraries.temp_dated != null && myRepo.getLibraryData().libraries.temp_dated)
                                {
                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                                    LocalDateTime now = LocalDateTime.now();
                                    stamp = "-" + dtf.format(now);
                                }

                                String location;
                                String path = "";
                                String fn = Utils.scrubFilename(myRepo.getLibraryData().libraries.description).replaceAll(" ", "");
                                if (myRepo.getLibraryData().libraries.temp_location != null && myRepo.getLibraryData().libraries.temp_location.length() > 0)
                                {
                                    path = myRepo.getLibraryData().libraries.temp_location;
                                    String sep = myRepo.getSeparator();
                                    if (!path.endsWith(sep))
                                        path += sep;
                                    location = path + fn;
                                }
                                else
                                    location = fn;
                                location += "_collection-generated" + stamp + ".json";
                                cfg.setExportCollectionFilename(location);

                                for (Library subLib : myRepo.getLibraryData().libraries.bibliography)
                                {
                                    if ((!cfg.isSpecificLibrary() || cfg.isSelectedLibrary(subLib.name)) &&
                                            (!cfg.isSpecificExclude() || !cfg.isExcludedLibrary(subLib.name)))
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
                                        subLib.name = "ELS-SUBSCRIBER-SKIP_" + subLib.name;
                                    }
                                }

                                // otherwise it must be -S so do not scan
                                myRepo.exportItems(true);

                                response = new String(Files.readAllBytes(Paths.get(location)));
                            }
                            catch (MungeException e)
                            {
                                logger.error(e.getMessage());
                            }
                            continue;
                        }

                        // -------------- copy ------------------------------
                        if (theCommand.equalsIgnoreCase("copy"))
                        {
                            boolean valid = false;
                            if (t.hasMoreTokens())
                            {
                                String from = getNextToken(t);
                                String to = getNextToken(t);
                                if (from.length() > 0 && to.length() > 0)
                                {
                                    boolean success = true;
                                    try
                                    {
                                        valid = true;
                                        File f = new File(from);
                                        context.transfer.copyFile(from, Files.getLastModifiedTime(f.toPath(), LinkOption.NOFOLLOW_LINKS), to, false, true);
                                    }
                                    catch (Exception e)
                                    {
                                        success = false;
                                        logger.error(Utils.getStackTrace(e));
                                    }
                                    response = (isTerminal ? "ok" + (success ? ", copied" : "") + "\r\n" : Boolean.toString(success));
                                }
                            }
                            if (!valid)
                            {
                                response = (isTerminal ? "copy command requires a 2 arguments, from and to\r\n" : "false");
                            }
                            continue;
                        }

                        // -------------- execute hint ------------------------------
                        if (theCommand.equalsIgnoreCase("execute"))
                        {
                            if (context.hintKeys == null)
                            {
                                response = (isTerminal ? "execute command requires a --keys file\r\n" : "false");
                                logger.warn("execute command received with no --keys file specified");
                            }
                            else
                            {
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
                            }
                            continue;
                        }

                        // -------------- fault ----------------------------------
                        if (theCommand.equalsIgnoreCase("fault"))
                        {
                            fault = true;
                            stop = true;
                            if (!context.timeout)
                                send("End-Execution", trace ? "Write End-Execution" : "");
                            Thread.sleep(3000);
                        }

                        // -------------- hint -----------------------------------
                        if (theCommand.equalsIgnoreCase("hint"))
                        {
                            if (context.hintKeys == null)
                            {
                                response = (isTerminal ? "hint command requires a --keys file\r\n" : "false");
                                logger.warn("hint command received with no --keys file specified");
                            }
                            else
                            {
                                boolean valid = false;
                                if (t.hasMoreTokens())
                                {
                                    String filename = getNextToken(t);
                                    String command = getNextToken(t).trim();
                                    command += " \"" + getNextToken(t) + "\""; // arg1
                                    if (t.hasMoreTokens())
                                    {
                                        String arg = getNextToken(t);
                                        if (!arg.equals("\n"))
                                            command += " \"" + arg + "\""; // possible arg2
                                    }
                                    if (filename.length() > 0 && command.length() > 0)
                                    {
                                        valid = true;
                                        context.localMode = true;
                                        response = context.transfer.writeUpdateHint(filename, command);
                                        context.localMode = false;
                                    }
                                }
                                if (!valid)
                                {
                                    response = (isTerminal ? "hint command requires a 2 arguments, filename and command\r\n" : "false");
                                }
                            }
                            continue;
                        }

                        // -------------- return library file --------------------
                        if (theCommand.equalsIgnoreCase("library"))
                        {
                            try
                            {
                                String stamp = "";
                                if (myRepo.getLibraryData().libraries.temp_dated != null && myRepo.getLibraryData().libraries.temp_dated)
                                {
                                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                                    LocalDateTime now = LocalDateTime.now();
                                    stamp = "-" + dtf.format(now);
                                }

                                String location;
                                String path = "";
                                String fn = Utils.scrubFilename(myRepo.getLibraryData().libraries.description).replaceAll(" ", "");
                                if (myRepo.getLibraryData().libraries.temp_location != null && myRepo.getLibraryData().libraries.temp_location.length() > 0)
                                {
                                    path = myRepo.getLibraryData().libraries.temp_location;
                                    String sep = myRepo.getSeparator();
                                    if (!path.endsWith(sep))
                                        path += sep;
                                    location = path + fn;
                                }
                                else
                                    location = fn;
                                location += "_library-generated" + stamp + ".json";
                                cfg.setExportCollectionFilename(location);

                                // do not scan
                                myRepo.exportItems(false);

                                response = new String(Files.readAllBytes(Paths.get(location)));
                            }
                            catch (MungeException e)
                            {
                                logger.error(e.getMessage());
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

                        // -------------- quit, exit --------------------------------
                        if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("exit"))
                        {
                            send("End-Execution", trace ? "Write End-Execution" : "");
                            Thread.sleep(1000);

                            // if this is the first command or keep going is not enabled then stop
                            if (commandCount == 1 || !cfg.isKeepGoing())
                                stop = true;
                            else
                                logger.info("Ignoring quit command, --listener-keep-going enabled");
                            break; // break the loop
                        }

                        // -------------- move ------------------------------
                        if (theCommand.equalsIgnoreCase("move"))
                        {
                            boolean valid = false;
                            if (t.hasMoreTokens())
                            {
                                String from = getNextToken(t);
                                String to = getNextToken(t);
                                if (from.length() > 0 && to.length() > 0)
                                {
                                    boolean success = true;
                                    try
                                    {
                                        valid = true;
                                        File f = new File(from);
                                        context.transfer.moveFile(from, Files.getLastModifiedTime(f.toPath(), LinkOption.NOFOLLOW_LINKS), to, true);
                                    }
                                    catch (Exception e)
                                    {
                                        success = false;
                                        logger.error(Utils.getStackTrace(e));
                                    }
                                    response = (isTerminal ? "ok" + (success ? ", moved" : "") + "\r\n" : Boolean.toString(success));
                                }
                            }
                            if (!valid)
                            {
                                response = (isTerminal ? "move command requires a 2 arguments, from and to\r\n" : "false");
                            }
                            continue;
                        }

                        // -------------- read -----------------------------------
                        if (theCommand.equalsIgnoreCase("read"))
                        {
                            boolean valid = false;
                            if (t.hasMoreTokens())
                            {
                                String filename = getNextToken(t);
                                if (filename.length() > 0)
                                {
                                    valid = true;
                                    response = Utils.readString(filename);
                                }
                            }
                            if (!valid)
                            {
                                response = (isTerminal ? "read command requires a 1 argument, filename\r\n" : "false");
                            }
                            continue;
                        }

                        // -------------- space check ----------------------
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
                                    response = ""; // let it default to sources as target locations
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

                            response += "  auth \"password\" = access Authorized commands, enclose password with quotes\r\n" +
                                    "  collection = get collection data from remote, can take a few moments to scan\r\n" +
                                    "  space \"[location]\" = free space at location on remote\r\n" +
                                    "  targets = get targets file from remote\r\n" +
                                    "\r\n  help or ? = this list\r\n" +
                                    "  logout = exit current level\r\n" +
                                    "  bye = disconnect and leave remote end running\r\n" +
                                    "  quit, exit = disconnect and quit remote end\r\n" +
                                    "\r\n";
                            // @formatter:on
                            continue;
                        }

                        response = "\r\nunknown command '" + theCommand + "', use 'help' for information\r\n";

                    }
                    catch (SocketTimeoutException toe)
                    {
                        context.timeout = true;
                        fault = true;
                        connected = false;
                        stop = true;
                        logger.error("SocketTimeoutException: " + Utils.getStackTrace(toe));
                        break;
                    }
                    catch (SocketException se)
                    {
                        if (se.toString().contains("timed out"))
                            context.timeout = true;
                        fault = true;
                        connected = false;
                        stop = true;
                        logger.debug("SocketException, timeout is: " + context.timeout);
                        logger.error(Utils.getStackTrace(se));
                        break;
                    }
                    catch (Exception e)
                    {
                        fault = true;
                        connected = false;
                        stop = true;
                        logger.error(Utils.getStackTrace(e));
                        try
                        {
                            if (!context.timeout)
                            {
                                send(e.getMessage(), "Subscriber exception");
                                Thread.sleep(1000);
                            }
                        }
                        catch (Exception ex)
                        {
                        }
                        break;
                    }
                }
            }
            finally
            {
                stopHeartBeat();
            }
        }
        logger.trace("subscriber session done, stop = " + stop);
        if (fault)
            context.fault = true;
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
