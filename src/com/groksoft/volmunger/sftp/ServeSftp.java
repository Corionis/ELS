package com.groksoft.volmunger.sftp;

import com.groksoft.volmunger.Utils;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.SftpErrorStatusDataHandler;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemEnvironment;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import java.io.IOException;
import java.util.Collections;

/*
 *  See:
 *  https://mina.apache.org/sshd-project
 *  https://github.com/apache/mina-sshd/blob/master/docs/sftp.md
 *
 *  https://javadoc.io/doc/org.apache.sshd
 *  https://javadoc.io/doc/org.apache.sshd/sshd-sftp/latest/index.html
 */

public class ServeSftp implements SftpErrorStatusDataHandler
{
    private transient Logger logger = LogManager.getLogger("applog");

    private String hostname;
    private int hostport;
    private int loginAttempts = 1;
    private String loginAttemptAddress = "";
    private Repository myRepo;
    private String password;
    private SshServer sshd;
    private Repository theirRepo;
    private String user;

    private ServeSftp()
    {
        // hide default constructor
    }

    public ServeSftp(Repository mine, Repository theirs)
    {
        myRepo = mine;
        theirRepo = theirs;

        hostname = Utils.parseHost(theirRepo.getLibraryData().libraries.site);
        hostport = Utils.getPort(theirRepo.getLibraryData().libraries.site) + 1;

        user = myRepo.getLibraryData().libraries.key;
        password = theirRepo.getLibraryData().libraries.key;
    }

    @Override
    public String resolveErrorMessage(SftpSubsystemEnvironment sftpSubsystem, int id, Throwable e, int subStatus, int cmd, Object... args)
    {
        //logger.info("resolveErrorMessage command: " + cmd + " " + args.toString() + "\r\n" + e.toString());
        return e.toString();
    }

    @Override
    public int resolveSubStatus(SftpSubsystemEnvironment sftpSubsystem, int id, Throwable e, int cmd, Object... args)
    {
        //logger.info("resolveSubStatus: " + cmd + " " + args.toString() + "\r\n" + e.getMessage());
        return 1;
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
                    boolean authenticated = false;

                    if (s.equals(user) && s1.equals(password))
                    {
                        authenticated = true;
                        loginAttempts = 1;
                        loginAttemptAddress = "";
                        logger.info("ServeSftp server connected to " + serverSession.getClientAddress());
                    }
                    else
                    {
                        if (serverSession.getClientAddress().toString().equals(loginAttemptAddress))
                        {
                            ++loginAttempts;
                        }
                        else
                        {
                            loginAttempts = 1;
                        }
                        loginAttemptAddress = serverSession.getClientAddress().toString();
                        logger.warn("Sftp login attempt " + loginAttempts + " failed using \"" + user + "\n/\"" + password + "\n from " + serverSession.getClientAddress());
                        if (loginAttempts > 3)
                        {
                            // todo Random sleep, 1 to 3 minutes
                        }
                    }
                    return authenticated;
                }
            });

            //logger.info("ServeSftp server starting secure channel listener");
            sshd.start();
            logger.info("ServeSftp server is listening on " + sshd.getHost() + ":" + sshd.getPort());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("ServeSftp server cannot start secure channel");
        }
    }

    public void stopServer() throws IOException
    {
        logger.info("ServeSftp server listener stopping");
        sshd.stop();
    }

}
