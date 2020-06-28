package com.groksoft.volmunger.stty;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.MungerException;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.stty.gui.TerminalGui;
import com.groksoft.volmunger.repository.Repository;
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
 * SttyClient -to- Stty server, used for both manual (interactive) and automated sessions
 */
public class SttyClient
{
    private transient Logger logger = LogManager.getLogger("applog");

    private Configuration cfg;
    private boolean isConnected = false;
    private boolean isTerminal = false;
    private Socket socket;

    DataInputStream in = null;
    DataOutputStream out = null;

    private Repository myRepo;
    private Repository theirRepo;
    private String myKey;
    private String theirKey;

    /**
     * Instantiate a SttyClient.<br>
     *
     * @param config     The Configuration object
     * @param isTerminal True if an interactive client, false if an automated client
     */
    public SttyClient(Configuration config, boolean isTerminal)
    {
        this.cfg = config;
        this.isTerminal = isTerminal;
    }

    public long availableSpace(String location)
    {
        long space = 0L;
        String response = roundTrip("space " + location);
        if (response != null && response.length() > 0)
        {
            space = Long.parseLong(response);
        }
        return space;
    }

    public boolean checkBannerCommands() throws MungerException
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

                            // change cfg -S to -s so -s handling in Process.process retrieves the data
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
                throw new MungerException("Unknown banner receive");
            }
        }
        return hasCommands;
    }

    public boolean connect(Repository mine, Repository theirs) throws MungerException
    {
        this.myRepo = mine;
        this.theirRepo = theirRepo;

        if (this.theirRepo != null &&
                this.theirRepo.getLibraryData() != null &&
                this.theirRepo.getLibraryData().libraries != null &&
                this.theirRepo.getLibraryData().libraries.site != null)
        {

            this.myKey = mine.getLibraryData().libraries.key;
            this.theirKey = theirRepo.getLibraryData().libraries.key;

            String host = Utils.parseHost(this.theirRepo.getLibraryData().libraries.site);
            if (host == null || host.isEmpty())
            {
                host = null;
            }
            int port = Utils.getPort(this.theirRepo.getLibraryData().libraries.site);
            logger.info("Opening connection to: " + (host == null ? "localhost" : host) + ":" + port);

            try
            {
                this.socket = new Socket(host, port);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

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
                    logger.error("Connection to " + this.theirRepo.getLibraryData().libraries.site + " failed handshake");
                }
            }
            else
            {
                logger.error("Could not make connection to " + this.theirRepo.getLibraryData().libraries.site);
            }
        }
        else
        {
            throw new MungerException("cannot get site from -r specified remote subscriber library");
        }

        return isConnected;
    }

    public void disconnect()
    {
        try
        {
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception e)
        {
        }
    }

    private boolean handshake()
    {
        boolean valid = false;
        String input = Utils.read(in, theirKey);
        if (input.equals("HELO"))
        {
            Utils.write(out, theirKey, (isTerminal ? "DribNit" : "DribNlt"));

            input = Utils.read(in, theirKey);
            if (input.equals(theirKey))
            {
                Utils.write(out, theirKey, myKey);

                input = Utils.read(in, theirKey);
                if (input.equals("ACK"))
                {
                    logger.info("Authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + theirRepo.getLibraryData().libraries.description);
                    valid = true;
                }
            }
        }
        return valid;
    }

    public boolean isConnected()
    {
        return isConnected;
    }

    public String receive()
    {
        String response = Utils.read(in, theirRepo.getLibraryData().libraries.key);
        return response;
    }

    public String retrieveRemoteData(String filename, String command) throws MungerException
    {
        String location = "";
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
                throw new MungerException("Exception while writing " + command + " file " + location + " trace: " + Utils.getStackTrace(fnf));
            }
        }
        return location;
    }

    public String roundTrip(String command)
    {
        send(command);
        String response = receive();
        return response;
    }

    public void send(String command)
    {
        Utils.write(out, theirRepo.getLibraryData().libraries.key, command);
    }

    public int guiSession()
    {
        int returnValue = 0;
        TerminalGui gui = new TerminalGui(this, cfg, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

}
