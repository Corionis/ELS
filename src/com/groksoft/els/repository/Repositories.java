package com.groksoft.els.repository;

import com.groksoft.els.Configuration;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;

public class Repositories
{
    private boolean initialized = false;
    private ArrayList<Meta> list = null;

    public Repositories()
    {
    }

    public Meta find(String key)
    {
        Meta meta = null;
        if (list != null)
        {
            for (Meta m : list)
            {
                if (m.key.equals(key))
                {
                    meta = m;
                    break;
                }
            }
        }
        return meta;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                "libraries";
        return path;
    }

    public ArrayList<Meta> getList()
    {
        return list;
    }

    public ArrayList<Meta> loadList(Configuration cfg) throws Exception
    {
        File libDir = new File(getDirectoryPath());
        if (libDir.exists() && libDir.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(libDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    Repository repo = new Repository(cfg, 0);
                    repo.read(entry.getPath());

                    Meta meta = new Meta();
                    meta.description = repo.getLibraryData().libraries.description;
                    meta.key = repo.getLibraryData().libraries.key;
                    meta.path = entry.getPath();
                    if (list == null)
                        list = new ArrayList<Meta>();
                    list.add(meta);
                }
            }
        }
        return list;
    }

    public class Meta
    {
        public String description;
        public String key;
        public String path;

        public String toString()
        {
            return description;
        }
    }
}
