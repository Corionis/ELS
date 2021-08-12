package com.groksoft.els.repository;

import com.groksoft.els.Main;
import com.groksoft.els.MungerException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class HintKeys
{
    public class HintKey
    {
        String type; // Me or For
        String name;
        String uuid;
    }

    private Main.Context context;
    private String filename;
    private  ArrayList<HintKey> keys;

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
        boolean foundFor = false;
        boolean foundMe = false;
        boolean foundPublisher = false;
        boolean foundSubscriber = false;
        String line;
        String part;
        while ((line = br.readLine()) != null)
        {
            ++count;
            String[] parts = line.split("[\\s]+");
            if (parts.length > 0)
            {
                if (parts.length > 0) // skip blank lines
                {
                    part = parts[0].trim();
                    if (part.length() < 1 || part.equals("#")) // skip comment lines
                    {
                        continue;
                    }
                    if (parts.length != 3)
                    {
                        throw new MungerException("Malformed line " + count + " reading ELS keys file: " + file);
                    }
                    if (part.equalsIgnoreCase("me")) // QUESTION: Is this needed?
                    {
                        if (foundMe)
                        {
                            throw new MungerException("There can be only one 'Me' in the ELS keys file: " + file);
                        }
                        foundMe = true;
                    }
                    else if ( ! part.equalsIgnoreCase("for"))
                    {
                        throw new MungerException("First word on key line must be either 'Me' or 'For', case insensitive. Line " + count + " in ELS keys file: " + file);
                    }
                    else
                    {
                        foundFor = true;
                    }

                    if (parts[2].equals(context.publisherRepo.getLibraryData().libraries.key))
                    {
                        foundPublisher = true;
                    }

                    if (parts[2].equals(context.subscriberRepo.getLibraryData().libraries.key))
                    {
                        foundSubscriber = true;
                    }

                    HintKey key = new HintKey();
                    key.type = part;
                    key.name = parts[1].trim();
                    key.uuid = parts[2].trim();

                    if (keys == null)
                        keys = new ArrayList<HintKey>();
                    keys.add(key);
                }
            }
        }
        if (!foundMe)
            throw new MungerException("A key line with first word 'Me' was not found");
        if (!foundFor)
            throw new MungerException("No key line with first word 'For' was found. Disable ELS hints by removing the -k argument from the command line");
        if (!foundPublisher)
            throw new MungerException("The current publisher key was not found in ELS keys file: " + file);
        if (context.subscriberRepo!= null && !foundSubscriber)
            throw new MungerException("The current subscriber key was not found in ELS keys file: " + file);
    }

}
