package com.groksoft.els.stty.hintServer;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.Transfer;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.stty.DaemonBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

/**
 * Hint Status Server Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
@SuppressWarnings("Duplicates")
public class Daemon extends DaemonBase
{
    protected static Logger logger = LogManager.getLogger("applog");

    private HintKeys.HintKey connectedKey;
    private Context context;
    private boolean fault = false;
    private Hints hints = null;
    private boolean isTerminal = false;

    /**
     * Instantiate the Daemon service
     *
     * @param config
     * @param ctxt
     */
    public Daemon(Configuration config, Context ctxt, Repository mine, Repository theirs)
    {
        super(config, mine, theirs);
        context = ctxt;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     */
    public synchronized String dumpStatistics()
    {
        String data = "\r\nConsole currently connected: " + ((connected) ? "true" : "false") + "\r\n";
        data += "  Connected on port: " + port + "\r\n";
        data += "  Connected to: " + address + "\r\n";
        return data;
    } // dumpStatistics

    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName()
    {
        return "Daemon";
    } // getName

    /**
     * Get the next available token trimmed
     *
     * @param t StringTokenizer
     * @return Next token or an empty string
     */
    private String getNextToken(StringTokenizer t)
    {
        String value = "";
        if (t.hasMoreTokens())
        {
            value = t.nextToken();
            if (value.trim().length() == 0 && t.hasMoreTokens())
                value = t.nextToken();
        }
        return value;
    }

    /**
     * Special handshake using hints keys file instead of point-to-point
     *
     * @return String name of back-up system
     */
    public String handshake()
    {
        String system = "";
        boolean valid = false;
        try
        {
            Utils.writeStream(out, myKey, "HELO");

            String input = Utils.readStream(in, myKey);
            if (input.equals("DribNit") || input.equals("DribNlt"))
            {
                isTerminal = input.equals("DribNit");
                if (isTerminal)  // terminal not allowed for hint status server
                {
                    Utils.writeStream(out, myKey, "Terminal session not allowed");
                    return system; // empty
                }
                Utils.writeStream(out, myKey, myKey);

                input = Utils.readStream(in, myKey);
                connectedKey = context.hintKeys.findKey(input);  // look for matching key in hints keys file
                if (connectedKey != null)
                {
                    // send my flavor
                    Utils.writeStream(out, myKey, myRepo.getLibraryData().libraries.flavor);

                    system = connectedKey.name;
                    logger.info("Authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                    valid = true;
                }
            }
        }
        catch (Exception e)
        {
            fault = true;
            logger.error(e.getMessage());
        }
        return system;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    public boolean process(Socket aSocket) throws Exception
    {
        socket = aSocket;
        port = aSocket.getPort();
        address = aSocket.getInetAddress();
        int attempts = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        boolean tout = false;

        // Get ELS hints keys
        hints = new Hints(cfg, context, context.hintKeys);
        context.transfer = new Transfer(cfg, context);

        // setup i/o
        aSocket.setSoTimeout(120000); // time-out so this thread does not hang server

        in = new DataInputStream(aSocket.getInputStream());
        out = new DataOutputStream(aSocket.getOutputStream());

        connected = true;

        String system = handshake();
        if (system.length() == 0) // special handshake using hints keys file instead of point-to-point
        {
            logger.error("Connection to incoming request failed handshake");
        }
        else
        {
            if (isTerminal)  // terminal not allowed to hint status server in handshake()
            {
                response = "Enter 'help' for information\r\n"; // "Enter " checked in ClientStty.checkBannerCommands()
            }
            else // is automation
            {
                response = "CMD";
            }

            // prompt for & process interactive commands
            while (stop == false)
            {
                try
                {
                    // prompt the user for a command
                    if (!tout)
                    {
                        try
                        {
                            Utils.writeStream(out, myKey, response + (isTerminal ? prompt : ""));
                        }
                        catch (Exception e)
                        {
                            logger.info("Client appears to have disconnected");
                            break;
                        }
                    }
                    tout = false;
                    response = "";

                    line = readStream(in, myKey);  // special readStream() variant for continuous server
                    if (line == null)
                    {
                        // break read loop and let the connection be closed
                        break;
                    }

                    if (line.trim().length() < 1)
                    {
                        response = "\r";
                        continue;
                    }

                    logger.info("Processing command: " + line + " from: " + system + ", " + Utils.formatAddresses(getSocket()));

                    // parse the command
                    StringTokenizer t = new StringTokenizer(line, "\"");
                    if (!t.hasMoreTokens())
                        continue; // ignore if empty

                    String theCommand = t.nextToken().trim();

                    // -------------- get status --------------------------------
                    if (theCommand.equalsIgnoreCase("get"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            String itemLib = getNextToken(t);
                            String itemPath = getNextToken(t);
                            String systemName = getNextToken(t);
                            String defaultStatus = getNextToken(t);
                            if (itemLib.length() > 0 && itemPath.length() > 0 && systemName.length() > 0 && defaultStatus.length() > 0)
                            {
                                valid = true;
                                response = context.datastore.getStatus(itemLib, itemPath, systemName, defaultStatus);
                                logger.info("  > get response: " + response);
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- logout ------------------------------------
                    if (theCommand.equalsIgnoreCase("logout"))
                    {
                        if (authorized)
                        {
                            authorized = false;
                            prompt = basePrompt;
                            continue;
                        }
                        else
                        {
                            // break read loop and let the connection be closed
                            break;
                        }
                    }

                    // -------------- quit, bye, exit ---------------------------
                    if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("bye") || theCommand.equalsIgnoreCase("exit"))
                    {
                        //Utils.writeStream(out, myKey, "End-Execution");
                        stop = true;
                        break; // break the loop
                    }

                    // -------------- set status --------------------------------
                    if (theCommand.equalsIgnoreCase("set"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            String itemLib = getNextToken(t);
                            String itemPath = getNextToken(t);
                            String systemName = getNextToken(t);
                            String status = getNextToken(t);
                            if (itemLib.length() > 0 && itemPath.length() > 0 && systemName.length() > 0 && status.length() > 0)
                            {
                                valid = true;
                                response = context.datastore.setStatus(itemLib, itemPath, systemName, status);
                                logger.info("  > set response: " + response);
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    response = "\r\nunknown command '" + theCommand + "\r\n";

                } // try
                catch (Exception e)
                {
                    fault = true;
                    connected = false;
                    stop = true;
                    logger.error(Utils.getStackTrace(e));
                    try
                    {
                        Utils.writeStream(out, myKey, e.getMessage());
                    }
                    catch (Exception ex)
                    {
                    }
                    break;
                }
            }
        }
        return stop;
    } // process

    /**
     * Continuous Server - Read an encrypted data stream, return decrypted string
     * <p>
     * Special variation for operating a continuous server that does not shutdown
     * if the connection is broken.
     *
     * @param in  DataInputStream to read, e.g. remote connection
     * @param key UUID key to decrypt the data stream
     * @return String read from stream; null if connection is closed
     */
    public String readStream(DataInputStream in, String key) throws Exception
    {
        byte[] buf = {};
        String input = "";
        while (true)
        {
            try
            {
                int count = in.readInt();
                int pos = 0;
                if (count > 0)
                {
                    buf = new byte[count];
                    int remaining = count;
                    while (true)
                    {
                        int readCount = in.read(buf, pos, remaining);
                        if (readCount < 0)
                            break;
                        pos += readCount;
                        remaining -= readCount;
                        if (pos == count)
                            break;
                    }
                    if (pos != count)
                    {
                        logger.warn("Read counts do not match, expected " + count + ", received " + pos);
                    }
                }
                break;
            }
            catch (SocketTimeoutException e)
            {
                continue;
            }
            catch (EOFException e)
            {
                input = null;
                break;
            }
            catch (IOException e)
            {
                if (e.getMessage().toLowerCase().contains("connection reset"))
                {
                    logger.info("Connection closed by client");
                    input = null;
                    break;
                }
                else // do not throw on disconnect
                    throw e;
            }
        }
        if (buf.length > 0)
            input = Utils.decrypt(key, buf);
        return input;
    }

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("Requesting stop for stty session on: " + Utils.formatAddresses(socket));
    } // requestStop

} // Daemon
