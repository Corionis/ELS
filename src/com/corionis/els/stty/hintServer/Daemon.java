package com.corionis.els.stty.hintServer;

import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.repository.Repository;
import com.corionis.els.stty.AbstractDaemon;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.stty.ServeStty;
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
import java.text.MessageFormat;
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
    private ServeStty instance = null;

    /**
     * Instantiate the Daemon service
     *
     * @param instance The ServeStty
     * @param context  The Context
     * @param mine My Repository
     * @param theirs Their Repository
     */
    public Daemon(ServeStty instance, Context context, Repository mine, Repository theirs)
    {
        super(context, mine, theirs);
        this.instance = instance;
    } // constructor

    /**
     * Dump statistics from all available internal sources.
     */
    public synchronized String dumpStatistics()
    {
        String data = context.cfg.gs("Stty.r.nconsole.currently.connected") + ((connected) ? "true" : "false") + "\r\n";
        data += context.cfg.gs("Stty.connected.on.port") + port + "\r\n";
        data += context.cfg.gs("Stty.connected.to.address") + address + "\r\n";
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
     * Handshake using hint keys file (only) instead of point-to-point
     *
     * @return String name of back-up system
     */
    public String handshake()
    {
        String system = "";
        try
        {
            logger.trace(context.cfg.gs("Stty.hint.server.listener.handshake"));
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
                        logger.info(MessageFormat.format(context.cfg.gs("Stty.hint.server.authenticated.choice.terminal.automated.session"), isTerminal ? 0 : 1) + system);
                        context.fault = false;
                        context.timeout = false;
                    }
                    else
                    {
                        logger.error(context.cfg.gs("Stty.hint.server.cannot.find.key") + input);
                    }
                }
                else if (theirRepo != null && input.equals(theirRepo.getLibraryData().libraries.key)) // otherwise validate point-to-point
                {
                    // send my flavor
                    send(myRepo.getLibraryData().libraries.flavor, "");

                    system = theirRepo.getLibraryData().libraries.description;
                    logger.info(MessageFormat.format(context.cfg.gs("Stty.hint.server.authenticated.choice.terminal.automated.session"), isTerminal ? 0 : 1) + system);
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
    public int process() throws Exception
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
            if (!context.cfg.isKeepGoing())
                status = 1;
            logger.error(MessageFormat.format(context.cfg.gs("Stty.connection.to.failed.handshake"), Utils.formatAddresses(socket)));
        }
        else
        {
            if (isTerminal)  // terminal not allowed to hint status server in handshake()
            {
                response = context.cfg.gs("Stty.enter.help.for.information.r.n"); // "Enter " checked in ClientStty.checkBannerCommands()
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
            while (status == 0)
            {
                try
                {
                    // prompt the user for a command
                    if (context.fault || context.timeout)
                    {
                        fault = true;
                        status = 1;
                        logger.warn(context.cfg.gs("Stty.process.fault.ending.stty"));
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
                        if (!context.cfg.isKeepGoing())
                        {
                            fault = true; // exit on EOF
                            status = 2;
                            logger.warn(context.cfg.gs("Stty.eof.line.process.ended.prematurely"));
                        }
                        else
                            logger.info(context.cfg.gs("Stty.eof.line.listener.keep.going.enabled"));
                        break; // break read loop and let the connection be closed
                    }

                    isPing = false;
                    if (line.startsWith("ping"))
                    {
                        isPing = true;
                        logger.trace(context.cfg.gs("Stty.heartbeat.received") + system);
                        continue;
                    }

                    line = line.trim();
                    if (line.length() < 1)
                    {
                        response = "\r";
                        continue;
                    }

                    ++commandCount;
                    logger.info(MessageFormat.format(context.cfg.gs("Stty.processing.command.from"), line) + system); // + ", " + Utils.formatAddresses(getSocket()));

                    // parse the command
                    StringTokenizer t = new StringTokenizer(line, "\"");
                    if (!t.hasMoreTokens())
                        continue; // ignore if empty

                    String theCommand = t.nextToken().trim();

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
                            if (context.cfg.isKeepGoing())
                                theCommand = "bye";
                            else
                                theCommand = "quit";
                        }
                    }

                    // -------------- bye ---------------------------------------
                    if (theCommand.equalsIgnoreCase("bye"))
                    {
                        out.flush();
                        Thread.sleep(1500);
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
                            ArrayList<Hint> results = context.hintsHandler.checkConflicts(lib, itemPath);
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

                    // -------------- directory ------------------------
                    if (theCommand.equalsIgnoreCase("directory"))
                    {
                        response = context.cfg.getWorkingDirectory();
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

                    // -------------- Hint write or update --------------------------------
                    if (theCommand.equalsIgnoreCase("hint"))
                    {
                        boolean valid = false;
                        if (t.hasMoreTokens())
                        {
                            if (line.length() > 7)
                            {
                                String json = line.substring(7);
                                Hint hint = gsonParser.fromJson(json, Hint.class);
                                context.hintsHandler.writeOrUpdateHint(hint, null);
                                valid = true;
                                response = "true";
                                logger.info(context.cfg.gs("Stty.hint.updated") + hint.getLocalUtc(context));
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- quit, exit --------------------------------
                    if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("exit"))
                    {
                        out.flush();
                        Thread.sleep(1500);

                        // if keep going is not enabled then stop
                        if (context.cfg.isKeepGoing())
                            logger.info(context.cfg.gs("Stty.ignoring.quit.command.listener.keep.going.enabled"));
                        else
                            status = 1;
                        break; // break the loop
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
                                logger.info(context.cfg.gs("Stty.all.hints.saved"));
                            }
                        }
                        if (!valid)
                            response = "false";
                        continue;
                    }

                    // -------------- stop --------------------------------
                    if (theCommand.equalsIgnoreCase("stop"))
                    {
                        out.flush();
                        Thread.sleep(1500);
                        status = 2;
                        break; // break the loop
                    }

                    response = context.cfg.gs("Stty.r.nunknown.command") + theCommand + "'\r\n";

                } // try
                catch (SocketTimeoutException toe)
                {
                    context.timeout = true;
                    fault = true;
                    connected = false;
                    status = 1;
                    logger.error(context.cfg.gs("Stty.sockettimeoutexception") + Utils.getStackTrace(toe));
                    break;
                }
                catch (SocketException se)
                {
                    if (se.toString().contains("timed out"))
                        context.timeout = true;
                    fault = true;
                    connected = false;
                    status = 1;
                    logger.debug(context.cfg.gs("Stty.socketexception.timeout.is") + context.timeout);
                    logger.error(Utils.getStackTrace(se));
                    break;
                }
                catch (Exception e)
                {
                    fault = true;
                    connected = false;
                    status = 1;
                    logger.error(Utils.getStackTrace(e));
                    try
                    {
                        if (!context.timeout)
                        {
                            send(e.getMessage(), context.cfg.gs("Stty.hint.server.exception"));
                            Thread.sleep(1500);
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
        String statMsg = status == 0 ? "Success" : (status == 1 ? "Quit" : "Stop");
        logger.trace(context.cfg.gs("Stty.hint.server.session.done.status") + statMsg + ", fault = " + context.fault);
        return status;
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
                        logger.warn(MessageFormat.format(context.cfg.gs("Stty.read.counts.do.not.match.expected.received"), count,pos));
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
                    logger.warn(context.cfg.gs("Stty.connection.closed.by.client"));
                    input = null;
                    break;
                }
                else // do not throw on disconnect
                    throw e;
            }
        }
        if (buf.length > 0)
            input = context.main.decrypt(key, buf);
        return input;
    }

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        status = 1;
        logger.debug(context.cfg.gs("Stty.requesting.stop.for.stty.session") + Utils.formatAddresses(socket));
    } // requestStop

} // Daemon
