package com.corionis.els.stty;

import com.corionis.els.stty.subscriber.Daemon;
import com.corionis.els.Configuration;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Vector;

/**
 * Manage all connections and enforce limits.
 * <p>
 * The ServeStty class is a subclass of Thread. It keeps a list of all
 * connections, and enforces the maximum connection limit.
 * <p>
 * Each connection uses a separate thread.
 * <p>
 * There is one ServeStty for the entire server.
 * <p>
 * The ServeStty uses another thread to remove dead connections from the
 * allConnections list.
 */
public class ServeStty extends Thread
{
    /**
     * The single instance of this class
     */
    private static ServeStty instance = null;

    /**
     * The maximum connections allowed for this entire server instance
     */
    protected int maxConnections;

    /**
     * Flag used to determine when to stop listening
     */
    private boolean _stop = false;

    /**
     * The list of all service connections
     */
    private Vector<Connection> allConnections;

    private ThreadGroup allSessionThreads;
    private Hashtable<String, Listener> allSessions;
    private Configuration cfg;
    private Context context;
    private int listenPort;
    private transient Logger logger = LogManager.getLogger("applog");
    private boolean primaryServers;

    /**
     * Count of total connections since started
     */
    private int totalConnections = 0;

    /**
     * Instantiates the ServeStty object and set it as a daemon so the Java
     * Virtual Machine does not wait for it to exit.
     */
    public ServeStty(ThreadGroup aGroup, int aMaxConnections, Configuration config, Context ctxt, boolean primaryServers)
    {
        // instantiate this object in the specified thread group to
        // enforce the specified maximum connections limitation.
        super(aGroup, "stty." + aGroup.getName());
        instance = this;
        instance.cfg = config;
        instance.context = ctxt;
        instance.primaryServers = primaryServers;

        // make it a daemon so the JVM does not wait for it to exit
        this.setDaemon(true);
        this.setMaxConnections(aMaxConnections);
        this.allConnections = new Vector<Connection>();
        this.allSessions = new Hashtable<String, Listener>();
        this.allSessionThreads = aGroup;
    } // constructor

    /**
     * Get this instance.
     */
    public static ServeStty getInstance()
    {
        return instance;
    }

    /**
     * Add a connection for a service.
     * <p>
     * Responds to a connection request. The maximum connection limit is
     * checked. If the limit has not been exceeded the new connection is added
     * to allConnections, and a thread is started to service the request.
     */
    public synchronized void addConnection(Socket aSocket) throws MungeException
    {
        // check for maximum connections
        if (allConnections.size() >= maxConnections)
        {
            // maximum connections exceeded - try to tell user
            try
            {
                PrintWriter clientOut = new PrintWriter(aSocket.getOutputStream());
                clientOut.println("Connection request denied; maximum users exceeded");
                clientOut.flush();

                // log it
                logger.info("Maximum connections (" + maxConnections + ") exceeded");
                logger.info("Connection refused from: " + Utils.formatAddresses(aSocket));

                // close the connection
                aSocket.close();
            }
            catch (IOException e)
            {
                logger.info(e);
            }
        }
        else
        // if limit has not been reached
        {
            // create a connection thread for this request
            Connection theConnection;
            if (cfg.isPublisherListener())
            {
                theConnection = new Connection(aSocket, "publisher", new com.corionis.els.stty.publisher.Daemon(context, context.publisherRepo, context.subscriberRepo));
            }
            else if (cfg.isSubscriberListener() || cfg.isSubscriberTerminal())
            {
                theConnection = new Connection(aSocket, "subscriber", new Daemon(context, context.subscriberRepo, context.publisherRepo));
            }
            else if (cfg.isStatusServer())
            {
                theConnection = new Connection(aSocket, "hintserver", new com.corionis.els.stty.hintServer.Daemon(context, context.hintsRepo, null));
            }
            else
            {
                throw new MungeException("FATAL: Unknown connection type");
            }
            allConnections.add(theConnection);

            // log it
            logger.info((cfg.isStatusServer() ? "Status Server" : (cfg.isPublisherListener() ? "Publisher" : "Subscriber")) + " daemon opened stty: " + Utils.formatAddresses(aSocket));

            // start the connection thread
            theConnection.start();
            ++totalConnections;
        }
    }

    /**
     * Add a listener service on host and port
     *
     * @param host Hostname of listener
     * @param aPort Port for listener
     * @throws Exception
     */
    protected void addListener(String host, int aPort) throws Exception
    {
        //Integer key = new Integer(aPort);   // hashtable key

        // do not allow duplicate port assignments
        if (allSessions.get("listener:" + host + ":" + aPort) != null)
            throw new IllegalArgumentException("Port " + aPort + " already in use");

        // create a listener on the port
        Listener listener = new Listener(allSessionThreads, host, aPort, cfg, context);

        // put it in the hashtable
        allSessions.put("listener:" + host + ":" + aPort, listener);

        // log it
        logger.info("Stty server is listening on: " + (host == null ? "localhost" : listener.getInetAddr()) + ":" + aPort);

        // fire it up
        listener.start();
    }

