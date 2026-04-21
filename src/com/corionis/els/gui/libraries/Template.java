package com.corionis.els.gui.libraries;

import com.corionis.els.Context;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

public class Template
{
    String content = "";
    Context context;

    private Template()
    {
    }

    public Template(Context context)
    {
        this.context = context;
    }

    private void defaultInitial(String format)
    {
        content = "";
        if (format.equalsIgnoreCase("html"))
            content = "<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">\n";

        content += "This email is for gathering some basic information needed to setup access to ELS.\n";

        if (format.equalsIgnoreCase("html"))
            content += "</body></html>";

        content += "\n";
    }

    private void defaultInvite(String format)
    {
        content = "";
        if (format.equalsIgnoreCase("html"))
            content = "<html>\n  <head>\n    <style type=\"text/css\">\n      <!-- body {font-family: Arial, Helvetica, sans-serif; font-size: 100%;} -->\n    </style>\n  </head>\n<body>\n";

//        content += "This an invitation to access ELS. The needed data files are attached.\n";

        if (format.equalsIgnoreCase("html"))
            content += "</body>\n</html>";

        content += "\n";
    }

    private void defaultUpdate(String format)
    {
        content = "";
        if (format.equalsIgnoreCase("html"))
            content = "<!DOCTYPE html><html><body style=\"font-family: Arial, Helvetica, sans-serif;font-size: 100%;\">\n";

        content += "This an <b>update</b> of the data files used to access ELS.\n";

        if (format.equalsIgnoreCase("html"))
            content += "</body></html>";

        content += "\n";
    }

    public String getContent(String type, String format, boolean reset) throws Exception
    {
        switch (type)
        {
            case "Initial":
                if (reset || !read(type, format))
                    defaultInitial(format);
                break;
            case "Invite":
                if (reset || !read(type, format))
                    defaultInvite(format);
                break;
            case "Update":
                if (reset || !read(type, format))
                    defaultUpdate(format);
                break;
        }
        content = stripBreaks();
        return content;
    }

    private String getFilename(String type, String format)
    {
        String filename = type;
        if (format.equalsIgnoreCase("html"))
            filename += ".html";
        else
            filename += ".txt";
        filename = getFullPath(filename);
        return filename;
    }

    private String getFullPath(String filename)
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "local" + System.getProperty("file.separator") + "Templates";
        path += System.getProperty("file.separator") + filename;
        return path;
    }

    private boolean read(String type, String format) throws Exception
    {
        boolean exists = false;
        String filename = getFilename(type, format);
        File file = new File(filename);
        if (file.exists())
        {
            exists = true;
            content = "";
            List<String> lines = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
            for (String line : lines)
            {
                content += line + "\n";
            }
        }
        return exists;
    }

    public void remove(String type, String format)
    {
        String filename = getFilename(type, format);
        File file = new File(filename);
        if (file.exists())
            file.delete();
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    private String stripBreaks()
    {
        String cleaned = "";
        int pos = 0;
        while (pos < content.length())
        {
            int i = content.indexOf("<br", pos);
            if (i == -1)
                break;
            cleaned += content.substring(pos, i);
            while (content.charAt(i) != '>')
                ++i;
            pos = i + 1;
        }
        if (pos < content.length())
            cleaned += content.substring(pos);

        return cleaned;
    }

    public void write(String type, String format) throws Exception
    {
        String filename = getFilename(type, format);
        File file = new File(filename);
        Files.write(file.toPath(), content.getBytes(Charset.forName("UTF-8")));
    }

}
