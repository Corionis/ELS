package com.groksoft.els.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.groksoft.els.Context;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings(value = "unchecked")
public class Preferences implements Serializable
{
    private String accentColor = "2675BF";
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
    private int collectionOneSortColumn = 1;
    private int collectionOneSortDirection = 0;
    private int collectionTwoDateWidth = 80;
    private int collectionTwoDividerLocation = 150;
    private int collectionTwoNameWidth = 128;
    private int collectionTwoSizeWidth = 80;
    private int collectionTwoSortColumn = 1;
    private int collectionTwoSortDirection = 0;
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss aa";
    private boolean generateLongOptions = false;
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    private int jobsHeight = 470;
    private int jobsOriginDividerLocation = 142;
    private int jobsTaskDividerLocation = 142;
    private int jobsWidth = 570;
    private int jobsXpos = -1;
    private int jobsYpos = -1;
    private String lastHintKeysOpenFile = "";
    private String lastHintKeysOpenPath = "";
    private boolean lastHintTrackingIsRemote = false;
    private String lastHintTrackingOpenFile = "";
    private String lastHintTrackingOpenPath = "";
    private boolean lastIsRemote = false;
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
    private int operationDividerBottomSize = 143;
    private int operationDividerConfigLocation = 142;
    private int operationDividerLocation = 500;
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
    private int systemOneSortColumn = 1;
    private int systemOneSortDirection = 0;
    private int systemTwoDateWidth = 80;
    private int systemTwoDividerLocation = 152;
    private int systemTwoNameWidth = 128;
    private int systemTwoSizeWidth = 80;
    private int systemTwoSortColumn = 1;
    private int systemTwoSortDirection = 0;
    private int tabPlacement = JTabbedPane.LEFT;
    private int toolsDuplicateFinderHeight = 470;
    private int toolsDuplicateFinderWidth = 570;
    private int toolsDuplicateFinderXpos = -1;
    private int toolsDuplicateFinderYpos = -1;
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
    private transient Context context;

    /**
     * Constructor
     */
    public Preferences(Context context)
    {
        this.context = context;
        this.context.preferences = this;
    }

