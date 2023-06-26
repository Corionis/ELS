package com.groksoft.els.gui.tools.duplicateFinder;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.bookmarks.Bookmark;
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
import javax.swing.table.TableColumn;

public class DuplicateFinderUI extends JDialog
{
    private ArrayList<Dupe> dupes;
    private Context context;
    private boolean isPublisher = false;
    private NavHelp helpDialog;
    private Logger logger = LogManager.getLogger("applog");
    private boolean requestStop = false;
    private final DuplicateFinderUI thisDialog = this;
    private boolean workerRunning = false;

    private DuplicateFinderUI()
    {
        // hide default constructor
    }

    public DuplicateFinderUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        
        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position, size & divider
        if (context.preferences.getToolsDuplicateFinderXpos() > 0)
        {
            this.setLocation(context.preferences.getToolsDuplicateFinderXpos(), context.preferences.getToolsDuplicateFinderYpos());
            Dimension dim = new Dimension(context.preferences.getToolsDuplicateFinderWidth(), context.preferences.getToolsDuplicateFinderHeight());
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
                closeButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        adjustDupesTable();
    }

    private void actionCloseClicked(ActionEvent e)
    {
        if (workerRunning)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.stop.run.after.scan"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                requestStop = true;
                logger.info(context.cfg.gs("Z.run.cancelled"));
            }
            else
                return;
        }
        savePreferences();
        setVisible(false);
    }

    private void actionCrossLibraryClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("crossLibraryChanged"))
            {
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("DuplicateFinder.help"), "duplicatefinder_" + context.preferences.getLocale() + ".html");
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

    private void actionRunClicked(ActionEvent e)
    {
        String name = "";

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
        // do not allow system origin
        if (name.toLowerCase().contains("system"))
        {
            Object[] opts = { context.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, context.cfg.gs("DuplicateFinder.please.select.a.collection.for.run"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[0]);
            return;
        }
        // which is it?
        String which;
        if (name.toLowerCase().endsWith("one"))
        {
            isPublisher = true;
            which = context.cfg.gs("Z.publisher");
        }
        else
        {
            isPublisher = false;
            which = context.cfg.gs("Z.subscriber");
        }

        // prompt and process
        int reply = JOptionPane.showConfirmDialog(this, java.text.MessageFormat.format(context.cfg.gs("DuplicateFinder.run.tool.on.collection"), which), this.getTitle(), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            process();
        }
    }

    private void adjustDupesTable()
    {
        tableDupes.setShowGrid(false);
        tableDupes.getTableHeader().setReorderingAllowed(false);
        tableDupes.setCellSelectionEnabled(false);
        tableDupes.setColumnSelectionAllowed(false);
        tableDupes.setRowSelectionAllowed(true);
        tableDupes.setRowHeight(24);

        dupes = new ArrayList<Dupe>();
        tableDupes.setModel(new DupesTableModel(context, dupes));

        // path
        TableColumn column = tableDupes.getColumnModel().getColumn(0);
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setCellRenderer(cellRenderer);
        column.setMinWidth(32);
        column.setPreferredWidth(320);
        column.setWidth(320);
        column.setResizable(true);

        // size
        column = tableDupes.getColumnModel().getColumn(1);
        cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
        column.setCellRenderer(cellRenderer);
        column.setMinWidth(18);
        column.setPreferredWidth(56);
        column.setWidth(56);
        column.setResizable(true);

        // modified date
        column = tableDupes.getColumnModel().getColumn(2);
        cellRenderer = new DefaultTableCellRenderer();
        cellRenderer.setHorizontalAlignment(JLabel.RIGHT);
        column.setCellRenderer(cellRenderer);
        column.setMinWidth(18);
        column.setPreferredWidth(90);
        column.setWidth(90);
        column.setResizable(true);

        // goto
        column = tableDupes.getColumnModel().getColumn(3);
        ActionsCell actionsCell = new ActionsCell(context, this);
        column.setMinWidth(68);
        column.setMaxWidth(68);
        column.setWidth(68);
        column.setPreferredWidth(68);
        column.setCellRenderer(actionsCell);
        column.setCellEditor(actionsCell);
        column.setResizable(false);
    }

    public void gotoItem(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("goto"))
            {
                int selected = tableDupes.getSelectedRow();
                if (tableDupes.isEditing())
                    tableDupes.getCellEditor().stopCellEditing();
                if (selected > 0)
                {
                    Dupe dupe = (Dupe) tableDupes.getValueAt(selected, 3);
                    Bookmark bm = context.browser.bookmarkCreate(dupe.item, "goto-dupe", isPublisher);
                    // TODO If there are dupes in more than one storage location "goto" stops at the first one
                    context.browser.bookmarkGoto(bm);
                }
            }
        }
    }

    private void process()
    {
        try
        {
            final Repository repo = (isPublisher) ? context.publisherRepo : context.subscriberRepo;
            if (repo != null)
            {
                checkBoxCrossLibrary.setEnabled(false);
                buttonRun.setEnabled(false);
                dupes = new ArrayList<Dupe>();
                DupesTableModel dtm = (DupesTableModel) tableDupes.getModel();
                dtm.setDupes(dupes);
                dtm.fireTableDataChanged();

                logger.info(context.cfg.gs("DuplicateFinder.running"));
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                context.cfg.setCrossCheck(checkBoxCrossLibrary.isSelected());

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void >()
                {
                    int duplicates = 0;
                    int totalDirectories = 0;
                    int totalItems = 0;

                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        workerRunning = true;

                        // get content
                        if (isPublisher)
                        {
                            labelStatus.setText(context.cfg.gs("Z.scanning"));
                            repo.scan();
                        }
                        else
                        {
                            if (context.cfg.isRemoteSession())
                            {
                                if (!context.clientStty.isConnected())
                                {
                                    Object[] opts = { context.cfg.gs("Z.ok") };
                                    JOptionPane.showOptionDialog(thisDialog, context.cfg.gs("Browser.connection.lost"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }

                                if (context.transfer != null)
                                {
                                    labelStatus.setText(context.cfg.gs("Z.requesting.collection.data.from.remote"));
                                    context.transfer.requestCollection();
                                }
                                else
                                {
                                    Object[] opts = { context.cfg.gs("Z.ok") };
                                    JOptionPane.showOptionDialog(thisDialog, context.cfg.gs("Transfer.could.not.retrieve.remote.collection.file"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }
                            }
                            else
                            {
                                labelStatus.setText(context.cfg.gs("Z.scanning"));
                                repo.scan();
                            }

                        }
                        final Repository repo = (isPublisher) ? context.publisherRepo : context.subscriberRepo;

                        // analyze the collection items
                        for (Library lib : repo.getLibraryData().libraries.bibliography)
                        {
                            if (requestStop)
                                break;
                            String msg = context.cfg.gs("DuplicateFinder.analyzing.library") + "'" + lib.name + "'";
                            labelStatus.setText(msg);
                            logger.info(msg);
                            for (Item item : lib.items)
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
                            if (requestStop)
                                break;
                            String msg = context.cfg.gs("DuplicateFinder.analyzing.library") + "'" + pubLib.name + "'";
                            labelStatus.setText(msg);
                            for (Item item : pubLib.items)
                            {
                                if (item.getHas().size() > 0)
                                {
                                    duplicates = queueDupe(item, duplicates);
                                }
                            }
                        }

                        labelStatus.setText("  " + java.text.MessageFormat.format(context.cfg.gs("DuplicateFinder.duplicates.items.directories"),
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
                                    workerRunning = false;
                                    checkBoxCrossLibrary.setEnabled(true);
                                    buttonRun.setEnabled(true);
                                    //closeButton.setEnabled(true);
                                    DupesTableModel dtm = (DupesTableModel) tableDupes.getModel();
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
                Object[] opts = { context.cfg.gs("Z.ok") };
                JOptionPane.showOptionDialog(this, context.cfg.gs("DuplicateFinder.collection.not.loaded"),
                        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                        null, opts, opts[0]);
            }
        }
        catch (Exception ex)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(ex);
            logger.error(msg);
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
        context.preferences.setToolsDuplicateFinderHeight(this.getHeight());
        context.preferences.setToolsDuplicateFinderWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsDuplicateFinderXpos(location.x);
        context.preferences.setToolsDuplicateFinderYpos(location.y);
    }

    private void windowClosing(WindowEvent e)
    {
        closeButton.doClick();
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
        closeButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("DuplicateFinder.this.title"));
        setName("dialogDuplicateFinderUI");
        setMinimumSize(new Dimension(150, 126));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DuplicateFinderUI.this.windowClosing(e);
            }
        });
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
                        checkBoxCrossLibrary.setText(context.cfg.gs("DuplicateFinder.checkBoxCrossLibrary.text"));
                        checkBoxCrossLibrary.setActionCommand("crossLibraryChanged");
                        checkBoxCrossLibrary.setToolTipText(context.cfg.gs("DuplicateFinder.checkBoxCrossLibrary.toolTipText"));
                        checkBoxCrossLibrary.setMnemonic(context.cfg.gs("DuplicateFinder.checkBoxCrossLibrary.mnemonic").charAt(0));
                        checkBoxCrossLibrary.addActionListener(e -> actionCrossLibraryClicked(e));
                        panelTopButtons.add(checkBoxCrossLibrary);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("DuplicateFinder.buttonRun.text"));
                        buttonRun.setMnemonic(context.cfg.gs("DuplicateFinder.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("DuplicateFinder.buttonRun.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("DuplicateFinder.help"));
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

                        //---- closeButton ----
                        closeButton.setText(context.cfg.gs("Z.done"));
                        closeButton.setToolTipText(context.cfg.gs("Z.done.toolTipText"));
                        closeButton.addActionListener(e -> actionCloseClicked(e));
                        buttonBar.add(closeButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JButton closeButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
