package com.corionis.els.stty.hintServer;

import com.corionis.els.repository.HintKey;
import com.corionis.els.repository.Hints;
import com.corionis.els.repository.Repository;
import com.corionis.els.stty.AbstractDaemon;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.repository.HintKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.StringTokenizer;

/**
 * Hint Status Server Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
@SuppressWarnings("Duplicates")
public class Daemon extends AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

    private boolean fault = false;
    private boolean isTerminal = false;

    /**
     * Instantiate the Daemon service
     *
     * @param context  The Context
     */
    public Daemon(Context context, Repository mine, Repository theirs)
    {
        super(context, mine, theirs);
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
     * Handshake using hints keys file (only) instead of point-to-point
     *
     * @return String name of back-up system
     */
    public String handshake()
    {
        String system = "";
        try
        {
            send("HELO", "");

            String input = receive("", 5000);
            if (input.equals("DribNit") || input.equals("DribNlt"))
            {
                isTerminal = input.equals("DribNit");
                if (isTerminal)  // terminal not allowed for hint status server
                {
                    send("Terminal session not allowed", "");
                    return system; // empty
                }
                send(myKey, "");

                input = receive("", 5000);
                // validate with Hint Keys
                HintKey connectedKey = context.hintKeys.findKey(input);  // look for matching key in hints keys file
                if (connectedKey != null)
                {
                    // send my flavor
                    send(myRepo.getLibraryData().libraries.flavor, "");

                    system = connectedKey.name;
                    logger.info("Authenticated automated session: " + system);
                    context.fault = false;
                    context.timeout = false;
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
    public boolean process() throws Exception
    {
        int attempts = 0;
        int commandCount = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;

        port = getSocket().getPort();
        address = getSocket().getInetAddress();

        // Get ELS hints keys & Tracker if specified
        if (context.cfg.getHintKeysFile().length() > 0)
        {
            context.hintKeys = new HintKeys(context);
            context.hintKeys.read(context.cfg.getHintKeysFile());
            context.hints = new Hints(context, context.hintKeys);
        }

        // setup i/o
        getSocket().setKeepAlive(true); // keep alive to avoid time-out

        in = new DataInputStream(getSocket().getInputStream());
        out = new DataOutputStream(getSocket().getOutputStream());

        connected = true;

        String system = handshake();
        if (system.length() == 0) // special handshake using hints keys file instead of point-to-point
        {
            logger.error("Connection to " + Utils.formatAddresses(socket) + " failed handshake");
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
            boolean isPing = false;
            while (stop == false)
            {
                try
                {
                    // prompt the user for a command
                    if (context.fault || context.timeout)
                    {
                        fault = true;
                        stop = true;
                        logger.warn("process fault, ending stty");
                        break;
                    }

                    // send response or prompt the user for a command
                    if (!isPing)
                    {
                        String log = context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace") ? "writing response " + response.length() + " bytes to " + system : "";
                        send(response + (isTerminal ? prompt : ""), log);
                    }
                    response = "";

                    line = readStream(in, myKey);  // special readStream() variant for continuous server
                    if (line == null)
                    {
                        break; // break read loop and let the connection be closed
                    }

                    isPing = false;
                    if (line.startsWith("ping"))
                    {
                        isPing = true;
                        logger.trace("heartbeat received from " + system);
                        continue;
                    }

                    if (line.trim().length() < 1)
                    {
                        response = "\r";
                        continue;
                    }

                    ++commandCount;
                    logger.info("Processing command: " + line + " from: " + system + ", " + Utils.formatAddresses(getSocket()));

                    // parse the command
                    StringTokenizer t = new StringTokenizer(line, "\"");
                    if (!t.hasMoreTokens())
                        continue; // ignore if empty

                    String theCommand = t.nextToken().trim();

                    // -------------- bye ---------------------------------------
                    if (theCommand.equalsIgnoreCase("bye"))
                    {
                        break;  // let the connection close
                    }

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
                            theCommand = "quit";
                            // let the logic fall through to the 'quit' handler below
                        }
                    }

                    // -------------- quit, exit --------------------------------
                    if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("exit"))
                    {
                        out.flush();
                        Thread.sleep(3000);

                        // if this is the first command or keep going is not enabled then stop
                        if (commandCount == 1 || !context.cfg.isKeepGoing())
                            stop = true;
                        else
                            logger.info("Ignoring quit command, --listener-keep-going enabled");
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
                catch (SocketTimeoutException toe)
                {
                    context.timeout = true;
                    fault = true;
                    connected = false;
                    stop = true;
                    logger.error("SocketTimeoutException: " + Utils.getStackTrace(toe));
                    break;
                }
                catch (SocketException se)
                {
                    if (se.toString().contains("timed out"))
                        context.timeout = true;
                    fault = true;
                    connected = false;
                    stop = true;
                    logger.debug("SocketException, timeout is: " + context.timeout);
                    logger.error(Utils.getStackTrace(se));
                    break;
                }
                catch (Exception e)
                {
                    fault = true;
                    connected = false;
                    stop = true;
                    logger.error(Utils.getStackTrace(e));
                    try
                    {
                        if (!context.timeout)
                        {
                            send(e.getMessage(), "Hint Server exception");
                            Thread.sleep(3000);
                        }
                    }
                    catch (Exception ex)
                    {
                    }
                    break;
                }
            }
        }
        if (fault)
            context.fault = true;
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
                        logger.warn("read counts do not match, expected " + count + ", received " + pos);
                    }
                }
                break;
            }
            catch (SocketTimeoutException e)
            {
                continue; // ignore the time-out on a read
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
                    logger.warn("connection closed by client");
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
        logger.debug("requesting stop for stty session on: " + Utils.formatAddresses(socket));
    } // requestStop

} // Daemon
