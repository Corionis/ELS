package com.groksoft.els.repository;

import com.groksoft.els.Context;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")
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

    public int indexOf(String key)
    {
        int index = -1;
        if (list != null)
        {
            for (int i = 0; i < list.size(); ++i)
            {
                Meta m = list.get(i);
                if (m.key.equals(key))
                {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public ArrayList<Meta> loadList(Context context) throws Exception
    {
        File libDir = new File(getDirectoryPath());
        if (libDir.exists() && libDir.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(libDir, false);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    Repository repo = new Repository(context, 0);
                    repo.read(entry.getPath(), false);

                    Meta meta = new Meta();
                    meta.description = repo.getLibraryData().libraries.description;
                    meta.key = repo.getLibraryData().libraries.key;
                    meta.path = entry.getPath();
                    if (list == null)
                        list = new ArrayList<Meta>();
                    list.add(meta);
                }
            }
            Collections.sort(list);
        }
        return list;
    }

    public class Meta implements Comparable
    {
        public String description;
        public String key;
        public String path;

        @Override
        public int compareTo(Object o)
        {
            return this.description.compareTo(((Meta)o).description);
        }

        @Override
        public String toString()
        {
            return description;
        }
    }
}
