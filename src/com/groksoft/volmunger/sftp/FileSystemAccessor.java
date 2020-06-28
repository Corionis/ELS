package com.groksoft.volmunger.sftp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.subsystem.sftp.DirectoryHandle;
import org.apache.sshd.server.subsystem.sftp.FileHandle;
import org.apache.sshd.server.subsystem.sftp.SftpFileSystemAccessor;
import org.apache.sshd.server.subsystem.sftp.SftpSubsystemProxy;

import java.io.IOException;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FileSystemAccessor implements SftpFileSystemAccessor
{
    private transient Logger logger = LogManager.getLogger("applog");

    @Override
    public void closeDirectory(ServerSession session, SftpSubsystemProxy subsystem, DirectoryHandle dirHandle, Path dir, String handle, DirectoryStream<Path> ds) throws IOException
    {
        logger.info("closeDirectory");
    }

    @Override
    public void closeFile(ServerSession session, SftpSubsystemProxy subsystem, FileHandle fileHandle, Path file, String handle, Channel channel, Set<? extends OpenOption> options) throws IOException
    {
        logger.info("closeFile");
    }

    @Override
    public void copyFile(ServerSession session, SftpSubsystemProxy subsystem, Path src, Path dst, Collection<CopyOption> opts) throws IOException
    {
        logger.info("copyFile");
    }

    @Override
    public void createDirectory(ServerSession session, SftpSubsystemProxy subsystem, Path path) throws IOException
    {
        logger.info("createDirectory");
    }

    @Override
    public void createLink(ServerSession session, SftpSubsystemProxy subsystem, Path link, Path existing, boolean symLink) throws IOException
    {
        logger.info("createLink");
    }

    @Override
    public DirectoryStream<Path> openDirectory(ServerSession session, SftpSubsystemProxy subsystem, DirectoryHandle dirHandle, Path dir, String handle) throws IOException
    {
        logger.info("openDirectory");
        return null;
    }

    @Override
    public SeekableByteChannel openFile(ServerSession session, SftpSubsystemProxy subsystem, FileHandle fileHandle, Path file, String handle, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException
    {
        logger.info("openFile");
        return null;
    }

    @Override
    public Map<String, ?> readFileAttributes(ServerSession session, SftpSubsystemProxy subsystem, Path file, String view, LinkOption... options) throws IOException
    {
        logger.info("readFileAttributes");
        return null;
    }

    @Override
    public void removeFile(ServerSession session, SftpSubsystemProxy subsystem, Path path, boolean isDirectory) throws IOException
    {
        logger.info("removeFile");
    }

    @Override
    public void renameFile(ServerSession session, SftpSubsystemProxy subsystem, Path oldPath, Path newPath, Collection<CopyOption> opts) throws IOException
    {
        logger.info("renameFile");
    }

    @Override
    public UserPrincipal resolveFileOwner(ServerSession session, SftpSubsystemProxy subsystem, Path file, UserPrincipal name) throws IOException
    {
        logger.info("resolveFileOwner");
        return null;
    }

    @Override
    public GroupPrincipal resolveGroupOwner(ServerSession session, SftpSubsystemProxy subsystem, Path file, GroupPrincipal name) throws IOException
    {
        logger.info("resolveGroupOwner");
        return null;
    }

    @Override
    public String resolveLinkTarget(ServerSession session, SftpSubsystemProxy subsystem, Path link) throws IOException
    {
        logger.info("resolveLinkTarget");
        return null;
    }

    @Override
    public Path resolveLocalFilePath(ServerSession session, SftpSubsystemProxy subsystem, Path rootDir, String remotePath) throws IOException, InvalidPathException
    {
        logger.info("resolveLocalFilePath");
        return null;
    }

    @Override
    public void setFileAccessControl(ServerSession session, SftpSubsystemProxy subsystem, Path file, List<AclEntry> acl, LinkOption... options) throws IOException
    {
        logger.info("setFileAccessControl");
    }

    @Override
    public void setFileAttribute(ServerSession session, SftpSubsystemProxy subsystem, Path file, String view, String attribute, Object value, LinkOption... options) throws IOException
    {
        logger.info("setFileAttribute");
    }

    @Override
    public void setFileOwner(ServerSession session, SftpSubsystemProxy subsystem, Path file, Principal value, LinkOption... options) throws IOException
    {
        logger.info("setFileOwner");
    }

    @Override
    public void setFilePermissions(ServerSession session, SftpSubsystemProxy subsystem, Path file, Set<PosixFilePermission> perms, LinkOption... options) throws IOException
    {
        logger.info("setFilePermissions");
    }

    @Override
    public void setGroupOwner(ServerSession session, SftpSubsystemProxy subsystem, Path file, Principal value, LinkOption... options) throws IOException
    {
        logger.info("setGroupOwner");
    }

    @Override
    public void syncFileData(ServerSession session, SftpSubsystemProxy subsystem, FileHandle fileHandle, Path file, String handle, Channel channel) throws IOException
    {
        logger.info("syncFileData");
    }

    @Override
    public FileLock tryLock(ServerSession session, SftpSubsystemProxy subsystem, FileHandle fileHandle, Path file, String handle, Channel channel, long position, long size, boolean shared) throws IOException
    {
        logger.info("tryLock");
        return null;
    }

}
