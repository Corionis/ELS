package com.corionis.els.gui;

import com.formdev.flatlaf.*;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings(value = "unchecked")
public class Preferences implements Serializable
{
    public static final String DEFAULT_ACCENT_COLOR = "2675BF";
    public static final int SCHEMA = 1; // schema version, set in write()
    private String accentColor = DEFAULT_ACCENT_COLOR;
    private int appHeight = 640;
    private int appWidth = 1024;
    private int appXpos = 0;
    private int appYpos = 0;
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
    private boolean defaultDryrun = true;
    private int directoryPickerXpos = 0;
    private int directoryPickerYpos = 0;
    private int fileEditorHeight = 365;
    private int fileEditorWidth = 425;
    private int fileEditorXpos = 0;
    private int fileEditorYpos = 0;
    private boolean generateLongOptions = false;
    private boolean hideFilesInTree = true;
    private boolean hideHiddenFiles = true;
    private int jobsHeight = 470;
    private int jobsOriginDividerLocation = 142;
    private int jobsTaskDividerLocation = 142;
    private int jobsWidth = 570;
    private int jobsXpos = 0;
    private int jobsYpos = 0;
    private boolean lastHintKeysInUse = false;
    private String lastHintKeysOpenFile = "";
    private String lastHintKeysOpenPath = "";
    private boolean lastHintTrackingInUse = false;
    private boolean lastHintTrackingIsRemote = false;
    private String lastHintTrackingOpenFile = "";
    private String lastHintTrackingOpenPath = "";
    private boolean lastPublisherInUse = false;
    private boolean lastPublisherIsWorkstation = false;
    private String lastPublisherOpenFile = "";
    private String lastPublisherOpenPath = "";
    private boolean lastSubscriberInUse = false;
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
    // The Look 'n Feel, 0-6
    private int lookAndFeel = -1; // default IntelliJ Dark, aka Darcula
    private boolean preserveFileTimes = true;
    private int progressHeight = -1;
    private int progressWidth = -1;
    private int progressXpos = 0;
    private int progressYpos = 0;
    private int schema = 1;
    private boolean showArrows = true;
    private boolean showCcpConfirmation = true;
    private boolean showDeleteConfirmation = true;
    private boolean showDnDConfirmation = true;
    private boolean showMnemonics = true;
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
    private int tabPlacement = JTabbedPane.TOP;
    private int toolOperationsDividerConfigLocation = 142;
    private int toolsDuplicateFinderHeight = 470;
    private int toolsDuplicateFinderWidth = 570;
    private int toolsDuplicateFinderXpos = 0;
    private int toolsDuplicateFinderYpos = 0;
    private int toolsEmptyDirectoryFinderHeight = 470;
    private int toolsEmptyDirectoryFinderWidth = 570;
    private int toolsEmptyDirectoryFinderXpos = 0;
    private int toolsEmptyDirectoryFinderYpos = 0;
    private int toolsJunkRemoverDividerLocation = 142;
    private int toolsJunkRemoverHeight = 470;
    private int toolsJunkRemoverWidth = 570;
    private int toolsJunkRemoverXpos = 0;
    private int toolsJunkRemoverYpos = 0;
    private int toolsOperationsHeight = 470;
    private int toolsOperationsWidth = 570;
    private int toolsOperationsXpos = 0;
    private int toolsOperationsYpos = 0;
    private int toolsRenamerDividerLocation = 142;
    private int toolsRenamerHeight = 470;
    private int toolsRenamerWidth = 570;
    private int toolsRenamerXpos = 0;
    private int toolsRenamerYpos = 0;
    private int toolsSleepDividerLocation = 142;
    private int toolsSleepHeight = 470;
    private int toolsSleepWidth = 570;
    private int toolsSleepXpos = 0;
    private int toolsSleepYpos = 0;
    private boolean useLastPublisherSubscriber = true;
    private transient Context context;
    private transient Logger logger = LogManager.getLogger("applog");
    /**
     * Constructor
     */
    public Preferences()
    {
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

        if (table == null || table.getName().equalsIgnoreCase("tableLocations"))
        {
            librariesLocationColumnWidth = context.mainFrame.tableLocations.getColumnModel().getColumn(0).getWidth();
            librariesMinimumSizeColumnWidth = context.mainFrame.tableLocations.getColumnModel().getColumn(1).getWidth();
        }
    }

