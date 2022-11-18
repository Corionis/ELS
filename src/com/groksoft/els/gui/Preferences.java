package com.groksoft.els.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Configuration;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class Preferences implements Serializable
{
    private int appHeight = 640;
    private int appWidth = 1024;
    private int appXpos = -1;
    private int appYpos = -1;
    private boolean autoRefresh = true;
    private boolean binaryScale = true; // true = 1024, false = 1000
    private int browserBottomSize = 143;
    private int centerDividerLocation = 512;
    private int centerDividerOrientation = 1;
    private int collectionOneDateWidth = 80;
    private int collectionOneDividerLocation = 150;
    private int collectionOneNameWidth = 128;
    private int collectionOneSizeWidth = 80;
    private int collectionTwoDateWidth = 80;
    private int collectionTwoDividerLocation = 150;
    private int collectionTwoNameWidth = 128;
    private int collectionTwoSizeWidth = 80;
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss aa";
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    private String hintTrackingColor = "336633";
    private int jobsHeight = 470;
    private int jobsOriginDividerLocation = 142;
    private int jobsTaskDividerLocation = 142;
    private int jobsWidth = 570;
    private int jobsXpos = -1;
    private int jobsYpos = -1;
    private String lastHintKeysOpenFile = "";
    private String lastHintKeysOpenPath = "";
    private boolean lastIsRemote = true;
    private boolean lastIsWorkstation = false;
    private String lastPublisherOpenFile = "";
    private String lastPublisherOpenPath = "";
    private String lastSubscriberOpenFile = "";
    private String lastSubscriberOpenPath = "";
    private String locale = "";
    // The Look 'n Feel, 0-6
    // 0=System default look 'n feel - use for Windows,
    // 1=MetalLookAndFeel, 2=NimbusLookAndFeel, 3=FlatLightLaf,
    // 4=FlatDarkLaf, 5=FlatIntelliJLaf, 6=FlatDarculaLaf (default)
    private int lookAndFeel = 6;
    private boolean preserveFileTimes = true;
    private int progressHeight = -1;
    private int progressWidth = -1;
    private int progressXpos = -1;
    private int progressYpos = -1;
    private boolean showCcpConfirmation = true;
    private boolean showDeleteConfirmation = true;
    private boolean showDnDConfirmation = true;
    private boolean showTouchConfirmation = true;
    private boolean sortCaseInsensitive = true;
    private boolean sortFoldersBeforeFiles = true;
    private boolean sortReverse = false;
    private int systemOneDateWidth = 80;
    private int systemOneDividerLocation = 152;
    private int systemOneNameWidth = 128;
    private int systemOneSizeWidth = 80;
    private int systemTwoDateWidth = 80;
    private int systemTwoDividerLocation = 152;
    private int systemTwoNameWidth = 128;
    private int systemTwoSizeWidth = 80;
    private int tabPlacement = JTabbedPane.LEFT;
    private int toolsEmptyDirectoryFinderHeight = 470;
    private int toolsEmptyDirectoryFinderWidth = 570;
    private int toolsEmptyDirectoryFinderXpos = -1;
    private int toolsEmptyDirectoryFinderYpos = -1;
    private int toolsJunkRemoverDividerLocation = 142;
    private int toolsJunkRemoverHeight = 470;
    private int toolsJunkRemoverWidth = 570;
    private int toolsJunkRemoverXpos = -1;
    private int toolsJunkRemoverYpos = -1;
    private int toolsRenamerDividerLocation = 142;
    private int toolsRenamerHeight = 470;
    private int toolsRenamerWidth = 570;
    private int toolsRenamerXpos = -1;
    private int toolsRenamerYpos = -1;
    private transient Configuration cfg;
    private transient Context context;
    /**
     * Constructor
     */
    public Preferences(Configuration config, Context ctx)
    {
        cfg = config;
        context = ctx;
    }

    public void extractColumnSizes(GuiContext guiContext, JTable table)
    {
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                collectionOneNameWidth = guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).getWidth();
                collectionOneSizeWidth = guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).getWidth();
                collectionOneDateWidth = guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                collectionTwoNameWidth = guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).getWidth();
                collectionTwoSizeWidth = guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).getWidth();
                collectionTwoDateWidth = guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (guiContext.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                systemOneNameWidth = guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(1).getWidth();
                systemOneSizeWidth = guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(2).getWidth();
                systemOneDateWidth = guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                systemTwoNameWidth = guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).getWidth();
                systemTwoSizeWidth = guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).getWidth();
                systemTwoDateWidth = guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).getWidth();
            }
        }
    }

    public void fixApplication(GuiContext guiContext)
    {
        // set position and size
        if (guiContext.preferences.getAppXpos() > -1)
            guiContext.mainFrame.setLocation(guiContext.preferences.getAppXpos(), guiContext.preferences.getAppYpos());
        if (guiContext.preferences.getAppWidth() > -1)
            guiContext.mainFrame.setSize(guiContext.preferences.getAppWidth(), guiContext.preferences.getAppHeight());

        // dividers
        // the bottom divider is handler elsewhere
        guiContext.mainFrame.splitPaneTwoBrowsers.setOrientation(guiContext.preferences.getCenterDividerOrientation());
        guiContext.mainFrame.splitPaneTwoBrowsers.setDividerLocation(guiContext.preferences.getCenterDividerLocation());
        guiContext.mainFrame.splitPaneCollectionOne.setDividerLocation(guiContext.preferences.getCollectionOneDividerLocation());
        guiContext.mainFrame.splitPaneCollectionTwo.setDividerLocation(guiContext.preferences.getCollectionTwoDividerLocation());
        guiContext.mainFrame.splitPaneSystemOne.setDividerLocation(guiContext.preferences.getSystemOneDividerLocation());
        guiContext.mainFrame.splitPaneSystemTwo.setDividerLocation(guiContext.preferences.getSystemTwoDividerLocation());

        fixColumnSizes(guiContext, null);
    }

    /**
     * Fix (set) the position of the Browser bottom divider
     *
     * @param guiContext
     * @param bottomSize If < 0 use the bottomSize from Preferences
     */
    public void fixBrowserDivider(GuiContext guiContext, int bottomSize)
    {
        if (bottomSize < 0)
            bottomSize = guiContext.preferences.getBrowserBottomSize();

        int whole = guiContext.mainFrame.splitPaneBrowser.getHeight();
        int divider = guiContext.mainFrame.splitPaneBrowser.getDividerSize();
        int pos = whole - divider - bottomSize;
        guiContext.mainFrame.splitPaneBrowser.setDividerLocation(pos);
    }

    public void fixColumnSizes(GuiContext guiContext, JTable table)
    {
        // column sizes
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                guiContext.mainFrame.splitPaneCollectionOne.setDividerLocation(guiContext.preferences.getCollectionOneDividerLocation());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getCollectionOneNameWidth());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getCollectionOneNameWidth());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getCollectionOneSizeWidth());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getCollectionOneSizeWidth());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getCollectionOneDateWidth());
                guiContext.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getCollectionOneDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                guiContext.mainFrame.splitPaneCollectionTwo.setDividerLocation(guiContext.preferences.getCollectionTwoDividerLocation());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getCollectionTwoNameWidth());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getCollectionTwoNameWidth());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getCollectionTwoSizeWidth());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getCollectionTwoSizeWidth());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getCollectionTwoDateWidth());
                guiContext.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getCollectionTwoDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (guiContext.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                guiContext.mainFrame.splitPaneSystemOne.setDividerLocation(guiContext.preferences.getSystemOneDividerLocation());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getSystemOneNameWidth());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getSystemOneNameWidth());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getSystemOneSizeWidth());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getSystemOneSizeWidth());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getSystemOneDateWidth());
                guiContext.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getSystemOneDateWidth());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                guiContext.mainFrame.splitPaneSystemTwo.setDividerLocation(guiContext.preferences.getSystemTwoDividerLocation());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setPreferredWidth(guiContext.preferences.getSystemTwoNameWidth());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setWidth(guiContext.preferences.getSystemTwoNameWidth());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setPreferredWidth(guiContext.preferences.getSystemTwoSizeWidth());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setWidth(guiContext.preferences.getSystemTwoSizeWidth());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setPreferredWidth(guiContext.preferences.getSystemTwoDateWidth());
                guiContext.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setWidth(guiContext.preferences.getSystemTwoDateWidth());
            }
        }
    }

    public int getAppHeight()
    {
        return appHeight;
    }

    public int getAppWidth()
    {
        return appWidth;
    }

    public int getAppXpos()
    {
        return appXpos;
    }

    public int getAppYpos()
    {
        return appYpos;
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

    public String getFullPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                "preferences.json";
        return path;
    }

    public String getHintTrackingColor()
    {
        return hintTrackingColor;
    }

    public int getJobsHeight()
    {
        return jobsHeight;
    }

    public int getJobsOriginDividerLocation()
    {
        return jobsOriginDividerLocation;
    }

    public int getJobsTaskDividerLocation()
    {
        return jobsTaskDividerLocation;
    }

    public int getJobsWidth()
    {
        return jobsWidth;
    }

    public int getJobsXpos()
    {
        return jobsXpos;
    }

    public int getJobsYpos()
    {
        return jobsYpos;
    }

    public String getLastHintKeysOpenFile()
    {
        return lastHintKeysOpenFile;
    }

    public String getLastHintKeysOpenPath()
    {
        return lastHintKeysOpenPath;
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

    public String getLocale()
    {
        if (locale.length() == 0)
            locale = context.main.currentFilePart;
        return locale;
    }

    public int getLookAndFeel()
    {
        return lookAndFeel;
    }

    public int getProgressHeight()
    {
        return progressHeight;
    }

    public int getProgressWidth()
    {
        return progressWidth;
    }

    public int getProgressXpos()
    {
        return progressXpos;
    }

    public int getProgressYpos()
    {
        return progressYpos;
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

    public int getTabPlacement()
    {
        return getTabPlacement(tabPlacement);
    }

    public int getTabPlacement(int tabPlacementIndex)
    {
        int place;
        switch (tabPlacementIndex)
        {
            case 0:
                place = JTabbedPane.TOP;
                break;
            case 1:
                place = JTabbedPane.BOTTOM;
                break;
            default:
            case 2:
                place = JTabbedPane.LEFT;
                break;
            case 3:
                place = JTabbedPane.RIGHT;
                break;
        }
        return place;
    }

    public int getTabPlacementIndex()
    {
        return tabPlacement;
    }

    public int getToolsEmptyDirectoryFinderHeight()
    {
        return toolsEmptyDirectoryFinderHeight;
    }

    public int getToolsEmptyDirectoryFinderWidth()
    {
        return toolsEmptyDirectoryFinderWidth;
    }

    public int getToolsEmptyDirectoryFinderXpos()
    {
        return toolsEmptyDirectoryFinderXpos;
    }

    public int getToolsEmptyDirectoryFinderYpos()
    {
        return toolsEmptyDirectoryFinderYpos;
    }

    public int getToolsJunkRemoverDividerLocation()
    {
        return toolsJunkRemoverDividerLocation;
    }

    public int getToolsJunkRemoverHeight()
    {
        return toolsJunkRemoverHeight;
    }

    public int getToolsJunkRemoverWidth()
    {
        return toolsJunkRemoverWidth;
    }

    public int getToolsJunkRemoverXpos()
    {
        return toolsJunkRemoverXpos;
    }

    public int getToolsJunkRemoverYpos()
    {
        return toolsJunkRemoverYpos;
    }

    public int getToolsRenamerDividerLocation()
    {
        return toolsRenamerDividerLocation;
    }

    public int getToolsRenamerHeight()
    {
        return toolsRenamerHeight;
    }

    public int getToolsRenamerWidth()
    {
        return toolsRenamerWidth;
    }

    public int getToolsRenamerXpos()
    {
        return toolsRenamerXpos;
    }

    public int getToolsRenamerYpos()
    {
        return toolsRenamerYpos;
    }

    public boolean isAutoRefresh()
    {
        return autoRefresh;
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

    public boolean isLastIsWorkstation()
    {
        return lastIsWorkstation;
    }

    public boolean isPreserveFileTimes()
    {
        return preserveFileTimes;
    }

    public boolean isShowCcpConfirmation()
    {
        return showCcpConfirmation;
    }

    public boolean isShowDeleteConfirmation()
    {
        return showDeleteConfirmation;
    }

    public boolean isShowDnDConfirmation()
    {
        return showDnDConfirmation;
    }

    public boolean isShowTouchConfirmation()
    {
        return showTouchConfirmation;
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

    public void setAppHeight(int appHeight)
    {
        this.appHeight = appHeight;
    }

    public void setAppWidth(int appWidth)
    {
        this.appWidth = appWidth;
    }

    public void setAppXpos(int appXpos)
    {
        this.appXpos = appXpos;
    }

    public void setAppYpos(int appYpos)
    {
        this.appYpos = appYpos;
    }

    public void setAutoRefresh(boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
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

    public void setCfg(Configuration cfg)
    {
        this.cfg = cfg;
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

    public void setJobsHeight(int jobsHeight)
    {
        this.jobsHeight = jobsHeight;
    }

    public void setJobsOriginDividerLocation(int jobsOriginDividerLocation)
    {
        this.jobsOriginDividerLocation = jobsOriginDividerLocation;
    }

    public void setJobsTaskDividerLocation(int jobsTaskDividerLocation)
    {
        this.jobsTaskDividerLocation = jobsTaskDividerLocation;
    }

    public void setJobsWidth(int jobsWidth)
    {
        this.jobsWidth = jobsWidth;
    }

    public void setJobsXpos(int jobsXpos)
    {
        this.jobsXpos = jobsXpos;
    }

    public void setJobsYpos(int jobsYpos)
    {
        this.jobsYpos = jobsYpos;
    }

    public void setLastHintKeysOpenFile(String lastHintKeysOpenFile)
    {
        this.lastHintKeysOpenFile = lastHintKeysOpenFile;
    }

    public void setLastHintKeysOpenPath(String lastHintKeysOpenPath)
    {
        this.lastHintKeysOpenPath = lastHintKeysOpenPath;
    }

    public void setLastIsRemote(boolean lastIsRemote)
    {
        this.lastIsRemote = lastIsRemote;
    }

    public void setLastIsWorkstation(boolean lastIsWorkstation)
    {
        this.lastIsWorkstation = lastIsWorkstation;
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

    public void setLocale(String locale)
    {
        this.locale = locale;
    }

    public void setLookAndFeel(int lookAndFeel)
    {
        this.lookAndFeel = lookAndFeel;
    }

    public void setPreserveFileTimes(boolean preserveFileTimes)
    {
        this.preserveFileTimes = preserveFileTimes;
    }

    public void setProgressHeight(int progressHeight)
    {
        this.progressHeight = progressHeight;
    }

    public void setProgressWidth(int progressWidth)
    {
        this.progressWidth = progressWidth;
    }

    public void setProgressXpos(int progressXpos)
    {
        this.progressXpos = progressXpos;
    }

    public void setProgressYpos(int progressYpos)
    {
        this.progressYpos = progressYpos;
    }

    public void setShowCcpConfirmation(boolean showCcpConfirmation)
    {
        this.showCcpConfirmation = showCcpConfirmation;
    }

    public void setShowDeleteConfirmation(boolean showDeleteConfirmation)
    {
        this.showDeleteConfirmation = showDeleteConfirmation;
    }

    public void setShowDnDConfirmation(boolean showDnDConfirmation)
    {
        this.showDnDConfirmation = showDnDConfirmation;
    }

    public void setShowTouchConfirmation(boolean showTouchConfirmation)
    {
        this.showTouchConfirmation = showTouchConfirmation;
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

    public void setTabPlacement(int tabPlacement)
    {
        this.tabPlacement = tabPlacement;
    }

    public void setToolsEmptyDirectoryFinderHeight(int toolsEmptyDirectoryFinderHeight)
    {
        this.toolsEmptyDirectoryFinderHeight = toolsEmptyDirectoryFinderHeight;
    }

    public void setToolsEmptyDirectoryFinderWidth(int toolsEmptyDirectoryFinderWidth)
    {
        this.toolsEmptyDirectoryFinderWidth = toolsEmptyDirectoryFinderWidth;
    }

    public void setToolsEmptyDirectoryFinderXpos(int toolsEmptyDirectoryFinderXpos)
    {
        this.toolsEmptyDirectoryFinderXpos = toolsEmptyDirectoryFinderXpos;
    }

    public void setToolsEmptyDirectoryFinderYpos(int toolsEmptyDirectoryFinderYpos)
    {
        this.toolsEmptyDirectoryFinderYpos = toolsEmptyDirectoryFinderYpos;
    }

    public void setToolsJunkRemoverDividerLocation(int toolsJunkRemoverDividerLocation)
    {
        this.toolsJunkRemoverDividerLocation = toolsJunkRemoverDividerLocation;
    }

    public void setToolsJunkRemoverHeight(int toolsJunkRemoverHeight)
    {
        this.toolsJunkRemoverHeight = toolsJunkRemoverHeight;
    }

    public void setToolsJunkRemoverWidth(int toolsJunkRemoverWidth)
    {
        this.toolsJunkRemoverWidth = toolsJunkRemoverWidth;
    }

    public void setToolsJunkRemoverXpos(int toolsJunkRemoverXpos)
    {
        this.toolsJunkRemoverXpos = toolsJunkRemoverXpos;
    }

    public void setToolsJunkRemoverYpos(int toolsJunkRemoverYpos)
    {
        this.toolsJunkRemoverYpos = toolsJunkRemoverYpos;
    }

    public void setToolsRenamerDividerLocation(int toolsRenamerDividerLocation)
    {
        this.toolsRenamerDividerLocation = toolsRenamerDividerLocation;
    }

    public void setToolsRenamerHeight(int toolsRenamerHeight)
    {
        this.toolsRenamerHeight = toolsRenamerHeight;
    }

    public void setToolsRenamerWidth(int toolsRenamerWidth)
    {
        this.toolsRenamerWidth = toolsRenamerWidth;
    }

    public void setToolsRenamerXpos(int toolsRenamerXpos)
    {
        this.toolsRenamerXpos = toolsRenamerXpos;
    }

    public void setToolsRenamerYpos(int toolsRenamerYpos)
    {
        this.toolsRenamerYpos = toolsRenamerYpos;
    }

    public void write(GuiContext guiContext) throws Exception
    {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // size & position
        appWidth = guiContext.mainFrame.getWidth();
        appHeight = guiContext.mainFrame.getHeight();
        Point location = guiContext.mainFrame.getLocation();
        appXpos = location.x;
        appYpos = location.y;

        // dividers
//        int whole = guiContext.form.splitPaneBrowser.getHeight();
//        int divider = guiContext.form.splitPaneBrowser.getDividerSize();
//        int pos = whole - divider - guiContext.preferences.getBrowserBottomSize();
//        browserBottomSize = pos;
        centerDividerOrientation = guiContext.mainFrame.splitPaneTwoBrowsers.getOrientation();
        centerDividerLocation = guiContext.mainFrame.splitPaneTwoBrowsers.getDividerLocation();
        collectionOneDividerLocation = guiContext.mainFrame.splitPaneCollectionOne.getDividerLocation();
        systemOneDividerLocation = guiContext.mainFrame.splitPaneSystemOne.getDividerLocation();
        collectionTwoDividerLocation = guiContext.mainFrame.splitPaneCollectionTwo.getDividerLocation();
        systemTwoDividerLocation = guiContext.mainFrame.splitPaneSystemTwo.getDividerLocation();

        // all columns
        extractColumnSizes(guiContext, null);

        json = gson.toJson(this);
        try
        {
            File f = new File(getFullPath());
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath());
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException("Error writing: " + getFullPath() + " trace: " + Utils.getStackTrace(fnf));
        }
    }

}
