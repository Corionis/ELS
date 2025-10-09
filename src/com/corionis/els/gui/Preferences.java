package com.corionis.els.gui;

import com.corionis.els.gui.browser.BrowserTableCellRenderer;
import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

@SuppressWarnings(value = "unchecked")
public class Preferences implements Serializable
{
    public static final String DEFAULT_ACCENT_COLOR = "2675BF";
    public static final int SCHEMA = 1; // schema version, set in write()
    private String accentColor = DEFAULT_ACCENT_COLOR;
    private int appHeight = 640;
    private int appWidth = 1024;
    private int appXpos = -1;
    private int appYpos = 0;
    private boolean askSendEmail = true;
    private boolean autoRefresh = false;
    private boolean binaryScale = true; // true = 1024, false = 1000
    private int bookmarksHeight = 320;
    private int bookmarksNameWidth = -1;
    private int bookmarksPathWidth = -1;
    private int bookmarksWidth = 440;
    private int bookmarksXpos = -1;
    private int bookmarksYpos = 0;
    private int browserBottomSize = 90;
    private int centerDividerLocation = 188;
    private int centerDividerOrientation = 0;
    private int collectionOneDateWidth = 80;
    private int collectionOneDividerLocation = 150;
    private int collectionOneNameWidth = 128;
    private int collectionOneSizeWidth = 80;
    private int collectionOneSortColumn = 1;
    private int collectionOneSortDirection = 0;
    private int collectionOneTableWidth = -1;
    private int collectionOneTreeWidth = -1;
    private int collectionTwoDateWidth = 80;
    private int collectionTwoDividerLocation = 150;
    private int collectionTwoNameWidth = 128;
    private int collectionTwoSizeWidth = 80;
    private int collectionTwoSortColumn = 1;
    private int collectionTwoSortDirection = 0;
    private int collectionTwoTableWidth = -1;
    private int collectionTwoTreeWidth = -1;
    // https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html
    private String dateFormat = "yyyy-MM-dd hh:mm:ss a";
    private boolean defaultDryrun = true;
    private String defaultEmailServer = "";
    private int directoryPickerXpos = -1;
    private int directoryPickerYpos = 0;
    private int fileEditorHeight = 365;
    private int fileEditorWidth = 425;
    private int fileEditorXpos = -1;
    private int fileEditorYpos = 0;
    private boolean generateLongOptions = false;
    private int helpHeight = 550;
    private int helpWidth = 600;
    private int helpXpos = -1;
    private int helpYpos = 0;
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    private int hintsActionWidth = 60;
    private int hintsByWidth = 46;
    private int hintsDateWidth = 154;
    private int hintsFromItemWidth = 160;
    private int hintsFromLibWidth = 88;
    private int hintsHeight = 520;
    private int hintsStatusWidth = 104;
    private int hintsSystemWidth = 104;
    private int hintsToItemWidth = 160;
    private int hintsToLibWidth = 88;
    private int hintsWidth = 1140;
    private int hintsXpos = -1;
    private int hintsYpos = 0;
    private int jobsHeight = 470;
    private int jobsOriginDividerLocation = 142;
    private int jobsTaskDividerLocation = 142;
    private int jobsWidth = 570;
    private int jobsXpos = -1;
    private int jobsYpos = 0;
    private boolean lastHintKeysIsOpen = false;
    private String lastHintKeysOpenFile = "";
    private String lastHintKeysOpenPath = "";
    private boolean lastHintTrackingIsOpen = false;
    private boolean lastHintTrackingIsRemote = false;
    private String lastHintTrackingOpenFile = "";
    private String lastHintTrackingOpenPath = "";
    private boolean lastOverrideHintHost = false;
    private String lastOverrideSubscriber = "";
    private boolean lastPublisherIsOpen = false;
    private boolean lastPublisherIsWorkstation = false;
    private String lastPublisherOpenFile = "";
    private String lastPublisherOpenPath = "";
    private boolean lastSubscriberIsOpen = false;
    private boolean lastSubscriberIsRemote = false;
    private String lastSubscriberOpenFile = "";
    private String lastSubscriberOpenPath = "";
    private int librariesBiblioDividerLocation = 142;
    private String librariesDefaultMinimum = "40";
    private String librariesDefaultMinimumScale = "GB";
    private int librariesDividerLocation = 142;
    private int librariesLocationColumnWidth = 300;
    private int librariesMinimumSizeColumnWidth = 120;
    private String locale = "";
    private int lookAndFeel = -1; // Look 'n Feel, 0-6, default IntelliJ Dark, aka Darcula
    private boolean macosLauncher = false;
    private boolean preserveFileTimes = true;
    private int progressHeight = -1;
    private int progressWidth = -1;
    private int progressXpos = -1;
    private int progressYpos = 0;
    private int runOption = 0;
    private int schema = 1;
    private boolean showArrows = true;
    private boolean showCcpConfirmation = true;
    private boolean showDeleteConfirmation = true;
    private boolean showDnDConfirmation = true;
    private boolean showGettingStarted = true;
    private boolean showMnemonics = true;
    private boolean showNavigation = true;
    private boolean showToolbar = true;
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
    private int systemOneTableWidth = -1;
    private int systemOneTreeWidth = -1;
    private int systemTwoDateWidth = 80;
    private int systemTwoDividerLocation = 152;
    private int systemTwoNameWidth = 128;
    private int systemTwoSizeWidth = 80;
    private int systemTwoSortColumn = 1;
    private int systemTwoSortDirection = 0;
    private int tabPlacement = 0; // top
    private int toolOperationsDividerConfigLocation = 142;
    private int toolsDuplicateFinderHeight = 470;
    private int toolsDuplicateFinderWidth = 570;
    private int toolsDuplicateFinderXpos = -1;
    private int toolsDuplicateFinderYpos = 0;
    private int toolsEmailDividerLocation = 142;
    private int toolsEmailHeight = 470;
    private int toolsEmailWidth = 570;
    private int toolsEmailXpos = -1;
    private int toolsEmailYpos = 0;
    private int toolsEmptyDirectoryFinderHeight = 470;
    private int toolsEmptyDirectoryFinderWidth = 570;
    private int toolsEmptyDirectoryFinderXpos = -1;
    private int toolsEmptyDirectoryFinderYpos = 0;
    private int toolsJunkRemoverDividerLocation = 142;
    private int toolsJunkRemoverHeight = 470;
    private int toolsJunkRemoverWidth = 570;
    private int toolsJunkRemoverXpos = -1;
    private int toolsJunkRemoverYpos = 0;
    private int toolsOperationsHeight = 470;
    private int toolsOperationsWidth = 570;
    private int toolsOperationsXpos = -1;
    private int toolsOperationsYpos = 0;
    private int toolsRenamerDividerLocation = 142;
    private int toolsRenamerHeight = 470;
    private int toolsRenamerWidth = 570;
    private int toolsRenamerXpos = -1;
    private int toolsRenamerYpos = 0;
    private int toolsSleepDividerLocation = 142;
    private int toolsSleepHeight = 470;
    private int toolsSleepWidth = 570;
    private int toolsSleepXpos = -1;
    private int toolsSleepYpos = 0;
    private boolean tooltipsLargeTables = true;
    private boolean useLastPublisherSubscriber = true;
    private transient Context context;
    private transient boolean lookAndFeelInitialized = false;
    /**
     * Constructor
     */
    public Preferences()
    {
    }

