package com.groksoft.volmunger;

import java.io.*;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Key;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.groksoft.volmunger.repository.Libraries;
import com.groksoft.volmunger.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * The type Utils.
 */
public class Utils
{
    private static Logger logger = LogManager.getLogger("applog");

    private static Cipher cipher = null;

    /**
     * Do not instantiate.
     */
    private Utils()
    {
        // do not instantiate
    }

    /**
     * Available space on local target.
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
            space = f.getFreeSpace();
        } catch (SecurityException e)
        {
            logger.error("Exception '" + e.getMessage() + "' getting available space from " + location);
        }
        return space;
    }

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
        } catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return encrypted;
    }

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
        } catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        return output;
    }

    /**
     * Get the file separator for the flavor of operating system
     *
     * @param flavor
     * @return String containing matching file separator character
     * @throws MungerException
     */
    public static String getFileSeparator(String flavor) throws MungerException
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
        else if (flavor.equalsIgnoreCase(Libraries.APPLE))
        {
            separator = ":";
        }
        else
        {
            throw new MungerException("unknown flavor '" + flavor + "'");
        }
        return separator;
    }

    /**
     * Gets scaled value.
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
     * Format a long number with byte, MB, GB and TB as applicable
     */
    public static String formatLong(long value)
    {
        String response;
        DecimalFormat shorterFormatter = new DecimalFormat("###,###,###,###,###,###,###,###.###");
        DecimalFormat longerFormatter = new DecimalFormat("###,###,###,###,###,###,###,###");
        response = longerFormatter.format(value) + " bytes";
        if (value > (1024))
        {
            response += ", " + longerFormatter.format(value / 1024.0) + " KB";
        }
        if (value > (1024.0 * 1024.0))
        {
            response += ", " + longerFormatter.format(value / (1024.0 * 1024.0)) + " MB";
        }
        if (value > (1024.0 * 1024.0 * 1024.0))
        {
            response += ", " + longerFormatter.format(value / (1024.0 * 1024.0 * 1024.0)) + " GB";
        }
        if (value > (1024.0 * 1024.0 * 1024.0 * 1024.0))
        {
            response += ", " + shorterFormatter.format(value / (1024.0 * 1024.0 * 1024.0 * 1024.0)) + " TB";
        }
        return response;
    }

    /**
     * Gets last path.
     *
     * @param full the full
     * @return the last path
     */
    public static String getLastPath(String full, String sep) throws MungerException
    {
        String path = "";
        int p = full.indexOf(sep);
        if (p >= 0)
        {
            path = full.substring(0, p);
        } else
        {
            p = full.indexOf(sep);
            if (p >= 0)
            {
                path = full.substring(0, p);
            } else
            {
                path = full;
            }
        }
        return path;
    }

    /**
     * O/S independent way of setting file permissions<br/>
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
     * Gets stack trace.
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
     * Parse the host from a site string
     * <p>
     * Expected format: [hostname|IP address]:[port number]
     *
     * @param location
     * @return host
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

    public static String pipe(Repository repo, String path) throws MungerException
    {
        String p = path.replaceAll(repo.getWriteSeparator(), "|");
        return p;
    }

    public static String read(DataInputStream in, String key)
    {
        byte[] buf = {};
        String input = "";
        while (true)
        {
            try
            {
                int count = in.readInt();
                if (count > 0)
                {
                    buf = new byte[count];
                    int readCount = in.read(buf);
                    if (count != readCount)
                    {
                        logger.warn("read buffer counts do not match " + count + " size, " + readCount + " actually read");
                    }
                    input += decrypt(key, buf);
                }
                break;
            } catch (SocketTimeoutException e)
            {
                continue;
            } catch (EOFException e)
            {
                input = null;
                break;
            } catch (IOException e)
            {
                if (e.getMessage().toLowerCase().contains("connection reset"))
                {
                    logger.info("connection closed by client");
                    input = null;
                    break;
                }
                logger.error(e.getMessage());
                break;
            }
        }
        return input;
    }

    public static void write(DataOutputStream out, String key, String message)
    {
        try
        {
            byte[] buf = encrypt(key, message);
            out.writeInt(buf.length);
            out.write(buf);
            out.flush();
        } catch (IOException e)
        {
            if (!e.getMessage().toLowerCase().contains("connection reset"))
            {
                logger.error(e.getMessage());
            }
        }
    }

}
