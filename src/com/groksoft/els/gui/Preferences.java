package com.groksoft.els.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;

public class Preferences implements Serializable
{
    private boolean binaryScale = true; // true = 1024, false = 1000
    private int browserBottomSize = 143; // tracked value
    private int centerDividerLocation = 512; // tracked value
    private int centerDividerOrientation = 1; // tracked value
    private int collectionOneDateWidth = 80; // tracked value
    private int collectionOneDividerLocation = 150; // tracked value
    private int collectionOneNameWidth = 128; // tracked value
    private int collectionOneSizeWidth = 80; // tracked value
    private int collectionTwoDateWidth = 80; // tracked value
    private int collectionTwoDividerLocation = 150; // tracked value
    private int collectionTwoNameWidth = 128; // tracked value
    private int collectionTwoSizeWidth = 80; // tracked value
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss aa";
    private int height = 640; // tracked value
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    private String hintTrackingColor = "336633";
    // The Look 'n Feel, 0-6
    // 0=System default look 'n feel - use for Windows,
    // 1=MetalLookAndFeel, 2=NimbusLookAndFeel, 3=FlatLightLaf,
    // 4=FlatDarkLaf, 5=FlatIntelliJLaf, 6=FlatDarculaLaf (default)
    private int lafStyle = 6;
    private boolean lastIsRemote = true;
    private String lastPublisherOpenFile = "";
    private String lastPublisherOpenPath = "";
    private String lastSubscriberOpenFile = "";
    private String lastSubscriberOpenPath = "";
    private boolean preserveFileTimes = true;
    private boolean showConfirmations = true; // show confirmation dialogs for copy, move, delete, etc.
    private boolean showHintTrackingButton = true;
    private boolean sortCaseInsensitive = false;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;
    private int systemOneDateWidth = 80; // tracked value
    private int systemOneDividerLocation = 152; // tracked value
    private int systemOneNameWidth = 128; // tracked value
    private int systemOneSizeWidth = 80; // tracked value
    private int systemTwoDateWidth = 80; // tracked value
    private int systemTwoDividerLocation = 152; // tracked value
    private int systemTwoNameWidth = 128; // tracked value
    private int systemTwoSizeWidth = 80; // tracked value
    private int width = 1024; // tracked value
    private int xpos = -1; // tracked value
    private int ypos = -1; // tracked value
    /**
     * Constructor
     */
    public Preferences()
    {
        // Ideas for a Settings tool.
        // Tracked values are not preferences/settings.
        //
        // General
        //      Preserve file time stamps
        //      Restore previous session
        //      Show confirmation dialogs
        //      Show Hint Tracking button
        // Appearance
        //      Look 'n Feel
        //      Binary/decimal scale
        //      Date format
        //      Hint Tracking button color
        // Browser
        //      Hide files in tree
        //      Hide hidden files
        //      Sort case insensitive
        //      Sort folders before files
        //      Sort reverse
        // Backup
        // Libraries
    }

