package com.groksoft.volmunger.comm.publisher;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.MungerException;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.*;
import java.util.*;

public class Remote
{
    private transient Logger logger = LogManager.getLogger("applog");

    private Configuration cfg;
    private boolean isConnected = false;
    private Socket socket;

    //BufferedReader in = null;
    DataInputStream in = null;
    //PrintWriter out = null;
    DataOutputStream out = null;

    private Repository publisherRepo;
    private Repository subscriberRepo;
    private String publisherKey;
    private String subscriberKey;

    public Remote (Configuration config)
    {
        cfg = config;
    }

    public boolean connect(Repository pubRepo, Repository subRepo) throws Exception
    {
        this.publisherRepo = pubRepo;
        this.subscriberRepo = subRepo;

        if (subscriberRepo != null &&
                subscriberRepo.getLibraryData() != null &&
                subscriberRepo.getLibraryData().libraries != null &&
                subscriberRepo.getLibraryData().libraries.site != null)
        {
            this.publisherKey = pubRepo.getLibraryData().libraries.key;
            this.subscriberKey = subRepo.getLibraryData().libraries.key;

            String host = Utils.parseHost(subscriberRepo.getLibraryData().libraries.site);
            if (host == null || host.isEmpty())
            {
                host = null;
                logger.info("remote subscriber host not defined, using default: localhost");
            }
            int port = Utils.getPort(subscriberRepo.getLibraryData().libraries.site);

            try
            {
                socket = new Socket(host, port);
                //out = new PrintWriter(socket.getOutputStream(), true);
                //in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                //in = new BufferedReader(new InputStreamReader(aSocket.getInputStream()));
                in = new DataInputStream(socket.getInputStream());
                //out = new PrintWriter(new OutputStreamWriter(aSocket.getOutputStream()));
                out = new DataOutputStream(socket.getOutputStream());

            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }

            if (handshake()) {
                isConnected = true;
                logger.info("Connected to " + subscriberRepo.getLibraryData().libraries.site);
            }
            else
            {
                logger.warn("Connection to " + subscriberRepo.getLibraryData().libraries.site + " failed handshake");
            }

        }
        else
        {
            throw new MungerException("cannot get site from -r specified remote subscriber library");
        }

        return isConnected;
    }

    public void disconnect()
    {
        try {
            out.flush();
            out.close();
            in.close();
        }
        catch (Exception e)
        {
        }
    }

    private boolean handshake()
    {
        boolean valid = false;
        String input = Utils.read(in, subscriberKey);
        if (input.equals("HELO"))
        {
            Utils.write(out, subscriberKey, "DribNit");
            //out.println("DribNit");
            //out.flush();
            //input = read();
            input = Utils.read(in, subscriberKey);
            //input = new String(Utils.decrypt(subscriberRepo.getLibraryData().libraries.key, input.getBytes()));
            if (input.equals(subscriberKey))
            {
                //out.println(publisherRepo.getLibraryData().libraries.key);
                //out.flush();
                Utils.write(out, subscriberKey, publisherKey);
                //input = read();
                input = Utils.read(in, subscriberKey);
                if (input.equals("ACK"))
                {
                    logger.info("Session authenticated");
                    valid = true;
                }
            }
        }
        return valid;
    }

}
