package com.groksoft.els.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.groksoft.els.*;
import com.groksoft.els.gui.operations.OperationsUI;
import com.groksoft.els.gui.bookmarks.Bookmark;
import com.groksoft.els.gui.bookmarks.Bookmarks;
import com.groksoft.els.gui.browser.Browser;
import com.groksoft.els.gui.browser.NavTransferHandler;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.gui.jobs.JobsUI;
import com.groksoft.els.gui.tools.duplicateFinder.DuplicateFinderUI;
import com.groksoft.els.gui.tools.emptyDirectoryFinder.EmptyDirectoryFinderUI;
import com.groksoft.els.gui.tools.junkRemover.JunkRemoverUI;
import com.groksoft.els.gui.tools.renamer.RenamerUI;
import com.groksoft.els.gui.util.GuiLogAppender;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Hints;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.stty.ClientStty;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.AbstractConfiguration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.*;

public class Navigator
{
    public static GuiContext guiContext;
    public Bookmarks bookmarks;
    public DuplicateFinderUI dialogDuplicateFinder;
    public EmptyDirectoryFinderUI dialogEmptyDirectoryFinder;
    public JobsUI dialogJobs = null;
    public JunkRemoverUI dialogJunkRemover = null;
    private Settings dialogSettings = null;
    public Job[] jobs;
    private int lastFindPosition = 0;
    private String lastFindString = "";
    private int lastFindTab = -1;
    public boolean showHintTrackingButton = false;
    private int bottomSize;
    private boolean quitRemote = false;
    private boolean remoteJobRunning = false;
    public RenamerUI dialogRenamer = null;
    private transient Logger logger = LogManager.getLogger("applog");
    public SwingWorker<Void, Void> worker;


    public Navigator(Main main, Configuration config, Context ctx)
    {
        guiContext = new GuiContext();
        guiContext.cfg = config;
        guiContext.context = ctx;
        guiContext.navigator = this;
    }

    public void disableGui(boolean disable)
    {
        boolean enable = !disable;

        if (enable == false)
        {
            bottomSize = guiContext.preferences.getBrowserBottomSize();
        }
        guiContext.mainFrame.panelBrowserTop.setVisible(enable);

        guiContext.mainFrame.menuItemOpenPublisher.setEnabled(enable);
        guiContext.mainFrame.menuItemOpenSubscriber.setEnabled(enable);
        guiContext.mainFrame.menuItemOpenHintKeys.setEnabled(enable);
        // TODO guiContext.mainFrame.menuItemOpenHintServer.setEnabled(disable);

        guiContext.mainFrame.menuItemFind.setEnabled(enable);
        guiContext.mainFrame.menuItemFindNext.setEnabled(enable);
        guiContext.mainFrame.menuItemNewFolder.setEnabled(enable);
        guiContext.mainFrame.menuItemRename.setEnabled(enable);
        guiContext.mainFrame.menuItemTouch.setEnabled(enable);
        guiContext.mainFrame.menuItemCopy.setEnabled(enable);
        guiContext.mainFrame.menuItemCut.setEnabled(enable);
        guiContext.mainFrame.menuItemPaste.setEnabled(enable);
        guiContext.mainFrame.menuItemDelete.setEnabled(enable);

        guiContext.mainFrame.menuItemRefresh.setEnabled(enable);
        guiContext.mainFrame.menuItemAutoRefresh.setEnabled(enable);
        guiContext.mainFrame.menuItemShowHidden.setEnabled(enable);
        guiContext.mainFrame.menuItemWordWrap.setEnabled(enable);

        for (int i = 0; i < guiContext.mainFrame.menuBookmarks.getItemCount(); ++i)
        {
            if (guiContext.mainFrame.menuBookmarks.getItem(i) != null)
                guiContext.mainFrame.menuBookmarks.getItem(i).setEnabled(enable);
        }

        for (int i = 0; i < guiContext.mainFrame.menuTools.getItemCount(); ++i)
        {
            if (guiContext.mainFrame.menuTools.getItem(i) != null)
                guiContext.mainFrame.menuTools.getItem(i).setEnabled(enable);
        }

        for (int i = 0; i < guiContext.mainFrame.menuJobs.getItemCount(); ++i)
        {
            if (guiContext.mainFrame.menuJobs.getItem(i) != null)
                guiContext.mainFrame.menuJobs.getItem(i).setEnabled(enable);
        }

        guiContext.mainFrame.menuItemSplitHorizontal.setEnabled(enable);
        guiContext.mainFrame.menuItemSplitVertical.setEnabled(enable);

        if (enable == true)
            guiContext.preferences.fixBrowserDivider(guiContext, bottomSize);

        // TODO guiContext.mainFrame.menuItemUpdates.setEnabled(disable);
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
        guiContext.context.main.loadLocale(guiContext.preferences.getLocale());

        guiContext.context.main.savedEnvironment = new SavedEnvironment(guiContext, guiContext.cfg, guiContext.context);
        guiContext.context.main.savedEnvironment.save();

        if (guiContext.cfg.getPublisherCollectionFilename().length() > 0)
        {
            guiContext.preferences.setLastIsWorkstation(false);
            guiContext.preferences.setLastPublisherOpenFile(guiContext.cfg.getPublisherCollectionFilename());
            guiContext.preferences.setLastPublisherOpenPath(Utils.getLeftPath(guiContext.cfg.getPublisherCollectionFilename(),
                    Utils.getSeparatorFromPath(guiContext.cfg.getPublisherCollectionFilename())));
        }
        else if (guiContext.cfg.getPublisherLibrariesFileName().length() > 0)
        {
            guiContext.preferences.setLastIsWorkstation(true);
            guiContext.preferences.setLastPublisherOpenPath(guiContext.cfg.getPublisherLibrariesFileName());
            guiContext.preferences.setLastPublisherOpenPath(Utils.getLeftPath(guiContext.cfg.getPublisherLibrariesFileName(),
                    Utils.getSeparatorFromPath(guiContext.cfg.getPublisherLibrariesFileName())));
        }

        if (guiContext.cfg.getSubscriberCollectionFilename().length() > 0)
        {
            guiContext.preferences.setLastIsRemote(true);
            guiContext.preferences.setLastSubscriberOpenFile(guiContext.cfg.getSubscriberCollectionFilename());
            guiContext.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(guiContext.cfg.getSubscriberCollectionFilename(),
                    Utils.getSeparatorFromPath(guiContext.cfg.getSubscriberCollectionFilename())));
        }
        else if (guiContext.cfg.getSubscriberLibrariesFileName().length() > 0)
        {
            guiContext.preferences.setLastIsRemote(false);
            guiContext.preferences.setLastSubscriberOpenFile(guiContext.cfg.getSubscriberLibrariesFileName());
            guiContext.preferences.setLastSubscriberOpenPath(Utils.getLeftPath(guiContext.cfg.getSubscriberLibrariesFileName(),
                    Utils.getSeparatorFromPath(guiContext.cfg.getSubscriberLibrariesFileName())));
        }

