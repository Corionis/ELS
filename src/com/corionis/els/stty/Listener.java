package com.corionis.els.stty;

import com.corionis.els.Configuration;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;

/**
 * Listen for a connection request for a service.
 * <p>
 * The Listener class is a subclass of Thread. It listens for connections
 * on a specified port. When a connection is requested it is handled by the
 * ConnectionManager. There is one Listener for each service on a specified
 * port.
 */
public class Listener extends Thread
{
    protected static Logger logger = LogManager.getLogger("applog");//
    private InetAddress addr;
    private Configuration cfg;
    private Context context;
    private ServeStty instance = null;

    /**
     * The socket to listen on for the associated service
     */
    private ServerSocket listenSocket;

    /**
     * The port to listen on for the associated service
     */
    private int port;

    /**
     * Flag used to determine when to stop listening
     */
    private boolean stop = false;

    /**
     * Setup a new Listener on a specified port.
     * <p>
     * The socket is set with a timeout to allow accept() to be interrupted, and
     * the service to be removed from the server.
     *
     * @param group The thread group used for the listener.
     * @param instance The ServeStty instance using this listener.
     * @param host The hostname
     * @param aPort The port to listen on.
     * @param ctxt The Context
     */
    public Listener(ThreadGroup group, ServeStty instance, String host, int aPort, Context ctxt) throws Exception
    {
        super(group, "listener:" + host + ":" + aPort);

        // setup this listener
        this.instance = instance;
        addr = Inet4Address.getByName(host);
        this.port = aPort;
        this.context = ctxt;
        this.cfg = ctxt.cfg;

        // create server socket for up to 5 concurrent pending connection requests
        listenSocket = new ServerSocket(this.port, 5, addr);

        listenSocket.setSoTimeout(2000); // set listen time-out on the socket so accept() may be interrupted
    } // constructor

    public String getInetAddr()
    {
        return addr.getHostAddress();
    }

    private boolean isListed(Socket aSocket, boolean whiteList) throws IOException
    {
        boolean sense = whiteList;
        String file = (whiteList ? cfg.getIpWhitelist() : cfg.getBlacklist());
        if (file != null && file.length() > 0)
        {
            String filename;
            if (Utils.isRelativePath(file))
                filename = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + file;
            else
                filename = file;
            if (filename.length() > 0)
            {
                String inet = aSocket.getInetAddress().toString();
                if (inet != null)
                {
                    sense = false;
                    inet = inet.replaceAll("/", "");
                    inet = inet.replaceAll("\\\\", "");
                    BufferedReader br = new BufferedReader(new FileReader(filename));
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        line = line.trim();
                        if (line.length() > 0 && !line.startsWith("#"))
                        {
                            if (inet.equals(line))
                            {
                                sense = true;
                                break;
                            }
                        }
                    }
                    br.close();
                }
            }
        }
        return sense;
    }

    /**
     * Politely request the listener to stop.
     */
    public void requestStop()
    {
        this.stop = true;
        this.interrupt();
    }

    /**
     * Run listen thread body.
     * <p>
     * Waits for a connection request. When a request is received an attempt is
     * made to create a new connection thread via a call to the addConnection()
     * method.
     */
    public void run()
    {
        Socket socket = null;
        while (!stop)
        {
            try
            {
                socket = listenSocket.accept(); // new socket
                if (isListed(socket, true)) // if it is whitelisted or there is no whitelist
                {
                    if (isListed(socket, false)) // if it is blacklisted disconnect
                    {
                        socket.close();
                        logger.warn("blacklisted IP " + socket.getInetAddress().toString().replaceAll("/", "").replaceAll("\\\\", "") + " attempted login");
                    }
                    else
                    {
                        instance.addConnection(socket);
                    }
                }
                else
                    logger.warn("not whitelisted IP " + socket.getInetAddress().toString().replaceAll("/", "").replaceAll("\\\\", "") + " attempted login");
            }
            catch (SocketTimeoutException e)
            {
                //logger.debug("socket timeout on listener port " + port + ", stop=" + ((stop) ? "true" : "false"));
                continue; // ignore listen time-out
            }
            catch (InterruptedIOException e)
            {
                logger.debug("listener interrupted on port " + port + ", stop=" + ((stop) ? "true" : "false"));
                break;
            }
            catch (IOException e)
            {
                logger.error(Utils.getStackTrace(e));
                stop = true;
                context.fault = true;
            }
            catch (MungeException e)
            {
                logger.error(Utils.getStackTrace(e));
                stop = true;
                context.fault = true;
            }
        }

        if (logger != null)
            logger.debug("stopping stty listener on: " + listenSocket.getLocalSocketAddress().toString() + ":" + listenSocket.getLocalPort());

        if (listenSocket != null && listenSocket.isBound())
        {
            try
            {
                listenSocket.close();
                listenSocket = null;
            }
            catch (Exception se)
            {
            }
        }
    }
} // Listener
