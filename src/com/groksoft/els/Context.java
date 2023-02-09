package com.groksoft.els;

import com.groksoft.els.gui.*;
import com.groksoft.els.gui.browser.Browser;
import com.groksoft.els.gui.operations.OperationsUI;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hintServer.Datastore;

/**
 * Runtime Context
 * <br/>
 * Public data members of ELS configuration, components, protocols and data
 */
public class Context
{
    // which members are non-null depends on the runtime or task configuration

    // ELS core
    public HintKeys authKeys = null;
    public ClientSftp clientSftp = null;
    public ClientStty clientStty = null;
    public Configuration cfg = null;
    public Datastore datastore = null;
    public boolean fault = false; // process fault indicator
    public Hints hints = null;
    public HintKeys hintKeys = null;
    public boolean localMode = false; // operation intended for local execution
    public Main main = null;
    public Navigator navigator = null;
    public boolean nestedProcesses = false; // nested processes are running
    public Repository publisherRepo = null;
    public ServeSftp serveSftp = null;
    public ServeStty serveStty = null;
    public Repository statusRepo = null;
    public ClientStty statusStty = null;
    public Repository subscriberRepo = null;
    public boolean timeout = false; // time-out indicator
    public boolean trace = false; // trace logging
    public Transfer transfer = null;

    // Navigator
    public Browser browser = null;
    public MainFrame mainFrame = null;
    public OperationsUI operationsUI = null;
    public Preferences preferences = null;
    public Progress progress = null;
    public SavedEnvironment savedEnvironment;
}
