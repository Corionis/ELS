package com.corionis.els.stty.hintServer;

import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.repository.Repository;
import com.corionis.els.stty.AbstractDaemon;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
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
                // validate with Authentication Keys if specified
                if (context.authKeys != null)
                {
                    HintKey connectedKey = context.authKeys.findKey(input);  // look for matching key in hints keys file
                    if (connectedKey != null)
                    {
                        // send my flavor
                        send(myRepo.getLibraryData().libraries.flavor, "");

                        system = connectedKey.system;
                        logger.info("Hint Server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
                        context.fault = false;
                        context.timeout = false;
                    }
                }
                else if (input.equals(theirRepo.getLibraryData().libraries.key)) // otherwise validate point-to-point
                {
                    // send my flavor
                    send(myRepo.getLibraryData().libraries.flavor, "");

                    system = theirRepo.getLibraryData().libraries.description;
                    logger.info("Hint Server authenticated " + (isTerminal ? "terminal" : "automated") + " session: " + system);
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
        int commandCount = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;

        // Get ELS Authentication Keys if specified
        try
        {
            if (context.cfg.getAuthKeysFile().length() > 0)
            {
                context.authKeys = new HintKeys(context);
                context.authKeys.read(context.cfg.getAuthKeysFile());
            }
        }
        catch (Exception e)
        {
            context.fault = true;
            throw e;
        }

        port = getSocket().getPort();
        address = getSocket().getInetAddress();

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

            // setup JSON handlers
            Gson gsonBuilder = new GsonBuilder().create();
            Gson gsonParser = new Gson();

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

                    line = line.trim();
                    if (line.length() < 1)
                    {
                        response = "\r";
                        continue;
                    }

                    ++commandCount;
                    logger.info("Processing command: " + line + ", from: " + system); // + ", " + Utils.formatAddresses(getSocket()));

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

                    // -------------- conflict ---------------------------------------
                    if (theCommand.equalsIgnoreCase("conflict"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            String lib = getNextToken(t);
                            String itemPath = getNextToken(t);
                            ArrayList<Hint> results = context.hints.checkConflicts(lib, itemPath);
                            if (results != null && results.size() > 0)
                            {
                                String json = gsonBuilder.toJson(results);
                                if (json != null && json.length() > 0)
                                {
                                    valid = true;
                                    response = json;
                                }
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- count ---------------------------------------
                    if (theCommand.equalsIgnoreCase("count"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            String who = getNextToken(t);
                            int count = context.datastore.count(who);
                            response = Integer.toString(count);
                            valid = true;
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- get hint(s) --------------------------------
                    if (theCommand.equalsIgnoreCase("get"))
                    {
                        String json = "";
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            String mode = getNextToken(t);

                            if (mode.toLowerCase().equals("all"))
                            {
                                ArrayList<Hint> hints = context.datastore.getAll(null, mode);
                                if (hints != null)
                                {
                                    json = gsonBuilder.toJson(hints);
                                }
                            }
                            else if (mode.toLowerCase().equals("for"))
                            {
                                if (t.hasMoreTokens())
                                {
                                    String hintSystemName = getNextToken(t);
                                    ArrayList<Hint> results = context.datastore.getFor(hintSystemName);
                                    if (results != null)
                                        json = gsonBuilder.toJson(results);
                                }
                            }
                            else
                            {
                                int start = 4 + mode.length() + 2;
                                if (line.length() > start)
                                {
                                    json = line.substring(start);
                                    Hint hint = gsonParser.fromJson(json, Hint.class);
                                    hint = context.datastore.get(hint, mode);
                                    if (hint != null)
                                    {
                                        json = gsonBuilder.toJson(hint);
                                    }
                                    else
                                        json = null;
                                }
                            }
                            if (json != null && json.length() > 0)
                            {
                                valid = true;
                                response = json;
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- save all --------------------------------
                    if (theCommand.equalsIgnoreCase("save"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            if (line.length() > 7)
                            {
                                String json = line.substring(7);
                                Type listType = new TypeToken<ArrayList<Hint>>()
                                {
                                }.getType();
                                context.datastore.hints = gsonParser.fromJson(json, listType);
                                context.datastore.write();
                                valid = true;
                                response = "true";
                                logger.info("All Hints saved");
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- write or update Hint --------------------------------
                    if (theCommand.equalsIgnoreCase("hint"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            if (line.length() > 7)
                            {
                                String json = line.substring(7);
                                Hint hint = gsonParser.fromJson(json, Hint.class);
                                context.hints.writeOrUpdateHint(hint, null);
                                valid = true;
                                response = "true";
                                logger.info("Hint updated: " + hint.getLocalUtc(context));
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
