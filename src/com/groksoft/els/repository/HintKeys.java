package com.groksoft.els.repository;

import com.groksoft.els.Main;
import com.groksoft.els.MungeException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class HintKeys
{
    private Main.Context context;
    private String filename;
    private ArrayList<HintKey> keys;
    public HintKeys(Main.Context ctx)
    {
        context = ctx;
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

    public String getFilename()
    {
        return filename;
    }

    public void read(String file) throws Exception
    {
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

                if (parts[1].equals(context.publisherRepo.getLibraryData().libraries.key))
                {
                    foundPublisher = true;
                }

                if (parts[1].equals(context.subscriberRepo.getLibraryData().libraries.key))
                {
                    foundSubscriber = true;
                }

                HintKey key = new HintKey();
                key.name = parts[0].trim();
                key.uuid = parts[1].trim();

                if (keys == null)
                    keys = new ArrayList<HintKey>();

                keys.add(key);
            }
        }
        if (!foundPublisher)
            throw new MungeException("The current publisher key was not found in ELS keys file: " + file);
        if (context.subscriberRepo != null && !foundSubscriber)
            throw new MungeException("The current subscriber key was not found in ELS keys file: " + file);
    }

    public class HintKey
    {
        String name;
        String uuid;
    }

}
