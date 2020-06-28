package com.groksoft.volmunger.sftp;

import java.io.IOException;
import java.util.Collections;

import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemEnvironment;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

/*
 *  See:
 *  https://mina.apache.org/sshd-project
 *  https://github.com/apache/mina-sshd/blob/master/docs/sftp.md
 *
 *  https://javadoc.io/doc/org.apache.sshd
 *  https://javadoc.io/doc/org.apache.sshd/sshd-sftp/latest/index.html
 */

public class Server implements SftpErrorStatusDataHandler
{
    private transient Logger logger = LogManager.getLogger("applog");

    private String hostname;
    private int hostport;
    private String password;
    private Repository publisherRepo;
    private SshServer sshd;
    private Repository subscriberRepo;
    private String user;

    private Server()
    {
        // hide default constructor
    }

    public Server(Repository publisherRep, Repository subscriberRep)
    {
        subscriberRepo = subscriberRep;
        publisherRepo = publisherRep;

        hostname = Utils.parseHost(subscriberRepo.getLibraryData().libraries.site);
        hostport = Utils.getPort(subscriberRepo.getLibraryData().libraries.site) + 1;

        user = publisherRepo.getLibraryData().libraries.key;
        password = subscriberRepo.getLibraryData().libraries.key;
    }

    public void startServer()
    {
        try
        {
            sshd = SshServer.setUpDefaultServer();
            sshd.setHost(hostname);
            sshd.setPort(hostport);
            sshd.setPublickeyAuthenticator(null);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

            SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder()
                    //.withFileSystemAccessor(new FileSystemAccessor())
                    .withSftpErrorStatusDataHandler(this)
                    .build();

            //factory.addSftpEventListener(new EventListener());

            sshd.setSubsystemFactories(Collections.singletonList(factory));

            sshd.setPasswordAuthenticator(new PasswordAuthenticator()
            {
                @Override
                public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException
                {
                    logger.info("Sftp server connected to " + serverSession.getClientAddress());
                    return true;
                }
            });

            //logger.info("Sftp server starting secure channel listener");
            sshd.start();
            logger.info("Sftp server is listening on " + sshd.getHost() + ":" + sshd.getPort());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("Sftp server cannot start secure channel");
        }
    }

    public void stopServer() throws IOException
    {
        logger.info("Sftp server listener stopping");
        sshd.stop();
    }

    @Override
    public int resolveSubStatus(SftpSubsystemEnvironment sftpSubsystem, int id, Throwable e, int cmd, Object... args)
    {
        logger.info("resolveSubStatus: " + cmd + " " + args.toString() + "\r\n" + e.getMessage());
        return 0;
    }

    @Override
    public String resolveErrorMessage(SftpSubsystemEnvironment sftpSubsystem, int id, Throwable e, int subStatus, int cmd, Object... args)
    {
        logger.info("resolveErrorMessage: " + cmd + " " + args.toString() + "\r\n" + e.getMessage());
        return null;
    }

}
