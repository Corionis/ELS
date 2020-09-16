package com.groksoft.els.stty;

import com.groksoft.els.Configuration;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Daemon service.
 *
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
public abstract class DaemonBase
{
    protected static Logger logger = LogManager.getLogger("applog");

    protected InetAddress address;
    protected boolean authorized = false;
    protected boolean connected = false;
    protected int port;
    protected Socket socket;
    protected boolean stop = false;

    protected DataInputStream in = null;
    protected DataOutputStream out = null;
    protected String response = "";

    protected Configuration cfg;
    protected Repository myRepo;
    protected String myKey;
    protected Repository theirRepo;
    protected String theirKey;

    /**
     * Instantiate the Daemon service
     */
    public DaemonBase(Configuration config, Repository mine, Repository theirs)
    {
        this.cfg = config;
        this.myRepo = mine;
        this.theirRepo = theirs;
        this.theirKey = theirRepo.getLibraryData().libraries.key;
        this.myKey = myRepo.getLibraryData().libraries.key;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     *
     * @param aWriter The PrintWriter to be used to print the list.
     */
    public synchronized void dumpStatistics (PrintWriter aWriter)
    {
		/*
		aWriter.println("\r\Daemon currently connected: " + ((connected) ? "true" : "false"));
		aWriter.println("  Connected on port: " + port);
		aWriter.println("  Connected to: " + address);
		try
		{
			aWriter.println("  Work: " + work);
		}
		catch (Exception e)
		{
			aWriter.println("Exception " + e.getMessage());
			e.printStackTrace();
		}
		*/
    }

    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName ()
    {
        return "DaemonBase";
    }

    /**
     * Request the Daemon service to stop
     */
    public void requestStop ()
    {
        this.stop = true;
        logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
    }

    /**
     * Process a connection request to the Daemon service.
     *
     */
    public abstract void process(Socket aSocket) throws IOException;

    /**
     * Perform initial handshake for this session.
     *
     */
    public abstract boolean handshake();

} // DaemonBase
