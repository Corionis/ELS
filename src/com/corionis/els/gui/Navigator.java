package com.corionis.els.gui;

import com.corionis.els.*;
import com.corionis.els.gui.bookmarks.Bookmark;
import com.corionis.els.gui.bookmarks.Bookmarks;
import com.corionis.els.gui.browser.NavTransferHandler;
import com.corionis.els.gui.browser.NavTreeNode;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.tools.duplicateFinder.DuplicateFinderUI;
import com.corionis.els.gui.tools.emptyDirectoryFinder.EmptyDirectoryFinderUI;
import com.corionis.els.gui.tools.junkRemover.JunkRemoverUI;
import com.corionis.els.gui.tools.operations.OperationsUI;
import com.corionis.els.gui.tools.renamer.RenamerUI;
import com.corionis.els.gui.tools.sleep.SleepUI;
import com.corionis.els.jobs.Job;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Origins;
import com.corionis.els.repository.Repository;
import com.corionis.els.sftp.ClientSftp;
import com.corionis.els.stty.ClientStty;
import com.corionis.els.gui.browser.Browser;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.JobsUI;
import com.corionis.els.gui.libraries.LibrariesUI;
import com.corionis.els.gui.system.FileEditor;
import com.corionis.els.gui.update.DownloadUpdater;
import com.corionis.els.gui.util.GuiLogAppender;

