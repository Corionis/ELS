package com.groksoft.volmunger.sftp;

import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.subsystem.sftp.SftpClient;
import org.apache.sshd.client.subsystem.sftp.impl.DefaultSftpClientFactory;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

public class Client implements SftpErrorStatusDataHandler
{
    private transient Logger logger = LogManager.getLogger("applog");
    private transient byte[] buffer;

    private String hostname;
    private int hostport;
    private String password;
    private Repository myRepo;
    private Repository theirRepo;
    private SftpClient sftpClient;
    private SshClient sshClient;
    private ClientSession session;
    private String user;

    private final int BUFFER_SIZE = 1048576;
// todo    private final int BUFFER_SIZE = 10485760;

    private Client()
    {
        // hide default constructor
    }

    public Client(Repository mine, Repository theirs)
    {
        myRepo = mine;
        theirRepo = theirs;

        hostname = Utils.parseHost(theirRepo.getLibraryData().libraries.site);
        hostport = Utils.getPort(theirRepo.getLibraryData().libraries.site) + 1;

        user = myRepo.getLibraryData().libraries.key;
        password = theirRepo.getLibraryData().libraries.key;
    }

    public void startClient()
    {
        try
        {
            sshClient = SshClient.setUpDefaultClient();
            sshClient.start();

            session = sshClient.connect(user, hostname, hostport).verify(180000L).getSession();
            session.addPasswordIdentity(password);
            session.auth().verify(180000L).await();

            sftpClient = DefaultSftpClientFactory.INSTANCE.createSftpClient(session);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
    }

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

    public void transmitFile(String src, String dest) throws IOException
    {
        try
        {
            SftpClient.Attributes destAttr;
            SftpClient.Attributes srcAttr;
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
                        readOffset = (int) destAttr.getSize();
                        writeOffset = readOffset;
                    }
                }
            }
            catch (IOException e)
            {
                if (!e.toString().trim().toLowerCase().startsWith("sftp error (ssh_fx_ok):"))
                {
                    throw e;
                }
                destAttr = null;
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
            SftpClient.Attributes attr = new SftpClient.Attributes().perms(Utils.getLocalPermissions(src));
            sftpClient.setStat(handle, attr);

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
                logger.error(e.getMessage());
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
