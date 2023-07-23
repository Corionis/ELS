package com.groksoft.els.repository;

import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.ArrayList;

/**
 * Hint Keys class.
 * <p>
 * Correlates the UUIDs in the publisher and subscriber JSON files
 * with shorter names used inside hint .else files to track the
 * status of completion for each defined node of an ELS system.
 */
public class HintKeys
{
    private Context context;
    private String filename;
    private ArrayList<HintKey> keys;
    private transient Logger logger = LogManager.getLogger("applog");

    public HintKeys(Context context)
    {
        this.context = context;
        this.keys = new ArrayList<>();
    }

    public HintKey findKey(String uuid)
    {
        for (HintKey key : keys)
        {
            if (key.uuid.equals(uuid))
                return key;
        }
        return null;
    }

    public ArrayList<HintKey> get()
    {
        return keys;
    }

    public String getFilename()
    {
        return filename;
    }

    public void read(String file) throws Exception
    {
        if (Utils.isRelativePath(file))
            filename = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + file;
        else
            filename = file;
        BufferedReader br = new BufferedReader(new FileReader(filename));
        int count = 0;
        boolean foundPublisher = false;
        boolean foundSubscriber = false;
        String line;
        String part;
        while ((line = br.readLine()) != null)
        {
            ++count;
            String[] parts = line.split("[\\s]+");
            if (parts.length > 0) // skip blank lines
            {
                part = parts[0].trim();
                if (part.length() < 1 || part.equals("#")) // skip comment lines
                {
                    continue;
                }
                if (parts.length != 2)
                {
                    throw new MungeException("Malformed line " + count + " reading ELS keys file: " + file);
                }

                if (!context.cfg.isStatusServer() && !context.cfg.isNavigator())
                {
                    if (parts[1].equals(context.publisherRepo.getLibraryData().libraries.key))
                    {
                        foundPublisher = true;
                    }

                    if (parts[1].equals(context.subscriberRepo.getLibraryData().libraries.key))
                    {
                        foundSubscriber = true;
                    }
                }

                HintKey key = new HintKey();
                key.name = parts[0].trim();
                key.uuid = parts[1].trim();

                if (keys == null)
                    keys = new ArrayList<HintKey>();

                keys.add(key);
            }
        }

        if (!context.cfg.isStatusServer() && !context.cfg.isNavigator())
        {
            if (!foundPublisher)
                throw new MungeException("The current publisher key was not found in ELS keys file: " + file);
            if (context.subscriberRepo != null && !foundSubscriber)
                throw new MungeException("The current subscriber key was not found in ELS keys file: " + file);
        }

        logger.info("Read Hint keys " + file + " successfully");
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void write(String header) throws Exception
    {
        String base = FilenameUtils.getFullPathNoEndSeparator(filename);
        File outdir = new File(base);
        outdir.mkdirs();

        BufferedWriter fw = new BufferedWriter(new FileWriter(filename));
        fw.write(header);
        fw.write(System.getProperty("line.separator"));
        for (int i = 0; i < keys.size(); ++i)
        {
            HintKey hintKey = keys.get(i);
            if (hintKey.name != null && hintKey.name.length() > 0 && hintKey.uuid != null && hintKey.uuid.length() > 0)
            {
                String line = hintKey.name + "\t\t" + hintKey.uuid + System.getProperty("line.separator");
                fw.write(line);
            }
        }
        fw.write(System.getProperty("line.separator"));
        fw.close();
    }

}
