package com.groksoft.els.stty;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.stty.gui.TerminalGui;
import com.groksoft.els.repository.Repository;
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
    private transient Logger logger = LogManager.getLogger("applog");

    private Configuration cfg;
    private boolean isConnected = false;
    private boolean isTerminal = false;
    private Socket socket;

    DataInputStream in = null;
    DataOutputStream out = null;
    TerminalGui gui = null;

    private Repository myRepo;
    private Repository theirRepo;
    private String myKey;
    private String theirKey;
    private boolean primaryServers;

    /**
     * Instantiate a ClientStty.<br>
     *
     * @param config     The Configuration object
     * @param isManualTerminal True if an interactive client, false if an automated client
     */
    public ClientStty(Configuration config, boolean isManualTerminal, boolean primaryServers)
    {
        this.cfg = config;
        this.isTerminal = isManualTerminal;
        this.primaryServers = primaryServers;
    }

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

    public boolean checkBannerCommands() throws Exception
    {
        boolean hasCommands = false;
        String response = receive(); // read opening terminal banner
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
        return hasCommands;
    }

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

    public int guiSession() throws Exception
    {
        int returnValue = 0;
        gui = new TerminalGui(this, cfg, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

    private boolean handshake() throws Exception
    {
        boolean valid = false;
        String input = Utils.readStream(in, theirKey);
        if (input.equals("HELO"))
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
        }
        return valid;
    }

    public boolean isConnected()
    {
        return isConnected;
    }

    public boolean quitStatusServer(Context context, boolean fault)
    {
        if (cfg.isQuitStatusServer())
        {
            if (context.statusRepo == null)
            {
                logger.warn("-q requires a -h hints file");
                return true;
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
                fault = true;
            }
        }
        return fault;
    }

    public String receive() throws Exception
    {
        String response = Utils.readStream(in, theirRepo.getLibraryData().libraries.key);
        return response;
    }

    public String retrieveRemoteData(String filename, String command) throws Exception
    {
        String location = null;
        String response = "";

        response = roundTrip(command);
        if (response != null && response.length() > 0)
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
            LocalDateTime now = LocalDateTime.now();
            String stamp = dtf.format(now);
            location = filename + "_" + command + "-received-" + stamp + ".json";
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

    public String roundTrip(String command) throws Exception
    {
        send(command);
        String response = receive();
        return response;
    }

    public void send(String command) throws Exception
    {
        Utils.writeStream(out, theirRepo.getLibraryData().libraries.key, command);
    }

}