        // setup the needed tools
        guiContext.context.transfer = new Transfer(guiContext.cfg, guiContext.context, guiContext);
        try
        {
            guiContext.context.transfer.initialize();

            if (guiContext.cfg.getHintKeysFile() != null && guiContext.cfg.getHintKeysFile().length() > 0)
            {
                // Get ELS hints keys & Tracker if specified
                guiContext.context.hintKeys = new HintKeys(guiContext.cfg, guiContext.context);
                guiContext.context.hintKeys.read(guiContext.cfg.getHintKeysFile());
                guiContext.hints = new Hints(guiContext.cfg, guiContext.context, guiContext.context.hintKeys);
                showHintTrackingButton = true;

                File json = new File(guiContext.cfg.getHintKeysFile());
                String path = json.getAbsolutePath();
                guiContext.preferences.setLastHintKeysOpenFile(path);
                guiContext.preferences.setLastHintKeysOpenPath(FilenameUtils.getFullPathNoEndSeparator(path));
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
            return false;
        }

        // setup the GUI
        guiContext.operationsUI = new OperationsUI(guiContext);
        guiContext.mainFrame = new MainFrame(guiContext);
        if (!guiContext.context.fault)
        {
            // setup the Main Menu and primary tabs
            initializeMainMenu();
            guiContext.browser = new Browser(guiContext);
            guiContext.operationsUI.initialize();
            // TODO Add Library tab content creation here

            // disable back-fill because we never know what combination of items might be selected
            guiContext.cfg.setNoBackFill(true);

            guiContext.cfg.setPreserveDates(guiContext.preferences.isPreserveFileTimes());

            // set the TextArea for the GUI log
            LoggerContext loggerContext = (LoggerContext) LogManager.getContext(false);
            AbstractConfiguration loggerContextConfiguration = (AbstractConfiguration) loggerContext.getConfiguration();
            LoggerConfig loggerConfig = loggerContextConfiguration.getLoggerConfig("applog");
            Map<String, Appender> appenders = loggerConfig.getAppenders();
            GuiLogAppender appender = (GuiLogAppender) appenders.get("GuiLogAppender");
            appender.setTextArea(guiContext);

            // show banner in Navigator log tabs
            if (guiContext.cfg.isRemoteSession())
                logger.info("ELS: Remote Navigator, version " + guiContext.cfg.getVersionStamp());
            else
                logger.info("ELS: Local Navigator, version " + guiContext.cfg.getVersionStamp());

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
        return !guiContext.context.fault;
    }

    private void initializeMainMenu()
    {
        // --- Main Menu ------------------------------------------
        //
        // -- File Menu
        // ---
        // Open Publisher
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
                        return guiContext.cfg.gs("Navigator.menu.Open.publisher.files");
                    }
                });
                fc.setDialogTitle(guiContext.cfg.gs("Navigator.menu.Open.publisher"));
                fc.setFileHidingEnabled(false);
                if (guiContext.preferences.getLastPublisherOpenPath().length() > 0)
                {
                    File ld = new File(guiContext.preferences.getLastPublisherOpenPath());
                    if (ld.exists() && ld.isDirectory())
                        fc.setCurrentDirectory(ld);
                }
                if (guiContext.preferences.getLastPublisherOpenFile().length() > 0)
                {
                    File lf = new File(guiContext.preferences.getLastPublisherOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Workstation/Collection radio button accessory
                JPanel jp = new JPanel();
                GridBagLayout layout = new GridBagLayout();

                jp.setLayout(layout);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(guiContext.mainFrame.textFieldLocation.getBorder());

                JLabel lab = new JLabel(guiContext.cfg.gs("Navigator.menu.Open.publisher.system.type"));

                JRadioButton rbCollection = new JRadioButton(guiContext.cfg.gs("Navigator.menu.Open.publisher.collection.radio"));
                rbCollection.setToolTipText(guiContext.cfg.gs("Navigator.menu.Open.publisher.collection.radio.tooltip"));
                rbCollection.setSelected(!guiContext.preferences.isLastIsWorkstation());

                JRadioButton rbWorkstation = new JRadioButton(guiContext.cfg.gs("Navigator.menu.Open.publisher.workstation.radio"));
                rbWorkstation.setToolTipText(guiContext.cfg.gs("Navigator.menu.Open.publisher.workstation.radio.tooltip"));
                rbWorkstation.setSelected(guiContext.preferences.isLastIsWorkstation());

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
                    int selection = fc.showOpenDialog(guiContext.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        boolean isWorkstation = rbWorkstation.isSelected();
                        guiContext.preferences.setLastIsWorkstation(isWorkstation);
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastPublisherOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.open.error.file.not.found") + file.getName(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.open.error.select.a.file.only"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            guiContext.preferences.setLastPublisherOpenFile(file.getAbsolutePath());
                            if (isWorkstation)
                            {
                                guiContext.cfg.setPublisherCollectionFilename("");
                                guiContext.cfg.setPublisherLibrariesFileName(file.getAbsolutePath());
                            }
                            else
                            {
                                guiContext.cfg.setPublisherCollectionFilename(file.getAbsolutePath());
                                guiContext.cfg.setPublisherLibrariesFileName("");
                            }
                            guiContext.context.publisherRepo = guiContext.context.main.readRepo(guiContext.cfg, Repository.PUBLISHER, Repository.VALIDATE);
                            guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionOne, guiContext.context.publisherRepo, false);
                            guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemOne, guiContext.context.publisherRepo,false);
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Open.publisher.error.opening.publisher.library") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.mainFrame.menuItemOpenPublisher.addActionListener(openPublisherAction);

        // ---
        // Open Subscriber
        AbstractAction openSubscriberAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.context.publisherRepo == null)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Open.subscriber.please.open.a.publisher.library.first"), guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                        return guiContext.cfg.gs("Navigator.menu.Open.subscriber.files");
                    }
                });
                fc.setDialogTitle(guiContext.cfg.gs("Navigator.menu.Open.subscriber"));
                fc.setFileHidingEnabled(false);
                if (guiContext.preferences.getLastSubscriberOpenPath().length() > 0)
                {
                    File ld = new File(guiContext.preferences.getLastSubscriberOpenPath());
                    if (ld.exists() && ld.isDirectory())
                        fc.setCurrentDirectory(ld);
                }
                if (guiContext.preferences.getLastSubscriberOpenFile().length() > 0)
                {
                    File lf = new File(guiContext.preferences.getLastSubscriberOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Remote Connection checkbox accessory
                JPanel jp = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                jp.setLayout(gb);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(guiContext.mainFrame.textFieldLocation.getBorder());
                JCheckBox cbIsRemote = new JCheckBox("<html><head><style>body{margin-left:4px;}</style></head><body>" +
                        guiContext.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox") + "</body></html>");
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(guiContext.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox.tooltip"));
                cbIsRemote.setSelected(guiContext.preferences.isLastIsRemote());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 0, 0, 8);
                gb.setConstraints(cbIsRemote, gbc);
                jp.add(cbIsRemote);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(guiContext.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        if (guiContext.cfg.isRemoteSession() && guiContext.context.clientStty.isConnected())
                        {
                            int r = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.menu.Open.subscriber.close.current.remote.connection"),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                return;

                            try
                            {
                                guiContext.context.clientStty.send("bye", "Sending bye command to remote");
                            }
                            catch (Exception e)
                            {
                            }
                            guiContext.context.clientStty.disconnect();
                            guiContext.context.clientSftp.stopClient();
                        }

                        guiContext.preferences.setLastIsRemote(cbIsRemote.isSelected());
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastSubscriberOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            // this defines the value returned by guiContext.cfg.isRemoteSession()
                            if (guiContext.preferences.isLastIsRemote())
                            {
                                guiContext.cfg.setRemoteType("P"); // publisher to remote subscriber
                                guiContext.mainFrame.menuItemQuitTerminate.setVisible(true);
                            }
                            else
                            {
                                guiContext.cfg.setRemoteType("-"); // not remote
                                guiContext.mainFrame.menuItemQuitTerminate.setVisible(false);
                            }

                            guiContext.preferences.setLastSubscriberOpenFile(file.getAbsolutePath());
                            guiContext.cfg.setSubscriberLibrariesFileName(file.getAbsolutePath());
                            guiContext.cfg.setSubscriberCollectionFilename("");
                            guiContext.context.subscriberRepo = guiContext.context.main.readRepo(guiContext.cfg, Repository.SUBSCRIBER, !guiContext.preferences.isLastIsRemote());

                            if (guiContext.preferences.isLastIsRemote())
                            {
                                // connect to the hint status server if defined
                                guiContext.context.main.connectHintServer(guiContext.context.publisherRepo);

                                // start the serveStty client for automation
                                guiContext.context.clientStty = new ClientStty(guiContext.cfg, guiContext.context, false, true);
                                if (!guiContext.context.clientStty.connect(guiContext.context.publisherRepo, guiContext.context.subscriberRepo))
                                {
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }
                                if (guiContext.context.clientStty.checkBannerCommands())
                                {
                                    logger.info(guiContext.cfg.gs("Transfer.received.subscriber.commands") + (guiContext.cfg.isRequestCollection() ? "RequestCollection " : "") + (guiContext.cfg.isRequestTargets() ? "RequestTargets" : ""));
                                }

                                // start the serveSftp client
                                guiContext.context.clientSftp = new ClientSftp(guiContext.cfg, guiContext.context.publisherRepo, guiContext.context.subscriberRepo, true);
                                if (!guiContext.context.clientSftp.startClient())
                                {
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }
                            }

                            // load the subscriber library
                            guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, guiContext.preferences.isLastIsRemote());
                            guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemTwo, guiContext.context.subscriberRepo, guiContext.preferences.isLastIsRemote());
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.menu.Open.subscriber.error.opening.subscriber.library") + e.getMessage(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.mainFrame.menuItemOpenSubscriber.addActionListener(openSubscriberAction);
        if (guiContext.context.subscriberRepo != null)
            guiContext.preferences.setLastIsRemote(guiContext.cfg.isRemoteSession());

        // ---
        // Open Hint Keys
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
                        return guiContext.cfg.gs("Navigator.menu.Open.hint.keys.files");
                    }
                });
                fc.setDialogTitle(guiContext.cfg.gs("Navigator.menu.Open.hint.keys"));
                fc.setFileHidingEnabled(false);
                if (guiContext.preferences.getLastHintKeysOpenPath().length() > 0)
                {
                    File ld = new File(guiContext.preferences.getLastHintKeysOpenPath());
                    if (ld.exists() && ld.isDirectory())
                        fc.setCurrentDirectory(ld);
                }
                if (guiContext.preferences.getLastHintKeysOpenFile().length() > 0)
                {
                    File lf = new File(guiContext.preferences.getLastHintKeysOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                while (true)
                {
                    int selection = fc.showOpenDialog(guiContext.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastHintKeysOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            guiContext.preferences.setLastHintKeysOpenFile(file.getAbsolutePath());
                            guiContext.cfg.setHintKeysFile(file.getAbsolutePath());
                            guiContext.context.hintKeys = new HintKeys(guiContext.cfg, guiContext.context);
                            guiContext.context.hintKeys.read(guiContext.cfg.getHintKeysFile());
                            if (!showHintTrackingButton)
                            {
                                showHintTrackingButton = true;
                                guiContext.mainFrame.panelHintTracking.setVisible(true);
                                guiContext.mainFrame.buttonHintTracking.doClick();
                            }
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.menu.Open.hint.keys.error.opening.hint.keys") + e.getMessage(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.mainFrame.menuItemOpenHintKeys.addActionListener(openHintKeysAction);

        // ---
        // Open Hint Tracking
        AbstractAction openHintTracking = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.cfg.getHintKeysFile() == null || guiContext.cfg.getHintKeysFile().length() == 0)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.menu.Open.hint.tracking.please.open.a.publisher.library.first"), guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                        return guiContext.cfg.gs("Navigator.menu.Open.hint.tracking.files");
                    }
                });
                fc.setDialogTitle(guiContext.cfg.gs("Navigator.menu.Open.hint.tracking"));
                fc.setFileHidingEnabled(false);
                if (guiContext.preferences.getLastSubscriberOpenPath().length() > 0)
                {
                    File ld = new File(guiContext.preferences.getLastSubscriberOpenPath());
                    if (ld.exists() && ld.isDirectory())
                        fc.setCurrentDirectory(ld);
                }
                if (guiContext.preferences.getLastSubscriberOpenFile().length() > 0)
                {
                    File lf = new File(guiContext.preferences.getLastSubscriberOpenFile());
                    if (lf.exists())
                        fc.setSelectedFile(lf);
                }

                // Remote Connection checkbox accessory
                JPanel jp = new JPanel();
                GridBagLayout gb = new GridBagLayout();
                jp.setLayout(gb);
                jp.setBackground(UIManager.getColor("TextField.background"));
                jp.setBorder(guiContext.mainFrame.textFieldLocation.getBorder());
                JCheckBox cbIsRemote = new JCheckBox("<html><head><style>body{margin-left:4px;}</style></head><body>" +
                        guiContext.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox") + "</body></html>");
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText(guiContext.cfg.gs("Navigator.menu.Open.subscriber.connection.checkbox.tooltip"));
                cbIsRemote.setSelected(guiContext.preferences.isLastIsRemote());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 0, 0, 8);
                gb.setConstraints(cbIsRemote, gbc);
                jp.add(cbIsRemote);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(guiContext.mainFrame);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        if (guiContext.cfg.isRemoteSession() && guiContext.context.clientStty.isConnected())
                        {
                            int r = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.menu.Open.subscriber.close.current.remote.connection"),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);

                            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                return;

                            try
                            {
                                guiContext.context.clientStty.send("bye", "Sending bye command to remote");
                            }
                            catch (Exception e)
                            {
                            }
                            guiContext.context.clientStty.disconnect();
                            guiContext.context.clientSftp.stopClient();
                        }

                        guiContext.preferences.setLastIsRemote(cbIsRemote.isSelected());
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastSubscriberOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            // this defines the value returned by guiContext.cfg.isRemoteSession()
                            if (guiContext.preferences.isLastIsRemote())
                            {
                                guiContext.cfg.setRemoteType("P"); // publisher to remote subscriber
                                guiContext.mainFrame.menuItemQuitTerminate.setVisible(true);
                            }
                            else
                            {
                                guiContext.cfg.setRemoteType("-"); // not remote
                                guiContext.mainFrame.menuItemQuitTerminate.setVisible(false);
                            }

                            guiContext.preferences.setLastSubscriberOpenFile(file.getAbsolutePath());
                            guiContext.cfg.setSubscriberLibrariesFileName(file.getAbsolutePath());
                            guiContext.cfg.setSubscriberCollectionFilename("");
                            guiContext.context.subscriberRepo = guiContext.context.main.readRepo(guiContext.cfg, Repository.SUBSCRIBER, !guiContext.preferences.isLastIsRemote());

                            if (guiContext.preferences.isLastIsRemote())
                            {
                                // connect to the hint status server if defined
                                guiContext.context.main.connectHintServer(guiContext.context.publisherRepo);

                                // start the serveStty client for automation
                                guiContext.context.clientStty = new ClientStty(guiContext.cfg, guiContext.context, false, true);
                                if (!guiContext.context.clientStty.connect(guiContext.context.publisherRepo, guiContext.context.subscriberRepo))
                                {
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }
                                if (guiContext.context.clientStty.checkBannerCommands())
                                {
                                    logger.info(guiContext.cfg.gs("Transfer.received.subscriber.commands") + (guiContext.cfg.isRequestCollection() ? "RequestCollection " : "") + (guiContext.cfg.isRequestTargets() ? "RequestTargets" : ""));
                                }

                                // start the serveSftp client
                                guiContext.context.clientSftp = new ClientSftp(guiContext.cfg, guiContext.context.publisherRepo, guiContext.context.subscriberRepo, true);
                                if (!guiContext.context.clientSftp.startClient())
                                {
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }
                            }

                            // load the subscriber library
                            guiContext.browser.loadCollectionTree(guiContext.mainFrame.treeCollectionTwo, guiContext.context.subscriberRepo, guiContext.preferences.isLastIsRemote());
                            guiContext.browser.loadSystemTree(guiContext.mainFrame.treeSystemTwo, guiContext.context.subscriberRepo, guiContext.preferences.isLastIsRemote());
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Navigator.menu.Open.subscriber.error.opening.subscriber.library") + e.getMessage(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
/*
        guiContext.mainFrame.menuItemOpenSubscriber.addActionListener(openSubscriberAction);
        if (guiContext.context.subscriberRepo != null)
            guiContext.preferences.setLastIsRemote(guiContext.cfg.isRemoteSession());
*/

        // Save Layout
        AbstractAction saveLayoutAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    guiContext.preferences.write(guiContext);
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.Save.layout.error.saving.layout") + e.getMessage(),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.mainFrame.menuItemSaveLayout.addActionListener(saveLayoutAction);

        // ---
        // Quit & Stop Remote
        guiContext.mainFrame.menuItemQuitTerminate.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.mainFrame.verifyClose())
                {
                    int r = JOptionPane.showConfirmDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.QuitTerminate.stop.remote"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                    if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                        quitRemote = false;
                    else
                        quitRemote = true;
                    stop();
                }
            }
        });
        if (!guiContext.cfg.isRemoteSession())
            guiContext.mainFrame.menuItemQuitTerminate.setVisible(false);

        // ---
        // Quit
        // Handled in MainFrame.menuItemFileQuitActionPerformed()

        //
        // -- Edit Menu
        // ---
        // Find in Log
        AbstractAction findAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String name;
                lastFindTab = guiContext.mainFrame.tabbedPaneMain.getSelectedIndex();
                if (lastFindTab == 0)
                    name = guiContext.cfg.gs("Navigator.splitPane.Browser.tab.title");
                else if (lastFindTab == 1)
                    name = guiContext.cfg.gs("Navigator.splitPane.Operations.tab.title");
                else
                {
                    lastFindTab = -1;
                    return;
                }

                Object obj = JOptionPane.showInputDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Operations.find"),
                        name, JOptionPane.QUESTION_MESSAGE,
                        null, null, lastFindString);
                lastFindString = (String) obj;
                if (lastFindString != null && lastFindString.length() > 0)
                {
                    String content;
                    if (lastFindTab == 0)
                        content = guiContext.mainFrame.textAreaLog.getText().toLowerCase();
                    else
                        content = guiContext.mainFrame.textAreaOperationLog.getText().toLowerCase();
                    lastFindPosition = content.indexOf(lastFindString.toLowerCase(), 0);
                    if (lastFindPosition > 0)
                    {
                        if (lastFindTab == 0)
                        {
                            guiContext.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                            guiContext.mainFrame.textAreaLog.requestFocus();
                            guiContext.mainFrame.textAreaLog.setSelectionStart(lastFindPosition);
                            guiContext.mainFrame.textAreaLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                        }
                        else
                        {
                            guiContext.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                            guiContext.mainFrame.textAreaOperationLog.requestFocus();
                            guiContext.mainFrame.textAreaOperationLog.setSelectionStart(lastFindPosition);
                            guiContext.mainFrame.textAreaOperationLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                        }
                        lastFindPosition += lastFindString.length();
                    }
                }
            }
        };
        guiContext.mainFrame.menuItemFind.addActionListener(findAction);
        guiContext.mainFrame.popupMenuItemFind.addActionListener(findAction);
        guiContext.mainFrame.popupMenuItemOperationFind.addActionListener(findAction);

        // ---
        // Find Next
        AbstractAction findNextAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (lastFindTab < 0 || lastFindString.length() == 0)
                    return;
                String content;
                if (lastFindTab == 0)
                    content = guiContext.mainFrame.textAreaLog.getText().toLowerCase();
                else
                    content = guiContext.mainFrame.textAreaOperationLog.getText().toLowerCase();
                if (content != null && content.length() > 0)
                {
                    lastFindPosition = content.indexOf(lastFindString.toLowerCase(), lastFindPosition);
                    if (lastFindPosition > 0)
                    {
                        if (lastFindTab == 0)
                        {
                            guiContext.mainFrame.tabbedPaneMain.setSelectedIndex(0);
                            guiContext.mainFrame.textAreaLog.requestFocus();
                            try
                            {
                                Rectangle rect = guiContext.mainFrame.textAreaLog.modelToView(lastFindPosition);
                                guiContext.mainFrame.textAreaLog.scrollRectToVisible(rect);
                            }
                            catch (Exception e)
                            {
                                System.out.println("bad scroll position");
                            }
                            guiContext.mainFrame.textAreaLog.setSelectionStart(lastFindPosition);
                            guiContext.mainFrame.textAreaLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                        }
                        else
                        {
                            guiContext.mainFrame.tabbedPaneMain.setSelectedIndex(1);
                            guiContext.mainFrame.textAreaOperationLog.requestFocus();
                            try
                            {
                                Rectangle rect = guiContext.mainFrame.textAreaOperationLog.modelToView(lastFindPosition);
                                guiContext.mainFrame.textAreaOperationLog.scrollRectToVisible(rect);
                            }
                            catch (Exception e)
                            {
                                System.out.println("bad scroll position");
                            }
                            guiContext.mainFrame.textAreaOperationLog.setSelectionStart(lastFindPosition);
                            guiContext.mainFrame.textAreaOperationLog.setSelectionEnd(lastFindPosition + lastFindString.length());
                        }
                        lastFindPosition += lastFindString.length();
                    }
                }
            }
        };
        guiContext.mainFrame.menuItemFindNext.addActionListener(findNextAction);
        guiContext.mainFrame.popupMenuItemFindNext.addActionListener(findNextAction);
        guiContext.mainFrame.popupMenuItemOperationFindNext.addActionListener(findNextAction);

        // ---
        // New Folder
        AbstractAction newFolderAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                boolean tooMany = false;
                JTree tree = null;
                NavTreeUserObject tuo = null;
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    tree = (JTree) object;
                }
                else if (object instanceof JTable)
                {
                    tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
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
                    String path = guiContext.browser.selectLibrarySource(tuo);
                    if (path == null || path.length() < 1)
                    {
                        JOptionPane.showMessageDialog(guiContext.mainFrame,
                                guiContext.cfg.gs("Navigator.menu.New.folder.cannot.create.new.folder.in.current.location"),
                                guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    else if (path.equals("_cancelled_"))
                        return;

                    boolean error = false;
                    String reply = "";
                    if (path.length() > 0)
                    {
                        reply = JOptionPane.showInputDialog(guiContext.mainFrame,
                                guiContext.cfg.gs("Navigator.menu.New.folder.for") + path + ": ",
                                guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE);
                        if (reply != null && reply.length() > 0)
                        {
                            NavTreeUserObject createdTuo = null;
                            try
                            {
                                path = path + Utils.getSeparatorFromPath(path) + reply;
                                String msg = guiContext.cfg.gs("Navigator.menu.New.folder.creating") +
                                        (tuo.isRemote ? guiContext.cfg.gs("Z.remote.lowercase") + " " : "") +
                                        guiContext.cfg.gs("Navigator.menu.New.folder.directory") + ": " + path;
                                logger.info(msg);

                                if (guiContext.context.transfer.makeDirs((tuo.isRemote ? path + Utils.getSeparatorFromPath(path) + "dummyfile.els" : path), true, tuo.isRemote))
                                {
                                    // make tuo and add node
                                    NavTreeNode createdNode = new NavTreeNode(guiContext, tuo.node.getMyRepo(), tree);
                                    if (tuo.isRemote)
                                    {
                                        Thread.sleep(500L); // give the remote time to register new hint file
                                        SftpATTRS attrs = guiContext.context.clientSftp.stat(path);
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
                                    logger.error(guiContext.cfg.gs("Navigator.menu.New.folder.directory.not.created.check.permissions"));
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.menu.New.folder.directory.not.created.check.permissions"),
                                            guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                                }
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.mainFrame,
                                        guiContext.cfg.gs("Navigator.menu.New.folder.error.creating") +
                                                (tuo.isRemote ? guiContext.cfg.gs("Z.remote.lowercase") + " " : "") +
                                                guiContext.cfg.gs("Navigator.menu.New.folder.directory") + ": " +
                                                e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                error = true;
                            }

                            if (!error)
                            {
                                guiContext.browser.refreshByObject(tree);
                                if (object instanceof JTree)
                                    tuo.node.selectMe();
                                else
                                {
                                    // update table & select relevant row
                                    tuo.node.loadTable();
                                    if (createdTuo != null)
                                    {
                                        int row = guiContext.browser.findRowIndex((JTable) object, createdTuo);
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
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.New.folder.please.select.a.single.destination.for.a.new.folder"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.mainFrame.menuItemNewFolder.addActionListener(newFolderAction);
        guiContext.mainFrame.popupMenuItemNewFolder.addActionListener(newFolderAction);

        // ---
        // Rename
        ActionListener renameAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                int[] rows = {0};
                boolean tooMany = false;
                JTree tree = null;
                NavTreeUserObject tuo = null;
                Object object = guiContext.browser.lastComponent;
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
                    tree = guiContext.browser.navTransferHandler.getTargetTree((JTable) object);
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
                        JOptionPane.showMessageDialog(guiContext.mainFrame,
                                guiContext.cfg.gs("Navigator.menu.Rename.cannot.rename.current.location"),
                                guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String reply = name;
                    if (path.length() > 0)
                    {

                        Object obj = JOptionPane.showInputDialog(guiContext.mainFrame,
                                guiContext.cfg.gs("Navigator.menu.Rename") + name + " " +
                                        guiContext.cfg.gs("Navigator.menu.Rename.to"),
                                guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE,
                                null, null, reply);
                        reply = (String) obj;
                        if (reply != null && reply.length() > 0)
                        {
                            try
                            {
                                String to = Utils.getLeftPath(path, null);
                                to = to + Utils.getSeparatorFromPath(path) + reply;
                                guiContext.context.transfer.rename(path, to, tuo.isRemote);

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
                                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                                            guiContext.cfg.gs("Navigator.error.writing.hint") + "  " +
                                                    e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }

                                guiContext.browser.refreshByObject(tree);
                                if (object instanceof JTree)
                                    tuo.node.selectMe();
                                else
                                    ((JTable) object).setRowSelectionInterval(rows[0], rows[0]);
                            }
                            catch (Exception e)
                            {
                                logger.error(Utils.getStackTrace(e));
                                JOptionPane.showMessageDialog(guiContext.mainFrame,
                                        guiContext.cfg.gs("Navigator.menu.Rename.error.renaming") +
                                                (tuo.isRemote ? guiContext.cfg.gs("Z.remote.lowercase") +
                                                        " " : "") + name + ": " + e.getMessage(),
                                        guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.Rename.please.select.a.single.item.to.be.renamed"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.mainFrame.menuItemRename.addActionListener(renameAction);
        guiContext.mainFrame.popupMenuItemRename.addActionListener(renameAction);

        // ---
        // Touch Date/Time
        ActionListener touchAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    guiContext.browser.touchSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    guiContext.browser.touchSelected(sourceTable);
                }
            }
        };
        guiContext.mainFrame.menuItemTouch.addActionListener(touchAction);
        guiContext.mainFrame.popupMenuItemTouch.addActionListener(touchAction);

        // ---
        // Copy
        ActionListener copyAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "copy");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        };
        guiContext.mainFrame.menuItemCopy.addActionListener(copyAction);
        guiContext.mainFrame.popupMenuItemCopy.addActionListener(copyAction);

        // ---
        // Cut
        ActionListener cutAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "cut");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        };
        guiContext.mainFrame.menuItemCut.addActionListener(cutAction);
        guiContext.mainFrame.popupMenuItemCut.addActionListener(cutAction);

        // ---
        // Paste
        ActionListener pasteAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.browser.lastComponent != null)
                {
                    ActionEvent ev = new ActionEvent(guiContext.browser.lastComponent, ActionEvent.ACTION_PERFORMED, "paste");
                    guiContext.browser.lastComponent.requestFocus();
                    guiContext.browser.lastComponent.getActionMap().get(ev.getActionCommand()).actionPerformed(ev);
                }
            }
        };
        guiContext.mainFrame.menuItemPaste.addActionListener(pasteAction);
        guiContext.mainFrame.popupMenuItemPaste.addActionListener(pasteAction);

        // ---
        // Delete
        ActionListener deleteAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    guiContext.browser.deleteSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    guiContext.browser.deleteSelected(sourceTable);
                }
            }
        };
        guiContext.mainFrame.menuItemDelete.addActionListener(deleteAction);
        guiContext.mainFrame.popupMenuItemDelete.addActionListener(deleteAction);

        //
        // Settings
        guiContext.mainFrame.menuItemSettings.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogSettings == null || !dialogSettings.isShowing())
                {
                    dialogSettings = new Settings(guiContext.mainFrame, guiContext);
                    dialogSettings.setVisible(true);
                }
                else
                {
                    dialogSettings.toFront();
                    dialogSettings.requestFocus();
                }
            }
        });

        //
        // -- View Menu
        // ---
        // Refresh
        ActionListener refreshAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.browser.rescanByObject(guiContext.browser.lastComponent);
            }
        };
        guiContext.mainFrame.menuItemRefresh.addActionListener(refreshAction);
        guiContext.mainFrame.popupMenuItemRefresh.addActionListener(refreshAction);

        // ---
        // Progress
        guiContext.mainFrame.menuItemProgress.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.progress == null || !guiContext.progress.isBeingUsed())
                {
                    ActionListener cancel = new ActionListener()
                    {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent)
                        {
                            // noop
                        }
                    };
                    guiContext.progress = new Progress(guiContext, guiContext.mainFrame, cancel, false);
                }
                guiContext.progress.view();
            }
        });

        // ---
        // Auto-Refresh
        guiContext.mainFrame.menuItemAutoRefresh.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.preferences.setAutoRefresh(!guiContext.preferences.isAutoRefresh());
                if (guiContext.preferences.isAutoRefresh())
                    guiContext.mainFrame.menuItemAutoRefresh.setSelected(true);
                else
                    guiContext.mainFrame.menuItemAutoRefresh.setSelected(false);
            }
        });
        // set initial state of Auto-Refresh checkbox
        if (guiContext.preferences.isAutoRefresh())
            guiContext.mainFrame.menuItemAutoRefresh.setSelected(true);
        else
            guiContext.mainFrame.menuItemAutoRefresh.setSelected(false);

        // ---
        // Show Hidden
        guiContext.mainFrame.menuItemShowHidden.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.browser.toggleShowHiddenFiles();
            }
        });
        // set initial state of Show Hidden checkbox
        if (guiContext.preferences.isHideHiddenFiles())
            guiContext.mainFrame.menuItemShowHidden.setSelected(false);
        else
            guiContext.mainFrame.menuItemShowHidden.setSelected(true);

        // ---
        // Word Wrap Log
        ActionListener wordWrapAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                boolean selected = false;
                if (actionEvent.getSource() == guiContext.mainFrame.menuItemWordWrap)
                    selected = guiContext.mainFrame.menuItemWordWrap.isSelected();
                if (actionEvent.getSource() == guiContext.mainFrame.popupCheckBoxMenuItemWordWrap)
                    selected = guiContext.mainFrame.popupCheckBoxMenuItemWordWrap.isSelected();
                if (actionEvent.getSource() == guiContext.mainFrame.popupCheckBoxMenuItemOperationWordWrap)
                    selected = guiContext.mainFrame.popupCheckBoxMenuItemOperationWordWrap.isSelected();
                guiContext.mainFrame.menuItemWordWrap.setSelected(selected);
                guiContext.mainFrame.popupCheckBoxMenuItemWordWrap.setSelected(selected);
                guiContext.mainFrame.popupCheckBoxMenuItemOperationWordWrap.setSelected(selected);
                guiContext.mainFrame.textAreaLog.setLineWrap(guiContext.mainFrame.menuItemWordWrap.isSelected());
                guiContext.mainFrame.textAreaOperationLog.setLineWrap(guiContext.mainFrame.menuItemWordWrap.isSelected());
            }
        };
        // set initial state of Word Wrap Log
        guiContext.mainFrame.menuItemWordWrap.setSelected(true);
        guiContext.mainFrame.popupCheckBoxMenuItemWordWrap.setSelected(true);
        guiContext.mainFrame.popupCheckBoxMenuItemOperationWordWrap.setSelected(true);
        guiContext.mainFrame.menuItemWordWrap.addActionListener(wordWrapAction);
        guiContext.mainFrame.popupCheckBoxMenuItemWordWrap.addActionListener(wordWrapAction);
        guiContext.mainFrame.popupCheckBoxMenuItemOperationWordWrap.addActionListener(wordWrapAction);

        // -- Bookmarks Menu
        //
        // ---
        // Add Current Location
        guiContext.mainFrame.menuItemAddBookmark.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Object object = guiContext.browser.lastComponent;
                if (object instanceof JTree)
                {
                    JTree sourceTree = (JTree) object;
                    guiContext.browser.bookmarkSelected(sourceTree);
                }
                else if (object instanceof JTable)
                {
                    JTable sourceTable = (JTable) object;
                    guiContext.browser.bookmarkSelected(sourceTable);
                }
            }
        });

        // ---
        // Bookmarks Delete
        guiContext.mainFrame.menuItemBookmarksDelete.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String message = guiContext.cfg.gs("Browser.select.one.or.more.bookmarks.to.delete");
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

                int opt = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, guiContext.cfg.gs("Navigator.delete.bookmarks"), JOptionPane.OK_CANCEL_OPTION);
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
                            JOptionPane.showMessageDialog(guiContext.mainFrame,
                                    guiContext.cfg.gs("Browser.error.saving.bookmarks") + e.getMessage(),
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        //
        // -- Tools Menu
        //
        // ---
        // Duplicate Finder
        guiContext.mainFrame.menuItemDuplicates.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogDuplicateFinder == null || !dialogDuplicateFinder.isShowing())
                {
                    dialogDuplicateFinder = new DuplicateFinderUI(guiContext.mainFrame, guiContext);
                    dialogDuplicateFinder.setVisible(true);
                }
                else
                {
                    dialogDuplicateFinder.toFront();
                    dialogDuplicateFinder.requestFocus();
                }
            }
        });

        // ---
        // Empty Directory Finder
        guiContext.mainFrame.menuItemEmptyFinder.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogEmptyDirectoryFinder == null || !dialogEmptyDirectoryFinder.isShowing())
                {
                    dialogEmptyDirectoryFinder = new EmptyDirectoryFinderUI(guiContext.mainFrame, guiContext);
                    dialogEmptyDirectoryFinder.setVisible(true);
                }
                else
                {
                    dialogEmptyDirectoryFinder.toFront();
                    dialogEmptyDirectoryFinder.requestFocus();
                }
            }
        });

        // ---
        // Junk Remover Tool
        guiContext.mainFrame.menuItemJunk.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogJunkRemover == null || !dialogJunkRemover.isShowing())
                {
                    dialogJunkRemover = new JunkRemoverUI(guiContext.mainFrame, guiContext);
                    dialogJunkRemover.setVisible(true);
                }
                else
                {
                    dialogJunkRemover.toFront();
                    dialogJunkRemover.requestFocus();
                }
            }
        });

        // ---
        // Renamer Tool
        guiContext.mainFrame.menuItemRenamer.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogRenamer == null || !dialogRenamer.isShowing())
                {
                    dialogRenamer = new RenamerUI(guiContext.mainFrame, guiContext);
                    dialogRenamer.setVisible(true);
                }
                else
                {
                    dialogJobs.toFront();
                    dialogRenamer.requestFocus();
                }
            }
        });

        //
        // -- Jobs Menu
        guiContext.mainFrame.menuItemJobsManage.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (dialogJobs == null || !dialogJobs.isShowing())
                {
                    dialogJobs = new JobsUI(guiContext.mainFrame, guiContext);
                    dialogJobs.setVisible(true);
                }
                else
                {
                    dialogJobs.toFront();
                    dialogJobs.requestFocus();
                }
            }
        });

        //
        // -- Window Menu
        // ---
        // Maximize
        guiContext.mainFrame.menuItemMaximize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.setExtendedState(guiContext.mainFrame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });

        // ---
        // Minimize
        guiContext.mainFrame.menuItemMinimize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.setState(JFrame.ICONIFIED);
            }
        });

        // ---
        // Restore
        guiContext.mainFrame.menuItemRestore.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.setExtendedState(JFrame.NORMAL);
            }
        });

        // ---
        // Split Horizontal
        guiContext.mainFrame.menuItemSplitHorizontal.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.tabbedPaneBrowserOne.setVisible(true);
                guiContext.mainFrame.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.mainFrame.splitPaneTwoBrowsers.getHeight();
                guiContext.mainFrame.splitPaneTwoBrowsers.setOrientation(JSplitPane.VERTICAL_SPLIT);
                guiContext.mainFrame.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // ---
        // Split Vertical
        guiContext.mainFrame.menuItemSplitVertical.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.tabbedPaneBrowserOne.setVisible(true);
                guiContext.mainFrame.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.mainFrame.splitPaneTwoBrowsers.getWidth();
                guiContext.mainFrame.splitPaneTwoBrowsers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                guiContext.mainFrame.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // -- Help Menu
        // ---
        // Controls
        guiContext.mainFrame.menuItemControls.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavHelp dialog = new NavHelp(guiContext.mainFrame, guiContext.mainFrame, guiContext, guiContext.cfg.gs("Settings.date.format.help.title"), "controls_" + guiContext.preferences.getLocale() + ".html");
                dialog.setTitle(guiContext.cfg.gs("Navigator.controls.help.title"));
                dialog.setVisible(true);
            }
        });

        // ---
        // Documentation
        guiContext.mainFrame.menuItemDocumentation.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS/wiki");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.error.launching.browser"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // ---
        // GitHub Project
        guiContext.mainFrame.menuItemGitHubProject.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    URI uri = new URI("https://github.com/GrokSoft/ELS");
                    Desktop.getDesktop().browse(uri);
                }
                catch (Exception e)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Navigator.error.launching.browser"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // ---
        // About
        guiContext.mainFrame.menuItemAbout.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                About about = new About(guiContext.mainFrame, guiContext);
                about.setVisible(true);
            }
        });

        // popup menu log
        // ---
        // Bottom
        guiContext.mainFrame.popupMenuItemBottom.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = guiContext.mainFrame.scrollPaneLog.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
        // ---
        // Clear
        guiContext.mainFrame.popupMenuItemClear.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.textAreaLog.setText("");
            }
        });
        // ---
        // Top
        guiContext.mainFrame.popupMenuItemTop.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = guiContext.mainFrame.scrollPaneLog.getVerticalScrollBar();
                vertical.setValue(0);
            }
        });

        // popup menu operation log
        // ---
        // Bottom
        guiContext.mainFrame.popupMenuItemOperationBottom.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = guiContext.mainFrame.scrollPaneOperationLog.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
        // ---
        // Clear
        guiContext.mainFrame.popupMenuItemOperationClear.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.mainFrame.textAreaOperationLog.setText("");
            }
        });
        // ---
        // Top
        guiContext.mainFrame.popupMenuItemOperationTop.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = guiContext.mainFrame.scrollPaneOperationLog.getVerticalScrollBar();
                vertical.setValue(0);
            }
        });
    }

    public void loadBookmarksMenu()
    {
        JMenu menu = guiContext.mainFrame.menuBookmarks;
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
                        int index = findMenuItemIndex(guiContext.mainFrame.menuBookmarks, selected) - 3;
                        if (index >= 0 && index < bookmarks.size())
                        {
                            Bookmark bm = bookmarks.get(index);
                            if (bm != null)
                                guiContext.browser.bookmarkGoto(bm);
                        }
                    }
                });
                menu.add(item);
            }
        }
    }

    public void loadJobsMenu()
    {
        JMenu menu = guiContext.mainFrame.menuJobs;
        int count = menu.getItemCount();

        // A -2 offset for "Manage ..." and separator
        if (count > 3)
        {
            for (int i = count - 1; i > 1; --i)
            {
                menu.remove(i);
            }
        }

        Job tmpJob = new Job(guiContext.cfg, guiContext.context, "temp");
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
                        return new Job(guiContext.cfg, guiContext.context, "");
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
                            String msg = guiContext.cfg.gs("Z.exception") + entry.getName() + " " + Utils.getStackTrace(e);
                            logger.error(msg);
                            JOptionPane.showMessageDialog(guiContext.mainFrame, msg,
                                    guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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

                            // A -2 offset for "Manage ..." and separator
                            int index = findMenuItemIndex(guiContext.mainFrame.menuJobs, selected) - 2;
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
        String status = job.validate(guiContext.cfg);
        if (status.length() == 0)
        {
            // make dialog pieces
            String message = java.text.MessageFormat.format(guiContext.cfg.gs("JobsUI.run.as.defined"), job.getConfigName());
            JCheckBox checkbox = new JCheckBox(guiContext.cfg.gs("Navigator.dryrun"));
            checkbox.setToolTipText(guiContext.cfg.gs("Navigator.dryrun.tooltip"));
            checkbox.setSelected(true);
            Object[] params = {message, checkbox};

            // confirm run of job
            int reply = JOptionPane.showConfirmDialog(guiContext.mainFrame, params, guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
            boolean isDryRun = checkbox.isSelected();
            if (reply == JOptionPane.YES_OPTION)
            {
                ArrayList<Task> tasks = job.getTasks();
                if (tasks.size() > 0)
                {
                    worker = job.process(guiContext, guiContext.mainFrame, guiContext.cfg.getNavigatorName(), job, isDryRun);
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
                                    {
                                        processTerminated(job);
                                    }
                                }
                            }
                        });
                        worker.execute();
                    }
                    else
                        processTerminated(job);
                }
            }
        }
        else
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame, status,
                    guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void processTerminated(Job job)
    {
        try
        {
            if (guiContext.progress != null)
                guiContext.progress.done();

            reconnectRemote(guiContext.cfg, guiContext.context, guiContext.context.publisherRepo, guiContext.context.subscriberRepo);
            remoteJobRunning = false;
            guiContext.browser.refreshTree(guiContext.mainFrame.treeCollectionTwo);
            guiContext.browser.refreshTree(guiContext.mainFrame.treeSystemTwo);
        }
        catch (Exception e)
        {
        }
        if (job.isRequestStop())
        {
            logger.info(job.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
            guiContext.mainFrame.labelStatusMiddle.setText(job.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
        }
        else
        {
            logger.info(job.getConfigName() + guiContext.cfg.gs("Z.completed"));
            guiContext.mainFrame.labelStatusMiddle.setText(job.getConfigName() + guiContext.cfg.gs("Z.completed"));
        }
        disableGui(false);
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

    public void readPreferences()
    {
        try
        {
            Gson gson = new Gson();
            String json = new String(Files.readAllBytes(Paths.get(guiContext.preferences.getFullPath())));
            Preferences prefs = gson.fromJson(json, guiContext.preferences.getClass());
            if (prefs != null)
            {
                guiContext.preferences = gson.fromJson(json, guiContext.preferences.getClass());
                guiContext.preferences.setCfg(guiContext.cfg);
            }
        }
        catch (IOException e)
        {
            // file might not exist
        }
    }

    private boolean reconnectRemote(Configuration config, Context context, Repository publisherRepo, Repository subscriberRepo) throws Exception
    {
        // is this necessary?
        if (config.isRemoteSession() && subscriberRepo != null &&
                context.clientStty != null && context.clientStty.getTheirKey().equals(subscriberRepo.getLibraryData().libraries.key) &&
                context.clientStty.isConnected())
            return true;

        // close any existing connections
        if (context.clientStty != null && context.clientStty.isConnected())
        {
            try
            {
                context.clientStty.send("bye", "");
                context.clientSftp.stopClient();
                Thread.sleep(500);
            }
            catch (Exception e)
            {
            }
        }

        // connect to the hint status server if defined
        context.main.connectHintServer(context.publisherRepo);

        if (config.isRemoteSession())
        {
            // start the serveStty client for automation
            context.clientStty = new ClientStty(guiContext.cfg, guiContext.context, false, true);
            if (!context.clientStty.connect(publisherRepo, subscriberRepo))
            {
                config.setRemoteType("-");
                if (guiContext != null)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.remote.subscriber.failed.to.connect"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
                return false;
            }

            // check for opening commands from Subscriber
            // *** might change cfg options for subscriber and targets that are handled below ***
            if (context.clientStty.checkBannerCommands())
            {
                logger.info(config.gs("Transfer.received.subscriber.commands") + (config.isRequestCollection() ? "RequestCollection " : "") + (config.isRequestTargets() ? "RequestTargets" : ""));
            }

            // start the serveSftp client
            context.clientSftp = new ClientSftp(guiContext.cfg, publisherRepo, subscriberRepo, true);
            if (!context.clientSftp.startClient())
            {
                guiContext.cfg.setRemoteType("-");
                if (guiContext != null)
                {
                    JOptionPane.showMessageDialog(guiContext.mainFrame,
                            guiContext.cfg.gs("Navigator.menu.Open.subscriber.subscriber.sftp.failed.to.connect"),
                            guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                guiContext.preferences = new Preferences(guiContext.cfg, guiContext.context);
                readPreferences();

                // TODO Add as needed: Set command line overrides on Navigator Preferences

                // preserve file times
                String o = guiContext.cfg.getOriginalCommandline();
                if (guiContext.cfg.getOriginalCommandline().contains("-y"))
                    guiContext.preferences.setPreserveFileTimes(guiContext.cfg.isPreserveDates());
                else
                    guiContext.preferences.setPreserveFileTimes(guiContext.preferences.isPreserveFileTimes());

                // binary or decimal scale
                if (guiContext.cfg.getOriginalCommandline().contains("-z"))
                    guiContext.cfg.setLongScale(guiContext.cfg.isBinaryScale());
                else
                    guiContext.cfg.setLongScale(guiContext.preferences.isBinaryScale());

                // execute the Navigator GUI
                if (initialize())
                {
                    logger.trace(guiContext.cfg.gs("Navigator.initialized"));
                    guiContext.preferences.fixApplication(guiContext);

                    for (ActionListener listener : guiContext.mainFrame.buttonHintTracking.getActionListeners())
                    {
                        listener.actionPerformed(new ActionEvent(guiContext.mainFrame.buttonHintTracking, ActionEvent.ACTION_PERFORMED, null));
                    }

                    String os = Utils.getOS();
                    logger.debug(guiContext.cfg.gs("Navigator.detected.local.system.as") + os);
                    guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("Navigator.detected.local.system.as") + os);

                    logger.trace(guiContext.cfg.gs("Navigator.displaying"));
                    guiContext.mainFrame.setVisible(true);

                    guiContext.preferences.fixBrowserDivider(guiContext, -1);
                    guiContext.mainFrame.treeCollectionOne.requestFocus();
                }
                else
                {
                    guiContext.mainFrame = null; // failed
                    guiContext.context.fault = true;
                    stop();
                }
            }
        });

        return 0;
    }

    public void setComponentEnabled(boolean enabled, Component component)
    {
        component.setEnabled(enabled);
        if (component instanceof Container)
        {
            Component[] components = ((Container) component).getComponents();
            if (components != null && components.length > 0)
            {
                for (Component comp : components)
                {
                    setComponentEnabled(enabled, comp);
                }
            }
        }
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

    public void stop()
    {
        if (guiContext.context.clientStty != null)
        {
            try
            {
                guiContext.context.clientSftp.stopClient();
                if (guiContext.context.clientStty.isConnected() && !guiContext.context.timeout)
                {
                    if (guiContext.context.fault)
                    {
                        String resp;
                        try
                        {
                            resp = guiContext.context.clientStty.roundTrip("fault", "Sending fault to remote", 1000);
                        }
                        catch (Exception e)
                        {
                            resp = null;
                        }
                    }
                    else if (quitRemote)
                        guiContext.context.clientStty.send("quit", "Sending quit command to remote");
                    else
                        guiContext.context.clientStty.send("bye", "Sending bye command to remote");
                }
            }
            catch (Exception e)
            {
                guiContext.context.fault = true;
                logger.error(Utils.getStackTrace(e));
            }
        }

        if (guiContext.mainFrame != null)
        {
            try // save the settings
            {
                guiContext.preferences.write(guiContext);
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                JOptionPane.showMessageDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Z.exception") + e.getMessage(),
                        guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            }
            guiContext.mainFrame.setVisible(false);
            guiContext.mainFrame.dispose();
        }

        guiContext.context.main.stopVerbiage();

        // end the Navigator Swing thread
        System.exit(guiContext.context.fault ? 1 : 0);
    }

}
