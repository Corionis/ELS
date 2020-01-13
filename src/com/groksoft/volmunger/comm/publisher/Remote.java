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
    private BufferedReader in;
    private boolean isConnected = false;
    private PrintWriter out;
    private Socket socket;

    public Remote (Configuration config)
    {
        cfg = config;
    }

    public boolean Connect(Repository publisherRepo, Repository subscriberRepo) throws Exception
    {
        if (subscriberRepo != null &&
                subscriberRepo.getLibraryData() != null &&
                subscriberRepo.getLibraryData().libraries != null &&
                subscriberRepo.getLibraryData().libraries.site != null)
        {
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
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }

            isConnected = true;
            handshake(publisherRepo, subscriberRepo);
            logger.info("Connected to " + subscriberRepo.getLibraryData().libraries.site);


        }
        else
        {
            throw new MungerException("cannot get site from -r specified remote subscriber library");
        }

        return isConnected;
    }

    private boolean handshake(Repository publisherRepo, Repository subscriberRepo)
    {
        boolean valid = false;
        String input = read();
        if (input == "HELO")
        {
            out.println("DribNit");
            out.flush();

        }

        return valid;
    }

    private String read()
    {
        String input = "";
        try {
            input = in.readLine();
            logger.debug("read: " + input);
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
        return input;
    }

}
