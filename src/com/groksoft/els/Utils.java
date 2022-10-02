package com.groksoft.els;

import com.groksoft.els.repository.Libraries;
import com.groksoft.els.repository.Repository;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.io.IoUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
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

    private static Configuration cfg;

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
     * Ellipse a String ending with a filename and extension
     *
     * @param component
     * @param text
     * @return Original or ellipsed string based on pixel length
     */
    public static String ellipseFileString(Component component, String text)
    {
        FontMetrics metrics = component.getFontMetrics(component.getFont());
        int width = metrics.stringWidth(text);
        int max = component.getWidth();
        if (width > max && text.length() > 4)
        {
            String ext = Utils.getFileExtension(text);
            int l = metrics.stringWidth(ext);
            max = max - l - 4;
            text = text.substring(0, text.length() - ext.length());
            while (width > max && text.length() > 4)
            {
                text = StringUtils.abbreviate(text, text.length() - 1);
                width = metrics.stringWidth(text);
            }
            text += ext;
        }
        return text;
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
     * Format an integer number with commas
     *
     * @param value
     * @return String of formatted value
     */
    public static String formatInteger(int value)
    {
        DecimalFormat form = new DecimalFormat("###,###,###,###");
        return form.format(value);
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

        double scale = cfg.getLongScale();

        brief = shortForm.format(value) + " B";
        full = brief;
        if (value >= (scale))
        {
            brief = shortForm.format(value / scale) + " KB";
            full += ", " + brief;
        }
        if (value >= (scale * scale))
        {
            brief = shortForm.format(value / (scale * scale)) + " MB";
            full += ", " + brief;
        }
        if (value >= (scale * scale * scale))
        {
            brief = shortForm.format(value / (scale * scale * scale)) + " GB";
            full += ", " + brief;
        }
        if (value >= (scale * scale * scale * scale))
        {
            brief = longForm.format(value / (scale * scale * scale * scale)) + " TB";
            full += ", " + brief;
        }
        return (isFull ? full : brief);
    }

    /**
     * Get the application Configuration class
     *
     * @return Configuration runtime class
     */
    public static Configuration getCfg()
    {
        return cfg;
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

    public static String getFileExtension(String name)
    {
        String ext = "";
        int i = name.lastIndexOf(".");
        if (i > 0)
            ext = name.substring(i + 1);
        return ext;
    }

    /**
     * Get the file separator for the flavor of operating system
     *
     * @param flavor
     * @return String containing matching file separator character
     * @throws MungeException
     */
    public static String getFileSeparator(String flavor)
    {
        String separator = "";
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
            separator = "\\\\";
        }
        return separator;
    }

    public static FileTime getLocalFileTime(String path)
    {
        if (path != null && path.length() > 0)
        {
            try
            {
                return Files.getLastModifiedTime(Paths.get(path));
            }
            catch (Exception e)
            {
                // noop
            }
        }
        return FileTime.from(0, TimeUnit.SECONDS);
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
     * @param sep  The directory separator for the local O/S, if null get separator from full
     * @return the last path
     */
    public static String getLastPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
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
     * @param sep  The directory separator for the local O/S, if null get separator from full
     * @return String of left path
     */
    public static String getLeftPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
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
     * Get the right path segment
     *
     * @param full Full path to parse
     * @param sep  The directory separator for the local O/S, if null get separator from full
     * @return String of right path
     */
    public static String getRightPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
        int p = full.lastIndexOf(sep);
        if (p >= 0 && p < full.length() - 1)
        {
            path = full.substring(p + 1);
        }
        if (path.length() == 0)
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

    public static String getSeparatorFromPath(String path)
    {
        String separator = "";
        if (path.contains("\\"))
        {
            separator = "\\\\";
        }
        else if (path.contains("/"))
        {
            separator = "/";
        }
        else if (path.contains(":"))
        {
            separator = ":";
        }
        return separator;
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
     * Convert an icon to an image including the alpha channel
     *
     * @param icon Icon to convert
     * @return Image from icon
     */
    public static Image iconToImage(Icon icon)
    {
        if (icon instanceof ImageIcon)
        {
            return ((ImageIcon)icon).getImage();
        }
        else
        {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return image;
        }
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
     * Replace source path separators with pipe character for comparison
     *
     * @param path Path to modify with pipe characters
     * @param separator String separator to use
     * @return String Modified path
     * @throws MungeException
     */
    public static String pipe(String path, String separator)
    {
        String p = path.replaceAll(separator, "|");
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
     * Read an entire file as a String
     *
     * @param filename The local file to read
     */
    public static String readString(String filename) throws Exception
    {
        String content = "";
        URL url = new URL("file:" + filename);
        List<String> lines = IoUtils.readAllLines(url);
        for (int i = 0; i < lines.size(); ++i)
        {
            content += lines.get(i) + System.getProperty("line.separator");
        }
        return content;
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
     * Scrub invalid filename characters from a filename
     * @param name Filename to scrub
     * @return Scrubbed filename, may be the same if there were no invalid characters
     */
    public static String scrubFilename(String name)
    {
        String scubbed = name.replaceAll("[\\\\/:*?\"<>|]", "");
        return scubbed;
    }

    /**
     * Set Utils configuration value for some Utils methods
     * @param c
     */
    public static void setConfiguration(Configuration c)
    {
        Utils.cfg = c;
    }

    /**
     * Find the right-side Nth occurrence of a character
     *
     * @param value String to search
     * @param find Character to find
     * @param rightSideOccurrence Which occurrence to return, 0 = last segment only
     * @return Position in value of Nth occurrence of find character, or -1 if not found
     */
    public static int rightIndexOf(String value, String find, int rightSideOccurrence)
    {
        int count = 0;
        for (int i = value.length() - 1; i > -1; --i)
        {
            if (value.charAt(i) == find.charAt(0))
            {
                if (count == rightSideOccurrence)
                    return i;
                ++count;
            }
        }
        return -1;
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
     * Replace source pipe character with path separators
     *
     * @param path Path to modify with pipe characters
     * @param separator The separator string to use
     * @return String Modified path
     * @throws MungeException
     */
    public static String unpipe(String path, String separator)
    {
        String p = path.replaceAll("\\|", separator);
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
