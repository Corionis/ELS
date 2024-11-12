package com.corionis.els.gui;

import com.corionis.els.*;
import com.corionis.els.gui.bookmarks.Bookmark;
import com.corionis.els.gui.bookmarks.Bookmarks;
import com.corionis.els.gui.browser.NavTransferHandler;
import com.corionis.els.gui.browser.NavTreeNode;
import com.corionis.els.gui.browser.NavTreeUserObject;
import com.corionis.els.gui.hints.HintsUI;
import com.corionis.els.gui.tools.duplicateFinder.DuplicateFinderUI;
import com.corionis.els.gui.tools.emptyDirectoryFinder.EmptyDirectoryFinderUI;
import com.corionis.els.gui.tools.junkRemover.JunkRemoverUI;
import com.corionis.els.gui.tools.operations.OperationsUI;
import com.corionis.els.gui.tools.renamer.RenamerUI;
import com.corionis.els.gui.tools.sleep.SleepUI;
import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

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

import static com.corionis.els.gui.system.FileEditor.EditorTypes.*;

public class Navigator
{
    public Bookmarks bookmarks;
    public Context context;
    public DuplicateFinderUI dialogDuplicateFinder;
    public EmptyDirectoryFinderUI dialogEmptyDirectoryFinder;
    public HintsUI dialogHints = null;
    public JobsUI dialogJobs = null;
    public JunkRemoverUI dialogJunkRemover = null;
    public OperationsUI dialogOperations = null;
    public RenamerUI dialogRenamer = null;
    public SleepUI dialogSleep = null;
    public FileEditor fileeditor = null;
    public Job[] jobs;
    public SwingWorker<Void, Void> worker;
    boolean mockMode = false; // instead of downloading version.info get from mock/bin/
    private boolean blockingProcessRunning = false;
    private int bottomSizeBrowser;
    private Settings dialogSettings = null;
    private int lastFindPosition = 0;
    private String lastFindString = "";
    private int lastFindTab = -1;
    private ArrayList<ArrayList<Origin>> originsArray = null;
    private boolean quitRemoteHintStatusServer = false;
    private boolean quitRemoteSubscriber = false;
    private boolean secondaryNavigator = false;
    private String updaterJar = null;
    private boolean updaterProcess = false;
    private boolean workerRunning = false;
    private transient Logger logger = LogManager.getLogger("applog");

    /**
     * Constructor
     *
     * @param context The Context
     */
    public Navigator(Context context)
    {
        this.context = context;
        if (this.context.navigator != null)
            this.secondaryNavigator = true;
        this.context.navigator = this;
    }

