package com.groksoft.volmonger.vio.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

import com.groksoft.volmonger.Configuration;

//----------------------------------------------------------------------------
/**
 * Session service.
 *
 */
public class Session
{
	private transient Logger logger = LogManager.getLogger("applog");


	static final String credFilename = "server-credentials.ser";

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

	//------------------------------------------------------------------------
	/**
	 * Instantiate the Session service
	 */
	public Session()
	{
		this.passwordClear = "";
		this.passwordEncrypted = "";
		this.secretClear = "";
		this.secretEncrypted = "";
	} // constructor

	//------------------------------------------------------------------------
	/**
	 * Dump statistics from all available internal sources.
	 *
	 */
	public synchronized void dumpStatistics (PrintWriter aWriter)
	{
		aWriter.println("\r\nSession currently connected: " + ((_connected) ? "true" : "false"));
		aWriter.println("  Connected on port: " + port);
		aWriter.println("  Connected to: " + address);
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
	 * Get the passwords for the Session
	 */
	private boolean getPasswords ()
	{
		boolean flag = false;
		return flag;
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
	 * Save the passwords
	 */
	private boolean savePasswords ()
	{
		boolean flag = false;
		return flag;
	}


	//------------------------------------------------------------------------
	/**
	 * Serve a connection request to the Session service.
	 *
	 * The Session service provides a manual interface for monitor and command
	 * this server.
	 *
	 */
	public void serve (Socket aSocket) throws IOException
	{
		socket = aSocket;
		port = aSocket.getPort();
		address = aSocket.getInetAddress();
		int attempts = 0;
		int secattempts = 0;
		String line;
		String basePrompt = "> ";
		String prompt = basePrompt;
		boolean tout = false;

		// setup i/o
		aSocket.setSoTimeout(10000); // time-out so this thread does not hang server

		in = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
		out = new PrintWriter(new OutputStreamWriter(aSocket.getOutputStream()));

		// hello
		out.println("\r\nVersion " + Configuration.getVOLMONGER_VERSION());
		out.println("There are " + ConnectionManager.getInstance().getAllConnections().size() + " active Session connections");
		out.println("Enter 'help' or '?' for list of commands.");

		getPasswords();
		_connected = true;

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
					if ((this.passwordEncrypted.length() == 0 && pw.length() == 0) || this.passwordEncrypted.equals(pw.trim()))
					{
						out.println("password accepted\r\n");
						authorized = true; // grant access
						prompt = "$ ";
						logger.info("Command password accepted");
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

				// -------------- password change for current level ---------
				if (theCommand.equalsIgnoreCase("password"))
				{
					if (!authorized && !secret)
						out.println("error: not authorized\r\n");
					else
					{
						String pw = "";
						if (t.hasMoreTokens())
							pw = t.nextToken(); // get the password
						if (pw.length() > 0)
						{
							if (this.secret == true)
							{
								this.secretClear = pw.trim();
							}
							else if (this.authorized == true)
							{
								this.passwordClear = pw.trim();
							}
							savePasswords();
							out.println("password changed and saved\r\n");
							logger.info((this.secret ? "Secret" : "Authorized") + " password changed and saved");
						}
						else
						{
							out.println("error: no password specified\r\n");
						}
					}
					continue;
				}

				// -------------- quit, bye, exit ---------------------------
				if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("bye") || theCommand.equalsIgnoreCase("exit"))
				{
					out.println("\r\n" + theCommand);
					break; // break the loop
				}

				// -------------- secret level password (FCS-only) ----------
				if (theCommand.equalsIgnoreCase("secret"))
				{
					++secattempts;
					String pw = "";
					if (t.hasMoreTokens())
						pw = t.nextToken(); // get the password
					if ((this.secretEncrypted.length() == 0 && pw.length() == 0) || this.secretEncrypted.equals(pw.trim()))
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
						ConnectionManager.getInstance().dumpStatistics(out);
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

				    if (secret == true)
					{
						out.println(
								"  secret <password> = switch to secret level !" +
							    "\r\n\r\n And:"
								);
					}

					if (authorized == true || secret == true)
					{
						out.println(
								"  password <password> = changes password for level $\r\n" +
								"  resetAdmin = logout any existing Admin Client login $\r\n" +
								"  status = server and console status information $" +
							    "\r\n\r\n And:"
								);
					}

					out.println(
							"  auth <password> = switch to authorized level\r\n" +
							"  logout = to exit current level\r\n" +
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
			out.println("\r\n\r\nShutdown and/or restart, disconnecting\r\n");
		}

		// all done, close everything
		if (logger != null)
		{
			logger.info("Close connection on port " + port + " to " + address);
		}
		out.close();
		in.close();
	}

} // Session

