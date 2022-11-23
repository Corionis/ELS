package com.groksoft.els.gui.tools.duplicateFinder;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.repository.Item;
import com.groksoft.els.repository.Library;
import com.groksoft.els.repository.Repository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class DuplicateFinderUI extends JDialog
{
    private boolean crossLibrary = false;
    private ArrayList<Dupe> dupes;
    private final DuplicateFinderUI thisDialog = this;
    private GuiContext guiContext;
    private boolean isPublisher = false;
    private NavHelp helpDialog;
    private Logger logger = LogManager.getLogger("applog");

    private DuplicateFinderUI()
    {
        // hide default constructor
    }

    public DuplicateFinderUI(Window owner, GuiContext guiContext)
    {
        super(owner);
        this.guiContext = guiContext;
        
        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position, size & divider
        if (guiContext.preferences.getToolsDuplicateFinderXpos() > 0)
        {
            this.setLocation(guiContext.preferences.getToolsDuplicateFinderXpos(), guiContext.preferences.getToolsDuplicateFinderYpos());
            Dimension dim = new Dimension(guiContext.preferences.getToolsDuplicateFinderWidth(), guiContext.preferences.getToolsDuplicateFinderHeight());
            this.setSize(dim);
        }
        else
        {
            Point parentPos = this.getParent().getLocation();
            Dimension parentSize = this.getParent().getSize();
            Dimension mySize = this.getSize();
            Point myPos = new Point(parentPos.x + (parentSize.width / 2 - mySize.width / 2),
                    parentPos.y + (parentSize.height / 2 - mySize.height / 2));
            this.setLocation(myPos);
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

        adjustDupesTable();
    }

    private void actionCancelClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void actionCrossLibraryClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("crossLibraryChanged"))
            {
                crossLibrary = checkBoxCrossLibrary.isSelected();
                guiContext.cfg.setCrossCheck(crossLibrary);
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("DuplicateFinder.help"), "duplicatefinder_" + guiContext.preferences.getLocale() + ".html");
        }
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

    private void actionOkClicked(ActionEvent e)
    {
        savePreferences();
        setVisible(false);
    }

    private void actionRunClicked(ActionEvent e)
    {
        String name = "";

        // publisher or subscriber?
        Object object = guiContext.browser.lastComponent;
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
        // do not allow system origin
        if (name.toLowerCase().contains("system"))
        {
            Object[] opts = { "OK"};
            JOptionPane.showOptionDialog(this, guiContext.cfg.gs("DuplicateFinder.please.select.a.collection.for.run"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }
        // which is it?
        String which;
        if (name.toLowerCase().endsWith("one"))
        {
            isPublisher = true;
            which = guiContext.cfg.gs("Z.publisher");
        }
        else
        {
            isPublisher = false;
            which = guiContext.cfg.gs("Z.subscriber");
        }

        // prompt and process
        int reply = JOptionPane.showConfirmDialog(this, java.text.MessageFormat.format(guiContext.cfg.gs("DuplicateFinder.run.tool.on.collection"), which), this.getTitle(), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            process();
        }
    }

    private void adjustDupesTable()
    {
        dupes = new ArrayList<Dupe>();
        tableDupes.setModel(new DupesTableModel(guiContext.cfg, dupes));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();

        // path
        TableColumn column = tableDupes.getColumnModel().getColumn(0);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setMinWidth(32);
        column.setCellRenderer(cellRenderer);
        column.setResizable(true);

        // selection column
        column = tableDupes.getColumnModel().getColumn(1);
        column.setResizable(false);
        column.setWidth(96);
        column.setPreferredWidth(96);
        column.setMaxWidth(96);
        column.setMinWidth(96);
    }

    private void process()
    {
        try
        {
            final Repository repo = (isPublisher) ? guiContext.context.publisherRepo : guiContext.context.subscriberRepo;
            if (repo != null)
            {
                checkBoxCrossLibrary.setEnabled(false);
                buttonRun.setEnabled(false);
                okButton.setEnabled(false);
                cancelButton.setEnabled(false);
                dupes = new ArrayList<Dupe>();
                DupesTableModel dtm = (DupesTableModel) tableDupes.getModel();
                dtm.setDupes(dupes);
                dtm.fireTableDataChanged();
                labelStatus.setText("Working ...");
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void >()
                {
                    int duplicates = 0;
                    int totalDirectories = 0;
                    int totalItems = 0;

                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        // get content
                        if (isPublisher)
                        {
                            labelStatus.setText("Scanning ...");
                            repo.scan();
                        }
                        else
                        {
                            if (guiContext.cfg.isRemoteSession())
                            {
                                if (!guiContext.context.clientStty.isConnected())
                                {
                                    Object[] opts = {"OK"};
                                    JOptionPane.showOptionDialog(thisDialog, guiContext.cfg.gs("Browser.connection.lost"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }

                                if (guiContext.context.transfer != null)
                                {
                                    labelStatus.setText("Requesting collection data from remote ...");
                                    guiContext.context.transfer.requestCollection();
                                }
                                else
                                {
                                    Object[] opts = {"OK"};
                                    JOptionPane.showOptionDialog(thisDialog, guiContext.cfg.gs("Transfer.could.not.retrieve.remote.collection.file"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }
                            }
                            else
                            {
                                labelStatus.setText("Scanning ...");
                                repo.scan();
                            }

                        }
                        final Repository repo = (isPublisher) ? guiContext.context.publisherRepo : guiContext.context.subscriberRepo;

                        // analyze the collection items
                        totalDirectories = 0;
                        totalItems = 0;
                        for (Library pubLib : repo.getLibraryData().libraries.bibliography)
                        {
                            String msg = guiContext.cfg.gs("DuplicateFinder.analyzing.library") + "'" + pubLib.name + "'";
                            labelStatus.setText(msg);
                            guiContext.browser.printLog(msg);
                            for (Item item : pubLib.items)
                            {
                                if (item.isDirectory())
                                    ++totalDirectories;
                                else
                                    ++totalItems;

                                // populate the item.hasList
                                repo.hasPublisherDuplicate(item, Utils.pipe(repo, item.getItemPath()));
                            }
                        }

                        for (Library pubLib : repo.getLibraryData().libraries.bibliography)
                        {
                            String msg = guiContext.cfg.gs("DuplicateFinder.analyzing.library") + "'" + pubLib.name + "'";
                            labelStatus.setText(msg);
                            for (Item item : pubLib.items)
                            {
                                if (item.getHas().size() > 0)
                                {
                                    duplicates = queueDupe(item, duplicates);
                                }
                            }
                        }

                        labelStatus.setText("  " + java.text.MessageFormat.format(guiContext.cfg.gs("DuplicateFinder.duplicates.items.directories"),
                                duplicates, totalItems, totalDirectories));

                        return null;
                    };
                };
                if (worker != null)
                {
                    worker.addPropertyChangeListener(new PropertyChangeListener()
                    {
                        @Override
                        public void propertyChange(PropertyChangeEvent e)
                        {
                            if (e.getPropertyName().equals("state"))
                            {
                                if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                {
                                    checkBoxCrossLibrary.setEnabled(true);
                                    buttonRun.setEnabled(true);
                                    okButton.setEnabled(true);
                                    cancelButton.setEnabled(true);
                                    DupesTableModel dtm = (DupesTableModel) tableDupes.getModel();
                                    dtm = (DupesTableModel) tableDupes.getModel();
                                    dtm.setDupes(dupes);
                                    dtm.fireTableDataChanged();
                                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                                }
                            }
                        }
                    });
                }
                worker.execute();
            }
            else
            {
                Object[] opts = { "OK"};
                JOptionPane.showOptionDialog(this, guiContext.cfg.gs("DuplicateFinder.collection.not.loaded"),
                        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                        null, opts, opts[0]);
            }
        }
        catch (Exception ex)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(ex);
            guiContext.browser.printLog(msg, true);
            JOptionPane.showMessageDialog(this, msg, this.getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private int queueDupe(Item item, int duplicates)
    {
        if (!item.isReported())
        {
            dupes.add(new Dupe(item, true));
            dupes.add(new Dupe(item));
            ++duplicates;
            item.setReported(true);
            for (Item dupe : item.getHas())
            {
                if (!dupe.isReported())
                {
                    dupes.add(new Dupe(dupe));
                    ++duplicates;
                    dupe.setReported(true);
                }
            }
            dupes.add(new Dupe(null));
        }
        return duplicates;
    }

    private void savePreferences()
    {
        guiContext.preferences.setToolsDuplicateFinderHeight(this.getHeight());
        guiContext.preferences.setToolsDuplicateFinderWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setToolsDuplicateFinderXpos(location.x);
        guiContext.preferences.setToolsDuplicateFinderYpos(location.y);
    }

    // ================================================================================================================

    public class Dupe
    {
        Item item;
        boolean gone = false;
        boolean separator = false;

        public Dupe(Item item)
        {
            this.item = item;
        }

        public Dupe(Item item, boolean separator)
        {
            this.item = item;
            this.separator = separator;
        }
    }

    // ================================================================================================================

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        checkBoxCrossLibrary = new JCheckBox();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        scrollPaneDupes = new JScrollPane();
        tableDupes = new JTable();
        panelBottom = new JPanel();
        labelStatus = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(guiContext.cfg.gs("DuplicateFinder.this.title"));
        setName("dialogDuplicateFinderUI");
        setMinimumSize(new Dimension(150, 126));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setMinimumSize(new Dimension(214, 152));
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

                        //---- checkBoxCrossLibrary ----
                        checkBoxCrossLibrary.setText(guiContext.cfg.gs("DuplicateFinder.checkBoxCrossLibrary.text"));
                        checkBoxCrossLibrary.setActionCommand("crossLibraryChanged");
                        checkBoxCrossLibrary.setToolTipText(guiContext.cfg.gs("DuplicateFinder.checkBoxCrossLibrary.toolTipText"));
                        checkBoxCrossLibrary.addActionListener(e -> actionCrossLibraryClicked(e));
                        panelTopButtons.add(checkBoxCrossLibrary);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("DuplicateFinder.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("DuplicateFinder.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("DuplicateFinder.buttonRun.toolTipText"));
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
                        labelHelp.setToolTipText(guiContext.cfg.gs("DuplicateFinder.labelHelp.toolTipText"));
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

                //======== scrollPaneDupes ========
                {

                    //---- tableDupes ----
                    tableDupes.setFillsViewportHeight(true);
                    tableDupes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    scrollPaneDupes.setViewportView(tableDupes);
                }
                contentPanel.add(scrollPaneDupes, BorderLayout.CENTER);

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
                        okButton.setText(guiContext.cfg.gs("DuplicateFinder.okButton.text"));
                        okButton.setToolTipText(guiContext.cfg.gs("Z.save.changes.toolTipText"));
                        okButton.addActionListener(e -> actionOkClicked(e));
                        buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));

                        //---- cancelButton ----
                        cancelButton.setText(guiContext.cfg.gs("DuplicateFinder.cancelButton.text"));
                        cancelButton.setToolTipText(guiContext.cfg.gs("Z.cancel.changes.toolTipText"));
                        cancelButton.addActionListener(e -> actionCancelClicked(e));
                        buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 0), 0, 0));
                    }
                    panelBottom.add(buttonBar, BorderLayout.LINE_END);
                }
                contentPanel.add(panelBottom, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JCheckBox checkBoxCrossLibrary;
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JScrollPane scrollPaneDupes;
    private JTable tableDupes;
    private JPanel panelBottom;
    private JLabel labelStatus;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
