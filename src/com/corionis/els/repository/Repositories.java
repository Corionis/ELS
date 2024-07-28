package com.corionis.els.repository;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

@SuppressWarnings(value = "unchecked")

public class Repositories
{
    private ArrayList<RepoMeta> metaList = null;
    private ArrayList<Repository> repoList = null;
    private Logger logger = LogManager.getLogger("applog");

    public Repositories()
    {
    }

    private void addRepo(Context context, String path)
    {
        Repository found = findRepoPath(path);
        if (found == null)
        {
            try
            {
                Repository repo = new Repository(context, 0);
                if (repo.read(path, "a", false))
                {
                    repoList.add(repo);
                    RepoMeta repoMeta = new RepoMeta();
                    repoMeta.description = repo.getLibraryData().libraries.description;
                    repoMeta.key = repo.getLibraryData().libraries.key;
                    repoMeta.path = repo.getJsonFilename();
                    metaList.add(repoMeta);
                }
            }
            catch (Exception e)
            {
                logger.error(context.cfg.gs("Z.exception") + Utils.getStackTrace(e));
                if (context.mainFrame != null)
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.error.reading") + path, context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public Repository addRepo(Context context, String path, int purpose)
    {
        Repository repo = null;
        if (path != null && !path.isEmpty())
        {
            try
            {
                repo = new Repository(context, purpose);
                if (repo.read(path, "a", false))
                {
                    repoList.add(repo);
                    RepoMeta repoMeta = new RepoMeta();
                    repoMeta.description = repo.getLibraryData().libraries.description;
                    repoMeta.key = repo.getLibraryData().libraries.key;
                    repoMeta.path = repo.getJsonFilename();
                    metaList.add(repoMeta);
                }
            }
            catch (Exception e)
            {
                logger.error(context.cfg.gs("Z.exception") + Utils.getStackTrace(e));
                if (context.mainFrame != null)
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.error.reading") + path, context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                repo = null;
            }
        }
        return repo;
    }

    private RepoMeta findMeta(String key)
    {
        RepoMeta repoMeta = null;
        if (key != null && !key.isEmpty())
        {
            if (metaList != null)
            {
                for (RepoMeta m : metaList)
                {
                    if (m.key.equals(key))
                    {
                        repoMeta = m;
                        break;
                    }
                }
            }
        }
        return repoMeta;
    }

    public RepoMeta findMetaAdd(Context context, String key, String path, int purpose)
    {
        RepoMeta repoMeta = null;

        if (key != null && !key.isEmpty())
        {
            repoMeta = findMeta(key);
            if (repoMeta == null && !path.startsWith(getDirectoryPath()))
            {
                if (path != null && !path.isEmpty())
                {
                    addRepo(context, path, purpose);
                    repoMeta = findMeta(key);
                }
            }
        }
        return repoMeta;
    }

    public RepoMeta findMetaPath(String path)
    {
        RepoMeta repoMeta = null;
        if (path != null && !path.isEmpty())
        {
            if (metaList != null)
            {
                for (RepoMeta m : metaList)
                {
                    if (m.path.endsWith(path))
                    {
                        repoMeta = m;
                        break;
                    }
                }
            }
        }
        return repoMeta;
    }

/*
    public Repository findRepo(String key)
    {
        Repository repo = null;
        if (key != null && !key.isEmpty())
        {
            if (repoList != null)
            {
                for (Repository r : repoList)
                {
                    if (r.getLibraryData().libraries.key.equals(key))
                    {
                        repo = r;
                        break;
                    }
                }
            }
        }
        return repo;
    }
*/

/*
    public Repository findRepoDescription(String description)
    {
        Repository repo = null;
        if (description != null && !description.isEmpty())
        {
            if (description.endsWith(" *"))
                description = description.substring(0, description.length() - 2);
            if (repoList != null)
            {
                for (Repository r : repoList)
                {
                    if (r.getLibraryData().libraries.description.equals(description))
                    {
                        repo = r;
                        break;
                    }
                }
            }
        }
        return repo;
    }
*/

    public Repository findRepoPath(String path)
    {
        Repository repo = null;
        if (path.length() > 0)
        {
            for (int i = 0; i < repoList.size(); ++i)
            {
                if (repoList.get(i).getJsonFilename().endsWith(path))
                {
                    repo = repoList.get(i);
                    break;
                }
            }
        }
        return repo;
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "libraries";
        return path;
    }

    public ArrayList<RepoMeta> getMetaList()
    {
        return metaList;
    }

    public ArrayList<Repository> getRepoList()
    {
        return repoList;
    }

    public ArrayList<RepoMeta> loadList(Context context)
    {
        boolean dynamicPublisher = false;
        boolean dynamicSubscriber = false;
        boolean dynamicHints = false;
        Repository repo;

        if (context.publisherRepo != null && context.publisherRepo.getLibraryData().libraries.key != null)
            dynamicPublisher = context.publisherRepo.isDynamic();

        if (context.subscriberRepo != null && context.subscriberRepo.getLibraryData().libraries.key != null)
            dynamicSubscriber = context.subscriberRepo.isDynamic();

        if (context.hintsRepo != null && context.hintsRepo.getLibraryData().libraries.key != null)
            dynamicHints = context.hintsRepo.isDynamic();


        repoList = new ArrayList<Repository>();
        metaList = new ArrayList<RepoMeta>();

        // load from Libraries directory
        File libDir = new File(getDirectoryPath());
        if (libDir.exists() && libDir.isDirectory())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(libDir, true);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                        addRepo(context, entry.getAbsolutePath());
                }
            }

            // add publisher if not in Libraries directory
            if (context.publisherRepo != null && context.publisherRepo.getLibraryData().libraries.key != null)
            {
                RepoMeta meta = findMetaPath(context.publisherRepo.getJsonFilename());
                if (meta == null && !context.publisherRepo.getJsonFilename().startsWith(getDirectoryPath()))
                {
                    repo = addRepo(context, context.publisherRepo.getJsonFilename(), Repository.PUBLISHER);
                    repo.setDynamic(dynamicPublisher);
                }
            }

            // add subscriber if not in Libraries directory
            if (context.subscriberRepo != null && context.subscriberRepo.getLibraryData().libraries.key != null)
            {
                RepoMeta meta = findMetaPath(context.subscriberRepo.getJsonFilename());
                if (meta == null && !context.subscriberRepo.getJsonFilename().startsWith(getDirectoryPath()))
                {
                    repo = addRepo(context, context.subscriberRepo.getJsonFilename(), Repository.SUBSCRIBER);
                    repo.setDynamic(dynamicSubscriber);
                }
            }

            // add Hint Server if not in Libraries directory
            if (context.hintsRepo != null && context.hintsRepo.getLibraryData().libraries.key != null)
            {
                RepoMeta meta = findMetaPath(context.hintsRepo.getJsonFilename());
                if (meta == null && !context.hintsRepo.getJsonFilename().startsWith(getDirectoryPath()))
                {
                    repo = addRepo(context, context.hintsRepo.getJsonFilename(), Repository.HINT_SERVER);
                    repo.setDynamic(dynamicHints);
                }
            }

            if (repoList.size() > 0)
            {
                Collections.sort(repoList);
                Collections.sort(metaList);
            }
        }
        return metaList;
    }

}
