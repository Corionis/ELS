package com.corionis.els.repository;

import com.corionis.els.Context;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")

public class Repositories
{
    private boolean initialized = false;
    private ArrayList<RepoMeta> list = null;

    public Repositories()
    {
    }

    public RepoMeta find(String key)
    {
        RepoMeta repoMeta = null;
        if (list != null)
        {
            for (RepoMeta m : list)
            {
                if (m.key.equals(key))
                {
                    repoMeta = m;
                    break;
                }
            }
        }
        return repoMeta;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "libraries";
        return path;
    }

    public ArrayList<RepoMeta> getList()
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
                RepoMeta m = list.get(i);
                if (m.key.equals(key))
                {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public ArrayList<RepoMeta> loadList(Context context) throws Exception
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
                    if (repo.read(entry.getPath(), "a", false))
                    {
                        RepoMeta repoMeta = new RepoMeta();
                        repoMeta.description = repo.getLibraryData().libraries.description;
                        repoMeta.key = repo.getLibraryData().libraries.key;
                        repoMeta.path = entry.getPath();
                        if (list == null)
                            list = new ArrayList<RepoMeta>();
                        list.add(repoMeta);
                    }
                }
            }
            Collections.sort(list);
        }
        return list;
    }

}
