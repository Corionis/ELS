package com.groksoft.volmunger.comm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.log4j.Logger;

import java.io.*;
import java.net.*;

//----------------------------------------------------------------------------
/**
 * Handle individual client connections.
 * 
 * The Connection class is a subclass of Thread. It handles individual
 * connections between a Service and a client user. Each connection has a
 * separate thread. Each Service can have multiple connection requests pending
 * at once.
 */
public class Connection extends Thread
{
	protected static Logger logger = LogManager.getLogger("applog");//
//	protected static Logger logger = Logger.getLogger(Connection.class);

	/** The service for the connection */
	protected Session _service;
	/** The socket for the connection */
	protected Socket _socket;

	//------------------------------------------------------------------------
	/**
	 * Constructor.
	 * 
	 * Connection objects are created by Listener threads as part of the
	 * server's thread group. The superclass constructor is called to create a
	 * new thread to handle the connection request.
	 * 
	 * @param aSocket Socket for connection
	 * @param aService Service for connection
	 */
	public Connection (Socket aSocket, Session aService)
	{
		super("Session.Connection:" + aSocket.getInetAddress().getHostAddress() + ":" + aSocket.getPort());
		this._socket = aSocket;
		this._service = aService;
	} // constructor
	
	//------------------------------------------------------------------------
	/**
	 * Get the associated Session instance
	 */
	public Session getConsole ()
	{
		return _service;
	}
	
	//------------------------------------------------------------------------
	/**
	 * Get the associated Socket instance
	 */
	public Socket getSocket ()
	{
		return _socket;
	}

	//------------------------------------------------------------------------
	/**
	 * Run the service for this connection.
	 * 
	 * Creates input and output streams for the connection and calls the
	 * serveIt() interface method for the Service. Calls endConnection() when
	 * the serveIt() method returns for any reason.
	 * 
	 */
	public void run ()
	{
		try
		{
			_service.process(_socket);
		}
		catch (IOException e)
		{
			logger.info(e);
		}
		finally
		{
			// notify the ConnectionManager that this connection has closed
			CommManager cm = CommManager.getInstance();
			if (cm != null)
			{
				cm.endConnection();
			}
		}
	}
} // Connection

