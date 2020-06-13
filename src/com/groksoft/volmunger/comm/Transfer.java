package com.groksoft.volmunger.comm;

import java.io.IOException;
import java.util.Arrays;

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

public class Transfer
{
    private SshServer sshd;
    private transient Logger logger = LogManager.getLogger("applog");

    public void start() {
        try {
            sshd = SshServer.setUpDefaultServer();
            sshd.setPort(50272);
            sshd.setHost("localhost");
            sshd.setPublickeyAuthenticator(null);
            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
            sshd.setSubsystemFactories(Arrays.<NamedFactory<Command>>asList(new SftpSubsystemFactory()));
            sshd.setCommandFactory(new ScpCommandFactory());
            sshd.setPasswordAuthenticator(new PasswordAuthenticator()
            {
                @Override
                public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException {
                    return true;
                }
            });

            logger.info("Starting secure transfer channel ...");
            sshd.start();
            logger.info("Started");

        } catch (IOException e) {
            e.printStackTrace();
            logger.info("Can not start secure transfer channel");
        }
    }

}
