package com.groksoft.volmunger.comm.subscriber;

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
 * Session service.
 *
 * The Session service is the command interface used to communicate between
 * the endpoints.
 */
public class Session
{
	protected static Logger logger = LogManager.getLogger("applog");

	boolean authorized = false;
	private String passwordClear;
	private String passwordEncrypted;
	boolean secret = false;
	private String secretClear;
	private String secretEncrypted;
	private boolean _connected = false;
	private Socket socket;
	private int port;
	private InetAddress address;
	private boolean _stop = false;

	BufferedReader in = null;
	PrintWriter out = null;

	private Configuration cfg;
    private Repository publisherRepo;
    private Repository subscriberRepo;

	//------------------------------------------------------------------------
	/**
	 * Instantiate the Session service
	 */
	public Session(Configuration config, Repository pubRepo, Repository subRepo)
	{
		this.passwordClear = "";
		this.passwordEncrypted = "";
		this.secretClear = "";
		this.secretEncrypted = "";
		this.cfg = config;
        this.publisherRepo = pubRepo;
        this.subscriberRepo = subRepo;
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
		aWriter.println("\r\Session currently connected: " + ((_connected) ? "true" : "false"));
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
		return "Session";
	}

	//------------------------------------------------------------------------
	/**
	 * Request the Session service to stop
	 */
	public void requestStop ()
	{
		this._stop = true;
		logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
	}

	//------------------------------------------------------------------------
	/**
	 * Process a connection request to the Session service.
	 *
	 * The Session service provides an interface for this instance.
	 *
	 */
	public void process(Socket aSocket) throws IOException
	{
		socket = aSocket;
		port = aSocket.getPort();
		address = aSocket.getInetAddress();
		int attempts = 0;
		int secattempts = 0;
		String line;
		String basePrompt = "";
		String prompt = basePrompt;
		boolean tout = false;

		// setup i/o
		aSocket.setSoTimeout(120000); // time-out so this thread does not hang server

		in = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(aSocket.getOutputStream()));

		_connected = true;

		if (! handshake())
        {
            _stop = true; // just hang-up on the connection
            logger.info("Connection to " + publisherRepo.getLibraryData().libraries.site + " failed handshake");
        }

		// prompt for & process interactive commands
		while (_stop == false)
		{
			try
			{
				// prompt the user for a command
				if (!tout)
				{
					out.print(prompt);
					out.flush();
				}
				tout = false;

//				byte[] b = in.
				line = in.readLine(); // get command from user
				if (line == null)
					break; // exit on EOF

				if (line.trim().length() < 1)
				{
					out.print("\r");
					continue;
				}

				// parse the command
				StringTokenizer t = new StringTokenizer(line);
				if (!t.hasMoreTokens())
					continue; // ignore if empty

				String theCommand = t.nextToken();

				// -------------- authorized level password -----------------
				if (theCommand.equalsIgnoreCase("auth"))
				{
					++attempts;
					String pw = "";
					if (t.hasMoreTokens())
						pw = t.nextToken(); // get the password
					if ((this.passwordEncrypted.length() == 0 && pw.length() == 0)) // || this.passwordEncrypted.equals(PasswordService.getInstance().encrypt(pw.trim())))
					{
						out.println("password accepted\r\n");
						authorized = true; // grant access
						prompt = "$ ";
						logger.info("Command auth accepted");
					}
					else
					{
						out.println("invalid password\r\n");
						logger.warn("Auth password attempt failed");
						if (attempts >= 3) // disconnect on too many attempts
						{
							logger.error("Too many Auth password failures, disconnecting");
							break;
						}
					}
					continue;
				}

				// -------------- logout ------------------------------------
				if (theCommand.equalsIgnoreCase("logout"))
				{
					if (secret == true)
					{
						secret = false;
						prompt = authorized ? "$ " : basePrompt;
						continue;
					}
					else if (authorized == true)
					{
						authorized = false;
						prompt = basePrompt;
						continue;
					}
					else
					{
						theCommand = "quit";
						// let the logic fall through to the 'quit' handler
					}
				}

				// -------------- quit, bye, exit ---------------------------
				if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("bye") || theCommand.equalsIgnoreCase("exit"))
				{
					out.println("\r\n" + theCommand);
					break; // break the loop
				}

				// -------------- secret level password ----------
				if (theCommand.equalsIgnoreCase("secret"))
				{
					++secattempts;
					String pw = "";
					if (t.hasMoreTokens())
						pw = t.nextToken(); // get the password
					if ((this.secretEncrypted.length() == 0 && pw.length() == 0)) // || this.secretEncrypted.equals(PasswordService.getInstance().encrypt(pw.trim())))
					{
						out.println("password accepted\r\n");
						secret = true; // grant secret access
						prompt = "! ";
						logger.info("Command secret accepted");
					}
					else
					{
						out.println("invalid password\r\n");
						logger.warn("Secret password attempt failed");
						if (secattempts >= 3) // disconnect on too many attempts
						{
							logger.error("Too many Secret failures, disconnecting");
							break;
						}
					}
					continue;
				}

				// -------------- status information ------------------------
				if (theCommand.equalsIgnoreCase("status"))
				{
					if (!authorized && !secret)
						out.println("error: not authorized\r\n");
					else
					{
						out.println("");
						CommManager.getInstance().dumpStatistics(out);
						dumpStatistics(out);
						out.println("");
					}
					continue;
				}

				// -------------- help! -------------------------------------
				if (theCommand.equalsIgnoreCase("help") || theCommand.equals("?"))
				{
					// @formatter:off
					out.println("\r\nAvailable commands, not case sensitive:\r\n");

					if (authorized == true || secret == true)
					{
						out.println(
								"  logout = to exit current level\r\n" +
								"  status = server and console status information $" +
							    "\r\n\r\n And:"
								);
					}

					out.println(
							"  help or ? = this list\r\n" +
							"  quit, bye, exit = disconnect\r\n" +
							"\r\nNote: Does not support backspace or command-line editing." +
						    "\r\n"
							);
				    // @formatter:on
					continue;
				}

				out.println("\r\nunknown command '" + theCommand + "', use 'help' for information\r\n");

			} // try
			catch (SocketTimeoutException e)
			{
				tout = true;
				continue;
			}
			catch (Exception e)
			{
				out.println(e);
				break;
			}
		} // while

		_connected = false;

		if (_stop == true)
		{
			out.println("\r\n\r\nSession is being shutdown and/or restarted, disconnecting\r\n");
		}

		// all done, close everything
		if (logger != null)
		{
			logger.info("Close connection on port " + port + " to " + address.getHostAddress());
		}
		out.close();
		in.close();
	}

    private boolean handshake()
    {
        boolean valid = false;
        try {
            out.println("HELO");
            out.flush();
            String input = read();
            if (input.equals("DribNit"))
            {
                out.println(new String (Utils.encrypt(subscriberRepo.getLibraryData().libraries.key, subscriberRepo.getLibraryData().libraries.key)));
                out.flush();
                input = read();
                if (input.equals(publisherRepo.getLibraryData().libraries.key))
                {
                    out.println("ACK");
                    out.flush();
                    logger.info("Session authenticated");
                    valid = true;
                }
            }
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return valid;
    }

    private String read()
    {
        String input = "";
        try {
            input = in.readLine();
            logger.debug("read: " + input);
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
        return input;
    }

} // Session

