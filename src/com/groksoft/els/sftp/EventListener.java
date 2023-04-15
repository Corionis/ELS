package com.groksoft.els.sftp;

import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.FileHandle;
import org.apache.sshd.sftp.server.Handle;
import org.apache.sshd.sftp.server.SftpEventListener;

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;

/**
 * Apache Mina sftp event listener
 */

public class EventListener implements SftpEventListener
{
    private transient Logger logger = LogManager.getLogger("applog");

    @Override
    public void blocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask, Throwable thrown) throws IOException
    {
        logger.debug("Sftp: blocked " + localHandle.getFile().toString());
    }

    @Override
    public void blocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, int mask) throws IOException
    {
        //logger.trace("Sftp: blocking");
    }

    @Override
    public void closed(ServerSession session, String remoteHandle, Handle localHandle, Throwable thrown) throws IOException
    {
        logger.trace("Sftp: closed " + localHandle.getFile().toString());
    }

    @Override
    public void closing(ServerSession session, String remoteHandle, Handle localHandle) throws IOException
    {
        //logger.debug("Sftp: closing");
    }

    @Override
    public void created(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException
    {
        logger.debug("Sftp: created " + path);
    }

    @Override
    public void creating(ServerSession session, Path path, Map<String, ?> attrs) throws IOException
    {
        //logger.debug("Sftp: creating");
    }

    @Override
    public void destroying(ServerSession session) throws IOException
    {
        //logger.trace("Sftp: destroying");
    }

    @Override
    public void exiting(ServerSession session, Handle handle) throws IOException
    {
        logger.debug("Sftp: exiting");
    }

    @Override
    public void initialized(ServerSession session, int version) throws IOException
    {
        //logger.trace("Sftp: initialized");
    }

    @Override
    public void linked(ServerSession session, Path source, Path target, boolean symLink, Throwable thrown) throws IOException
    {
        logger.debug("Sftp: linked " + source.toString() + " -> " + target.toString());
    }

    @Override
    public void linking(ServerSession session, Path source, Path target, boolean symLink) throws IOException
    {
        //logger.debug("Sftp: linking");
    }

    @Override
    public void modifiedAttributes(ServerSession session, Path path, Map<String, ?> attrs, Throwable thrown) throws IOException
    {
        logger.debug("Sftp: modifiedAttributes " + path.toString());
        if (thrown != null)
            logger.error(Utils.getStackTrace(thrown));
    }

    @Override
    public void modifyingAttributes(ServerSession session, Path path, Map<String, ?> attrs) throws IOException
    {
        //logger.debug("Sftp: modifyingAttributes");
    }

    @Override
    public void moved(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts, Throwable thrown) throws IOException
    {
        logger.debug("Sftp: moved " + srcPath + " -> " + dstPath);
        if (thrown != null)
            logger.error(Utils.getStackTrace(thrown));
    }

    @Override
    public void moving(ServerSession session, Path srcPath, Path dstPath, Collection<CopyOption> opts) throws IOException
    {
        //logger.debug("Sftp: moving");
    }

    @Override
    public void open(ServerSession session, String remoteHandle, Handle localHandle) throws IOException
    {
        logger.debug("Sftp: open " + localHandle.getFile().toString());
    }

    @Override
    public void openFailed(ServerSession session, String remotePath, Path localPath, boolean isDirectory, Throwable thrown) throws IOException
    {
        logger.error("Sftp: openFailed "  + localPath.toString());
    }

    @Override
    public void opening(ServerSession session, String remoteHandle, Handle localHandle) throws IOException
    {
        //logger.debug("Sftp: opening");
    }

    @Override
    public void read(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, int readLen, Throwable thrown) throws IOException
    {
        logger.trace("Sftp: read " + localHandle.getFile().toString());
        if (thrown != null)
            logger.error(Utils.getStackTrace(thrown));
    }

    @Override
    public void reading(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) throws IOException
    {
        //logger.debug("Sftp: reading");
    }

    @Override
    public void removed(ServerSession session, Path path, boolean isDirectory, Throwable thrown) throws IOException
    {
        if (thrown != null)
        {
            if (!(thrown instanceof java.nio.file.NoSuchFileException)) // ignore file not found exceptions
                logger.error(Utils.getStackTrace(thrown));
        } else
            logger.debug("Sftp: removed " + (isDirectory ? "directory " : "file ") + path);
    }

    @Override
    public void removing(ServerSession session, Path path, boolean isDirectory) throws IOException
    {
        //logger.debug("Sftp: removing");
    }

    @Override
    public void unblocked(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length, Throwable thrown) throws IOException
    {
        logger.trace("Sftp: unblocked");
    }

    @Override
    public void unblocking(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, long length) throws IOException
    {
        //logger.debug("Sftp: unblocking");
    }

    @Override
    public void writing(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen) throws IOException
    {
        //logger.trace("Sftp: writing " + localHandle.getFile().toString());
    }

    @Override
    public void written(ServerSession session, String remoteHandle, FileHandle localHandle, long offset, byte[] data, int dataOffset, int dataLen, Throwable thrown) throws IOException
    {
        //logger.debug("Sftp: written " + localHandle.getFile().getFileName());
    }

}
