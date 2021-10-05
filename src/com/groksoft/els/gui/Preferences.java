package com.groksoft.els.gui;

public class Preferences
{
    private boolean sortCaseInsensitive = true;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;

    public boolean initialize()
    {
        return true;
    }

    public boolean isSortCaseInsensitive()
    {
        return sortCaseInsensitive;
    }

    public boolean isSortFoldersBeforeFiles()
    {
        return sortFoldersBeforeFiles;
    }

    public boolean isSortReverse()
    {
        return sortReverse;
    }

    public void setSortCaseInsensitive(boolean sortCaseInsensitive)
    {
        this.sortCaseInsensitive = sortCaseInsensitive;
    }

    public void setSortFoldersBeforeFiles(boolean sortFoldersBeforeFiles)
    {
        this.sortFoldersBeforeFiles = sortFoldersBeforeFiles;
    }

    public void setSortReverse(boolean sortReverse)
    {
        this.sortReverse = sortReverse;
    }
}
