package com.groksoft.els.stty.publisher;

import com.groksoft.els.*;
import com.groksoft.els.Process;
import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
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

    private Main.Context context;
    private boolean fault = false;
    private boolean isTerminal = false;
    private Process process; // munge process for get command

    /**
     * Instantiate the Daemon service
     *
     * @param config
     * @param ctxt
     */
    public Daemon(Configuration config, Main.Context ctxt, Repository mine, Repository theirs)
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

    public boolean handshake()
    {
        boolean valid = false;
        try
        {
            Utils.write(out, myKey, "HELO");

            String input = Utils.read(in, myKey);
            if (input.equals("DribNit") || input.equals("DribNlt"))
            {
                isTerminal = input.equals("DribNit");
                Utils.write(out, myKey, myKey);

                input = Utils.read(in, myKey);
                if (input.equals(theirKey))
                {
                    // send my flavor
                    Utils.write(out, myKey, myRepo.getLibraryData().libraries.flavor);

                    logger.info("Authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + theirRepo.getLibraryData().libraries.description);
                    valid = true;
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            logger.error(e.getMessage());
        }
        return valid;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    public void process(Socket aSocket) throws IOException
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
        process = new Process(cfg, context); // munge process for get command

        // setup i/o
        aSocket.setSoTimeout(120000); // time-out so this thread does not hang server

        in = new DataInputStream(aSocket.getInputStream());
        out = new DataOutputStream(aSocket.getOutputStream());

        connected = true;

        if (!handshake())
        {
            stop = true; // just hang-up on the connection
            logger.info("Connection to " + theirRepo.getLibraryData().libraries.host + " failed handshake");
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
                    Utils.write(out, myKey, response + (isTerminal ? prompt : ""));
                }
                tout = false;
                response = "";

                line = Utils.read(in, myKey);
                if (line == null)
                {
                    logger.info("EOF line");
                    stop = true;
                    break; // exit on EOF
                }

                if (line.trim().length() < 1)
                {
                    response = "\r";
                    continue;
                }

                logger.info("Processing command: " + line);

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
                            myRepo.scan(subLib.name);
                        }

                        // otherwise it must be -S so do not scan
                        myRepo.exportItems();

                        response = new String(Files.readAllBytes(Paths.get(location)));
                    }
                    catch (MungerException e)
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
                            Utils.write(out, myKey, response);

                            line = Utils.read(in, myKey);
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
                                        throw new MungerException("Publisher sftp client failed to connect");
                                    }

                                    // start the serveStty client for automation
                                    context.clientStty = new ClientStty(cfg, false, false);
                                    if (!context.clientStty.connect(myRepo, theirRepo))
                                    {
                                        throw new MungerException("Publisher stty client failed to connect");
                                    }
                                }
                                response = process.copyGroup(group, totalSize, true);
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
                    Utils.write(out, myKey, "End-Execution");
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
                            response = ""; // let it default to sources as target locations v3.00
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

                    response += "  auth [password] = access Authorized commands\r\n" +
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
                    Utils.write(out, myKey, e.getMessage());
                }
                catch (Exception ex) {}
                break;
            }
        } // while

        if (stop)
        {
            // all done, close everything
            if (logger != null)
            {
                logger.info("Close connection on port " + port + " to " + address.getHostAddress());

                // mark the process as successful so it may be detected with automation
                if (!fault)
                    logger.fatal("Process completed normally");
            }
            out.close();
            in.close();

            Runtime.getRuntime().exit(0);
        }

    } // process

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
        logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
    } // requestStop

} // Daemon