    public void extractColumnSizes(Context context, JTable table)
    {
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (context.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                collectionOneNameWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).getWidth();
                collectionOneSizeWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).getWidth();
                collectionOneDateWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (context.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                collectionTwoNameWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).getWidth();
                collectionTwoSizeWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).getWidth();
                collectionTwoDateWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (context.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                systemOneNameWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).getWidth();
                systemOneSizeWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).getWidth();
                systemOneDateWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).getWidth();
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (context.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                systemTwoNameWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).getWidth();
                systemTwoSizeWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).getWidth();
                systemTwoDateWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).getWidth();
            }
        }
    }

    public void fixApplication(Context context)
    {
        // set position and size
        if (context.preferences.getAppXpos() > -1)
            context.mainFrame.setLocation(context.preferences.getAppXpos(), context.preferences.getAppYpos());
        if (context.preferences.getAppWidth() > -1)
            context.mainFrame.setSize(context.preferences.getAppWidth(), context.preferences.getAppHeight());

        // dividers
        // the bottom divider is handler elsewhere
        context.mainFrame.splitPaneTwoBrowsers.setOrientation(context.preferences.getCenterDividerOrientation());
        context.mainFrame.splitPaneTwoBrowsers.setDividerLocation(context.preferences.getCenterDividerLocation());
        context.mainFrame.splitPaneCollectionOne.setDividerLocation(context.preferences.getCollectionOneDividerLocation());
        context.mainFrame.splitPaneCollectionTwo.setDividerLocation(context.preferences.getCollectionTwoDividerLocation());
        context.mainFrame.splitPaneSystemOne.setDividerLocation(context.preferences.getSystemOneDividerLocation());
        context.mainFrame.splitPaneSystemTwo.setDividerLocation(context.preferences.getSystemTwoDividerLocation());

        fixColumnSizes(context, null);
    }

    /**
     * Fix (set) the position of the Browser bottom divider
     *
     * @param context
     * @param bottomSize If < 0 use the bottomSize from Preferences
     */
    public void fixBrowserDivider(Context context, int bottomSize)
    {
        if (bottomSize < 0)
            bottomSize = context.preferences.getBrowserBottomSize();

        int whole = context.mainFrame.splitPaneBrowser.getHeight();
        int divider = context.mainFrame.splitPaneBrowser.getDividerSize();
        int pos = whole - divider - bottomSize;
        context.mainFrame.splitPaneBrowser.setDividerLocation(pos);
    }

    public void fixColumnSizes(Context context, JTable table)
    {
        // column sizes
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            if (context.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneCollectionOne.setDividerLocation(context.preferences.getCollectionOneDividerLocation());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setWidth(context.preferences.getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setWidth(context.preferences.getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setPreferredWidth(context.preferences.getCollectionOneDateWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setWidth(context.preferences.getCollectionOneDateWidth());
                setTableSort(context.mainFrame.tableCollectionOne, getCollectionOneSortColumn(), getCollectionOneSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (context.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneCollectionTwo.setDividerLocation(context.preferences.getCollectionTwoDividerLocation());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setWidth(context.preferences.getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setWidth(context.preferences.getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setPreferredWidth(context.preferences.getCollectionTwoDateWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setWidth(context.preferences.getCollectionTwoDateWidth());
                setTableSort(context.mainFrame.tableCollectionTwo, getCollectionTwoSortColumn(), getCollectionTwoSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (context.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneSystemOne.setDividerLocation(context.preferences.getSystemOneDividerLocation());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setWidth(context.preferences.getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setWidth(context.preferences.getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setPreferredWidth(context.preferences.getSystemOneDateWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setWidth(context.preferences.getSystemOneDateWidth());
                setTableSort(context.mainFrame.tableSystemOne, getSystemOneSortColumn(), getSystemOneSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (context.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneSystemTwo.setDividerLocation(context.preferences.getSystemTwoDividerLocation());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setWidth(context.preferences.getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setWidth(context.preferences.getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setPreferredWidth(context.preferences.getSystemTwoDateWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setWidth(context.preferences.getSystemTwoDateWidth());
                setTableSort(context.mainFrame.tableSystemTwo, getSystemTwoSortColumn(), getSystemTwoSortDirection());
            }
        }
    }

    /**
     * Fix (set) the position of the Operations bottom divider
     *
     * @param context
     * @param bottomSize If < 0 use the bottomSize from Preferences
     */
    public void fixOperationsDivider(Context context, int bottomSize)
    {
        if (bottomSize < 0)
            bottomSize = context.preferences.getOperationDividerBottomSize();

        int whole = context.mainFrame.splitPaneOperation.getHeight();
        int divider = context.mainFrame.splitPaneOperation.getDividerSize();
        int pos = whole - divider - bottomSize;
        context.mainFrame.splitPaneOperation.setDividerLocation(pos);
    }

    public String getAccentColor()
    {
        return accentColor;
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

    public int getCollectionOneSortColumn()
    {
        return collectionOneSortColumn;
    }

    public int getCollectionOneSortDirection()
    {
        return collectionOneSortDirection;
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

    public int getCollectionTwoSortColumn()
    {
        return collectionTwoSortColumn;
    }

    public int getCollectionTwoSortDirection()
    {
        return collectionTwoSortDirection;
    }

    public String getDateFormat()
    {
        return dateFormat;
    }

    public String getFullPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") + "local" + System.getProperty("file.separator") +
                "preferences.json";
        return path;
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

    public String getLastHintTrackingOpenFile()
    {
        return lastHintTrackingOpenFile;
    }

    public String getLastHintTrackingOpenPath()
    {
        return lastHintTrackingOpenPath;
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

    public int getOperationDividerBottomSize()
    {
        return operationDividerBottomSize;
    }

    public int getOperationDividerConfigLocation()
    {
        return operationDividerConfigLocation;
    }

    public int getOperationDividerLocation()
    {
        return operationDividerLocation;
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

    public int getSystemOneSortColumn()
    {
        return systemOneSortColumn;
    }

    public int getSystemOneSortDirection()
    {
        return systemOneSortDirection;
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

    public int getSystemTwoSortColumn()
    {
        return systemTwoSortColumn;
    }

    public int getSystemTwoSortDirection()
    {
        return systemTwoSortDirection;
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

    public SortMeta getTableSort(JTable table)
    {
        SortMeta sortMeta = null;
        List<? extends RowSorter.SortKey> rowSorter = table.getRowSorter().getSortKeys();
        Iterator<? extends RowSorter.SortKey> it = rowSorter.iterator();
        while (it.hasNext())
        {
            RowSorter.SortKey sortKey = it.next();
            if (sortKey.getSortOrder().compareTo(SortOrder.UNSORTED) != 0)
            {
                int direction = 0;
                if (sortKey.getSortOrder().compareTo(SortOrder.ASCENDING) == 0)
                    direction = 1;
                else if (sortKey.getSortOrder().compareTo(SortOrder.DESCENDING) == 0)
                    direction = -1;
                sortMeta = new SortMeta(sortKey.getColumn(), direction);
                break;
            }
        }
        if (sortMeta == null)
            sortMeta = new SortMeta(1, 0);
        return sortMeta;
    }

    public int getToolsDuplicateFinderHeight()
    {
        return toolsDuplicateFinderHeight;
    }

    public int getToolsDuplicateFinderWidth()
    {
        return toolsDuplicateFinderWidth;
    }

    public int getToolsDuplicateFinderXpos()
    {
        return toolsDuplicateFinderXpos;
    }

    public int getToolsDuplicateFinderYpos()
    {
        return toolsDuplicateFinderYpos;
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

    public boolean isGenerateLongOptions()
    {
        return generateLongOptions;
    }

    public boolean isHideFilesInTree()
    {
        return hideFilesInTree;
    }

    public boolean isHideHiddenFiles()
    {
        return hideHiddenFiles;
    }

    public boolean isLastHintTrackingIsRemote()
    {
        return lastHintTrackingIsRemote;
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

    public void setAccentColor(String accentColor)
    {
        this.accentColor = accentColor;
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

    public void setContext(Context context)
    {
        this.context = context;
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

    public void setCollectionOneSortColumn(int collectionOneSortColumn)
    {
        this.collectionOneSortColumn = collectionOneSortColumn;
    }

    public void setCollectionOneSortDirection(int collectionOneSortDirection)
    {
        this.collectionOneSortDirection = collectionOneSortDirection;
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

    public void setCollectionTwoSortColumn(int collectionTwoSortColumn)
    {
        this.collectionTwoSortColumn = collectionTwoSortColumn;
    }

    public void setCollectionTwoSortDirection(int collectionTwoSortDirection)
    {
        this.collectionTwoSortDirection = collectionTwoSortDirection;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setGenerateLongOptions(boolean generateLongOptions)
    {
        this.generateLongOptions = generateLongOptions;
    }

    public void setHideFilesInTree(boolean hideFilesInTree)
    {
        this.hideFilesInTree = hideFilesInTree;
    }

    public void setHideHiddenFiles(boolean hideHiddenFiles)
    {
        this.hideHiddenFiles = hideHiddenFiles;
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

    public void setLastHintTrackingIsRemote(boolean lastHintTrackingIsRemote)
    {
        this.lastHintTrackingIsRemote = lastHintTrackingIsRemote;
    }

    public void setLastHintTrackingOpenFile(String lastHintTrackingOpenFile)
    {
        this.lastHintTrackingOpenFile = lastHintTrackingOpenFile;
    }

    public void setLastHintTrackingOpenPath(String lastHintTrackingOpenPath)
    {
        this.lastHintTrackingOpenPath = lastHintTrackingOpenPath;
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

    public void setOperationDividerBottomSize(int operationDividerBottomSize)
    {
        this.operationDividerBottomSize = operationDividerBottomSize;
    }

    public void setOperationDividerConfigLocation(int operationDividerConfigLocation)
    {
        this.operationDividerConfigLocation = operationDividerConfigLocation;
    }

    public void setOperationDividerLocation(int operationDividerLocation)
    {
        this.operationDividerLocation = operationDividerLocation;
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

    public void setSystemOneSortColumn(int systemOneSortColumn)
    {
        this.systemOneSortColumn = systemOneSortColumn;
    }

    public void setSystemOneSortDirection(int systemOneSortDirection)
    {
        this.systemOneSortDirection = systemOneSortDirection;
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

    public void setSystemTwoSortColumn(int systemTwoSortColumn)
    {
        this.systemTwoSortColumn = systemTwoSortColumn;
    }

    public void setSystemTwoSortDirection(int systemTwoSortDirection)
    {
        this.systemTwoSortDirection = systemTwoSortDirection;
    }

    public void setTabPlacement(int tabPlacement)
    {
        this.tabPlacement = tabPlacement;
    }

    public void setTableSort(JTable table, int column, int direction)
    {
        DefaultRowSorter rowSorter = (DefaultRowSorter) table.getRowSorter();
        SortOrder so = (direction == 1) ? SortOrder.ASCENDING : (direction == -1) ? SortOrder.DESCENDING : SortOrder.UNSORTED;
        RowSorter.SortKey sortKey = new RowSorter.SortKey(column, so);
        ArrayList<RowSorter.SortKey> list = new ArrayList<>();
        list.add(sortKey);
        rowSorter.setSortKeys(list);
    }

    public void setToolsDuplicateFinderHeight(int toolsDuplicateFinderHeight)
    {
        this.toolsDuplicateFinderHeight = toolsDuplicateFinderHeight;
    }

    public void setToolsDuplicateFinderWidth(int toolsDuplicateFinderWidth)
    {
        this.toolsDuplicateFinderWidth = toolsDuplicateFinderWidth;
    }

    public void setToolsDuplicateFinderXpos(int toolsDuplicateFinderXpos)
    {
        this.toolsDuplicateFinderXpos = toolsDuplicateFinderXpos;
    }

    public void setToolsDuplicateFinderYpos(int toolsDuplicateFinderYpos)
    {
        this.toolsDuplicateFinderYpos = toolsDuplicateFinderYpos;
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

    public void write(Context context) throws Exception
    {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // size & position
        appWidth = context.mainFrame.getWidth();
        appHeight = context.mainFrame.getHeight();
        Point location = context.mainFrame.getLocation();
        appXpos = location.x;
        appYpos = location.y;

        // dividers
//        int whole = context.form.splitPaneBrowser.getHeight();
//        int divider = context.form.splitPaneBrowser.getDividerSize();
//        int pos = whole - divider - context.preferences.getBrowserBottomSize();
//        browserBottomSize = pos;
        centerDividerOrientation = context.mainFrame.splitPaneTwoBrowsers.getOrientation();
        centerDividerLocation = context.mainFrame.splitPaneTwoBrowsers.getDividerLocation();

        SortMeta sortMeta;
        collectionOneDividerLocation = context.mainFrame.splitPaneCollectionOne.getDividerLocation();
        sortMeta = getTableSort(context.mainFrame.tableCollectionOne);
        collectionOneSortColumn = sortMeta.column;
        collectionOneSortDirection = sortMeta.direction;

        systemOneDividerLocation = context.mainFrame.splitPaneSystemOne.getDividerLocation();
        sortMeta = getTableSort(context.mainFrame.tableSystemOne);
        systemOneSortColumn = sortMeta.column;
        systemOneSortDirection = sortMeta.direction;

        collectionTwoDividerLocation = context.mainFrame.splitPaneCollectionTwo.getDividerLocation();
        sortMeta = getTableSort(context.mainFrame.tableCollectionTwo);
        collectionTwoSortColumn = sortMeta.column;
        collectionTwoSortDirection = sortMeta.direction;

        systemTwoDividerLocation = context.mainFrame.splitPaneSystemTwo.getDividerLocation();
        sortMeta = getTableSort(context.mainFrame.tableSystemTwo);
        systemTwoSortColumn = sortMeta.column;
        systemTwoSortDirection = sortMeta.direction;

        // all columns
        extractColumnSizes(context, null);

        // other panels
        context.operationsUI.savePreferences();

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

    // ================================================================================================================

    private class SortMeta
    {
        int column = -1;
        int direction = 0;

        public SortMeta(int column, int direction)
        {
            this.column = column;
            this.direction = direction;
        }
    }

}
