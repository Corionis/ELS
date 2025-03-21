package com.corionis.els;

import com.corionis.els.gui.Preferences;
import com.corionis.els.gui.browser.NavTreeNode;
import com.corionis.els.repository.Libraries;
import com.corionis.els.repository.Repository;
import com.google.gson.Gson;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.io.IoUtils;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.nio.file.*;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utils class of static utility methods
 * <p>
 * Requires logger to be initialized.
 */
public class Utils
{
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
    public static synchronized long availableSpace(String location)
    {
        long space = 0;
        try
        {
            while (true)
            {
                File f = new File(location);
                if (f.exists())
                {
                    space = f.getUsableSpace();
                    break;
                }
                else
                {
                    location = Utils.getLeftPath(location, Utils.getSeparatorFromPath(location));
                    if (location.length() == 0)
                        break;
                }
            }
        }
        catch (SecurityException e)
        {
            logger.error("Exception '" + e.getMessage() + "' getting usable space from " + location);
        }
        return space;
    }

    /**
     * Compact a String by removing all spaces and regex characters
     *
     * @param value String to clean
     * @return String cleaned
     */
    public static String compactString(String value)
    {
        String bad = "\\.[]{}()<>*+-=!?^$| ";
        String clean = "";
        for (int i = 0; i < value.length(); ++i)
        {
            boolean skip = false;
            for (int j = 0; j < bad.length(); ++j)
            {
                if (value.charAt(i) == bad.charAt(j))
                {
                    skip = true;
                    break;
                }
            }
            if (!skip)
                clean = clean + value.charAt(i);
        }
        return clean;
    }

    /**
     * Compare two TreePaths for sorting
     *
     * @param tp1 TreePath A
     * @param tp2 TreePath B
     * @return -1 if A < B, 0 if A == B, 1 if A > B
     */
    public static int compareTreePaths(TreePath tp1, TreePath tp2)
    {
        // compare each path component name
        String[] sa1 = getTreePathStringArray(tp1);
        String[] sa2 = getTreePathStringArray(tp2);
        int max = Integer.min(sa1.length, sa2.length);
        for (int i = 0; i < max; ++i)
        {
            int comp = sa1[i].compareTo(sa2[i]);
            if (comp != 0)
                return comp;
        }

        // compare lengths
        if (tp1.getPathCount() < tp2.getPathCount())
            return -1;
        if (tp1.getPathCount() > tp2.getPathCount())
            return 1;

        return 0;
    }

    /**
     * Concatenate the values of a String array into one String with optional divider
     *
     * @param array   Strings to concatenate
     * @param divider Optional divider String between each element
     * @return String The String array concatenated
     */
    public static String concatStringArray(String[] array, String divider)
    {
        String result = "";
        for (int i = 0; i < array.length; ++i)
        {
            if (array[i].length() == 0)
                result += "?";
            else
                result += array[i];
            if (i + 1 < array.length)
                result += divider;
        }
        return result;
    }

    /**
     * Ellipse a String ending with a filename and extension
     *
     * @param component
     * @param text
     * @return Original or ellipsed string based on pixel length
     */
    public static synchronized String ellipseFileString(Component component, String text)
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
     * Format seconds to the greatest of hours, minutes and seconds
     *
     * @param seconds
     * @return Formatted String
     */
    public static String formatDuration(long seconds)
    {
        Duration duration = Duration.ofSeconds(seconds);
        int hours = duration.toHoursPart();
        int mins = duration.toMinutesPart();
        int secs = duration.toSecondsPart();
        String fd = ((hours > 0) ? hours + "h " : "") + ((mins > 0) ? mins + "m " : "") + ((secs > 0) ? secs + "s" : "");
        return fd;
    }

    /**
     * Format an integer as a hexadecimal string
     *
     * @param value Integer to format
     * @param width Desired total width with leading zeros
     * @return Formatted hexadecimal string
     */
    public static synchronized String formatHex(int value, int width)
    {
        String result = Integer.toHexString(value);
        while (result.length() < width)
        {
            result = "0" + result;
        }
        return result;
    }

