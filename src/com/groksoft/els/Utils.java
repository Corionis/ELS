package com.groksoft.els;

import com.groksoft.els.repository.Libraries;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils class of static utility methods.
 */
public class Utils
{
    private static Cipher cipher = null;
    private static Logger logger = LogManager.getLogger("applog");

    /**
     * Static methods - do not instantiate
     */
    private Utils()
    {
        // do not instantiate
    }

    /**
     * Available space on local target
     *
     * @param location the path to the target
     * @return the long space available on target in bytes
     */
    public static long availableSpace(String location)
    {
        long space = 0;
        try
        {
            File f = new File(location);
            space = f.getUsableSpace();
        }
        catch (SecurityException e)
        {
            logger.error("Exception '" + e.getMessage() + "' getting usable space from " + location);
        }
        return space;
    }

    /**
     * Decrypt a byte array to a string using provided key
     *
     * @param key       UUID key
     * @param encrypted Data to decrypt
     * @return String Decrypted texts
     */
    public static String decrypt(String key, byte[] encrypted)
    {
        String output = "";
        try
        {
            // Create key and cipher
            key = key.replaceAll("-", "");
            if (key.length() > 16)
            {
                key = key.substring(0, 16);
            }
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            if (cipher == null)
            {
                cipher = Cipher.getInstance("AES");
            }
            // decrypt the text
            cipher.init(Cipher.DECRYPT_MODE, aesKey);
            output = new String(cipher.doFinal(encrypted));
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return output;
    }

    /**
     * Encrypt a string using provided key
     *
     * @param key  UUID key
     * @param text Data to encrypt
     * @return byte[] of encrypted data
     */
    public static byte[] encrypt(String key, String text)
    {
        byte[] encrypted = {};
        try
        {
            // Create key and cipher
            key = key.replaceAll("-", "");
            if (key.length() > 16)
            {
                key = key.substring(0, 16);
            }
            Key aesKey = new SecretKeySpec(key.getBytes(), "AES");
            if (cipher == null)
            {
                cipher = Cipher.getInstance("AES");
            }
            // encrypt the text
            cipher.init(Cipher.ENCRYPT_MODE, aesKey);
            encrypted = cipher.doFinal(text.getBytes());
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return encrypted;
    }

    /**
     * Format remote & local IP addresses and ports
     *
     * @param socket
     * @return String of formatting information
     */
    public static String formatAddresses(Socket socket)
    {
        return socket.getInetAddress().toString() + ":" + socket.getPort() +
                ", local " + socket.getLocalAddress().toString() + ":" + socket.getLocalPort();
    }

    /**
     * Format a long number with byte, MB, GB and TB as applicable
     *
     * @param value Long value to format
     * @return String Formatting text
     */
    public static String formatLong(long value, boolean isFull)
    {
        String full;
        String brief;
        DecimalFormat longForm = new DecimalFormat("###,###,###,###,###,###,###,###.###");
        DecimalFormat shortForm = new DecimalFormat("###,###,###,###,###,###,###,###.#");

        brief = shortForm.format(value) + " B";
        full = brief;
        if (value >= (1024))
        {
            brief = shortForm.format(value / 1024.0) + " KB";
            full += ", " + brief;
        }
        if (value >= (1024.0 * 1024.0))
        {
            brief = shortForm.format(value / (1024.0 * 1024.0)) + " MB";
            full += ", " + brief;
        }
        if (value >= (1024.0 * 1024.0 * 1024.0))
        {
            brief = shortForm.format(value / (1024.0 * 1024.0 * 1024.0)) + " GB";
            full += ", " + brief;
        }
        if (value >= (1024.0 * 1024.0 * 1024.0 * 1024.0))
        {
            brief = longForm.format(value / (1024.0 * 1024.0 * 1024.0 * 1024.0)) + " TB";
            full += ", " + brief;
        }
        return (isFull ? full : brief);
    }

    /**
     * Get the duration string
     *
     * @param millis
     * @return String
     */
    public static String getDuration(long millis)
    {
        if (millis < 0)
        {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days > 0)
        {
            sb.append(days);
            sb.append(" days ");
        }
        if (hours > 0)
        {
            sb.append(hours);
            sb.append(" hrs ");
        }
        if (minutes > 0)
        {
            sb.append(minutes);
            sb.append(" mins ");
        }
        sb.append(seconds);
        sb.append(" secs");

        return (sb.toString());
    }

    /**
     * Get the file separator for the flavor of operating system
     *
     * @param flavor
     * @return String containing matching file separator character
     * @throws MungeException
     */
    public static String getFileSeparator(String flavor) throws MungeException
    {
        String separator;
        if (flavor.equalsIgnoreCase(Libraries.WINDOWS))
        {
            separator = "\\\\";
        }
        else if (flavor.equalsIgnoreCase(Libraries.LINUX))
        {
            separator = "/";
        }
        else if (flavor.equalsIgnoreCase(Libraries.MAC))
        {
            separator = ":";
        }
        else
        {
            throw new MungeException("unknown flavor '" + flavor + "'");
        }
        return separator;
    }

    /**
     * Get the local system hostname
     *
     * @return Hostname or empty
     */
    public static String getHostname()
    {
        String hostname = "";
        try
        {
            InetAddress addr;
            addr = InetAddress.getLocalHost();
            hostname = addr.getHostName();
            int i = hostname.indexOf(".");
            if (i > 0)
                hostname = hostname.substring(0, i);
            hostname = hostname.toLowerCase();
            hostname = hostname.substring(0, 1).toUpperCase() + hostname.substring(1);
        }
        catch (UnknownHostException ex)
        {
            hostname = "";
        }
        return hostname;
    }

    /**
     * Gets last path that includes a filename
     *
     * @param full the full
     * @return the last path
     */
    public static String getLastPath(String full, String sep) throws MungeException
    {
        String path = "";
        int p = full.indexOf(sep);
        if (p >= 0)
        {
            path = full.substring(0, p);
        }
        else
        {
            path = full;
        }
        return path;
    }

    /**
     * Get the path to the left of the filename
     *
     * @param full Full path to parse
     * @param sep  The directory separator for the local O/S
     * @return String of left path
     */
    public static String getLeftPath(String full, String sep)
    {
        String path = "";
        int p = full.lastIndexOf(sep);
        if (p >= 0)
        {
            path = full.substring(0, p);
        }
        else
        {
            path = full;
        }
        return path;
    }

    /**
     * O/S independent way of setting file permissions
     *
     * @param thing String of local file to check
     * @return int permissions
     */
    public static int getLocalPermissions(String thing)
    {
        int perms = 000;
        Path p = Paths.get(thing);

        //if (Files.notExists(p))

        //if (Files.isReadable(p))
        //    perms = 444;

        if (Files.isWritable(p))
            perms = 644;

        if (Files.isExecutable(p))
            perms = 755;

        return perms;
    }

    public static String getOS()
    {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("windows"))
            os = "Windows";
        else if (os.toLowerCase().startsWith("mac"))
            os = "Mac";
        else
            os = "Linux";
        return os;
    }

    /**
     * Parse the port number from a site returned as integer
     *
     * @param site Site string, e.g. hostname:port
     * @return int Port number
     */
    public static int getPort(String site)
    {
        int port = 0;
        String sport = parsePort(site);
        if (sport == null || sport.length() < 1)
        {
            sport = "50271";                    // SPOT Daemon Default Daemon port
            logger.info(site + " port not defined, using default: " + sport);
        }
        port = Integer.valueOf(sport);
        if (port < 1)
        {
            logger.info(site + " is disabled, port < 1");
        }
        return port;
    }

    /**
     * Gets scaled value
     *
     * @param size the string to parse
     * @return the scaled value
     */
    public static long getScaledValue(String size)
    {
        long returnValue = -1;
        Pattern patt = Pattern.compile("([\\d.]+)([TGMK]B)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = patt.matcher(size);
        Map<String, Integer> powerMap = new HashMap<String, Integer>();
        powerMap.put("TB", 4);
        powerMap.put("GB", 3);
        powerMap.put("MB", 2);
        powerMap.put("KB", 1);
        if (matcher.find())
        {
            String number = matcher.group(1);
            int pow = powerMap.get(matcher.group(2).toUpperCase());
            BigDecimal bytes = new BigDecimal(number);
            bytes = bytes.multiply(BigDecimal.valueOf(1024).pow(pow));
            returnValue = bytes.longValue();
        }
        return returnValue;
    }

    /**
     * Get the short last path element, directory or file
     *
     * @param full Full path to parse
     * @param sep  The directory separator for the local O/S
     * @return String of path
     */
    public static String getShortPath(String full, String sep)
    {
        String path = "";
        int p = full.lastIndexOf(sep);
        if (p >= 0)
        {
            if (full.length() > (p + 1))
                path = full.substring(p + 1);
            else
                path = full.substring(0, p);
        }
        else
        {
            path = full;
        }
        return path;
    }

    /**
     * Gets stack trace
     *
     * @param throwable the throwable
     * @return the stack trace
     */
    public static String getStackTrace(final Throwable throwable)
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    /**
     * Is the path just a filename with no directory to the left?
     *
     * @param path Path to check
     * @return true if it is just a filename
     */
    public static boolean isFileOnly(String path)
    {
        if (!path.contains("/") &&
                !path.contains("\\") &&
                !path.contains("|"))
            return true;
        return false;
    }

    /**
     * Parse the host from a site string
     * <p>
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location Site string
     * @return String host
     */
    public static String parseHost(String location)
    {
        String host = null;
        String[] a = location.split(":");
        if (a.length >= 1)
        {
            host = a[0];
        }
        if (host.length() < 1)
            host = "localhost";
        return host;
    }

    /**
     * Parse the port from a site string
     * <p>
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location
     * @return port
     */
    public static String parsePort(String location)
    {
        String port = "";
        String[] a = location.split(":");
        if (a.length == 2)
        {
            port = a[1];
        }
        return port;
    }

    /**
     * Replace source path separators with pipe character for comparison
     *
     * @param repo Repository of source of path
     * @param path Path to modify with pipe characters
     * @return String Modified path
     * @throws MungeException
     */
    public static String pipe(Repository repo, String path) throws MungeException
    {
        String p = path.replaceAll(repo.getWriteSeparator(), "|");
        return p;
    }

    /**
     * Read an encrypted data stream, return decrypted string
     *
     * @param in  DataInputStream to read, e.g. remote connection
     * @param key UUID key to decrypt the data stream
     * @return String read from stream; null if connection is closed
     */
    public static String readStream(DataInputStream in, String key) throws Exception
    {
        byte[] buf = {};
        String input = "";
        while (true)
        {
            try
            {
                int count = in.readInt();
                int pos = 0;
                if (count > 0)
                {
                    buf = new byte[count];
                    int remaining = count;
                    while (true)
                    {
                        int readCount = in.read(buf, pos, remaining);
                        if (readCount < 0)
                            break;
                        pos += readCount;
                        remaining -= readCount;
                        if (pos == count)
                            break;
                    }
                    if (pos != count)
                    {
                        logger.warn("Read counts do not match, expected " + count + ", received " + pos);
                    }
                }
                break;
            }
            catch (SocketTimeoutException e)
            {
                continue;
            }
            catch (EOFException e)
            {
                input = null;
                break;
            }
            catch (IOException e)
            {
                if (e.getMessage().toLowerCase().contains("connection reset"))
                {
                    logger.info("Connection closed by client");
                    input = null;
                }
                throw e;
            }
        }
        if (buf.length > 0)
            input = decrypt(key, buf);
        return input;
    }

    /**
     * Remove a directory tree and contents
     *
     * @param directory The directory tree to be deleted
     * @return true if not all directories, files were also deleted
     */
    public static boolean removeDirectoryTree(File directory)
    {
        boolean notAllDirectories = false;
        File[] all = directory.listFiles();
        for (File entry : all)
        {
            if (!entry.isDirectory())
            {
                notAllDirectories = true;
                entry.delete();
            }
            else
            {
                removeDirectoryTree(entry);
            }
        }
        directory.delete();
        return notAllDirectories;
    }

    /**
     * Replace source pipe character with path separators
     *
     * @param repo Repository of source of path
     * @param path Path to modify with pipe characters
     * @return String Modified path
     * @throws MungeException
     */
    public static String unpipe(Repository repo, String path) throws MungeException
    {
        String p = path.replaceAll("\\|", repo.getWriteSeparator());
        return p;
    }

    /**
     * Write an encrypted string to output stream
     *
     * @param out     DataOutputStream to write
     * @param key     UUID key to encrypt the string
     * @param message String to encrypted and write
     */
    public static void writeStream(DataOutputStream out, String key, String message) throws Exception
    {
        byte[] buf = encrypt(key, message);
        out.writeInt(buf.length);
        out.write(buf);
        out.flush();
    }

}
