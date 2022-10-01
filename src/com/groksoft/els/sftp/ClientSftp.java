package com.groksoft.els.sftp;

import com.groksoft.els.Configuration;
import com.groksoft.els.Utils;
import com.groksoft.els.repository.Libraries;
import com.groksoft.els.repository.Repository;
import com.jcraft.jsch.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Vector;

/**
 * ClientSftp -to- ServerSftp
 * <br/>
 * This implementation uses the Jsch client library:
 * http://www.jcraft.com/jsch/
 * https://epaul.github.io/jsch-documentation/
 */
public class ClientSftp
{
    private Configuration cfg;
    private String hostname;
    private int hostport;
    private Channel jChannel;
    private Session jSession;
    private JSch jsch;
    private transient Logger logger = LogManager.getLogger("applog");
    private Repository myRepo;
    private String password;
    private Repository theirRepo;
    private String user;

    private ClientSftp()
    {
        // hide default constructor
    }

    /**
     * Instantiate this class.
     *
     * @param mine   Repository of local system
     * @param theirs Repository of remote system
     */
    public ClientSftp(Configuration config, Repository mine, Repository theirs, boolean primaryServers)
    {
        cfg = config;
        myRepo = mine;
        theirRepo = theirs;

        hostname = Utils.parseHost(theirRepo.getLibraryData().libraries.host);
        hostport = Utils.getPort(theirRepo.getLibraryData().libraries.host) + ((primaryServers) ? 1 : 3);

        user = myRepo.getLibraryData().libraries.key;
        password = theirRepo.getLibraryData().libraries.key;
    }

    public synchronized Vector listDirectory(String directory) throws Exception
    {
        ChannelSftp jSftp = connect();
        Vector listing = jSftp.ls(directory);
        jSftp.disconnect();
        return listing;
    }

    public void get(String source, String dest) throws Exception
    {
        ChannelSftp jSftp = connect();
        jSftp.get(source, dest);
        jSftp.disconnect();
    }

    /**
     * Make a remote directory tree
     *
     * @param pathname Path and filename. Note that an ending filename is required but not used
     * @return True if any directories were created
     * @throws IOException
     */
    public String makeDirectory(String pathname) throws Exception
    {
        if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.WINDOWS))
        {
            pathname = pathname.replaceAll("\\\\", "\\\\\\\\");
        }
        if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.LINUX))
        {
            pathname = pathname.replaceAll("//", "/");
        }

        String sep = theirRepo.getWriteSeparator();
        String[] parts = pathname.split(sep);

        sep = theirRepo.getSeparator();
        String whole = "";
        for (int i = 0; i < parts.length - 1; ++i)
        {
            try
            {
                // is it a Windows drive letter: ?
                if (i == 0 && parts[i].endsWith(":"))
                {
                    // don't try to create a Windows root directory, e.g. C:\
                    if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.WINDOWS) &&
                            parts[i].length() == 2)
                    {
                        whole = parts[i];
                        continue;
                    }
                }
                whole = whole + ((i > 0) ? sep : "") + parts[i];

                // protect the root of drives
                if (whole.length() < 1 || whole.equals(sep))
                    continue;

                // try to create next directory segment
                ChannelSftp jSftp = connect();
                jSftp.mkdir(whole);
                jSftp.disconnect();
            }
            catch (SftpException e)
            {
                String msg = e.toString().trim().toLowerCase();
                if (!msg.contains("alreadyexists")) // ignore "already exists" errors
                {
                    throw new SftpException(e.id, e.getMessage() + ": " + whole);
                }
            }
        }
        return whole;
    }

    private ChannelSftp connect()
    {
        ChannelSftp jSftp = null;
        try
        {
            jChannel = jSession.openChannel("sftp");
            jChannel.connect();
            jSftp = (ChannelSftp) jChannel;
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return jSftp;
    }

    public void remove(String path, boolean isDir) throws Exception
    {
        ChannelSftp jSftp = connect();
        if (isDir)
            jSftp.rmdir(path);
        else
            jSftp.rm(path);
        jSftp.disconnect();
    }

    public void rename(String from, String to) throws Exception
    {
        ChannelSftp jSftp = connect();
        jSftp.rename(from, to);
        jSftp.disconnect();
    }

    public void setDate(String dest, long mtime) throws Exception
    {
        ChannelSftp jSftp = connect();
        jSftp.setMtime(dest, (int) mtime);
        jSftp.disconnect();
    }

    /**
     * Start this sftp client
     */
    public boolean startClient()
    {
        try
        {
            logger.info("Opening sftp connection to: " + (hostname == null ? "localhost" : hostname) + ":" + hostport);
            jsch = new JSch();
            jSession = jsch.getSession(user, hostname, hostport);
            jSession.setConfig("StrictHostKeyChecking", "no");
            jSession.setPassword(password);
            // Could implement strict key checking if more security is needed
            //jsch.setKnownHosts("known_hosts");
            //jsch.addIdentity("id_rsa");

            jSession.connect(60000); // sftp session connection timeout

            // If this is a remote Navigator session then "keep alive" the connection
            if (cfg.isRemoteSession() && cfg.isNavigator())
                jSession.setServerAliveInterval(500000); // Navigator keep alive timeout
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Stat a remote entry
     */
    public synchronized SftpATTRS stat(String path) throws Exception
    {
        ChannelSftp jSftp = connect();
        SftpATTRS attrs = jSftp.stat(path);
        jSftp.disconnect();
        return attrs;
    }

    /**
     * Stop this sftp client
     */
    public void stopClient()
    {
        logger.debug("Disconnecting sftp: " + (hostname == null ? "localhost" : hostname) + ":" + hostport);
        if (jChannel != null)
            jChannel.disconnect();

        if (jSession != null)
            jSession.disconnect();
    }

    /**
     * Transmit a file from local to remote system
     *
     * @param src  Source file path with local separators
     * @param dest Destination file path with remove separators
     * @throws IOException
     */
    public void transmitFile(String src, String dest, boolean overwrite) throws Exception
    {
        SftpATTRS destAttr = null;
        int readOffset = 0;
        long writeOffset = 0L;

        ChannelSftp jSftp = connect();
        String copyDest = dest + ".els-part";

        // does the destination already exist?
        // automatically resume/continue transfer
        try
        {
            destAttr = jSftp.stat(copyDest);
            if (destAttr != null)
            {
                if (destAttr.isReg() && destAttr.getSize() > 0)
                {
                    if (!overwrite)
                    {
                        readOffset = (int) destAttr.getSize();
                        writeOffset = readOffset + 1;
                    }
                }
            }
        }
        catch (SftpException e)
        {
            String msg = e.toString().trim().toLowerCase();
            if (!msg.contains("nosuchfileexception"))
                throw e;
            destAttr = null;
        }

        if (destAttr == null) // file does not exist, try making directory tree
        {
            makeDirectory(copyDest);
        }

        int mode = jSftp.OVERWRITE;
        if (writeOffset > 0)
        {
            mode = jSftp.RESUME;
            logger.info("Resuming transfer at " + writeOffset);
        }

        // copy the .els-part file
        jSftp.put(src, copyDest, mode);

        // delete any old original file
        try
        {
            jSftp.rm(dest);
        }
        catch (SftpException e)
        {
            String msg = e.toString().trim().toLowerCase();
            if (!msg.contains("nosuchfileexception"))
                throw e;
        }

        // rename .els-part file to original
        jSftp.rename(copyDest, dest);

        jSftp.disconnect();
    }

}
