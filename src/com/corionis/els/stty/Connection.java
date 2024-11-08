package com.corionis.els.stty;

import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.util.Vector;

/**
 * Handle individual client connections.
 * <p>
 * The Connection class is a subclass of Thread. It handles individual
 * connections between a Service and a client. Each connection has a
 * separate thread. Each Service can have multiple connection requests
 * pending at once.
 */
public class Connection extends Thread
{
    protected static Logger logger = LogManager.getLogger("applog");
    private ServeStty instance = null;

    /**
     * The service for the connection
     */
    protected AbstractDaemon service;
    /**
     * The socket for the connection
     */
    protected Socket socket;

    /**
     * Constructor.
     * <p>
     * Connection objects are created by Listener threads as part of the
     * server's thread group. The superclass constructor is called to create a
     * new thread to handle the connection request.
     *
     * @param instance The ServeStty instance using this connection
     * @param aSocket  Socket for connection
     * @param name The name for this connection
     * @param aService Service for connection
     */
    public Connection(ServeStty instance, Socket aSocket, String name, AbstractDaemon aService)
    {
        super("stty." + name + (aService.context.trace ? ":" + Utils.formatAddresses(aSocket) : ""));
        this.instance = instance;
        this.socket = aSocket;
        this.service = aService;
    } // constructor

    /**
     * Get the associated Daemon instance
     */
    public AbstractDaemon getConsole()
    {
        return service;
    }

    /**
     * Get the associated Socket instance
     */
    public Socket getSocket()
    {
        return socket;
    }

    /**
     * Run the service for this connection.
     * <p>
     * Creates input and output streams for the connection and calls the
     * interface method for the Service. Calls endConnection() when
     * the method returns for any reason.
     */
    public void run()
    {
        int status = 0;
        try
        {
            service.socket = socket;
            status = service.process();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        finally
        {
            if (instance != null && instance.isAlive()) // && !service.localContext.timeout)
            {
                logger.info("closing stty connection to: " + Utils.formatAddresses(socket));
                Vector conns = instance.getAllConnections();
                conns.remove(this);
            }

            if (status > 0)
            {
                try
                {
                    logger.trace("shutdown via stty");

                    if (status == 2)
                    {
                        // exit triggers the shutdown hook
                        // see Main isListening clause with Runtime.getRuntime().addShutdownHook()
                        if (service.context.main.context.fault)
                            logger.error("Exiting with error code");
                        service.context.main.shutdown();
                        System.exit(0);
                    }
                }
                catch (Exception e)
                {
                    service.context.fault = true;
                    logger.error(Utils.getStackTrace(e));
                    if (status == 2)
                    {
                        service.context.main.shutdown();
                        //System.exit(0);
                    }
                }
            }
        }
    }

} // Connection
