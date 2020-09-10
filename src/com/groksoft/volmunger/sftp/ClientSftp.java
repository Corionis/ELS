package com.groksoft.volmunger.sftp;

import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Libraries;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.impl.DefaultSftpClientFactory;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

/**
 * ClientSftp -to- ServerSftp
 */
public class ClientSftp implements SftpErrorStatusDataHandler
{
    private final int BUFFER_SIZE = 1048576;
    private transient byte[] buffer;

    private String hostname;
    private int hostport;
    private transient Logger logger = LogManager.getLogger("applog");
    private Repository myRepo;
    private String password;
    private ClientSession session;
    private SftpClient sftpClient;
    private SshClient sshClient;
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
    public ClientSftp(Repository mine, Repository theirs, boolean primaryServers)
    {
        myRepo = mine;
        theirRepo = theirs;

        hostname = Utils.parseHost(theirRepo.getLibraryData().libraries.host);
        hostport = Utils.getPort(theirRepo.getLibraryData().libraries.host) + ((primaryServers) ? 1 : 3);

        user = myRepo.getLibraryData().libraries.key;
        password = theirRepo.getLibraryData().libraries.key;
    }

    /**
     * Make a remote directory tree
     *
     * @param pathname Path and filename. Note that an ending filename is required but not used
     * @return True if any directories were created
     * @throws IOException
     */
    public String makeRemoteDirectory(String pathname) throws Exception
    {
        if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.WINDOWS))
        {
            pathname = pathname.replaceAll("\\\\", "\\\\\\\\");
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
                if (whole.equals(sep))
                    continue;

                // try to create next directory segment
                sftpClient.mkdir(whole);
            }
            catch (IOException e)
            {
                String msg = e.toString().trim().toLowerCase();
                if (msg.startsWith("sftp error"))
                {
                    if (!msg.contains("alreadyexists")) // ignore "already exists" errors
                        throw e;
                }
            }
        }
        return whole;
    }

    /**
     * Start this sftp client
     */
    public boolean startClient()
    {
        try
        {
            sshClient = SshClient.setUpDefaultClient();

            sshClient.setServerKeyVerifier(new ServerKeyVerifier()
            {
                @Override
                public boolean verifyServerKey(ClientSession clientSession, SocketAddress socketAddress, PublicKey publicKey)
                {
                    // IDEA Cross-key verification could be added plus keys in the JSON library file if higher security is needed
                    return true;
                }
            });

            sshClient.start();

            logger.info("Opening sftp connection to: " + (hostname == null ? "localhost" : hostname) + ":" + hostport);
            session = sshClient.connect(user, hostname, hostport).verify(180000L).getSession();
            session.addPasswordIdentity(password);
            session.auth().verify(180000L).await();

            sftpClient = DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Stop this sftp client
     */
    public void stopClient()
    {
        try
        {
            if (sftpClient != null)
                session.close();

            if (sshClient != null)
                sshClient.close();
        }
        catch (IOException e)
        {

        }
    }

    /**
     * Transmit a file from local to remote system
     *
     * @param src  Source file path with local separators
     * @param dest Destination file path with remove separators
     * @throws IOException
     */
    public void transmitFile(String src, String dest, boolean overwrite) throws IOException
    {
        try
        {
            SftpClient.Attributes destAttr = null;
            int readOffset = 0;
            long writeOffset = 0L;

            String copyDest = dest + ".part";

            // does the destination already exist?
            // automatically resume/continue transfer
            try
            {
                destAttr = sftpClient.stat(copyDest);
                if (destAttr != null)
                {
                    if (destAttr.isRegularFile() && destAttr.getSize() > 0)
                    {
                        if (!overwrite)
                        {
                            readOffset = (int) destAttr.getSize();
                            writeOffset = readOffset;
                        }
                    }
                }
            }
            catch (IOException e)
            {
                String msg = e.toString().trim().toLowerCase();
                if (msg.startsWith("sftp error"))
                {
                    if (!msg.contains("nosuchfileexception"))
                        throw e;
                }
                destAttr = null;
            }

            if (destAttr == null) // file does not exist, try making directory tree
            {
                makeRemoteDirectory(copyDest);
            }

            // append to existing file, otherwise create
            Collection<SftpClient.OpenMode> mode;
            if (writeOffset > 0L)
            {
                mode = EnumSet.of(
                        SftpClient.OpenMode.Read,
                        SftpClient.OpenMode.Write,
                        SftpClient.OpenMode.Append,
                        SftpClient.OpenMode.Exclusive);
                logger.warn("Resuming partial transfer");
            }
            else
            {
                mode = EnumSet.of(
                        SftpClient.OpenMode.Read,
                        SftpClient.OpenMode.Write,
                        SftpClient.OpenMode.Create,
                        SftpClient.OpenMode.Exclusive);
            }

            // open remote file
            SftpClient.Handle handle = sftpClient.open(copyDest, mode);

            // open local file
            FileInputStream srcStream = new FileInputStream(src);
            srcStream.skip(readOffset);

            // copy with chunks to avoid out of memory problems
            buffer = new byte[BUFFER_SIZE];
            int size = 0;
            while (true)
            {
                size = srcStream.read(buffer, 0, BUFFER_SIZE);
                if (size < 1)
                    break;
                sftpClient.write(handle, writeOffset, buffer, 0, size);
                Arrays.fill(buffer, (byte) 0);
                writeOffset += size;
            }

            //SftpClient.Attributes attr = new SftpClient.Attributes().perms(Utils.getLocalPermissions(src));
            //if (theirRepo.getLibraryData().libraries.flavor.equalsIgnoreCase(Libraries.LINUX))
            //    attr.setPermissions(0x644);
            //sftpClient.setStat(handle, attr);

            srcStream.close();
            sftpClient.close(handle);

            // delete old file
            try
            {
                sftpClient.remove(dest);
            }
            catch (FileNotFoundException fnf)
            {
                // ignore FileNotFoundException
            }
            catch (IOException e)
            {
                String msg = e.toString().trim().toLowerCase();
                if (msg.startsWith("sftp error"))
                {
                    if (!msg.contains("nosuchfileexception"))
                        throw e;
                }
            }

            // rename .part file
            sftpClient.rename(copyDest, dest);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage() + "\r\n" + Utils.getStackTrace(e));
        }
    }

}
