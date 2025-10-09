package com.corionis.els.tools.email;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.tools.email.EmailUI;
import com.corionis.els.repository.Libraries;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.List;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Email class
 * <p>
 * Implemented using Jakarta Mail:  https://jakartaee.github.io/mail-api/
 * <p>
 * Download:  https://eclipse-ee4j.github.io/angus-mail/
 * <p>
 * Implemented using OAuth2 and PKCE:  https://datatracker.ietf.org/doc/html/rfc7636
 *
 */
public class EmailHandler extends Thread
{
    // GMail : https://developers.google.com/identity/protocols/oauth2/
    private final String GOOGLE_CLIENT_ID = "863161091698-hj38ceukvile50k4qruj0p6mdjith5l8.apps.googleusercontent.com";

    // Outlook : https://learn.microsoft.com/en-us/entra/identity-platform/v2-protocols-oidc
    private final String OUTLOOK_CLIENT_ID = "28d6da7f-9a7c-4cea-aea5-73b9c59c7a15";
    private final int OUTLOOK_FIXED_PORT = 60271;

    // Zoho : https://www.zoho.com/accounts/protocol/oauth/mobile-applications.html
    private final String ZOHO_CLIENT_ID = "1000.88UA8YCET6S12R7Y9U6KKS2EFMOJQZ";
    private final int ZOHO_FIXED_PORT = 60271;

    private final Logger logger = LogManager.getLogger("applog");
    public static enum Function {AUTH, FAULT, SEND, TEST};

    private String api_domain = "";
    private String attachment = null;
    private String authToken;
    private String codeVerifier;
    private String codeChallenge;
    private final Context context;
    private EmailUI emailUi = null;
    private String error = "";
    private boolean expired = false;
    private boolean fault = false;
    private Function function = null;
    private HttpExchange httpExchange = null;
    private int httpPort = -1;
    private String location = null;
    private boolean nothingToDo = false;
    private String now;
    private Map<String, List> parameters = null;
    private String response = "";
    private boolean responseCompleted = false;
    private String scope = null;
    private HttpServer server = null;
    private int stateInt;
    private boolean stop = false;
    private boolean success = false;
    private boolean timeout = false;
    private EmailTool tool = null;
    private boolean workerRunning = false;

    public EmailHandler(Context context, EmailUI emailUi, EmailTool tool, Function function)
    {
        this.context = context;
        this.emailUi = emailUi;
        this.tool = tool;
        this.function = function;
    }

