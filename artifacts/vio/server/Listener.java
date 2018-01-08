package com.groksoft.volmonger.vio.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

//----------------------------------------------------------------------------
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
	private transient Logger logger = LogManager.getLogger("applog");


	/** The default timeout for socket connections, in milliseconds */
	protected static final int _socketTimeout = 2000;

	/** The socket to listen on for the associated service */
	private ServerSocket _listenSocket;
	/** The port to listen on for the associated service */
	private int _port;
	/** Flag used to determine when to stop listening */
	private boolean _stop = false;

	//------------------------------------------------------------------------
	/**
	 * Setup a new Listener on a specified port.
	 * 
	 * The socket is set with a timeout to allow accept() to be interrupted, and
	 * the service to be removed from the server.
	 * 
	 * @param aGroup The thread group used for the listener.
	 * @param aPort The port to listen on.
	 */
	public Listener (ThreadGroup aGroup, int aPort) throws IOException
	{
		super(aGroup, "Listener:" + aPort);

		// setup this listener
		this._port = aPort;

		_listenSocket = new ServerSocket(aPort);

		// set a non-zero timeout on the socket so accept() may be interrupted
		_listenSocket.setSoTimeout(_socketTimeout);
	} // constructor

	//------------------------------------------------------------------------
	/**
	 * Politely request the listener to stop.
	 * 
	 */
	public void requestStop ()
	{
		this._stop = true;
		this.interrupt();
	}

	//------------------------------------------------------------------------
	/**
	 * Run listen thread body.
	 * 
	 * Waits for a connection request. When a request is received an attempt is
	 * made to create a new connection thread via a call to the addConnection()
	 * method.
	 */
	public void run ()
	{
		logger.info("Listening on port " + _port);
		while (_stop == false)
		{
			try
			{
				Socket theSocket = (Socket)_listenSocket.accept();
				theSocket.setTcpNoDelay(true);
				theSocket.setSoLinger(false, -1);
				ConnectionManager.getInstance().addConnection(theSocket);
			}
			catch (SocketTimeoutException e)
			{
				//logger.info("Listen accept timeout on port " + _port + ", stop=" + ((_stop)?"true, stopping":"false, continuing"));
				continue;
			}
			catch (InterruptedIOException e)
			{
				logger.info("listener interrupted on port " + _port + ", stop=" + ((_stop)?"true":"false"));
			}
			catch (IOException e)
			{
				logger.error(e);
				_stop = true;
			}
		}
		if (logger != null)
			logger.info("Stopped listener on port " + _port);
	}
} // Listener
