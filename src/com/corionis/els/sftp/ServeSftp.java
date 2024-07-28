package com.corionis.els.sftp;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.hints.HintKey;
import com.corionis.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.session.SessionDisconnectHandler;
import org.apache.sshd.common.session.helpers.AbstractSession;
import org.apache.sshd.common.session.helpers.TimeoutIndicator;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpErrorStatusDataHandler;
import org.apache.sshd.sftp.server.SftpSubsystemEnvironment;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;

import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.util.*;

/**
 * SFTP server class
 * <br/>
 *  See:<br/>
 *  https://mina.apache.org/sshd-project  <br/>
 *  https://github.com/apache/mina-sshd/blob/master/docs/sftp.md  <br/>
 *  https://javadoc.io/doc/org.apache.sshd  <br/>
 *  https://javadoc.io/doc/org.apache.sshd/sshd-sftp/latest/index.html  <br/>
 */
public class ServeSftp implements SftpErrorStatusDataHandler
{
    private Context context;
    private String hostname;
    private int listenport;
    private transient Logger logger = LogManager.getLogger("applog");
    private String loginAttemptAddress = "";
    private int loginAttempts = 1;
    private Repository myRepo;
    private String password;
    private SshServer sshd;
    private Repository theirRepo;
    private String user;

    private ServeSftp()
    {
        // hide default constructor
    }

    /**
     * Instantiate this class.
     *
     * @param mine   Repository of local system
     * @param theirs Repository of remote system
     */
    public ServeSftp(Context ctxt, Repository mine, Repository theirs, boolean primaryServers)
    {
        context = ctxt;
        myRepo = mine;
        theirRepo = theirs;

        String address = myRepo.getLibraryData().libraries.listen;
        if (address == null || address.isEmpty())
            address = myRepo.getLibraryData().libraries.host;

        hostname = Utils.parseHost(address);
        listenport = Utils.getPort(address) + ((primaryServers) ? 1 : 3);

        user = theirRepo.getLibraryData().libraries.key;
        password = myRepo.getLibraryData().libraries.key;
    }

    /**
     * Get a formatted String of bound IP addresses for this session
     *
     * @return
     */
    private String getIps()
    {
        // assemble listen IP(s)
        String ips = "";
        Set<SocketAddress> addrs;
        addrs = sshd.getBoundAddresses();
        for (SocketAddress a : addrs)
        {
            ips = ips + a.toString() + " ";
        }
        return ips;
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

    /**
     * Start this SFTP server session
     */
    public void startServer()
    {
        try
        {
            sshd = SshServer.setUpDefaultServer();
            sshd.setHost(hostname);
            sshd.setPort(listenport);

            try
            {
                sshd.setPublickeyAuthenticator(new PublickeyAuthenticator()
                {
                    @Override
                    public boolean authenticate(String s, PublicKey publicKey, ServerSession serverSession) throws AsyncAuthException
                    {
                        // Public key verification could be added if higher security is needed
                        return true;
                    }
                });
            }
            catch (AsyncAuthException aae)
            {
                logger.warn("AsyncAuthException");
            }

            sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

            SftpSubsystemFactory factory = new SftpSubsystemFactory.Builder().withSftpErrorStatusDataHandler(this).build();
            factory.addSftpEventListener(new EventListener());
            sshd.setSubsystemFactories(Collections.singletonList(factory));

            sshd.setPasswordAuthenticator(new PasswordAuthenticator()
            {
                @Override
                public boolean authenticate(String s, String s1, ServerSession serverSession) throws PasswordChangeRequiredException, AsyncAuthException
                {
                    boolean authenticated = false;

                    if (context.hintKeys != null)
                    {
                        HintKey connectedKey = context.hintKeys.findKey(password);  // look for matching key in hints keys file
                        if (connectedKey != null)
                        {
                            authenticated = true;
                            loginAttempts = 1;
                            loginAttemptAddress = "";
                            String them = "";
                            HintKey theirKey = context.hintKeys.findKey(s);
                            if (theirKey != null)
                                them = theirKey.system + " at ";
                            logger.info("Sftp server authenticated: " + them + serverSession.getClientAddress().toString());
                        }
                    } else if (s.equals(user) && s1.equals(password))
                    {
                        authenticated = true;
                        loginAttempts = 1;
                        loginAttemptAddress = "";
                        logger.info("Sftp server authenticated: " + serverSession.getClientAddress().toString());
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
                        logger.warn("sftp login attempt " + loginAttempts + " failed, user \"" + user + "\n/\"" + password + "\n from " + serverSession.getClientAddress());
                        if (loginAttempts > 3)
                        {
                            try
                            {
                                // random sleep for 1-3 minutes to discourage automated attacks
                                Random rand = new Random();
                                Thread.sleep(rand.nextInt(3) * 2000L);
                            }
                            catch (InterruptedException e)
                            {
                            }
                        }
                    }
                    return authenticated;
                }
            });

            SessionDisconnectHandler disconnector = new SessionDisconnectHandler()
            {
                @Override
                public boolean handleTimeoutDisconnectReason(Session session, TimeoutIndicator timeoutStatus) throws IOException
                {
                    String tor = timeoutStatus.getStatus().toString();
                    logger.fatal("ELS sftp session time-out, ending session, " + tor);
                    context.timeout = true;
                    context.fault = true;
                    return true;
                }
            };
            sshd.setSessionDisconnectHandler(disconnector);

            // infinite inactivity time-out for sftp; timeouts in stty control process
            //int tout = theirRepo.getLibraryData().libraries.timeout * 60 * 1000;
            int tout = 0;

            // set the default idle timeout
            logger.trace("Setting sftp idle timeout to " + tout);
            sshd.getProperties().put("idle-timeout", tout); // sftp idle time-out
            Object o = sshd.getProperties().get("idle-timeout");
            logger.trace("sftp idle timeout is " + o.toString());

            // run the SFTP server
            sshd.start();

            // assemble listen IP(s)
            String ips = getIps();
            logger.info("Sftp server is listening on: " + ips);
        }
        catch (IOException e)
        {
            context.fault = true;
            e.printStackTrace();
            logger.warn("sftp server cannot start secure channel");
        }
    }

    /**
     * Stop this SFTP server
     */
    public void stopServer()
    {
        try
        {
            if (sshd != null)
            {
                String ips = getIps();
                logger.debug("stopping sftp server on: " + ips);
                List<AbstractSession> sessions = sshd.getActiveSessions();
                if (sessions != null)
                {
                    for (AbstractSession session : sessions)
                    {
                        session.close();
                    }
                }
                sshd.stop();
                sshd = null;
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

}
