package com.groksoft.els.gui;

public class Preferences
{
    private boolean binaryScale = true; // true = 1024, false = 1000
    private int browserBottomSize = 0;
    private boolean confirmation = true; // show confirmation dialogs for copy, move, delete, etc.
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss aa";
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    // The Look 'n Feel, 0-6
    // 0=System default look 'n feel, use for Windows,
    // 1=MetalLookAndFeel, 2=NimbusLookAndFeel, 3=FlatLightLaf,
    // 4=FlatDarkLaf, 5=FlatIntelliJLaf, 6=FlatDarculaLaf (default)
    private int lafStyle = 6;
    private boolean preserveFileTime = true;
    private boolean sortCaseInsensitive = false;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;
    public Preferences()
    {
        // Appearance
        //      Theme
        //      Date format
        //      Binary/decimal scale
        // Browser
        //      Restore previous session
        //      Case sensitive
        //      Folders before files
        //      Sort reverse
        // Backup
        // Profiles
        // Keys
    }

    public int getBrowserBottomSize()
    {
        return browserBottomSize;
    }

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

    public boolean isBinaryScale()
    {
        return binaryScale;
    }

    public boolean isConfirmation()
    {
        return confirmation;
    }

    public boolean isHideFilesInTree()
    {
        return hideFilesInTree;
    }

    public boolean isHideHiddenFiles()
    {
        return hideHiddenFiles;
    }

    public boolean isPreserveFileTime()
    {
        return preserveFileTime;
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

    public void setBinaryScale(boolean binaryScale)
    {
        this.binaryScale = binaryScale;
    }

    public void setBrowserBottomSize(int browserBottomSize)
    {
        this.browserBottomSize = browserBottomSize;
    }

    public void setConfirmation(boolean confirmation)
    {
        this.confirmation = confirmation;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setHideFilesInTree(boolean hideFilesInTree)
    {
        this.hideFilesInTree = hideFilesInTree;
    }

    public void setHideHiddenFiles(boolean hideHiddenFiles)
    {
        this.hideHiddenFiles = hideHiddenFiles;
    }

    public void setLafStyle(int lafStyle)
    {
        this.lafStyle = lafStyle;
    }

    public void setPreserveFileTime(boolean preserveFileTime)
    {
        this.preserveFileTime = preserveFileTime;
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