    public boolean checkForConflicts(NavTreeUserObject tuo, String action)
    {
        if (tuo.type == NavTreeUserObject.REAL)
        {
            if (context.cfg.isHintTrackingEnabled() &&
                    context.browser.isHintTrackingButtonEnabled() &&
                    tuo.node.getMyTree().getName().toLowerCase().contains("collection"))
            {
                ArrayList<Hint> hints = null;
                String libName = tuo.getParentLibrary().getUserObject().name;
                try
                {
                    hints = context.hintsHandler.checkConflicts(libName, tuo.getItemPath(libName, tuo.getPath()));
                    if (hints != null && hints.size() > 0)
                    {
                        logger.info(context.cfg.gs("NavTransferHandler.action.cancelled"));
                        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("NavTransferHandler.action.cancelled"));
                        String msg = "" + hints.size() + " Hint(s) conflict with the current " + action;
                        JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return true;
                    }
                }
                catch (Exception e)
                {
                    String message = e.getMessage();
                    logger.error(message);
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Z.exception"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                    return true;
                }
            }
        }
        else
            return true;
        return false;
    }

    public int checkForHints()
    {
        int count = 0;
        if (context.cfg.isHintTrackingEnabled() && context.hintsHandler != null && context.publisherRepo != null)
        {
            try
            {
                HintKey hk = context.hintsHandler.findHintKey(context.publisherRepo);
                if (hk != null)
                    count = context.hintsHandler.getCount(hk.system);
                if (count > 0)
                {
                    String text = "" + count + " " + context.cfg.gs("Navigator.hints.available");
                    logger.info(text);
                    context.mainFrame.labelAlertHintsMenu.setToolTipText(text);
                    context.mainFrame.labelAlertHintsToolbar.setToolTipText(text);
                    context.mainFrame.labelAlertHintsMenu.setVisible(true);
                    context.mainFrame.labelAlertHintsToolbar.setVisible(true);
                }
                else
                {
                    context.mainFrame.labelAlertHintsMenu.setVisible(false);
                    context.mainFrame.labelAlertHintsToolbar.setVisible(false);
                }
            }
            catch (Exception e)
            {
                context.fault = true;
                logger.error(Utils.getStackTrace(e));
            }
        }
        else
        {
            context.mainFrame.labelAlertHintsMenu.setVisible(false);
            context.mainFrame.labelAlertHintsToolbar.setVisible(false);
        }
        return count;
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
            String updateInfoPath = context.cfg.getInstalledPath() + System.getProperty("file.separator") + "bin";

            // check if it's installed
            if (Utils.isOsMac())
            {
                File installed = new File(updateInfoPath);
                if (!installed.canWrite())
                {
                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    message = context.cfg.gs("Updater.application.path.not.writable");
                    logger.info(message);
                    if (!checkOnly)
                    {
                        JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                                JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE, null, opts, opts[0]);
                    }
                    return false;
                }
            }

            updateInfoPath = context.cfg.getInstalledPath() + System.getProperty("file.separator") +
                    "bin" + System.getProperty("file.separator") + "update.info";

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
                    {
                        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.installed.up.to.date"));
                        return false;
                    }
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.update.available"));
                    return true;
                }

                if (flags.toLowerCase().contains("ignore") || version.get(Configuration.BUILD_NUMBER).equals(Configuration.getBuildNumber())) // manual check
                {
                    // yes, up-to-date
                    message = context.cfg.gs("Navigator.installed.up.to.date");
                    logger.info(message);
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.installed.up.to.date"));
                    context.mainFrame.labelAlertUpdateMenu.setVisible(false);
                    context.mainFrame.labelAlertUpdateToolbar.setVisible(false);
                    Object[] opts = {context.cfg.gs("Z.ok")};
                    JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                            JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null, opts, opts[0]);
                    return false;
                }
                else
                {
                    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.update.available"));
                    context.mainFrame.labelAlertUpdateMenu.setVisible(true);
                    context.mainFrame.labelAlertUpdateToolbar.setVisible(true);
                    while (true)
                    {
                        // a new version is available
                        String prompt = context.cfg.gs("Navigator.install.update.version");
                        String mprompt = context.cfg.gs("Navigator.install.new.version");
                        message = java.text.MessageFormat.format(Utils.isOsMac() ? mprompt : prompt,
                                Configuration.getBuildDate(), version.get(Configuration.BUILD_DATE));
                        Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no"), context.cfg.gs("Navigator.recent.changes")};
                        Object[] mopts = {context.cfg.gs("Z.goto.website"), context.cfg.gs("Z.no"), context.cfg.gs("Navigator.recent.changes")};
                        int reply = JOptionPane.showOptionDialog(context.mainFrame, message, context.cfg.gs("Navigator.update"),
                                JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE, null,
                                Utils.isOsMac() ? mopts : opts, Utils.isOsMac() ? mopts[0] : opts[0]);

                        // proceed?
                        if (reply == JOptionPane.YES_OPTION)
                        {
                            if (!Utils.isOsMac())
                            {
                                // execute the download and unpack procedure then execute the Updater
                                new DownloadUpdater(this, version, prefix);
                            }
                            else
                            {
                                try
                                {
                                    URI uri = new URI("https://corionis.github.io/ELS/");
                                    Desktop.getDesktop().browse(uri);
                                }
                                catch (Exception e)
                                {
                                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }
                            }
                            break;
                        }
                        else if (reply == JOptionPane.CANCEL_OPTION) // show Changelist
                        {
                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            NavHelp helpDialog = new NavHelp(context.mainFrame, context.mainFrame, context,
                                    context.cfg.gs("Navigator.recent.changes"), version.get(Configuration.BUILD_CHANGES_URL), true);
                            if (!helpDialog.fault)
                            {
                                helpDialog.buttonFocus();
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

        context.mainFrame.menuItemCopy.setEnabled(enable);
        context.mainFrame.menuItemCut.setEnabled(enable);
        context.mainFrame.menuItemPaste.setEnabled(enable);
        context.mainFrame.menuItemDelete.setEnabled(enable);
        context.mainFrame.menuItemFind.setEnabled(enable);
        context.mainFrame.menuItemFindNext.setEnabled(enable);
        context.mainFrame.menuItemNewFolder.setEnabled(enable);
        context.mainFrame.menuItemRename.setEnabled(enable);
        context.mainFrame.menuItemTouch.setEnabled(enable);

        context.mainFrame.menuItemRefresh.setEnabled(enable);
        context.mainFrame.radioButtonAutoRefresh.setEnabled(enable);
        context.mainFrame.menuItemShowHidden.setEnabled(enable);
        context.mainFrame.menuItemWordWrap.setEnabled(enable);

        context.mainFrame.menuTbCopy.setEnabled(enable);
        context.mainFrame.menuTbCut.setEnabled(enable);
        context.mainFrame.menuTbPaste.setEnabled(enable);
        context.mainFrame.menuTbDelete.setEnabled(enable);
        context.mainFrame.menuTbNewFolder.setEnabled(enable);
        context.mainFrame.menuTbRefresh.setEnabled(enable);

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
        context.mainFrame.menuItemExternalTools.setEnabled(false);
        context.mainFrame.menuItemPlexGenerator.setEnabled(false);
        context.mainFrame.menuItemHandbrake.setEnabled(false);

        context.mainFrame.menuItemSplitHorizontal.setEnabled(enable);
        context.mainFrame.menuItemSplitVertical.setEnabled(enable);

        if (enable == true)
        {
            context.preferences.fixBrowserDivider(context, bottomSizeBrowser);
        }

    }

    public void disconnectSubscriber()
    {
        disconnectSubscriber(true);
        context.subscriberRepo = null;
        context.cfg.setSubscriberCollectionFilename("");
        context.cfg.setSubscriberLibrariesFileName("");
        context.cfg.setOperation("-");
    }

    public void disconnectSubscriber(boolean clear)
    {
        quitByeRemotes(true, false);
        NavTreeNode root = context.browser.setCollectionRoot(null, context.mainFrame.treeCollectionTwo, context.cfg.gs("Browser.open.a.subscriber"), false);
        root.loadTable();
        root = context.browser.setCollectionRoot(null, context.mainFrame.treeSystemTwo, context.cfg.gs("Browser.open.a.subscriber"), false);
        root.loadTable();
        setQuitTerminateVisibility();
    }

    public void enableDisableSystemMenus(FileEditor.EditorTypes type, boolean enable)
    {
        if (enable)
        {
            context.mainFrame.menuItemHints.setEnabled(true);
            context.mainFrame.menuItemAuthKeys.setEnabled(true);
            context.mainFrame.menuItemHintKeys.setEnabled(true);
            context.mainFrame.menuItemBlacklist.setEnabled(true);
            context.mainFrame.menuItemWhitelist.setEnabled(true);
        }
        else
        {
            switch (type)
            {
                case Authentication:
                    context.mainFrame.menuItemHints.setEnabled(false);
                    context.mainFrame.menuItemAuthKeys.setEnabled(true);
                    context.mainFrame.menuItemHintKeys.setEnabled(false);
                    context.mainFrame.menuItemBlacklist.setEnabled(false);
                    context.mainFrame.menuItemWhitelist.setEnabled(false);
                    break;
                case Hints:
                    context.mainFrame.menuItemHints.setEnabled(true);
                    context.mainFrame.menuItemAuthKeys.setEnabled(false);
                    context.mainFrame.menuItemHintKeys.setEnabled(false);
                    context.mainFrame.menuItemBlacklist.setEnabled(false);
                    context.mainFrame.menuItemWhitelist.setEnabled(false);
                    break;
                case HintKeys:
                    context.mainFrame.menuItemHints.setEnabled(false);
                    context.mainFrame.menuItemAuthKeys.setEnabled(false);
                    context.mainFrame.menuItemHintKeys.setEnabled(true);
                    context.mainFrame.menuItemBlacklist.setEnabled(false);
                    context.mainFrame.menuItemWhitelist.setEnabled(false);
                    break;
                case BlackList:
                    context.mainFrame.menuItemHints.setEnabled(false);
                    context.mainFrame.menuItemAuthKeys.setEnabled(false);
                    context.mainFrame.menuItemHintKeys.setEnabled(false);
                    context.mainFrame.menuItemBlacklist.setEnabled(true);
                    context.mainFrame.menuItemWhitelist.setEnabled(false);
                    break;
                case WhiteList:
                    context.mainFrame.menuItemHints.setEnabled(false);
                    context.mainFrame.menuItemAuthKeys.setEnabled(false);
                    context.mainFrame.menuItemHintKeys.setEnabled(false);
                    context.mainFrame.menuItemBlacklist.setEnabled(false);
                    context.mainFrame.menuItemWhitelist.setEnabled(true);
                    break;
            }
        }
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
            context.preferences.setLastPublisherOpenFile(context.cfg.getPublisherLibrariesFileName());
            context.preferences.setLastPublisherOpenPath(Utils.getLeftPath(context.cfg.getPublisherLibrariesFileName(),
                    Utils.getSeparatorFromPath(context.cfg.getPublisherLibrariesFileName())));
        }
        if (!context.preferences.getLastPublisherOpenFile().isEmpty() &&
                context.preferences.getLastPublisherOpenPath().equals(context.preferences.getLastPublisherOpenFile()))
            context.preferences.setLastPublisherOpenPath("");

        if (context.cfg.getSubscriberCollectionFilename().length() > 0)
        {
            context.preferences.setLastSubscriberIsRemote(context.cfg.isRemoteSubscriber());
            context.preferences.setLastOverrideSubscriber(context.cfg.getOverrideSubscriberHost());
            context.preferences.setLastSubscriberOpenFile(context.cfg.getSubscriberCollectionFilename());
            context.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(context.cfg.getSubscriberCollectionFilename(),
                    Utils.getSeparatorFromPath(context.cfg.getSubscriberCollectionFilename())));
        }
        else if (context.cfg.getSubscriberLibrariesFileName().length() > 0)
        {
            context.preferences.setLastSubscriberIsRemote(context.cfg.isRemoteSubscriber());
            context.preferences.setLastOverrideSubscriber(context.cfg.getOverrideSubscriberHost());
            context.preferences.setLastSubscriberOpenFile(context.cfg.getSubscriberLibrariesFileName());
            context.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(context.cfg.getSubscriberLibrariesFileName(),
                    Utils.getSeparatorFromPath(context.cfg.getSubscriberLibrariesFileName())));
        }
        if (!context.preferences.getLastSubscriberOpenFile().isEmpty() &&
                context.preferences.getLastSubscriberOpenPath().equals(context.preferences.getLastSubscriberOpenFile()))
            context.preferences.setLastSubscriberOpenPath("");

        if (context.cfg.isHintTrackingEnabled())
        {
            context.preferences.setLastHintTrackingIsRemote(context.cfg.getHintsDaemonFilename().length() > 0);
            context.preferences.setLastOverrideHintHost(context.cfg.isOverrideHintsHost());
            context.preferences.setLastHintTrackingOpenFile(context.cfg.getHintHandlerFilename());
            context.preferences.setLastHintTrackingOpenPath(Utils.getLeftPath(context.cfg.getHintHandlerFilename(),
                    Utils.getSeparatorFromPath(context.cfg.getHintHandlerFilename())));
            if (context.preferences.getLastHintKeysOpenPath().equals(context.preferences.getLastHintKeysOpenFile()))
                context.preferences.setLastHintTrackingOpenPath("");
        }
        else if (context.cfg.getHintHandlerFilename().length() > 0)
        {
            // might be null in existing preferences.json
            context.preferences.setLastOverrideHintHost(context.cfg.isOverrideHintsHost());
            if (!context.preferences.getLastHintKeysOpenFile().isEmpty() &&
                    context.preferences.getLastHintTrackingOpenPath().equals(context.preferences.getLastHintTrackingOpenFile()))
                context.preferences.setLastHintTrackingOpenPath("");
        }

        if (!isLogger())
        {
            // setup the needed tools
            context.transfer = new Transfer(context);
            try
            {
                context.transfer.initialize();

                if (context.cfg.getHintKeysFile() != null && context.cfg.getHintKeysFile().length() > 0)
                {
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

            // set the GuiLogAppender localContext for a second invocation
            if (context.main.primaryExecution)
            {
                GuiLogAppender appender = context.main.guiLogAppender;
                appender.setContext(context);
                // this causes the preBuffer to be appended to the Navigator Log panels
                logger.info(context.cfg.gs("Navigator.appender.updated"));
            }

            // disable back-fill because we never know what combination of items might be selected
            context.cfg.setNoBackFill(true);

            context.cfg.setPreserveDates(context.preferences.isPreserveFileTimes());

            setQuitTerminateVisibility();

            if (!context.cfg.isLoggerView())
            {
                // add any defined bookmarks to the menu
                bookmarks = new Bookmarks();
                loadBookmarksMenu();

                // add any defined jobs to the menu
                loadJobsMenu();
            }

            context.cfg.setNavigator(true);
        }

        if (!secondaryNavigator)
            context.main.saveEnvironment();

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
                {
                    ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
                    if (!ld.exists())
                        ld = new File(context.cfg.getWorkingDirectory());
                }
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
                Font font = lab.getFont();
                Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                lab.setFont(boldFont);

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

                        if (context.cfg.isRemoteSubscriber() || (context.cfg.isHintTrackingEnabled() && context.cfg.isRemoteStatusServer()))
                        {
                            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                    context.cfg.gs(("Navigator.remote.connections.must.be.closed")),
                                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                            if (r != JOptionPane.YES_OPTION)
                                break;

                            context.preferences.setLastSubscriberIsOpen(false);
                            disconnectSubscriber();

                            quitByeRemotes(false, true);
                            context.hintsHandler = null;
                            context.hintKeys = null;
                            context.cfg.setHintKeysFile("");
                            context.hintsRepo = null;
                            context.cfg.setHintTrackerFilename("");
                            context.cfg.setHintsDaemonFilename("");
                            context.preferences.setLastHintKeysIsOpen(false);
                            context.browser.setupHintTrackingButton();
                        }

                        try
                        {
                            context.preferences.setLastPublisherOpenFile(file.getAbsolutePath());
                            context.preferences.setLastPublisherOpenPath(last.getAbsolutePath());
                            context.preferences.setLastPublisherIsWorkstation(isWorkstation);
                            context.preferences.setLastPublisherIsOpen(true);
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
                            context.libraries.loadConfigurations();

                            checkForHints();
                            if (dialogHints != null && dialogHints.isVisible())
                                dialogHints.refresh();
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
                {
                    ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
                    if (!ld.exists())
                        ld = new File(context.cfg.getWorkingDirectory());
                }
                if (ld.exists() && ld.isDirectory())
                    fc.setCurrentDirectory(ld);

                if (context.preferences.getLastSubscriberOpenFile().length() > 0)
                {
                    File lf = new File(context.preferences.getLastSubscriberOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Subscriber Remote Connection checkbox accessory
                JPanel jp = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                jp.setLayout(gb);

                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(context.mainFrame.textFieldLocation.getBorder());

                JLabel cbLabel = new JLabel(context.cfg.gs("Navigator.labelRemote.text"));
                Font font = cbLabel.getFont();
                Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                cbLabel.setFont(boldFont);

                JCheckBox cbIsRemote = new JCheckBox();
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(context.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox.tooltip"));
                cbIsRemote.setSelected(context.preferences.isLastSubscriberIsRemote());

                JRadioButton hostButton = new JRadioButton(context.cfg.gs("Navigator.labelHostInternet.text"));
                hostButton.setSelected(context.preferences.getLastOverrideSubscriber().isEmpty());

                JLabel hostLabel = new JLabel();

                JRadioButton listenButton = new JRadioButton(context.cfg.gs("Navigator.labelListenLan.text"));
                listenButton.setSelected(context.preferences.getLastOverrideSubscriber().equals("true"));

                JLabel listenLabel = new JLabel();

                boolean custom = !context.preferences.getLastOverrideSubscriber().isEmpty() &&
                        !context.preferences.getLastOverrideSubscriber().equals("true");
                JRadioButton customButton = new JRadioButton(context.cfg.gs("Navigator.labelCustom.text"));
                customButton.setSelected(custom);

                ButtonGroup group = new ButtonGroup();
                group.add(hostButton);
                group.add(listenButton);
                group.add(customButton);

                JTextField customAddress = new JTextField();
                if (custom)
                    customAddress.setText(context.preferences.getLastOverrideSubscriber());
                else
                    customAddress.setText("");
                customAddress.setEnabled(customButton.isSelected());

                cbIsRemote.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (cbIsRemote.isSelected())
                        {
                            hostButton.setEnabled(true);
                            hostLabel.setEnabled(true);
                            listenButton.setEnabled(true);
                            listenLabel.setEnabled(true);
                            customButton.setEnabled(true);
                            customAddress.setEnabled(customButton.isSelected());
                        }
                        else
                        {
                            hostButton.setEnabled(false);
                            hostLabel.setEnabled(false);
                            listenButton.setEnabled(false);
                            listenLabel.setEnabled(false);
                            customButton.setEnabled(false);
                            customAddress.setEnabled(false);
                        }
                    }
                });
                if (cbIsRemote.isSelected())
                {
                    hostButton.setEnabled(true);
                    hostLabel.setEnabled(true);
                    listenButton.setEnabled(true);
                    listenLabel.setEnabled(true);
                    customButton.setEnabled(true);
                    customAddress.setEnabled(customButton.isSelected());
                }
                else
                {
                    hostButton.setEnabled(false);
                    hostLabel.setEnabled(false);
                    listenButton.setEnabled(false);
                    listenLabel.setEnabled(false);
                    customButton.setEnabled(false);
                    customAddress.setEnabled(false);
                }

                hostButton.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (hostButton.isSelected())
                        {
                            customAddress.setText("");
                            customAddress.setEnabled(false);
                        }
                    }
                });

                listenButton.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (listenButton.isSelected())
                        {
                            customAddress.setText("");
                            customAddress.setEnabled(false);
                        }
                    }
                });

                customButton.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (customButton.isSelected())
                        {
                            if (customAddress.getText().isEmpty())
                            {
                                if (!context.preferences.getLastOverrideSubscriber().trim().equals("true"))
                                    customAddress.setText(context.preferences.getLastOverrideSubscriber());
                            }
                            customAddress.setEnabled(true);
                        }
                    }
                });

                jp.add(cbLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(cbIsRemote, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(hostButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 12, 4, 4), 0, 0));
                jp.add(hostLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(listenButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 12, 4, 4), 0, 0));
                jp.add(listenLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(customButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 12, 4, 4), 0, 0));
                jp.add(customAddress, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));
                fc.setAccessory(jp);

                fc.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
                    {
                        File selected = fc.getSelectedFile();
                        logger.info(propertyChangeEvent.getPropertyName() + " :: " + selected);
                        if (setFileChooserHostListen(fc, hostLabel, listenLabel))
                            cbIsRemote.setEnabled(true);
                        else
                            cbIsRemote.setEnabled(false);
                        if (cbIsRemote.isSelected())
                        {
                            hostButton.setEnabled(true);
                            hostLabel.setEnabled(true);
                            listenButton.setEnabled(true);
                            listenLabel.setEnabled(true);
                            customButton.setEnabled(true);
                            customAddress.setEnabled(true);
                        }
                        else
                        {
                            hostButton.setEnabled(false);
                            hostLabel.setEnabled(false);
                            listenButton.setEnabled(false);
                            listenLabel.setEnabled(false);
                            customButton.setEnabled(false);
                            customAddress.setEnabled(false);
                        }
                    }
                });
                setFileChooserHostListen(fc, hostLabel, listenLabel); // set initial values, if any

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

                        // verify file
                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            continue;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            continue;
                        }

                        context.preferences.setLastSubscriberOpenFile(file.getAbsolutePath());
                        context.preferences.setLastSubscriberOpenPath(last.getAbsolutePath());
                        context.preferences.setLastSubscriberIsRemote(cbIsRemote.isSelected());
                        context.preferences.setLastSubscriberIsOpen(true);
                        boolean closeRemote = context.cfg.isRemoteSubscriber();

                        // read the selected local repository
                        try
                        {
                            context.cfg.setSubscriberLibrariesFileName(file.getAbsolutePath());
                            context.cfg.setSubscriberCollectionFilename("");
                            context.mainFrame.tabbedPaneMain.setSelectedIndex(0);

                            // this defines the value returned by localContext.cfg.isRemoteSession()
                            if (cbIsRemote.isSelected())
                                context.cfg.setOperation("P"); // publisher to remote subscriber
                            else
                                context.cfg.setOperation("-"); // not remote

                            context.subscriberRepo = context.main.readRepo(context, Repository.SUBSCRIBER, !context.preferences.isLastSubscriberIsRemote());
                        }
                        catch (Exception e)
                        {
                            context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Open.subscriber.error.opening.subscriber.library") + e.getMessage(),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            context.fault = false;
                            return;
                        }

                        if (cbIsRemote.isSelected())
                        {
                            if (hostButton.isSelected())
                            {
                                context.cfg.setOverrideSubscriberHost("");
                                context.preferences.setLastOverrideSubscriber("");
                            }
                            else if (listenButton.isSelected())
                            {
                                context.cfg.setOverrideSubscriberHost("true");
                                context.preferences.setLastOverrideSubscriber("true");
                            }
                            else
                            {
                                String host = Utils.parseHost(customAddress.getText());
                                String port = Utils.parsePort(customAddress.getText());
                                int p = -1;
                                if (!port.isEmpty())
                                    p = Integer.parseInt(port);
                                if (!host.isEmpty() && p > 0 && p <= 65535)
                                {
                                    context.cfg.setOverrideSubscriberHost(customAddress.getText());
                                    context.preferences.setLastOverrideSubscriber(customAddress.getText());
                                }
                                else
                                {
                                    JOptionPane.showMessageDialog(context.mainFrame,
                                            context.cfg.gs("Navigator.menu.Open.valid.port") + file.getName(),
                                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    continue;
                                }
                            }
                        }

                        if (closeRemote)
                        {
                            int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.menu.Open.subscriber.close.current.remote.connection"),
                                    context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                return;

                            disconnectSubscriber(false);
                            try
                            {
                                Thread.sleep(1000); // give the communications a moment to close
                            }
                            catch (Exception e)
                            {
                            }
                        }

                        if (context.preferences.isLastSubscriberIsRemote())
                        {
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

                                    if (context.preferences.isLastSubscriberIsRemote())
                                    {
                                        // start the serveStty client for automation
                                        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                                        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                                        context.clientStty = new ClientStty(context, false, true, false);
                                        if (!context.clientStty.connect(context.publisherRepo, context.subscriberRepo))
                                        {
                                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                            disconnectSubscriber();
                                            JOptionPane.showMessageDialog(context.mainFrame,
                                                    context.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                            context.cfg.setOperation("-");
                                            context.fault = false;
                                            return;
                                        }

                                        context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                                        if (context.clientStty.checkBannerCommands())
                                        {
                                            logger.info(context.cfg.gs("Transfer.received.subscriber.commands") + (context.cfg.isRequestCollection() ? "RequestCollection " : "") + (context.cfg.isRequestTargets() ? "RequestTargets" : ""));
                                        }
                                        context.transfer.requestLibrary();

                                        // start the serveSftp transfer client
                                        context.clientSftp = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                                        if (!context.clientSftp.startClient("transfer"))
                                        {
                                            context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                                            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                            disconnectSubscriber();
                                            JOptionPane.showMessageDialog(context.mainFrame,
                                                    context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                            context.fault = false;
                                            return;
                                        }

                                        // start the serveSftp metadata client
                                        context.clientSftpMetadata = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
                                        if (!context.clientSftpMetadata.startClient("metadata"))
                                        {
                                            context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
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
                                    context.libraries.loadConfigurations();
                                    context.browser.loadCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.preferences.isLastSubscriberIsRemote());
                                    context.browser.loadSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.preferences.isLastSubscriberIsRemote());
                                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

                                    checkForHints();
                                    if (dialogHints != null && dialogHints.isVisible())
                                        dialogHints.refresh();
                                }
                                catch (Exception e)
                                {
                                    context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
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
                {
                    ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "system");
                    if (!ld.exists())
                        ld = new File(context.cfg.getWorkingDirectory());
                }
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
                            context.preferences.setLastHintKeysOpenFile(file.getAbsolutePath());
                            context.preferences.setLastHintKeysOpenPath(last.getAbsolutePath());
                            context.preferences.setLastHintKeysIsOpen(true);
                            context.cfg.setHintKeysFile(file.getAbsolutePath());
                            context.main.setupHints(context.publisherRepo);
                            //context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
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
                {
                    ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
                    if (!ld.exists())
                        ld = new File(context.cfg.getWorkingDirectory());
                }
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

                JLabel cbLabel = new JLabel(context.cfg.gs("Navigator.labelRemote.text"));
                Font font = cbLabel.getFont();
                Font boldFont = new Font(font.getFontName(), Font.BOLD, font.getSize());
                cbLabel.setFont(boldFont);

                JCheckBox cbIsRemote = new JCheckBox();
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(context.cfg.gs("Navigator.menu.Open.hint.tracking.checkbox.tooltip"));
                cbIsRemote.setSelected(context.preferences.isLastHintTrackingIsRemote());

                JRadioButton hostButton = new JRadioButton(context.cfg.gs("Navigator.labelHostInternet.text"));
                hostButton.setSelected(!context.preferences.isLastOverrideHintHost());

                JLabel hostLabel = new JLabel();

                JRadioButton listenButton = new JRadioButton(context.cfg.gs("Navigator.labelListenLan.text"));
                listenButton.setSelected(context.preferences.isLastOverrideHintHost());

                JLabel listenLabel = new JLabel();

                ButtonGroup group = new ButtonGroup();
                group.add(hostButton);
                group.add(listenButton);

                cbIsRemote.addActionListener(new AbstractAction()
                {
                    @Override
                    public void actionPerformed(ActionEvent actionEvent)
                    {
                        if (cbIsRemote.isSelected())
                        {
                            hostButton.setEnabled(true);
                            hostLabel.setEnabled(true);
                            listenButton.setEnabled(true);
                            listenLabel.setEnabled(true);
                        }
                        else
                        {
                            hostButton.setEnabled(false);
                            hostLabel.setEnabled(false);
                            listenButton.setEnabled(false);
                            listenLabel.setEnabled(false);
                        }
                    }
                });
                if (cbIsRemote.isSelected())
                {
                    hostButton.setEnabled(true);
                    hostLabel.setEnabled(true);
                    listenButton.setEnabled(true);
                    listenLabel.setEnabled(true);
                }
                else
                {
                    hostButton.setEnabled(false);
                    hostLabel.setEnabled(false);
                    listenButton.setEnabled(false);
                    listenLabel.setEnabled(false);
                }

                jp.add(cbLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(cbIsRemote, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(hostButton, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 12, 4, 4), 0, 0));
                jp.add(hostLabel, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                jp.add(listenButton, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 12, 4, 4), 0, 0));
                jp.add(listenLabel, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 4, 4, 4), 0, 0));
                fc.setAccessory(jp);

                fc.addPropertyChangeListener(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY, new PropertyChangeListener()
                {
                    @Override
                    public void propertyChange(PropertyChangeEvent propertyChangeEvent)
                    {
                        File selected = fc.getSelectedFile();
                        logger.info(propertyChangeEvent.getPropertyName() + " :: " + selected);
                        if (setFileChooserHostListen(fc, hostLabel, listenLabel))
                            cbIsRemote.setEnabled(true);
                        else
                            cbIsRemote.setEnabled(false);

                        if (cbIsRemote.isSelected())
                        {
                            hostButton.setEnabled(true);
                            hostLabel.setEnabled(true);
                            listenButton.setEnabled(true);
                            listenLabel.setEnabled(true);
                        }
                        else
                        {
                            hostButton.setEnabled(false);
                            hostLabel.setEnabled(false);
                            listenButton.setEnabled(false);
                            listenLabel.setEnabled(false);
                        }
                    }
                });
                setFileChooserHostListen(fc, hostLabel, listenLabel); // set initial values, if any

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

                        try
                        {
                            context.preferences.setLastHintTrackingOpenFile(file.getAbsolutePath());
                            context.preferences.setLastHintTrackingOpenPath(last.getAbsolutePath());
                            context.preferences.setLastHintTrackingIsRemote(cbIsRemote.isSelected());
                            context.preferences.setLastHintTrackingIsOpen(true);
                            String filename;
                            if (context.preferences.isLastHintTrackingIsRemote())
                            {
                                context.cfg.setHintsDaemonFilename(file.getAbsolutePath());
                                context.cfg.setHintTrackerFilename("");
                                filename = context.cfg.getHintsDaemonFilename();
                            }
                            else
                            {
                                context.cfg.setHintsDaemonFilename("");
                                context.cfg.setHintTrackerFilename(file.getAbsolutePath());
                                filename = context.cfg.getHintTrackerFilename();
                            }
                            context.cfg.setOverrideHintsHost(listenButton.isSelected());
                            context.preferences.setLastOverrideHintHost(listenButton.isSelected());

                            // read the selected Hint Server repository
                            try
                            {
                                context.hintsRepo = new Repository(context, Repository.HINT_SERVER);
                                context.hintsRepo.read(filename, context.cfg.gs("Libraries.hint.server"), false);
                            }
                            catch (Exception e)
                            {
                                context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
                                JOptionPane.showMessageDialog(context.mainFrame,
                                        context.cfg.gs("Navigator.menu.Open.hint.error.opening.hint.library") + e.getMessage(),
                                        context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                context.fault = false;
                                return;
                            }

                            if (context.cfg.isHintTrackingEnabled() && context.cfg.isRemoteStatusServer())
                            {
                                int r = JOptionPane.showConfirmDialog(context.mainFrame,
                                        context.cfg.gs("Navigator.menu.Open.hint.tracking.close.current.status.server"),
                                        context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                                if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                    return;

                                quitByeRemotes(false, true);
                            }

                            // connect to the hint tracker or status server
                            context.main.setupHints(context.publisherRepo);
                            //context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                            context.browser.setupHintTrackingButton();
                            setQuitTerminateVisibility();
                            context.libraries.loadConfigurations();

                            checkForHints();
                            if (dialogHints != null && dialogHints.isVisible())
                                dialogHints.refresh();
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

                if (context.subscriberRepo != null && context.cfg.getSubscriberFilename().length() > 0)
                    context.mainFrame.menuItemCloseSubscriber.setVisible(true);
                else
                    context.mainFrame.menuItemCloseSubscriber.setVisible(false);

                if (context.hintKeys != null && context.cfg.getHintKeysFile().length() > 0)
                    context.mainFrame.menuItemCloseHintKeys.setVisible(true);
                else
                    context.mainFrame.menuItemCloseHintKeys.setVisible(false);

                if (context.hintsRepo != null && context.cfg.getHintHandlerFilename().length() > 0)
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
                    context.preferences.setLastPublisherIsOpen(false);
                    setQuitTerminateVisibility();
                    context.libraries.loadConfigurations();

                    checkForHints();
                    if (dialogHints != null && dialogHints.isVisible())
                        dialogHints.refresh();
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
                    context.preferences.setLastSubscriberIsOpen(false);
                    disconnectSubscriber();
                    context.libraries.loadConfigurations();

                    checkForHints();
                    if (dialogHints != null && dialogHints.isVisible())
                        dialogHints.refresh();
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
                if (context.hintsRepo != null && context.hintsRepo.isInitialized())
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
                    context.hintsHandler = null;
                    context.hintKeys = null;
                    context.cfg.setHintKeysFile("");
                    context.preferences.setLastHintKeysIsOpen(false);
                    context.hintsRepo = null;
                    context.cfg.setHintTrackerFilename("");
                    context.cfg.setHintsDaemonFilename("");
                    context.preferences.setLastHintTrackingIsOpen(false);
                    context.browser.setupHintTrackingButton();
                    context.libraries.loadConfigurations();

                    checkForHints();
                    if (dialogHints != null && dialogHints.isVisible())
                        dialogHints.refresh();
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
                    context.hintsRepo = null;
                    context.cfg.setHintTrackerFilename("");
                    context.cfg.setHintsDaemonFilename("");
                    context.preferences.setLastHintTrackingIsOpen(false);
                    context.browser.setupHintTrackingButton();
                    setQuitTerminateVisibility();

                    checkForHints();
                    if (dialogHints != null && dialogHints.isVisible())
                        dialogHints.refresh();
                }
            }
        });

        // --- Generate
        AbstractAction generateAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Generator generator = new Generator(context, true);
                generator.showDialog(null, null, Configuration.NAVIGATOR_NAME);
            }
        };
        context.mainFrame.menuItemGenerate.addActionListener(generateAction);

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

                    if (context.hintsStty != null)
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

        if (Utils.isOsMac())
        {
            FlatDesktop.setQuitHandler(response -> {
                boolean canQuit = context.mainFrame.verifyClose();
                if (canQuit)
                    context.navigator.stop();
                else
                    response.cancelQuit();
            });
            context.mainFrame.menuItemFileQuit.setVisible(false);
        }
        else
        {
            context.mainFrame.menuItemFileQuit.addActionListener(new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    if (context.mainFrame.verifyClose())
                        context.navigator.stop();
                }
            });

            // hide separator if not a remote operation
            if (!context.cfg.isRemoteOperation())
            {
                for (Component comp : context.mainFrame.menuFile.getComponents())
                {
                    if (comp instanceof JSeparator && ((JSeparator) comp).getName().equalsIgnoreCase("separatorQuit"))
                    {
                        ((JSeparator) comp).setVisible(false);
                    }
                }
            }
        }

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
        context.mainFrame.menuTbCopy.addActionListener(copyAction);

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
        context.mainFrame.menuTbCut.addActionListener(cutAction);

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
        context.mainFrame.menuTbPaste.addActionListener(pasteAction);

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
        context.mainFrame.menuTbDelete.addActionListener(deleteAction);

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
                                            Thread.sleep(1000L); // give the remote time to register new hint file
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
        context.mainFrame.menuTbNewFolder.addActionListener(newFolderAction);

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

                        // check for Hint conflicts after deep scan
                        if (context.navigator.checkForConflicts(tuo, context.cfg.gs("HintsUI.action.rename")))
                            return;

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

        // --- Progress
        context.mainFrame.menuItemProgress.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (context.progress == null)
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
                context.progress.requestFocus();
                context.progress.toFront();
            }
        });

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
        context.mainFrame.menuTbRefresh.addActionListener(refreshAction);

        // --- Auto-Refresh
        context.mainFrame.radioButtonAutoRefresh.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.preferences.setAutoRefresh(!context.preferences.isAutoRefresh());
                if (context.preferences.isAutoRefresh())
                {
                    context.mainFrame.radioButtonAutoRefresh.setSelected(true);
                }
                else
                {
                    context.mainFrame.radioButtonAutoRefresh.setSelected(false);
                }
            }
        });
        // set initial state of Auto-Refresh radioButton
        if (context.preferences.isAutoRefresh())
            context.mainFrame.radioButtonAutoRefresh.setSelected(true);
        else
            context.mainFrame.radioButtonAutoRefresh.setSelected(false);

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

        // --- Show Toolbar
        context.mainFrame.menuItemShowToolbar.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.preferences.setShowToolbar(context.mainFrame.menuItemShowToolbar.isSelected());
                if (context.mainFrame.menuItemShowToolbar.isSelected())
                {
                    context.preferences.setShowToolbar(true);
                    context.mainFrame.panelAlertsMenu.setVisible(false);
                    context.mainFrame.panelToolbar.setVisible(true);
                }
                else
                {
                    context.preferences.setShowToolbar(false);
                    context.mainFrame.panelAlertsMenu.setVisible(true);
                    context.mainFrame.panelToolbar.setVisible(false);
                }
            }
        });
        // set initial state of Show Toolbar checkbox
        if (context.preferences.isShowToolbar())
            context.mainFrame.menuItemShowToolbar.setSelected(true);
        else
            context.mainFrame.menuItemShowToolbar.setSelected(false);

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

        // --- Jobs Manage
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
                    dialogJobs.setVisible(true);
                    dialogJobs.toFront();
                    dialogJobs.requestFocus();
                }
            }
        });

        // see loadJobsMenu() for submenu of list of Jobs

        // -- System Menu
        // --------------------------------------------------------

        // --- Hints
        context.mainFrame.menuItemHints.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                enableDisableSystemMenus(Hints, false);
                dialogHints = new HintsUI(context);
            }
        });

        // --- Authorization Keys
        context.mainFrame.menuItemAuthKeys.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, Authentication);
            }
        });

        // --- Hint Keys
        context.mainFrame.menuItemHintKeys.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, HintKeys);
            }
        });

        // --- Blacklist
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

        // --- Whitelist
        context.mainFrame.menuItemWhitelist.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (fileeditor != null && fileeditor.isVisible())
                    fileeditor.requestFocus();
                else
                    fileeditor = new FileEditor(context, WhiteList);
            }
        });

        // --- Settings
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
            FlatDesktop.setPreferencesHandler(() -> {
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
                if (comp instanceof JSeparator && ((JSeparator) comp).getName().equalsIgnoreCase("separatorSettings"))
                {
                    ((JSeparator) comp).setVisible(false);
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
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.controls.help.title"), "controls_" + context.preferences.getLocale() + ".html", false);
                if (!dialog.fault)
                    dialog.buttonFocus();
            }
        });

        // --- Getting Started
        context.mainFrame.menuItemGettingStarted.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.getting.started"), "gettingstarted_" + context.preferences.getLocale() + ".html", false);
                if (!dialog.fault)
                    dialog.buttonFocus();
            }
        });

        // --- Web Site
        context.mainFrame.menuItemWebSite.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://corionis.github.io/ELS/");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Navigator.error.launching.browser"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                NavHelp dialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.changes.help.title"), "changes_" + context.preferences.getLocale() + ".html", false);
                if (!dialog.fault)
                    dialog.buttonFocus();
            }
        });

        // --- Release Notes
        context.mainFrame.menuItemReleaseNotes.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp helpDialog = new NavHelp(context.mainFrame, context.mainFrame, context, context.cfg.gs("Navigator.release.notes"), "releasenotes_" + context.preferences.getLocale() + ".html", false);
                if (!helpDialog.fault)
                    helpDialog.buttonFocus();
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
            FlatDesktop.setAboutHandler(() -> {
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

        /* Other popup menu items are defined under the Edit menu */

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

        // -- alerts panel
        // --------------------------------------------------------

        //--- Hints alert
        MouseAdapter mad = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                context.mainFrame.menuItemHints.doClick();
            }
        };
        context.mainFrame.labelAlertHintsMenu.addMouseListener(mad);
        context.mainFrame.labelAlertHintsToolbar.addMouseListener(mad);

        //--- Update alert
        mad = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                checkForUpdates(false);
            }
        };
        context.mainFrame.labelAlertUpdateMenu.addMouseListener(mad);
        context.mainFrame.labelAlertUpdateToolbar.addMouseListener(mad);

    }

    public boolean isBlockingProcessRunning()
    {
        return blockingProcessRunning;
    }

    public boolean isLogger()
    {
        return context.cfg.isLoggerView();
    }

    public boolean isUpdaterProcess()
    {
        return updaterProcess;
    }

    public boolean isWorkerRunning()
    {
        return workerRunning;
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
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, true);
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
                            String msg = context.cfg.gs("Z.exception") + entry.getName() + ", " + Utils.getStackTrace(e);
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

    private void processJob(Job job)
    {
        // validate job tasks and origins
        String status = job.validate(context.cfg);
        if (status.length() == 0)
        {
            // make dialog pieces
            String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
            JCheckBox checkbox = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
            checkbox.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
            checkbox.setSelected(context.preferences.isDefaultDryrun());
            Object[] params = {message, checkbox};

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(context.mainFrame, params, context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            boolean isDryRun = checkbox.isSelected();
            if (reply == JOptionPane.YES_OPTION)
            {
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
                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    disableComponent(false, context.mainFrame.getContentPane());
                    context.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                    context.mainFrame.menuItemFileQuit.setEnabled(true);
                    setBlockingProcessRunning(true);
                    setWorkerRunning(true);
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

                    JScrollBar vertical = context.mainFrame.scrollPaneLog.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                else
                    processTerminated(job, isDryRun);
            }
        }
        else
            JOptionPane.showMessageDialog(context.mainFrame, status, context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
    }

    private void processLoggerJob()
    {
        try
        {
            Job tmpJob = new Job(context, "temp");
            Job job = tmpJob.load(context.cfg.getJobName());
            if (job == null)
                logger.error("Job \"" + context.cfg.getJobName() + "\" could not be loaded");
            else
            {
                worker = job.process(context, context.mainFrame, context.cfg.getNavigatorName(), job, context.cfg.isDryRun());
                if (worker != null)
                {
                    setBlockingProcessRunning(true);
                    setWorkerRunning(true);

                    context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    disableComponent(false, context.mainFrame.getContentPane());
                    disableGui(true);

                    context.mainFrame.labelAlertHintsMenu.setVisible(false);
                    context.mainFrame.labelAlertUpdateMenu.setVisible(false);

                    context.mainFrame.tabbedPaneMain.setTitleAt(0, job.getConfigName());
                    context.mainFrame.tabbedPaneMain.remove(1);
                    context.mainFrame.tabbedPaneMain.repaint();

                    context.mainFrame.tabbedPaneNavigatorBottom.setEnabledAt(1, false);
                    context.mainFrame.tabbedPaneNavigatorBottom.remove(1);
                    context.mainFrame.tabbedPaneNavigatorBottom.repaint();

                    context.mainFrame.menuFile.removeAll();
                    context.mainFrame.menuFile.add(context.mainFrame.menuItemFileQuit);
                    context.mainFrame.menuItemFileQuit.setEnabled(true);

                    context.mainFrame.menuEdit.removeAll();
                    context.mainFrame.menuEdit.add(context.mainFrame.menuItemFind);
                    context.mainFrame.menuItemFind.setEnabled(true);
                    context.mainFrame.menuEdit.add(context.mainFrame.menuItemFindNext);
                    context.mainFrame.menuItemFindNext.setEnabled(true);

                    context.mainFrame.menuView.removeAll();
                    context.mainFrame.menuView.add(context.mainFrame.menuItemWordWrap);
                    context.mainFrame.menuItemWordWrap.setEnabled(true);

                    context.mainFrame.menuBookmarks.setVisible(false);
                    context.mainFrame.menuTools.setVisible(false);
                    context.mainFrame.menuJobs.setVisible(false);
                    context.mainFrame.menuSystem.setVisible(false);
                    context.mainFrame.menuWindows.setVisible(false);

                    context.mainFrame.menuItemUpdates.setVisible(false);

                    worker.addPropertyChangeListener(new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange(PropertyChangeEvent e)
                        {
                            if (e.getPropertyName().equals("state"))
                            {
                                if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                    processTerminated(job, context.cfg.isDryRun());
                            }
                        }
                    });
                    worker.execute();

                    JScrollBar vertical = context.mainFrame.scrollPaneLog.getVerticalScrollBar();
                    vertical.setValue(vertical.getMaximum());
                }
                else
                    processTerminated(job, context.cfg.isDryRun());
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
    }

    private void processTerminated(Job job, boolean isDryRun)
    {
        try
        {
            context.mainFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            // reset and reload relevant trees
            if (!isDryRun && !isLogger())
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
            {
                context.progress.done();
                context.progress.dispose();
                context.progress = null;
            }

            setBlockingProcessRunning(false);

            if (originsArray != null && originsArray.size() == 8)
                Origins.setAllOrigins(context, context.mainFrame, originsArray);

            if (!isLogger())
            {
                disableGui(false);
                reconnectRemote(context, context.publisherRepo, context.subscriberRepo);
            }
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
        {
            if (!context.main.isListening())
            {
                String msg = java.text.MessageFormat.format(context.cfg.gs(context.fault ? "Job.failed.job" : "Job.completed.job"),
                        job.getConfigName() + (context.cfg.isDryRun() ? context.cfg.gs("Z.dry.run") : ""));
                logger.info(msg);
                context.mainFrame.labelStatusMiddle.setText(msg);
                context.main.stopVerbiage();
            }
        }

        setWorkerRunning(false);
    }

    public void quitByeRemotes(boolean elsListener, boolean hintStatusServer)
    {
        boolean closure = false;
        if (elsListener && context.clientStty != null)
        {
            try
            {
                closure = true;
                if (context.clientSftp != null)
                {
                    context.clientSftp.stopClient();
                }
                if (context.clientSftpMetadata != null)
                {
                    context.clientSftpMetadata.stopClient();
                }

                if (context.clientStty != null && context.clientStty.isConnected())
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

                        context.clientStty.disconnect();
                    }
                }
                context.clientStty = null;
                context.clientSftp = null;
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
                    context.hintsStty.send("quit", "Sending quit command to remote Hint Status Server");
                else
                    context.hintsStty.send("bye", "Sending bye command to remote Hint Status Server");

                context.hintsStty.disconnect();
                context.hintsStty = null;
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
            context.clientStty = new ClientStty(context, false, true, false);
            if (!context.clientStty.connect(publisherRepo, subscriberRepo))
            {
                context.cfg.setOperation("-");
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

            // start the serveSftp transfer client
            context.clientSftp = new ClientSftp(context, publisherRepo, subscriberRepo, true);
            if (!context.clientSftp.startClient("transfer"))
            {
                context.cfg.setOperation("-");
                if (context.navigator != null)
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }

            // start the serveSftp metadata client
            context.clientSftpMetadata = new ClientSftp(context, context.publisherRepo, context.subscriberRepo, true);
            if (!context.clientSftpMetadata.startClient("metadata"))
            {
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
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                // TODO EXTEND+ Add as needed: Set command line overrides on Navigator Preferences

                // preserve file times
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

                    if (!isLogger())
                    {
                        if (checkForUpdates(true))
                        {
                            logger.info(context.cfg.gs("Navigator.update.available"));
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.update.available"));
                            context.mainFrame.labelAlertUpdateMenu.setVisible(true);
                            context.mainFrame.labelAlertUpdateToolbar.setVisible(true);
                        }
                        else
                        {
                            logger.info(context.cfg.gs("Navigator.installed.up.to.date"));
                            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Navigator.installed.up.to.date"));
                            context.mainFrame.labelAlertUpdateMenu.setVisible(false);
                            context.mainFrame.labelAlertUpdateToolbar.setVisible(false);
                        }

                        checkForHints();
                    }

                    if (context.preferences.isShowToolbar() && !isLogger())
                    {
                        context.mainFrame.panelAlertsMenu.setVisible(false);
                        context.mainFrame.panelToolbar.setVisible(true);
                    }
                    else
                    {
                        context.mainFrame.panelAlertsMenu.setVisible(true);
                        context.mainFrame.panelToolbar.setVisible(false);
                    }

                    context.mainFrame.setVisible(true);

                    context.preferences.fixBrowserDivider(context, -1);
                    context.mainFrame.treeCollectionOne.requestFocus();

                    if (context.cfg.isLoggerView())
                    {
                        context.navigator.processLoggerJob();
                    }
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

    public void setBlockingProcessRunning(boolean blockingProcessRunning)
    {
        this.blockingProcessRunning = blockingProcessRunning;
    }

    private boolean setFileChooserHostListen(JFileChooser fc, JLabel hostField, JLabel listenField)
    {
        String host = "";
        String listen = "";
        try
        {
            Repository repo = new Repository(context, Repository.SUBSCRIBER);
            repo.read(fc.getSelectedFile().getAbsolutePath(), "Subscriber", true);
            host = repo.getLibraryData().libraries.host;
            listen = repo.getLibraryData().libraries.listen;
        }
        catch (Exception e)
        {
            hostField.setText("");
            listenField.setText("");
            return false;
        }
        hostField.setText(host);
        listenField.setText(listen);
        return true;
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

    public void setWorkerRunning(boolean sense)
    {
        workerRunning = sense;
    }

    public void stop()
    {
        //if (context.cfg.isRemoteActive())
        //    context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Main.disconnecting"));

        quitByeRemotes(true, true);

        if (context.mainFrame != null)
        {
            if (!context.main.secondaryNavigator && !isLogger())
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
            }

            context.mainFrame.setVisible(false);
            context.mainFrame.dispose();
        }

        // end the Navigator Swing thread
        if (!context.main.secondaryNavigator)
        {
            if (isUpdaterProcess())
            {
                try
                {
                    String cmd = "";
                    String[] parms = {Utils.getTempUpdaterDirectory() + System.getProperty("file.separator") +
                            "rt" + System.getProperty("file.separator") +
                            "bin" + System.getProperty("file.separator") +
                            "java" + (Utils.isOsWindows() ? ".exe" : ""),
                            "-jar",
                            updaterJar};
                    for (int i = 0; i < parms.length; ++i)
                    {
                        cmd += parms[i] + " ";
                    }

                    logger.info(context.cfg.gs("Navigator.starting.els.updater") + cmd);

                    Process proc = Runtime.getRuntime().exec(parms);
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

            if (!context.cfg.isLoggerView())
                context.main.shutdown();

            System.exit(0);
        }
        else
        {
            GuiLogAppender appender = context.main.guiLogAppender;
            appender.setContext(context.main.previousContext);
        }
    }

}
