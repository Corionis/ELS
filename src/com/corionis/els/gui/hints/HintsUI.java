package com.corionis.els.gui.hints;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.browser.BrowserTableCellRenderer;
import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintStatus;
import com.corionis.els.hints.Hints;
import com.corionis.els.repository.Repositories;
import com.corionis.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.border.*;

public class HintsUI extends JDialog
{
    private Context context;
    private boolean changesMade = false;
    private NavHelp helpDialog;
    ArrayList<Hint> hints = null;
    private String hintPublisherName = "-";
    private String hintSubscriberName = "-";
    private Logger logger = LogManager.getLogger("applog");
    private HintsTableModel model;
    private int pendingPublisher = 0;
    private int pendingSubscriber = 0;
    private ArrayList<Hint> pendingPublisherHints;
    private ArrayList<Hint> pendingSubscriberHints;
    private String publisherDisplayName = "";
    private String subscriberDisplayName = "";
    private Repositories repositories = null;

    public HintsUI(Context context)
    {
        super(context.mainFrame);
        this.context = context;
        boolean fault = false;

        try
        {
            repositories = getRepositories();

            // get data for initialization of table and model
            if (context.datastore != null)
                context.datastore.reload();
            else
                hints = new ArrayList<>();

            if (context.hintsHandler == null)
                context.hintsHandler = new Hints(context, null);

            hints = context.hintsHandler.getAll();
        }
        catch (Exception e)
        {
            fault = true;
            String msg = context.cfg.gs("Z.exception") + e.getMessage();
            logger.error(msg);
            JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
        }

        if (!fault)
        {
            initComponents();
            model = (HintsTableModel) tableHints.getModel();

            // scale the help icon
            Icon icon = labelHelp.getIcon();
            Image image = Utils.iconToImage(icon);
            Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
            Icon replacement = new ImageIcon(scaled);
            labelHelp.setIcon(replacement);

            // restore window & column preferences
            if (context.preferences.getHintsXpos() != -1 && Utils.isOnScreen(context.preferences.getHintsXpos(),
                    context.preferences.getHintsYpos()))
            {
                this.setLocation(context.preferences.getHintsXpos(), context.preferences.getHintsYpos());
                Dimension dim = new Dimension(context.preferences.getHintsWidth(), context.preferences.getHintsHeight());
                this.setSize(dim);
            }
            else
            {
                this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
            }

            this.addComponentListener(new ComponentAdapter()
            {
                @Override
                public void componentResized(ComponentEvent e)
                {
                    super.componentResized(e);
                    Dimension d = labelStatus.getSize();
                    labelStatus.setPreferredSize(d);
                }
            });
            Dimension d = labelStatus.getSize();
            labelStatus.setPreferredSize(d);

            // Escape key
            ActionListener escListener = new AbstractAction()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    cancelButton.doClick();
                }
            };
            getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

            if (model != null && hints != null)
            {
                for (Hint hint : hints)
                {
                    hint.selected = false;
                }
                model.fireTableDataChanged();
            }

            if (context.cfg.isHintTrackingEnabled())
            {
                refresh();
            }
            else
            {
                labelStatus.setText(context.cfg.gs("HintsUI.hint.tracker.server.not.enabled"));
            }

            setButtons();

            context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");