    /**
     * Dump statistics of connections.
     */
    public synchronized String dumpStatistics()
    {
        String data = "Listening on: " + listenPort + "\r\n" +
                "Active connections: " + allConnections.size() + "\r\n";
        for (int index = 0; index < allConnections.size(); ++index)
        {
            Connection c = (Connection) allConnections.elementAt(index);
            data += "  " + c.service.getName() + " to " + Utils.formatAddresses(c.socket) + "\r\n";
        }

        // dump connection counts
        data += "  Total connections since started: " + totalConnections + "\r\n";
        data += "  Maximum allowed connections: " + maxConnections + "\r\n";

        return data;
    }

    /**
     * End a client connection.
     * <p>
     * Notifies the ServeStty that this connection has been closed. Called
     * from the run() method of the Connection thread created by addConnection()
     * when the connection is closed for any reason.
     *
     * @see Connection
     */
    public synchronized void endConnection()
    {
        // notify the ServeStty thread that this connection has closed
        this.notify();
    }

    /**
     * Get the connections Vector
     */
    public Vector getAllConnections()
    {
        return this.allConnections;
    }

    /**
     * Politely request the listener to stop.
     */
    public void requestStop()
    {
        this._stop = true;
        for (int index = 0; index < allConnections.size(); ++index)
        {
            // stop live connections
            Connection c = (Connection) allConnections.elementAt(index);
            if (c.isAlive())
            {
/*
                try
                {
                    c.socket.close();
                }
                catch (IOException ioe)
                {}
                c.getConsole().requestStop();
*/
            }
        }
        this.interrupt();
    }

    /**
     * Thread used to clean-up dead connections.
     * <p>
     * Waits to be notified of a closed connection via a call to the
     * endConnection() method, then scans all current connections for any that
     * are dead. Each dead connection is removed from the allConnections list.
     */
    public void run()
    {
        // log it
        logger.info("Starting stty server for up to " + maxConnections + " incoming connections");
        while (_stop == false)
        {
            for (int index = 0; index < allConnections.size(); ++index)
            {
                // remove dead connections
                Connection c = (Connection) allConnections.elementAt(index);
                if (!c.isAlive())
                {
                    allConnections.removeElementAt(index);
                }
            }

            // wait for notify of closed connection
            try
            {
                synchronized (this)
                {
                    this.wait();
                }
            }
            catch (InterruptedException e)
            {
                logger.debug("stty interrupted, stop=" + ((_stop) ? "true" : "false"));
                _stop = true;
                break;
            }
        }
    }

    /**
     * Set or change the maximum number of connections allowed for this server.
     */
    public synchronized void setMaxConnections(int aMax)
    {
        maxConnections = aMax;
    }

    /**
     * Start a session listener
     */
    public void startListening(Repository listenerRepo) throws Exception
    {
        if (listenerRepo != null &&
                listenerRepo.getLibraryData() != null &&
                listenerRepo.getLibraryData().libraries != null)
        {
            String address = listenerRepo.getLibraryData().libraries.listen;
            if (address == null || address.isEmpty())
                address = listenerRepo.getLibraryData().libraries.host;
            startServer(address);
        }
        else
        {
            throw new MungeException("cannot get site from -r specified remote library");
        }
    }

    /**
     * Start this stty server
     *
     * @param listen The listen value from the JSON library file
     *
     * @throws Exception
     */
    public void startServer(String listen) throws Exception
    {
        String host = Utils.parseHost(listen);
        if (host == null || host.isEmpty())
        {
            host = null;
            logger.info("Host not defined, using default: localhost");
        }
        listenPort = Utils.getPort(listen) + ((primaryServers) ? 0 : 2);
        if (listenPort > 0)
        {
            this.start();
            addListener(host, listenPort);
        }
        if (listenPort < 1)
        {
            logger.info("Stty is disabled");
        }
    }

    /**
     * Stop this stty server
     */
    public void stopServer()
    {
        if (allSessions != null)
        {
            logger.debug("stopping all stty listener threads");
            Collection<Listener> lc = allSessions.values();
            for (Listener listener : lc)
            {
                if (listener != null)
                {
                    if (listener.isAlive())
                        listener.requestStop();
                }
            }
            try { Thread.sleep(500L); } catch (Exception e) {};
            this.requestStop();
            allSessions = null;
        }
        else
        {
            //logger.debug("nothing to stop");
        }
    }

} // ServeStty
