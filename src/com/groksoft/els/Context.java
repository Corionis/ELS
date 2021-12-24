package com.groksoft.els;

import com.groksoft.els.gui.Navigator;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hintServer.Datastore;

/**
 * Context class to make passing these data easier.
 */
public class Context
{
    // some of these will be null at runtime depending on configuration
    public ClientSftp clientSftp;
    public ClientStty clientStty;
    public Datastore datastore;
    public boolean fault = false;
    public HintKeys hintKeys;
    public boolean hintMode = false;
    public Main main;
    public Navigator navigator;
    public Repository publisherRepo;
    public ServeSftp serveSftp;
    public ServeStty serveStty;
    public Repository statusRepo;
    public ClientStty statusStty;
    public Repository subscriberRepo;
    public Transfer transfer;
}
