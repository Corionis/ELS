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
    protected static Logger logger = LogManager.getLogger("applog");//

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
     * @param aSocket  Socket for connection
     * @param aService Service for connection
     */
    public Connection(Socket aSocket, String name, AbstractDaemon aService)
    {
        super("stty." + name + (aService.context.trace ? ":" + Utils.formatAddresses(aSocket) : ""));
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
        boolean stop = false;
        try
        {
            service.socket = socket;
            stop = service.process();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            stop = true;
        }
        finally
        {
/*
            // notify the ConnectionManager that this connection has closed
            ServeStty cm = ServeStty.getInstance();
            if (cm != null && cm.isAlive()) // && !service.localContext.timeout)
            {
                logger.debug("closing stty connection to: " + Utils.formatAddresses(socket));
                cm.endConnection();
                if (stop)
                {
                    cm.stopServer();
                }
            }
*/

            ServeStty cm = ServeStty.getInstance();
            if (cm != null && cm.isAlive()) // && !service.localContext.timeout)
            {
                logger.info("closing stty connection to: " + Utils.formatAddresses(socket));
                Vector conns = cm.getAllConnections();
                conns.remove(this);
            }

            if (stop)
            {
                try // also done in Main.process() finally{ shutdownHook }
                {
                    if (service.context.main.primaryExecution)
                    {
                        logger.trace("shutdown via stty");

                        // optionally command status server to quit
                        if (service.context.main.context.hintsStty != null)
                            service.context.main.context.hintsStty.quitStatusServer(service.context);  // do before stopping the services

                        service.context.main.stopServices();
                        sleep(2000);
                        service.context.main.stopVerbiage();
                    }
                    else
                        service.context.main.restoreEnvironment();

                    // halt kills the remaining threads
                    // see Main isListening clause with Runtime.getRuntime().addShutdownHook()
                    if (service.context.main.context.fault)
                        logger.error("Exiting with error code");
                    if (service.context.main.primaryExecution)
                        Runtime.getRuntime().halt(service.context.main.context.fault ? 1 : 0);
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    if (service.context.main.primaryExecution)
                        Runtime.getRuntime().halt(1);
                }
            }
        }
    }

} // Connection
