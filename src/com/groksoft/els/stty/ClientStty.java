package com.groksoft.els.stty;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.stty.gui.TerminalGui;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * ClientStty -to- ServeStty, used for both manual (interactive) and automated sessions
 */
public class ClientStty
{
    TerminalGui gui = null;
    DataInputStream in = null;
    DataOutputStream out = null;
    private Configuration cfg;
    private boolean isConnected = false;
    private boolean isTerminal = false;
    private String myKey;
    private Repository myRepo;
    private boolean primaryServers;
    private Socket socket;
    private String theirKey;
    private Repository theirRepo;
    private transient Logger logger = LogManager.getLogger("applog");

    /**
     * Instantiate a ClientStty.<br>
     *
     * @param config           The Configuration object
     * @param isManualTerminal True if an interactive client, false if an automated client
     * @param primaryServers   True if base servers, false if secondary servers for Publisher
     */
    public ClientStty(Configuration config, boolean isManualTerminal, boolean primaryServers)
    {
        this.cfg = config;
        this.isTerminal = isManualTerminal;
        this.primaryServers = primaryServers;
    }

    /**
     * Return available space disk of the remote location
     *
     * @param location Path on remote
     * @return long Available space in bytes
     * @throws Exception
     */
    public long availableSpace(String location) throws Exception
    {
        long space = 0L;
        String response = roundTrip("space \"" + location + "\"");
        if (response != null && response.length() > 0)
        {
            //logger.debug("space command returned: " + response);
            space = Long.parseLong(response);
        }
        return space;
    }

    /**
     * Read opening terminal banner for possible commands
     * <p>
     * Handles subscriber-side commands sent to publisher at login time
     * for RequestCollection to retrieve the current subscriber collection,
     * and RequestTargets to retrieve the current subscriber targets.
     *
     * @return true if commands were present and processed
     * @throws Exception
     */
    public boolean checkBannerCommands() throws Exception
    {
        boolean hasCommands = false;
        String response = receive(); // read opening terminal banner
        if (!cfg.isNavigator()) // ignore subscriber commands with Navigator
        {
            if (!response.startsWith("Enter "))
            {
                String[] cmdSplit = response.split(":");
                if (cmdSplit.length > 0)
                {
                    if (cmdSplit[0].equals("CMD"))
                    {
                        for (int i = 1; i < cmdSplit.length; ++i)
                        {
                            if (cmdSplit[i].equals("RequestCollection"))
                            {
                                hasCommands = true;
                                cfg.setRequestCollection(true);
                                String location;
                                if (cfg.getSubscriberCollectionFilename().length() > 0)
                                    location = cfg.getSubscriberCollectionFilename();
                                else
                                    location = cfg.getSubscriberLibrariesFileName();

                                // change cfg -S to -s so -s handling in Transfer.initialize retrieves the data
                                cfg.setSubscriberLibrariesFileName(location);
                                cfg.setSubscriberCollectionFilename("");
                            }
                            else if (cmdSplit[i].equals("RequestTargets"))
                            {
                                hasCommands = true;
                                cfg.setRequestTargets(true);
                            }
                        }
                    }
                }
                else
                {
                    throw new MungeException("Unknown banner receive");
                }
            }
        }
        return hasCommands;
    }

    /**
     * Connect this STTY to the other end
     *
     * @param mine   Local Repository
     * @param theirs Remote Repository
     * @return true if connected
     * @throws Exception
     */
    public boolean connect(Repository mine, Repository theirs) throws Exception
    {
        this.myRepo = mine;
        this.theirRepo = theirs;

        if (this.theirRepo != null &&
                this.theirRepo.getLibraryData() != null &&
                this.theirRepo.getLibraryData().libraries != null &&
                this.theirRepo.getLibraryData().libraries.host != null)
        {
            this.myKey = myRepo.getLibraryData().libraries.key;
            this.theirKey = theirRepo.getLibraryData().libraries.key;

            String host = Utils.parseHost(this.theirRepo.getLibraryData().libraries.host);
            if (host == null || host.isEmpty())
            {
                host = null;
            }
            int port = Utils.getPort(this.theirRepo.getLibraryData().libraries.host) + ((primaryServers) ? 0 : 2);
            logger.info("Opening stty connection to: " + (host == null ? "localhost" : host) + ":" + port);

            try
            {
                this.socket = new Socket(host, port);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());
                logger.info("Successfully connected stty to: " + Utils.formatAddresses(this.socket));
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }

            if (in != null && out != null)
            {
                if (handshake())
                {
                    isConnected = true;
                }
                else
                {
                    logger.error("Connection to " + Utils.formatAddresses(socket) + " failed handshake");
                }
            }
        }
        else
        {
            throw new MungeException("Cannot get site from -s | -S specified remote subscriber library");
        }

