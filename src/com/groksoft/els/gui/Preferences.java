package com.groksoft.els.gui;

public class Preferences
{
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss aa";
    // The Look 'n Feel, 0-6
    // 0=System default look 'n feel, use for Windows,
    // 1=MetalLookAndFeel, 2=NimbusLookAndFeel, 3=FlatLightLaf,
    // 4=FlatDarkLaf, 5=FlatIntelliJLaf, 6=FlatDarculaLaf (default)
    private int lafStyle = 6;  // 0-6, see getLookAndFeel(),
    private boolean showBookmarksInTree = true;
    private boolean sortCaseInsensitive = true;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;

    public String getDateFormat()
    {
        return dateFormat;
    }

    public int getLafStyle()
    {
        return lafStyle;
    }

    public boolean initialize()
    {
        return true;
    }

    public boolean isShowBookmarksInTree()
    {
        return showBookmarksInTree;
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

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setLafStyle(int lafStyle)
    {
        this.lafStyle = lafStyle;
    }

    public void setShowBookmarksInTree(boolean showBookmarksInTree)
    {
        this.showBookmarksInTree = showBookmarksInTree;
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
