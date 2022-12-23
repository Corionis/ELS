package com.groksoft.els.gui.tools.emptyDirectoryFinder;

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
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

public class EmptyDirectoryFinderUI extends JDialog
{
    private ArrayList<Empty> empties;
    private GuiContext guiContext;
    private boolean isPublisher = false;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean requestStop = false;
    private final EmptyDirectoryFinderUI thisDialog = this;
    private boolean workerRunning = false;

    private EmptyDirectoryFinderUI()
    {
        // hide default constructor
    }

    public EmptyDirectoryFinderUI(Window owner, GuiContext guiContext)
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
        if (guiContext.preferences.getToolsEmptyDirectoryFinderXpos() > 0)
        {
            this.setLocation(guiContext.preferences.getToolsEmptyDirectoryFinderXpos(), guiContext.preferences.getToolsEmptyDirectoryFinderYpos());
            Dimension dim = new Dimension(guiContext.preferences.getToolsEmptyDirectoryFinderWidth(), guiContext.preferences.getToolsEmptyDirectoryFinderHeight());
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

        adjustEmptiesTable();
    }

    private void actionAllClicked(ActionEvent e)
    {
        if (empties.size() > 0)
        {
            for (int i = 0; i < empties.size(); ++i)
            {
                empties.get(i).isSelected = true;
            }
            EmptiesTableModel etm = (EmptiesTableModel) tableEmpties.getModel();
            etm.fireTableDataChanged();
        }
    }

