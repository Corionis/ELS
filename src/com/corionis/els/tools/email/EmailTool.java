package com.corionis.els.tools.email;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.time.Instant;

/**
 * Email Tool class
 * <p>
 * Unlike other ELS Tools this is a data class with no real functionality.
 * <p>
 * See com.corionis.els.email.Email for Email functionality.
 */
public class EmailTool extends AbstractTool
{
    // @formatter:off
    public static final String INTERNAL_NAME = "Email";
    public static final String SUBSYSTEM = "local";
    //
    private String configName; // user-specified name for this instance
    private String internalName = INTERNAL_NAME;
    private String profile = "Apple";
    private String server = "";
    private String username = "";
    private byte[] password = {};
    private String port = "";
    private String security = "STARTTLS";
    private String authMethod = "Password";
    private long accessExpires = 0L;
    private long refreshExpires = 0L;
    private String refreshUrl = "";
    private byte[] accessToken = {};
    private byte[] refreshToken = {};
    // @formatter:on

    /**
     * Constructor
     *
     * @param context
     */
    public EmailTool(Context context)
    {
        super(context);
        this.context = context;
        setDisplayName(getCfg().gs("Email.displayName"));
        this.setDataHasChanged(false);
    }

    @Override
    public EmailTool clone()
    {
        EmailTool clone = new EmailTool(context);
        clone.configName = this.configName;
        clone.internalName = this.internalName;
        clone.profile = this.profile;
        clone.server = this.server;
        clone.username = this.username;
        clone.password = this.password;
        clone.port = this.port;
        clone.security = this.security;
        clone.authMethod = this.authMethod;
        clone.accessExpires = this.accessExpires;
        clone.refreshExpires = this.refreshExpires;
        clone.accessToken = new byte[this.accessToken.length];
        System.arraycopy(this.accessToken, 0, clone.accessToken, 0, this.accessToken.length);
        clone.refreshToken = new byte[this.refreshToken.length];
        System.arraycopy(this.refreshToken, 0, clone.refreshToken, 0, this.refreshToken.length);
        return clone;
    }

    /**
     * Get the access token expiration long value
     */
    public long getAccessExpires()
    {
        return accessExpires;
    }

    /**
     * Return decrypted access token
     */
    public String getAccessToken()
    {
        return context.main.decrypt(context.main.systemKey, accessToken);
    }

    public String getAuthMethod()
    {
        return authMethod;
    }

    @Override
    public String getConfigName()
    {
        return configName;
    }

    @Override
    public String getDisplayName()
    {
        return displayName;
    }

    public String getServer()
    {
        return server;
    }

    @Override
    public String getInternalName()
    {
        return this.internalName;
    }

    /**
     * Return decrypted password
     */
    public String getPassword()
    {
        if (password.length == 0)
            return "";
        else
            return context.main.decrypt(context.main.systemKey, password);
    }

    public String getPort()
    {
        return port;
    }

    public String getProfile()
    {
        return profile;
    }

    public long getRefreshExpires()
    {
        return refreshExpires;
    }

    /**
     * Return decrypted refresh token
     */
    public String getRefreshToken()
    {
        return context.main.decrypt(context.main.systemKey, refreshToken);
    }

    public String getRefreshUrl()
    {
        return refreshUrl;
    }

    public String getSecurity()
    {
        return security;
    }

    @Override
    public String getSubsystem()
    {
        return SUBSYSTEM;
    }

    public String getUsername()
    {
        return username;
    }

    @Override
    public boolean isDataChanged()
    {
        return dataHasChanged;
    }

    @Override
    public void processTool(Task task) throws Exception
    {
        context.fault = false;
    }

    /**
     * Is the expiration value expired as of "now"?
     */
    public boolean isExpired(long value)
    {
        long utc = Instant.now().toEpochMilli();
        if (utc > value)
            return true;
        return false;
    }

    /**
     * Set the access token expiration
     * <p>
     * Takes the "expires in" value and computes the value from "now"
     */
    public void setAccessExpires(long accessExpires)
    {
        long utc = Instant.now().toEpochMilli();
        accessExpires = (accessExpires - 120); // reduce by 2 minutes
        if (accessExpires > 0)
            accessExpires = utc + (accessExpires * 1000);
        else
            accessExpires = utc;
        this.accessExpires = accessExpires;
    }

    /**
     * Encrypt and set the access token
     */
    public void setAccessToken(String accessToken)
    {
        if (!accessToken.isEmpty())
            this.accessToken = context.main.encrypt(context.main.systemKey, accessToken);
        else
            this.accessToken = new byte[0];
    }

    public void setAuthMethod(String authMethod)
    {
        this.authMethod = authMethod;
    }

    @Override
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    public void setServer(String server)
    {
        this.server = server;
    }

    /**
     * Encrypt and set the user password
     */
    public void setPassword(String password)
    {
        if (!password.isEmpty())
            this.password = context.main.encrypt(context.main.systemKey, password);
        else
            this.password = new byte[0];
    }

    public void setPort(String port)
    {
        this.port = port;
    }

    public void setProfile(String profile)
    {
        this.profile = profile;
    }

    /**
     * Set the refresh token expiration
     * <p>
     * Takes the "expires in" value and computes the value from "now"
     */
    public void setRefreshExpires(long refreshExpires)
    {
        long utc = Instant.now().toEpochMilli();
        refreshExpires = (refreshExpires - 120); // reduce by 2 minutes
        if (refreshExpires > 0)
            refreshExpires = utc + (refreshExpires * 1000);
        else
            refreshExpires = utc;
        this.refreshExpires = refreshExpires;
    }

    /**
     * Encrypt and set the refresh token
     */
    public void setRefreshToken(String refreshToken)
    {
        if (!refreshToken.isEmpty())
            this.refreshToken = context.main.encrypt(context.main.systemKey, refreshToken);
        else
            this.refreshToken = new byte[0];
    }

    public void setRefreshUrl(String refreshUrl)
    {
        this.refreshUrl = refreshUrl;
    }

    public void setSecurity(String security)
    {
        this.security = security;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Write the Email Server JSON file
     */
    public void write() throws Exception
    {
        if (!getAuthMethod().equalsIgnoreCase("oauth2"))
        {
            setAccessToken("");
            setAccessExpires(-1L);
            setRefreshToken("");
            setRefreshExpires(-1L);
            setRefreshUrl("");
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(getCfg().gs("Z.error.writing") + getFullPath() + ": " + Utils.getStackTrace(fnf));
        }
    }

}
