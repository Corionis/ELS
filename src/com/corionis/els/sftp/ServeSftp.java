package com.corionis.els.sftp;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintKeys;
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.SocketAddress;
import java.security.PublicKey;
import java.text.MessageFormat;
import java.util.*;

/**
 * SFTP server class
 * <br/>
 *  See:<br/>
 *  <a href="https://mina.apache.org/sshd-project">sshd-project</a>  <br/>
 *  <a href="https://javadoc.io/doc/org.apache.sshd">sshd</a>  <br/>
 *  <a href="https://github.com/apache/mina-sshd/blob/master/docs/sftp.md">sftp</a>  <br/>
 *  <a href="https://javadoc.io/doc/org.apache.sshd/sshd-sftp/latest/index.html">sshd-sftp</a>  <br/>
 */
public class ServeSftp implements SftpErrorStatusDataHandler
{
    private Context context;
    private String hostListen;
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

        String address = "";
        if (context.cfg.getOverrideSubscriberHost().isEmpty() || context.cfg.getOverrideSubscriberHost().trim().equals("true"))
        {
            address = myRepo.getLibraryData().libraries.listen;
            hostListen = context.cfg.gs("Z.listen");
            if (address == null || address.isEmpty())
            {
                address = myRepo.getLibraryData().libraries.host;
                hostListen = context.cfg.gs("Z.host");
            }
        }
        else
        {
            address = context.cfg.getOverrideSubscriberHost();
            hostListen = context.cfg.gs("Z.custom");
        }

        hostname = Utils.parseHost(address);
        listenport = Utils.getPort(address) + ((primaryServers) ? 1 : 3);

        if (theirRepo != null)
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

    private boolean isListed(SocketAddress socketAddress, boolean whiteList) throws IOException
    {
        boolean sense = whiteList;
        String file = (whiteList ? context.cfg.getIpWhitelist() : context.cfg.getBlacklist());
        if (file != null && file.length() > 0)
        {
            String filename = Utils.getFullPathLocal(file);
            if (filename.length() > 0)
            {
                String inet = socketAddress.toString();
                if (inet != null)
                {
                    sense = false;
                    inet = inet.replaceAll("/", "");
                    inet = inet.replaceAll("\\\\", "");
                    inet = inet.substring(0, inet.lastIndexOf(":"));
                    BufferedReader br = new BufferedReader(new FileReader(filename));
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        line = line.trim();
                        if (line.length() > 0 && !line.startsWith("#"))
                        {
                            if (inet.equals(line))
                            {
                                sense = true;
                                break;
                            }
                        }
                    }
                    br.close();
                }
            }
        }
        return sense;
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

                    // check blacklist then whitelist
                    boolean ipVerified = false;
                    try
                    {
                        SocketAddress sa = serverSession.getClientAddress();
                        if (isListed(sa, false)) // blacklisted, disconnect
                        {
                            logger.warn(context.cfg.gs("Comm.blacklisted.ip") +
                                    sa.toString().replaceAll("/", "").replaceAll("\\\\", "") +
                                    context.cfg.gs("Comm.attempted.login"));
                        }
                        else if (isListed(sa, true)) // if it is whitelisted or there is no whitelist
                        {
                            ipVerified = true;
                        }
                        else // not whitelisted, disconnect
                        {
                            logger.warn(context.cfg.gs("Comm.not.whitelisted.ip") +
                                    sa.toString().replaceAll("/", "").replaceAll("\\\\", "") +
                                    context.cfg.gs("Comm.attempted.login"));
                        }
                    }
                    catch (IOException e)
                    {
                        logger.error(Utils.getStackTrace(e));
                    }

                    // check credentials
                    if (ipVerified)
                    {
                        HintKeys keys = (context.authKeys != null) ? context.authKeys : context.hintKeys;
                        if (keys != null)
                        {
                            HintKey connectedKey = keys.findKey(password);  // look for matching key in hints keys file
                            if (connectedKey != null)
                            {
                                authenticated = true;
                                loginAttempts = 1;
                                loginAttemptAddress = "";
                                String them = "";
                                HintKey theirKey = keys.findKey(s);
                                if (theirKey != null)
                                    them = MessageFormat.format(context.cfg.gs("Z.at"), theirKey.system);
                                logger.info(context.cfg.gs("Sftp.server.authenticated") + them + serverSession.getClientAddress().toString());
                            }
                        }
                        else if (s.equals(user) && s1.equals(password))
                        {
                            authenticated = true;
                            loginAttempts = 1;
                            loginAttemptAddress = "";
                            logger.info(context.cfg.gs("Sftp.server.authenticated") + serverSession.getClientAddress().toString());
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
                            logger.warn(MessageFormat.format(context.cfg.gs("Sftp.login.attempt.failed.user"), loginAttempts) +
                                    user + "\n/\"" + password + context.cfg.gs("Sftp.from") + serverSession.getClientAddress());
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
                                    //
                                }
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
                    logger.fatal(context.cfg.gs("Sftp.session.time.out.ending.session") + tor);
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
            logger.trace(context.cfg.gs("Sftp.setting.sftp.idle.timeout.to") + tout);
            sshd.getProperties().put("idle-timeout", tout); // sftp idle time-out
            Object o = sshd.getProperties().get("idle-timeout");
            logger.trace(context.cfg.gs("Sftp.idle.timeout.is") + o.toString());

            sshd.getProperties().put("sftp-auto-follow-links", true);

            // run the SFTP server
            sshd.start();

            // assemble listen IP(s)
            String ips = getIps();
            logger.info(context.cfg.gs("Sftp.server.is.listening.on") + ips.trim() + hostListen);
        }
        catch (IOException e)
        {
            context.fault = true;
            e.printStackTrace();
            logger.warn(context.cfg.gs("Sftp.server.cannot.start.secure.channel"));
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
                logger.debug(context.cfg.gs("Sftp.stopping.sftp.server.on") + ips + hostListen);
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