    public void fixApplication(Context context)
    {
        // set position and size

        if (Utils.isOffScreen(getAppXpos(), getAppYpos()))
        {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            int x = screenSize.width / 2 - getAppWidth() / 2;
            if (x < 0)
                x = 1;
            int y = screenSize.height / 2 - getAppHeight() / 2;
            if (y < 0)
                y = 1;
            setAppXpos(x);
            setAppYpos(y);
        }

        context.mainFrame.setLocation(getAppXpos(), getAppYpos());
        context.mainFrame.setSize(getAppWidth(), getAppHeight());

        // dividers
        // the bottom divider is handler elsewhere
        context.mainFrame.splitPaneTwoBrowsers.setOrientation(getCenterDividerOrientation());
        context.mainFrame.splitPaneTwoBrowsers.setDividerLocation(getCenterDividerLocation());
        context.mainFrame.splitPaneCollectionOne.setDividerLocation(getCollectionOneDividerLocation());
        context.mainFrame.splitPaneCollectionTwo.setDividerLocation(getCollectionTwoDividerLocation());
        context.mainFrame.splitPaneSystemOne.setDividerLocation(getSystemOneDividerLocation());
        context.mainFrame.splitPaneSystemTwo.setDividerLocation(getSystemTwoDividerLocation());

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
            bottomSize = getBrowserBottomSize();

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
                context.mainFrame.splitPaneCollectionOne.setDividerLocation(getCollectionOneDividerLocation());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setPreferredWidth(getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(1).setWidth(getCollectionOneNameWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setPreferredWidth(getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(2).setWidth(getCollectionOneSizeWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setPreferredWidth(getCollectionOneDateWidth());
                context.mainFrame.tableCollectionOne.getColumnModel().getColumn(3).setWidth(getCollectionOneDateWidth());
                setTableSort(context.mainFrame.tableCollectionOne, getCollectionOneSortColumn(), getCollectionOneSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableCollectionTwo"))
        {
            if (context.mainFrame.tableCollectionTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneCollectionTwo.setDividerLocation(getCollectionTwoDividerLocation());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setPreferredWidth(getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(1).setWidth(getCollectionTwoNameWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setPreferredWidth(getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(2).setWidth(getCollectionTwoSizeWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setPreferredWidth(getCollectionTwoDateWidth());
                context.mainFrame.tableCollectionTwo.getColumnModel().getColumn(3).setWidth(getCollectionTwoDateWidth());
                setTableSort(context.mainFrame.tableCollectionTwo, getCollectionTwoSortColumn(), getCollectionTwoSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemOne"))
        {
            if (context.mainFrame.tableSystemOne.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneSystemOne.setDividerLocation(getSystemOneDividerLocation());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setPreferredWidth(getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(1).setWidth(getSystemOneNameWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setPreferredWidth(getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(2).setWidth(getSystemOneSizeWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setPreferredWidth(getSystemOneDateWidth());
                context.mainFrame.tableSystemOne.getColumnModel().getColumn(3).setWidth(getSystemOneDateWidth());
                setTableSort(context.mainFrame.tableSystemOne, getSystemOneSortColumn(), getSystemOneSortDirection());
            }
        }

        if (table == null || table.getName().equalsIgnoreCase("tableSystemTwo"))
        {
            if (context.mainFrame.tableSystemTwo.getColumnModel().getColumnCount() == 4)
            {
                context.mainFrame.splitPaneSystemTwo.setDividerLocation(getSystemTwoDividerLocation());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setPreferredWidth(getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(1).setWidth(getSystemTwoNameWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setPreferredWidth(getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(2).setWidth(getSystemTwoSizeWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setPreferredWidth(getSystemTwoDateWidth());
                context.mainFrame.tableSystemTwo.getColumnModel().getColumn(3).setWidth(getSystemTwoDateWidth());
                setTableSort(context.mainFrame.tableSystemTwo, getSystemTwoSortColumn(), getSystemTwoSortDirection());
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
    //private transient LookAndFeel laf = null;

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
        if (workingDirectory == null || workingDirectory.length() == 0)
            workingDirectory = System.getProperty("user.dir");

        String path = workingDirectory + System.getProperty("file.separator") + "local" +
                System.getProperty("file.separator") + "preferences.json";
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
        if (locale.length() == 0)
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
            //if (isInitial) // not sure if this matters
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
        }
        catch (Exception e)
        {
            if (context != null)
                context.fault = true;
            throw e;
        }
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

    public boolean isLastHintKeysInUse()
    {
        return lastHintKeysInUse;
    }

    public boolean isLastHintTrackingInUse()
    {
        return lastHintTrackingInUse;
    }

    public boolean isLastHintTrackingIsRemote()
    {
        return lastHintTrackingIsRemote;
    }

    public boolean isLastPublisherInUse()
    {
        return lastPublisherInUse;
    }

    public boolean isLastPublisherIsWorkstation()
    {
        return lastPublisherIsWorkstation;
    }

    public boolean isLastSubscriberInUse()
    {
        return lastSubscriberInUse;
    }

    public boolean isLastSubscriberIsRemote()
    {
        return lastSubscriberIsRemote;
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

    public boolean isShowMnemonics()
    {
        return showMnemonics;
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

    public void setLastHintKeysInUse(boolean lastHintKeysInUse)
    {
        this.lastHintKeysInUse = lastHintKeysInUse;
    }

    public void setLastHintKeysOpenFile(String lastHintKeysOpenFile)
    {
        this.lastHintKeysOpenFile = lastHintKeysOpenFile;
    }

    public void setLastHintKeysOpenPath(String lastHintKeysOpenPath)
    {
        this.lastHintKeysOpenPath = lastHintKeysOpenPath;
    }

    public void setLastHintTrackingInUse(boolean lastHintTrackingInUse)
    {
        this.lastHintTrackingInUse = lastHintTrackingInUse;
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

    public void setLastPublisherInUse(boolean lastPublisherInUse)
    {
        this.lastPublisherInUse = lastPublisherInUse;
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

    public void setLastSubscriberInUse(boolean lastSubscriberInUse)
    {
        this.lastSubscriberInUse = lastSubscriberInUse;
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

    public void setShowMnemonics(boolean showMnemonics)
    {
        this.showMnemonics = showMnemonics;
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

        // dividers
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
            throw new MungeException("Error writing: " + (f != null ? f.getAbsolutePath() : "preferences file,") + " trace: " + Utils.getStackTrace(fnf));
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
