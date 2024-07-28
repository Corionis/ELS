package com.corionis.els.sftp;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.repository.Libraries;
import com.corionis.els.repository.Repository;
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
    private Context context;
    private String hostListen;
    private String hostname;
    private int hostport;
    private Channel jChannel;
    private Session jSession;
    private JSch jsch;
    private transient Logger logger = LogManager.getLogger("applog");
    private Repository myRepo;
    private String password;
    private String purpose = "";
    private Repository theirRepo;
    private String user;

    private ClientSftp()
    {
        // hide default constructor
    }

    /**
     * Constructor
     *
     * @param context The Context
     * @param mine   Repository of local system
     * @param theirs Repository of remote system
     * @param primaryServers Is this the primary or secondary servers (for -r L) setup
     */
    public ClientSftp(Context context, Repository mine, Repository theirs, boolean primaryServers)
    {
        this.context = context;
        myRepo = mine;
        theirRepo = theirs;

        String address;
        if (context.cfg.isOverrideSubscriberHost())
        {
            address = theirRepo.getLibraryData().libraries.listen;
            if (address == null || address.isEmpty())
                address = theirRepo.getLibraryData().libraries.host;
            hostListen = context.cfg.gs("Z.listen");
        }
        else
        {
            address = theirRepo.getLibraryData().libraries.host;
            hostListen = context.cfg.gs("Z.host");
        }

        hostname = Utils.parseHost(address);
        hostport = Utils.getPort(address) + ((primaryServers) ? 1 : 3);

        user = myRepo.getLibraryData().libraries.key;
        password = theirRepo.getLibraryData().libraries.key;
    }

    /**
     * Establish a remote channel connection
     *
     * @return ChannelSftp object or null
     */
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

    /**
     * Get a remote file
     *
     * @param source Source path of file
     * @param dest Destination path on local system
     * @throws Exception
     */
    public void get(String source, String dest) throws Exception
    {
        ChannelSftp jSftp = connect();
        source = Utils.pipe(source);
        source = Utils.unpipe(source, "/");
        dest = Utils.pipe(dest);
        dest = Utils.unpipe(dest, "/");
        if (context.progress != null)
            jSftp.get(source, dest, context.progress);
        else
            jSftp.get(source, dest);
        jSftp.disconnect();
    }

    /**
     * Is the sftp connected?
     */
    public boolean isConnected()
    {
        boolean connected = false;
        if (jChannel != null && jSession != null)
            connected = true;
        return connected;
    }

    /**
     * List a remote directory
     *
     * @param directory Path to list
     * @return Vector of entries
     * @throws Exception
     */
    public synchronized Vector listDirectory(String directory) throws Exception
    {
        ChannelSftp jSftp = connect();
        directory = Utils.pipe(directory);
        directory = Utils.unpipe(directory, "/");
        Vector listing = jSftp.ls(directory);
        jSftp.disconnect();
        return listing;
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
        pathname = Utils.pipe(pathname);
        String[] parts = pathname.split("\\|");

        String sep = "/";
        String whole = "";
        for (int i = 0; i < parts.length - 1; ++i)
        {
            try
            {
                // is it a Windows drive letter: ?
                if (parts[i].endsWith(":"))
                {
                    // don't try to create a Windows root directory, e.g. C:\
                    if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.WINDOWS)) // && parts[i].length() == 2)
                    {
                        whole = sep + parts[i];
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

    /**
     * Remove a remote file or directory
     *
     * @param path Remote path
     * @param isDir True if path is a directory
     * @throws Exception
     */
    public void remove(String path, boolean isDir) throws Exception
    {
        ChannelSftp jSftp = connect();
        path = Utils.pipe(path);
        path = Utils.unpipe(path, "/");
        if (isDir)
            jSftp.rmdir(path);
        else
            jSftp.rm(path);
        jSftp.disconnect();
    }

    /**
     * Rename a remote file or directory
     *
     * @param from Remote path to rename
     * @param to New name
     * @throws Exception
     */
    public void rename(String from, String to) throws Exception
    {
        ChannelSftp jSftp = connect();
        from = Utils.pipe(from);
        from = Utils.unpipe(from, "/");
        to = Utils.pipe(to);
        to = Utils.unpipe(to, "/");
        jSftp.rename(from, to);
        jSftp.disconnect();
    }

    /**
     * Set the modified time of a remote file or directory
     *
     * @param dest Remote path
     * @param mtime Modified date/time
     * @throws Exception
     */
    public void setDate(String dest, long mtime) throws Exception
    {
        ChannelSftp jSftp = connect();
        dest = Utils.pipe(dest);
        dest = Utils.unpipe(dest, "/");
        jSftp.setMtime(dest, (int) mtime);
        jSftp.disconnect();
    }

    /**
     * Start this sftp client
     */
    public boolean startClient(String purpose)
    {
        this.purpose = purpose;
        try
        {
            logger.info("Opening sftp " + purpose + " connection to: " + (hostname == null ? "localhost" : hostname) + ":" + hostport + hostListen);
            jsch = new JSch();
            jSession = jsch.getSession(user, hostname, hostport);
            jSession.setConfig("StrictHostKeyChecking", "no");
            jSession.setPassword(password);

            // Could implement strict key checking if more security is needed
            //jsch.setKnownHosts("known_hosts");
            //jsch.addIdentity("id_rsa");

            jSession.connect(60000); // sftp session connection time-out, 60 secs

            //jSession.setTimeout(theirRepo.getLibraryData().libraries.timeout * 60 * 1000); // inactivity time-out; NOW handled per-operation
            logger.trace("client sftp timeout is " + jSession.getTimeout());
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
        path = Utils.pipe(path);
        path = Utils.unpipe(path, "/");
        SftpATTRS attrs = jSftp.stat(path);
        jSftp.disconnect();
        return attrs;
    }

    /**
     * Stop this sftp client
     */
    public void stopClient()
    {
        logger.debug(java.text.MessageFormat.format(context.cfg.gs("Main.disconnecting.sftp"), purpose, (hostname == null ? "localhost" : hostname)) + hostport);
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
        long readOffset = 0;
        long writeOffset = 0L;

        ChannelSftp jSftp = connect();
        String copyDest = Utils.pipe(dest + ".els-part");
        copyDest = Utils.pipe(copyDest);
        copyDest = Utils.unpipe(copyDest, "/");

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
                        readOffset = destAttr.getSize();
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
        if (context.progress != null)
            jSftp.put(src, copyDest, context.progress, mode);
        else
            jSftp.put(src, copyDest, mode);

        // delete any old original file
        try
        {
            dest = Utils.pipe(dest);
            dest = Utils.unpipe(dest, "/");
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
