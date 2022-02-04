package com.groksoft.els.gui;

import com.google.gson.Gson;
import com.groksoft.els.*;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.Repository;
import com.groksoft.els.sftp.ClientSftp;
import com.groksoft.els.stty.ClientStty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.sshd.common.util.io.IoUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;
import java.util.ResourceBundle;

public class Navigator
{
    public static GuiContext guiContext;
    public boolean showHintTrackingButton = false;
    ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
    private transient Logger logger = LogManager.getLogger("applog");
    private boolean quitRemote = false;

    // QUESTION:
    //  1. How to organize editing JSON server and targets files with N-libraries with N-sources each?
    //      a. A tree control of JSON nodes and values with add/delete?

    // TODO:
    //  ! TEST Hints with spread-out files, e.g. TV Show in two locations.
    //  * Display Collection:
    //     * Whole tree - done
    //     * !-Z alphabetic
    //  * Overwrite true/false option?
    //
    // TODO:
    //  * Try using skeleton JSON file with forced pull of collection from subscriber
    //  * Remove -n | --rename options and JSON objects; Update documentation
    //
    // TODO:
    //  * Progress
    //
    // QUESTION:
    //  * Can a Library be added for updating JSON files?
    //     * Or should skeleton files be used with pull options always enabled?
    //     * Or both?

    public Navigator(Main main, Configuration config, Context ctx)
    {
        guiContext = new GuiContext();
        guiContext.cfg = config;
        guiContext.context = ctx;
        guiContext.navigator = this;
        guiContext.preferences = new Preferences();
        guiContext.cfg.setLongScale(guiContext.preferences.isBinaryScale());
    }

    /**
     * Initialize everything for the GUI
     *
     * @return true if successful, false if a fault occurred
     */
    private boolean initialize()
    {
        readPreferences();

        if (guiContext.cfg.getPublisherCollectionFilename().length() > 0)
        {
            guiContext.preferences.setLastIsWorkstation(true);
            guiContext.preferences.setLastPublisherOpenFile(guiContext.cfg.getPublisherCollectionFilename());
            guiContext.preferences.setLastPublisherOpenPath(Utils.getLeftPath(guiContext.cfg.getPublisherCollectionFilename(),
                    Utils.getSeparatorFromPath(guiContext.cfg.getPublisherCollectionFilename())));
        }
        else if (guiContext.cfg.getPublisherLibrariesFileName().length() > 0)
        {
            guiContext.preferences.setLastIsWorkstation(false);
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
        guiContext.context.transfer = new Transfer(guiContext.cfg, guiContext.context);
        try
        {
            guiContext.context.transfer.initialize();

            if (guiContext.cfg.getHintKeysFile() != null && guiContext.cfg.getHintKeysFile().length() > 0)
            {
                // Get ELS hints keys
                guiContext.context.hintKeys = new HintKeys(guiContext.cfg, guiContext.context);
                guiContext.context.hintKeys.read(guiContext.cfg.getHintKeysFile());
                showHintTrackingButton = true;
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            guiContext.context.fault = true;
            return false;
        }

        // setup the GUI
        guiContext.form = new MainFrame(guiContext);
        if (!guiContext.context.fault)
        {
            // setup the Main Menu and primary tabs
            initializeMainMenu();
            guiContext.browser = new Browser(guiContext);

            // TODO Add Backup, and other tab content creation here


            // disable back-fill
            guiContext.cfg.setNoBackFill(true);

            guiContext.cfg.setPreserveDates(guiContext.preferences.isPreserveFileTimes());

/*
        Thread.setDefaultUncaughtExceptionHandler( (thread, throwable) -> {
            logger.error("GOT IT: " + Utils.getStackTrace(throwable));
        });
*/

/*
        PrintStream originalOut = System.out;
        System.setErr(new PrintStream(new OutputStream()
        {
            @Override
            public void write(int i) throws IOException
            {

            }
        }));
*/
        }

        return !guiContext.context.fault;
    }

    private void initializeMainMenu()
    {
        // --- Main Menu ------------------------------------------
        //
        // -- File Menu
        //-
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
                        return "ELS Library files (*.json)";
                    }
                });
                fc.setDialogTitle("Open ELS Publisher Library");
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
                jp.setBorder(guiContext.form.textFieldLocation.getBorder());