    private void actionCloseClicked(ActionEvent e)
    {
        if (workerRunning)
        {
            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.stop.run.after.scan"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                requestStop = true;
                logger.info(guiContext.cfg.gs("Z.run.cancelled"));
            }
            else
                return;
        }
        savePreferences();
        setVisible(false);
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("EmptyDirectoryFinder.delete.the.selected.empties"),
                this.getTitle(), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            for (int i = 0; i < empties.size(); ++i)
            {
                Empty empty = empties.get(i);
                if (empty.isSelected)
                {
                    if (guiContext.context.transfer != null)
                    {
                        try
                        {
                            logger.info(guiContext.cfg.gs("EmptyDirectoryFinder.removing") + empty.path);
                            guiContext.context.transfer.remove(empty.path, true, guiContext.cfg.isRemoteSession());
                        }
                        catch (Exception ex)
                        {
                            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(ex);
                            logger.error(msg, true);
                            JOptionPane.showMessageDialog(this, msg, this.getTitle(), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
            Object[] opts = { guiContext.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, guiContext.cfg.gs("EmptyDirectoryFinder.removal.of.empties.successful"),
                    this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.INFORMATION_MESSAGE,
                    null, opts, opts[0]);
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("EmptyDirectoryFinder.help"), "emptydirectoryfinder_" + guiContext.preferences.getLocale() + ".html");
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

    private void actionNoneClicked(ActionEvent e)
    {
        if (empties.size() > 0)
        {
            for (int i = 0; i < empties.size(); ++i)
            {
                empties.get(i).isSelected = false;
            }
            EmptiesTableModel etm = (EmptiesTableModel) tableEmpties.getModel();
            etm.fireTableDataChanged();
        }
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
            Object[] opts = { guiContext.cfg.gs("Z.ok") };
            JOptionPane.showOptionDialog(this, guiContext.cfg.gs("EmptyDirectoryFinder.please.select.a.collection.for.run"),
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
            which = guiContext.cfg.gs("Z.subscriber");

        // prompt and process
        int reply = JOptionPane.showConfirmDialog(this, java.text.MessageFormat.format(guiContext.cfg.gs("EmptyDirectoryFinder.run.tool.on.collection"), which), this.getTitle(), JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION)
        {
            process();
        }
    }

    private void adjustEmptiesTable()
    {
        empties = new ArrayList<Empty>();
        tableEmpties.setModel(new EmptiesTableModel(guiContext.cfg, empties));

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();

        // selection column
        TableColumn column = tableEmpties.getColumnModel().getColumn(0);
        column.setResizable(false);
        column.setWidth(32);
        column.setPreferredWidth(32);
        column.setMaxWidth(32);
        column.setMinWidth(32);

        // path
        column = tableEmpties.getColumnModel().getColumn(1);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setMinWidth(32);
        column.setCellRenderer(cellRenderer);
        column.setResizable(true);
    }

    private void process()
    {
        try
        {
            final Repository repo = (isPublisher) ? guiContext.context.publisherRepo : guiContext.context.subscriberRepo;
            if (repo != null)
            {
                buttonDelete.setEnabled(false);
                buttonRun.setEnabled(false);
                //closeButton.setEnabled(false);
                empties = new ArrayList<Empty>();
                EmptiesTableModel etm = (EmptiesTableModel) tableEmpties.getModel();
                etm.setEmpties(empties);
                etm.fireTableDataChanged();

                logger.info(guiContext.cfg.gs("EmptyDirectoryFinder.running"));
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void >()
                {
                    int emptyCount = 0;
                    int totalDirectories = 0;
                    int totalItems = 0;

                    @Override
                    protected Void doInBackground() throws Exception
                    {
                        workerRunning = true;

                        // get content
                        if (isPublisher)
                        {
                            labelStatus.setText(guiContext.cfg.gs("Z.scanning"));
                            repo.scan();
                        }
                        else
                        {
                            if (guiContext.cfg.isRemoteSession())
                            {
                                if (!guiContext.context.clientStty.isConnected())
                                {
                                    Object[] opts = { guiContext.cfg.gs("Z.ok") };
                                    JOptionPane.showOptionDialog(thisDialog, guiContext.cfg.gs("Browser.connection.lost"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }

                                if (guiContext.context.transfer != null)
                                {
                                    labelStatus.setText(guiContext.cfg.gs("Z.requesting.collection.data.from.remote"));
                                    guiContext.context.transfer.requestCollection();
                                }
                                else
                                {
                                    Object[] opts = { guiContext.cfg.gs("Z.ok") };
                                    JOptionPane.showOptionDialog(thisDialog, guiContext.cfg.gs("Transfer.could.not.retrieve.remote.collection.file"),
                                            thisDialog.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                                            null, opts, opts[0]);
                                }
                            }
                            else
                            {
                                labelStatus.setText(guiContext.cfg.gs("Z.scanning"));
                                repo.scan();
                            }
                        }
                        final Repository repo = (isPublisher) ? guiContext.context.publisherRepo : guiContext.context.subscriberRepo;

                        // scan for empties
                        for (Library lib : repo.getLibraryData().libraries.bibliography)
                        {
                            if (requestStop)
                                break;
                            String msg = guiContext.cfg.gs("DuplicateFinder.analyzing.library") + "'" + lib.name + "'";
                            labelStatus.setText(msg);
                            for (Item item : lib.items)
                            {
                                if (item.isDirectory())
                                {
                                    ++totalDirectories;
                                    if (item.getSize() == 0)
                                    {
                                        ++emptyCount;
                                        Empty empty = new Empty(item.getFullPath());
                                        empties.add(empty);
                                    }
                                }
                                else
                                    ++totalItems;
                            }
                        }

                        labelStatus.setText("  " + java.text.MessageFormat.format(guiContext.cfg.gs("EmptyDirectoryFinder.empty.items.directories"),
                                emptyCount, totalItems, totalDirectories));

                        etm.fireTableDataChanged();
                        guiContext.mainFrame.labelStatusMiddle.setText(emptyCount + guiContext.cfg.gs("EmptyDirectoryFinder.empty.directories"));

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
                                    buttonDelete.setEnabled(true);
                                    buttonRun.setEnabled(true);
                                    //closeButton.setEnabled(true);
                                    EmptiesTableModel etm = (EmptiesTableModel) tableEmpties.getModel();
                                    etm.setEmpties(empties);
                                    etm.fireTableDataChanged();
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
                Object[] opts = { guiContext.cfg.gs("Z.ok") };
                JOptionPane.showOptionDialog(this, guiContext.cfg.gs("EmptyDirectoryFinder.collection.not.loaded"),
                        this.getTitle(), JOptionPane.PLAIN_MESSAGE, JOptionPane.WARNING_MESSAGE,
                        null, opts, opts[0]);
            }
        }
        catch (Exception ex)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(ex);
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, this.getTitle(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePreferences()
    {
        guiContext.preferences.setToolsEmptyDirectoryFinderHeight(this.getHeight());
        guiContext.preferences.setToolsEmptyDirectoryFinderWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setToolsEmptyDirectoryFinderXpos(location.x);
        guiContext.preferences.setToolsEmptyDirectoryFinderYpos(location.y);
    }

    private void windowClosing(WindowEvent e)
    {
        closeButton.doClick();
    }

    // ================================================================================================================

    public class Empty
    {
        boolean isSelected = false;
        String path = "";

        public Empty(String path)
        {
            this.path = path;
        }
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
        buttonRun = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonDelete = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        scrollPaneEmpties = new JScrollPane();
        tableEmpties = new JTable();
        panelOptionsButtons = new JPanel();
        buttonAll = new JButton();
        buttonNone = new JButton();
        panelBottom = new JPanel();
        labelStatus = new JLabel();
        buttonBar = new JPanel();
        closeButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(guiContext.cfg.gs("EmptyDirectoryFinder.this.title"));
        setMinimumSize(new Dimension(150, 126));
        setName("dialogEmptyDirectoryUI");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                EmptyDirectoryFinderUI.this.windowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
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

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonDelete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("EmptyDirectoryFinder.buttonDelete.mnemonic_2").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);
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
                        labelHelp.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.labelHelp.toolTipText"));
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

                //======== scrollPaneEmpties ========
                {

                    //---- tableEmpties ----
                    tableEmpties.setFillsViewportHeight(true);
                    tableEmpties.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    scrollPaneEmpties.setViewportView(tableEmpties);
                }
                contentPanel.add(scrollPaneEmpties, BorderLayout.CENTER);

                //======== panelOptionsButtons ========
                {
                    panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                    //---- buttonAll ----
                    buttonAll.setText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonAll.text"));
                    buttonAll.setFont(buttonAll.getFont().deriveFont(buttonAll.getFont().getSize() - 2f));
                    buttonAll.setPreferredSize(new Dimension(78, 24));
                    buttonAll.setMinimumSize(new Dimension(78, 24));
                    buttonAll.setMaximumSize(new Dimension(78, 24));
                    buttonAll.setMnemonic(guiContext.cfg.gs("EmptyDirectoryFinder.buttonAll.mnemonic").charAt(0));
                    buttonAll.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonAll.toolTipText"));
                    buttonAll.addActionListener(e -> actionAllClicked(e));
                    panelOptionsButtons.add(buttonAll);

                    //---- buttonNone ----
                    buttonNone.setText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonNone.text"));
                    buttonNone.setFont(buttonNone.getFont().deriveFont(buttonNone.getFont().getSize() - 2f));
                    buttonNone.setPreferredSize(new Dimension(78, 24));
                    buttonNone.setMinimumSize(new Dimension(78, 24));
                    buttonNone.setMaximumSize(new Dimension(78, 24));
                    buttonNone.setMnemonic(guiContext.cfg.gs("EmptyDirectoryFinder.buttonNone.mnemonic_2").charAt(0));
                    buttonNone.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.buttonNone.toolTipText"));
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

                    //---- closeButton ----
                    closeButton.setText(guiContext.cfg.gs("EmptyDirectoryFinder.closeButton.text"));
                    closeButton.setToolTipText(guiContext.cfg.gs("EmptyDirectoryFinder.closeButton.toolTipText"));
                    closeButton.addActionListener(e -> actionCloseClicked(e));
                    buttonBar.add(closeButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JButton buttonRun;
    private JPanel hSpacerBeforeRun;
    private JButton buttonDelete;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JScrollPane scrollPaneEmpties;
    private JTable tableEmpties;
    private JPanel panelOptionsButtons;
    private JButton buttonAll;
    private JButton buttonNone;
    private JPanel panelBottom;
    private JLabel labelStatus;
    private JPanel buttonBar;
    private JButton closeButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    //
    // @formatter:on
    // </editor-fold>
}
