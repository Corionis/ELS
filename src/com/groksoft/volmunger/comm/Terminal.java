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
    private Socket socket;

    DataInputStream in = null;
    DataOutputStream out = null;

    private Repository myRepo;
    private Repository theirRepo;
    private String myKey;
    private String theirKey;

    public Terminal(Configuration config) {
        cfg = config;
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
                logger.info("Remote host not defined, using default: localhost");
            }
            int port = Utils.getPort(this.theirRepo.getLibraryData().libraries.site);

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
                    logger.error("Connection failed handshake to " + this.theirRepo.getLibraryData().libraries.site);
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
            Utils.write(out, theirKey, "DribNit");

            input = Utils.read(in, theirKey);
            if (input.equals(theirKey)) {
                Utils.write(out, theirKey, myKey);

                input = Utils.read(in, theirKey);
                if (input.equals("ACK")) {
                    logger.info("Server authenticated: " + theirRepo.getLibraryData().libraries.description);
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
        TerminalGui gui = new TerminalGui(cfg, in, out);
        returnValue = gui.run(myRepo, theirRepo);
        return returnValue;
    }

}
