package com.groksoft.volmunger.stty;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.MungerException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Listen for a connection request for a service.
 * 
 * The Listener class is a subclass of Thread. It listens for connections
 * on a specified port. When a connection is requested it is handled by the
 * ConnectionManager. There is one Listener for each service on a specified
 * port.
 * 
 */
public class Listener extends Thread
{
	protected static Logger logger = LogManager.getLogger("applog");//

	/** The default timeout for socket connections, in milliseconds */
	protected static final int socketTimeout = 2000;

	/** The socket to listen on for the associated service */
	private ServerSocket listenSocket;
	/** The port to listen on for the associated service */
	private int port;
	/** Flag used to determine when to stop listening */
	private boolean stop = false;

	private Configuration cfg;

	/**
	 * Setup a new Listener on a specified port.
	 * 
	 * The socket is set with a timeout to allow accept() to be interrupted, and
	 * the service to be removed from the server.
	 * 
	 * @param group The thread group used for the listener.
	 * @param aPort The port to listen on.
	 */
	public Listener (ThreadGroup group, String host, int aPort, Configuration config) throws Exception
	{
		super(group, "Listener:" + host + ":" + aPort);

		// setup this listener
        this.cfg = config;
		this.port = aPort;
		InetAddress addr = Inet4Address.getByName(host);

		listenSocket = new ServerSocket(this.port, 5, addr);

        // QUESTION how to handle persistent listener AND properly close the socket when application is killed
		// set a non-zero timeout on the socket so accept() may be interrupted
		listenSocket.setSoTimeout(socketTimeout);
	} // constructor

	/**
	 * Politely request the listener to stop.
	 * 
	 */
	public void requestStop ()
	{
		this.stop = true;
		this.interrupt();
	}

	/**
	 * Run listen thread body.
	 * 
	 * Waits for a connection request. When a request is received an attempt is
	 * made to create a new connection thread via a call to the addConnection()
	 * method.
	 */
	public void run ()
	{
		while (stop == false)
		{
			try
			{
				Socket theSocket = (Socket) listenSocket.accept();
				theSocket.setTcpNoDelay(true);
                //theSocket.setSoLinger(false, -1);
                theSocket.setSoLinger(true, 10000); // linger 10 seconds after transmission completed

				ServeStty.getInstance().addConnection(theSocket);
			}
			catch (SocketTimeoutException e)
			{
				//logger.info("Listen accept timeout on port " + port + ", stop=" + ((stop)?"true, stopping":"false, continuing"));
				continue;
			}
			catch (InterruptedIOException e)
			{
				logger.info("listener interrupted on port " + port + ", stop=" + ((stop)?"true":"false"));
			}
			catch (IOException e)
			{
				logger.error(e);
				stop = true;
			}
			catch (MungerException e)
			{
				logger.error(e);
				stop = true;
			}
		}
		if (logger != null)
			logger.info("Stopped listener on port " + port);
	}
} // Listener
