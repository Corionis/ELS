package com.groksoft.els.gui;

class FolderColumn implements Comparable
{
    Preferences preferences;
    String name;
    boolean isDir;

    public FolderColumn(String value, boolean dir)
    {
        name = value;
        isDir = dir;
    }

    @Override
    public int compareTo(Object o)
    {
        if (isDir == ((FolderColumn)o).isDir)
        {
            if (Navigator.guiContext.preferences.isSortCaseInsensitive())
            {
                return name.compareToIgnoreCase(((FolderColumn)o).name);
            }
            return name.compareTo(((FolderColumn)o).name);
        }
        if (Navigator.guiContext.preferences.isSortFoldersBeforeFiles())
        {
            return (isDir && !((FolderColumn) o).isDir) ? -1 : 0;
        }
        if (Navigator.guiContext.preferences.isSortCaseInsensitive())
        {
            return name.compareToIgnoreCase(((FolderColumn)o).name);
        }
        return name.compareTo(((FolderColumn)o).name);

    }

    public String toString()
    {
        return name;
    }

}
