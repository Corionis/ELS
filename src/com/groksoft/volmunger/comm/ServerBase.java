package com.groksoft.volmunger.comm;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

//----------------------------------------------------------------------------
/**
 * Server service.
 *
 * The Server service is the command interface used to communicate between
 * the endpoints.
 */
public abstract class ServerBase
{
    protected static Logger logger = LogManager.getLogger("applog");

    protected boolean authorized = false;
    protected String passwordClear;
    protected String passwordEncrypted;
    protected boolean secret = false;
    protected String secretClear;
    protected String secretEncrypted;
    protected boolean _connected = false;
    protected Socket socket;
    protected int port;
    protected InetAddress address;
    protected boolean _stop = false;

    protected DataInputStream in = null;
    protected DataOutputStream out = null;

    protected Configuration cfg;
    protected Repository publisherRepo;
    protected Repository subscriberRepo;
    protected String publisherKey;
    protected String subscriberKey;

    //------------------------------------------------------------------------
    /**
     * Instantiate the Server service
     */
    public ServerBase(Configuration config, Repository pubRepo, Repository subRepo)
    {
        this.passwordClear = "";
        this.passwordEncrypted = "";
        this.secretClear = "";
        this.secretEncrypted = "";
        this.cfg = config;
        this.publisherRepo = pubRepo;
        this.subscriberRepo = subRepo;
        this.subscriberKey = subscriberRepo.getLibraryData().libraries.key;
        this.publisherKey = publisherRepo.getLibraryData().libraries.key;
    } // constructor

    //------------------------------------------------------------------------
    /**
     * Dump statistics from all available internal sources.
     *
     * @param aWriter The PrintWriter to be used to print the list.
     */
    public synchronized void dumpStatistics (PrintWriter aWriter)
    {
		/*
		aWriter.println("\r\Server currently connected: " + ((_connected) ? "true" : "false"));
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

    //------------------------------------------------------------------------
    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName ()
    {
        return "ServerBase";
    }

    //------------------------------------------------------------------------
    /**
     * Request the Server service to stop
     */
    public void requestStop ()
    {
        this._stop = true;
        logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
    }

    //------------------------------------------------------------------------
    /**
     * Process a connection request to the Server service.
     *
     */
    public abstract void process(Socket aSocket) throws IOException;

    //------------------------------------------------------------------------
    /**
     * Perform initial handshake for this session.
     *
     */
    public abstract boolean handshake();

} // ServerBase
