package com.groksoft.volmunger.comm;

import com.groksoft.volmunger.Configuration;
import com.groksoft.volmunger.MungerException;
import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.comm.gui.TerminalGui;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class Terminal
{
    private transient Logger logger = LogManager.getLogger("applog");

    private Configuration cfg;
    private boolean isConnected = false;
    private boolean isTerminal = false;
    private Socket socket;

    DataInputStream in = null;
    DataOutputStream out = null;

    private Repository myRepo;
    private Repository theirRepo;
    private String myKey;
    private String theirKey;

    /**
     * Instantiate a Terminal.<br>
     * <br>
     * An interactive (manual) client includes the prompt, whereas the prompt
     * is not appended for automated connections.<br>
     * <br>
     * @param config The Configuration object
     * @param isTerminal True if an interactive client, false if an automated client
     */
    public Terminal(Configuration config, boolean isTerminal) {
        this.cfg = config;
        this.isTerminal = isTerminal;
    }

    public long availableSpace(String location) {
        long space = 0L;
        String response = roundTrip("space " + location);
        if (response != null && response.length() > 0)
        {
            space = Long.parseLong(response);
        }
        return space;
    }

    public boolean connect(Repository myRepo, Repository theirRepo) throws Exception {
        this.myRepo = myRepo;
        this.theirRepo = theirRepo;

        if (this.theirRepo != null &&
                this.theirRepo.getLibraryData() != null &&
                this.theirRepo.getLibraryData().libraries != null &&
                this.theirRepo.getLibraryData().libraries.site != null) {

            this.myKey = myRepo.getLibraryData().libraries.key;
            this.theirKey = theirRepo.getLibraryData().libraries.key;

            String host = Utils.parseHost(this.theirRepo.getLibraryData().libraries.site);
            if (host == null || host.isEmpty()) {
                host = null;
            }
            int port = Utils.getPort(this.theirRepo.getLibraryData().libraries.site);
            logger.info("Opening connection to: " + (host == null ? "localhost" : host) + ":" + port);

            try {
                this.socket = new Socket(host, port);
                in = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

            } catch (Exception e) {
                logger.error(e.getMessage());
            }

            if (in != null && out != null) {
                if (handshake()) {
                    isConnected = true;
                } else {
                    logger.error("Connection to " + this.theirRepo.getLibraryData().libraries.site + " failed handshake");
                }
            }
            else
            {
                logger.error ("Could not make connection to " + this.theirRepo.getLibraryData().libraries.site);
            }
        } else {
            throw new MungerException("cannot get site from -r specified remote subscriber library");
        }

        return isConnected;
    }

    public void disconnect() {
        try {
            out.flush();
            out.close();
            in.close();
        } catch (Exception e) {
        }
    }

    private boolean handshake() {
        boolean valid = false;
        String input = Utils.read(in, theirKey);
        if (input.equals("HELO")) {
            Utils.write(out, theirKey, (isTerminal ? "DribNit" : "DribNlt"));

            input = Utils.read(in, theirKey);
            if (input.equals(theirKey)) {
                Utils.write(out, theirKey, myKey);

                input = Utils.read(in, theirKey);
                if (input.equals("ACK")) {
                    logger.info("Authenticated: " + theirRepo.getLibraryData().libraries.description);
                    valid = true;
                }
            }
        }
        return valid;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int session() {
        int returnValue = 0;
        TerminalGui gui = new TerminalGui(this, cfg, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

    public String receive()
    {
        String response = Utils.read(in, theirRepo.getLibraryData().libraries.key);
        return response;
    }

    public String roundTrip(String command)
    {
        send(command);
        String response = receive();
        return response;
    }

    public int send(String command)
    {
        Utils.write(out, theirRepo.getLibraryData().libraries.key, command);
        return 0;
    }

}