        return isConnected;
    }

    /**
     * Disconnect this STTY from the other end
     */
    public void disconnect()
    {
        try
        {
            if (isConnected)
            {
                isConnected = false;
                logger.debug("Disconnecting stty: " + Utils.formatAddresses(socket));
                if (gui != null)
                    gui.stop();
                out.flush();
                out.close();
                in.close();
            }
        }
        catch (Exception e)
        {
        }
    }

    public String getMyKey()
    {
        return myKey;
    }

    public Repository getMyRepo()
    {
        return myRepo;
    }

    public String getTheirKey()
    {
        return theirKey;
    }

    public Repository getTheirRepo()
    {
        return theirRepo;
    }

    /**
     * Start an interactive GUI terminal session
     *
     * @return
     * @throws Exception
     */
    public int guiSession() throws Exception
    {
        int returnValue = 0;
        gui = new TerminalGui(this, cfg, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

    /**
     * Perform a handshake with the other end
     *
     * @return true if connection authenticated
     * @throws Exception
     */
    private boolean handshake() throws Exception
    {
        boolean valid = false;
        String input = Utils.readStream(in, theirKey);
        if (input != null && input.equals("HELO"))
        {
            Utils.writeStream(out, theirKey, (isTerminal ? "DribNit" : "DribNlt"));

            input = Utils.readStream(in, theirKey);
            if (input.equals(theirKey))
            {
                Utils.writeStream(out, theirKey, myKey);

                // get the subscriber's flavor
                input = Utils.readStream(in, theirKey);
                try
                {
                    // if Utils.getFileSeparator() does not throw an exception
                    // the subscriber's flavor is valid
                    Utils.getFileSeparator(input);

                    logger.info("Authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + theirRepo.getLibraryData().libraries.description);
                    valid = true;

                    // override what we THINK the subscriber flavor is with what we are told
                    theirRepo.getLibraryData().libraries.flavor = input;
                }
                catch (Exception e)
                {
                    // ignore
                }
            }
            else if (input.equalsIgnoreCase("Terminal session not allowed"))
                logger.warn("Attempted to login interactively but terminal sessions are not allowed");
        }
        return valid;
    }

    /**
     * Return if this STTY session is connected
     *
     * @return
     */
    public boolean isConnected()
    {
        return isConnected;
    }

    /**
     * Send command to Hint Status Server to quit
     *
     * @param context The Context
     * @return Resulting fault indicator
     */
    public void quitStatusServer(Context context)
    {
        if (cfg.isQuitStatusServer())
        {
            if (context.statusRepo == null)
            {
                logger.warn("-q requires a -h hints file");
                context.fault = true;
            }
            try
            {
                if (isConnected())
                {
                    logger.info("Sending quit command to hint status server: " + context.statusRepo.getLibraryData().libraries.description);
                    context.statusStty.send("quit");
                }
                else
                    logger.warn("Could not send quit command to hint status server: " + context.statusRepo.getLibraryData().libraries.description);
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                context.fault = true;
            }
        }
    }

    /**
     * Receive a response from the other end
     *
     * @return String of response text
     * @throws Exception
     */
    public String receive() throws Exception
    {
        String response = Utils.readStream(in, theirRepo.getLibraryData().libraries.key);
        return response;
    }

    /**
     * Retrieve remote data and store it in a file
     *
     * @param filename File path to store the data
     * @param command  The command to send
     * @return The resulting date-stamped file path
     * @throws Exception
     */
    public String retrieveRemoteData(String filename, String command) throws Exception
    {
        String location = null;
        String response = "";

        response = roundTrip(command);
        if (response != null && response.length() > 0)
        {
            String stamp = "";
            if (myRepo.getLibraryData().libraries.temp_dated != null && myRepo.getLibraryData().libraries.temp_dated)
            {
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
                LocalDateTime now = LocalDateTime.now();
                stamp = "-" + dtf.format(now);
            }

            String path = "";
            String fn = Utils.scrubFilename(theirRepo.getLibraryData().libraries.description).replaceAll(" ", "");
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
            location += "_" + command + "-received" + stamp + ".json";
            try
            {
                PrintWriter outputStream = new PrintWriter(location);
                outputStream.println(response);
                outputStream.close();
            }
            catch (FileNotFoundException fnf)
            {
                throw new MungeException("Exception while writing " + command + " file " + location + " trace: " + Utils.getStackTrace(fnf));
            }
        }
        return location;
    }

    /**
     * Make a round-trip to the other end by sending a command and receiving the response
     *
     * @param command The command to send
     * @return String of the response
     * @throws Exception
     */
    public synchronized String roundTrip(String command) throws Exception
    {
        send(command);
        String response = receive();
        return response;
    }

    /**
     * Send a command to the other end
     *
     * @param command The command to send
     * @throws Exception
     */
    public void send(String command) throws Exception
    {
        Utils.writeStream(out, theirRepo.getLibraryData().libraries.key, command);
    }

}
