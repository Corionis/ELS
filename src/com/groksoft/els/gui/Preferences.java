package com.groksoft.els.gui;

public class Preferences
{
    // 0=System default look 'n feel, use for Windows,
    // 1=MetalLookAndFeel, 2=NimbusLookAndFeel, 3=FlatLightLaf,
    // 4=FlatDarkLaf, 5=FlatIntelliJLaf, 6=FlatDarculaLaf (default)
    private int lafStyle = 6;  // 0-6, see getLookAndFeel(),
    private boolean sortCaseInsensitive = true;
    private boolean sortFoldersBeforeFiles = false;
    private boolean sortReverse = false;

    public int getLafStyle()
    {
        return lafStyle;
    }

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

    public void setLafStyle(int lafStyle)
    {
        this.lafStyle = lafStyle;
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
