package com.corionis.els.repository;

public class RepoMeta implements Comparable
{
    public String description;
    public String key;
    public String path;

    @Override
    public RepoMeta clone()
    {
        RepoMeta repoMeta = new RepoMeta();
        repoMeta.description = this.description;
        repoMeta.key = this.key;
        repoMeta.path = this.path;
        return repoMeta;
    }

    @Override
    public int compareTo(Object o)
    {
        return this.description.compareTo(((RepoMeta) o).description);
    }

    @Override
    public String toString()
    {
        return description;
    }
}
