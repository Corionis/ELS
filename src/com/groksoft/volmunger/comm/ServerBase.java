package com.groksoft.volmunger.comm;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Server service.
 *
 * The Server service is the command interface used to communicate between
 * the endpoints.
 */
public abstract class ServerBase
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
    protected Repository publisherRepo;
    protected String publisherKey;
    protected Repository subscriberRepo;
    protected String subscriberKey;

    /**
     * Instantiate the Server service
     */
    public ServerBase(Configuration config, Repository pubRepo, Repository subRepo)
    {
        this.cfg = config;
        this.publisherRepo = pubRepo;
        this.subscriberRepo = subRepo;
        this.subscriberKey = subscriberRepo.getLibraryData().libraries.key;
        this.publisherKey = publisherRepo.getLibraryData().libraries.key;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     *
     * @param aWriter The PrintWriter to be used to print the list.
     */
    public synchronized void dumpStatistics (PrintWriter aWriter)
    {
		/*
		aWriter.println("\r\Server currently connected: " + ((connected) ? "true" : "false"));
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
        return "ServerBase";
    }

    /**
     * Request the Server service to stop
     */
    public void requestStop ()
    {
        this.stop = true;
        logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
    }

    /**
     * Process a connection request to the Server service.
     *
     */
    public abstract void process(Socket aSocket) throws IOException;

    /**
     * Perform initial handshake for this session.
     *
     */
    public abstract boolean handshake();

} // ServerBase