import com.formdev.flatlaf.extras.FlatDesktop;
import com.formdev.flatlaf.util.SystemInfo;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.*;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.Process;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Navigator
{
    public Bookmarks bookmarks;
    public Context context;
    public DuplicateFinderUI dialogDuplicateFinder;
    public EmptyDirectoryFinderUI dialogEmptyDirectoryFinder;
    public JobsUI dialogJobs = null;
    public JunkRemoverUI dialogJunkRemover = null;
    public OperationsUI dialogOperations = null;
    public RenamerUI dialogRenamer = null;
    public SleepUI dialogSleep = null;
    public FileEditor fileeditor = null;
    public Job[] jobs;
    public boolean showHintTrackingButton = false;
    public SwingWorker<Void, Void> worker;
    private int bottomSizeBrowser;
    private Settings dialogSettings = null;
    private int lastFindPosition = 0;
    private String lastFindString = "";
    private int lastFindTab = -1;
    boolean mockMode = false; // instead of downloading version.info get from mock/bin/
    private ArrayList<ArrayList<Origin>> originsArray = null;
    private boolean quitRemoteHintStatusServer = false;
    private boolean quitRemoteSubscriber = false;
    private boolean remoteJobRunning = false;
    private boolean updaterProcess = false;
    private String updaterJar = null;
    private transient Logger logger = LogManager.getLogger("applog");

    public Navigator(Main main, Context context)
    {
        this.context = context;
        this.context.navigator = this;
    }

    /**
     * Check for updates
     */
    public boolean checkForUpdates(boolean checkOnly)
    {
        ArrayList<String> version = new ArrayList<>();

        try
        {
            String message;
            String prefix;
            URL url = null;
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            // set location to find update.info for the URL prefix
            String updateInfoPath = context.cfg.getInstalledPath() + System.getProperty("file.separator") +
                    (Utils.isOsMac() ? "Contents/Java" : "bin") + System.getProperty("file.separator") +
                    "update.info";

            // get update.info
            // putting the ELS deploy URL prefix in a file allows it to be changed manually if necessary
            File updateInfo = new File(updateInfoPath);
            if (updateInfo.exists())
            {
                prefix = new String(Files.readAllBytes(Paths.get(updateInfoPath)));
                prefix = prefix.trim();
            }
            else
            {
                prefix = context.cfg.getUrlPrefix(); // use the hardcoded URL
                logger.warn("update.info not found: " + updateInfoPath + ", using coded URL: " + prefix);
            }

            // download the latest version.info
            String versionPath = "";
            BufferedReader bufferedReader = null;
            try
            {
                if (!mockMode)
                {
                    versionPath = prefix + "/version.info";
                    url = new URL(versionPath);
                    bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
                }
                else // assume (mock) working directory
                {
                    versionPath = context.cfg.getWorkingDirectory() + System.getProperty("file.separator") +
                            "bin" + System.getProperty("file.separator") +
                            "version.info";
                    bufferedReader = new BufferedReader(new FileReader(versionPath));
                }
                String buf;
                while ((buf = bufferedReader.readLine()) != null)
                {
                    version.add(buf.trim());
                }
                bufferedReader.close();
            }
            catch (Exception e)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                message = java.text.MessageFormat.format(context.cfg.gs("Navigator.update.info.not.found"), versionPath);
                logger.error(message);
                Object[] opts = {context.cfg.gs("Z.ok")};
                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                return false;
            }

            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            if (version.size() < Configuration.VERSION_SIZE)
            {
                message = java.text.MessageFormat.format(context.cfg.gs("Navigator.version.info.missing.or.malformed"), versionPath);
                logger.info(message);
                Object[] opts = {context.cfg.gs("Z.ok")};
                JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                        JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                return false;
            }
            else
            {
                // get optional build flags
                String flags = (version.size() > Configuration.VERSION_SIZE) ? version.get(Configuration.BUILD_FLAGS) : "";

                // do the build numbers match?
                if (checkOnly) // automated check
                {
                    if (flags.toLowerCase().contains("ignore") || version.get(Configuration.BUILD_NUMBER).equals(Configuration.getBuildNumber()))
                        return false;
                    return true;
                }

                if (flags.toLowerCase().contains("ignore") || version.get(Configuration.BUILD_NUMBER).equals(Configuration.getBuildNumber())) // manual check
                {
                    // yes, up-to-date
                    message = context.cfg.gs("Navigator.installed.up.to.date");
                    logger.info(message);
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);
                    return false;
                }
                else
                {
                    while (true)
                    {
                        // a new version is available
                        message = java.text.MessageFormat.format(context.cfg.gs("Navigator.install.new.version"),
                                Configuration.getBuildDate(), version.get(Configuration.BUILD_DATE));
                        Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no"), context.cfg.gs("Navigator.recent.changes")};
                        int reply = JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                                JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);

                        // proceed?
                        if (reply == JOptionPane.YES_OPTION)
                        {
                            // execute the download and unpack procedure then execute the Updater
                            new DownloadUpdater(this, version, prefix);
                            break;
                        }
                        else if (reply == JOptionPane.CANCEL_OPTION) // show Changelist
                        {
                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            NavHelp helpDialog = new NavHelp(context.mainFrame, context.mainFrame, context,
                                    context.cfg.gs("Navigator.recent.changes"), version.get(Configuration.BUILD_CHANGES_URL));
                            if (!helpDialog.fault)
                            {
                                helpDialog.setModal(true);
                                Point loc = context.mainFrame.getLocation();
                                loc.x = loc.x + (context.mainFrame.getWidth() / 2) - (helpDialog.getWidth() / 2);
                                loc.y = loc.y + (context.mainFrame.getHeight() / 2) - (helpDialog.getHeight() / 2);
                                helpDialog.setLocation(loc);
                                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                helpDialog.setVisible(true);
                            }
                        }
                        else
                        {
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            logger.error("Error downloading update: " + Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Error downloading update") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        return true;
    }

    public void disableComponent(boolean disable, Component component)
    {
        boolean enable = !disable;
        component.setEnabled(enable);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            if (components != null && components.length > 0)
            {
                for (Component comp : components)
                {
                    disableComponent(disable, comp);
                }
            }
        }
    }

    public void disableGui(boolean disable)
    {
        boolean enable = !disable;

        if (enable == false)
        {
            bottomSizeBrowser = context.preferences.getBrowserBottomSize();
        }

        context.mainFrame.panelBrowserTop.setVisible(enable);

        context.mainFrame.menuItemOpenPublisher.setEnabled(enable);
        context.mainFrame.menuItemOpenSubscriber.setEnabled(enable);
        context.mainFrame.menuItemOpenHintKeys.setEnabled(enable);
        context.mainFrame.menuItemOpenHintTracking.setEnabled(enable);

        context.mainFrame.menuItemFind.setEnabled(enable);
        context.mainFrame.menuItemFindNext.setEnabled(enable);
        context.mainFrame.menuItemNewFolder.setEnabled(enable);
        context.mainFrame.menuItemRename.setEnabled(enable);
        context.mainFrame.menuItemTouch.setEnabled(enable);
        context.mainFrame.menuItemCopy.setEnabled(enable);
        context.mainFrame.menuItemCut.setEnabled(enable);
        context.mainFrame.menuItemPaste.setEnabled(enable);
        context.mainFrame.menuItemDelete.setEnabled(enable);

        context.mainFrame.menuItemRefresh.setEnabled(enable);
        context.mainFrame.menuItemAutoRefresh.setEnabled(enable);
        context.mainFrame.menuItemShowHidden.setEnabled(enable);
        context.mainFrame.menuItemWordWrap.setEnabled(enable);

        for (int i = 0; i < context.mainFrame.menuBookmarks.getItemCount(); ++i)
        {
            if (context.mainFrame.menuBookmarks.getItem(i) != null)
                context.mainFrame.menuBookmarks.getItem(i).setEnabled(enable);
        }

        for (int i = 0; i < context.mainFrame.menuTools.getItemCount(); ++i)
        {
            if (context.mainFrame.menuTools.getItem(i) != null)
                context.mainFrame.menuTools.getItem(i).setEnabled(enable);
        }

        for (int i = 0; i < context.mainFrame.menuJobs.getItemCount(); ++i)
        {
            if (context.mainFrame.menuJobs.getItem(i) != null)
                context.mainFrame.menuJobs.getItem(i).setEnabled(enable);
        }

        context.mainFrame.menuItemSplitHorizontal.setEnabled(enable);
        context.mainFrame.menuItemSplitVertical.setEnabled(enable);

        if (enable == true)
        {
            context.preferences.fixBrowserDivider(context, bottomSizeBrowser);
        }

    }

    public void disconnectSubscriber()
    {
        quitByeRemotes(true, false);
        NavTreeNode root = context.browser.setCollectionRoot(null, context.mainFrame.treeCollectionTwo, context.cfg.gs("Browser.open.a.subscriber"), false);
        root.loadTable();
        root = context.browser.setCollectionRoot(null, context.mainFrame.treeSystemTwo, context.cfg.gs("Browser.open.a.subscriber"), false);
        root.loadTable();
        context.subscriberRepo = null;
        context.cfg.setSubscriberCollectionFilename("");
        context.cfg.setSubscriberLibrariesFileName("");
        context.preferences.setLastSubscriberInUse(false);
        context.cfg.setRemoteType("-");
        setQuitTerminateVisibility();
    }

    public void enableDisableToolMenus(AbstractToolDialog dialog, boolean enable)
    {
        context.mainFrame.menuItemJunk.setEnabled(enable);
        context.mainFrame.menuItemJunk.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        context.mainFrame.menuItemOperations.setEnabled(enable);
        context.mainFrame.menuItemOperations.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        context.mainFrame.menuItemRenamer.setEnabled(enable);
        context.mainFrame.menuItemRenamer.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        context.mainFrame.menuItemSleep.setEnabled(enable);
        context.mainFrame.menuItemSleep.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        context.mainFrame.menuItemJobsManage.setEnabled(enable);
        context.mainFrame.menuItemJobsManage.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        context.mainFrame.menuItemUpdates.setEnabled(enable);
        context.mainFrame.menuItemUpdates.setToolTipText(enable ? "" : context.cfg.gs("Navigator.a.tool.is.active"));

        if (!enable)
        {
            if (dialog instanceof JunkRemoverUI)
            {
                context.mainFrame.menuItemJunk.setEnabled(true);
                context.mainFrame.menuItemJunk.setToolTipText("");
            }
            else if (dialog instanceof OperationsUI)
            {
                context.mainFrame.menuItemOperations.setEnabled(true);
                context.mainFrame.menuItemOperations.setToolTipText("");
            }
            else if (dialog instanceof RenamerUI)
            {
                context.mainFrame.menuItemRenamer.setEnabled(true);
                context.mainFrame.menuItemRenamer.setToolTipText("");
            }
            else if (dialog instanceof SleepUI)
            {
                context.mainFrame.menuItemSleep.setEnabled(true);
                context.mainFrame.menuItemSleep.setToolTipText("");
            }
            else if (dialog instanceof JobsUI)
            {
                context.mainFrame.menuItemJobsManage.setEnabled(true);
                context.mainFrame.menuItemJobsManage.setToolTipText("");
            }
        }
    }

    public boolean execExternalExe(String commandLine)
    {
        Marker SIMPLE = MarkerManager.getMarker("SIMPLE");
        try
        {
            final Process proc = Runtime.getRuntime().exec(commandLine);
            Thread thread = new Thread()
            {
                public void run()
                {
                    String line;
                    BufferedReader input = new BufferedReader(new InputStreamReader(proc.getErrorStream()));//getInputStream()));

                    try
                    {
                        while ((line = input.readLine()) != null)
                            logger.info(SIMPLE, line);
                        input.close();
                    }
                    catch (IOException e)
                    {
                        logger.error(context.cfg.gs("Z.exception") + e.getMessage());
                        JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.exception") + Utils.getStackTrace(e), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            };

            // run it
            thread.start();
            int result = proc.waitFor();
            thread.join();
            if (result != 0)
            {
                logger.error(context.cfg.gs("Z.process.failed") + result);
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.process.failed") + result, context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            else
                return true;
        }
        catch (Exception e)
        {
            logger.error(context.cfg.gs("Z.process.failed") + Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.process.failed") + Utils.getStackTrace(e), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }

    private int findMenuItemIndex(JMenu menu, JMenuItem item)
    {
        for (int i = 0; i < menu.getItemCount(); ++i)
        {
            Component comp = menu.getMenuComponent(i);
            if (comp instanceof JMenuItem)
            {
                if (item == (JMenuItem) comp)
                    return i;
            }
        }
        return -1;
    }

    /**
     * Initialize everything for the GUI
     *
     * @return true if successful, false if a fault occurred
     */
    private boolean initialize()
    {
        context.cfg.loadLocale(context.preferences.getLocale());

        if (context.cfg.getPublisherCollectionFilename().length() > 0)
        {
            context.preferences.setLastPublisherIsWorkstation(false);
            context.preferences.setLastPublisherOpenFile(context.cfg.getPublisherCollectionFilename());
            context.preferences.setLastPublisherOpenPath(Utils.getLeftPath(context.cfg.getPublisherCollectionFilename(),
                    Utils.getSeparatorFromPath(context.cfg.getPublisherCollectionFilename())));
        }
        else if (context.cfg.getPublisherLibrariesFileName().length() > 0)
        {
            context.preferences.setLastPublisherIsWorkstation(true);
            context.preferences.setLastPublisherOpenPath(context.cfg.getPublisherLibrariesFileName());
            context.preferences.setLastPublisherOpenPath(Utils.getLeftPath(context.cfg.getPublisherLibrariesFileName(),
                    Utils.getSeparatorFromPath(context.cfg.getPublisherLibrariesFileName())));
        }
        if (context.preferences.getLastPublisherOpenPath().equals(context.preferences.getLastPublisherOpenFile()))
            context.preferences.setLastPublisherOpenPath(context.cfg.getWorkingDirectory());

        if (context.cfg.getSubscriberCollectionFilename().length() > 0)
        {
            context.preferences.setLastSubscriberIsRemote(true);
            context.preferences.setLastSubscriberOpenFile(context.cfg.getSubscriberCollectionFilename());
            context.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(context.cfg.getSubscriberCollectionFilename(),
                    Utils.getSeparatorFromPath(context.cfg.getSubscriberCollectionFilename())));
        }
        else if (context.cfg.getSubscriberLibrariesFileName().length() > 0)
        {
            context.preferences.setLastSubscriberIsRemote(false);
            context.preferences.setLastSubscriberOpenFile(context.cfg.getSubscriberLibrariesFileName());
            context.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(context.cfg.getSubscriberLibrariesFileName(),
                    Utils.getSeparatorFromPath(context.cfg.getSubscriberLibrariesFileName())));
        }
        if (context.preferences.getLastSubscriberOpenPath().equals(context.preferences.getLastSubscriberOpenFile()))
            context.preferences.setLastSubscriberOpenPath(""); //context.cfg.getWorkingDirectory());

        if (context.cfg.isUsingHintTracking())
        {
            context.preferences.setLastHintTrackingIsRemote(context.cfg.getHintsDaemonFilename().length() > 0);
            context.preferences.setLastHintTrackingOpenFile(context.cfg.getHintHandlerFilename());
            context.preferences.setLastHintTrackingOpenPath(Utils.getLeftPath(context.cfg.getHintHandlerFilename(),
                    Utils.getSeparatorFromPath(context.cfg.getHintHandlerFilename())));
            if (context.preferences.getLastHintKeysOpenPath().equals(context.preferences.getLastHintKeysOpenFile()))
                context.preferences.setLastHintTrackingOpenPath(context.cfg.getWorkingDirectory());
        }
        else
        {
            // might be null in existing preferences.json
            if (context.preferences.getLastHintTrackingOpenFile() == null)
                context.preferences.setLastHintTrackingOpenFile("");
            if (context.preferences.getLastHintTrackingOpenPath() == null)
                context.preferences.setLastHintTrackingOpenPath(context.cfg.getWorkingDirectory());
        }

        // setup the needed tools
        context.transfer = new Transfer(context);
        try
        {
            context.transfer.initialize();

            if (context.cfg.getHintKeysFile() != null && context.cfg.getHintKeysFile().length() > 0)
            {
                showHintTrackingButton = true;
                context.preferences.setLastHintKeysOpenFile(context.cfg.getHintKeysFile());
                context.preferences.setLastHintKeysOpenPath(FilenameUtils.getFullPathNoEndSeparator(context.cfg.getHintKeysFile()));
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            context.fault = true;
            return false;
        }

        // setup the GUI
        if (context.main.previousContext != null && context.main.previousContext.navigator != null)
            context.main.secondaryNavigator = true;

        context.mainFrame = new MainFrame(context);
        if (!context.fault)
        {
            if (Utils.isOsMac() && SystemInfo.isMacFullWindowContentSupported)
            {
                context.mainFrame.getRootPane().putClientProperty("apple.awt.fullscreenable", true);
            }

            // setup the Main Menu and primary tabs
            initializeMainMenu();
            context.browser = new Browser(context);
            context.libraries = new LibrariesUI(context);

            // set the GuiLogAppender context for a second invocation
            if (context.main.secondaryInvocation)
            {
                GuiLogAppender appender = context.main.getGuiLogAppender();
                appender.setContext(context);
                // this causes the preBuffer to be appended to the Navigator Log panels
                logger.info(context.cfg.gs("Navigator.secondary,context"));
            }

            // disable back-fill because we never know what combination of items might be selected
            context.cfg.setNoBackFill(true);

            context.cfg.setPreserveDates(context.preferences.isPreserveFileTimes());

            setQuitTerminateVisibility();

            // add any defined bookmarks to the menu
            bookmarks = new Bookmarks();
            loadBookmarksMenu();

            // add any defined jobs to the menu
            loadJobsMenu();

/*
            Thread.setDefaultUncaughtExceptionHandler( (thread, throwable) -> {
                logger.error("GOT IT: " + Utils.getStackTrace(throwable));
            });
*/

        }

        context.savedEnvironment = new SavedEnvironment(context);
        context.savedEnvironment.save();

        return !context.fault;
    }

    private void initializeMainMenu()
    {
        // --- Main Menu ------------------------------------------

        // -- File Menu
        // --------------------------------------------------------

        // --- Open Publisher
        AbstractAction openPublisherAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File file)
                    {
                        if (file.isDirectory())
                            return true;
                        return (file.getName().toLowerCase().endsWith(".json"));
                    }

                    @Override
                    public String getDescription()
                    {
                        return context.cfg.gs("Navigator.menu.Open.publisher.files");
                    }
                });
                fc.setDialogTitle(context.cfg.gs("Navigator.menu.Open.publisher"));
                fc.setFileHidingEnabled(false);
                File ld;
                if (context.preferences.getLastPublisherOpenPath().length() > 0)
                    ld = new File(context.preferences.getLastPublisherOpenPath());
                else
                    ld = new File(context.cfg.getWorkingDirectory());
                if (ld.exists() && ld.isDirectory())
                    fc.setCurrentDirectory(ld);

                if (context.preferences.getLastPublisherOpenFile().length() > 0)
                {
                    File lf = new File(context.preferences.getLastPublisherOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Workstation/Collection radio button accessory
                JPanel jp = new JPanel();
                GridBagLayout layout = new GridBagLayout();

                jp.setLayout(layout);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(context.mainFrame.textFieldLocation.getBorder());

                JLabel lab = new JLabel(context.cfg.gs("Navigator.menu.Open.publisher.system.type"));

                JRadioButton rbCollection = new JRadioButton(context.cfg.gs("Navigator.menu.Open.publisher.collection.radio"));
                rbCollection.setToolTipText(context.cfg.gs("Navigator.menu.Open.publisher.collection.radio.tooltip"));
                rbCollection.setSelected(!context.preferences.isLastPublisherIsWorkstation());

                JRadioButton rbWorkstation = new JRadioButton(context.cfg.gs("Navigator.menu.Open.publisher.workstation.radio"));
                rbWorkstation.setToolTipText(context.cfg.gs("Navigator.menu.Open.publisher.workstation.radio.tooltip"));
                rbWorkstation.setSelected(context.preferences.isLastPublisherIsWorkstation());

                ButtonGroup group = new ButtonGroup();
                group.add(rbCollection);
                group.add(rbWorkstation);

                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 8, 4, 8);
                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.anchor = GridBagConstraints.WEST;
                layout.setConstraints(lab, gbc);
                gbc.gridy = 1;
                layout.setConstraints(rbCollection, gbc);
                gbc.insets = new Insets(0, 8, 0, 8);
                gbc.gridy = 2;
                layout.setConstraints(rbWorkstation, gbc);
                jp.add(lab);
                jp.add(rbCollection);
                jp.add(rbWorkstation);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(context.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        boolean isWorkstation = rbWorkstation.isSelected();
                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.open.error.select.a.file.only"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }

                        if (context.cfg.isRemoteSubscriber())
                        {
                            for (ActionListener listener : context.mainFrame.menuItemCloseSubscriber.getActionListeners())
                            {
                                listener.actionPerformed(new ActionEvent(context.mainFrame.menuItemCloseSubscriber, ActionEvent.ACTION_PERFORMED, null));
                            }
                        }

                        if (context.cfg.isUsingHintTracking() && context.cfg.isRemoteStatusServer())
                        {
                            for (ActionListener listener : context.mainFrame.menuItemCloseHintTracking.getActionListeners())
                            {
                                listener.actionPerformed(new ActionEvent(context.mainFrame.menuItemCloseHintTracking, ActionEvent.ACTION_PERFORMED, null));
                            }
                        }

                        try
                        {
                            context.preferences.setLastPublisherInUse(true);
                            context.preferences.setLastPublisherOpenFile(file.getAbsolutePath());
                            context.preferences.setLastPublisherOpenPath(last.getAbsolutePath());
                            context.preferences.setLastPublisherIsWorkstation(isWorkstation);
                            if (isWorkstation)
                            {
                                context.cfg.setPublisherCollectionFilename("");
                                context.cfg.setPublisherLibrariesFileName(file.getAbsolutePath());
                            }
                            else
                            {
                                context.cfg.setPublisherCollectionFilename(file.getAbsolutePath());
                                context.cfg.setPublisherLibrariesFileName("");
                            }
                            context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                            context.publisherRepo = context.main.readRepo(context, Repository.PUBLISHER, Repository.VALIDATE);
                            context.browser.loadCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false);
                            context.browser.loadSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false);
                            setQuitTerminateVisibility();
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Open.publisher.error.opening.publisher.library") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        context.mainFrame.menuItemOpenPublisher.addActionListener(openPublisherAction);

        // --- Open Subscriber
        AbstractAction openSubscriberAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File file)
                    {
                        if (file.isDirectory())
                            return true;
                        return (file.getName().toLowerCase().endsWith(".json"));
                    }

                    @Override
                    public String getDescription()
                    {
                        return context.cfg.gs("Navigator.menu.Open.subscriber.files");
                    }
                });
                fc.setDialogTitle(context.cfg.gs("Navigator.menu.Open.subscriber"));
                fc.setFileHidingEnabled(false);
                File ld;
                if (context.preferences.getLastSubscriberOpenPath().length() > 0)
                    ld = new File(context.preferences.getLastSubscriberOpenPath());
                else
                    ld = new File(context.cfg.getWorkingDirectory());
                if (ld.exists() && ld.isDirectory())
                    fc.setCurrentDirectory(ld);

                if (context.preferences.getLastSubscriberOpenFile().length() > 0)
                {
                    File lf = new File(context.preferences.getLastSubscriberOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Remote Connection checkbox accessory
                JPanel jp = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                jp.setLayout(gb);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(context.mainFrame.textFieldLocation.getBorder());
                JCheckBox cbIsRemote = new JCheckBox("<html><head><style>body{margin-left:4px;}</style></head><body>" +
                        context.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox") + "</body></html>");
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(context.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox.tooltip"));
                cbIsRemote.setSelected(context.preferences.isLastSubscriberIsRemote());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 0, 0, 8);
                gb.setConstraints(cbIsRemote, gbc);
                jp.add(cbIsRemote);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(context.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        if (cbIsRemote.isSelected() && context.publisherRepo == null)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Open.a.publisher.library.required"), context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }

                        if (context.cfg.isRemoteSubscriber())
                        {
                            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Open.subscriber.close.current.remote.connection"),
                                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                return;

                            disconnectSubscriber();
                        }

                        context.preferences.setLastSubscriberInUse(true);
                        context.preferences.setLastSubscriberOpenFile(file.getAbsolutePath());
                        context.preferences.setLastSubscriberOpenPath(last.getAbsolutePath());
                        context.preferences.setLastSubscriberIsRemote(cbIsRemote.isSelected());

                        // this defines the value returned by context.cfg.isRemoteSession()
                        if (context.preferences.isLastSubscriberIsRemote())
                            context.cfg.setRemoteType("P"); // publisher to remote subscriber
                        else
                            context.cfg.setRemoteType("-"); // not remote

                        context.cfg.setSubscriberLibrariesFileName(file.getAbsolutePath());
                        context.cfg.setSubscriberCollectionFilename("");
                        context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                        if (context.preferences.isLastSubscriberIsRemote())
                        {
                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Transfer.requesting.subscriber.library"));
                            context.mainFrame.repaint();
                            context.mainFrame.labelStatusMiddle.repaint();
                        }

                        SwingUtilities.invokeLater(new Runnable()
                        {
                            public void run()
                            {
                                try
                                {
                                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                    context.mainFrame.repaint();
                                    context.mainFrame.labelStatusMiddle.repaint();

                                    context.subscriberRepo = context.main.readRepo(context, Repository.SUBSCRIBER, !context.preferences.isLastSubscriberIsRemote());

                                    if (context.preferences.isLastSubscriberIsRemote())
                                    {
                                        // start the serveStty client for automation
                                        context.clientStty = new ClientStty(context, false, true);
                                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                                        {
                                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                            disconnectSubscriber();
                                            JOptionPane.showMessageDialog(context.mainFrame,
                                                    context.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                            context.cfg.setRemoteType("-");
                                            context.fault = false;
                                            return;
                                        }

                                        if (context.clientStty.checkBannerCommands())
                                        {
                                            logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
                                        }
                                        context.transfer.requestLibrary();

                                        // start the serveSftp client
                                        context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                                        if (!context.clientSftp.startClient())
                                        {
                                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                            disconnectSubscriber();
                                            JOptionPane.showMessageDialog(context.mainFrame,
                                                    context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                            context.fault = false;
                                            return;
                                        }
                                    }

                                    // load the subscriber library
                                    setQuitTerminateVisibility();
                                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.preferences.isLastSubscriberIsRemote());
                                    context.browser.loadSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.preferences.isLastSubscriberIsRemote());
                                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                }
                                catch (Exception e)
                                {
                                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                    disconnectSubscriber();
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Navigator.menu.Open.subscriber.error.opening.subscriber.library") + e.getMessage(),
                                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    context.fault = false;
                                }
                            }
                        });
                    }
                    break;
                }
            }
        };
        context.mainFrame.menuItemOpenSubscriber.addActionListener(openSubscriberAction);
        if (context.subscriberRepo != null)
            context.preferences.setLastSubscriberIsRemote(context.cfg.isRemoteOperation());

        // --- Open Hint Keys
        AbstractAction openHintKeysAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File file)
                    {
                        if (file.isDirectory())
                            return true;
                        return (file.getName().toLowerCase().endsWith(".keys"));
                    }

                    @Override
                    public String getDescription()
                    {
                        return context.cfg.gs("Navigator.menu.Open.hint.keys.files");
                    }
                });
                fc.setDialogTitle(context.cfg.gs("Navigator.menu.Open.hint.keys"));
                fc.setFileHidingEnabled(false);
                File ld;
                if (context.preferences.getLastHintKeysOpenPath().length() > 0)
                    ld = new File(context.preferences.getLastHintKeysOpenPath());
                else
                    ld = new File(context.cfg.getWorkingDirectory());
                if (ld.exists() && ld.isDirectory())
                    fc.setCurrentDirectory(ld);

                if (context.preferences.getLastHintKeysOpenFile().length() > 0)
                {
                    File lf = new File(context.preferences.getLastHintKeysOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                while (true)
                {
                    int selection = fc.showOpenDialog(context.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }

                        try
                        {
                            context.preferences.setLastHintKeysInUse(true);
                            context.preferences.setLastHintKeysOpenFile(file.getAbsolutePath());
                            context.preferences.setLastHintKeysOpenPath(last.getAbsolutePath());
                            context.cfg.setHintKeysFile(file.getAbsolutePath());
                            context.main.setupHints(context.publisherRepo);
                            if (!showHintTrackingButton)
                            {
                                showHintTrackingButton = true;
                                context.mainFrame.panelHintTracking.setVisible(true);
                                context.mainFrame.buttonHintTracking.doClick();
                            }
                            context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Open.hint.keys.error.opening.hint.keys") + e.getMessage(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        context.mainFrame.menuItemOpenHintKeys.addActionListener(openHintKeysAction);

        // --- Open Hint Tracking
        AbstractAction openHintTrackingAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.cfg.getHintKeysFile() == null || context.cfg.getHintKeysFile().length() == 0)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Open.hint.tracking.please.open.hints.keys.first"), context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                    return;
                }

                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileFilter()
                {
                    @Override
                    public boolean accept(File file)
                    {
                        if (file.isDirectory())
                            return true;
                        return (file.getName().toLowerCase().endsWith(".json"));
                    }

                    @Override
                    public String getDescription()
                    {
                        return context.cfg.gs("Navigator.menu.Open.hint.tracking.files");
                    }
                });
                fc.setDialogTitle(context.cfg.gs("Navigator.menu.Open.hint.tracking"));
                fc.setFileHidingEnabled(false);
                File ld;
                if (context.preferences.getLastHintTrackingOpenPath().length() > 0)
                    ld = new File(context.preferences.getLastHintTrackingOpenPath());
                else
                    ld = new File(context.cfg.getWorkingDirectory());

                if (ld.exists() && ld.isDirectory())
                    fc.setCurrentDirectory(ld);

                if (context.preferences.getLastHintTrackingOpenFile().length() > 0)
                {
                    File lf = new File(context.preferences.getLastHintTrackingOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Remote Hint Status Server checkbox accessory
                JPanel jp = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                jp.setLayout(gb);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(context.mainFrame.textFieldLocation.getBorder());
                JCheckBox cbIsRemote = new JCheckBox("<html><head><style>body{margin-left:4px;}</style></head><body>" +
                        context.cfg.gs("Navigator.menu.Open.hint.tracking.checkbox") + "</body></html>");
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(context.cfg.gs("Navigator.menu.Open.hint.tracking.checkbox.tooltip"));
                cbIsRemote.setSelected(context.preferences.isLastHintTrackingIsRemote());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 0, 0, 8);
                gb.setConstraints(cbIsRemote, gbc);
                jp.add(cbIsRemote);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(context.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        if (cbIsRemote.isSelected() && context.publisherRepo == null)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.menu.Open.a.publisher.library.required"), context.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }

                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }

                        if (context.cfg.isUsingHintTracking() && context.cfg.isRemoteStatusServer())
                        {
                            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Open.hint.tracking.close.current.status.server"),
                                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                return;

                            quitByeRemotes(false, true);
                        }

                        try
                        {
                            context.preferences.setLastHintTrackingInUse(true);
                            context.preferences.setLastHintTrackingOpenFile(file.getAbsolutePath());
                            context.preferences.setLastHintTrackingOpenPath(last.getAbsolutePath());
                            context.preferences.setLastHintTrackingIsRemote(cbIsRemote.isSelected());

                            if (context.preferences.isLastHintTrackingIsRemote())
                            {
                                context.cfg.setHintsDaemonFilename(file.getAbsolutePath());
                                context.cfg.setHintTrackerFilename("");
                            }
                            else
                            {
                                context.cfg.setHintsDaemonFilename("");
                                context.cfg.setHintTrackerFilename(file.getAbsolutePath());
                            }

                            // connect to the hint tracker or status server
                            context.main.setupHints(context.publisherRepo);
                            context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                            context.browser.toggleHints(true);
                            setQuitTerminateVisibility();
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame, e.getMessage(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        context.mainFrame.menuItemOpenHintTracking.addActionListener(openHintTrackingAction);

        // --- Close ...
        context.mainFrame.menuItemClose.addMenuListener(new MenuListener()
        {
            @Override
            public void menuCanceled(MenuEvent menuEvent)
            {
            }

            @Override
            public void menuDeselected(MenuEvent menuEvent)
            {
            }

            @Override
            public void menuSelected(MenuEvent menuEvent)
            {
                if (context.publisherRepo != null && context.publisherRepo.isInitialized())
                {
                    context.mainFrame.menuItemClosePublisher.setVisible(true);
                    if (context.cfg.isRemoteActive())
                    {
                        context.mainFrame.menuItemClosePublisher.setEnabled(false);
                        context.mainFrame.menuItemClosePublisher.setToolTipText(context.cfg.gs("Navigator.menu.Open.a.publisher.library.required"));
                    }
                    else
                    {
                        context.mainFrame.menuItemClosePublisher.setEnabled(true);
                        context.mainFrame.menuItemClosePublisher.setToolTipText(context.cfg.gs(""));
                    }
                }
                else
                    context.mainFrame.menuItemClosePublisher.setVisible(false);

                if (context.subscriberRepo != null && context.subscriberRepo.isInitialized())
                    context.mainFrame.menuItemCloseSubscriber.setVisible(true);
                else
                    context.mainFrame.menuItemCloseSubscriber.setVisible(false);

                if (context.hintKeys != null && context.cfg.getHintKeysFile().length() > 0)
                    context.mainFrame.menuItemCloseHintKeys.setVisible(true);
                else
                    context.mainFrame.menuItemCloseHintKeys.setVisible(false);

                if (context.statusRepo != null && context.statusRepo.isInitialized())
                {
                    if (context.cfg.isRemoteStatusServer())
                        context.mainFrame.menuItemCloseHintTracking.setText(context.cfg.gs("Z.hint.server") + " ...");
                    else
                        context.mainFrame.menuItemCloseHintTracking.setText(context.cfg.gs("Z.hint.tracker") + " ...");
                    context.mainFrame.menuItemCloseHintTracking.setVisible(true);
                }
                else
                    context.mainFrame.menuItemCloseHintTracking.setVisible(false);
            }
        });

        // --- Close Publisher ...
        context.mainFrame.menuItemClosePublisher.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                int r = JOptionPane.showConfirmDialog(context.mainFrame,
                        context.cfg.gs("Z.are.you.sure.you.want.to.close") + context.cfg.gs("Z.publisher"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION)
                {
                    NavTreeNode root = context.browser.setCollectionRoot(null, context.mainFrame.treeCollectionOne, context.cfg.gs("Browser.open.a.publisher"), false);
                    root.loadTable();
                    root = context.browser.setCollectionRoot(null, context.mainFrame.treeSystemOne, context.cfg.gs("Browser.open.a.publisher"), false);
                    root.loadTable();
                    context.publisherRepo = null;
                    context.cfg.setPublisherCollectionFilename("");
                    context.cfg.setPublisherLibrariesFileName("");
                    context.preferences.setLastPublisherInUse(false);
                    setQuitTerminateVisibility();
                }
            }
        });

        // --- Close Subscriber ...
        context.mainFrame.menuItemCloseSubscriber.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                int r = JOptionPane.showConfirmDialog(context.mainFrame,
                        context.cfg.gs("Z.are.you.sure.you.want.to.close") + context.cfg.gs("Z.subscriber"),
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION)
                {
                    disconnectSubscriber();
                }
            }
        });

        // --- Close Hint Keys ...
        context.mainFrame.menuItemCloseHintKeys.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                int r;
                if (context.statusRepo != null && context.statusRepo.isInitialized())
                {
                    r = JOptionPane.showConfirmDialog(context.mainFrame,
                            context.cfg.gs("Z.are.you.sure.you.want.to.quit.and.close") + context.cfg.gs("Z.hint.key"),
                            context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                }
                else
                {
                    r = JOptionPane.showConfirmDialog(context.mainFrame,
                            context.cfg.gs("Z.are.you.sure.you.want.to.close") + context.cfg.gs("Z.hint.key"),
                            context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                }
                if (r == JOptionPane.YES_OPTION)
                {
                    // close Hint Keys and Hint Tracker/Server
                    quitByeRemotes(false, true);
                    context.hints = null;
                    context.hintKeys = null;
                    context.cfg.setHintKeysFile("");
                    context.preferences.setLastHintKeysInUse(false);
                    context.statusRepo = null;
                    context.cfg.setHintTrackerFilename("");
                    context.cfg.setHintsDaemonFilename("");
                    context.preferences.setLastHintTrackingInUse(false);
                    showHintTrackingButton = context.browser.resetHintTrackingButton();
                }
            }
        });

        // --- Close Hint Tracking ...
        context.mainFrame.menuItemCloseHintTracking.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                int r = JOptionPane.showConfirmDialog(context.mainFrame,
                        context.cfg.gs("Z.are.you.sure.you.want.to.close") +
                                (context.cfg.isRemoteStatusServer() ?
                                        context.cfg.gs("Z.hint.server") : context.cfg.gs("Z.hint.tracker")) + "?",
                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION)
                {
                    // close Hint Tracker/Server
                    quitByeRemotes(false, true);
                    context.statusRepo = null;
                    context.cfg.setHintTrackerFilename("");
                    context.cfg.setHintsDaemonFilename("");
                    context.preferences.setLastHintTrackingInUse(false);
                    showHintTrackingButton = context.browser.resetHintTrackingButton();
                    setQuitTerminateVisibility();
                }
            }
        });

        // --- Quit & Stop Remote(s)
        context.mainFrame.menuItemQuitTerminate.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.verifyClose())
                {
                    if (context.clientStty != null)
                    {
                        int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                context.cfg.gs("Navigator.menu.QuitTerminate.stop.subscriber"),
                                context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                            quitRemoteSubscriber = false;
                        else
                            quitRemoteSubscriber = true;
                    }

                    if (context.cfg.getHintsDaemonFilename() != null && context.cfg.getHintsDaemonFilename().length() > 0)
                    {
                        int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                context.cfg.gs("Navigator.menu.QuitTerminate.stop.hint.status.server"),
                                context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                        if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                            quitRemoteHintStatusServer = false;
                        else
                            quitRemoteHintStatusServer = true;
                    }

                    stop();
                }
            }
        });

        // --- Quit
        // Handled in MainFrame.menuItemFileQuitActionPerformed()

        // -- Edit Menu
        // --------------------------------------------------------

        // --- Copy
        ActionListener copyAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    if (context.browser.lastComponent != null)
                    {
                        ActionEvent ev = new ActionEvent(context.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "copy");
                        context.browser.lastComponent.requestFocus();
                        context.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                    }
                }
            }
        };
        context.mainFrame.menuItemCopy.addActionListener(copyAction);
        context.mainFrame.popupMenuItemCopy.addActionListener(copyAction);

        // --- Cut
        ActionListener cutAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    if (context.browser.lastComponent != null)
                    {
                        ActionEvent ev = new ActionEvent(context.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "cut");
                        context.browser.lastComponent.requestFocus();
                        context.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                    }
                }
            }
        };
        context.mainFrame.menuItemCut.addActionListener(cutAction);
        context.mainFrame.popupMenuItemCut.addActionListener(cutAction);

        // --- Paste
        ActionListener pasteAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    if (context.browser.lastComponent != null)
                    {
                        ActionEvent ev = new ActionEvent(context.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "paste");
                        context.browser.lastComponent.requestFocus();
                        context.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                    }
                }
            }
        };
        context.mainFrame.menuItemPaste.addActionListener(pasteAction);
        context.mainFrame.popupMenuItemPaste.addActionListener(pasteAction);

        // --- Delete
        ActionListener deleteAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    Object object = context.browser.lastComponent;
                    if (object instanceof JTree)
                    {
                        JTree sourceTree = (JTree) object;
                        context.browser.deleteSelected(sourceTree);
                    }
                    else if (object instanceof JTable)
                    {
                        JTable sourceTable = (JTable) object;
                        context.browser.deleteSelected(sourceTable);
                    }
                }
            }
        };
        context.mainFrame.menuItemDelete.addActionListener(deleteAction);
        context.mainFrame.popupMenuItemDelete.addActionListener(deleteAction);

        // --- Find in Log
        AbstractAction findAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    String name;
                    lastFindTab = context.mainFrame.tabbedPaneMain.getSelectedIndex();
                    if (lastFindTab == 0)
                        name = context.cfg.gs("Navigator.splitPane.Browser.tab.title");
                    else if (lastFindTab == 1)
                        name = "";
                    else
                    {
                        lastFindTab = -1;
                        return;
                    }

                    if (context.mainFrame.textAreaLog.getSelectedText() != null && context.mainFrame.textAreaLog.getSelectedText().length() > 0)
                        lastFindString = context.mainFrame.textAreaLog.getSelectedText();

                    Object obj = JOptionPane.showInputDialog(context.mainFrame,
                            context.cfg.gs("Navigator.popupMenuItemFind.title"),
                            name, JOptionPane.QUESTION_MESSAGE,
                            null, null, lastFindString);
                    lastFindString = (String) obj;
                    if (lastFindString != null && lastFindString.length() > 0)
                    {
                        String content;
                        //if (lastFindTab == 0)
                        content = context.mainFrame.textAreaLog.getText().toLowerCase();
                        lastFindPosition = content.indexOf(lastFindString.toLowerCase(), 0);
                        if (lastFindPosition > 0)
                        {
                            if (lastFindTab == 0)
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                                context.mainFrame.textAreaLog.requestFocus();
                                context.mainFrame.textAreaLog.setSelectionStart(lastFindPosition);
                                context.mainFrame.textAreaLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                            }
                            lastFindPosition += lastFindString.length();
                        }
                    }
                }
            }
        };
        context.mainFrame.menuItemFind.addActionListener(findAction);
        context.mainFrame.popupMenuItemFind.addActionListener(findAction);

        // --- Find Next
        AbstractAction findNextAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    if (lastFindTab < 0 || lastFindString == null || lastFindString.length() == 0)
                        return;
                    String content;
                    //if (lastFindTab == 0)
                    content = context.mainFrame.textAreaLog.getText().toLowerCase();
                    if (content != null && content.length() > 0)
                    {
                        lastFindPosition = content.indexOf(lastFindString.toLowerCase(), lastFindPosition);
                        if (lastFindPosition > 0)
                        {
                            if (lastFindTab == 0)
                            {
                                context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                                context.mainFrame.textAreaLog.requestFocus();
                                try
                                {
                                    Rectangle rect = context.mainFrame.textAreaLog.modelToView(lastFindPosition);
                                    context.mainFrame.textAreaLog.scrollRectToVisible(rect);
                                }
                                catch (Exception e)
                                {
                                    System.out.println("bad scroll position");
                                }
                                context.mainFrame.textAreaLog.setSelectionStart(lastFindPosition);
                                context.mainFrame.textAreaLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                            }
                            lastFindPosition += lastFindString.length();
                        }
                    }
                }
            }
        };
        context.mainFrame.menuItemFindNext.addActionListener(findNextAction);
        context.mainFrame.popupMenuItemFindNext.addActionListener(findNextAction);

        // --- New Folder
        AbstractAction newFolderAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    boolean tooMany = false;
                    JTree tree = null;
                    NavTreeUserObject tuo = null;
                    Object object = context.browser.lastComponent;
                    if (object instanceof JTree)
                    {
                        tree = (JTree) object;
                    }
                    else if (object instanceof JTable)
                    {
                        tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
                    }
                    assert (tree != null);

                    TreePath[] paths = tree.getSelectionPaths();
                    if (paths.length == 1)
                    {
                        NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                        tuo = ntn.getUserObject();
                    }
                    else if (paths.length == 0)
                        return;
                    else
                        tooMany = true;

                    if (!tooMany)
                    {
                        // select source in library
                        String path = selectLibrarySource(tuo);
                        if (path == null || path.length() < 1)
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.New.folder.cannot.create.new.folder.in.current.location"),
                                    context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                            return;
                        }
                        else if (path.equals("_cancelled_"))
                            return;

                        boolean error = false;
                        String reply = "";
                        if (path.length() > 0)
                        {
                            reply = JOptionPane.showInputDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.New.folder.for") + path + ": ",
                                    context.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE);
                            if (reply != null && reply.length() > 0)
                            {
                                NavTreeUserObject createdTuo = null;
                                try
                                {
                                    path = path + Utils.getSeparatorFromPath(path) + reply;
                                    String msg = context.cfg.gs("Navigator.menu.New.folder.creating") +
                                            (tuo.isRemote ? context.cfg.gs("Z.remote.lowercase") + " " : "") +
                                            context.cfg.gs("Navigator.menu.New.folder.directory") + ": " + path;
                                    logger.info(msg);

                                    if (context.transfer.makeDirs((tuo.isRemote ? path + Utils.getSeparatorFromPath(path) + "dummyfile.els" : path), true, tuo.isRemote))
                                    {
                                        // make tuo and add node
                                        NavTreeNode createdNode = new NavTreeNode(context, tuo.node.getMyRepo(), tree);
                                        if (tuo.isRemote)
                                        {
                                            Thread.sleep(500L); // give the remote time to register new hint file
                                            SftpATTRS attrs = context.clientSftp.stat(path);
                                            createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(path, null),
                                                    path, attrs.getSize(), attrs.getMTime(), true);
                                        }
                                        else
                                        {
                                            createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(path, null), new File(path));
                                        }
                                        createdNode.setNavTreeUserObject(createdTuo);
                                        createdNode.setAllowsChildren(true);
                                        createdNode.setVisible(true);
                                        tuo.node.add(createdNode);
                                    }
                                    else
                                    {
                                        error = true;
                                        logger.error(context.cfg.gs("Navigator.menu.New.folder.directory.not.created.check.permissions"));
                                        JOptionPane.showMessageDialog(context.mainFrame,
                                                context.cfg.gs("Navigator.menu.New.folder.directory.not.created.check.permissions"),
                                                context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                                    }
                                }
                                catch (Exception e)
                                {
                                    logger.error(Utils.getStackTrace(e));
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Navigator.menu.New.folder.error.creating") +
                                                    (tuo.isRemote ? context.cfg.gs("Z.remote.lowercase") + " " : "") +
                                                    context.cfg.gs("Navigator.menu.New.folder.directory") + ": " +
                                                    e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    error = true;
                                }

                                if (!error)
                                {
                                    context.browser.refreshByObject(tree);
                                    if (object instanceof JTree)
                                        tuo.node.selectMe();
                                    else
                                    {
                                        // update table & select relevant row
                                        tuo.node.loadTable();
                                        if (createdTuo != null)
                                        {
                                            int row = context.browser.findRowIndex((JTable) object, createdTuo);
                                            if (row > -1)
                                            {
                                                ((JTable) object).setRowSelectionInterval(row, row);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(context.mainFrame,
                                context.cfg.gs("Navigator.menu.New.folder.please.select.a.single.destination.for.a.new.folder"),
                                context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        context.mainFrame.menuItemNewFolder.addActionListener(newFolderAction);
        context.mainFrame.popupMenuItemNewFolder.addActionListener(newFolderAction);

        // --- Rename
        ActionListener renameAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    int[] rows = {0};
                    boolean tooMany = false;
                    JTree tree = null;
                    NavTreeUserObject tuo = null;
                    Object object = context.browser.lastComponent;
                    if (object instanceof JTree)
                    {
                        tree = (JTree) object;
                        TreePath[] paths = tree.getSelectionPaths();
                        if (paths.length == 1)
                        {
                            NavTreeNode ntn = (NavTreeNode) paths[0].getLastPathComponent();
                            tuo = ntn.getUserObject();
                        }
                        else if (paths.length == 0)
                            return;
                        else
                            tooMany = true;
                    }
                    else if (object instanceof JTable)
                    {
                        tree = context.browser.navTransferHandler.getTargetTree((JTable) object);
                        rows = ((JTable) object).getSelectedRows();
                        if (rows.length == 1)
                        {
                            tuo = (NavTreeUserObject) ((JTable) object).getValueAt(rows[0], 1);
                        }
                        else if (rows.length == 0)
                            return;
                        else
                            tooMany = true;
                    }

                    if (!tooMany)
                    {
                        String name = "";
                        String path = "";
                        if (tuo.type == NavTreeUserObject.REAL)
                        {
                            name = tuo.name;
                            path = tuo.path;
                        }
                        else
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Rename.cannot.rename.current.location"),
                                    context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                            return;
                        }

                        String reply = name;
                        if (path.length() > 0)
                        {
                            Object obj = JOptionPane.showInputDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Rename") + name + " " +
                                            context.cfg.gs("Navigator.menu.Rename.to"),
                                    context.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE,
                                    null, null, reply);
                            reply = (String) obj;
                            if (reply != null && reply.length() > 0)
                            {
                                try
                                {
                                    String to = Utils.getLeftPath(path, null);
                                    to = to + Utils.getSeparatorFromPath(path) + reply;
                                    context.transfer.rename(path, to, tuo.isRemote);

                                    NavTreeUserObject orig = (NavTreeUserObject) tuo.clone();
                                    orig.node = tuo.node;

                                    tuo.path = to;
                                    tuo.name = reply;
                                    if (tuo.file != null)
                                    {
                                        tuo.file = new File(to);
                                    }

                                    try
                                    {
                                        ((NavTransferHandler) tree.getTransferHandler()).exportHint("mv", orig, tuo);
                                    }
                                    catch (Exception e)
                                    {
                                        logger.error(Utils.getStackTrace(e));
                                        JOptionPane.showMessageDialog(context.mainFrame,
                                                context.cfg.gs("Navigator.error.writing.hint") + "  " +
                                                        e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    }

                                    context.browser.refreshByObject(tree);
                                    if (object instanceof JTree)
                                        tuo.node.selectMe();
                                    else
                                        ((JTable) object).setRowSelectionInterval(rows[0], rows[0]);
                                }
                                catch (Exception e)
                                {
                                    logger.error(Utils.getStackTrace(e));
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Navigator.menu.Rename.error.renaming") +
                                                    (tuo.isRemote ? context.cfg.gs("Z.remote.lowercase") +
                                                            " " : "") + name + ": " + e.getMessage(),
                                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(context.mainFrame,
                                context.cfg.gs("Navigator.menu.Rename.please.select.a.single.item.to.be.renamed"),
                                context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        };
        context.mainFrame.menuItemRename.addActionListener(renameAction);
        context.mainFrame.popupMenuItemRename.addActionListener(renameAction);

        // --- Touch Date/Time
        ActionListener touchAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Object object = context.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    context.browser.touchSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    context.browser.touchSelected(sourceTable);
                }
            }
        };
        context.mainFrame.menuItemTouch.addActionListener(touchAction);
        context.mainFrame.popupMenuItemTouch.addActionListener(touchAction);

        // -- View Menu
        // --------------------------------------------------------

        // --- Refresh
        ActionListener refreshAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    context.browser.rescanByTreeOrTable(context.browser.lastComponent); // same as F5
                }
            }
        };
        context.mainFrame.menuItemRefresh.addActionListener(refreshAction);
        context.mainFrame.popupMenuItemRefresh.addActionListener(refreshAction);

        // --- Progress
        context.mainFrame.menuItemProgress.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.progress == null || !context.progress.isBeingUsed())
                {
                    ActionListener cancel = new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            // noop
                        }
                    };
                    context.progress = new Progress(context, context.mainFrame, cancel, false);
                }
                context.progress.view();
            }
        });

        // --- Auto-Refresh
        context.mainFrame.menuItemAutoRefresh.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.preferences.setAutoRefresh(!context.preferences.isAutoRefresh());
                if (context.preferences.isAutoRefresh())
                    context.mainFrame.menuItemAutoRefresh.setSelected(true);
                else
                    context.mainFrame.menuItemAutoRefresh.setSelected(false);
            }
        });
        // set initial state of Auto-Refresh checkbox
        if (context.preferences.isAutoRefresh())
            context.mainFrame.menuItemAutoRefresh.setSelected(true);
        else
            context.mainFrame.menuItemAutoRefresh.setSelected(false);

        // --- Show Hidden
        context.mainFrame.menuItemShowHidden.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    context.browser.toggleShowHiddenFiles();
                }
            }
        });
        // set initial state of Show Hidden checkbox
        if (context.preferences.isHideHiddenFiles())
            context.mainFrame.menuItemShowHidden.setSelected(false);
        else
            context.mainFrame.menuItemShowHidden.setSelected(true);

        // --- Word Wrap Log
        ActionListener wordWrapAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.mainFrame.tabbedPaneMain.getSelectedIndex() == 0)
                {
                    boolean selected = false;
                    if (actionEvent.getSource() == context.mainFrame.menuItemWordWrap)
                        selected = context.mainFrame.menuItemWordWrap.isSelected();
                    if (actionEvent.getSource() == context.mainFrame.popupCheckBoxMenuItemWordWrap)
                        selected = context.mainFrame.popupCheckBoxMenuItemWordWrap.isSelected();
                    context.mainFrame.menuItemWordWrap.setSelected(selected);
                    context.mainFrame.popupCheckBoxMenuItemWordWrap.setSelected(selected);
                    context.mainFrame.textAreaLog.setLineWrap(context.mainFrame.menuItemWordWrap.isSelected());
                }
            }
        };
        // set initial state of Word Wrap Log
        context.mainFrame.menuItemWordWrap.setSelected(true);
        context.mainFrame.popupCheckBoxMenuItemWordWrap.setSelected(true);
        context.mainFrame.menuItemWordWrap.addActionListener(wordWrapAction);
        context.mainFrame.popupCheckBoxMenuItemWordWrap.addActionListener(wordWrapAction);

        // -- Bookmarks Menu
        // --------------------------------------------------------

        // --- Bookmark Add Current Location
        context.mainFrame.menuItemAddBookmark.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Object object = context.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    context.browser.bookmarkSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    context.browser.bookmarkSelected(sourceTable);
                }
            }
        });

        // --- Bookmarks Delete
        context.mainFrame.menuItemBookmarksDelete.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String message = context.cfg.gs("Browser.select.one.or.more.bookmarks.to.delete");
                JList<String> names = new JList<String>();
                DefaultListModel<String> listModel = new DefaultListModel<String>();
                names.setModel(listModel);
                names.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                bookmarks.sort();
                for (int i = 0; i < bookmarks.size(); ++i)
                {
                    Bookmark bm = bookmarks.get(i);
                    listModel.addElement(bm.name);
                }

                JScrollPane pane = new JScrollPane();
                pane.setViewportView(names);
                names.requestFocus();
                Object[] params = {message, pane};

                int opt = JOptionPane.showConfirmDialog(context.mainFrame, params, context.cfg.gs("Navigator.delete.bookmarks"), JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.OK_OPTION)
                {
                    int[] selected = names.getSelectedIndices();
                    if (selected != null && selected.length > 0)
                    {
                        for (int i = selected.length - 1; i > -1; --i)
                        {
                            bookmarks.delete(selected[i]);
                        }

                        try
                        {
                            bookmarks.write();
                            loadBookmarksMenu();
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Browser.error.saving.bookmarks") + e.getMessage(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        // -- Tools Menu
        // --------------------------------------------------------

        // --- Duplicate Finder
        context.mainFrame.menuItemDuplicates.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogDuplicateFinder == null || !dialogDuplicateFinder.isShowing())
                {
                    dialogDuplicateFinder = new DuplicateFinderUI(context.mainFrame, context);
                    dialogDuplicateFinder.setVisible(true);
                }
                else
                {
                    dialogDuplicateFinder.toFront();
                    dialogDuplicateFinder.requestFocus();
                }
            }
        });

        // --- Empty Directory Finder
        context.mainFrame.menuItemEmptyFinder.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogEmptyDirectoryFinder == null || !dialogEmptyDirectoryFinder.isShowing())
                {
                    dialogEmptyDirectoryFinder = new EmptyDirectoryFinderUI(context.mainFrame, context);
                    dialogEmptyDirectoryFinder.setVisible(true);
                }
                else
                {
                    dialogEmptyDirectoryFinder.toFront();
                    dialogEmptyDirectoryFinder.requestFocus();
                }
            }
        });

        // --- Junk Remover Tool
        context.mainFrame.menuItemJunk.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogJunkRemover == null || !dialogJunkRemover.isShowing())
                {
                    dialogJunkRemover = new JunkRemoverUI(context.mainFrame, context);
                    dialogJunkRemover.setVisible(true);
                }
                else
                {
                    dialogJunkRemover.toFront();
                    dialogJunkRemover.requestFocus();
                }
            }
        });

        // --- Operations Tool
        context.mainFrame.menuItemOperations.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogOperations == null || !dialogOperations.isShowing())
                {
                    dialogOperations = new OperationsUI(context.mainFrame, context);
                    dialogOperations.setVisible(true);
                }
                else
                {
                    dialogOperations.toFront();
                    dialogOperations.requestFocus();
                }
            }
        });

        // --- Renamer Tool
        context.mainFrame.menuItemRenamer.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogRenamer == null || !dialogRenamer.isShowing())
                {
                    dialogRenamer = new RenamerUI(context.mainFrame, context);
                    dialogRenamer.setVisible(true);
                }
                else
                {
                    dialogRenamer.toFront();
                    dialogRenamer.requestFocus();
                }
            }
        });

        // --- Sleep Tool
        context.mainFrame.menuItemSleep.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogSleep == null || !dialogSleep.isShowing())
                {
                    dialogSleep = new SleepUI(context.mainFrame, context);
                    dialogSleep.setVisible(true);
                }
                else
                {
                    dialogSleep.toFront();
                    dialogSleep.requestFocus();
                }
            }
        });

        // -- Jobs Menu
        // --------------------------------------------------------

        // --- Manage
        context.mainFrame.menuItemJobsManage.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogJobs == null || !dialogJobs.isShowing())
                {
                    dialogJobs = new JobsUI(context.mainFrame, context);
                    dialogJobs.setVisible(true);
                }
                else
                {
                    dialogJobs.toFront();
                    dialogJobs.requestFocus();
                }
            }
        });

        // see loadJobsMenu() for submenu of list of Jobs

        // -- System Menu
        // --------------------------------------------------------

        context.mainFrame.menuItemAuthKeys.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, FileEditor.EditorTypes.Authentication);
            }
        });

        context.mainFrame.menuItemHintKeys.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, FileEditor.EditorTypes.HintKeys);
            }
        });

        context.mainFrame.menuItemBlacklist.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, FileEditor.EditorTypes.BlackList);
            }
        });

        context.mainFrame.menuItemWhitelist.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, FileEditor.EditorTypes.WhiteList);
            }
        });

        // --- Generate
        AbstractAction generateAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Generator generator = new Generator(context);
                String configName = Configuration.NAVIGATOR_NAME;
                generator.showDialog(context.mainFrame, null, configName);
            }
        };
        context.mainFrame.menuItemGenerate.addActionListener(generateAction);

        // --- Save Layout
        AbstractAction saveLayoutAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    context.preferences.write(context);
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.preferences.saved"));
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Save.layout.error.saving.layout") + e.getMessage(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        context.mainFrame.menuItemSaveLayout.addActionListener(saveLayoutAction);

        // --- Settings
        if (Utils.isOsMac())
        {
            FlatDesktop.setPreferencesHandler( () -> {
                if (dialogSettings == null || !dialogSettings.isShowing())
                {
                    dialogSettings = new Settings(context.mainFrame, context);
                    dialogSettings.setVisible(true);
                }
                else
                {
                    dialogSettings.toFront();
                    dialogSettings.requestFocus();
                }
            });

            for (Component comp : context.mainFrame.menuSystem.getComponents())
            {
                if (comp instanceof  JSeparator && ((JSeparator)comp).getName().equalsIgnoreCase("separatorSettings")) {
                    ((JSeparator)comp).setVisible(false);
                }
            }
            context.mainFrame.menuItemSettings.setVisible(false);
        }
        else
        {
            context.mainFrame.menuItemSettings.addActionListener(new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    if (dialogSettings == null || !dialogSettings.isShowing())
                    {
                        dialogSettings = new Settings(context.mainFrame, context);
                        dialogSettings.setVisible(true);
                    }
                    else
                    {
                        dialogSettings.toFront();
                        dialogSettings.requestFocus();
                    }
                }
            });
        }

        // -- Window Menu
        // --------------------------------------------------------

        // --- Maximize
        context.mainFrame.menuItemMaximize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.setExtendedState(context.mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });

        // --- Minimize
        context.mainFrame.menuItemMinimize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.setState(JFrame.ICONIFIED);
            }
        });

        // --- Restore
        context.mainFrame.menuItemRestore.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.setExtendedState(JFrame.NORMAL);
            }
        });

        // --- Split Horizontal
        context.mainFrame.menuItemSplitHorizontal.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.tabbedPaneBrowserOne.setVisible(true);
                context.mainFrame.tabbedPaneBrowserTwo.setVisible(true);
                int size = context.mainFrame.splitPaneTwoBrowsers.getHeight();
                context.mainFrame.splitPaneTwoBrowsers.setOrientation(JSplitPane.VERTICAL_SPLIT);
                context.mainFrame.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // --- Split Vertical
        context.mainFrame.menuItemSplitVertical.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.tabbedPaneBrowserOne.setVisible(true);
                context.mainFrame.tabbedPaneBrowserTwo.setVisible(true);
                int size = context.mainFrame.splitPaneTwoBrowsers.getWidth();
                context.mainFrame.splitPaneTwoBrowsers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                context.mainFrame.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // -- Help Menu
        // --------------------------------------------------------

        // --- Controls
        context.mainFrame.menuItemControls.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Settings.date.format.help.title"), "controls_" + context.preferences.getLocale() + ".html");
                if (!dialog.fault)
                {
                    dialog.setTitle(context.cfg.gs("Navigator.controls.help.title"));
                    dialog.setVisible(true);
                }
            }
        });

        // --- Discussions
        context.mainFrame.menuItemDiscussions.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/Corionis/ELS/discussions");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- Documentation
        context.mainFrame.menuItemDocumentation.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/Corionis/ELS/wiki");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- Getting Started
        context.mainFrame.menuItemGettingStarted.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.getting.started"), "gettingstarted_" + context.preferences.getLocale() + ".html");
                if (!dialog.fault)
                {
                    dialog.setTitle(context.cfg.gs("Navigator.getting.started"));
                    dialog.setVisible(true);
                }
            }
        });

        // --- GitHub Project
        context.mainFrame.menuItemGitHubProject.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/Corionis/ELS");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- Submit Issue
        context.mainFrame.menuItemIssue.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/Corionis/ELS/issues");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // --- Changelist
        context.mainFrame.menuItemChangelist.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Settings.date.format.help.title"), "changes_" + context.preferences.getLocale() + ".html");
                if (!dialog.fault)
                {
                    dialog.setTitle(context.cfg.gs("Navigator.changes.help.title"));
                    dialog.setVisible(true);
                }
            }
        });

        // --- Release Notes
        context.mainFrame.menuItemReleaseNotes.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp helpDialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.release.notes"), "releasenotes_" + context.preferences.getLocale() + ".html");
                if (!helpDialog.fault)
                {
                    helpDialog.setModal(true);
                    Point loc = context.mainFrame.getLocation();
                    loc.x = loc.x + (context.mainFrame.getWidth() / 2) - (helpDialog.getWidth() / 2);
                    loc.y = loc.y + (context.mainFrame.getHeight() / 2) - (helpDialog.getHeight() / 2);
                    helpDialog.setLocation(loc);
                    helpDialog.setVisible(true);
                }
            }
        });

        // --- Check for Updates
        context.mainFrame.menuItemUpdates.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                checkForUpdates(false);
            }
        });

        // --- About
        if (Utils.isOsMac())
        {
            FlatDesktop.setAboutHandler( () -> {
                About about = new About(context.mainFrame, context);
                about.setVisible(true);
            });
            context.mainFrame.menuItemAbout.setVisible(false);
        }
        else
        {
            context.mainFrame.menuItemAbout.addActionListener(new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    About about = new About(context.mainFrame, context);
                    about.setVisible(true);
                }
            });
        }


        // -- popup menu browser log tab
        // --------------------------------------------------------

        // --- Bottom
        context.mainFrame.popupMenuItemBottom.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = context.mainFrame.scrollPaneLog.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });

        // --- Clear
        context.mainFrame.popupMenuItemClear.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.mainFrame.textAreaLog.setText("");
            }
        });

        // --- Top
        context.mainFrame.popupMenuItemTop.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = context.mainFrame.scrollPaneLog.getVerticalScrollBar();
                vertical.setValue(0);
            }
        });
    }

    public boolean isUpdaterProcess()
    {
        return updaterProcess;
    }

    public void loadBookmarksMenu()
    {
        JMenu menu = context.mainFrame.menuBookmarks;
        int count = menu.getItemCount();

        // A -3 offset for Add, Delete and separator
        if (count > 3)
        {
            for (int i = count - 1; i > 2; --i)
            {
                menu.remove(i);
            }
        }

        readBookmarks();
        count = bookmarks.size();
        if (count > 0)
        {
            bookmarks.sort();
            for (int i = 0; i < count; ++i)
            {
                JMenuItem item = new JMenuItem(bookmarks.get(i).name);
                item.setHorizontalAlignment(SwingConstants.LEADING);
                item.setHorizontalTextPosition(SwingConstants.TRAILING);
                item.setMargin(new Insets(2, 18, 2, 2));
                item.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        JMenuItem selected = (JMenuItem) actionEvent.getSource();
                        String name = selected.getText();
                        // A -3 offset for Add, Delete and separator
                        int index = findMenuItemIndex(context.mainFrame.menuBookmarks, selected) - 3;
                        if (index >= 0 && index < bookmarks.size())
                        {
                            Bookmark bm = bookmarks.get(index);
                            if (bm != null)
                                context.browser.bookmarkGoto(bm);
                        }
                    }
                });
                menu.add(item);
            }
        }
    }

    public void loadJobsMenu()
    {
        final int OffsetCount = 2; // number of static Job menu items

        JMenu menu = context.mainFrame.menuJobs;
        int count = menu.getItemCount();

        // offset for static top items
        if (count > OffsetCount)
        {
            for (int i = count - 1; i > OffsetCount - 1; --i)
            {
                menu.remove(i);
            }
        }

        Job tmpJob = new Job(context, "temp");
        File jobsDir = new File(tmpJob.getDirectoryPath());
        if (jobsDir.exists())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, false);
            if (files.length > 0)
            {
                class objInstanceCreator implements InstanceCreator
                {
                    @Override
                    public Object createInstance(java.lang.reflect.Type type)
                    {
                        return new Job(context, "");
                    }
                }
                GsonBuilder builder = new GsonBuilder();
                builder.registerTypeAdapter(Job.class, new objInstanceCreator());

                int index = 0;
                jobs = new Job[files.length];
                for (File entry : files)
                {
                    if (!entry.isDirectory())
                    {
                        try
                        {
                            String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                            if (json != null)
                            {
                                Job job = builder.create().fromJson(json, Job.class);
                                if (job != null)
                                {
                                    jobs[index++] = job;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            String msg = context.cfg.gs("Z.exception") + entry.getName() + " " + Utils.getStackTrace(e);
                            logger.error(msg);
                            JOptionPane.showMessageDialog(context.mainFrame, msg,
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }

                Arrays.sort(jobs);
                for (int i = 0; i < jobs.length; ++i)
                {
                    JMenuItem item = new JMenuItem(jobs[i].getConfigName());
                    item.setHorizontalAlignment(SwingConstants.LEADING);
                    item.setHorizontalTextPosition(SwingConstants.TRAILING);
                    item.setMargin(new Insets(2, 18, 2, 2));
                    item.addActionListener(new AbstractAction()
                    {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            JMenuItem selected = (JMenuItem) actionEvent.getSource();

                            // offset for static top items
                            int index = findMenuItemIndex(context.mainFrame.menuJobs, selected) - OffsetCount;
                            if (index >= 0)
                            {
                                Job job = jobs[index];
                                processJob(job);
                            }
                        }
                    });
                    menu.add(item);
                }
            }
        }
    }

    public void processJob(Job job)
    {
        // validate job tasks and origins
        String status = job.validate(context.cfg);
        if (status.length() == 0)
        {
            // make dialog pieces
            String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
            JCheckBox checkbox = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
            checkbox.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
            checkbox.setSelected(true);
            Object[] params = {message, checkbox};

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, params, context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            boolean isDryRun = checkbox.isSelected();
            if (reply == JOptionPane.YES_OPTION)
            {
                context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                context.navigator.disableComponent(false, context.mainFrame.getContentPane());
                context.mainFrame.menuItemFileQuit.setEnabled(true);

                // capture current selections
                try
                {
                    originsArray = Origins.makeAllOrigins(context, context.mainFrame);
                }
                catch (Exception e)
                {
                    if (!e.getMessage().equals("HANDLED_INTERNALLY"))
                    {
                        String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                        if (context.navigator != null)
                        {
                            logger.error(msg);
                            JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                        }
                        else
                            logger.error(msg);
                    }
                }

                worker = job.process(context, context.mainFrame, context.cfg.getNavigatorName(), job, isDryRun);
                if (worker != null)
                {
                    remoteJobRunning = true;
                    disableGui(true);
                    worker.addPropertyChangeListener(new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange(PropertyChangeEvent e)
                        {
                            if (e.getPropertyName().equals("state"))
                            {
                                if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                    processTerminated(job, isDryRun);
                            }
                        }
                    });
                    worker.execute();
                }
                else
                    processTerminated(job, isDryRun);
            }
        }
        else
            JOptionPane.showMessageDialog(context.mainFrame, status, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
    }

    private void processTerminated(Job job, boolean isDryRun)
    {
        try
        {
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            // reset and reload relevant trees
            if (!isDryRun)
            {
                if (job.usesPublisher())
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.publisher"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false, false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false, false);
                }
                if (job.usesSubscriber())
                {
                    if (context.progress != null)
                        context.progress.update(context.cfg.gs("Navigator.scanning.subscriber"));
                    context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.cfg.isRemoteOperation(), false);
                    context.browser.deepScanSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.cfg.isRemoteOperation(), false);
                }
            }

            if (context.progress != null)
                context.progress.done();

            remoteJobRunning = false;
            disableGui(false);

            if (originsArray != null && originsArray.size() == 8)
                Origins.setAllOrigins(context, context.mainFrame, originsArray);

            reconnectRemote(context, context.publisherRepo, context.subscriberRepo);
        }
        catch (Exception e)
        {
        }

        if (job.isRequestStop())
        {
            logger.info(job.getConfigName() + context.cfg.gs("Z.cancelled"));
            context.mainFrame.labelStatusMiddle.setText(job.getConfigName() + context.cfg.gs("Z.cancelled"));
        }
        else
            context.mainFrame.labelStatusMiddle.setText(job.getConfigName() + context.cfg.gs("Z.completed"));
    }

    public void quitByeRemotes(boolean elsListener, boolean hintStatusServer)
    {
        boolean closure = false;
        if (elsListener && context.clientStty != null)
        {
            try
            {
                closure = true;
                context.clientSftp.stopClient();
                if (context.clientStty.isConnected())
                {
                    if (!context.timeout)
                    {
                        logger.info(context.cfg.gs("Main.disconnecting.stty"));
                        if (context.fault)
                        {
                            String resp;
                            try
                            {
                                resp = context.clientStty.roundTrip("fault", "Sending fault to remote", 1000);
                            }
                            catch (Exception e)
                            {
                                resp = null;
                            }
                        }
                        else if (quitRemoteSubscriber)
                            context.clientStty.send("quit", "Sending quit command to remote subscriber");
                        else
                            context.clientStty.send("bye", "Sending bye command to remote subscriber");
                    }
                }
                context.clientStty = null;
            }
            catch (Exception e)
            {
                context.fault = true;
                context.clientStty = null;
                logger.error(Utils.getStackTrace(e));
            }
        }

        if (hintStatusServer && context.cfg.isRemoteStatusServer())
        {
            try
            {
                closure = true;
                logger.info(context.cfg.gs("Main.disconnecting.status.server"));
                if (quitRemoteHintStatusServer)
                    context.statusStty.send("quit", "Sending quit command to remote Hint Status Server");
                else
                    context.statusStty.send("bye", "Sending bye command to remote Hint Status Server");
                context.statusStty = null;
            }
            catch (Exception e)
            {
                context.fault = true;
                logger.error(Utils.getStackTrace(e));
            }
        }

        // give the quit/bye commands a moment to be received
        if (closure && elsListener && hintStatusServer)
        {
            try
            {
                Thread.sleep(1500);
            }
            catch (Exception e)
            {
                // ignore
            }
        }
    }

    public void readBookmarks()
    {
        try
        {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(Paths.get(bookmarks.getFullPath())));
            bookmarks = gson.fromJson(json, bookmarks.getClass());
        }
        catch (IOException e)
        {
            // file might not exist
        }
    }

    public boolean reconnectRemote(Context context, Repository publisherRepo, Repository subscriberRepo) throws Exception
    {
        // is this necessary?
        if (context.cfg.isRemoteOperation() && subscriberRepo != null &&
                context.clientStty != null && context.clientStty.getTheirKey().equals(subscriberRepo.getLibraryData().libraries.key) &&
                context.clientStty.isConnected())
            return true;

        // close any existing connections
        if (context.cfg.isRemoteSubscriber())
        {
            try
            {
                context.clientStty.send("bye", "");
                context.clientSftp.stopClient();
                Thread.sleep(1500);
            }
            catch (Exception e)
            {
            }
        }

        // connect to the hint status server if defined
        context.main.setupHints(context.publisherRepo);

        if (context.cfg.isRemoteOperation())
        {
            // start the serveStty client for automation
            context.clientStty = new ClientStty(context, false, true);
            if (!context.clientStty.connect(publisherRepo, subscriberRepo))
            {
                context.cfg.setRemoteType("-");
                if (context.navigator != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }

            // check for opening commands from Subscriber
            // *** might change cfg options for subscriber and targets that are handled below ***
            if (context.clientStty.checkBannerCommands())
            {
                logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
            }

            // start the serveSftp client
            context.clientSftp = new ClientSftp(context, publisherRepo, subscriberRepo, true);
            if (!context.clientSftp.startClient())
            {
                context.cfg.setRemoteType("-");
                if (context.navigator != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }
        }
        return true;
    }

    public int run() throws Exception
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // TODO Add as needed: Set command line overrides on Navigator Preferences

                // preserve file times
                String o = context.cfg.getOriginalCommandline();
                if (context.cfg.getOriginalCommandline().contains("-y"))
                    context.preferences.setPreserveFileTimes(context.cfg.isPreserveDates());
                else
                    context.preferences.setPreserveFileTimes(context.preferences.isPreserveFileTimes());

                // binary or decimal scale
                if (context.cfg.getOriginalCommandline().contains("-z"))
                    context.cfg.setLongScale(context.cfg.isBinaryScale());
                else
                    context.cfg.setLongScale(context.preferences.isBinaryScale());

                // execute the Navigator GUI
                if (initialize())
                {
                    logger.trace(context.cfg.gs("Navigator.initialized"));
                    context.preferences.fixApplication(context);

                    for (ActionListener listener : context.mainFrame.buttonHintTracking.getActionListeners())
                    {
                        listener.actionPerformed(new ActionEvent(context.mainFrame.buttonHintTracking, ActionEvent.ACTION_PERFORMED, null));
                    }

                    String os = Utils.getOS();
                    logger.debug(context.cfg.gs("Navigator.detected.local.system.as") + os);

                    if (checkForUpdates(true))
                    {
                        logger.info(context.cfg.gs("Navigator.update.available"));
                        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.update.available"));
                    }
                    else
                    {
                        logger.info(context.cfg.gs("Navigator.installed.up.to.date"));
                        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.installed.up.to.date"));
                    }

                    context.mainFrame.setVisible(true);

                    context.preferences.fixBrowserDivider(context, -1);
                    context.mainFrame.treeCollectionOne.requestFocus();
                }
                else
                {
                    context.mainFrame = null; // failed
                    context.fault = true;
                    stop();
                }
            }
        });

        return 0;
    }

    private String selectLibrarySource(NavTreeUserObject tuo)
    {
        String path = "";
        if (tuo.type == NavTreeUserObject.REAL)
            path = tuo.path;
        else if (tuo.type == NavTreeUserObject.LIBRARY)
        {
            if (tuo.sources.length == 1)
                path = tuo.sources[0];
            else
            {
                try
                {
                    // make dialog pieces
                    String message = java.text.MessageFormat.format(context.cfg.gs("Navigator.menu.New.folder.select.library.source"), tuo.sources.length, tuo.name);
                    JList<String> sources = new JList<String>();
                    DefaultListModel<String> listModel = new DefaultListModel<String>();
                    sources.setModel(listModel);
                    sources.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    for (String src : tuo.sources)
                    {
                        long space = context.transfer.getFreespace(src, tuo.isRemote);
                        String line = src + "  " + Utils.formatLong(space, false, context.cfg.getLongScale()) + context.cfg.gs("Navigator.newFolder.free");
                        listModel.addElement(line);
                    }
                    sources.setSelectedIndex(0);

                    JScrollPane pane = new JScrollPane();
                    pane.setViewportView(sources);
                    sources.requestFocus();
                    Object[] params = {message, pane};

                    int opt = JOptionPane.showConfirmDialog(context.mainFrame, params, context.cfg.getNavigatorName(), JOptionPane.OK_CANCEL_OPTION);
                    if (opt == JOptionPane.YES_OPTION)
                    {
                        int index = sources.getSelectedIndex();
                        path = tuo.sources[index];
                    }
                    else
                    {
                        path = "_cancelled_";
                    }
                }
                catch (Exception e)
                {
                    logger.error(context.cfg.gs("Z.exception") + e.getMessage());
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Z.exception") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return path;
    }

    private void setQuitTerminateVisibility()
    {
        if (context.cfg.isRemoteActive())
            context.mainFrame.menuItemQuitTerminate.setVisible(true);
        else
            context.mainFrame.menuItemQuitTerminate.setVisible(false);
    }

    private void setTableEnabled(boolean disable, JTable table)
    {
        for (int i = 0; i < table.getRowCount(); ++i)
        {
            for (int j = 0; j < table.getColumnCount(); ++j)
            {
                Component comp = table.getComponentAt(i, j);
                comp.setEnabled(disable);
            }
        }
    }

    public void setUpdaterProcess(String updaterJar)
    {
        this.updaterJar = updaterJar;
        this.updaterProcess = true;
    }

    public void stop()
    {
        quitByeRemotes(true, true);
        if (context.mainFrame != null)
        {
            try
            {
                // save the settings
                context.libraries.savePreferences();
                context.preferences.write(context);
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                JOptionPane.showMessageDialog(context.mainFrame,
                        context.cfg.gs("Z.exception") + e.getMessage(),
                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            context.mainFrame.setVisible(false);
            context.mainFrame.dispose();
        }

        context.main.stopVerbiage();

        // end the Navigator Swing thread
        if (!context.main.secondaryNavigator)
        {
            if (isUpdaterProcess())
            {
                try
                {
                    String[] args;
                    String cmd = "";
                    if (Utils.isOsMac())
                    {
                        String[] parms = { Utils.getSystemTempDirectory() +
                                "/ELS_Updater/ELS_Updater/ELS_Updater.app/Contents/MacOS/ELS_Updater" };
                        args = parms;
                        cmd = parms[0];
                    }
                    else
                    {
                        String[] parms = { Utils.getSystemTempDirectory() + System.getProperty("file.separator") +
                                "ELS_Updater" + System.getProperty("file.separator") +
                                "rt" + System.getProperty("file.separator") +
                                "bin" + System.getProperty("file.separator") +
                                "java" + (Utils.isOsWindows() ? ".exe" : ""),
                            "-jar",
                            updaterJar };
                        args = parms;
                        cmd = parms.toString();
                    }

                    logger.info(context.cfg.gs("Navigator.starting.els.updater") + cmd);

                    Process proc = Runtime.getRuntime().exec(args);
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    String message = context.cfg.gs("Navigator.error.launching.els.updater") + e.getMessage();
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                    return;
                }
            }

            System.exit(context.fault ? 1 : 0);
        }
        else
        {
            GuiLogAppender appender = context.main.getGuiLogAppender();
            appender.setContext(context.main.previousContext);
        }
    }

}
