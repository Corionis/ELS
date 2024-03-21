package com.corionis.els.stty;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.repository.Repository;
import com.corionis.els.stty.gui.TerminalGui;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * ClientStty -to- ServeStty, used for both manual (interactive) and automated sessions
 */
public class ClientStty
{
    private Context context;
    protected TerminalGui gui = null;
    private Thread heartBeat = null;
    private boolean heartBeatEnabled = true;
    protected DataInputStream in = null;
    private boolean isConnected = false;
    private boolean isTerminal = false;
    private String myKey;
    private Repository myRepo;
    protected DataOutputStream out = null;
    private boolean primaryServers;
    private Socket socket;
    private Repository theirRepo;
    private transient Logger logger = LogManager.getLogger("applog");

    /**
     * Instantiate a ClientStty.<br>
     *
     * @param context          The Context
     * @param isManualTerminal True if an interactive client, false if an automated client
     * @param primaryServers   True if base servers, false if secondary servers for Publisher
     */
    public ClientStty(Context context, boolean isManualTerminal, boolean primaryServers)
    {
        this.context = context;
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
        String response = roundTrip("space \"" + location + "\"", "", 5000);
        if (response != null && response.length() > 0)
        {
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
        String response = receive("", 5000); // read opening terminal banner
        if (!context.cfg.isNavigator()) // ignore subscriber commands with Navigator
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
                                setupCollectionRequest();
                            }
                            else if (cmdSplit[i].equals("RequestTargets"))
                            {
                                hasCommands = true;
                                context.cfg.setRequestTargets(true);
                            }
                        }
                    }
                }
                else
                {
                    throw new MungeException("Unknown banner receive");
                }

                // if no content (a Library) and a request is not set then set it
                // fixes logical problem if neither publisher or subscriber ask or force sending collection
                if (!context.subscriberRepo.hasContent() && !context.cfg.isRequestCollection())
                {
                    logger.info(context.cfg.gs("ClientStty.forcing.request.of.collection.with.s"));
                    setupCollectionRequest();
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

            String host = Utils.parseHost(this.theirRepo.getLibraryData().libraries.host);
            if (host == null || host.isEmpty())
            {
                host = null;
            }
            int port = Utils.getPort(this.theirRepo.getLibraryData().libraries.host) + ((primaryServers) ? 0 : 2);
            logger.info("Opening stty connection to: " + (host == null ? "localhost" : host) + ":" + port);

            try
            {
                this.socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(host, port);
                this.socket.connect(socketAddress, theirRepo.getLibraryData().libraries.timeout * 60 * 1000);

                this.socket.setKeepAlive(true); // keep alive to avoid time-out
                this.socket.setSoTimeout(myRepo.getLibraryData().libraries.timeout * 60 * 1000); // read time-out
                this.socket.setSoLinger(true, 10000); // time-out to linger after transmission completed, 10 sec.

                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                logger.info("Successfully connected stty to: " + Utils.formatAddresses(this.socket));
            }
            catch (Exception e)
            {
                context.fault = true;
                //logger.error(Utils.getStackTrace(e));
                logger.error(e.getMessage());
            }

            if (in != null && out != null)
            {
                if (handshake())
                {
                    isConnected = true;
                    createHeartBeat();
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
     * Create and start the internal heartbeat
     */
    private void createHeartBeat()
    {
        heartBeat = new Thread()
        {
            public void run()
            {
                String exceptionMessage = "";
                String errorMessage = "";
                try
                {
                    sleep(40 * 1000); // offset this heartbeat timing
                    String desc = (theirRepo != null) ? " to " + theirRepo.getLibraryData().libraries.description : "";
                    while (true)
                    {
                        sleep(1 * 60 * 1000); // heartbeat sleep time in milliseconds
                        if (heartBeatEnabled)
                        {
                            send("ping", context.trace ? "heartbeat sent" + desc : "");
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    logger.trace("heartbeat interrupted");
                }
                catch (Exception e)
                {
                    context.fault = true;
                    if (context.browser != null)
                        context.browser.toggleHintTracking(false);
                    if (context.mainFrame != null)
                        context.mainFrame.buttonHintTracking.setEnabled(false);
                    errorMessage = "(Hint Server) " + e.getMessage();
                    exceptionMessage = Utils.getStackTrace(e);
                    heartBeat.interrupt();
                }
                stopClient(errorMessage, exceptionMessage);
            }
        };
        logger.trace("starting heartbeat");
        heartBeat.start();
    }

    /**
     * Temporarily disable the internal heartbeat
     */
    private void disableHeartBeat()
    {
        if (heartBeat != null)
        {
            if (!heartBeatEnabled)
                logger.warn("Client heartbeat already disabled");
            else
                logger.trace("Client heartbeat disabled");
            heartBeatEnabled = false;
        }
    }

    /**
     * Disconnect this STTY from the other end
     */
    public void disconnect()
    {
        try
        {
            stopHeartBeat();
            if (isConnected)
            {
                isConnected = false;
                logger.debug("disconnecting stty: " + Utils.formatAddresses(socket));
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

    /**
     * Enable the internal heartbeat
     */
    private void enableHeartBeat()
    {
        if (heartBeat != null)
        {
            if (heartBeatEnabled)
                logger.warn("heartbeat already enabled");
            else
                logger.trace("heartbeat enabled");
            heartBeatEnabled = true;
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

    public Socket getSocket()
    {
        return socket;
    }

    public String getTheirKey()
    {
        return theirRepo.getLibraryData().libraries.key;
    }

    public Repository getTheirRepo()
    {
        return theirRepo;
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
        logger.trace("handshake");
        String input = receive("", 5000);
        if (input != null && input.equals("HELO"))
        {
            send((isTerminal ? "DribNit" : "DribNlt"), "");

            input = receive("", 5000);
            if (input.equals(theirRepo.getLibraryData().libraries.key))
            {
                send(myKey, "");

                // get the subscriber's flavor
                input = receive("", 5000);
                try
                {
                    // if Utils.getFileSeparator() does not throw an exception
                    // the subscriber's flavor is valid
                    Utils.getFileSeparator(input);

                    logger.info("Stty client authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + theirRepo.getLibraryData().libraries.description);
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
                logger.warn("attempt to login interactively but terminal sessions are not allowed");
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
        if (context.cfg.isQuitStatusServer())
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
                    logger.info("Sending quit command to Hint Status Server: " + context.statusRepo.getLibraryData().libraries.description);
                    context.statusStty.send("quit", "");
                    Thread.sleep(3000);
                    context.statusStty.disconnect();
                    context.statusStty = null;
                }
                else
                    logger.warn("could not send quit command to Hint Status Server: " + context.statusRepo.getLibraryData().libraries.description);
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
     * @param log     Line to be logged, if any
     * @param timeout Timeout for operationsUI in milliseconds
     * @return String of response text
     * @throws Exception
     */
    public String receive(String log, int timeout) throws Exception
    {
        if (getSocket().isOutputShutdown())
            throw new MungeException("socket output shutdown, keep alive " + getSocket().getKeepAlive());
        if (!getSocket().isBound())
            throw new MungeException("socket not bound");
        if (!getSocket().isConnected())
            throw new MungeException("socket not connected");
        if (getSocket().isClosed())
            throw new MungeException("socket closed");

        if (timeout < 0)
            timeout = myRepo.getLibraryData().libraries.timeout * 60 * 1000;
        getSocket().setSoTimeout(timeout); // set read time-out
        if (log != null && log.length() > 0)
            logger.debug(log + ", " + timeout + " ms");
        logger.trace("sotimeout " + getSocket().getSoTimeout());
        //logger.trace("keep alive " + getSocket().getKeepAlive());

        String response = null;
        while (true)
        {
            response = Utils.readStream(in, theirRepo.getLibraryData().libraries.key);
            if (response != null && response.startsWith("ping"))
                logger.trace("heartbeat received" + ((theirRepo != null) ? " from " + theirRepo.getLibraryData().libraries.description : ""));
            else
                break;
        }
        return response;
    }

    /**
     * Retrieve remote data and store it in a file
     *
     * @param message The command to send
     * @param log     Line to be logged, if any
     * @param timeout Timeout for operationsUI in milliseconds
     * @return The resulting date-stamped file path
     * @throws Exception
     */
    public String retrieveRemoteData(String message, String log, int timeout) throws Exception
    {
        String location = null;
        String response = "";

        response = roundTrip(message, log, timeout);
        if (response != null && response.length() > 0)
        {
            location = Utils.scrubFilename(theirRepo.getLibraryData().libraries.description).replaceAll(" ", "");
            location = Utils.getStampedFilename(myRepo, location + "_" + message + "-received");
            location = Utils.getTemporaryFilePrefix(myRepo, location) + ".json";

            try
            {
                PrintWriter outputStream = new PrintWriter(location);
                outputStream.println(response);
                outputStream.close();
            }
            catch (FileNotFoundException fnf)
            {
                context.fault = true;
                throw new MungeException("Exception while writing " + message + " file " + location + " trace: " + Utils.getStackTrace(fnf));
            }
        }
        return location;
    }

    /**
     * Make a round-trip to the other end by sending a command and receiving the response
     *
     * @param message The command to send
     * @param log     The line to be logged, if any
     * @param timeout Timeout for operationsUI
     * @return String of the response
     * @throws Exception
     */
    public synchronized String roundTrip(String message, String log, int timeout) throws Exception
    {
        send(message, ""); // log only once
        String response = receive(log, timeout);
        return response;
    }

    /**
     * Send a command to the other end
     *
     * @param message The command to send
     * @param log     Line to be logged, if any
     * @throws Exception
     */
    public void send(String message, String log) throws Exception
    {
        if (log != null && log.length() > 0)
            logger.debug(log);
        //logger.trace("keep alive " + getSocket().getKeepAlive());
        if (getSocket().isOutputShutdown())
            throw new MungeException("socket output shutdown, keep alive " + getSocket().getKeepAlive());
        if (!getSocket().isBound())
            throw new MungeException("socket not bound");
        if (!getSocket().isConnected())
            throw new MungeException("socket not connected");
        if (getSocket().isClosed())
            throw new MungeException("socket closed");

        if (!message.equalsIgnoreCase("ping"))
            disableHeartBeat();

        // for the rare circumstance when if, in the middle of reading a requested collection file from disk,
        // and sending a heartbeat in the same 1-2 seconds, the data may not be entirely there.
        if (out != null && theirRepo != null && theirRepo.getLibraryData() != null && theirRepo.getLibraryData().libraries != null &&
                theirRepo.getLibraryData().libraries.key != null && theirRepo.getLibraryData().libraries.key.length() > 0)
        {
            Utils.writeStream(out, theirRepo.getLibraryData().libraries.key, message);
        }

        if (!message.equalsIgnoreCase("ping"))
            enableHeartBeat();
    }

    private void setupCollectionRequest()
    {
        context.cfg.setRequestCollection(true);
        String location;
        if (context.cfg.getSubscriberCollectionFilename().length() > 0)
            location = context.cfg.getSubscriberCollectionFilename();
        else
            location = context.cfg.getSubscriberLibrariesFileName();

        // change cfg -S to -s so -s handling in Transfer.initialize retrieves the data
        context.cfg.setSubscriberLibrariesFileName(location);
        context.cfg.setSubscriberCollectionFilename("");
    }

    private void stopClient(String errorMessage, String exceptionMessage)
    {
        if (context.fault)
        {
            disconnect();

            if (exceptionMessage.length() > 0)
                logger.error(context.cfg.gs("Z.fault") + exceptionMessage);

            if (context.mainFrame != null && errorMessage.length() > 0)
            {
                JOptionPane.showMessageDialog(context.mainFrame, "Client Stty: " + errorMessage,
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }

            if (context.serveStty != null && context.serveStty.isAlive())
            {
                context.serveStty.requestStop();
                context.serveStty.stopServer();
            }
        }
    }

    /**
     * Stop the internal heart beat thread
     */
    private void stopHeartBeat()
    {
        if (heartBeat != null && heartBeat.isAlive())
        {
            logger.trace("stopping heartbeat thread");
            heartBeat.interrupt();
        }
    }

    /**
     * Start an interactive GUI terminal session
     *
     * @return
     * @throws Exception
     */
    public int terminalSession() throws Exception
    {
        int returnValue = 0;
        gui = new TerminalGui(this, context, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

}