    /**
     * Format an integer number with commas
     *
     * @param value
     * @return String of formatted value
     */
    public static synchronized String formatInteger(int value)
    {
        DecimalFormat form = new DecimalFormat("###,###,###,###");
        return form.format(value);
    }

    /**
     * Format a long number with byte, MB, GB and TB as applicable
     *
     * @param value  Long value to format
     * @param isFull Full long list of B, KB, MB, etc. = true, short highest value = false
     * @param scale  Binary (1024) or decimal (1000) scale
     * @return String Formatting text
     */
    public static synchronized String formatLong(long value, boolean isFull, double scale)
    {
        String full;
        String brief;
        DecimalFormat longForm = new DecimalFormat("###,###,###,###,###,###,###,###.###");
        DecimalFormat shortForm = new DecimalFormat("###,###,###,###,###,###,###,###.#");

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
     * Format a long number with bps, Mbps, Gbps and Tbps as applicable
     *
     * @param value Long value to format
     * @param scale Binary (1024) or decimal (1000) scale
     * @return String Formatting text
     */
    public static synchronized String formatRate(long value, double scale)
    {
        String result;
        DecimalFormat form = new DecimalFormat("###,###,###,###,###,###,###,###.##");

        result = form.format(value) + " bps";
        if (value >= (scale))
        {
            result = form.format(value / scale) + " Kbs";
        }
        if (value >= (scale * scale))
        {
            result = form.format(value / (scale * scale)) + " Mbps";
        }
        if (value >= (scale * scale * scale))
        {
            result = form.format(value / (scale * scale * scale)) + " Gbps";
        }
        if (value >= (scale * scale * scale * scale))
        {
            result = form.format(value / (scale * scale * scale * scale)) + " Tbps";
        }
        return result;
    }

    /**
     * Get an absolute working file path
     * <br/>
     * If the filename is relative the current working directory is prefixed,
     * otherwise the filename is returned.
     * <br/>
     * The path separator is for the local system.
     *
     * @param filename Filename to set
     * @return String Absolute path to work file
     */
    public static String getFullPathLocal(String filename)
    {
        String location;
        if (filename.matches("^\\\\[a-zA-Z]:.*") || filename.matches("^/[a-zA-Z]:.*"))
            filename = filename.substring(1);

        if (Utils.isRelativePath(filename))
            location = System.getProperty("user.dir") + System.getProperty("file.separator") + filename;
        else
            location = filename;

        location = pipe(location);
        location = unpipe(location, System.getProperty("file.separator"));
/*
        File file = new File(location);
        try
        {
            location = file.getCanonicalPath();
        }
        catch (IOException e)
        {
            location = file.getPath();
        }
*/
        return location;
    }

    /**
     * Get the duration string
     *
     * @param millis
     * @return String
     */
    public static synchronized String getDuration(long millis)
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
     * Parse file extension from filename
     *
     * @param name Filename to parse
     * @return Extension
     */
    public static synchronized String getFileExtension(String name)
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
    public static synchronized String getFileSeparator(String flavor)
    {
        String separator = "";
        if (flavor.equalsIgnoreCase(Libraries.WINDOWS))
        {
            separator = "\\";
        }
        else if (flavor.equalsIgnoreCase(Libraries.LINUX))
        {
            separator = "/";
        }
        else if (flavor.equalsIgnoreCase(Libraries.MAC))
        {
            separator = "/";
        }
        return separator;
    }

    /**
     * Get the local system hostname
     *
     * @return Hostname or empty
     */
    public static synchronized String getHostname()
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
    public static synchronized String getLastPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
        full = pipe(full);
        int p = full.indexOf("|");
        if (p >= 0)
        {
            path = full.substring(0, p);
        }
        else
        {
            path = full;
        }
        path = unpipe(path, sep);
        return path;
    }

