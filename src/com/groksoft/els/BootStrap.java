package com.groksoft.els;

//import javax.swing.*;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import javax.swing.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.nio.file.*;

/**
 * BootStrap class
 * <p>Java main() for processing updates.</p>
 * <p>ELS uses an embedded JRE from the OpenJDK project.<br/>
 *   * https://openjdk.org/<br/>
 *   * https://github.com/AdoptOpenJDK<br/>
 *   * https://wiki.openjdk.org/display/jdk8u/Main<br/>
 *   * https://github.com/AdoptOpenJDK/openjdk9-binaries/releases</p>
 *
 * TODO + Add a menu option for "Create shortcut" that uses Generator and current settings
 * TODO remove man pages from Java staging files
 */
public class BootStrap
{
    private Main main;
    private String restartCommandLine;
    private String updateFile;
    private String updateTargetPath;

    private JDialog dialog;
    private JPanel panel;
    private JLabel label;

    public BootStrap(String[] args)
    {
        this.process(args);
    }

    /**
     * main() entry point
     *
     * @param args the input arguments
     */
    public static void main(String[] args)
    {
        new BootStrap(args);
    }

    private boolean isWindows()
    {
        String os = System.getProperty("os.name");
        if (os.toLowerCase().startsWith("windows"))
            return true;
        return false;
    }

    private void log(String string)
    {
        // for updates only - write log file to system tmp directory
    }

    private void process(String[] args)
    {
        // instantiate (load) all ELS Java library-level classes needed in BootStrap
        if (isWindows())
        {
            // zip
        }
        else
        {
            // tar gz
        }

        // check if an update is ready to be installed

        // otherwise run ELS
        System.out.println("BOOTSTRAP");
        main = new Main(args);
        if (main.navigatorSession)
        {
            Runtime.getRuntime().addShutdownHook(new Thread(() ->
            {
                try
                {
                    System.out.println("Navigator shutdown");

                    // check if an update is ready to be installed then restart ELS
                    updateFile = main.context.cfg.getUpdateFilePath();
                    if (updateExists())
                    {
                        System.out.println("update file = " + updateFile);

                        updateTargetPath = main.context.cfg.getUpdateTargetPath();
                        System.out.println("target path = " + updateTargetPath);
                        update();
                    }
                    else
                        System.out.println("no update file = " + updateFile);
                }
                catch (Exception e)
                {
                    System.out.println("BootStrap: " + Utils.getStackTrace(e));
                }
            }));
            System.out.println("Navigator shutdown hook added");

            System.out.println("EXITING BOOTSTRAP");
        }

    }

    private void update()
    {
        try
        {
            //javax.swing.SwingUtilities.invokeAndWait(new Runnable()
            {
                //@Override
                //public void run()
                {
/*
                    dialog = new JDialog();
                    panel = new JPanel();
                    label = new JLabel("STATUS");
                    panel.add(label);
                    dialog.add(panel);
                    dialog.setVisible(true);
*/

                    if (isWindows())
                        updateWindows();
                    else
                        updateLinux();

//                    dialog.setVisible(false);
                }
            }   // );
        }
        catch (Exception e)
        {
            System.out.println("BootStrap: " + Utils.getStackTrace(e));
        }
    }

    private boolean updateExists()
    {
        boolean sense = false;
        File update = new File(updateFile);
        if (update.exists())
        {
            System.out.println("Update exists");
            sense = true;
        }
        return sense;
    }

    private void updateLinux()
    {
        // unpack well-defined archive tar.gz to use same download as users
        try
        {
            // Apache Commons Compress
            // https://commons.apache.org/proper/commons-compress
            Path path = Paths.get(updateFile);
            Path outPath = Paths.get(updateTargetPath);
            TarArchiveInputStream in = new TarArchiveInputStream(new GzipCompressorInputStream(new BufferedInputStream(Files.newInputStream(path))));
            TarArchiveEntry entry = null;
            while ((entry = in.getNextTarEntry()) != null)
            {
                String name = entry.getName();
                if (name.length() == 0)
                    continue;
                if (name.startsWith("ELS"))
                {
                    if (name.equals("ELS/"))
                        continue;
                    name = name.substring(4, name.length());
                }
                Path entryPath = outPath.resolve(name);
                System.out.println(entryPath.toString());
//                label.setText(entryPath.toString());
                if (entry.isDirectory())
                {
                    if (!Files.exists(entryPath))
                        Files.createDirectories(entryPath);
                    Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                }
                else
                {
                    Files.createDirectories(entryPath.getParent());
//                    if (Files.exists(entryPath))
//                        Files.delete(entryPath);
                    Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
                    int mode = entry.getMode();
//                    Files.setPosixFilePermissions(entryPath, );
                    Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                }
            }
            in.close();


            // delete archive when done

        }
        catch (Exception e)
        {
            System.out.println(Utils.getStackTrace(e));
        }
    }

    private void updateWindows()
    {
        // unpack well-defined archive zip to use same download as users
        try
        {
            // Apache Commons Compress
            // https://commons.apache.org/proper/commons-compress
            Path path = Paths.get(updateFile);
            Path outPath = Paths.get(updateTargetPath);
            ZipArchiveInputStream in = new ZipArchiveInputStream(new BufferedInputStream(Files.newInputStream(path)));
            ZipArchiveEntry entry = null;
            while ((entry = in.getNextZipEntry()) != null)
            {
                String name = entry.getName();
                if (name.length() == 0)
                    continue;
                if (name.startsWith("ELS"))
                {
                    if (name.equals("ELS/"))
                        continue;
                    name = name.substring(4, name.length());
                }
                Path entryPath = outPath.resolve(name);
                System.out.println(entryPath.toString());
//                label.setText(entryPath.toString());
                if (entry.isDirectory())
                {
                    if (!Files.exists(entryPath))
                        Files.createDirectories(entryPath);
                    Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                }
                else
                {
                    Files.createDirectories(entryPath.getParent());
//                    if (Files.exists(entryPath))
//                        Files.delete(entryPath);
                    Files.copy(in, entryPath, StandardCopyOption.REPLACE_EXISTING);
//                    int mode = entry.getMode();
//                    Files.setPosixFilePermissions(entryPath, );
                    Files.setLastModifiedTime(entryPath, entry.getLastModifiedTime());
                }
            }
            in.close();


            // delete archive when done

        }
        catch (Exception e)
        {
            System.out.println(Utils.getStackTrace(e));
        }
    }

}