            setVisible(true);
            requestFocus();
        }
    }

    private void actionAllClicked(ActionEvent e)
    {
        if (model != null && hints != null)
        {
            for (Hint hint : hints)
            {
                hint.selected = true;
            }
            model.fireTableDataChanged();
        }
        int count = setButtons();
        labelStatus.setText(count + context.cfg.gs("HintsUI.hints.selected"));
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int count = 0;
        if (model != null && hints != null)
        {
            for (Hint hint : hints)
            {
                if (hint.selected)
                    ++count;
            }
            if (count > 0)
            {
                labelStatus.setText(count + context.cfg.gs("HintsUI.hints.selected"));
                int r = JOptionPane.showConfirmDialog(this,
                        java.text.MessageFormat.format(context.cfg.gs("HintsUI.are.you.sure.you.want.to.delete"), count),
                        context.cfg.gs("HintsUI.this.title"), JOptionPane.YES_NO_OPTION);
                if (r == JOptionPane.YES_OPTION)
                {
                    for (int i = hints.size() - 1; i >= 0; --i)
                    {
                        if (hints.get(i).selected)
                        {
                            hints.remove(i);
                            changesMade = true;
                        }
                    }
                    model.fireTableDataChanged();
                    checkNotificationDisplay();
                }
            }
        }
        labelStatus.setText("");
    }
    
    private void actionCancelClicked(ActionEvent e)
    {
        if (changesMade)
        {
            Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no")};
            int reply = JOptionPane.showOptionDialog(this,
                    context.cfg.gs("Z.cancel.all.changes"),
                    getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                    null, opts, opts[1]);
            if (reply != JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        context.navigator.checkForHints();
        context.navigator.enableDisableSystemMenus(null, true);
        setVisible(false);
    }

    private void actionOkClicked(ActionEvent e)
    {
        if (changesMade)
        {
            try
            {
                context.hintsHandler.save(hints);
            }
            catch (Exception e1)
            {
                String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e1);
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
        savePreferences();
        context.navigator.enableDisableSystemMenus(null, true);
        setVisible(false);
    }

    private void actionHelpClicked(MouseEvent e)
    {
        helpDialog = new NavHelp(this, this, context, context.cfg.gs("HintsUI.help"), "hints_" + context.preferences.getLocale() + ".html", false);
        if (!helpDialog.fault)
            helpDialog.buttonFocus();
    }

    private void actionNoneClicked(ActionEvent e)
    {
        if (model != null && hints != null)
        {
            for (Hint hint : hints)
            {
                hint.selected = false;
            }
            model.fireTableDataChanged();
        }
        setButtons();
        labelStatus.setText("");
    }

    private void actionRunClicked(ActionEvent e)
    {
        JTree currentTree = null;
        String displayName = "";
        boolean isPublisher = false;
        String name = "";
        Repository repo;

        labelStatus.setText("");

        // publisher or subscriber?
        Object object = context.browser.lastComponent;
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            name = sourceTree.getName();
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            name = sourceTable.getName();
        }

        // do not allow System origin
        if (name.toLowerCase().contains("system"))
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, context.cfg.gs("Z.select.collection.for.run"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }

        // which is it?
        String which;
        if (name.toLowerCase().endsWith("one"))
        {
            isPublisher = true;
            repo = context.publisherRepo;
            currentTree = context.mainFrame.treeCollectionOne;
            which = context.cfg.gs("Z.publisher");
            if (context.publisherRepo != null)
                displayName = context.publisherRepo.getLibraryData().libraries.description;
        }
        else
        {
            repo = context.subscriberRepo;
            currentTree = context.mainFrame.treeCollectionTwo;
            which = context.cfg.gs("Z.subscriber");
            if (context.subscriberRepo != null)
                displayName = context.subscriberRepo.getLibraryData().libraries.description;
        }

        // is it in keys?
        if (context.hintKeys.findKey(repo.getLibraryData().libraries.key) == null)
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this,
                    java.text.MessageFormat.format(context.cfg.gs("HintsUI.current.key.was.not.found.in.hint.keys"), displayName),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }

        // is it open?
        if (displayName.length() < 1)
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, context.cfg.gs("HintsUI.select.open"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }

        // is it a workstation?
        if (isPublisher && context.preferences.isLastPublisherIsWorkstation())
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, context.cfg.gs("HintsUI.not.workstation"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }

        // any Hints selected For chosen collection?
        ArrayList<Hint> pendingFor = getSelectedFor(isPublisher ? hintPublisherName : hintSubscriberName);
        if (pendingFor == null || pendingFor.size() < 1)
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, java.text.MessageFormat.format(context.cfg.gs("HintsUI.no.selected.hints.pending.for"), displayName),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }

        // prompt and process
        int reply = JOptionPane.showConfirmDialog(this, java.text.MessageFormat.format(context.cfg.gs("HintsUI.run.tool.on.collection"), displayName),
                context.cfg.gs("HintsUI.this.title"), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            try
            {
                String result = context.hintsHandler.hintsMunge(pendingFor, isPublisher, model, null);
                if (!result.toLowerCase().equals("false"))
                {
                    if (context.preferences.isAutoRefresh())
                        context.browser.rescanByTreeOrTable(currentTree);
                    logger.info(context.cfg.gs(("HintsUI.hints.run.complete")) + result);
                }
                refresh();
            }
            catch (Exception e1)
            {
                String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e1);
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void checkNotificationDisplay()
    {
        int count = 0;
        if (context.cfg.isHintTrackingEnabled() && context.hintsHandler != null)
        {
            try
            {
                HintKey hk = context.hintsHandler.findHintKey(context.publisherRepo);
                if (hk != null)
                    count = getHintCount(hk.system);

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
    }

    private int getHintCount(String system)
    {
        int count = 0;
        if (hints != null && hints.size() > 0)
        {
            for (int i = 0; i < hints.size(); ++i)
            {
                HintStatus hs = hints.get(i).findStatus(system);
                if (hs != null && !hs.status.toLowerCase().equals("done"))
                    ++count;
                else if (hs == null)
                    ++count;
            }
        }
        return count;
    }

    private Repositories getRepositories()
    {
        Repositories repositories = null;
        try
        {
            repositories = new Repositories();
            repositories.loadList(context);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
        }
        return repositories;
    }

    private ArrayList<Hint> getSelectedFor(String hintSystemName)
    {
        ArrayList<Hint> results = new ArrayList<Hint>();
        if (model != null && hints != null)
        {
            for (Hint hint : hints)
            {
                if (hint.selected)
                {
                    if (hint.isFor(hintSystemName) >= -1)
                        results.add(hint);
                }
            }
            // must be in utc time order regardless of sort
            if (results.size() > 0)
                Collections.sort(results);
        }
        return results;
    }

    public void refresh()
    {
        try
        {
            repositories = getRepositories();

            if (context.cfg.isHintTrackingEnabled())
            {
                hints = context.hintsHandler.getAll();

                BrowserTableCellRenderer btcr = new BrowserTableCellRenderer(context, tableHints);
                // only the textual columns
                for (int i = 1; i < 9; ++i)
                {
                    tableHints.getColumnModel().getColumn(i).setCellRenderer(btcr);
                }

                btcr = (BrowserTableCellRenderer) tableHints.getColumnModel().getColumn(4).getCellRenderer();
                btcr.setHorizontalAlignment(JLabel.CENTER);
                tableHints.getColumnModel().getColumn(4).setCellRenderer(btcr);

                model.fireTableDataChanged();

                pendingPublisher = 0;
                pendingSubscriber = 0;

                if (context.publisherRepo != null)
                {
                    publisherDisplayName = context.publisherRepo.getLibraryData().libraries.description;
                    HintKey hk = context.hintKeys.findKey(context.publisherRepo.getLibraryData().libraries.key);
                    if (hk != null)
                    {
                        hintPublisherName = hk.system;
                        pendingPublisherHints = context.hintsHandler.getFor(hintPublisherName);
                        if (pendingPublisherHints != null)
                            pendingPublisher = pendingPublisherHints.size();
                    }
                    else
                    {
                        if (context.preferences.isLastPublisherIsWorkstation())
                            hintPublisherName = publisherDisplayName + "*";
                        else
                            hintPublisherName = java.text.MessageFormat.format(context.cfg.gs("HintsUI.not.in.hint.keys"),
                                    context.publisherRepo.getLibraryData().libraries.description);
                    }
                }
                else
                    publisherDisplayName = context.cfg.gs("HintsUI.not.loaded");

                if (context.subscriberRepo != null)
                {
                    HintKey hk = context.hintKeys.findKey(context.subscriberRepo.getLibraryData().libraries.key);
                    if (hk != null)
                    {
                        hintSubscriberName = hk.system;
                        subscriberDisplayName = context.subscriberRepo.getLibraryData().libraries.description;
                        pendingSubscriberHints = context.hintsHandler.getFor(hintSubscriberName);
                        if (pendingSubscriberHints != null)
                            pendingSubscriber = pendingSubscriberHints.size();
                    }
                    else
                        subscriberDisplayName = java.text.MessageFormat.format(context.cfg.gs("HintsUI.not.in.hint.keys"),
                                context.subscriberRepo.getLibraryData().libraries.description);
                }
                else
                {
                    subscriberDisplayName = context.cfg.gs("HintsUI.not.loaded");
                }

                tableHints.getColumnModel().getColumn(0).setPreferredWidth(22);
                setWidths();
                model.fireTableDataChanged();
                refreshStatus();
            }
            else
            {
                labelStatus.setText(context.cfg.gs("HintsUI.hint.tracker.server.not.enabled"));
            }

            context.navigator.checkForHints();
            setButtons();
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshStatus()
    {
        String msg;
        int count = 0;
        if (model != null && hints != null && hints.size() > 0)
            count = hints.size();
        msg = java.text.MessageFormat.format(context.cfg.gs("HintsUI.hints.for"),
                count, context.hintKeys.size(), pendingPublisher, publisherDisplayName, pendingSubscriber, subscriberDisplayName);
        labelStatus.setText(msg);
    }

    private void savePreferences()
    {
        context.preferences.setHintsHeight(this.getHeight());
        context.preferences.setHintsWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setHintsXpos(location.x);
        context.preferences.setHintsYpos(location.y);

        if (context.cfg.isHintTrackingEnabled())
        {
            context.preferences.setHintsSystemWidth(tableHints.getColumnModel().getColumn(1).getWidth());
            context.preferences.setHintsByWidth(tableHints.getColumnModel().getColumn(2).getWidth());
            context.preferences.setHintsDateWidth(tableHints.getColumnModel().getColumn(3).getWidth());
            context.preferences.setHintsActionWidth(tableHints.getColumnModel().getColumn(4).getWidth());
            context.preferences.setHintsFromLibWidth(tableHints.getColumnModel().getColumn(5).getWidth());
            context.preferences.setHintsFromItemWidth(tableHints.getColumnModel().getColumn(6).getWidth());
            context.preferences.setHintsToLibWidth(tableHints.getColumnModel().getColumn(7).getWidth());
            context.preferences.setHintsToItemWidth(tableHints.getColumnModel().getColumn(8).getWidth());
            context.preferences.setHintsStatusWidth(tableHints.getColumnModel().getColumn(9).getWidth());
        }
    }

    public int setButtons()
    {
        int count = 0;
        if (model != null && hints != null && hints.size() > 0)
        {
            for (Hint hint : hints)
            {
                if (hint.selected)
                    ++count;
            }
        }
        boolean enable = count > 0 ? true : false;
        buttonDelete.setEnabled(enable);
        buttonRun.setEnabled(enable);
        return count;
    }

    private void setWidths()
    {
        tableHints.getColumnModel().getColumn(0).setPreferredWidth(22);
        tableHints.getColumnModel().getColumn(0).setWidth(22);
        tableHints.getColumnModel().getColumn(0).setResizable(false);
        tableHints.getColumnModel().getColumn(1).setPreferredWidth(context.preferences.getHintsSystemWidth());
        tableHints.getColumnModel().getColumn(1).setWidth(context.preferences.getHintsSystemWidth());
        tableHints.getColumnModel().getColumn(2).setPreferredWidth(context.preferences.getHintsByWidth());
        tableHints.getColumnModel().getColumn(2).setWidth(context.preferences.getHintsByWidth());
        tableHints.getColumnModel().getColumn(3).setPreferredWidth(context.preferences.getHintsDateWidth());
        tableHints.getColumnModel().getColumn(3).setWidth(context.preferences.getHintsDateWidth());
        tableHints.getColumnModel().getColumn(4).setPreferredWidth(context.preferences.getHintsActionWidth());
        tableHints.getColumnModel().getColumn(4).setWidth(context.preferences.getHintsActionWidth());
        tableHints.getColumnModel().getColumn(5).setPreferredWidth(context.preferences.getHintsFromLibWidth());
        tableHints.getColumnModel().getColumn(5).setWidth(context.preferences.getHintsFromLibWidth());
        tableHints.getColumnModel().getColumn(6).setPreferredWidth(context.preferences.getHintsFromItemWidth());
        tableHints.getColumnModel().getColumn(6).setWidth(context.preferences.getHintsFromItemWidth());
        tableHints.getColumnModel().getColumn(7).setPreferredWidth(context.preferences.getHintsToLibWidth());
        tableHints.getColumnModel().getColumn(7).setWidth(context.preferences.getHintsToLibWidth());
        tableHints.getColumnModel().getColumn(8).setPreferredWidth(context.preferences.getHintsToItemWidth());
        tableHints.getColumnModel().getColumn(8).setWidth(context.preferences.getHintsToItemWidth());

        for (int i = 0; i < context.hintKeys.size(); ++i)
        {
            tableHints.getColumnModel().getColumn(i + 9).setPreferredWidth(context.preferences.getHintsStatusWidth());
            tableHints.getColumnModel().getColumn(i + 9).setWidth(context.preferences.getHintsStatusWidth());
        }
    }

    private void windowClosing(WindowEvent e)
    {
        context.navigator.enableDisableSystemMenus(null, true);
        cancelButton.doClick();
    }


    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonDelete = new JButton();
        hSpace42 = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        scrollPaneHints = new JScrollPane();
        tableHints = new JTable();
        panelOptionsButtons = new JPanel();
        buttonAll = new JButton();
        buttonNone = new JButton();
        panelBottom = new JPanel();
        labelStatus = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        tableHints.setModel(new HintsTableModel(context, repositories, this));

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("HintsUI.this.title"));
        setMinimumSize(new Dimension(150, 126));
        setName("dialogEmptyDirectoryUI");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                HintsUI.this.windowClosing(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(600, 550));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setMinimumSize(new Dimension(140, 38));
                    panelTop.setPreferredSize(new Dimension(570, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("HintsUI.buttonDelete.text"));
                        buttonDelete.setMnemonic('D');
                        buttonDelete.setToolTipText(context.cfg.gs("HintsUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpace42 ----
                        hSpace42.setMinimumSize(new Dimension(22, 6));
                        hSpace42.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpace42);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("Z.run.ellipsis"));
                        buttonRun.setMnemonic(context.cfg.gs("HintsUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("HintsUI.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.WEST);

                    //======== panelHelp ========
                    {
                        panelHelp.setPreferredSize(new Dimension(40, 38));
                        panelHelp.setMinimumSize(new Dimension(0, 38));
                        panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                        //---- labelHelp ----
                        labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelHelp.setPreferredSize(new Dimension(32, 30));
                        labelHelp.setMinimumSize(new Dimension(32, 30));
                        labelHelp.setMaximumSize(new Dimension(32, 30));
                        labelHelp.setToolTipText(context.cfg.gs("HintsUI.labelHelp.toolTipText"));
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelHelp.setIconTextGap(0);
                        labelHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                actionHelpClicked(e);
                            }
                        });
                        panelHelp.add(labelHelp);
                    }
                    panelTop.add(panelHelp, BorderLayout.CENTER);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== scrollPaneHints ========
                {

                    //---- tableHints ----
                    tableHints.setFillsViewportHeight(true);
                    tableHints.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    tableHints.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
                    tableHints.setShowHorizontalLines(false);
                    tableHints.setShowVerticalLines(false);
                    tableHints.setAutoCreateRowSorter(true);
                    scrollPaneHints.setViewportView(tableHints);
                }
                contentPanel.add(scrollPaneHints, BorderLayout.CENTER);

                //======== panelOptionsButtons ========
                {
                    panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                    //---- buttonAll ----
                    buttonAll.setText(context.cfg.gs("HintsUI.buttonAll.text"));
                    buttonAll.setFont(buttonAll.getFont().deriveFont(buttonAll.getFont().getSize() - 2f));
                    buttonAll.setPreferredSize(new Dimension(78, 24));
                    buttonAll.setMinimumSize(new Dimension(78, 24));
                    buttonAll.setMaximumSize(new Dimension(78, 24));
                    buttonAll.setMnemonic(context.cfg.gs("HintsUI.buttonAll.mnemonic").charAt(0));
                    buttonAll.setToolTipText(context.cfg.gs("HintsUI.buttonAll.toolTipText"));
                    buttonAll.setMargin(new Insets(0, -10, 0, -10));
                    buttonAll.addActionListener(e -> actionAllClicked(e));
                    panelOptionsButtons.add(buttonAll);

                    //---- buttonNone ----
                    buttonNone.setText(context.cfg.gs("HintsUI.buttonNone.text"));
                    buttonNone.setFont(buttonNone.getFont().deriveFont(buttonNone.getFont().getSize() - 2f));
                    buttonNone.setPreferredSize(new Dimension(78, 24));
                    buttonNone.setMinimumSize(new Dimension(78, 24));
                    buttonNone.setMaximumSize(new Dimension(78, 24));
                    buttonNone.setMnemonic(context.cfg.gs("HintsUI.buttonNone.mnemonic").charAt(0));
                    buttonNone.setToolTipText(context.cfg.gs("HintsUI.buttonNone.toolTipText"));
                    buttonNone.setMargin(new Insets(0, -10, 0, -10));
                    buttonNone.addActionListener(e -> actionNoneClicked(e));
                    panelOptionsButtons.add(buttonNone);
                }
                contentPanel.add(panelOptionsButtons, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== panelBottom ========
            {
                panelBottom.setLayout(new BorderLayout());

                //---- labelStatus ----
                labelStatus.setMaximumSize(new Dimension(32768, 0));
                panelBottom.add(labelStatus, BorderLayout.CENTER);

                //======== buttonBar ========
                {
                    buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                    buttonBar.setLayout(new GridBagLayout());
                    ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                    ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                    //---- okButton ----
                    okButton.setText(context.cfg.gs("Z.ok"));
                    okButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                    okButton.addActionListener(e -> actionOkClicked(e));
                    buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                    //---- cancelButton ----
                    cancelButton.setText(context.cfg.gs("Z.cancel"));
                    cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
                    cancelButton.addActionListener(e -> actionCancelClicked(e));
                    buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                panelBottom.add(buttonBar, BorderLayout.LINE_END);
            }
            dialogPane.add(panelBottom, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    public JPanel dialogPane;
    public JPanel contentPanel;
    public JPanel panelTop;
    public JPanel panelTopButtons;
    public JButton buttonDelete;
    public JPanel hSpace42;
    public JButton buttonRun;
    public JPanel panelHelp;
    public JLabel labelHelp;
    public JScrollPane scrollPaneHints;
    public JTable tableHints;
    public JPanel panelOptionsButtons;
    public JButton buttonAll;
    public JButton buttonNone;
    public JPanel panelBottom;
    public JLabel labelStatus;
    public JPanel buttonBar;
    public JButton okButton;
    public JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    //
    // @formatter:on
    // </editor-fold>

}