    /**
     * Get the path to the left of the filename
     *
     * @param full Full path to parse
     * @param sep  The directory separator for the local O/S, if null get separator from full
     * @return String of left path
     */
    public static synchronized String getLeftPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
        full = pipe(full);
        int p = full.lastIndexOf("|");
        if (p >= 0)
        {
            path = full.substring(0, p);
        }
        else
        {
            path = full;
        }
        path = unpipe(path, sep);
        return path;
    }

    /**
     * Read last modified time from path
     *
     * @param path Path to get time
     * @return FileTime of path
     */
    public static synchronized FileTime getLocalFileTime(String path)
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
     * Get list of local hard drives
     * <br/>
     * Empty drives such as a CD or DVD with no disc are not returned.
     *
     * @return Pipe-separated list of hard drives, e.g. C:\|D:\
     */
    public static String getLocalHardDrives()
    {
        File[] drives;
        drives = File.listRoots();
        String driveList = "";
        for (int i = 0; i < drives.length; ++i)
        {
            File drive = drives[i];
            boolean empty = false;
            try
            {
                FileStore store = Files.getFileStore(drive.toPath());
            }
            catch (IOException ioe)
            {
                empty = true;
            }
            if (!empty)
            {
                if (i > 0)
                    driveList += "|";
                driveList += drive.getPath();
            }
        }
        return driveList;
    }

    /**
     * O/S independent way of setting file permissions
     *
     * @param thing String of local file to check
     * @return int permissions
     */
    public static synchronized int getLocalPermissions(String thing)
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

    /**
     * Get the operating system name from the JVM
     *
     * @return O/S name string, "Linux", "Mac" or "Windows" only
     */
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
    public static synchronized int getPort(String site)
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
     * Get a Point to position component centered on it's parent
     *
     * @param parent The parent Window
     * @param window The current Window
     * @return Point
     */
    public static Point getRelativePosition(Window parent, Window window)
    {
        Point parentPos = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension mySize = window.getSize();
        Point center = new Point(parentPos.x + parentSize.width / 2, parentPos.y + parentSize.height / 2);
        int x = center.x - mySize.width / 2;
        int y = center.y - mySize.height / 2;
        Point position = new Point(x, y);
        return position;
    }

    /**
     * Get the right path segment
     *
     * @param full Full path to parse
     * @param sep  The directory separator for the local O/S, if null get separator from full
     * @return String of right path
     */
    public static synchronized String getRightPath(String full, String sep)
    {
        String path = "";
        if (sep == null)
            sep = getSeparatorFromPath(full);
        full = pipe(full);
        int p = full.lastIndexOf("|");
        if (p >= 0 && p < full.length() - 1)
        {
            path = full.substring(p + 1);
        }
        if (path.length() == 0)
        {
            path = full;
        }
        path = unpipe(path, sep);
        return path;
    }

    /**
     * Gets scaled value
     *
     * @param size the string to parse
     * @return the scaled value
     */
    public static synchronized long getScaledValue(String size)
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
     * Parse the file separator from a path
     *
     * @param path Path to parse
     * @return File separator character
     */
    public static synchronized String getSeparatorFromPath(String path)
    {
        String separator = "";
        if (path.contains("\\"))
        {
            separator = "\\";
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
    public static synchronized String getShortPath(String full, String sep)
    {
        String path = "";
        full = pipe(full);
        int p = full.lastIndexOf("|");
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
        path = unpipe(path, sep);
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
     * Get a date/time stamped filename
     * <br/>
     * If the Repository temp_dated = true a formatted stamp is appended to the filename.
     *
     * @param repo     Repository with temp_dated defined
     * @param filename The left portion of the filename, no extension
     * @return String The filename with an optional date/time stamp appended, or not
     */
    public static String getStampedFilename(Repository repo, String filename)
    {
        String stamped = "";
        if (repo.getLibraryData().libraries.temp_dated != null && repo.getLibraryData().libraries.temp_dated)
        {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
            LocalDateTime now = LocalDateTime.now();
            stamped = filename + "-" + dtf.format(now);
        }
        else
            stamped = filename;
        return stamped;
    }

    /**
     * Get the local system temporary files directory + ELS_Updater_ + user name
     *
     * @return String path to directory, no trailing separator
     */
    public static String getTempUpdaterDirectory()
    {
        String path = System.getProperty("java.io.tmpdir") + System.getProperty("file.separator") + "ELS_Updater_" + System.getProperty("user.name");
        return path;
    }

    /**
     * Get the left-side absolute path for a temporary file
     * <br/>
     * If the Repository temp_location is defined use that path, otherwise use
     * the current working directory. In either case any relative path is expanded
     * to be an fully-qualified path.
     *
     * @param repo     Repository with temp_location defined
     * @param filename The right portion of the filename
     * @return String The fully-qualified path
     */
    public static String getTemporaryFilePrefix(Repository repo, String filename)
    {
        String location = filename;
        String path = "";
        if (repo.getLibraryData().libraries.temp_location != null && repo.getLibraryData().libraries.temp_location.length() > 0)
            path = repo.getLibraryData().libraries.temp_location;
        else
            path = "output";
        String sep = repo.getSeparator();
        if (!path.endsWith(sep))
            path += sep;
        location = path + filename;
        location = Utils.getFullPathLocal(location);
        return location;
    }

    /**
     * Returns an ordered String array of the elements of a TreePath
     *
     * @param tp TreePath
     * @return String[] of the elements of the TreePath
     */
    public static String[] getTreePathStringArray(TreePath tp)
    {
        String[] ps = null;
        if (tp.getPathCount() > 0)
        {
            ps = new String[tp.getPathCount()];
            Object[] objs = tp.getPath();
            for (int i = 0; i < tp.getPathCount(); ++i)
            {
                NavTreeNode node = (NavTreeNode) objs[i];
                ps[i] = node.getUserObject().name;
            }
        }
        return ps;
    }

    /**
     * Convert an icon to an image including the alpha channel
     *
     * @param icon Icon to convert
     * @return Image from icon
     */
    public static synchronized Image iconToImage(Icon icon)
    {
        if (icon instanceof ImageIcon)
        {
            return ((ImageIcon) icon).getImage();
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
     * Detect whether the coordinates appear on any screen
     *
     * @param locX
     * @param locY
     * @return true if NOT within any screen (monitor), otherwise false
     */
    public static boolean isOffScreen(int locX, int locY)
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();
        for (int screenIndex = 0; screenIndex < devices.length; ++screenIndex)
        {
            Rectangle bounds = devices[screenIndex].getDefaultConfiguration().getBounds();
            if (bounds.contains(locX, locY))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Detect whether the coordinates appear on any screen
     *
     * @param locX
     * @param locY
     * @return true if within any screen (monitor), otherwise false
     */
    public static boolean isOnScreen(int locX, int locY)
    {
        return !isOffScreen(locX, locY);
    }

    /**
     * Is the operating system Linux?
     *
     * @return true if Linux from getOS(), otherwise false
     */
    public static boolean isOsLinux()
    {
        if (getOS().equalsIgnoreCase("linux"))
            return true;
        return false;
    }

    /**
     * Is the operating system MacOS
     *
     * @return true if mac from getOS(), otherwise false
     */
    public static boolean isOsMac()
    {
        if (getOS().equalsIgnoreCase("mac"))
            return true;
        return false;
    }

    /**
     * Is the operating system Windows
     *
     * @return true if windows from getOS(), otherwise false
     */
    public static boolean isOsWindows()
    {
        if (getOS().equalsIgnoreCase("windows"))
            return true;
        return false;
    }

    /**
     * Is the path relative or absolute?
     *
     * @param path Path to check
     * @return true if a relative path, false if an absolute fully-qualified path
     */
    public static boolean isRelativePath(String path)
    {
        if (path.matches("^[a-zA-Z]:.*"))
            return false;
        if (path.startsWith("/") || path.startsWith("\\") || path.startsWith("|"))
            return false;
        return true;
    }

    /**
     * Make a path a Linux path with forward-slash separators
     * <br/>
     * Internally Java handles using Linux separators
     *
     * @param path
     * @return String path with / separators
     */
    public static String makeLinuxPath(String path)
    {
        path = pipe(path);
        path = unpipe(path, "/");
        return path;
    }

    /**
     * Make a path relative to the working directory if possible
     *
     * @param workingDirectory The current working directory, localContext.cfg.getWorkingDirectory()
     * @param path             The path to reduce
     * @return String the path, potentially shortened to be relative to the working path
     */
    public static String makeRelativePath(String workingDirectory, String path)
    {
        workingDirectory = pipe(workingDirectory);
        path = pipe(path);
        if (!path.equals(workingDirectory) && path.startsWith(workingDirectory))
            path = path.substring(workingDirectory.length() + 1);
        path = unpipe(path, "/");
        return path;
    }

    /**
     * Parse a String command line with quoted values
     * <p><p>
     * Parses individual arguments on whitespace and quoted
     * values as separate arguments.
     * <p><p>
     * Does not use regular expressions so command lines with
     * arguments containing regex characters are parsed correctly.
     * <p><p>
     * Wildcard expansion is not performed.
     *
     * @param commandLine String command line
     * @return String[] of individual arguments and quoted values
     */
    public static String[] parseCommandLIne(String commandLine)
    {
        String arg = "";
        ArrayList<String> list = new ArrayList<>();

        int len = commandLine.length();
        int i;
        for (i = 0; i < commandLine.length(); ++i)
        {
            String c;
            if (i < commandLine.length() - 1)
                c = commandLine.substring(i, i + 1);
            else
                c = commandLine.substring(i);
            if (c.equals("\""))
            {
                ++i;
                while (i < commandLine.length() - 1)
                {
                    c = commandLine.substring(i, i + 1);
                    if (c.equals("\""))
                        break;
                    arg += c;
                    ++i;
                }
                list.add(arg);
                arg = "";
                ++i;
            }
            else if (c.equals(" ") || c.equals("\t"))
            {
                list.add(arg);
                arg = "";
            }
            else
                arg += c;
        }

        if (arg.length() > 0)
            list.add(arg);

        String[] results = list.toArray(new String[0]);
        return results;
    }

    /**
     * Parse the host from a site string
     * <p>
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location Site string
     * @return String host
     */
    public static synchronized String parseHost(String location)
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
    public static synchronized String parsePort(String location)
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
     * Replace all path separators of / or \ with pipe character
     *
     * @param path Path to modify with pipe characters
     * @return String Modified path
     * @throws MungeException
     */
    public static synchronized String pipe(String path)
    {
        String separator = "\\\\";
        path = path.replaceAll(separator, "|");
        separator = "/";
        path = path.replaceAll(separator, "|");
        return path;
    }

    /**
     * Replace source path separators with pipe character for comparison
     *
     * @param repo Repository of source of path
     * @param path Path to modify with pipe characters
     * @return String Modified path
     * @throws MungeException
     */
    public static synchronized String pipe(Repository repo, String path) throws MungeException
    {
        String separator = repo.getWriteSeparator();
        if (separator.equals("\\"))
            separator = "\\\\";
        String p = path.replaceAll(separator, "|");
        return p;
    }

    /**
     * Replace source path separators with pipe character for comparison
     *
     * @param path      Path to modify with pipe characters
     * @param separator String separator to use
     * @return String Modified path
     * @throws MungeException
     */
    public static synchronized String pipe(String path, String separator)
    {
        if (separator.equals("\\"))
            separator = "\\\\";
        String p = path.replaceAll(separator, "|");
        return p;
    }

    /**
     * Read the user Preferences file
     *
     * @param context The Context where it's preferences element will be populated
     */
    public static void readPreferences(Context context)
    {
        try
        {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(Paths.get(context.preferences.getFullPath(context.cfg.getWorkingDirectory()))));
            Preferences prefs = gson.fromJson(json, context.preferences.getClass());
            if (prefs != null)
            {
                context.preferences = gson.fromJson(json, context.preferences.getClass());
                context.preferences.setContext(context);
            }
        }
        catch (IOException e)
        {
            // file might not exist
        }
    }

    /**
     * Read an entire file as a String
     *
     * @param filename The local file to read
     */
    public static synchronized String readString(String filename) throws Exception
    {
        String content = "";
        File file = new File(filename);
        if (file.exists())
        {
            URL url = new URL("file:" + filename);
            List<String> lines = IoUtils.readAllLines(url);
            for (int i = 0; i < lines.size(); ++i)
            {
                content += lines.get(i) + System.getProperty("line.separator");
            }
        }
        return content;
    }

    /**
     * Remove a directory tree and contents
     *
     * @param directory The directory tree to be deleted
     * @return true if not all directories, files were also deleted
     */
    public static synchronized boolean removeDirectoryTree(File directory)
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

    public static String[] removeEmptyElements(String[] elements)
    {
        String[] cleaned = new String[elements.length];
        int j = 0;
        for (int i = 0; i < elements.length; ++i)
        {
            if (elements[i] != null && elements[i].length() > 0)
                cleaned[j++] = elements[i];
        }
        elements = new String[j];
        for (int i = 0; i < j; ++i)
        {
            elements[i] = cleaned[i];
        }
        return elements;
    }

    /**
     * Find the right-side Nth occurrence of a character
     *
     * @param value               String to search
     * @param find                Character to find
     * @param rightSideOccurrence Which occurrence to return, 0 = last segment only
     * @return Position in value of Nth occurrence of find character, or -1 if not found
     */
    public static synchronized int rightIndexOf(String value, String find, int rightSideOccurrence)
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
     * Scrub invalid filename characters from a filename
     *
     * @param name Filename to scrub
     * @return Scrubbed filename, may be the same if there were no invalid characters
     */
    public static synchronized String scrubFilename(String name)
    {
        String scubbed = name.replaceAll("[\\/:*?\"<>|]", "");
        return scubbed;
    }

    /**
     * Translate Linux mode to a set of Posix file permissions
     *
     * @param mode Integer of item mode
     * @return Set<PosixFilePermission> of file permissions
     */
    public static Set<PosixFilePermission> translateModeToPosix(int mode)
    {
        Set<PosixFilePermission> perms = new HashSet<>();
        if ((mode & 0001) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_EXECUTE);
        }
        if ((mode & 0002) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_WRITE);
        }
        if ((mode & 0004) > 0)
        {
            perms.add(PosixFilePermission.OTHERS_READ);
        }
        if ((mode & 0010) > 0)
        {
            perms.add(PosixFilePermission.GROUP_EXECUTE);
        }
        if ((mode & 0020) > 0)
        {
            perms.add(PosixFilePermission.GROUP_WRITE);
        }
        if ((mode & 0040) > 0)
        {
            perms.add(PosixFilePermission.GROUP_READ);
        }
        if ((mode & 0100) > 0)
        {
            perms.add(PosixFilePermission.OWNER_EXECUTE);
        }
        if ((mode & 0200) > 0)
        {
            perms.add(PosixFilePermission.OWNER_WRITE);
        }
        if ((mode & 0400) > 0)
        {
            perms.add(PosixFilePermission.OWNER_READ);
        }
        return perms;
    }

    /**
     * Replace source pipe character with path separators
     *
     * @param repo Repository of source of path
     * @param path Path to modify with pipe characters
     * @return String Modified path
     * @throws MungeException
     */
    public static synchronized String unpipe(Repository repo, String path) throws MungeException
    {
        String separator = repo.getWriteSeparator();
        if (separator.equals("\\"))
            separator = "\\\\";
        String p = path.replaceAll("\\|", separator);
        return p;
    }

    /**
     * Replace source pipe character with path separators
     *
     * @param path      Path to modify with pipe characters
     * @param separator The separator string to use
     * @return String Modified path
     */
    public static synchronized String unpipe(String path, String separator)
    {
        if (separator.equals("\\"))
            separator = "\\\\";
        String p = path.replaceAll("\\|", separator);
        return p;
    }

}
