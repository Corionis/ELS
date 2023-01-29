package com.groksoft.els.stty;

import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
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
     * @param aPort The port to listen on.
     */
    public Listener(ThreadGroup group, String host, int aPort, Configuration config, Context ctxt) throws Exception
    {
        super(group, "Listener:" + host + ":" + aPort);

        // setup this listener
        this.cfg = config;
        this.context = ctxt;
        this.port = aPort;
        addr = Inet4Address.getByName(host);

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
        String filename = (whiteList ? cfg.getIpWhitelist() : cfg.getBlacklist());
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
                        ServeStty.getInstance().addConnection(socket);
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
                logger.error(e);
                stop = true;
                context.fault = true;
            }
            catch (MungeException e)
            {
                logger.error(e);
                stop = true;
                context.fault = true;
            }
        }
        if (logger != null && socket != null)
            logger.debug("stopping stty listener on: " + socket.getLocalAddress().toString() + ":" + socket.getPort());
    }
} // Listener