    public void export(GuiContext guiContext) throws Exception
    {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // size & position
        width = guiContext.form.getWidth();
        height = guiContext.form.getHeight();
        Point location = guiContext.form.getLocation();
        xpos = location.x;
        ypos = location.y;

        // dividers
        centerDividerOrientation = guiContext.form.splitPaneTwoBrowsers.getOrientation();
        centerDividerLocation = guiContext.form.splitPaneTwoBrowsers.getDividerLocation();
        collectionOneDividerLocation = guiContext.form.splitPaneCollectionOne.getDividerLocation();
        systemOneDividerLocation = guiContext.form.splitPaneSystemOne.getDividerLocation();
        collectionTwoDividerLocation = guiContext.form.splitPaneCollectionTwo.getDividerLocation();
        systemTwoDividerLocation = guiContext.form.splitPaneSystemTwo.getDividerLocation();

        // all columns
        extractColumnSizes(guiContext, null);

        json = gson.toJson(this);
        try
        {
            PrintWriter outputStream = new PrintWriter(getFilename());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException("Exception while writing file " + getFilename() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

    public void extractColumnSizes(GuiContext guiContext, JTable table)
    {
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (guiContext.form.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                collectionOneNameWidth = guiContext.form.tableCollectionOne.getColumnModel().getColumn(1).getWidth();
                collectionOneSizeWidth = guiContext.form.tableCollectionOne.getColumnModel().getColumn(2).getWidth();
                collectionOneDateWidth = guiContext.form.tableCollectionOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (guiContext.form.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                collectionTwoNameWidth = guiContext.form.tableCollectionTwo.getColumnModel().getColumn(1).getWidth();
                collectionTwoSizeWidth = guiContext.form.tableCollectionTwo.getColumnModel().getColumn(2).getWidth();
                collectionTwoDateWidth = guiContext.form.tableCollectionTwo.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (guiContext.form.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                systemOneNameWidth = guiContext.form.tableSystemOne.getColumnModel().getColumn(1).getWidth();
                systemOneSizeWidth = guiContext.form.tableSystemOne.getColumnModel().getColumn(2).getWidth();
                systemOneDateWidth = guiContext.form.tableSystemOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (guiContext.form.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                systemTwoNameWidth = guiContext.form.tableSystemTwo.getColumnModel().getColumn(1).getWidth();
                systemTwoSizeWidth = guiContext.form.tableSystemTwo.getColumnModel().getColumn(2).getWidth();
                systemTwoDateWidth = guiContext.form.tableSystemTwo.getColumnModel().getColumn(3).getWidth();
            }
        }
    }

    public void fixApplication(GuiContext guiContext)
    {
        // set position and size
        if (guiContext.preferences.getXpos() > -1)
            guiContext.form.setLocation(guiContext.preferences.getXpos(), guiContext.preferences.getYpos());
        if (guiContext.preferences.getWidth() > -1)
            guiContext.form.setSize(guiContext.preferences.getWidth(), guiContext.preferences.getHeight());

        // dividers
        // the bottom divider is handler elsewhere
        guiContext.form.splitPaneTwoBrowsers.setOrientation(guiContext.preferences.getCenterDividerOrientation());
        guiContext.form.splitPaneTwoBrowsers.setDividerLocation(guiContext.preferences.getCenterDividerLocation());
        guiContext.form.splitPaneCollectionOne.setDividerLocation(guiContext.preferences.getCollectionOneDividerLocation());
        guiContext.form.splitPaneCollectionTwo.setDividerLocation(guiContext.preferences.getCollectionTwoDividerLocation());
        guiContext.form.splitPaneSystemOne.setDividerLocation(guiContext.preferences.getSystemOneDividerLocation());
        guiContext.form.splitPaneSystemTwo.setDividerLocation(guiContext.preferences.getSystemTwoDividerLocation());

        fixColumnSizes(guiContext, null);
    }

    public void fixColumnSizes(GuiContext guiContext, JTable table)
    {
        // column sizes
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (guiContext.form.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                guiContext.form.splitPaneCollectionOne.setDividerLocation(guiContext.preferences.getCollectionOneDividerLocation());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getCollectionOneNameWidth());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getCollectionOneNameWidth());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getCollectionOneSizeWidth());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getCollectionOneSizeWidth());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getCollectionOneDateWidth());
                guiContext.form.tableCollectionOne.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getCollectionOneDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (guiContext.form.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                guiContext.form.splitPaneCollectionTwo.setDividerLocation(guiContext.preferences.getCollectionTwoDividerLocation());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getCollectionTwoNameWidth());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getCollectionTwoNameWidth());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getCollectionTwoSizeWidth());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getCollectionTwoSizeWidth());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getCollectionTwoDateWidth());
                guiContext.form.tableCollectionTwo.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getCollectionTwoDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (guiContext.form.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                guiContext.form.splitPaneSystemOne.setDividerLocation(guiContext.preferences.getSystemOneDividerLocation());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getSystemOneNameWidth());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getSystemOneNameWidth());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getSystemOneSizeWidth());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getSystemOneSizeWidth());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getSystemOneDateWidth());
                guiContext.form.tableSystemOne.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getSystemOneDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (guiContext.form.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                guiContext.form.splitPaneSystemTwo.setDividerLocation(guiContext.preferences.getSystemTwoDividerLocation());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getSystemTwoNameWidth());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getSystemTwoNameWidth());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getSystemTwoSizeWidth());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getSystemTwoSizeWidth());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getSystemTwoDateWidth());
                guiContext.form.tableSystemTwo.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getSystemTwoDateWidth());
            }
        }
    }

    public int getBrowserBottomSize()
    {
        return browserBottomSize;
    }

    public int getCenterDividerLocation()
    {
        return centerDividerLocation;
    }

    public int getCenterDividerOrientation()
    {
        return centerDividerOrientation;
    }

    public int getCollectionOneDateWidth()
    {
        return collectionOneDateWidth;
    }

    public int getCollectionOneDividerLocation()
    {
        return collectionOneDividerLocation;
    }

    public int getCollectionOneNameWidth()
    {
        return collectionOneNameWidth;
    }

    public int getCollectionOneSizeWidth()
    {
        return collectionOneSizeWidth;
    }

    public int getCollectionTwoDateWidth()
    {
        return collectionTwoDateWidth;
    }

    public int getCollectionTwoDividerLocation()
    {
        return collectionTwoDividerLocation;
    }

    public int getCollectionTwoNameWidth()
    {
        return collectionTwoNameWidth;
    }

    public int getCollectionTwoSizeWidth()
    {
        return collectionTwoSizeWidth;
    }

    public String getDateFormat()
    {
        return dateFormat;
    }

    public String getFilename()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") + ".els-settings.json";
        return path;
    }

    public int getHeight()
    {
        return height;
    }

    public String getHintTrackingColor()
    {
        return hintTrackingColor;
    }

    public int getLafStyle()
    {
        return lafStyle;
    }

    public String getLastPublisherOpenFile()
    {
        return lastPublisherOpenFile;
    }

    public String getLastPublisherOpenPath()
    {
        return lastPublisherOpenPath;
    }

    public String getLastSubscriberOpenFile()
    {
        return lastSubscriberOpenFile;
    }

    public String getLastSubscriberOpenPath()
    {
        return lastSubscriberOpenPath;
    }

    public int getSystemOneDateWidth()
    {
        return systemOneDateWidth;
    }

    public int getSystemOneDividerLocation()
    {
        return systemOneDividerLocation;
    }

    public int getSystemOneNameWidth()
    {
        return systemOneNameWidth;
    }

    public int getSystemOneSizeWidth()
    {
        return systemOneSizeWidth;
    }

    public int getSystemTwoDateWidth()
    {
        return systemTwoDateWidth;
    }

    public int getSystemTwoDividerLocation()
    {
        return systemTwoDividerLocation;
    }

    public int getSystemTwoNameWidth()
    {
        return systemTwoNameWidth;
    }

    public int getSystemTwoSizeWidth()
    {
        return systemTwoSizeWidth;
    }

    public int getWidth()
    {
        return width;
    }

    public int getXpos()
    {
        return xpos;
    }

    public int getYpos()
    {
        return ypos;
    }

    public boolean isBinaryScale()
    {
        return binaryScale;
    }

    public boolean isHideFilesInTree()
    {
        return hideFilesInTree;
    }

    public boolean isHideHiddenFiles()
    {
        return hideHiddenFiles;
    }

    public boolean isLastIsRemote()
    {
        return lastIsRemote;
    }

    public boolean isPreserveFileTimes()
    {
        return preserveFileTimes;
    }

    public boolean isShowConfirmations()
    {
        return showConfirmations;
    }

    public boolean isShowHintTrackingButton()
    {
        return showHintTrackingButton;
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

    public void setCenterDividerLocation(int centerDividerLocation)
    {
        this.centerDividerLocation = centerDividerLocation;
    }

    public void setCenterDividerOrientation(int centerDividerOrientation)
    {
        this.centerDividerOrientation = centerDividerOrientation;
    }

    public void setCollectionOneDateWidth(int collectionOneDateWidth)
    {
        this.collectionOneDateWidth = collectionOneDateWidth;
    }

    public void setCollectionOneDividerLocation(int collectionOneDividerLocation)
    {
        this.collectionOneDividerLocation = collectionOneDividerLocation;
    }

    public void setCollectionOneNameWidth(int collectionOneNameWidth)
    {
        this.collectionOneNameWidth = collectionOneNameWidth;
    }

    public void setCollectionOneSizeWidth(int collectionOneSizeWidth)
    {
        this.collectionOneSizeWidth = collectionOneSizeWidth;
    }

    public void setCollectionTwoDateWidth(int collectionTwoDateWidth)
    {
        this.collectionTwoDateWidth = collectionTwoDateWidth;
    }

    public void setCollectionTwoDividerLocation(int collectionTwoDividerLocation)
    {
        this.collectionTwoDividerLocation = collectionTwoDividerLocation;
    }

    public void setCollectionTwoNameWidth(int collectionTwoNameWidth)
    {
        this.collectionTwoNameWidth = collectionTwoNameWidth;
    }

    public void setCollectionTwoSizeWidth(int collectionTwoSizeWidth)
    {
        this.collectionTwoSizeWidth = collectionTwoSizeWidth;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setHeight(int height)
    {
        this.height = height;
    }

    public void setHideFilesInTree(boolean hideFilesInTree)
    {
        this.hideFilesInTree = hideFilesInTree;
    }

    public void setHideHiddenFiles(boolean hideHiddenFiles)
    {
        this.hideHiddenFiles = hideHiddenFiles;
    }

    public void setHintTrackingColor(String hintTrackingColor)
    {
        this.hintTrackingColor = hintTrackingColor;
    }

    public void setLafStyle(int lafStyle)
    {
        this.lafStyle = lafStyle;
    }

    public void setLastIsRemote(boolean lastIsRemote)
    {
        this.lastIsRemote = lastIsRemote;
    }

    public void setLastPublisherOpenFile(String lastPublisherOpenFile)
    {
        this.lastPublisherOpenFile = lastPublisherOpenFile;
    }

    public void setLastPublisherOpenPath(String lastPublisherOpenPath)
    {
        this.lastPublisherOpenPath = lastPublisherOpenPath;
    }

    public void setLastSubscriberOpenFile(String lastSubscriberOpenFile)
    {
        this.lastSubscriberOpenFile = lastSubscriberOpenFile;
    }

    public void setLastSubscriberOpenPath(String lastSubscriberOpenPath)
    {
        this.lastSubscriberOpenPath = lastSubscriberOpenPath;
    }

    public void setPreserveFileTimes(boolean preserveFileTimes)
    {
        this.preserveFileTimes = preserveFileTimes;
    }

    public void setShowConfirmations(boolean showConfirmations)
    {
        this.showConfirmations = showConfirmations;
    }

    public void setShowHintTrackingButton(boolean showHintTrackingButton)
    {
        this.showHintTrackingButton = showHintTrackingButton;
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

    public void setSystemOneDateWidth(int systemOneDateWidth)
    {
        this.systemOneDateWidth = systemOneDateWidth;
    }

    public void setSystemOneDividerLocation(int systemOneDividerLocation)
    {
        this.systemOneDividerLocation = systemOneDividerLocation;
    }

    public void setSystemOneNameWidth(int systemOneNameWidth)
    {
        this.systemOneNameWidth = systemOneNameWidth;
    }

    public void setSystemOneSizeWidth(int systemOneSizeWidth)
    {
        this.systemOneSizeWidth = systemOneSizeWidth;
    }

    public void setSystemTwoDateWidth(int systemTwoDateWidth)
    {
        this.systemTwoDateWidth = systemTwoDateWidth;
    }

    public void setSystemTwoDividerLocation(int systemTwoDividerLocation)
    {
        this.systemTwoDividerLocation = systemTwoDividerLocation;
    }

    public void setSystemTwoNameWidth(int systemTwoNameWidth)
    {
        this.systemTwoNameWidth = systemTwoNameWidth;
    }

    public void setSystemTwoSizeWidth(int systemTwoSizeWidth)
    {
        this.systemTwoSizeWidth = systemTwoSizeWidth;
    }

    public void setWidth(int width)
    {
        this.width = width;
    }

    public void setXpos(int xpos)
    {
        this.xpos = xpos;
    }

    public void setYpos(int ypos)
    {
        this.ypos = ypos;
    }
}
