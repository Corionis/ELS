package com.corionis.els;

import com.corionis.els.gui.*;
import com.corionis.els.gui.browser.Browser;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.hints.Hints;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.sftp.ServeSftp;
import com.corionis.els.stty.ClientStty;
import com.corionis.els.stty.ServeStty;
import com.corionis.els.stty.hintServer.Datastore;
import com.corionis.els.gui.libraries.LibrariesUI;
import com.corionis.els.tools.Tools;

/**
 * Runtime Context
 * <br/>
 * Public data members of ELS configuration, components, protocols and data
 */
public class Context
{
    // which members are non-null depends on the runtime or task configuration

    // Core
    public HintKeys authKeys = null;
    public ClientSftp clientSftp = null; // file transfers
    public ClientSftp clientSftpMetadata = null; // Navigator queries
    public ClientStty clientStty = null; // commands and JSON files
    public Configuration cfg = null;
    public Datastore datastore = null; // Hints datastore
    public boolean fault = false; // process fault indicator
    public Hints hints = null;
    public HintKeys hintKeys = null;
    public Repository hintsRepo = null;
    public ClientStty hintsStty = null;
    public boolean localMode = false; // operation intended for local execution
    public Main main = null;
    public Navigator navigator = null;
    public Repository publisherRepo = null;
    public ServeSftp serveSftp = null;
    public ServeStty serveStty = null;
    public Repository subscriberRepo = null;
    public boolean timeout = false; // time-out indicator
    public Tools tools = null;
    public boolean trace = false; // trace logging
    public Transfer transfer = null;

    // Navigator
    public Browser browser = null;
    public Environment environment;
    public LibrariesUI libraries;
    public MainFrame mainFrame = null;
    public Preferences preferences = null;
    public Progress progress = null;

    /**
     * Clone Context
     *
     * @return Context Object
     */
    @Override
    public Object clone()
    {
        Context clone = new Context();
        // ELS core
        clone.authKeys = this.authKeys;
        clone.clientSftp = this.clientSftp;
        clone.clientSftpMetadata = this.clientSftpMetadata;
        clone.clientStty = this.clientStty;
        clone.cfg = (Configuration) this.cfg.clone();
        clone.datastore = this.datastore;
        clone.fault = this.fault;
        clone.hints = this.hints;
        clone.hintKeys = this.hintKeys;
        clone.hintsRepo = this.hintsRepo;
        clone.hintsStty = this.hintsStty;
        clone.localMode = this.localMode;
        clone.main = this.main;
        clone.navigator = this.navigator;
        clone.publisherRepo = this.publisherRepo;
        clone.serveSftp = this.serveSftp;
        clone.serveStty = this.serveStty;
        clone.subscriberRepo = this.subscriberRepo;
        clone.timeout = this.timeout;
        if (this.tools != null)
            clone.tools = (Tools) this.tools.clone();
        clone.trace = this.trace;
        clone.transfer = this.transfer;

        // Navigator
        clone.browser = this.browser;
        clone.environment = this.environment;
        clone.libraries = this.libraries;
        clone.mainFrame = this.mainFrame;
        clone.preferences = this.preferences;
        clone.progress = this.progress;
        return clone;
    }

}
