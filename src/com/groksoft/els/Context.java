package com.groksoft.els;

import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;
import com.groksoft.els.stty.hintServer.Datastore;

/**
 * Class to make passing these data easier
 */
public class Context
{
    public ClientSftp clientSftp;
    public ClientStty clientStty;
    public HintKeys hintKeys;
    public Datastore datastore;
    public Repository publisherRepo;
    public ServeSftp serveSftp;
    public ServeStty serveStty;
    public Repository statusRepo;
    public ClientStty statusStty;
    public Repository subscriberRepo;
    public Transfer transfer;
    public boolean hintMode = false;
}