    /**
     * Authenticate user credentials, get authorization code and access & refresh tokens
     * <p>
     * Used by Navigator
     */
    private void authenticate()
    {
        fault = false;
        success = false;
        timeout = false;

        // Start HTTP server on a dynamic or fixed port
        startHttpServer();
        if (!fault)
        {
            // Generate random code verifier and code challenge for PKCE
            generatePKCE();
            if (!fault)
            {
                // Generate authorization URI, launch browser for "Sign-In with", authenticate the user
                authenticateUser();
                if (!fault && !stop && !timeout)
                {
                    // Generate POST request and get access & refresh tokens
                    requestAccessAndRefreshTokens();

                    if (!fault && !stop && !timeout)
                    {
                        // Send result landing page to browser
                        writeServerResponse();
                    }
                }
            }
        }

        emailUi.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if (fault)
        {
            String msg = context.cfg.gs("EmailUI.authentication.error") + error;
            logger.error(msg);
            if (emailUi != null)
            {
                emailUi.labelStatus.setText(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("EmailUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            fault = false;
            success = false;
            timeout = false;
        }
        else
        {
            if (timeout)
            {
                String msg = context.cfg.gs("EmailUI.authentication.timeout");
                logger.error(msg);
                emailUi.labelStatus.setText(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("EmailUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                if (!stop)
                {
                    success = true;
                    try
                    {
                        tool.write();
                    }
                    catch (Exception e)
                    {
                        logger.error(Utils.getStackTrace(e));
                        JOptionPane.showMessageDialog(context.navigator.dialogEmail, e.getMessage(), context.cfg.gs("EmailUI.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    emailUi.labelStatus.setText(context.cfg.gs("EmailUI.authentication.success"));
                }
            }
        }

        // stop everything
        requestStop();
        if (workerRunning)
            stopHttpServer();

        emailUi.updateControls();
        emailUi.buttonAuth.setText(context.cfg.gs("EmailUI.buttonAuth.text"));
        emailUi.buttonAuth.setToolTipText(context.cfg.gs("EmailUI.buttonAuth.toolTipText"));
    }

    /**
     * Formulate the URI and launch a browser to "Sign-In As"
     */
    private void authenticateUser()
    {
        // formulate the authorization URI and launch browser
        try
        {
            stateInt = ThreadLocalRandom.current().nextInt(10000, 99999); // random state tracker

            // generate the authorization URI
            URI uri = null;
            switch (tool.getProfile())
            {
                case "Apple":
                    break;
                case "GMail":
                    // https://developers.google.com/identity/protocols/oauth2/native-app
                    uri = new URI("https://accounts.google.com/o/oauth2/v2/auth?" +
                            "client_id=" + GOOGLE_CLIENT_ID + "&" +
                            "response_type=code&" +
                            "redirect_uri=http://127.0.0.1:" + httpPort + "&" +
                            "code_challenge_method=S256" + "&" +
                            "code_challenge=" + codeChallenge + "&" +
                            "scope=email&" +
                            "state=" + stateInt + "&" +
                            "login_hint=" + tool.getUsername());
                    //Force user to reauthorize  - TO BE IMPLEMENTED
                    // prompt=consent
                    break;
                case "Outlook":
                    // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#request-an-authorization-code
                    uri = new URI("https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?" +
                            "client_id=" + OUTLOOK_CLIENT_ID + "&" +
                            "response_type=code&" +
                            "redirect_uri=http://127.0.0.1:" + OUTLOOK_FIXED_PORT + "&" +
                            //"scope=offline_access%20https://graph.microsoft.com/Mail.Send%20SMTP.Send%20openid&" +
                            //"scope=offline_access%20openid%20https://graph.microsoft.com/Mail.Send%20https://graph.microsoft.com/SMTP.Send&" +
                            "scope=offline_access%20openid%20Mail.Send%20SMTP.Send&" +
                            "state=" + stateInt + "&" +
                            "code_challenge_method=S256&" +
                            "code_challenge=" + codeChallenge + "&" +
                            "login_hint=" + tool.getUsername());
                    break;
                case "SMTP":
                    // does not use OAUTH2 access token
                    break;
                case "Zoho":
                    // https://www.zoho.com/accounts/protocol/oauth/mobile-and-desktop-apps/get-authorization-code.html
                    uri = new URI("https://accounts.zoho.com/oauth/v2/auth?" +
                            "client_id=" + ZOHO_CLIENT_ID + "&" +
                            "response_type=code&" +
                            "redirect_uri=http://127.0.0.1:" + ZOHO_FIXED_PORT + "&" +
                            "scope=ZohoMail.messages.CREATE&" +
                            //"scope=ZohoMail.messages.CREATE,ZohoMail.accounts.READ&" +
                            "code_challenge_method=S256&" +
                            "code_challenge=" + codeChallenge + "&" +
                            "access_type=offline");
                    break;
            }

            // launch browser with URI
            assert (uri != null);
            Desktop.getDesktop().browse(uri);
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            error = e.getMessage();
            fault = true;
        }

        if (!fault && !stop && !timeout)
        {
            // Read HTTP server response - blocking
            readServerResponse(600000); // 10 minute timeout
            if (!fault && !stop && !timeout)
            {
                // Parse response and get authorization token
                parameters = parseResponse();
            }
        }
    }

    private String buildOAuth2Token(String email, String accessToken)
    {
        String authString = "user=" + email + "\u0001auth=Bearer " + accessToken + "\u0001\u0001";
        return Base64.getEncoder().encodeToString(authString.getBytes());
    }

    /**
     * Send fault or subscribed emails
     * <p>
     * Used by Navigator and command line processes
     */
    private void email()
    {
        fault = false;
        success = false;
        timeout = false;

        emailConnect();

        if (emailUi != null)
            emailUi.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        String msg;
        if (fault)
        {
            msg = error;
            logger.error(msg);
        }
        else if (timeout)
        {
            msg = context.cfg.gs("EmailUI.authentication.timeout");
            logger.error(msg);
        }
        else if (expired)
        {
            msg = MessageFormat.format(context.cfg.gs("EmailHandler.the.access.and.refresh.tokens.have.expired.for"), tool.getUsername());
            logger.error(msg);
            msg = context.cfg.gs("EmailHandler.the.access.and.refresh.tokens.have.expired");
            if (emailUi != null)
                JOptionPane.showMessageDialog(emailUi, msg, context.cfg.gs("Email.title"), JOptionPane.WARNING_MESSAGE);
        }
        else
        {
            if (success)
            {
                msg = context.cfg.gs(context.cfg.gs("EmailHandler.email.sent.successfully"));
                logger.info(msg);
            }
            else if (nothingToDo)
            {
                msg = context.cfg.gs("EmailHandler.email.nothing.to.do");
            }
            else
            {
                msg = context.cfg.gs(context.cfg.gs("EmailHandler.email.send.failed") + error);
                logger.error(msg);
            }
        }

        if (emailUi != null)
        {
            emailUi.labelStatus.setText(msg);
            emailUi.labelStatus.updateUI();
            emailUi.updateControls();
            emailUi.buttonTest.setText(context.cfg.gs("EmailUI.buttonTest.text"));
            emailUi.buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonTest.toolTipText"));
        }
    }

    /**
     * Connect and send email(s)
     * <p>
     * If OAuth2 checks the expiration of Access and Refresh token.<br/>
     * Connects and sends email(s).
     */
    private void emailConnect()
    {
        try
        {
            // get list of recipients
            ArrayList<Recipient> recipients = fetchRecipients();
            if (recipients.isEmpty())
            {
                nothingToDo = true;
                return; // nothing to do
            }

            // check token expirations
            if (tool.getAuthMethod().equalsIgnoreCase("oauth2"))
            {
                if (tool.isExpired(tool.getAccessExpires()))
                {
                    if (tool.isExpired(tool.getRefreshExpires()))
                    {
                        expired = true;
                        error = context.cfg.gs("EmailHandler.tokens.expired.for") + tool.getUsername();
                        return;
                    }

                    refreshAccessToken();
                    if (fault || timeout || stop)
                        return;
                }
            }
            expired = false;

            // SMTP details
            Properties props = new Properties();

            props.put("mail.smtp.host", tool.getServer());
            props.put("mail.smtp.port", tool.getPort());

            // OAuth2
            if (tool.getAuthMethod().equalsIgnoreCase("oauth2") && !tool.getProfile().equalsIgnoreCase("smtp"))
            {
                // Apple
                if (tool.getProfile().equalsIgnoreCase("apple"))
                {
                }
                // GMail
                else if (tool.getProfile().equalsIgnoreCase("gmail"))
                {
                    props.put("mail.smtp.user", tool.getUsername());
                    props.put("mail.smtp.password", tool.getAccessToken());
                    props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.sasl.enable", "true");
                    props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
                    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                    //props.put("mail.transport.protocol","smtp");
                    //props.put("mail.smtp.starttls.required", "true");
                    //props.put("mail.smtp.sasl.mechanisms.oauth2.oauthToken", tool.getAccessToken());
                    //props.put("mail.smtp.socketFactory.port", tool.getPort());
                    //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

                    props.put("mail.smtp.auth.login.disable", "true");
                    props.put("mail.smtp.auth.plain.disable", "true");
                    props.put("mail.smtp.auth.xoauth2.disable", "false");
                }
                // Outlook
                else if (tool.getProfile().equalsIgnoreCase("outlook"))
                {
                    props.put("mail.smtp.user", tool.getUsername());
                    props.put("mail.smtp.password", tool.getAccessToken());
                    props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.sasl.enable", "true");
                    props.put("mail.smtp.sasl.mechanisms", "XOAUTH2");
                    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                    //props.put("mail.transport.protocol", "smtp");
                    //props.put("mail.smtp.starttls.required", "true");
                    //props.put("mail.smtp.sasl.mechanisms.oauth2.oauthToken", tool.getAccessToken());
                    //props.put("mail.smtp.socketFactory.port", tool.getPort());
                    //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

                    props.put("mail.smtp.auth.login.disable", "true");
                    props.put("mail.smtp.auth.plain.disable", "true");
                    props.put("mail.smtp.auth.xoauth2.disable", "false");
                }
                // Zoho
                else if (tool.getProfile().equalsIgnoreCase("zoho"))
                {
                    props.put("mail.smtp.auth.mechanisms", "XOAUTH2");
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.starttls.enable", "true");

                    props.put("mail.smtp.auth.login.disable", "true");
                    props.put("mail.smtp.auth.plain.disable", "true");
                    props.put("mail.smtp.auth.xoauth2.disable", "false");
                }
            }
            else // generic SMTP techniques, not OAUTH2
            {
                // set connection security
                if (tool.getSecurity().equalsIgnoreCase("starttls"))
                {
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.auth.mechanisms", "LOGIN");
                    props.put("mail.smtp.starttls.enable", "true");
                    props.put("mail.smtp.auth.plain.disable", "true");
                }
                else if (tool.getSecurity().equalsIgnoreCase("ssl/tls"))
                {
                    props.put("mail.smtp.auth", "true");
                    props.put("mail.smtp.ssl.enable", "true");
                    props.put("mail.smtp.ssl.protocols", "TLSv1.2");
                }
                else if (tool.getSecurity().equalsIgnoreCase("none"))
                {
                    // set nothing
                }

                // set authentication method
                if (tool.getAuthMethod().equalsIgnoreCase("plain"))
                {
                    props.put("mail.smtp.auth.login.disable", "true");
                }
                else // login
                    props.put("mail.smtp.auth.plain.disable", "true");
            }

            props.put("mail.smtp.connectiontimeout", 15000);    // 15 second timeout
            if (context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace"))
            {
                props.put("mail.debug", "true"); // For debugging
                props.put("mail.debug.auth", "true"); // For debugging
            }

            Authenticator authenticator = new Authenticator()
            {
                @Override
                protected PasswordAuthentication getPasswordAuthentication()
                {
                    if (tool.getAuthMethod().equalsIgnoreCase("oauth2"))
                        return new PasswordAuthentication(tool.getUsername(), tool.getAccessToken());
                    else // Plain
                        return new PasswordAuthentication(tool.getUsername(), tool.getPassword());
                }

            };
            Session session = Session.getInstance(props, authenticator);

            if (context.cfg.getDebugLevel().trim().equalsIgnoreCase("trace"))
                session.setDebug(true);

            Transport transport = session.getTransport("smtp");

//            transport.connect();
//            transport.connect(tool.getUsername(), null);
//            transport.connect(tool.getServer(), tool.getUsername(), null);
            transport.connect(tool.getServer(), tool.getUsername(), tool.getPassword());
//            transport.connect(tool.getServer(), tool.getUsername(), tool.getAccessToken());
//            transport.connect(tool.getServer(), tool.getUsername(), tool.getAccessTokenBase64());

            for (Recipient recipient : recipients)
            {
                if (function == Function.SEND)
                {
                    if (!recipient.mismatches && !recipient.whatsNew)
                        continue;
                    if (recipient.mismatches && context.cfg.getMismatchFilename().length() > 0)
                        emailSend(session, transport, recipient, true);
                    if (recipient.whatsNew && context.cfg.getWhatsNewFilename().length() > 0)
                        emailSend(session, transport, recipient, false);
                }
                else
                    emailSend(session, transport, recipient, false);
            }

            transport.close();
        }
        catch (Exception e)
        {
            error = e.getMessage();
            logger.error(Utils.getStackTrace(e));
            fault = true;
        }

        if (fault)
        {
            String msg = context.cfg.gs("EmailUI.email.send.error") + error;
            if (emailUi != null)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("EmailUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            fault = false;
        }
        else
            success = true;

    }

    /**
     * Formulate each email based on the preferred format and type of notification
     */
    private void emailSend(Session session, Transport transport, Recipient recipient, boolean isMisMatches) throws Exception
    {
        // create a MimeMessage object
        Message message = new MimeMessage(session);
        // set From email field
        message.setFrom(new InternetAddress(tool.getUsername()));
        // set To email field
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient.address));

        //message.addHeader("Authorization: ", "Zoho-oauthtoken " + tool.getAccessToken());

        // set subject field
        String ends = context.publisherRepo.getLibraryData().libraries.description + "-" + context.subscriberRepo.getLibraryData().libraries.description + " ";
        if (function == Function.FAULT)
        {
            message.setSubject("ELS Problem: " + ends + now);
        }
        else if (function == Function.SEND)
        {
            String subject = "";
            if (isMisMatches)
                message.setSubject("ELS Mismatches: " + ends + now);
            else
                message.setSubject("ELS What's New: " + ends + now);
        }
        else if (function == Function.TEST)
            message.setSubject("ELS Test Email: " + now);

        // set content
        Multipart multipart = new MimeMultipart("related"); // alternative, related, mixed

        if (recipient.format.equalsIgnoreCase("text"))
        {
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(fetchContent(recipient, isMisMatches));
            multipart.addBodyPart(textPart);
        }
        else if (recipient.format.equalsIgnoreCase("html"))
        {
            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(fetchContent(recipient, isMisMatches), "text/html; charset=utf-8");
            multipart.addBodyPart(htmlPart);
        }

        // add any attachment
        if (attachment != null && attachment.trim().length() > 0)
        {
            // set any attachment
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(attachment));
            multipart.addBodyPart(attachmentPart);
        }

        // set the content of the email message
        message.setContent(multipart);

        // send the email message
        transport.sendMessage(message, message.getAllRecipients());

        String msg;
        if (function == Function.FAULT)
            msg = "Fault" + context.cfg.gs("EmailHandler.email.sent.to") + recipient.address;
        else
            msg = (isMisMatches ? "Mismatches" : "What's New") + context.cfg.gs("EmailHandler.email.sent.to") + recipient.address;
        logger.info(msg);
    }

    /**
     * Format the content for an email
     */
    private String fetchContent(Recipient recipient, boolean isMisMatches) throws Exception
    {
        String text = "";

        // header
        if (function != Function.SEND)
        {
            String hostname = "";
            try
            {
                hostname = " (" + InetAddress.getLocalHost().getHostName() + ")";
            }
            catch (UnknownHostException e)
            {
                // ignored
            }
            String from = context.publisherRepo.getLibraryData().libraries.description + hostname;

            // header
            if (recipient.format.equalsIgnoreCase("html"))
            {
                text = "<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">\n";
                text += "<div><img src='https://www.elsnavigator.com/assets/images/els-logo-64px.png' style=\"vertical-align: middle;\"/>\n";
                text += "<span style=\"font-family: Arial, Helvetica, sans-serif;font-size: 120%;vertical-align: middle;\">\n";
                text += "<b>&nbsp;&nbsp;Automated email from: " + from + "</b></span></div>\n";
                text += "<hr/>\n";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text = "Automated email from: " + from + "\n";
                text += "------------------------------------------\n";
            }
        }

        // content
        if (function == Function.FAULT)
        {
            attachment = context.cfg.getLogFileFullPath();

            if (recipient.format.equalsIgnoreCase("html"))
            {
                text += "<p><b>Problem</b></p>";
                text += "<span style=\"font-family: monospace; font-size: 120%;\">";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text += "Problem\n\n";
            }
            text += "An ELS process appears to have failed. See attached log.\n";
            if (recipient.format.equalsIgnoreCase("html"))
            {
                text += "</span><hr/>";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text += "------------------------------------------\n";
            }
        }
        else if (function == Function.SEND)
        {
            String filename = (isMisMatches) ? context.cfg.getMismatchFilename() : context.cfg.getWhatsNewFilename();
            if (recipient.format.equalsIgnoreCase("html"))
                filename += ".html";
            BufferedReader bufferedReader = new BufferedReader(new FileReader(filename));
            String buf;
            while ((buf = bufferedReader.readLine()) != null)
            {
                text += buf.trim() + "\n";
            }
            bufferedReader.close();
        }
        else if (function == Function.TEST)
        {
            if (recipient.format.equalsIgnoreCase("html"))
            {
                text += "<p><b>Test Email</b></p>";
                text += "<span style=\"font-family: monospace; font-size: 120%;\">";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text += "Test Email\n\n";
            }

            text += "This is a test for sending emails using ";
            text += tool.getServer();
            text += "\n";

            if (recipient.format.equalsIgnoreCase("html"))
            {
                text += "</span><hr/>";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text += "------------------------------------------\n";
            }
        }

        // footer
        if (function != Function.SEND)
        {
            if (recipient.format.equalsIgnoreCase("html"))
            {
                text += "<br/><br/>------- " + context.cfg.gs("Email.do.not.reply") + " -------<br/>";
                text += "</body></html>";
            }
            else if (recipient.format.equalsIgnoreCase("text"))
            {
                text += "\n\n";
                text += "------- " + context.cfg.gs("Email.do.not.reply") + " -------\n";
            }
        }

        return text;
    }

    /**
     * Get list of email recipients
     * <p>
     * Publisher and Subscriber recipients will always receive FAULT emails.<br/?
     * Handles skipOffline for SEND emails if Subscriber failed to connect.<br/>
     * Only the Publisher receives Test emails.<br/>
     *
     * @return ArrayList<Recipient>
     */
    private ArrayList<Recipient> fetchRecipients()
    {
        ArrayList<Recipient> recipients = new ArrayList<>();

        if (context.publisherRepo != null)
        {
            Libraries lib = context.publisherRepo.getLibraryData().libraries;
            if (lib.email != null && !lib.email.isEmpty())
            {
                recipients.add(new Recipient(lib.email, lib.format, lib.mismatches, lib.whatsNew));
            }
        }
        if (function != Function.TEST && context.subscriberRepo != null)
        {
            Libraries lib = context.subscriberRepo.getLibraryData().libraries;
            if (lib.email != null && !lib.email.isEmpty())
            {
                if ((!Utils.couldNotConnect && lib.skipOffline) ||
                        (function == Function.FAULT && Utils.couldNotConnect && !lib.skipOffline))
                    recipients.add(new Recipient(lib.email, lib.format, lib.mismatches, lib.whatsNew));
            }
        }

        // TODO: Add publisher user emails and retrieve subscriber user emails from remote (Version 5)

        return recipients;
    }

    /**
     * Generate the codeVerifier and codeChallenge for OAuth2 handling
     */
    private void generatePKCE()
    {
        // https://datatracker.ietf.org/doc/html/rfc7636

        // generate random code verifier for PKCE with random length
        int randomLength = ThreadLocalRandom.current().nextInt(43, 128);
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[randomLength];
        secureRandom.nextBytes(randomBytes);
        codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        // generate code challenge
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            logger.error(Utils.getStackTrace(e));
            error = e.getMessage();
            fault = true;
        }
    }

    public void interrupt()
    {
        logger.debug(context.cfg.gs("EmailHandler.interrupting.email.handler"));
        requestStop();
        if (workerRunning)
            stopHttpServer();
        super.interrupt();
    }

    /**
     * Is the HTTP server running in a thread?
     */
    public boolean isWorkerRunning()
    {
        return workerRunning;
    }

    /**
     * Convert the "response" String into keyword-value pairs
     */
    private Map<String, List> parseResponse()
    {
        Map<String, List> parameters = new LinkedHashMap<>();
        String[] kvp = response.split("&");
        for (String pair : kvp)
        {
            String[] kv = pair.split("=", 2);

            String key = kv[0];
            String value = kv.length > 1 ? kv[1] : "";

            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            value = URLDecoder.decode(value, StandardCharsets.UTF_8);

            parameters.computeIfAbsent(key, k -> new List()).add(value);
        }

        // Get the authorization token
        List list;
        if (!parameters.isEmpty())
        {
            switch (tool.getProfile())
            {
                case "Apple":
                    break;
                case "GMail":
                    list = parameters.get("error");
                    if (list != null)
                    {
                        error = list.getItem(0);
                        fault = true;
                        break;
                    }

                    list = parameters.get("code");
                    if (list != null)
                    {
                        String value = list.getItem(0);
                        if (value != null)
                            this.authToken = value;
                    }
                    break;
                case "Outlook":
                    // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#successful-response
                    list = parameters.get("error");
                    if (list != null)
                    {
                        error = list.getItem(0);
                        list = parameters.get("error_description");
                        if (list != null)
                            error += ", " + list.getItem(0);
                        fault = true;
                        break;
                    }

                    list = parameters.get("code");
                    if (list != null)
                    {
                        String value = list.getItem(0);
                        if (value != null)
                            this.authToken = value;
                    }
                    break;
                case "SMTP":
                    // does not use OAUTH2 access token
                    break;
                case "Zoho":
                    list = parameters.get("error");
                    if (list != null)
                    {
                        error = list.getItem(0);
                        fault = true;
                        break;
                    }

                    list = parameters.get("code");
                    if (list != null)
                    {
                        String value = list.getItem(0);
                        if (value != null)
                            this.authToken = value;
                    }

                    list = parameters.get("location");
                    if (list != null)
                    {
                        String value = list.getItem(0);
                        if (value != null)
                            this.location = value;
                    }

                    list = parameters.get("accounts-server");
                    if (list != null)
                    {
                        String value = list.getItem(0);
                        if (value != null)
                            tool.setRefreshUrl(value);
                    }
                    break;
            }
            if (!fault)
                logger.info("Authorization token retrieved");
        }

        return parameters;
    }

    /**
     * Wait for "responseCompleted == true" from the HttpHandler
     */
    private void readServerResponse(int milliTimeout)
    {
        response = "";
        responseCompleted = false;
        long startTime = System.currentTimeMillis();
        while (!responseCompleted)
        {
            try
            {
                Thread.sleep(250);
                if (stop || System.currentTimeMillis() > startTime + milliTimeout)
                {
                    if (!stop)
                    {
                        error = context.cfg.gs("EmailHandler.session.timed.out");
                        timeout = true;
                    }
                    break;
                }
            }
            catch (InterruptedException e)
            {
            }
        }
    }

    /**
     * Refresh the Access Token
     */
    private void refreshAccessToken() // NOT complete or tested
    {
        String form;
        Map<String, String> parameters = new HashMap<>();
        HttpRequest request = null;

        // generate the request URI
        switch (tool.getProfile())
        {
            case "Apple":
                break;
            case "GMail":
                parameters.put("client_id", GOOGLE_CLIENT_ID);
                parameters.put("grant_type", "refresh_token");
                parameters.put("refresh_token", tool.getRefreshToken());

                form = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

                request = HttpRequest.newBuilder()
                        .timeout(Duration.ofMillis(10000))
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                break;
            case "Outlook":
                // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#refresh-the-access-token
                break;
            case "SMTP":
                break;
            case "Zoho":
                parameters.put("client_id", ZOHO_CLIENT_ID);
                parameters.put("client_secret", "");
                parameters.put("grant_type", "refresh_token");
                parameters.put("refresh_token", tool.getRefreshToken());

                form = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

                if (tool.getRefreshUrl().isEmpty())
                    tool.setRefreshUrl("https://accounts.zoho.com");

                request = HttpRequest.newBuilder()
                        .timeout(Duration.ofMillis(10000))
                        .uri(URI.create(tool.getRefreshUrl() + "/oauth/v2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                break;
        }

        assert (request != null);
        HttpClient client = HttpClient.newHttpClient();

        try
        {
            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().toString();
            int status = response.statusCode();

            Gson gson = new Gson();
            if (status == 200)
            {
                ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
                if (errorResponse.error != null && !errorResponse.error.isEmpty())
                {
                    error = errorResponse.error + ": " + errorResponse.error_description;
                    logger.error(context.cfg.gs("EmailHandler.error.refreshing.access.token.for") + tool.getUsername() + ": " + error);
                    fault = true;
                }
                else
                {
                    AccessResponse access = gson.fromJson(body, AccessResponse.class);
                    tool.setAccessToken(access.access_token); // encrypts token
                    tool.setAccessExpires(access.expires_in); // sets future epoch - 2 minutes
                    // api_domain, token_type, etc. ignored

                    // update tool
                    tool.write();
                    logger.info(context.cfg.gs("EmailHandler.access.token.refreshed.for") + tool.getUsername());
                }
            }
            else if (status == 400)
            {
                ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
                error = errorResponse.error + ": " + errorResponse.error_description;
                logger.error(context.cfg.gs("EmailHandler.error.refreshing.access.token.for") + tool.getUsername() + ": " + error);
                fault = true;
            }
            else if (status == 404)
            {

            }
        }
        catch (HttpTimeoutException te)
        {
            error = context.cfg.gs("EmailUI.authentication.timeout");
            timeout = true;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            error = e.getMessage();
            fault = true;
        }
    }

    @Override
    public void run()
    {
        long utc = Instant.now().toEpochMilli();
        Instant instant = Instant.ofEpochMilli(utc);
        ZonedDateTime zdt = instant.atZone(ZoneId.systemDefault());
        now = zdt.format(DateTimeFormatter.ofPattern(context.preferences.getDateFormat()));

        switch(function)
        {
            case AUTH:
                authenticate();
                break;
            case FAULT:
            case SEND:
            case TEST:
                email();
                break;
        }
    }

    /**
     * Use the Authorization Token to request offline Access (and Refresh) tokens
     */
    private void requestAccessAndRefreshTokens()
    {
        String form = null;
        Map<String, String> parameters = new HashMap<>();
        HttpRequest request = null;

        // generate the request URI
        switch (tool.getProfile())
        {
            case "Apple":
                break;
            case "GMail":
                // https://cloud.google.com/iam/docs/reference/sts/rest/v1/TopLevel/token#response-body
                parameters.put("client_id", GOOGLE_CLIENT_ID);
                //parameters.put("client_secret", GOOGLE_CLIENT_SECRET);
                parameters.put("grant_type", "authorization_code");
                parameters.put("code", authToken);
                parameters.put("access_type", "offline");
                parameters.put("redirect_uri", "http://127.0.0.1:" + httpPort);
                parameters.put("code_verifier", codeVerifier);

                form = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

                request = HttpRequest.newBuilder()
                        .timeout(Duration.ofMillis(10000))
                        .uri(URI.create("https://oauth2.googleapis.com/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                break;
            case "Outlook":
                // https://learn.microsoft.com/en-us/entra/identity-platform/v2-oauth2-auth-code-flow#redeem-a-code-for-an-access-token
                parameters.put("tenant", "consumers");
                parameters.put("client_id", OUTLOOK_CLIENT_ID);
                parameters.put("code", authToken);
                parameters.put("redirect_uri", "http://127.0.0.1:" + OUTLOOK_FIXED_PORT);
                parameters.put("grant_type", "authorization_code");
                parameters.put("code_verifier", codeVerifier);
                parameters.put("access_type", "offline");

                form = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

                if (tool.getRefreshUrl().isEmpty())
                    tool.setRefreshUrl("https://login.microsoftonline.com");

                request = HttpRequest.newBuilder()
                        .timeout(Duration.ofMillis(10000))
                        .uri(URI.create(tool.getRefreshUrl() + "/consumers/oauth2/v2.0/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                break;
            case "SMTP":
                break;
            case "Zoho":
                // https://www.zoho.com/accounts/protocol/oauth/mobile-and-desktop-apps/get-access-token.html
                parameters.put("client_id", ZOHO_CLIENT_ID);
                parameters.put("client_secret", "");
                parameters.put("grant_type", "authorization_code");
                parameters.put("code", authToken);
                parameters.put("redirect_uri", "http://127.0.0.1:" + httpPort);
                parameters.put("code_verifier", codeVerifier);

                form = parameters.entrySet()
                        .stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("&"));

                if (tool.getRefreshUrl().isEmpty())
                    tool.setRefreshUrl("https://accounts.zoho.com");

                request = HttpRequest.newBuilder()
                        .timeout(Duration.ofMillis(10000))
                        .uri(URI.create(tool.getRefreshUrl() + "/oauth/v2/token"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build();
                break;
        }

        assert (request != null);
        HttpClient client = HttpClient.newHttpClient();

        try
        {
            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body().toString();
            int status = response.statusCode();

            Gson gson = new Gson();
            if (status == 200)
            {
                ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
                if (errorResponse.error != null && !errorResponse.error.isEmpty())
                {
                    error = errorResponse.error + ": " + errorResponse.error_description;
                    logger.error(context.cfg.gs("EmailHandler.error.requesting.access.token.for") + tool.getUsername() + ": " + error);
                    fault = true;
                }
                else
                {
                    AccessResponse access = gson.fromJson(body, AccessResponse.class);
                    tool.setAccessToken(access.access_token); // encrypts token
                    tool.setAccessExpires(access.expires_in); // sets future epoch - 2 minutes
                    tool.setRefreshToken(access.refresh_token); // encrypts token
                    tool.setRefreshExpires(access.refresh_token_expires_in); // sets future epoch - 2 minutes
                    api_domain = access.api_domain;
                    location = access.location; // https://www.zoho.com/accounts/protocol/oauth/multi-dc.html#client-apps
                    scope = access.scope;
                    //logger.info("Scopes: " + scope);
                    logger.info(context.cfg.gs("EmailHandler.access.token.retrieved.for") + tool.getUsername());
                }
            }
            else if (status == 400)
            {
                ErrorResponse errorResponse = gson.fromJson(body, ErrorResponse.class);
                error = errorResponse.error + ": " + errorResponse.error_description;
                logger.error(error);
                fault = true;
            }
            else if (status == 404)
            {
                error = context.cfg.gs("EmailUI.404") + request.uri();
                logger.error(error);
                fault = true;
            }
        }
        catch (HttpTimeoutException te)
        {
            error = context.cfg.gs("EmailUI.authentication.timeout");
            timeout = true;
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            error = e.getMessage();
            fault = true;
        }
    }

    /**
     * Tell the readServerResponse method to stop waiting
     */
    private void requestStop()
    {
        this.stop = true;
    }

    /**
     * Start an HTTP server for OAuth2 authentication
     */
    private void startHttpServer()
    {
        boolean badPort = true;
        while (badPort) // keep trying until we find a usable dynamic port
        {
            try
            {
                byte[] lh = {127, 0, 0, 1};
                InetAddress addr = Inet4Address.getByAddress(lh);

                // port 0 always returned 0 which cannot be used for this purpose
                int randomPort = 0;
                if (tool.getProfile().equalsIgnoreCase("outlook"))
                    randomPort = OUTLOOK_FIXED_PORT;
                else if (tool.getProfile().equalsIgnoreCase("zoho"))
                    randomPort = ZOHO_FIXED_PORT;
                else
                    randomPort = ThreadLocalRandom.current().nextInt(49152, 65535); // dynamic port range
                InetSocketAddress socketAddr = new InetSocketAddress(addr, randomPort);
                server = HttpServer.create(socketAddr, 0);
                server.createContext("/", new HttpHServer());
                server.setExecutor(null); // creates a default executor thread
                httpPort = socketAddr.getPort();
                server.start();
                badPort = false;
                workerRunning = true;
            }
            catch (BindException be)
            {
                if (tool.getProfile().equalsIgnoreCase("outlook") || tool.getProfile().equalsIgnoreCase("zoho"))
                {
                    // fixed port
                    logger.error(Utils.getStackTrace(be));
                    error = be.getMessage();
                    fault = true;
                    workerRunning = false;
                    break;
                }
                // Otherwise cannot bind to port, try a different one
            }
            catch (IOException e)
            {
                logger.error(Utils.getStackTrace(e));
                error = e.getMessage();
                fault = true;
                workerRunning = false;
                break;
            }
        }
    }

    /**
     * Stop the HTTP server
     */
    private void stopHttpServer()
    {
        if (server != null)
            server.stop(0);
        server = null;
        workerRunning = false;
    }

    /**
     * Return a success or failure page at the completion of the OAuth2 authentication process
     */
    private void writeServerResponse()
    {
        int code;
        if (httpExchange != null)
        {
            if (!fault && !timeout)
            {
                code = 200;
                response = "<html><body style=\"background-color: #1C1B22; color: white; font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">";
                response += "<div><img src='https://www.elsnavigator.com/assets/images/els-logo-72px.png' style=\"vertical-align: middle;\"/><span style=\"font-family: Arial, Helvetica, sans-serif;font-size: 120%;vertical-align: middle;\">";
                response += "<b>&nbsp;&nbsp;" + context.cfg.gs("EmailUI.els.authentication.success") + "</b></span><div>";
                response += "<hr/><p>&nbsp;&nbsp;" + context.cfg.gs("EmailUI.els.authentication.end") + "</p>";
                response += "</body></html>";
            }
            else
            {
                code = 400;
                response = "<html><body style=\"background-color: #1C1B22; color: white; font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">";
                response += "<div><img src='https://www.elsnavigator.com/assets/images/els-logo-72px.png' style=\"vertical-align: middle;\"/><span style=\"font-family: Arial, Helvetica, sans-serif;font-size: 120%;vertical-align: middle;\">";
                response += "<b>&nbsp;&nbsp;" + context.cfg.gs("EmailUI.els.authentication.failed") + "</b></span><div>";
                response += "<hr/><p>&nbsp;&nbsp;" + error + "</p>";
                response += "</body></html>";
            }

            try
            {
                httpExchange.sendResponseHeaders(code, response.length());
                OutputStream out = httpExchange.getResponseBody();
                out.write(response.getBytes());
                out.close();
            }
            catch (IOException e)
            {
                logger.error(Utils.getStackTrace(e));
                error = e.getMessage();
                fault = true;
            }
        }
        response = "";
    }

    // ================================================================================================================

    private class AccessResponse
    {
        String access_token;
        String api_domain;
        int expires_in;
        String id_token;
        String location;
        String refresh_token;
        int refresh_token_expires_in;
        String scope;
        String token_type;
    }

    // ================================================================================================================

    private class ErrorResponse
    {
        String error;
        String error_description;
        int[] error_codes;
    }

    // ================================================================================================================

    private class HttpHServer implements HttpHandler
    {
        public HttpHServer()
        {
            super();
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException
        {
            httpExchange = exchange;
            String method = exchange.getRequestMethod();
            if (method.equals("GET"))
            {
                URI uri = exchange.getRequestURI();
                response = uri.getQuery();
                logger.debug("HTTP GET");
            }
            else if (method.equals("POST"))
            {
                byte[] buffer = new byte[1024];
                int bytesRead;
                InputStream in = exchange.getRequestBody();
                StringBuilder requestData = new StringBuilder();
                while ((bytesRead = in.read(buffer)) != -1)
                {
                    requestData.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
                in.close(); // Close the input stream
                response = new String(requestData.toString().getBytes());
                logger.debug("HTTP POST");
            }
            responseCompleted = true;
        }
    }

    // ================================================================================================================

    private class Recipient
    {
        public String address;
        public String format;
        public boolean mismatches = false;
        public boolean whatsNew = false;

        public Recipient(String address, String format, boolean mismatches, boolean whatsNew)
        {
            this.address = address;
            this.format = format;
            this.mismatches = mismatches;
            this.whatsNew = whatsNew;
        }
    }

}