                JRadioButton rbCollection = new JRadioButton("Collection");
                rbCollection.setToolTipText("Running on a media collection (server/back-up)");
                rbCollection.setSelected(!guiContext.preferences.isLastIsWorkstation());
                JRadioButton rbWorkstation = new JRadioButton("Workstation");
                rbWorkstation.setToolTipText("Running on a media workstation");
                rbWorkstation.setSelected(guiContext.preferences.isLastIsWorkstation());
                ButtonGroup group = new ButtonGroup();
                group.add(rbCollection);
                group.add(rbWorkstation);
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 4, 0, 2);
                layout.setConstraints(rbCollection, gbc);
                gbc.insets = new Insets(0, 2, 0, 4);
                layout.setConstraints(rbWorkstation, gbc);
                jp.add(rbCollection);
                jp.add(rbWorkstation);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(guiContext.form);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        guiContext.preferences.setLastIsWorkstation(rbWorkstation.isSelected());
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastPublisherOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "File not found: " + file.getName(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Select a file only", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            guiContext.preferences.setLastPublisherOpenFile(file.getAbsolutePath());
                            guiContext.cfg.setPublisherLibrariesFileName(file.getAbsolutePath());
                            guiContext.context.publisherRepo = guiContext.context.main.readRepo(guiContext.cfg, Repository.PUBLISHER, Repository.VALIDATE);
                            guiContext.browser.loadCollectionTree(guiContext.form.treeCollectionOne, guiContext.context.publisherRepo, false);
                            guiContext.browser.loadSystemTree(guiContext.form.treeSystemOne, false);
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Error opening publisher library:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.form.menuItemOpenPublisher.addActionListener(openPublisherAction);

        //-
        // Open Subscriber
        AbstractAction openSubscriberAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (guiContext.context.publisherRepo == null)
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Please open a Publisher Library first", guiContext.cfg.getNavigatorName(), JOptionPane.INFORMATION_MESSAGE);
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
                        return "ELS Library files (*.json)";
                    }
                });
                fc.setDialogTitle("Open ELS Subscriber Library");
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
                jp.setBorder(guiContext.form.textFieldLocation.getBorder());
                JCheckBox cbIsRemote = new JCheckBox("<html><head><style>body{margin-left:4px;}</style></head><body>&nbsp;&nbsp;Remote<br/>Connection&nbsp;&nbsp;</body></html>");
                cbIsRemote.setHorizontalTextPosition(SwingConstants.LEFT);
                cbIsRemote.setToolTipText("Be sure an ELS Subscriber Listener (--remote S) is running on the remote system");
                cbIsRemote.setSelected(guiContext.preferences.isLastIsRemote());
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(0, 0, 0, 4);
                gb.setConstraints(cbIsRemote, gbc);
                jp.add(cbIsRemote);
                fc.setAccessory(jp);

                while (true)
                {
                    int selection = fc.showOpenDialog(guiContext.form);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        if (guiContext.cfg.isRemoteSession())
                        {
                            if (guiContext.preferences.isShowConfirmations())
                            {
                                int r = JOptionPane.showConfirmDialog(guiContext.form, "Close current remote connection?", guiContext.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                                if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                                    return;
                            }
                            try
                            {
                                guiContext.context.clientStty.send("bye");
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
                            JOptionPane.showMessageDialog(guiContext.form, "File not found: " + file.getName(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Select a file only", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        try
                        {
                            // this defines the value returned by guiContext.cfg.isRemoteSession()
                            if (guiContext.preferences.isLastIsRemote())
                            {
                                guiContext.cfg.setRemoteType("P"); // publisher to remote subscriber
                                guiContext.form.menuItemQuitTerminate.setVisible(true);
                            }
                            else
                            {
                                guiContext.cfg.setRemoteType("-"); // not remote
                                guiContext.form.menuItemQuitTerminate.setVisible(false);
                            }

                            guiContext.preferences.setLastSubscriberOpenFile(file.getAbsolutePath());
                            guiContext.cfg.setSubscriberLibrariesFileName(file.getAbsolutePath());
                            guiContext.context.subscriberRepo = guiContext.context.main.readRepo(guiContext.cfg, Repository.SUBSCRIBER, !guiContext.preferences.isLastIsRemote());

                            if (guiContext.preferences.isLastIsRemote())
                            {
                                // connect to the hint status server if defined
                                guiContext.context.main.connectHintServer(guiContext.context.publisherRepo);

                                // start the serveStty client for automation
                                guiContext.context.clientStty = new ClientStty(guiContext.cfg, false, true);
                                if (!guiContext.context.clientStty.connect(guiContext.context.publisherRepo, guiContext.context.subscriberRepo))
                                {
                                    JOptionPane.showMessageDialog(guiContext.form, "Remote subscriber failed to connect", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }

                                // start the serveSftp client
                                guiContext.context.clientSftp = new ClientSftp(guiContext.cfg, guiContext.context.publisherRepo, guiContext.context.subscriberRepo, true);
                                if (!guiContext.context.clientSftp.startClient())
                                {
                                    JOptionPane.showMessageDialog(guiContext.form, "Subscriber sftp failed to connect", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                    guiContext.cfg.setRemoteType("-");
                                    return;
                                }
                            }

                            // load the subscriber library
                            guiContext.browser.loadCollectionTree(guiContext.form.treeCollectionTwo, guiContext.context.subscriberRepo, guiContext.preferences.isLastIsRemote());
                            guiContext.browser.loadSystemTree(guiContext.form.treeSystemTwo, guiContext.preferences.isLastIsRemote());
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Error opening subscriber library: " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.form.menuItemOpenSubscriber.addActionListener(openSubscriberAction);
        if (guiContext.context.subscriberRepo != null)
            guiContext.preferences.setLastIsRemote(guiContext.cfg.isRemoteSession());

        //-
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
                        return "ELS Hint Keys (*.keys)";
                    }
                });
                fc.setDialogTitle("Open ELS Hint Keys");
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
                    int selection = fc.showOpenDialog(guiContext.form);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        File last = fc.getCurrentDirectory();
                        guiContext.preferences.setLastHintKeysOpenPath(last.getAbsolutePath());
                        File file = fc.getSelectedFile();
                        if (!file.exists())
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "File not found: " + file.getName(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Select a file only", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                                guiContext.form.buttonHintTracking.setVisible(true);
//                                for (ActionListener listener : guiContext.form.buttonHintTracking.getActionListeners())
//                                {
//                                    listener.actionPerformed(new ActionEvent(guiContext.form.buttonHintTracking, ActionEvent.ACTION_PERFORMED, null));
//                                }
                            }
                        }
                        catch (Exception e)
                        {
                            JOptionPane.showMessageDialog(guiContext.form, "Error opening hint keys:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                    }
                    break;
                }
            }
        };
        guiContext.form.menuItemOpenHintKeys.addActionListener(openHintKeysAction);

        // Save Layout
        AbstractAction saveLayoutAction = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                try
                {
                    guiContext.preferences.export(guiContext);
                }
                catch (Exception e)
                {
                    guiContext.browser.printLog(Utils.getStackTrace(e), true);
                    JOptionPane.showMessageDialog(guiContext.form, "Error saving layout " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.form.menuItemSaveLayout.addActionListener(saveLayoutAction);

        //-
        // Quit & Exit Remote
        guiContext.form.menuItemQuitTerminate.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                quitRemote = true;
                stop();
            }
        });
        if (!guiContext.cfg.isRemoteSession())
            guiContext.form.menuItemQuitTerminate.setVisible(false);

        //
        // -- Edit Menu
        //-
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
                    String path = "";
                    if (tuo.type == NavTreeUserObject.REAL)
                        path = tuo.path;
                    else if (tuo.type == NavTreeUserObject.LIBRARY)
                    {
                        if (tuo.sources.length == 1)
                            path = tuo.sources[0];
                        else
                        {
                            int opt = JOptionPane.showOptionDialog(guiContext.form, "Select one of the " + tuo.sources.length + " locations defined for library " + tuo.name + ":",
                                    guiContext.cfg.getNavigatorName(), JOptionPane.DEFAULT_OPTION,
                                    JOptionPane.QUESTION_MESSAGE, null, tuo.sources, tuo.sources[0]);
                            if (opt > -1)
                            {
                                path = tuo.sources[opt];
                            }
                        }
                    }
                    else
                    {
                        JOptionPane.showMessageDialog(guiContext.form, "Cannot create new folder in current location", guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    boolean error = false;
                    String reply = "";
                    if (path.length() > 0)
                    {
                        reply = JOptionPane.showInputDialog(guiContext.form, "New folder for " + path + ": ", guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE);
                        if (reply != null && reply.length() > 0)
                        {
                            NavTreeUserObject createdTuo = null;
                            try
                            {
                                path = path + Utils.getSeparatorFromPath(path) + reply;
                                String msg = "Creating " + (tuo.isRemote ? "remote " : "") + "directory " + path;
                                guiContext.browser.printLog(msg);

                                if (guiContext.context.transfer.makeDirs((tuo.isRemote ? path + Utils.getSeparatorFromPath(path) + "dummyfile.els" : path), true, tuo.isRemote))
                                {
                                    // make tuo and add node
                                    NavTreeNode createdNode = new NavTreeNode(guiContext, tree);
                                    if (tuo.isRemote)
                                    {
                                        createdTuo = new NavTreeUserObject(createdNode, Utils.getRightPath(path, null),
                                                path, 0, LocalTime.now().toSecondOfDay(), true);
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
                                    guiContext.browser.printLog("Directory not created, check permissions.", true);
                                    JOptionPane.showMessageDialog(guiContext.form, "Directory not created, check permissions.", guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                                }
                            }
                            catch (Exception e)
                            {
                                guiContext.browser.printLog(Utils.getStackTrace(e), true);
                                JOptionPane.showMessageDialog(guiContext.form, "Error creating " + (tuo.isRemote ? "remote " : "") + "directory: " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(guiContext.form, "Please select a single destination for a new folder", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.form.menuItemNewFolder.addActionListener(newFolderAction);
        guiContext.form.popupMenuItemNewFolder.addActionListener(newFolderAction);

        //-
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
                        JOptionPane.showMessageDialog(guiContext.form, "Cannot rename current location", guiContext.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String reply = name;
                    if (path.length() > 0)
                    {

                        Object obj = JOptionPane.showInputDialog(guiContext.form, "Rename " + name + " to: ", guiContext.cfg.getNavigatorName(), JOptionPane.QUESTION_MESSAGE,
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
                                    tuo.file = new File(path);
                                }

                                try
                                {
                                    ((NavTransferHandler) tree.getTransferHandler()).exportHint("mv", orig, tuo);
                                }
                                catch (Exception e)
                                {
                                    logger.error(Utils.getStackTrace(e));
                                    JOptionPane.showMessageDialog(guiContext.form, "Error writing Hint:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                                }

                                guiContext.browser.refreshByObject(tree);
                                if (object instanceof JTree)
                                    tuo.node.selectMe();
                                else
                                    ((JTable) object).setRowSelectionInterval(rows[0], rows[0]);
                            }
                            catch (Exception e)
                            {
                                guiContext.browser.printLog(Utils.getStackTrace(e), true);
                                JOptionPane.showMessageDialog(guiContext.form, "Error renaming " + (tuo.isRemote ? "remote " : "") + name + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(guiContext.form, "Please select a single item to be renamed", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        guiContext.form.menuItemRename.addActionListener(renameAction);
        guiContext.form.popupMenuItemRename.addActionListener(renameAction);

        //-
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
        guiContext.form.menuItemTouch.addActionListener(touchAction);
        guiContext.form.popupMenuItemTouch.addActionListener(touchAction);

        //-
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
        guiContext.form.menuItemCopy.addActionListener(copyAction);
        guiContext.form.popupMenuItemCopy.addActionListener(copyAction);

        //-
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
        guiContext.form.menuItemCut.addActionListener(cutAction);
        guiContext.form.popupMenuItemCut.addActionListener(cutAction);

        //-
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
        guiContext.form.menuItemPaste.addActionListener(pasteAction);
        guiContext.form.popupMenuItemPaste.addActionListener(pasteAction);

        //-
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
        guiContext.form.menuItemDelete.addActionListener(deleteAction);
        guiContext.form.popupMenuItemDelete.addActionListener(deleteAction);

        //
        // Settings
        guiContext.form.menuItemSettings.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                Settings dialog = new Settings(guiContext.form, guiContext);
                dialog.setVisible(true);
            }
        });

        //
        // -- View Menu
        //-
        // Refresh
        guiContext.form.menuItemRefresh.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.browser.rescanByObject(guiContext.browser.lastComponent);
            }
        });

        //-
        // Show Hidden
        guiContext.form.menuItemShowHidden.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.preferences.setHideHiddenFiles(!guiContext.preferences.isHideHiddenFiles());
                if (guiContext.preferences.isHideHiddenFiles())
                    guiContext.form.menuItemShowHidden.setSelected(false);
                else
                    guiContext.form.menuItemShowHidden.setSelected(true);

                guiContext.browser.refreshTree(guiContext.form.treeCollectionOne);
                guiContext.browser.refreshTree(guiContext.form.treeSystemOne);
                guiContext.browser.refreshTree(guiContext.form.treeCollectionTwo);
                guiContext.browser.refreshTree(guiContext.form.treeSystemTwo);
            }
        });
        // set initial state of checkbox
        if (guiContext.preferences.isHideHiddenFiles())
            guiContext.form.menuItemShowHidden.setSelected(false);
        else
            guiContext.form.menuItemShowHidden.setSelected(true);

        //
        // -- Bookmarks Menu

        //
        // -- Tools Menu

        //
        // -- Window Menu
        //-
        // Maximize
        guiContext.form.menuItemMaximize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(guiContext.form.getExtendedState() | JFrame.MAXIMIZED_BOTH);
            }
        });

        //-
        // Minimize
        guiContext.form.menuItemMinimize.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setState(JFrame.ICONIFIED);
            }
        });

        //-
        // Restore
        guiContext.form.menuItemRestore.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.setExtendedState(JFrame.NORMAL);
            }
        });

        //-
        // Split Horizontal
        guiContext.form.menuItemSplitHorizontal.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getHeight();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.VERTICAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        //-
        // Split Vertical
        guiContext.form.menuItemSplitVertical.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.tabbedPaneBrowserOne.setVisible(true);
                guiContext.form.tabbedPaneBrowserTwo.setVisible(true);
                int size = guiContext.form.splitPaneTwoBrowsers.getWidth();
                guiContext.form.splitPaneTwoBrowsers.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                guiContext.form.splitPaneTwoBrowsers.setDividerLocation(size / 2);
            }
        });

        // -- Help Menu
        //-
        // Controls
        guiContext.form.menuItemControls.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                NavControls dialog = new NavControls(guiContext.form);
                String text = "";
                try
                {
                    String localized = "controls_" + guiContext.preferences.getLocale() + ".html";
                    URL url = Thread.currentThread().getContextClassLoader().getResource(localized);
                    List<String> lines = IoUtils.readAllLines(url);
                    for (int i = 0; i < lines.size(); ++i)
                    {
                        text += lines.get(i) + "\n";
                    }
                    dialog.controlsHelpText.setText(text);
                    dialog.setVisible(true);
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                }

            }
        });

        //-
        // Documentation
        guiContext.form.menuItemDocumentation.addActionListener(new AbstractAction()
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
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        //-
        // GitHub Project
        guiContext.form.menuItemGitHubProject.addActionListener(new AbstractAction()
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
                    JOptionPane.showMessageDialog(guiContext.form, "Error launching browser", guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // popup menu log
        //-
        // Bottom
        guiContext.form.popupMenuItemBottom.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JScrollBar vertical = guiContext.form.scrollPaneLog.getVerticalScrollBar();
                vertical.setValue(vertical.getMaximum());
            }
        });
        //-
        // Clear
        guiContext.form.popupMenuItemClear.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                guiContext.form.textAreaLog.setText("");
            }
        });

    }

    public void readPreferences()
    {
        try
        {
            String json;
            Gson gson = new Gson();
            json = new String(Files.readAllBytes(Paths.get(guiContext.preferences.getFilename())));
            Preferences prefs = gson.fromJson(json, guiContext.preferences.getClass());
            if (prefs != null)
            {
                guiContext.preferences = gson.fromJson(json, guiContext.preferences.getClass());
            }
        }
        catch (IOException e)
        {
            // file might not exist
        }
    }

    public int run() throws Exception
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                logger.info("Initializing Navigator");
                if (initialize())
                {
                    guiContext.preferences.fixApplication(guiContext);

                    //if (showHintTrackingButton)
                    {
                        for (ActionListener listener : guiContext.form.buttonHintTracking.getActionListeners())
                        {
                            listener.actionPerformed(new ActionEvent(guiContext.form.buttonHintTracking, ActionEvent.ACTION_PERFORMED, null));
                        }
                    }

                    String os = Utils.getOS();
                    logger.debug("Detected local system as " + os);
                    guiContext.form.labelStatusMiddle.setText("Detected local system as " + os);

                    logger.info("Displaying Navigator");
                    guiContext.form.setVisible(true);
                }
                else
                {
                    stop();
                    guiContext.form = null; // failed
                }
            }
        });
        return 0;
    }

    public void stop()
    {
        // tell remote end to exit
        if (guiContext.context.clientStty != null)
        {
            try
            {
                guiContext.context.clientSftp.stopClient();
                if (quitRemote)
                {
                    guiContext.context.clientStty.send("quit");
                }
                else
                {
                    guiContext.context.clientStty.send("bye");
                }
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
        }

        // report stats and shutdown
        if (guiContext.form != null)
        {
            try // save the settings
            {
                guiContext.preferences.export(guiContext);
            }
            catch (Exception e)
            {
            }
            guiContext.form.setVisible(false);
            guiContext.form.dispose();
        }
        Main.stopVerbiage();

        // stop the program if something blew-up
        if (guiContext.context.fault)
        {
            System.exit(1);
        }
    }

}
