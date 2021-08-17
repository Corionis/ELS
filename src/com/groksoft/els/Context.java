package com.groksoft.els;

import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.sftp.ServeSftp;
import com.groksoft.els.stty.ClientStty;
import com.groksoft.els.stty.ServeStty;

/**
 * Class to make passing these data easier
 */
public class Context
{
    public ClientSftp clientSftp;
    public ClientStty clientStty;
    public Repository publisherRepo;
    public ServeSftp serveSftp;
    public ServeStty serveStty;
    public Repository subscriberRepo;
    public Transfer transfer;
    public boolean hintMode = false;
    public int type;
}
