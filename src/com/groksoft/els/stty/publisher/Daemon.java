package com.groksoft.els.stty.publisher;

import com.groksoft.els.*;
import com.groksoft.els.repository.*;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.DaemonBase;
import com.groksoft.els.stty.ServeStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Publisher Daemon service.
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
    private Transfer transfer;

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

    /**
     * Perform a point-to-point handshake
     *
     * @return String name of back-up system
     */
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
                    logger.warn("Attempt made to login interactively but terminal sessions are not allowed");
                    return system;
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
    public boolean process(Socket aSocket) throws Exception, IOException
    {
        socket = aSocket;
        port = aSocket.getPort();
        address = aSocket.getInetAddress();
        int attempts = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        long size;
        boolean tout = false;

        // for get command
        long totalSize = 0L;
        ArrayList<Item> group = new ArrayList<>();

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
                        pw = remainingTokens(t);
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
                                logger.info("Skipping publisher library: " + subLib.name);
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
                                                logger.warn("File size was < 0 during get command, getting");
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
                            response += Utils.formatLong(totalSize, true) + "\r\n";
                            response += "Copy listed items (y/N)? ";
                            Utils.writeStream(out, myKey, response);

                            line = Utils.readStream(in, myKey);
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
                                    context.clientSftp = new ClientSftp(myRepo, theirRepo, false);
                                    if (!context.clientSftp.startClient())
                                    {
                                        throw new MungeException("Publisher sftp client failed to connect");
                                    }

                                    // start the serveStty client for automation
                                    context.clientStty = new ClientStty(cfg, false, false);
                                    if (!context.clientStty.connect(myRepo, theirRepo))
                                    {
                                        throw new MungeException("Publisher stty client failed to connect");
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
                        response += "  find [text] = search collection for all matching text, use collection command to refresh\r\n" +
                                "  get [text] = like find but offers the option to get/copy the listed items in overwrite mode\r\n" +
                                "  status = server and console status information\r\n" +
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
        logger.debug("Requesting stop for stty session on: " + socket.getInetAddress().toString() + ":" + socket.getPort());
    } // requestStop

} // Daemon
