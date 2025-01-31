package com.corionis.els.stty.subscriber;

import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.stty.AbstractDaemon;
import com.corionis.els.stty.ServeStty;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Transfer;
import com.corionis.els.Utils;
import com.corionis.els.repository.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Subscriber Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
@SuppressWarnings("Duplicates")
public class Daemon extends AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

    private boolean isTerminal = false;
    private ServeStty instance = null;

    /**
     * Instantiate the Daemon service
     *
     * @param instance The ServeStty
     * @param context  The Context
     * @param mine My Repository
     * @param theirs Their Repository
     */
    public Daemon(ServeStty instance, Context context, Repository mine, Repository theirs)
    {
        super(context, mine, theirs);
        this.instance = instance;
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

    public void exportLibrary(String filename) throws MungeException
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File outFile = new File(Utils.getFullPathLocal(filename));
        outFile.getParentFile().mkdirs();
        logger.info("Writing library file " + filename);
        Repository repo = myRepo.cloneNoItems(); // clone with no items
        String json = gson.toJson(repo.getLibraryData());
        try
        {
            PrintWriter outputStream = new PrintWriter(outFile.getPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException("Exception while writing library file " + outFile.getPath() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

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
            logger.trace("Subscriber listener handshake");
            send("HELO", "");

            String input = receive("", 5000);
            if (input != null && (input.equals("DribNit") || input.equals("DribNlt")))
            {
                isTerminal = input.equals("DribNit");
                if (isTerminal && myRepo.getLibraryData().libraries.terminal_allowed != null &&
                        !myRepo.getLibraryData().libraries.terminal_allowed)
                {
                    send("Terminal session not allowed", "");
                    logger.warn("attempt made to login interactively but terminal sessions are not allowed");
                    return system;
                }
                send(myKey, "");

                input = receive("", 5000);
                // validate with Authentication Keys if specified
                if (context.authKeys != null)
                {
                    HintKey connectedKey = context.authKeys.findKey(input);  // look for matching key in hints keys file
                    if (connectedKey != null)
                    {
                        // send my flavor
                        send(myRepo.getLibraryData().libraries.flavor, "");

                        system = connectedKey.system;
                        logger.info("Stty server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                        context.fault = false;
                        context.timeout = false;
                    }
                }
                else if (theirRepo != null && input.equals(theirRepo.getLibraryData().libraries.key)) // otherwise validate point-to-point
                {
                    // send my flavor
                    send(myRepo.getLibraryData().libraries.flavor, "");

                    system = theirRepo.getLibraryData().libraries.description;
                    logger.info("Stty server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                    context.fault = false;
                    context.timeout = false;
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
    public int process() throws Exception
    {
        int attempts = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        boolean trace = context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? true : false;

        port = getSocket().getPort();
        address = getSocket().getInetAddress();

        // Get ELS Authentication Keys if specified
        try
        {
            if (context.cfg.getAuthKeysFile().length() > 0)
            {
                context.authKeys = new HintKeys(context);
                context.authKeys.read(context.cfg.getAuthKeysFile());
            }
        }
        catch (Exception e)
        {
            context.fault = true;
            throw e;
        }

        // setup i/o
        context.transfer = new Transfer(context);

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
                status = 1;
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
                if (context.cfg.isForceCollection())
                {
                    response = response + ":RequestCollection";
                }

                //  -t Subscriber targets
                if (context.cfg.isForceTargets())
                {
                    response = response + ":RequestTargets";
                }
            }

            // setup JSON handlers
            Gson gsonBuilder = new GsonBuilder().create();
            Gson gsonParser = new Gson();

            // prompt for & process interactive commands
            try
            {
                while (status == 0)
                {
                    try
                    {
                        // prompt the user for a command
                        if (context.fault || (context.cfg.isKeepGoing() ? context.timeout : false))
                        {
                            fault = true;
                            status = 1;
                            logger.warn("process fault, ending stty");
                            break;
                        }

                        // send response or prompt the user for a command
                        send(response + (isTerminal ? prompt : ""), trace ? "writing response " + response.length() + " bytes to " + system : "");
                        response = "";

                        // read command
                        line = receive(trace ? "Reading command" : "", -1);
                        if (line == null)
                        {
                            if (!context.cfg.isKeepGoing())
                            {
                                fault = true; // exit on EOF
                                status = 2;
                                logger.warn("EOF line. Process ended prematurely");
                            }
                            else
                                logger.info("EOF line, --listener-keep-going enabled");
                            break; // break read loop and let the connection be closed
                        }

                        line = line.trim();
                        if (line.length() < 1)
                        {
                            response = "\r";
                            continue;
                        }

                        ++commandCount;
                        logger.info("Processing command: " + line + ", from: " + system); // + ", " + Utils.formatAddresses(getSocket()));

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
                            if (context.cfg.getAuthorizedPassword().equals(pw.trim()))
                            {
                                response = "password accepted\r\n";
                                authorized = true;
                                prompt = "$ ";
                                logger.info("Command auth accepted");
                            }
                            else
                            {
                                logger.warn("auth password attempt failed using: " + pw);
                                if (attempts >= 3) // disconnect on too many attempts
                                {
                                    logger.error("Too many authentication failures, disconnecting");
                                    break;
                                }
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
                                if (context.cfg.isKeepGoing())
                                    theCommand = "bye";
                                else
                                    theCommand = "quit";
                            }
                        }

                        // -------------- bye ---------------------------------------
                        if (theCommand.equalsIgnoreCase("bye"))
                        {
                            out.flush();
                            Thread.sleep(1500);
                            break;  // let the connection close
                        }

                        // -------------- return collection file --------------------
                        if (theCommand.equalsIgnoreCase("collection"))
                        {
                            try
                            {
                                String location = Utils.scrubFilename(myRepo.getLibraryData().libraries.description).replaceAll(" ", "");
                                location = Utils.getStampedFilename(myRepo, location + "_collection-generated");
                                location = Utils.getTemporaryFilePrefix(myRepo, location) + ".json";

                                context.cfg.setExportCollectionFilename(location);

                                for (Library subLib : myRepo.getLibraryData().libraries.bibliography)
                                {
                                    if ((!context.cfg.isSpecificLibrary() || context.cfg.isSelectedLibrary(subLib.name)) &&
                                            (!context.cfg.isSpecificExclude() || !context.cfg.isExcludedLibrary(subLib.name)))
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
                                Thread.sleep(1500);
                                Path jsonPath = Paths.get(Utils.getFullPathLocal(context.cfg.getExportCollectionFilename()));
                                response = new String(Files.readAllBytes(jsonPath));
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
                                        File f = new File(Utils.getFullPathLocal(from));
                                        context.transfer.copyFile(context.clientSftp, Utils.getFullPathLocal(from),
                                                Files.getLastModifiedTime(f.toPath()), Utils.getFullPathLocal(to), false, true);
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

                        // -------------- directory ------------------------
                        if (theCommand.equalsIgnoreCase("directory"))
                        {
                            response = context.cfg.getWorkingDirectory();
                            continue;
                        }

                        // -------------- drives of system ------------------------
                        if (theCommand.equalsIgnoreCase("drives"))
                        {
                            response = Utils.getLocalHardDrives();
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
                                    Hint hint = null;
                                    if (line.length() > 8)
                                    {
                                        valid = true;
                                        String json = line.substring(9);
                                        hint = gsonParser.fromJson(json, Hint.class);
                                        ArrayList<Hint> pending = new ArrayList<>();
                                        pending.add(hint);
                                        response = context.hintsHandler.hintsMunge(pending); // process locally
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
                            if (!context.cfg.isKeepGoing())
                            {
                                fault = true;
                                status = 1;
                            }
                            if (!context.timeout)
                                send("End-Execution", trace ? "send End-Execution" : "");
                            Thread.sleep(1500);
                            throw new MungeException("Fault received from Publisher");
                        }

                        // -------------- return library file --------------------
                        if (theCommand.equalsIgnoreCase("library"))
                        {
                            try
                            {
                                String location = Utils.scrubFilename(myRepo.getLibraryData().libraries.description).replaceAll(" ", "");
                                location = Utils.getStampedFilename(myRepo, location + "_library-generated");
                                location = Utils.getTemporaryFilePrefix(myRepo, location) + ".json";

                                exportLibrary(location);
                                Thread.sleep(2500);
                                Path jsonPath = Paths.get(location).toAbsolutePath();
                                response = new String(Files.readAllBytes(jsonPath));
                            }
                            catch (MungeException e)
                            {
                                logger.error(e.getMessage());
                            }
                            continue;
                        }

                        // -------------- quit, exit --------------------------------
                        if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("exit"))
                        {
                            out.flush();
                            Thread.sleep(1500);

                            // if keep going is not enabled then stop
                            if (context.cfg.isKeepGoing())
                                logger.info("Ignoring quit command, --listener-keep-going enabled");
                            else
                                status = 1;
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
                                        File f = new File(Utils.getFullPathLocal(from));
                                        context.transfer.moveFile(Utils.getFullPathLocal(from), Files.getLastModifiedTime(f.toPath()),
                                                Utils.getFullPathLocal(to), true);
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
                                    response = Utils.readString(Utils.getFullPathLocal(filename));
                                }
                            }
                            if (!valid)
                            {
                                response = (isTerminal ? "read command requires a 1 argument, filename\r\n" : "false");
                            }
                            continue;
                        }

                        // -------------- remove ----------------------
                        if (theCommand.equalsIgnoreCase("remove"))
                        {
                            String location = "";
                            if (t.hasMoreTokens())
                            {
                                location = t.nextToken();
                                context.transfer.remove(location, false);
                                logger.info("  deleted: " + Utils.getFullPathLocal(location));
                                response = "true";
                            }
                            else
                            {
                                response = "false";
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
                                long space = Utils.availableSpace(Utils.getFullPathLocal(location));
                                logger.info("  space: " + Utils.formatLong(space, true, context.cfg.getLongScale()) + " at " + location);
                                if (isTerminal)
                                {
                                    response = Utils.formatLong(space, true, context.cfg.getLongScale());
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
                                response = instance.dumpStatistics();
                                response += dumpStatistics();
                            }
                            continue;
                        }

                        // -------------- stop --------------------------------
                        if (theCommand.equalsIgnoreCase("stop"))
                        {
                            send("End-Execution", trace ? "send End-Execution" : "");
                            Thread.sleep(1500);
                            status = 2;
                            break; // break the loop
                        }

                        // -------------- return targets file -----------------------
                        if (theCommand.equalsIgnoreCase("targets"))
                        {
                            try
                            {
                                if (context.cfg.getTargetsFilename().length() > 0)
                                {
                                    response = new String(Files.readAllBytes(Paths.get(Utils.getFullPathLocal(context.cfg.getTargetsFilename()))));
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
                                    "  library = get library data from remote\r\n" +
                                    "  space \"[location]\" = free space at location on remote\r\n" +
                                    "  targets = get targets file from remote\r\n" +
                                    "\r\n  help or ? = this list\r\n" +
                                    "  bye = disconnect and leave remote end running\r\n" +
                                    "  copy \"[from]\" \"[to]\" = copy file from to\r\n" +
                                    "  directory = get working directory\r\n" +
                                    "  drives = get available drives\r\n" +
                                    "  logout = exit current level\r\n" +
                                    "  move \"[from]\" \"[to]\" = move file from to\r\n" +
                                    "  quit, exit = disconnect and quit remote end\r\n" +
                                    "  remove \"[from]\" = remove file from\r\n" +
                                    "  stop = force stop of remote and end\r\n" +
                                    "\r\n";
                            // @formatter:on
                            continue;
                        }

                        response = "\r\nunknown command '" + theCommand + "', use 'help' for information\r\n";
                    }
                    catch (SocketTimeoutException toe)
                    {
                        context.timeout = true;
                        connected = false;
                        logger.error("SocketTimeoutException: " + Utils.getStackTrace(toe));
                        if (!context.cfg.isKeepGoing())
                        {
                            fault = true;
                            status = 1;
                        }
                        else
                            logger.info("Ignoring exception, --listener-keep-going enabled");
                        break;
                    }
                    catch (SocketException se)
                    {
                        if (se.toString().contains("timed out"))
                            context.timeout = true;
                        connected = false;
                        logger.debug("SocketException, timeout is: " + context.timeout + ", " + se.getMessage());
                        if (!context.cfg.isKeepGoing())
                        {
                            fault = true;
                            status = 1;
                        }
                        else
                            logger.info("Ignoring exception, --listener-keep-going enabled");
                        break;
                    }
                    catch (Exception e)
                    {
                        //connected = false;
                        logger.error("Subscriber Listener exception");
                        logger.error(Utils.getStackTrace(e));
                        try
                        {
                            if (!context.timeout)
                            {
                                send(e.getMessage(), null);
                                Thread.sleep(1500);
                            }
                        }
                        catch (Exception ex)
                        {
                        }
                        if (!context.cfg.isKeepGoing())
                        {
                            fault = true;
                            status = 1;
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
        if (fault)
            context.fault = true;
        String statMsg = status == 0 ? "Success" : (status == 1 ? "Quit" : "Stop");
        logger.trace("Subscriber session done, status = " + statMsg + ", fault = " + context.fault);
        return status;
    } // process

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.status = 1;
        logger.debug("requesting quit for stty session on " + socket.getInetAddress().toString() + ":" + socket.getPort());
    } // requestStop

} // Daemon
