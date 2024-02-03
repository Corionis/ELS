package com.corionis.els.gui.hints;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.util.TooltipsTable;
import com.corionis.els.hints.Hint;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintStatus;
import com.corionis.els.repository.Repositories;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;

public class HintsUI extends JDialog
{
    private Context context;
    private boolean changesMade = false;
    private NavHelp helpDialog;
    private String hintPublisherName = "-";
    private String hintSubscriberName = "-";
    private Logger logger = LogManager.getLogger("applog");
    private HintsTableModel model;
    private int pendingPublisher = 0;
    private int pendingSubscriber = 0;
    private ArrayList<Hint> pendingPublisherHints;
    private ArrayList<Hint> pendingSubscriberHints;
    private Repositories repositories;

    public HintsUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        initComponents();

        ArrayList<Hint> hints = null;

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // restore window & column preferences
        if (context.preferences.getHintsXpos() != 0 && Utils.isOnScreen(context.preferences.getHintsXpos(),
                context.preferences.getHintsYpos()))
        {
            this.setLocation(context.preferences.getHintsXpos(), context.preferences.getHintsYpos());
            Dimension dim = new Dimension(context.preferences.getHintsWidth(), context.preferences.getHintsHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(this));
        }

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

        // get data
        repositories = getRepositories();

        pendingPublisher = 0;
        int count = 0;
        String publisherDisplayName = "";
        String subscriberDisplayName = "";
        if (context.cfg.isHintTrackingEnabled())
        {
            try
            {
                if (context.datastore != null)
                {
                    context.datastore.reload();
                    hints = context.hints.getAll();
                }
                else
                    hints = new ArrayList<>();
                model = new HintsTableModel(context, repositories, hints);
                tableHints.setModel(model);

                tableHints.getColumnModel().getColumn(0).setPreferredWidth(22);

                DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
                cellRenderer.setHorizontalAlignment(JLabel.CENTER);
                tableHints.getColumnModel().getColumn(4).setCellRenderer(cellRenderer);

                if (model != null && model.hints != null && model.hints.size() > 0)
                    count = model.hints.size();

                if (context.publisherRepo != null)
                {
                    publisherDisplayName = context.publisherRepo.getLibraryData().libraries.description;
                    HintKey hk = context.hintKeys.findKey(context.publisherRepo.getLibraryData().libraries.key);
                    if (hk != null)
                    {
                        hintPublisherName = hk.system;
                        pendingPublisherHints = context.hints.getFor(hintPublisherName);
                        if (pendingPublisherHints != null)
                            pendingPublisher = pendingPublisherHints.size();
                    }
                    else
                    {
                        if (context.preferences.isLastPublisherIsWorkstation())
                            hintPublisherName = publisherDisplayName + "*";
                        else
                            throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("Hints.the.current.key.was.not.found.in.hint.keys.file"),
                                    "publisher", context.hintKeys.getFilename()));
                    }
                }
                if (context.subscriberRepo != null)
                {
                    HintKey hk = context.hintKeys.findKey(context.subscriberRepo.getLibraryData().libraries.key);
                    if (hk != null)
                    {
                        hintSubscriberName = hk.system;
                        subscriberDisplayName = context.subscriberRepo.getLibraryData().libraries.description;
                        pendingSubscriberHints = context.hints.getFor(hintSubscriberName);
                        if (pendingSubscriberHints != null)
                            pendingSubscriber = pendingSubscriberHints.size();
                    }
                    else
                    {
                        throw new MungeException(java.text.MessageFormat.format(context.cfg.gs("Hints.the.current.key.was.not.found.in.hint.keys.file"),
                                "subscriber", context.hintKeys.getFilename()));
                    }
                }

                setWidths();

                String msg = java.text.MessageFormat.format(context.cfg.gs("HintsUI.hints.for"),
                        count, context.hintKeys.size(), pendingPublisher, publisherDisplayName, pendingSubscriber, subscriberDisplayName);