    public void extractPositionsSizes(Context context)
    {
        SortMeta sortMeta;

        // dividers
        centerDividerOrientation = context.mainFrame.splitPaneTwoBrowsers.getOrientation();
        centerDividerLocation = context.mainFrame.splitPaneTwoBrowsers.getDividerLocation();

        context.mainFrame.scrollPaneTreeCollectionOne.getWidth();

        if (context.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
        {
            collectionOneDividerLocation = context.mainFrame.splitPaneCollectionOne.getDividerLocation();
            collectionOneTableWidth = context.mainFrame.scrollPaneTableCollectionOne.getWidth();
            collectionOneTreeWidth = context.mainFrame.scrollPaneTreeCollectionOne.getWidth();

            sortMeta = getTableSort(context.mainFrame.tableCollectionOne);
            collectionOneSortColumn = sortMeta.column;
            collectionOneSortDirection = sortMeta.direction;

            collectionOneNameWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).getWidth();
            collectionOneSizeWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).getWidth();
            collectionOneDateWidth = context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).getWidth();
        }

        if (context.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
        {
            collectionTwoDividerLocation = context.mainFrame.splitPaneCollectionTwo.getDividerLocation();
            collectionTwoTableWidth = context.mainFrame.scrollPaneTableCollectionTwo.getWidth();
            collectionTwoTreeWidth = context.mainFrame.scrollPaneTreeCollectionTwo.getWidth();

            sortMeta = getTableSort(context.mainFrame.tableCollectionTwo);
            collectionTwoSortColumn = sortMeta.column;
            collectionTwoSortDirection = sortMeta.direction;

            collectionTwoNameWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).getWidth();
            collectionTwoSizeWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).getWidth();
            collectionTwoDateWidth = context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).getWidth();
        }

        if (context.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
        {
            systemOneDividerLocation = context.mainFrame.splitPaneSystemOne.getDividerLocation();
            systemOneTableWidth = context.mainFrame.scrollPaneTableSystemOne.getWidth();
            systemOneTreeWidth = context.mainFrame.scrollPaneTreeSystemOne.getWidth();

            sortMeta = getTableSort(context.mainFrame.tableSystemOne);
            systemOneSortColumn = sortMeta.column;
            systemOneSortDirection = sortMeta.direction;

            systemOneNameWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).getWidth();
            systemOneSizeWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).getWidth();
            systemOneDateWidth = context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).getWidth();
        }

        if (context.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
        {
            systemTwoDividerLocation = context.mainFrame.splitPaneSystemTwo.getDividerLocation();
            sortMeta = getTableSort(context.mainFrame.tableSystemTwo);
            systemTwoSortColumn = sortMeta.column;
            systemTwoSortDirection = sortMeta.direction;

            systemTwoNameWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).getWidth();
            systemTwoSizeWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).getWidth();
            systemTwoDateWidth = context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).getWidth();
        }

        librariesLocationColumnWidth = context.mainFrame.tableLocations.getColumnModel().getColumn(0).getWidth();
        librariesMinimumSizeColumnWidth = context.mainFrame.tableLocations.getColumnModel().getColumn(1).getWidth();
    }

    public void fixApplication(Context context)
    {
        // set position and size

        if (Utils.isOffScreen(getAppXpos(), getAppYpos()))
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - getAppWidth() / 2;
            int y = screenSize.height / 2 - getAppHeight() / 2;
            setAppXpos(x);
            setAppYpos(y);
        }

        context.mainFrame.setLocation(getAppXpos(), getAppYpos());
        context.mainFrame.setSize(getAppWidth(), getAppHeight());

        // dividers
        // the bottom divider is handler elsewhere
        context.mainFrame.splitPaneTwoBrowsers.setOrientation(getCenterDividerOrientation());
        context.mainFrame.splitPaneTwoBrowsers.setDividerLocation(getCenterDividerLocation());

        fixColumnSizes(context, null);
    }

    /**
     * Fix (set) the position of the Browser bottom divider
     *
     * @param context    The Context
     * @param bottomSize If < 0 use the bottomSize from Preferences
     */
    public void fixBrowserDivider(Context context, int bottomSize)
    {
        if (bottomSize < 0)
            bottomSize = getBrowserBottomSize();

        int whole = context.mainFrame.splitPaneBrowser.getHeight();
        int divider = context.mainFrame.splitPaneBrowser.getDividerSize();
        int pos = whole - divider - bottomSize;
        context.mainFrame.splitPaneBrowser.setDividerLocation(pos);
    }

    public void fixColumnSizes(Context context, JTable table)
    {
        if (table != null && context.navigator != null && context.mainFrame != null && context.mainFrame.isVisible())
            context.preferences.extractPositionsSizes(context);

        // column sizes
        if (table == null || table.getName().equalsIgnoreCase("tableCollectionOne"))
        {
            context.mainFrame.splitPaneCollectionOne.setDividerLocation(getCollectionOneDividerLocation());
            if (getCollectionOneTreeWidth() > -1) // added 11 April 2025 so will be missing in previous version's data
            {
                context.mainFrame.scrollPaneTreeCollectionOne.setSize(getCollectionOneTreeWidth(), context.mainFrame.scrollPaneTreeCollectionOne.getHeight());
                context.mainFrame.scrollPaneTableCollectionOne.setSize(getCollectionOneTableWidth(), context.mainFrame.scrollPaneTableCollectionOne.getHeight());
            }

            if (context.mainFrame.tableCollectionOne.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(0).setPreferredWidth(22);
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(0).setWidth(22);
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setPreferredWidth(getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setWidth(getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setPreferredWidth(getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setWidth(getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setPreferredWidth(getCollectionOneDateWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setWidth(getCollectionOneDateWidth());
                setTableSort(context.mainFrame.tableCollectionOne, getCollectionOneSortColumn(), getCollectionOneSortDirection());

                BrowserTableCellRenderer btcr = new BrowserTableCellRenderer(context, context.mainFrame.tableCollectionOne);
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setCellRenderer(btcr);
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setCellRenderer(btcr);
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setCellRenderer(btcr);
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            context.mainFrame.splitPaneCollectionTwo.setDividerLocation(getCollectionTwoDividerLocation());
            if (getCollectionTwoTreeWidth() > -1) // added 11 April 2025 so will be missing in previous version's data
            {
                context.mainFrame.scrollPaneTreeCollectionTwo.setSize(getCollectionTwoTreeWidth(), context.mainFrame.scrollPaneTreeCollectionTwo.getHeight());
                context.mainFrame.scrollPaneTableCollectionTwo.setSize(getCollectionTwoTableWidth(), context.mainFrame.scrollPaneTableCollectionTwo.getHeight());
            }

            if (context.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(0).setPreferredWidth(22);
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(0).setWidth(22);
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setPreferredWidth(getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setWidth(getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setPreferredWidth(getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setWidth(getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setPreferredWidth(getCollectionTwoDateWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setWidth(getCollectionTwoDateWidth());
                setTableSort(context.mainFrame.tableCollectionTwo, getCollectionTwoSortColumn(), getCollectionTwoSortDirection());

                BrowserTableCellRenderer btcr = new BrowserTableCellRenderer(context, context.mainFrame.tableCollectionTwo);
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setCellRenderer(btcr);
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setCellRenderer(btcr);
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setCellRenderer(btcr);
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            context.mainFrame.splitPaneSystemOne.setDividerLocation(getSystemOneDividerLocation());
            if (getSystemOneTreeWidth() > -1)
            {
                context.mainFrame.scrollPaneTreeSystemOne.setSize(getSystemOneTreeWidth(), context.mainFrame.scrollPaneTreeSystemOne.getHeight());
                context.mainFrame.scrollPaneTableSystemOne.setSize(getSystemOneTableWidth(), context.mainFrame.scrollPaneTableSystemOne.getHeight());
            }

            if (context.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(0).setPreferredWidth(22);
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(0).setWidth(22);
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setPreferredWidth(getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setWidth(getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setPreferredWidth(getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setWidth(getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setPreferredWidth(getSystemOneDateWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setWidth(getSystemOneDateWidth());
                setTableSort(context.mainFrame.tableSystemOne, getSystemOneSortColumn(), getSystemOneSortDirection());

                BrowserTableCellRenderer btcr = new BrowserTableCellRenderer(context, context.mainFrame.tableSystemOne);
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setCellRenderer(btcr);
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setCellRenderer(btcr);
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setCellRenderer(btcr);
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            context.mainFrame.splitPaneSystemTwo.setDividerLocation(getSystemTwoDividerLocation());
            if (context.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(0).setPreferredWidth(22);
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(0).setWidth(22);
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setPreferredWidth(getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setWidth(getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setPreferredWidth(getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setWidth(getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setPreferredWidth(getSystemTwoDateWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setWidth(getSystemTwoDateWidth());
                setTableSort(context.mainFrame.tableSystemTwo, getSystemTwoSortColumn(), getSystemTwoSortDirection());

                BrowserTableCellRenderer btcr = new BrowserTableCellRenderer(context, context.mainFrame.tableSystemTwo);
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setCellRenderer(btcr);
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setCellRenderer(btcr);
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setCellRenderer(btcr);
            }
        }
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

    public int getBookmarksHeight()
    {
        return bookmarksHeight;
    }

    public int getBookmarksNameWidth()
    {
        return bookmarksNameWidth;
    }

    public int getBookmarksPathWidth()
    {
        return bookmarksPathWidth;
    }

    public int getBookmarksWidth()
    {
        return bookmarksWidth;
    }

    public int getBookmarksXpos()
    {
        return bookmarksXpos;
    }

    public int getBookmarksYpos()
    {
        return bookmarksYpos;
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

    private int getCollectionOneTableWidth()
    {
        return collectionOneTableWidth;
    }

    private int getCollectionOneTreeWidth()
    {
        return collectionOneTreeWidth;
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

    private int getCollectionTwoTableWidth()
    {
        return collectionTwoTableWidth;
    }

    private int getCollectionTwoTreeWidth()
    {
        return collectionTwoTreeWidth;
    }

    public String getDateFormat()
    {
        if (dateFormat != null && !dateFormat.isEmpty())
        {
            if (dateFormat.toLowerCase().endsWith("aa"))
            {
                dateFormat = dateFormat.substring(0, dateFormat.length() - 1);
            }
        }
        return dateFormat;
    }

    public String getDefaultEmailServer()
    {
        if (defaultEmailServer.equalsIgnoreCase("None"))
            return "";
        return defaultEmailServer;
    }

    public int getDirectoryPickerXpos()
    {
        return directoryPickerXpos;
    }

    public int getDirectoryPickerYpos()
    {
        return directoryPickerYpos;
    }

    public int getFileEditorHeight()
    {
        return fileEditorHeight;
    }

    public int getFileEditorWidth()
    {
        return fileEditorWidth;
    }

    public int getFileEditorXpos()
    {
        return fileEditorXpos;
    }

    public int getFileEditorYpos()
    {
        return fileEditorYpos;
    }

    public String getFullPath(String workingDirectory)
    {
        if (workingDirectory == null || workingDirectory.isEmpty())
            workingDirectory = System.getProperty("user.dir");

        return workingDirectory + System.getProperty("file.separator") + "local" +
                System.getProperty("file.separator") + "preferences.json";
    }

    public int getHelpHeight()
    {
        return helpHeight;
    }

    public int getHelpWidth()
    {
        return helpWidth;
    }

    public int getHelpXpos()
    {
        return helpXpos;
    }

    public int getHelpYpos()
    {
        return helpYpos;
    }

    public int getHintsActionWidth()
    {
        return hintsActionWidth;
    }

    public int getHintsByWidth()
    {
        return hintsByWidth;
    }

    public int getHintsDateWidth()
    {
        return hintsDateWidth;
    }

    public int getHintsFromItemWidth()
    {
        return hintsFromItemWidth;
    }

    public int getHintsFromLibWidth()
    {
        return hintsFromLibWidth;
    }

    public int getHintsHeight()
    {
        return hintsHeight;
    }

    public int getHintsStatusWidth()
    {
        return hintsStatusWidth;
    }

    public int getHintsSystemWidth()
    {
        return hintsSystemWidth;
    }

    public int getHintsToItemWidth()
    {
        return hintsToItemWidth;
    }

    public int getHintsToLibWidth()
    {
        return hintsToLibWidth;
    }

    public int getHintsWidth()
    {
        return hintsWidth;
    }

    public int getHintsXpos()
    {
        return hintsXpos;
    }

    public int getHintsYpos()
    {
        return hintsYpos;
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

    public String getLastOverrideSubscriber()
    {
        return lastOverrideSubscriber;
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

    public int getLibrariesBiblioDividerLocation()
    {
        if (librariesBiblioDividerLocation == 0)
            librariesBiblioDividerLocation = 142;
        return librariesBiblioDividerLocation;
    }

    public String getLibrariesDefaultMinimum()
    {
        return librariesDefaultMinimum;
    }

    public String getLibrariesDefaultMinimumScale()
    {
        return librariesDefaultMinimumScale;
    }

    public int getLibrariesDividerLocation()
    {
        if (librariesDividerLocation == 0)
            librariesDividerLocation = 142;
        return librariesDividerLocation;
    }

    public int getLibrariesLocationColumnWidth()
    {
        return librariesLocationColumnWidth;
    }

    public int getLibrariesMinimumSizeColumnWidth()
    {
        return librariesMinimumSizeColumnWidth;
    }

    public String getLocale()
    {
        if (locale.isEmpty())
            locale = context.main.localeAbbrev;
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

    public int getRunOption()
    {
        return runOption;
    }

    public int getSchema()
    {
        return schema;
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

    public int getSystemOneTableWidth()
    {
        return systemOneTableWidth;
    }

    public int getSystemOneTreeWidth()
    {
        return systemOneTreeWidth;
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
                place = JTabbedPane.LEFT;
                break;
            case 2:
                place = JTabbedPane.BOTTOM;
                break;
            case 3:
                place = JTabbedPane.RIGHT;
                break;
            default:
                place = JTabbedPane.TOP;
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

    public int getToolOperationsDividerConfigLocation()
    {
        return toolOperationsDividerConfigLocation;
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

    public int getToolsEmailDividerLocation()
    {
        return toolsEmailDividerLocation;
    }

    public int getToolsEmailHeight()
    {
        return toolsEmailHeight;
    }

    public int getToolsEmailWidth()
    {
        return toolsEmailWidth;
    }

    public int getToolsEmailXpos()
    {
        return toolsEmailXpos;
    }

    public int getToolsEmailYpos()
    {
        return toolsEmailYpos;
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

    public int getToolsOperationsHeight()
    {
        return toolsOperationsHeight;
    }

    public int getToolsOperationsWidth()
    {
        return toolsOperationsWidth;
    }

    public int getToolsOperationsXpos()
    {
        return toolsOperationsXpos;
    }

    public int getToolsOperationsYpos()
    {
        return toolsOperationsYpos;
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

    public int getToolsSleepDividerLocation()
    {
        return toolsSleepDividerLocation;
    }

    public int getToolsSleepHeight()
    {
        return toolsSleepHeight;
    }

    public int getToolsSleepWidth()
    {
        return toolsSleepWidth;
    }

    public int getToolsSleepXpos()
    {
        return toolsSleepXpos;
    }

    public int getToolsSleepYpos()
    {
        return toolsSleepYpos;
    }

    public void initLookAndFeel(String name, boolean isInitial) throws Exception
    {
        try
        {
            if (isInitial) // not sure if this matters
            {
                if (Utils.getOS().equalsIgnoreCase("mac"))
                {
                    System.setProperty("apple.laf.useScreenMenuBar", "true");
                    System.setProperty("apple.awt.application.name", name);
                    System.setProperty("apple.awt.application.appearance", "system");
                }
                UIManager.put("Tree.showDefaultIcons", true);
                UIManager.put("ScrollBar.showButtons", isShowArrows()); // show scrollbar up/down buttons
                UIManager.put("Component.hideMnemonics", !isShowMnemonics()); // show/hide mnemonic letters
                UIManager.put("TabbedPane.showTabSeparators", true); // separators between tabs

                // set accent color for current LaF
                if (getAccentColor() == null || getAccentColor().length() < 1)
                {
                    setAccentColor(DEFAULT_ACCENT_COLOR);
                }
                String accent = getAccentColor();
                FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#" + accent));
            }

            if (getLookAndFeel() == -1)
            {
                if (Utils.getOS().equalsIgnoreCase("mac"))
                    setLookAndFeel(6);
                else
                    setLookAndFeel(4);
            }

            switch (getLookAndFeel())
            {
                // Built-in themes
                case 0:
                    UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
                    break;
                // FlatLaf themes
                case 1:
                    FlatLightLaf.setup();
                    break;
                case 2:
                    FlatDarkLaf.setup();
                    break;
                case 3:
                    FlatIntelliJLaf.setup();
                    break;
                case 4:
                default:
                    setLookAndFeel(4);
                    FlatDarculaLaf.setup();
                    break;
                case 5:
                    FlatMacLightLaf.setup();
                    break;
                case 6:
                    FlatMacDarkLaf.setup();
                    break;
            }

            FlatLaf.updateUI();
            lookAndFeelInitialized = true;
        }
        catch (Exception e)
        {
            if (context != null)
                context.fault = true;
            throw e;
        }
    }

    public boolean isAskSendEmail()
    {
        return askSendEmail;
    }

    public boolean isAutoRefresh()
    {
        return autoRefresh;
    }

    public boolean isBinaryScale()
    {
        return binaryScale;
    }

    public boolean isDefaultDryrun()
    {
        return defaultDryrun;
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

    public boolean isLastHintKeysIsOpen()
    {
        return lastHintKeysIsOpen;
    }

    public boolean isLastHintTrackingIsOpen()
    {
        return lastHintTrackingIsOpen;
    }

    public boolean isLastHintTrackingIsRemote()
    {
        return lastHintTrackingIsRemote;
    }

    public boolean isLastOverrideHintHost()
    {
        return lastOverrideHintHost;
    }

    public boolean isLastPublisherIsOpen()
    {
        return lastPublisherIsOpen;
    }

    public boolean isLastPublisherIsWorkstation()
    {
        return lastPublisherIsWorkstation;
    }

    public boolean isLastSubscriberIsOpen()
    {
        return lastSubscriberIsOpen;
    }

    public boolean isLastSubscriberIsRemote()
    {
        return lastSubscriberIsRemote;
    }

    public boolean isLookAndFeelInitialized()
    {
        return lookAndFeelInitialized;
    }

    public boolean isMacosLauncher()
    {
        return macosLauncher;
    }

    public boolean isPreserveFileTimes()
    {
        return preserveFileTimes;
    }

    public boolean isShowArrows()
    {
        return showArrows;
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

    public boolean isShowGettingStarted()
    {
        return showGettingStarted;
    }

    public boolean isShowMnemonics()
    {
        return showMnemonics;
    }

    public boolean isShowNavigation()
    {
        return showNavigation;
    }

    public boolean isShowToolbar()
    {
        return showToolbar;
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

    public boolean isTooltipsLargeTables()
    {
        return tooltipsLargeTables;
    }

    public boolean isUseLastPublisherSubscriber()
    {
        return useLastPublisherSubscriber;
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

    public void setAskSendEmail(boolean askSendEmail)
    {
        this.askSendEmail = askSendEmail;
    }

    public void setAutoRefresh(boolean autoRefresh)
    {
        this.autoRefresh = autoRefresh;
    }

    public void setBinaryScale(boolean binaryScale)
    {
        this.binaryScale = binaryScale;
    }

    public void setBookmarksHeight(int bookmarksHeight)
    {
        this.bookmarksHeight = bookmarksHeight;
    }

    public void setBookmarksNameWidth(int bookmarksNameWidth)
    {
        this.bookmarksNameWidth = bookmarksNameWidth;
    }

    public void setBookmarksPathWidth(int bookmarksPathWidth)
    {
        this.bookmarksPathWidth = bookmarksPathWidth;
    }

    public void setBookmarksWidth(int bookmarksWidth)
    {
        this.bookmarksWidth = bookmarksWidth;
    }

    public void setBookmarksXpos(int bookmarksXpos)
    {
        this.bookmarksXpos = bookmarksXpos;
    }

    public void setBookmarksYpos(int bookmarksYpos)
    {
        this.bookmarksYpos = bookmarksYpos;
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

    public void setContext(Context context)
    {
        this.context = context;
        if (this.context != null)
            this.context.preferences = this;
    }

    public void setDateFormat(String dateFormat)
    {
        this.dateFormat = dateFormat;
    }

    public void setDefaultDryrun(boolean defaultDryrun)
    {
        this.defaultDryrun = defaultDryrun;
    }

    public void setDefaultEmailServer(String defaultEmailServer)
    {
        this.defaultEmailServer = defaultEmailServer;
    }

    public void setDirectoryPickerXpos(int directoryPickerXpos)
    {
        this.directoryPickerXpos = directoryPickerXpos;
    }

    public void setDirectoryPickerYpos(int directoryPickerYpos)
    {
        this.directoryPickerYpos = directoryPickerYpos;
    }

    public void setFileEditorHeight(int fileEditorHeight)
    {
        this.fileEditorHeight = fileEditorHeight;
    }

    public void setFileEditorWidth(int fileEditorWidth)
    {
        this.fileEditorWidth = fileEditorWidth;
    }

    public void setFileEditorXpos(int fileEditorXpos)
    {
        this.fileEditorXpos = fileEditorXpos;
    }

    public void setFileEditorYpos(int fileEditorYpos)
    {
        this.fileEditorYpos = fileEditorYpos;
    }

    public void setGenerateLongOptions(boolean generateLongOptions)
    {
        this.generateLongOptions = generateLongOptions;
    }

    public void setHelpHeight(int helpHeight)
    {
        this.helpHeight = helpHeight;
    }

    public void setHelpWidth(int helpWidth)
    {
        this.helpWidth = helpWidth;
    }

    public void setHelpXpos(int helpXpos)
    {
        this.helpXpos = helpXpos;
    }

    public void setHelpYpos(int helpYpos)
    {
        this.helpYpos = helpYpos;
    }

    public void setHideFilesInTree(boolean hideFilesInTree)
    {
        this.hideFilesInTree = hideFilesInTree;
    }

    public void setHideHiddenFiles(boolean hideHiddenFiles)
    {
        this.hideHiddenFiles = hideHiddenFiles;
    }

    public void setHintsActionWidth(int hintsActionWidth)
    {
        this.hintsActionWidth = hintsActionWidth;
    }

    public void setHintsByWidth(int hintsByWidth)
    {
        this.hintsByWidth = hintsByWidth;
    }

    public void setHintsDateWidth(int hintsDateWidth)
    {
        this.hintsDateWidth = hintsDateWidth;
    }

    public void setHintsFromItemWidth(int hintsFromItemWidth)
    {
        this.hintsFromItemWidth = hintsFromItemWidth;
    }

    public void setHintsFromLibWidth(int hintsFromLibWidth)
    {
        this.hintsFromLibWidth = hintsFromLibWidth;
    }

    public void setHintsHeight(int hintsHeight)
    {
        this.hintsHeight = hintsHeight;
    }

    public void setHintsStatusWidth(int hintsStatusWidth)
    {
        this.hintsStatusWidth = hintsStatusWidth;
    }

    public void setHintsSystemWidth(int hintsSystemWidth)
    {
        this.hintsSystemWidth = hintsSystemWidth;
    }

    public void setHintsToItemWidth(int hintsToItemWidth)
    {
        this.hintsToItemWidth = hintsToItemWidth;
    }

    public void setHintsToLibWidth(int hintsToLibWidth)
    {
        this.hintsToLibWidth = hintsToLibWidth;
    }

    public void setHintsWidth(int hintsWidth)
    {
        this.hintsWidth = hintsWidth;
    }

    public void setHintsXpos(int hintsXpos)
    {
        this.hintsXpos = hintsXpos;
    }

    public void setHintsYpos(int hintsYpos)
    {
        this.hintsYpos = hintsYpos;
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

    public void setLastHintKeysIsOpen(boolean lastHintKeysIsOpen)
    {
        this.lastHintKeysIsOpen = lastHintKeysIsOpen;
    }

    public void setLastHintKeysOpenFile(String lastHintKeysOpenFile)
    {
        this.lastHintKeysOpenFile = lastHintKeysOpenFile;
    }

    public void setLastHintKeysOpenPath(String lastHintKeysOpenPath)
    {
        this.lastHintKeysOpenPath = lastHintKeysOpenPath;
    }

    public void setLastHintTrackingIsOpen(boolean lastHintTrackingIsOpen)
    {
        this.lastHintTrackingIsOpen = lastHintTrackingIsOpen;
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

    public void setLastOverrideHintHost(boolean lastOverrideHintHost)
    {
        this.lastOverrideHintHost = lastOverrideHintHost;
    }

    public void setLastOverrideSubscriber(String lastOverrideSubscriber)
    {
        this.lastOverrideSubscriber = lastOverrideSubscriber;
    }

    public void setLastPublisherIsOpen(boolean lastPublisherIsOpen)
    {
        this.lastPublisherIsOpen = lastPublisherIsOpen;
    }

    public void setLastPublisherIsWorkstation(boolean lastPublisherIsWorkstation)
    {
        this.lastPublisherIsWorkstation = lastPublisherIsWorkstation;
    }

    public void setLastPublisherOpenFile(String lastPublisherOpenFile)
    {
        this.lastPublisherOpenFile = lastPublisherOpenFile;
    }

    public void setLastPublisherOpenPath(String lastPublisherOpenPath)
    {
        this.lastPublisherOpenPath = lastPublisherOpenPath;
    }

    public void setLastSubscriberIsOpen(boolean lastSubscriberIsOpen)
    {
        this.lastSubscriberIsOpen = lastSubscriberIsOpen;
    }

    public void setLastSubscriberIsRemote(boolean lastSubscriberIsRemote)
    {
        this.lastSubscriberIsRemote = lastSubscriberIsRemote;
    }

    public void setLastSubscriberOpenFile(String lastSubscriberOpenFile)
    {
        this.lastSubscriberOpenFile = lastSubscriberOpenFile;
    }

    public void setLastSubscriberOpenPath(String lastSubscriberOpenPath)
    {
        this.lastSubscriberOpenPath = lastSubscriberOpenPath;
    }

    public void setLibrariesBiblioDividerLocation(int librariesBiblioDividerLocation)
    {
        this.librariesBiblioDividerLocation = librariesBiblioDividerLocation;
    }

    public void setLibrariesDefaultMinimum(String librariesDefaultMinimum)
    {
        this.librariesDefaultMinimum = librariesDefaultMinimum;
    }

    public void setLibrariesDefaultMinimumScale(String librariesDefaultMinimumScale)
    {
        this.librariesDefaultMinimumScale = librariesDefaultMinimumScale;
    }

    public void setLibrariesDividerLocation(int librariesDividerLocation)
    {
        this.librariesDividerLocation = librariesDividerLocation;
    }

    public void setLibrariesLocationColumnWidth(int librariesLocationColumnWidth)
    {
        this.librariesLocationColumnWidth = librariesLocationColumnWidth;
    }

    public void setLibrariesMinimumSizeColumnWidth(int librariesMinimumSizeColumnWidth)
    {
        this.librariesMinimumSizeColumnWidth = librariesMinimumSizeColumnWidth;
    }

    public void setLocale(String locale)
    {
        this.locale = locale;
    }

    public void setLookAndFeel(int lookAndFeel)
    {
        this.lookAndFeel = lookAndFeel;
    }

    public void setMacosLauncher(boolean macosLauncher)
    {
        this.macosLauncher = macosLauncher;
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

    public void setRunOption(int runOption)
    {
        this.runOption = runOption;
    }

    public void setSchema(int schema)
    {
        this.schema = schema;
    }

    public void setShowArrows(boolean showArrows)
    {
        this.showArrows = showArrows;
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

    public void setShowGettingStarted(boolean showGettingStarted)
    {
        this.showGettingStarted = showGettingStarted;
    }

    public void setShowMnemonics(boolean showMnemonics)
    {
        this.showMnemonics = showMnemonics;
    }

    public void setShowNavigation(boolean showNavigation)
    {
        this.showNavigation = showNavigation;
    }

    public void setShowToolbar(boolean showToolbar)
    {
        this.showToolbar = showToolbar;
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

    public void setSystemOneTableWidth(int systemOneTableWidth)
    {
        this.systemOneTableWidth = systemOneTableWidth;
    }

    public void setSystemOneTreeWidth(int systemOneTreeWidth)
    {
        this.systemOneTreeWidth = systemOneTreeWidth;
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

    public void setToolOperationsDividerConfigLocation(int toolOperationsDividerConfigLocation)
    {
        this.toolOperationsDividerConfigLocation = toolOperationsDividerConfigLocation;
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

    public void setToolsEmailDividerLocation(int toolsEmailDividerLocation)
    {
        this.toolsEmailDividerLocation = toolsEmailDividerLocation;
    }

    public void setToolsEmailHeight(int toolsEmailHeight)
    {
        this.toolsEmailHeight = toolsEmailHeight;
    }

    public void setToolsEmailWidth(int toolsEmailWidth)
    {
        this.toolsEmailWidth = toolsEmailWidth;
    }

    public void setToolsEmailXpos(int toolsEmailXpos)
    {
        this.toolsEmailXpos = toolsEmailXpos;
    }

    public void setToolsEmailYpos(int toolsEmailYpos)
    {
        this.toolsEmailYpos = toolsEmailYpos;
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

    public void setToolsOperationsHeight(int toolsOperationsHeight)
    {
        this.toolsOperationsHeight = toolsOperationsHeight;
    }

    public void setToolsOperationsWidth(int toolsOperationsWidth)
    {
        this.toolsOperationsWidth = toolsOperationsWidth;
    }

    public void setToolsOperationsXpos(int toolsOperationsXpos)
    {
        this.toolsOperationsXpos = toolsOperationsXpos;
    }

    public void setToolsOperationsYpos(int toolsOperationsYpos)
    {
        this.toolsOperationsYpos = toolsOperationsYpos;
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

    public void setToolsSleepDividerLocation(int toolsSleepDividerLocation)
    {
        this.toolsSleepDividerLocation = toolsSleepDividerLocation;
    }

    public void setToolsSleepHeight(int toolsSleepHeight)
    {
        this.toolsSleepHeight = toolsSleepHeight;
    }

    public void setToolsSleepWidth(int toolsSleepWidth)
    {
        this.toolsSleepWidth = toolsSleepWidth;
    }

    public void setToolsSleepXpos(int toolsSleepXpos)
    {
        this.toolsSleepXpos = toolsSleepXpos;
    }

    public void setToolsSleepYpos(int toolsSleepYpos)
    {
        this.toolsSleepYpos = toolsSleepYpos;
    }

    public void setTooltipsLargeTables(boolean tooltipsLargeTables)
    {
        this.tooltipsLargeTables = tooltipsLargeTables;
    }

    public void setUseLastPublisherSubscriber(boolean useLastPublisherSubscriber)
    {
        this.useLastPublisherSubscriber = useLastPublisherSubscriber;
    }

    public void write(Context context) throws Exception
    {
        String json;
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        schema = SCHEMA; // set current schema version

        // size & position
        appWidth = context.mainFrame.getWidth();
        appHeight = context.mainFrame.getHeight();
        Point location = context.mainFrame.getLocation();
        appXpos = location.x;
        appYpos = location.y;

        extractPositionsSizes(context);

        // shorten paths relative to the working directory if possible
        String savedHintKeysOpenFile = getLastHintKeysOpenFile();
        setLastHintKeysOpenFile(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastHintKeysOpenFile()));

        String savedHintKeysOpenPath = getLastHintKeysOpenPath();
        setLastHintKeysOpenPath(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastHintKeysOpenPath()));

        String savedHintTrackingOpenFile = getLastHintTrackingOpenFile();
        setLastHintTrackingOpenFile(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastHintTrackingOpenFile()));

        String savedHintTrackingOpenPath = getLastHintTrackingOpenPath();
        setLastHintTrackingOpenPath(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastHintTrackingOpenPath()));

        String savedPublisherOpenFile = getLastPublisherOpenFile();
        setLastPublisherOpenFile(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastPublisherOpenFile()));

        String savedPublisherOpenPath = getLastPublisherOpenPath();
        setLastPublisherOpenPath(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastPublisherOpenPath()));

        String savedSubscriberOpenFile = getLastSubscriberOpenFile();
        setLastSubscriberOpenFile(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastSubscriberOpenFile()));

        String savedSubscriberOpenPath = getLastSubscriberOpenPath();
        setLastSubscriberOpenPath(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), getLastSubscriberOpenPath()));

        // additions
        if (librariesDefaultMinimum == null)
            librariesDefaultMinimum = "40";
        if (librariesDefaultMinimumScale == null)
            librariesDefaultMinimumScale = "GB";

        json = gson.toJson(this);
        File f = null;
        try
        {
            f = new File(getFullPath(context.cfg.getWorkingDirectory()));
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath(context.cfg.getWorkingDirectory()));
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(context.cfg.gs("Z.error.writing") + (f != null ? f.getPath() : context.cfg.gs("Preferences.preferences.file")) +
                    " trace: " + Utils.getStackTrace(fnf));
        }

        // restore long paths
        setLastHintKeysOpenFile(savedHintKeysOpenFile);
        setLastHintKeysOpenPath(savedHintKeysOpenPath);
        setLastHintTrackingOpenFile(savedHintTrackingOpenFile);
        setLastHintTrackingOpenPath(savedHintTrackingOpenPath);
        setLastPublisherOpenFile(savedPublisherOpenFile);
        setLastPublisherOpenPath(savedPublisherOpenPath);
        setLastSubscriberOpenFile(savedSubscriberOpenFile);
        setLastSubscriberOpenPath(savedSubscriberOpenPath);
    }

    // ================================================================================================================

    public class SortMeta
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
