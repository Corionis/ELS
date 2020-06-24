package com.groksoft.volmunger.comm;

import java.io.IOException;
import java.util.Arrays;

import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.file.nativefs.NativeFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.scp.ScpCommandFactory;
import org.apache.sshd.server.session.ServerSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystem;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemFactory;

import javax.swing.filechooser.FileSystemView;

/*
 *  See:
  *  https://mina.apache.org/sshd-project
  *  https://github.com/apache/mina-sshd/blob/master/docs/sftp.md
  *  https://mina.apache.org/sshd-project/apidocs/index.html?org/apache/sshd/server/SshServer.html
 */

public class Transfer
{
    private transient Logger logger = LogManager.getLogger("applog");

    private SshClient client;
    private String hostname;
    private String hostport;
    private SshServer sshd;

    private Transfer()
    {
        // hide default constructor
    }

    public Transfer(String name, String port)
    {
        hostname = name;
        if (hostname.length() < 1)
            hostname = "localhost";

        hostport = port;
        if (port.length() < 1)
            hostport = "50271";
    }

    public void startServer()
    {
        try
        {
            sshd = SshServer.setUpDefaultServer();
            sshd.setHost(hostname);
            sshd.setPort(Integer.valueOf(hostport) + 1);
            sshd.setPublickeyAuthenticator(null);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
//            sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystemFactory()));
            sshd.setCommandFactory(new ScpCommandFactory());
            sshd.setPasswordAuthenticator(new PasswordAuthenticator()
            {
                @Override
                public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException
                {

                    logger.info("Transfer connected " + sshd.getHost() + ":" + sshd.getPort());
                    return true;
                }
            });

            logger.info("Transfer starting secure channel ...");
            sshd.start();
            logger.info("Transfer listener started");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.info("Transfer cannot start secure channel");
        }
    }

    public void stop () throws IOException
    {
        logger.info("Transfer listener stopping");
        sshd.stop();
    }

    public void startClient()
    {
        client = SshClient.setUpDefaultClient();
        //ClientSession session = client.cr
    }

}