                labelStatus.setText(msg);
            }
            catch (Exception e)
            {
                String msg = context.cfg.gs("Z.exception") + e.getMessage();
                logger.error(msg);
                JOptionPane.showMessageDialog(context.mainFrame, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
            }
        }

        if (model != null && model.hints != null)
        {
            for (Hint hint : model.hints)
            {
                hint.selected = false;
            }
            model.fireTableDataChanged();
        }

        setButtons();

        context.mainFrame.labelStatusMiddle.setText("");
        setVisible(true);
        requestFocus();
    }

    private void actionAllClicked(ActionEvent e)
    {
        if (model != null && model.hints != null)
        {
            for (Hint hint : model.hints)
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
        if (model != null && model.hints != null)
        {
            for (Hint hint : model.hints)
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
                    for (int i = model.hints.size() - 1; i >= 0; --i)
                    {
                        if (model.hints.get(i).selected)
                        {
                            model.hints.remove(i);
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
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.cancel.all.changes"),
                    context.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
            {
                return;
            }
        }
        context.navigator.checkForHints(true);
        context.navigator.enableDisableSystemMenus(null, true);
        setVisible(false);
    }

    private void actionOkClicked(ActionEvent e)
    {
        if (changesMade)
        {
            try
            {
                context.hints.save(model.hints);
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
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("HintsUI.help"), "hints_" + context.preferences.getLocale() + ".html");
        }
        if (!helpDialog.fault)
        {
            if (!helpDialog.isVisible())
            {
                helpDialog.setVisible(true);
                // offset the help dialog from the parent dialog
                Point loc = this.getLocation();
                loc.x = loc.x + 32;
                loc.y = loc.y + 32;
                helpDialog.setLocation(loc);
            }
            else
            {
                helpDialog.toFront();
            }
        }
    }

    private void actionNoneClicked(ActionEvent e)
    {
        if (model != null && model.hints != null)
        {
            for (Hint hint : model.hints)
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
            currentTree = context.mainFrame.treeCollectionOne;
            which = context.cfg.gs("Z.publisher");
            if (context.publisherRepo != null)
                displayName = context.publisherRepo.getLibraryData().libraries.description;
        }
        else
        {
            currentTree = context.mainFrame.treeCollectionTwo;
            which = context.cfg.gs("Z.subscriber");
            if (context.subscriberRepo != null)
                displayName = context.subscriberRepo.getLibraryData().libraries.description;
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
        int reply = JOptionPane.showConfirmDialog(this, java.text.MessageFormat.format(context.cfg.gs("HintsUI.run.tool.on.collection"), which),
                context.cfg.gs("HintsUI.this.title"), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            try
            {
                String result = context.hints.hintsMunge(pendingFor, isPublisher, model);
                if (!result.toLowerCase().equals("false"))
                {
                    if (context.preferences.isAutoRefresh())
                        context.browser.rescanByTreeOrTable(currentTree);
                    logger.info(context.cfg.gs(("HintsUI.hints.run.complete")) + result);
                }
                model.hints = context.hints.getAll();
                model.fireTableDataChanged();
                context.navigator.checkForHints(true);
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
        if (context.cfg.isHintTrackingEnabled() && context.hints != null)
        {
            try
            {
                HintKey hk = context.hints.findHintKey(context.publisherRepo);
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
        if (model.hints != null && model.hints.size() > 0)
        {
            for (int i = 0; i < model.hints.size(); ++i)
            {
                HintStatus hs = model.hints.get(i).findStatus(system);
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
        if (model != null && model.hints != null)
        {
            for (Hint hint : model.hints)
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
            model.hints = context.hints.getAll();
            model.fireTableDataChanged();
            context.navigator.checkForHints(true);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("HintsUI.this.title"), JOptionPane.ERROR_MESSAGE);
        }

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
        if (model != null && model.hints != null && model.hints.size() > 0)
        {
            for (Hint hint : model.hints)
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
        tableHints = new TooltipsTable();
        panelOptionsButtons = new JPanel();
        buttonAll = new JButton();
        buttonNone = new JButton();
        panelBottom = new JPanel();
        labelStatus = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
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
                        buttonRun.setText(context.cfg.gs("HintsUI.buttonRun.text"));
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
                    tableHints.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
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
    public TooltipsTable tableHints;
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
