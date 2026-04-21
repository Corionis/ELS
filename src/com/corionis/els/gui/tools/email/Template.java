package com.corionis.els.gui.tools.email;

import com.corionis.els.Context;
import com.corionis.els.repository.Repository;
import org.apache.sshd.common.util.io.IoUtils;

import java.io.File;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Template
{
    private boolean buildIn = false;
    private boolean changed = false;
    private String content = "";
    private Context context;
    private boolean custom = false;
    private String fileName;
    private String format;
    private Repository repo = null;

    private Template()
    {
        // hide default constructor
    }

    public Template(Context context, String fileName, String format, boolean buildIn)
    {
        this.context = context;
        this.fileName = fileName;
        this.format = format;
        this.buildIn = buildIn;
    }

    public Template(Context context, String fileName, String format, boolean buildIn, Repository repo)
    {
        this.context = context;
        this.fileName = fileName;
        this.format = format;
        this.buildIn = buildIn;
        this.repo = repo;
    }

    public Template clone()
    {
        Template clone = new Template(this.context, this.fileName, this.format, this.buildIn, this.repo);
        clone.content = this.content;
        clone.custom = this.custom;
        return clone;
    }

    public void defaultEmail(String name)
    {
        content = "";
        try
        {
            URL url = Thread.currentThread().getContextClassLoader().getResource(name);
            List<String> lines = IoUtils.readAllLines(url);
            for (int i = 0; i < lines.size(); ++i)
            {
                content += lines.get(i) + "\n";
            }
        }
        catch (Exception e)
        {
            content = name + context.cfg.gs("Z.not.found");
        }
    }

    /**
     * Get the list of built-in templates
     * @return list
     */
    public ArrayList<String> getBuiltIns()
    {
        ArrayList<String> list = new ArrayList<>();
        String locale = context.preferences.getLocale();
        list.add("Begin-Basic_" + locale + ".html");
        list.add("Begin-Basic_" + locale + ".txt");
        list.add("Begin-Advanced_" + locale + ".html");
        list.add("Begin-Advanced_" + locale + ".txt");
        list.add("Begin-Administrator_" + locale + ".html");
        list.add("Begin-Administrator_" + locale + ".txt");
        list.add("Invite-Basic_" + locale + ".html");
        list.add("Invite-Basic_" + locale + ".txt");
        list.add("Invite-Advanced_" + locale + ".html");
        list.add("Invite-Advanced_" + locale + ".txt");
        list.add("Invite-Administrator_" + locale + ".html");
        list.add("Invite-Administrator_" + locale + ".txt");
        list.add("Update_" + locale + ".html");
        list.add("Update_" + locale + ".txt");
        return list;
    }

    /**
     * Get available content from template file or default if a file does not exist
     * @param name Filename
     * @return text
     * @throws Exception I/O exception
     */
    public String getContent(String name) throws Exception
    {
        if (name == null || name.isEmpty())
            name = fileName;

        if (!isChanged())
        {
            if (!read(name))
                defaultEmail(name);
        }
        content = stripBreaks();
        return content;
    }

    /**
     * Get default content for this template
     * @return text
     */
    public String getDefault()
    {
        defaultEmail(fileName);
        content = stripBreaks();
        return content;
    }

    public String getFileName()
    {
        return fileName;
    }

    public String getFormat()
    {
        return format;
    }

    /**
     * Get formatted text
     * <p>
     * Reformats the raw text that include newlines and inserts a <br/> to match.
     * Substitutes any embedded variables if values are available.
     *
     * @param raw String of unformatted text
     * @return Formatted text
     */
    public String getFormattedText(String raw, Repository repo)
    {
        String text = "";

        // if using HTML
        int i = raw.indexOf("<body");
        if (format.equalsIgnoreCase("html") && i >= 0)
        {
            // copy everything up to the end of the BODY tag
            text = raw.substring(0, i);
            for ( ; i < raw.length() ; ++i)
            {
                text += raw.charAt(i);
                if (raw.charAt(i) == '>')
                {
                    ++i;
                    text += "\n";
                    if (raw.charAt(i) == '\n')
                        ++i;
                    break;
                }
            }

            // find the end BODY tag
            int k = raw.indexOf("</body>");

            for ( ; i < k; ++i)
            {
                if (raw.charAt(i) == '\n')
                {
                    text += "<br/>\n"; // insert break
                }
                else
                {
                    text += raw.charAt(i);
                }
            }

            for ( ; i < raw.length(); ++i)
            {
                text += raw.charAt(i);
            }
        }
        else
            text = raw;
        text = substituteVariables(text, repo);
        return text;
    }

    public String getFullPath(String filename)
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "local" + System.getProperty("file.separator") + "Templates";
        path += System.getProperty("file.separator") + filename;
        return path;
    }

    public boolean isBuildIn()
    {
        return buildIn;
    }

    public boolean isChanged()
    {
        return changed;
    }

    public boolean isCustom()
    {
        return custom;
    }

    private boolean read(String name) throws Exception
    {
        boolean exists = false;
        String filename = getFullPath(name);
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
            custom = true;
        }
        return exists;
    }

    public void remove()
    {
        String filename = getFullPath(fileName);
        File file = new File(filename);
        if (file.exists())
            file.delete();
        content = "";
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public void setContext(Context context)
    {
        this.context = context;
    }

    public void setChanged()
    {
        this.changed = true;
    }

    public void setChanged(boolean sense)
    {
        this.changed = sense;
    }

    public void setFileName(String fileName)
    {
        this.fileName = fileName;
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

    /**
     * Substitute embedded variables
     * <p>
     * Supported variables, case-insensitive:
     * <li>${publisherHost} : Current Publisher host</li>
     * <li>${publisherName} : Current Publisher Collection name</li>
     * <li>${publisherUser} : Current Publisher User name</li>
     * <li>${userName} : User name</li>
     * <li>${userType} : Type of user, Basic, Advanced or Administrator</li>
     *
     * @param text The content to be formatted
     * @param repo Repository of Subscriber for User, if null context.subscriberRepo is used
     * @return Expanded content
     */
    private String substituteVariables(String text, Repository repo)
    {
        if (context.publisherRepo != null)
        {
            text = text.replaceAll("\\$\\{(?i)publisherrepo}", context.publisherRepo.getLibraries().description);
            text = text.replaceAll("\\$\\{(?i)publishername}", context.publisherRepo.getUser().getName());
            text = text.replaceAll("\\$\\{(?i)publisherhost}", context.publisherRepo.getLibraries().host);
        }

        if (repo == null)
        {
            if (this.repo != null)
                repo = this.repo;
            else
                repo = context.subscriberRepo;
        }
        if (repo != null)
        {
            if (repo.getUser() != null)
            {
                text = text.replaceAll("\\$\\{(?i)userrepo}", repo.getLibraries().description);
                text = text.replaceAll("\\$\\{(?i)username}", repo.getUser().getName());
                String repl = repo.getUser().getTypeString();
                text = text.replaceAll("\\$\\{(?i)usertype}", repl);
            }
        }

        return text;
    }

    @Override
    public String toString()
    {
        return fileName;
    }

    public void write() throws Exception
    {
        String name = getFullPath(fileName);
        File file = new File(name);
        Files.write(file.toPath(), content.getBytes(Charset.forName("UTF-8")));
    }

}
