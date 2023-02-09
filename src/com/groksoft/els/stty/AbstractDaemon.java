package com.groksoft.els.stty;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Daemon service.
 * <p>
 * The Daemon service is the command interface used to communicate between
 * the endpoints.
 */
public abstract class AbstractDaemon
{
    protected static Logger logger = LogManager.getLogger("applog");

    protected InetAddress address;
    protected boolean authorized = false;
    protected boolean connected = false;
    protected Context context;
    protected boolean fault = false;
    private Thread heartBeat = null;
    private boolean heartBeatEnabled = true;
    protected DataInputStream in = null;
    protected String myKey;
    protected Repository myRepo;
    protected DataOutputStream out = null;
    protected int port;
    protected String response = "";
    protected Socket socket;
    protected boolean stop = false;
    protected Repository theirRepo;

    /**
     * Instantiate the Daemon service
     */
    public AbstractDaemon(Context context, Repository mine, Repository theirs)
    {
        this.context = context;
        this.myRepo = mine;
        if (theirs != null)
        {
            this.theirRepo = theirs;
        }
        this.myKey = myRepo.getLibraryData().libraries.key;
    } // constructor

    /**
     * Create and start the internal heartbeat
     */
    protected void createHeartBeat()
    {
        heartBeat = new Thread()
        {
            public void run()
            {
                try
                {
                    sleep(20 * 1000); // offset this heartbeat timing
                    String desc = (theirRepo != null) ? " to " + theirRepo.getLibraryData().libraries.description : "";
                    while (true)
                    {
                        sleep(1 * 60 * 1000); // heartbeat sleep time in milliseconds
                        if (heartBeatEnabled)
                        {
                            send("ping", context.main.trace ? "heartbeat sent" + desc : "");
                        }
                    }
                }
                catch (InterruptedException e)
                {
                    logger.trace("heartbeat interrupted");
                    interrupt();
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    context.fault = true;
                }
            }
        };
        logger.trace("starting heartbeat");
        heartBeat.start();
    }

    /**
     * Temporarily disable the internal heartbeat
     */
    private void disableHeartBeat()
    {
        if (heartBeat != null)
        {
            if (!heartBeatEnabled)
                logger.warn("heartbeat already disabled");
            else
                logger.trace("heartbeat disabled");
            heartBeatEnabled = false;
        }
    }

    /**
     * Enable the internal heartbeat
     */
    private void enableHeartBeat()
    {
        if (heartBeat != null)
        {
            if (heartBeatEnabled)
                logger.warn("heartbeat already enabled");
            else
                logger.trace("heartbeat enabled");
            heartBeatEnabled = true;
        }
    }

    /**
     * Get the fault indicator
     *
     * @return True if a fault occurred
     */
    public boolean getFault()
    {
        return fault;
    }

    /**
     * Get the short name of the service.
     *
     * @return Short name of this service.
     */
    public String getName()
    {
        return "Daemon";
    }

    /**
     * Get the daemon service socket
     *
     * @return Socket of this daemon
     */
    public Socket getSocket()
    {
        return socket;
    }

    /**
     * Perform initial handshake for this session.
     */
    public abstract String handshake();

    /**
     * Process a connection request to the Daemon service.
     */
    public abstract boolean process() throws IOException, Exception;

    /**
     * Receive a response from the other end
     *
     * @param log Log line, if any
     * @param timeout Timeout value in milliseconds
     * @return String of response text
     * @throws Exception
     */
    public String receive(String log, int timeout) throws Exception
    {
        if (getSocket().isOutputShutdown())
            throw new MungeException("socket output shutdown, keep alive " + getSocket().getKeepAlive());
        if (!getSocket().isBound())
            throw new MungeException("socket not bound");
        if (!getSocket().isConnected())
            throw new MungeException("socket not connected");
        if (getSocket().isClosed())
            throw new MungeException("socket closed");

        if (timeout < 0)
            timeout = myRepo.getLibraryData().libraries.timeout * 60 * 1000;
        getSocket().setSoTimeout(timeout); // set read time-out
        if (log != null && log.length() > 0)
            logger.debug(log + ", " + timeout + " ms");
        logger.trace("sotimeout " + getSocket().getSoTimeout());
        //logger.trace("keep alive " + getSocket().getKeepAlive());

        String response = null;
        while (true)
        {
            response = Utils.readStream(in, myRepo.getLibraryData().libraries.key);

            if (response != null && response.startsWith("ping"))
                logger.trace("heartbeat received" + ((theirRepo != null) ? " from " + theirRepo.getLibraryData().libraries.description : ""));
            else
                break;
        }
        return response;
    }

    /**
     * Request the Daemon service to stop
     */
    public void requestStop()
    {
        this.stop = true;
        logger.debug("requesting stop for stty session: " + Utils.formatAddresses(socket));
    }

    /**
     * Send a command to the other end
     *
     * @param message Data to be sent
     * @param log Log line, if any
     * @throws Exception
     */
    public void send(String message, String log) throws Exception
    {
        if (log != null && log.length() > 0)
            logger.debug(log);
        //logger.trace("keep alive " + getSocket().getKeepAlive());
        if (getSocket().isOutputShutdown())
            throw new MungeException("socket output shutdown, keep alive " + getSocket().getKeepAlive());
        if (!getSocket().isBound())
            throw new MungeException("socket not bound");
        if (!getSocket().isConnected())
            throw new MungeException("socket not connected");
        if (getSocket().isClosed())
            throw new MungeException("socket closed");

        if (!message.equalsIgnoreCase("ping"))
            disableHeartBeat();

        Utils.writeStream(out, myRepo.getLibraryData().libraries.key, message);

        if (!message.equalsIgnoreCase("ping"))
            enableHeartBeat();
    }

    /**
     * Stop the internal heart beat thread
     */
    protected void stopHeartBeat()
    {
        if (heartBeat != null && heartBeat.isAlive())
        {
            logger.trace("stopping heartbeat thread");
            heartBeat.interrupt();
        }
    }

} // Daemon
