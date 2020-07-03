package com.groksoft.volmunger.stty.publisher;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.stty.ServeStty;
import com.groksoft.volmunger.stty.DaemonBase;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Publisher Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
public class Daemon extends DaemonBase
{
    protected static Logger logger = LogManager.getLogger("applog");

    private boolean isTerminal = false;

    /**
     * Instantiate the Daemon service
     *
     * @param config
     * @param pubRepo
     * @param subRepo
     */

    public Daemon(Configuration config, Repository pubRepo, Repository subRepo) {
        super(config, pubRepo, subRepo);
    } // constructor


    /**
     * Dump statistics from all available internal sources.
     */
    public synchronized String dumpStatistics() {
        String data = "\r\nDaemon currently connected: " + ((connected) ? "true" : "false") + "\r\n";
        data += "  Connected on port: " + port + "\r\n";
        data += "  Connected to: " + address + "\r\n";
        return data;
    } // dumpStatistics

    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName() {
        return "Daemon";
    } // getName

    public boolean handshake() {
        boolean valid = false;
        try {
            Utils.write(out, publisherKey, "HELO");

            String input = Utils.read(in, publisherKey);
            if (input.equals("DribNit") || input.equals("DribNlt")) {
                isTerminal = input.equals("DribNit");
                // make sure subscriber terminal access is allowed
                if (isTerminal && publisherRepo.getLibraryData().libraries.terminal_allowed.equalsIgnoreCase("true")) {
					Utils.write(out, publisherKey, publisherKey);

					input = Utils.read(in, publisherKey);
					if (input.equals(subscriberKey)) {
						Utils.write(out, publisherKey, "ACK");

						logger.info("Authenticated: " + subscriberRepo.getLibraryData().libraries.description);
						valid = true;
					}
				}
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        return valid;
    } // handshake

    /**
     * Process a connection request to the Daemon service.
     * <p>
     * The Daemon service provides an interface for this instance.
     */
    @SuppressWarnings("Duplicates")
    public void process(Socket aSocket) throws IOException {
        socket = aSocket;
        port = aSocket.getPort();
        address = aSocket.getInetAddress();
        int attempts = 0;
        int secattempts = 0;
        String line;
        String basePrompt = ": ";
        String prompt = basePrompt;
        boolean tout = false;

        // setup i/o
        aSocket.setSoTimeout(120000); // time-out so this thread does not hang server

        in = new DataInputStream(aSocket.getInputStream());
        out = new DataOutputStream(aSocket.getOutputStream());

        connected = true;

        if (!handshake()) {
            stop = true; // just hang-up on the connection
            logger.info("Connection to " + publisherRepo.getLibraryData().libraries.site + " failed handshake");
        }

        response = "Enter 'help' for information\r\n";

        // prompt for & process interactive commands
        while (stop == false) {
            try {
                // prompt the user for a command
                if (!tout) {
                    Utils.write(out, publisherKey, response + (isTerminal ? prompt : ""));
                }
                tout = false;
                response = "";

                line = Utils.read(in, publisherKey);
                if (line == null) {
                    stop = true;
                    break; // exit on EOF
                }

                if (line.trim().length() < 1) {
                    response = "\r";
                    continue;
                }

                // parse the command
                StringTokenizer t = new StringTokenizer(line);
                if (!t.hasMoreTokens())
                    continue; // ignore if empty

                String theCommand = t.nextToken();

                // -------------- authorized level password -----------------
                if (theCommand.equalsIgnoreCase("auth")) {
                    ++attempts;
                    String pw = "";
                    if (t.hasMoreTokens())
                        pw = t.nextToken(); // get the password
                    if ((cfg.getAuthorizedPassword().length() == 0 && pw.length() == 0) ||
                            cfg.getAuthorizedPassword().equals(pw.trim()))
                    {
                        response = "password accepted\r\n";
                        authorized = true;
                        prompt = "$ ";
                        logger.info("Command auth accepted");
                    } else {
                        logger.warn("Auth password attempt failed using: " + pw);
                        if (attempts >= 3) // disconnect on too many attempts
                        {
                            logger.error("Too many failures, disconnecting");
                            break;
                        }
                    }
                    continue;
                }

                // -------------- logout ------------------------------------
                if (theCommand.equalsIgnoreCase("logout")) {
                    if (authorized) {
                        authorized = false;
                        prompt = basePrompt;
                        continue;
                    } else {
                        theCommand = "quit";
                        // let the logic fall through to the 'quit' handler below
                    }
                }

                // -------------- quit, bye, exit ---------------------------
                if (theCommand.equalsIgnoreCase("quit") || theCommand.equalsIgnoreCase("bye") || theCommand.equalsIgnoreCase("exit")) {
                    Utils.write(out, publisherKey, "\r\n" + theCommand);
                    stop = true;
                    break; // break the loop
                }

                // -------------- available disk space ----------------------
                if (theCommand.equalsIgnoreCase("space"))
                {
                    String location = "";
                    if (t.hasMoreTokens()) {
                        location = t.nextToken();
                        long space = Utils.availableSpace(location);
                        if (isTerminal) {
                            response = Utils.formatLong(space);
                        } else {
                            response = String.valueOf(space);
                        }
                    } else {
                        response = (isTerminal ? "space command requires a location\r\n" : "0");
                    }
                    continue;
                }

                // -------------- status information ------------------------
                if (theCommand.equalsIgnoreCase("status")) {
                    if (!authorized) {
                        response = "not authorized\r\n";
                    } else {
                        response = ServeStty.getInstance().dumpStatistics();
                        response += dumpStatistics();
                    }
                    continue;
                }

                // -------------- help! -------------------------------------
                if (theCommand.equalsIgnoreCase("help") || theCommand.equals("?")) {
                    // @formatter:off
                    response = "\r\nAvailable commands, not case sensitive:\r\n";

                    if (authorized) {
                        response += "  logout = to exit current level\r\n" +
                                "  status = server and console status information" +
                                "\r\n\r\n And:\r\n";
                    }

                    response += "  help or ? = this list\r\n" +
                            "  quit, bye, exit = disconnect\r\n" +
                            "\r\n";
                    // @formatter:on
                    continue;
                }

                response = "\r\nunknown command '" + theCommand + "', use 'help' for information\r\n";

            } // try
            catch (Exception e) {
                Utils.write(out, publisherKey, e.getMessage());
                break;
            }
        } // while

        connected = false;

        if (stop) {
            Utils.write(out, publisherKey, "\r\n\r\nSession is disconnecting\r\n");
        }

        // all done, close everything
        if (logger != null) {
            logger.info("Close connection on port " + port + " to " + address.getHostAddress());
        }
        out.close();
        in.close();
    } // process

    /**
     * Request the Daemon service to stop
     */
    public void requestStop() {
        this.stop = true;
        logger.info("Requesting stop for session on port " + socket.getPort() + " to " + socket.getInetAddress());
    } // requestStop

} // Daemon
