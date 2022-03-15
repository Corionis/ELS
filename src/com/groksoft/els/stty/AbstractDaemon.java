package com.groksoft.els.stty;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
public abstract class AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

    protected InetAddress address;
    protected boolean authorized = false;
    protected Configuration cfg;
    protected boolean connected = false;
    protected DataInputStream in = null;
    protected String myKey;
    protected Repository myRepo;
    protected DataOutputStream out = null;
    protected int port;
    protected String response = "";
    protected Socket socket;
    protected boolean stop = false;
    protected String theirKey;
    protected Repository theirRepo;

    /**
     * Instantiate the Daemon service
     */
    public AbstractDaemon(Configuration config, Repository mine, Repository theirs)
    {
        this.cfg = config;
        this.myRepo = mine;
        if (theirs != null)
        {
            this.theirRepo = theirs;
            this.theirKey = this.theirRepo.getLibraryData().libraries.key;
        }
        this.myKey = myRepo.getLibraryData().libraries.key;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     *
     * @param aWriter The PrintWriter to be used to print the list.
     */
    public synchronized void dumpStatistics(PrintWriter aWriter)
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
    public String getName()
    {
        return "Daemon";
    }

    public Socket getSocket()
    {
        return socket;
    }

    /**
     * Perform initial handshake for this session.
     */
    public abstract String handshake();

    /**
     * Process a connection request to the Daemon service.
     */
    public abstract boolean process(Socket aSocket) throws IOException, Exception;

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("Requesting stop for stty session: " + Utils.formatAddresses(socket));
    }

} // Daemon
