package com.groksoft.els.stty.publisher;

import com.groksoft.els.*;
import com.groksoft.els.repository.*;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Publisher Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
@SuppressWarnings("Duplicates")
public class Daemon extends com.groksoft.els.stty.AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

    private boolean fault = false;
    private boolean isTerminal = false;
    private Transfer transfer;

    /**
     * Instantiate the Daemon service
     *
     * @param context  The Context
     */
    public Daemon(Context context, Repository mine, Repository theirs)
    {
        super(context, mine, theirs);
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

                        system = connectedKey.name;
                        logger.info("Stty server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                        context.fault = false;
                        context.timeout = false;
                    }
                } else if (input.equals(theirRepo.getLibraryData().libraries.key)) // otherwise validate point-to-point
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
            logger.error(e.getMessage());
        }
        return system;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    public boolean process() throws Exception, IOException
    {
        int attempts = 0;
        int commandCount = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        long size;
        boolean trace = context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? true : false;

        port = getSocket().getPort();
        address = getSocket().getInetAddress();

        // for get command
        long totalSize = 0L;
        ArrayList<Item> group = new ArrayList<>();

        // Get ELS hints keys & Tracker if specified
        if (context.cfg.getHintKeysFile().length() > 0)
        {
            context.hintKeys = new HintKeys(context);
            context.hintKeys.read(context.cfg.getHintKeysFile());
            context.hints = new Hints(context, context.hintKeys);
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
                            logger.warn("process fault, ending stty");
                            break;
                        }
                        // send response or prompt the user for a command
                        send(response + (isTerminal ? prompt : ""), trace ? "writing response " + response.length() + " bytes to " + system : "");
                        response = "";

                        line = receive(trace ? "Reading command" : "", -1);
                        if (line == null)
                        {
                            if (!context.cfg.isKeepGoing())
                            {
                                fault = true; // exit on EOF
                                stop = true;
                                logger.warn("EOF line. Process ended prematurely");
                            }
                            else
                                logger.warn("EOF line. --listener-keep-going enabled");
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

                        // -------------- bye ---------------------------------------
                        if (theCommand.equalsIgnoreCase("bye"))
                        {
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
                                        logger.info("Skipping publisher library: " + subLib.name);
                                        subLib.name = "ELS-SUBSCRIBER-SKIP_" + subLib.name;
                                    }
                                }

                                // otherwise it must be -S so do not scan
                                myRepo.exportItems(true);
                                Thread.sleep(3000);
                                Path jsonPath = Paths.get(context.cfg.getExportCollectionFilename()).toAbsolutePath();
                                response = new String(Files.readAllBytes(jsonPath));
                            }
                            catch (MungeException e)
                            {
                                logger.error(e.getMessage());
                            }
                            continue;
                        }

                        // -------------- fault ----------------------------------
                        if (theCommand.equalsIgnoreCase("fault"))
                        {
                            fault = true;
                            stop = true;
                            if (!context.timeout)
                                send("End-Execution", trace ? "send End-Execution" : "");
                            Thread.sleep(3000);
                        }

                        // -------------- find --------------------------------------
                        if (theCommand.equalsIgnoreCase("find"))
                        {
                            if (!authorized)
                            {
                                response = "not authorized\r\n";
                            }
                            else
                            {
                                if (t.hasMoreTokens())
                                {
                                    String find = remainingTokens(t);
                                    find = find.toLowerCase();
                                    logger.info("find: " + find);
                                    for (Library subLib : myRepo.getLibraryData().libraries.bibliography)
                                    {
                                        boolean titled = false;
                                        if (subLib.items == null)
                                        {
                                            myRepo.scan(subLib.name);
                                        }
                                        for (Item item : subLib.items)
                                        {
                                            if (item.getItemPath().toLowerCase().contains(find))
                                            {
                                                if (!titled)
                                                {
                                                    response += "  In library: " + subLib.name + "\r\n";
                                                    titled = true;
                                                }
                                                response += "    " + item.getItemPath() + "\r\n";
                                            }
                                        }
                                    }
                                }
                                if (response.length() < 1)
                                {
                                    response = "No results found, try collection command if refresh is needed\r\n";
                                }
                            }
                            continue;
                        }

                        // -------------- get ---------------------------------------
                        if (theCommand.equalsIgnoreCase("get"))
                        {
                            if (!authorized)
                            {
                                response = "not authorized\r\n";
                            }
                            else
                            {
                                boolean found = false;
                                if (t.hasMoreTokens())
                                {
                                    String find = remainingTokens(t);
                                    find = find.toLowerCase();
                                    logger.info("get: " + find);
                                    for (Library subLib : myRepo.getLibraryData().libraries.bibliography)
                                    {
                                        boolean titled = false;
                                        if (subLib.items == null)
                                        {
                                            myRepo.scan(subLib.name);
                                        }
                                        for (Item item : subLib.items)
                                        {
                                            if (myRepo.ignore(item))
                                            {
                                                response += "  ! Ignoring '" + item.getItemPath() + "'\r\n";
                                                continue;
                                            }
                                            if (item.getItemPath().toLowerCase().contains(find))
                                            {
                                                if (!item.isDirectory())
                                                {
                                                    if (!titled)
                                                    {
                                                        response += "  In library: " + subLib.name + "\r\n";
                                                        titled = true;
                                                    }
                                                    response += "    " + item.getItemPath() + "\r\n";
                                                    if (item.getSize() < 0)
                                                    {
                                                        logger.warn("file size was < 0 during get command, getting");
                                                        size = Files.size(Paths.get(item.getFullPath()));
                                                        item.setSize(size);
                                                        totalSize += size;
                                                    }
                                                    else
                                                    {
                                                        totalSize += item.getSize();
                                                    }
                                                    group.add(item);
                                                    found = true;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (!found)
                                {
                                    response += "No results found, try collection command if refresh is needed\r\n";
                                }
                                else
                                {
                                    response += "  Total size: ";
                                    response += Utils.formatLong(totalSize, true, context.cfg.getLongScale()) + "\r\n";
                                    response += "Copy listed items (y/N)? ";
                                    send(response, "");

                                    line = receive("", -1);
                                    if (line == null)
                                    {
                                        logger.info("EOF line");
                                        stop = true;
                                        break; // exit on EOF
                                    }

                                    if (line.equalsIgnoreCase("Y"))
                                    {
                                        if (context.clientStty == null)
                                        {
                                            // start the serveSftp client
                                            context.clientSftp = new ClientSftp(context, myRepo, theirRepo, false);
                                            if (!context.clientSftp.startClient())
                                            {
                                                throw new MungeException("Publisher sftp client failed to connect");
                                            }

                                            // start the serveStty client for automation
                                            context.clientStty = new ClientStty(context, false, false);
                                            if (!context.clientStty.connect(myRepo, theirRepo))
                                            {
                                                throw new MungeException("Publisher stty client failed to connect");
                                            }
                                            if (context.clientStty.checkBannerCommands())
                                            {
                                                logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
                                            }
                                        }
                                        response = transfer.copyGroup(group, totalSize, true);
                                        group.clear();
                                    }
                                    else
                                    {
                                        response = "skipping get of items\r\n";
                                    }
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
                                theCommand = "quit";
                                // let the logic fall through to the 'quit' handler below
                            }
                        }

                        // -------------- quit, exit --------------------------------
                        if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("exit"))
                        {
                            send("End-Execution", trace ? "send End-Execution" : "");
                            Thread.sleep(3000);

                            // if this is the first command or keep going is not enabled then stop
                            if (commandCount == 1 || !context.cfg.isKeepGoing())
                                stop = true;
                            else
                                logger.info("Ignoring quit command, --listener-keep-going enabled");
                            break; // break the loop
                        }

                        // -------------- space check ----------------------
                        if (theCommand.equalsIgnoreCase("space"))
                        {
                            String location = "";
                            if (t.hasMoreTokens())
                            {
                                location = t.nextToken();
                                long space = Utils.availableSpace(location);
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
                                if (context.cfg.getTargetsFilename().length() > 0)
                                {
                                    response = new String(Files.readAllBytes(Paths.get(context.cfg.getTargetsFilename())));
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
                        response += "  find \"[text]\" = search collection for all matching text, use collection command to refresh\r\n" +
                                "  get \"[text]\" = like find but offers the option to get/copy the listed items in overwrite mode\r\n" +
                                "  status = server and console status information\r\n" +
                                "\r\n" + "" +
                                " And:\r\n";
                    }

                    response += "  auth \"password\" = access Authorized commands, enclose password in quotes\r\n" +
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

                    } // try
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
                                send(e.getMessage(), "Publisher exception");
                                Thread.sleep(3000);
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
        if (fault)
            context.fault = true;
        return stop;
    } // process

    /**
     * Collect the remaining tokens into a String
     *
     * @param t StringTokenizer
     * @return String of concatenated tokens
     */
    public String remainingTokens(StringTokenizer t)
    {
        String result = "";
        while (t.hasMoreTokens())
        {
            result += t.nextToken() + " ";
        }
        return result.trim();
    }

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("requesting stop for stty session on: " + socket.getInetAddress().toString() + ":" + socket.getPort());
    } // requestStop

} // Daemon
