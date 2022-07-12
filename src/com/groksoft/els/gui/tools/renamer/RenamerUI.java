package com.groksoft.els.gui.tools.renamer;

import javax.swing.table.*;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.renamer.RenamerTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;

public class RenamerUI extends JDialog
{
    private String[] cardNames = {"cardCaseChange", "cardInsert", "cardNumbering", "cardRemove", "cardReplace" };
    private RenamerConfigModel configModel;
    private ArrayList<RenamerTool> deletedTools;
    private String[] displayNames;
    private GuiContext guiContext;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private boolean isSubscriber;
    private SwingWorker<Void, Void> worker;
    private RenamerTool workerRenamer = null;
    private boolean workerRunning = false;

    private RenamerUI()
    {
        // hide default constructor
    }

    public RenamerUI(Window owner, GuiContext guiContext)
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
        if (guiContext.preferences.getToolsRenamerHeight() > 0)
        {
            this.setLocation(guiContext.preferences.getToolsRenamerXpos(), guiContext.preferences.getToolsRenamerYpos());
            Dimension dim = new Dimension(guiContext.preferences.getToolsRenamerWidth(), guiContext.preferences.getToolsRenamerHeight());
            this.setSize(dim);
            this.splitPaneContent.setDividerLocation(guiContext.preferences.getToolsRenamerDividerLocation());
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

        // setup the left-side list of configurations
        configModel = new RenamerConfigModel(guiContext, this);
        configModel.setColumnCount(1);
        configItems.setModel(configModel);
        configItems.getTableHeader().setUI(null);
        //
        ListSelectionModel lsm = configItems.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!listSelectionEvent.getValueIsAdjusting())
                {
                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    loadOptions(index);
                }
            }
        });

        configItems.setTableHeader(null);
        panelControls.setBorder(scrollPaneExamples.getBorder());

        displayNames = new String[6];
        displayNames[0] = guiContext.cfg.gs("RenameUI.type.combobox.case.change"); // 0
        displayNames[1] = guiContext.cfg.gs("RenameUI.type.combobox.insert");
        displayNames[2] = guiContext.cfg.gs("RenameUI.type.combobox.numbering");
        displayNames[3] = guiContext.cfg.gs("RenameUI.type.combobox.remove");
        displayNames[4] = guiContext.cfg.gs("RenameUI.type.combobox.replace"); // 5

        loadConfigurations();
        deletedTools = new ArrayList<RenamerTool>();

    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && workerRenamer != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Renamer.stop.running.renamer"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerRenamer.requestStop();
                guiContext.browser.printLog(java.text.MessageFormat.format(guiContext.cfg.gs("Renamer.config.cancelled"), workerRenamer.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.cancel.all.changes"),
                        guiContext.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                    setVisible(false);
            }
            else
                setVisible(false);
        }
    }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            RenamerTool origRenamer = (RenamerTool) configModel.getValueAt(index, 0);
            String rename = origRenamer.getConfigName() + guiContext.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                RenamerTool renamer = origRenamer.clone();
                renamer.setConfigName(rename);
                renamer.setDataHasChanged();
                //jrt.addJunkItem();
                configModel.addRow(new Object[]{renamer});

                // clear patterns table

                // set patterns table

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                        rename, guiContext.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            RenamerTool renamer = (RenamerTool) configModel.getValueAt(index, 0);

            // TODO check if Tool is used in any Jobs, prompt user accordingly AND handle for rename too

            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + renamer.getConfigName(),
                    guiContext.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                deletedTools.add(renamer);
                configModel.removeRow(index);
                configModel.fireTableDataChanged();
                if (index > 0)
                    index = configModel.getRowCount() - 1;
                configItems.requestFocus();
                if (index >= 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadOptions(index);
                }
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("Renamer.help"), "renamer_" + guiContext.preferences.getLocale() + ".html");
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
        saveConfigurations();
        savePreferences();
        setVisible(false);
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(guiContext.cfg.gs("Z.untitled"), null) == null)
        {
            JComboBox comboBoxRenameType = new JComboBox<>();
            comboBoxRenameType.setModel(new DefaultComboBoxModel<>(new String[] {
                    "Rename Type"
            }));

            // set Rename Type combobox
            String message = guiContext.cfg.gs("RenameUI.type.combobox.select.type");
            comboBoxRenameType.removeAllItems();
            comboBoxRenameType.addItem(guiContext.cfg.gs("RenameUI.type.combobox.case.change")); // 0
            comboBoxRenameType.addItem(guiContext.cfg.gs("RenameUI.type.combobox.insert"));
            comboBoxRenameType.addItem(guiContext.cfg.gs("RenameUI.type.combobox.numbering"));
            comboBoxRenameType.addItem(guiContext.cfg.gs("RenameUI.type.combobox.replace"));
            comboBoxRenameType.addItem(guiContext.cfg.gs("RenameUI.type.combobox.remove")); // 5

            Object[] params = {message, comboBoxRenameType};

            // confirm run of tool
            int opt = JOptionPane.showConfirmDialog(this, params, guiContext.cfg.gs("Renamer.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                RenamerTool renamer = new RenamerTool(guiContext, guiContext.cfg, guiContext.context);
                renamer.setConfigName(guiContext.cfg.gs("Z.untitled"));
                renamer.setDataHasChanged();

                renamer.setType(comboBoxRenameType.getSelectedIndex());
                ((CardLayout)panelOptionsCards.getLayout()).show(panelOptionsCards, cardNames[renamer.getType()]);
                labelRenameType.setText(displayNames[renamer.getType()]);

                configModel.addRow(new Object[]{renamer});

                if (configModel.getRowCount() > 0)
                {
                    buttonCopy.setEnabled(true);
                    buttonDelete.setEnabled(true);
                    buttonRun.setEnabled(true);
                    buttonRefresh.setEnabled(true);
                }

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
                configItems.requestFocus();
        }
        else
        {
            JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                    guiContext.cfg.gs("Z.untitled"), guiContext.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionRunClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            RenamerTool tool = (RenamerTool) configModel.getValueAt(index, 0);
            workerRenamer = tool.clone();
            processSelected(workerRenamer);
        }
    }

    private void actionRefreshClicked(ActionEvent e)
    {
    }

    public boolean checkForChanges()
    {
        if (deletedTools.size() > 0)
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((RenamerTool) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    private void configItemsMouseClicked(MouseEvent e)
    {
        JTable src = (JTable) e.getSource();
        if (e.getClickCount() == 1)
        {
            int index = src.getSelectedRow();
            loadOptions(index);
        }
    }

    public ArrayList<RenamerTool> getDeletedTools()
    {
        return deletedTools;
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    private void loadConfigurations()
    {
        try
        {
            Tools tools = new Tools();
            ArrayList<AbstractTool> toolList = tools.loadAllTools(guiContext, RenamerTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                RenamerTool renamer = (RenamerTool) tool;
                configModel.addRow(new Object[]{renamer});
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                guiContext.browser.printLog(msg, true);
                JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (configModel.getRowCount() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonRefresh.setEnabled(false);
        }
        else
        {
            loadOptions(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadOptions(int index)
    {
        RenamerTool renamer = null;
        if (index >= 0 && index < configModel.getRowCount())
        {
            renamer = (RenamerTool) configModel.getValueAt(index, 0);
        }
        else
        {
            // TODO empty Renamer
        }

        if ((index >= 0 && index < configModel.getRowCount()) && renamer != null)
        {
            ((CardLayout) panelOptionsCards.getLayout()).show(panelOptionsCards, cardNames[renamer.getType()]);
            labelRenameType.setText(displayNames[renamer.getType()]);
        }
    }

    public void processSelected(RenamerTool renamer)
    {
        if (renamer != null)
        {
            try
            {
                ArrayList<Origin> origins = new ArrayList<Origin>();
                isSubscriber = Origin.makeOriginsFromSelected(this, origins);

                if (origins != null && origins.size() > 0)
                {
                    int count = origins.size();

                    // make dialog pieces
                    String which = (isSubscriber) ? guiContext.cfg.gs("Z.subscriber") : guiContext.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(guiContext.cfg.gs("Renamer.run.on.N.locations"), renamer.getConfigName(), count, which);
                    JCheckBox checkbox = new JCheckBox(guiContext.cfg.gs("Navigator.dryrun"));
                    checkbox.setToolTipText(guiContext.cfg.gs("Navigator.dryrun.tooltip"));
                    Object[] params = {message, checkbox};

                    // confirm run of tool
                    int reply = JOptionPane.showConfirmDialog(this, params, guiContext.cfg.gs("Renamer.title"), JOptionPane.YES_NO_OPTION);
                    isDryRun = checkbox.isSelected();
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        try
                        {
                            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                            setComponentEnabled(false);
                            cancelButton.setEnabled(true);
                            cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                            labelHelp.setEnabled(true);
                            labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                            Task task = new Task(renamer.getInternalName(), renamer.getConfigName());
                            task.setOrigins(origins);

                            if (isSubscriber)
                                task.setSubscriberKey(Task.ANY_SERVER);
                            else
                                task.setPublisherKey(Task.ANY_SERVER);

                            worker = task.process(guiContext, renamer, isDryRun);
                            if (worker != null)
                            {
                                workerRunning = true;
                                worker.addPropertyChangeListener(new PropertyChangeListener()
                                {
                                    @Override
                                    public void propertyChange(PropertyChangeEvent e)
                                    {
                                        if (e.getPropertyName().equals("state"))
                                        {
                                            if (e.getNewValue() == SwingWorker.StateValue.DONE)
                                                processTerminated(renamer);
                                        }
                                    }
                                });
                            }
                        }
                        catch (Exception e)
                        {
                            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                            if (guiContext != null)
                            {
                                guiContext.browser.printLog(msg, true);
                                JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                            }
                            else
                                logger.error(msg);
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Renamer.nothing.selected.in.browser"),
                            guiContext.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (Exception e)
            {
                String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                if (guiContext != null)
                {
                    guiContext.browser.printLog(msg, true);
                    JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                }
                else
                    logger.error(msg);
            }
        }
    }

    private void processTerminated(RenamerTool renamer)
    {
        if (guiContext.progress != null)
            guiContext.progress.done();

        setComponentEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;
        workerRenamer = null;

        if (renamer.isRequestStop())
        {
            guiContext.browser.printLog(renamer.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
            guiContext.mainFrame.labelStatusMiddle.setText(renamer.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
        }
        else
        {
            guiContext.browser.printLog(renamer.getConfigName() + guiContext.cfg.gs("Z.completed"));
            guiContext.mainFrame.labelStatusMiddle.setText(renamer.getConfigName() + guiContext.cfg.gs("Z.completed"));
        }
    }

    private void saveConfigurations()
    {
        RenamerTool renamer = null;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                renamer = (RenamerTool) configModel.getValueAt(i, 0);
                if (renamer.isDataChanged())
                    renamer.write();
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                renamer = deletedTools.get(i);
                File file = new File(renamer.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                guiContext.browser.printLog(msg, true);
                JOptionPane.showMessageDialog(guiContext.navigator.dialogRenamer, msg, guiContext.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    private void savePreferences()
    {
        guiContext.preferences.setToolsRenamerHeight(this.getHeight());
        guiContext.preferences.setToolsRenamerWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setToolsRenamerXpos(location.x);
        guiContext.preferences.setToolsRenamerYpos(location.y);
        guiContext.preferences.setToolsRenamerDividerLocation(splitPaneContent.getDividerLocation());
    }

    public void setComponentEnabled(boolean enabled)
    {
        setComponentEnabled(enabled, getContentPane());
    }

    private void setComponentEnabled(boolean enabled, Component component)
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

    private void windowClosing(WindowEvent e)
    {
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
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        configItems = new JTable();
        panelOptions = new JPanel();
        panelControls = new JPanel();
        topOptions = new JPanel();
        vSpacer0 = new JPanel(null);
        panelRenameType = new JPanel();
        hSpacer1 = new JPanel(null);
        labelRenameType = new JLabel();
        panelFilenameSegment = new JPanel();
        comboBoxFilenameSegment = new JComboBox<>();
        hSpacer2 = new JPanel(null);
        panelCardBox = new JPanel();
        vSpacer1 = new JPanel(null);
        separator1 = new JSeparator();
        vSpacer2 = new JPanel(null);
        panelOptionsCards = new JPanel();
        panelCaseChangeCard = new JPanel();
        radioButtonFirstUpperCase = new JRadioButton();
        radioButtonLowerCase = new JRadioButton();
        radioButtonTitleCase = new JRadioButton();
        radioButtonUpperCase = new JRadioButton();
        panelInsertCard = new JPanel();
        labelTextToInsert = new JLabel();
        textFieldToInsert = new JTextField();
        labelInsertPosition = new JLabel();
        panelInsertPostion = new JPanel();
        textFieldInsertPosition = new JTextField();
        hSpacer3 = new JPanel(null);
        checkBoxInsertFromEnd = new JCheckBox();
        panelInsertOther = new JPanel();
        checkBoxInsertOverwrite = new JCheckBox();
        panelNumberingCard = new JPanel();
        labelStart = new JLabel();
        panelNums = new JPanel();
        textFieldStart = new JTextField();
        hSpacer5 = new JPanel(null);
        labelZeros = new JLabel();
        textFieldZeros = new JTextField();
        labelNumberingPosition = new JLabel();
        panelNumberingPostion = new JPanel();
        textFieldNumberingPosition = new JTextField();
        hSpacer4 = new JPanel(null);
        checkBoxNumberingFromEnd = new JCheckBox();
        panelNumberingOther = new JPanel();
        checkBoxNumberingOverwrite = new JCheckBox();
        panelRemoveCard = new JPanel();
        labelFrom = new JLabel();
        panelFrom = new JPanel();
        textFieldFrom = new JTextField();
        hSpacer6 = new JPanel(null);
        labelLength = new JLabel();
        panelLength = new JPanel();
        textFieldLength = new JTextField();
        panelRemoveOther = new JPanel();
        checkBoxRemoveFromEnd = new JCheckBox();
        panelReplaceCard = new JPanel();
        labelFind = new JLabel();
        textFieldFind = new JTextField();
        labeReplace = new JLabel();
        textFieldReplace = new JTextField();
        panelReplaceOther = new JPanel();
        checkBoxRegularExpr = new JCheckBox();
        checkBoxCase = new JCheckBox();
        scrollPaneExamples = new JScrollPane();
        tableExamples = new JTable();
        panelOptionsButtons = new JPanel();
        buttonRefresh = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("Renamer.title"));
        setName("renamerUI");
        setMinimumSize(new Dimension(150, 126));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                RenamerUI.this.windowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setMinimumSize(new Dimension(150, 80));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(570, 470));
                contentPanel.setMinimumSize(new Dimension(140, 120));
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

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("Renamer.button.New.text"));
                        buttonNew.setMnemonic(guiContext.cfg.gs("Renamer.button.New.mnemonic").charAt(0));
                        buttonNew.setToolTipText(guiContext.cfg.gs("Renamer.button.New.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("Renamer.button.Copy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("Renamer.button.Copy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("Renamer.button.Copy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("Renamer.button.Delete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("Renamer.button.Delete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("Renamer.button.Delete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("Renamer.button.Run.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("Renamer.button.Run.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("Renamer.button.Run.toolTipText"));
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
                        labelHelp.setToolTipText(guiContext.cfg.gs("Renamer.labelHelp.toolTipText"));
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
                    panelTop.add(panelHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);
                    splitPaneContent.setMinimumSize(new Dimension(140, 80));

                    //======== scrollPaneConfig ========
                    {
                        scrollPaneConfig.setMinimumSize(new Dimension(140, 16));
                        scrollPaneConfig.setPreferredSize(new Dimension(142, 146));

                        //---- configItems ----
                        configItems.setPreferredSize(new Dimension(128, 54));
                        configItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        configItems.setShowVerticalLines(false);
                        configItems.setFillsViewportHeight(true);
                        configItems.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                configItemsMouseClicked(e);
                            }
                        });
                        scrollPaneConfig.setViewportView(configItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneConfig);

                    //======== panelOptions ========
                    {
                        panelOptions.setMinimumSize(new Dimension(0, 78));
                        panelOptions.setLayout(new BorderLayout());

                        //======== panelControls ========
                        {
                            panelControls.setLayout(new BorderLayout());

                            //======== topOptions ========
                            {
                                topOptions.setLayout(new BorderLayout());

                                //---- vSpacer0 ----
                                vSpacer0.setPreferredSize(new Dimension(10, 2));
                                vSpacer0.setMinimumSize(new Dimension(10, 2));
                                vSpacer0.setMaximumSize(new Dimension(10, 2));
                                topOptions.add(vSpacer0, BorderLayout.NORTH);

                                //======== panelRenameType ========
                                {
                                    panelRenameType.setLayout(new BoxLayout(panelRenameType, BoxLayout.X_AXIS));

                                    //---- hSpacer1 ----
                                    hSpacer1.setPreferredSize(new Dimension(4, 10));
                                    hSpacer1.setMinimumSize(new Dimension(4, 12));
                                    hSpacer1.setMaximumSize(new Dimension(4, 32767));
                                    panelRenameType.add(hSpacer1);

                                    //---- labelRenameType ----
                                    labelRenameType.setText("Rename Type");
                                    labelRenameType.setMaximumSize(new Dimension(200, 16));
                                    labelRenameType.setFont(labelRenameType.getFont().deriveFont(labelRenameType.getFont().getSize() + 1f));
                                    panelRenameType.add(labelRenameType);
                                }
                                topOptions.add(panelRenameType, BorderLayout.CENTER);

                                //======== panelFilenameSegment ========
                                {
                                    panelFilenameSegment.setMaximumSize(new Dimension(144, 30));
                                    panelFilenameSegment.setLayout(new BoxLayout(panelFilenameSegment, BoxLayout.X_AXIS));

                                    //---- comboBoxFilenameSegment ----
                                    comboBoxFilenameSegment.setModel(new DefaultComboBoxModel<>(new String[] {
                                        "Name only",
                                        "Extension only",
                                        "Whole filename"
                                    }));
                                    panelFilenameSegment.add(comboBoxFilenameSegment);

                                    //---- hSpacer2 ----
                                    hSpacer2.setPreferredSize(new Dimension(4, 10));
                                    hSpacer2.setMinimumSize(new Dimension(4, 10));
                                    hSpacer2.setMaximumSize(new Dimension(4, 10));
                                    panelFilenameSegment.add(hSpacer2);
                                }
                                topOptions.add(panelFilenameSegment, BorderLayout.EAST);

                                //======== panelCardBox ========
                                {
                                    panelCardBox.setLayout(new BoxLayout(panelCardBox, BoxLayout.Y_AXIS));

                                    //---- vSpacer1 ----
                                    vSpacer1.setMinimumSize(new Dimension(12, 2));
                                    vSpacer1.setMaximumSize(new Dimension(32767, 2));
                                    vSpacer1.setPreferredSize(new Dimension(10, 2));
                                    panelCardBox.add(vSpacer1);
                                    panelCardBox.add(separator1);

                                    //---- vSpacer2 ----
                                    vSpacer2.setMinimumSize(new Dimension(12, 2));
                                    vSpacer2.setMaximumSize(new Dimension(32767, 2));
                                    vSpacer2.setPreferredSize(new Dimension(10, 2));
                                    panelCardBox.add(vSpacer2);

                                    //======== panelOptionsCards ========
                                    {
                                        panelOptionsCards.setMaximumSize(new Dimension(32676, 92));
                                        panelOptionsCards.setLayout(new CardLayout());

                                        //======== panelCaseChangeCard ========
                                        {
                                            panelCaseChangeCard.setPreferredSize(new Dimension(328, 92));
                                            panelCaseChangeCard.setMinimumSize(new Dimension(328, 92));
                                            panelCaseChangeCard.setMaximumSize(new Dimension(32767, 92));
                                            panelCaseChangeCard.setLayout(new GridLayout(2, 2));

                                            //---- radioButtonFirstUpperCase ----
                                            radioButtonFirstUpperCase.setText(guiContext.cfg.gs("Renamer.radioButtonFirstUpperCase.text"));
                                            radioButtonFirstUpperCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCaseChangeCard.add(radioButtonFirstUpperCase);

                                            //---- radioButtonLowerCase ----
                                            radioButtonLowerCase.setText(guiContext.cfg.gs("Renamer.radioButtonLowerCase.text"));
                                            radioButtonLowerCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCaseChangeCard.add(radioButtonLowerCase);

                                            //---- radioButtonTitleCase ----
                                            radioButtonTitleCase.setText(guiContext.cfg.gs("Renamer.radioButtonTitleCase.text"));
                                            radioButtonTitleCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCaseChangeCard.add(radioButtonTitleCase);

                                            //---- radioButtonUpperCase ----
                                            radioButtonUpperCase.setText(guiContext.cfg.gs("Renamer.radioButtonUpperCase.text"));
                                            radioButtonUpperCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelCaseChangeCard.add(radioButtonUpperCase);
                                        }
                                        panelOptionsCards.add(panelCaseChangeCard, "cardCaseChange");

                                        //======== panelInsertCard ========
                                        {
                                            panelInsertCard.setMaximumSize(new Dimension(32767, 92));
                                            panelInsertCard.setMinimumSize(new Dimension(328, 92));
                                            panelInsertCard.setPreferredSize(new Dimension(328, 92));
                                            panelInsertCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelInsertCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelInsertCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelTextToInsert ----
                                            labelTextToInsert.setText(guiContext.cfg.gs("Renamer.labelTextToInsert.text"));
                                            labelTextToInsert.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelInsertCard.add(labelTextToInsert, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldToInsert ----
                                            textFieldToInsert.setPreferredSize(new Dimension(320, 30));
                                            textFieldToInsert.setMinimumSize(new Dimension(50, 30));
                                            textFieldToInsert.setToolTipText(guiContext.cfg.gs("Renamer.textFieldToInsert.toolTipText"));
                                            panelInsertCard.add(textFieldToInsert, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelInsertPosition ----
                                            labelInsertPosition.setText(guiContext.cfg.gs("Renamer.labelInsertPosition.text"));
                                            labelInsertPosition.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelInsertCard.add(labelInsertPosition, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelInsertPostion ========
                                            {
                                                panelInsertPostion.setLayout(new BoxLayout(panelInsertPostion, BoxLayout.X_AXIS));

                                                //---- textFieldInsertPosition ----
                                                textFieldInsertPosition.setMinimumSize(new Dimension(96, 30));
                                                textFieldInsertPosition.setPreferredSize(new Dimension(96, 30));
                                                textFieldInsertPosition.setMaximumSize(new Dimension(96, 30));
                                                textFieldInsertPosition.setToolTipText(guiContext.cfg.gs("Renamer.textFieldInsertPosition.toolTipText"));
                                                panelInsertPostion.add(textFieldInsertPosition);

                                                //---- hSpacer3 ----
                                                hSpacer3.setMinimumSize(new Dimension(10, 10));
                                                hSpacer3.setMaximumSize(new Dimension(10, 10));
                                                panelInsertPostion.add(hSpacer3);

                                                //---- checkBoxInsertFromEnd ----
                                                checkBoxInsertFromEnd.setText(guiContext.cfg.gs("Renamer.checkBoxInsertFromEnd.text"));
                                                checkBoxInsertFromEnd.setToolTipText(guiContext.cfg.gs("Renamer.checkBoxInsertFromEnd.toolTipText"));
                                                panelInsertPostion.add(checkBoxInsertFromEnd);
                                            }
                                            panelInsertCard.add(panelInsertPostion, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelInsertOther ========
                                            {
                                                panelInsertOther.setPreferredSize(new Dimension(266, 30));
                                                panelInsertOther.setMinimumSize(new Dimension(266, 30));
                                                panelInsertOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxInsertOverwrite ----
                                                checkBoxInsertOverwrite.setText(guiContext.cfg.gs("Renamer.checkBoxInsertOverwrite.text"));
                                                checkBoxInsertOverwrite.setMargin(new Insets(2, 6, 2, 2));
                                                panelInsertOther.add(checkBoxInsertOverwrite);
                                            }
                                            panelInsertCard.add(panelInsertOther, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOptionsCards.add(panelInsertCard, "cardInsert");

                                        //======== panelNumberingCard ========
                                        {
                                            panelNumberingCard.setMaximumSize(new Dimension(32767, 92));
                                            panelNumberingCard.setMinimumSize(new Dimension(328, 92));
                                            panelNumberingCard.setPreferredSize(new Dimension(328, 92));
                                            panelNumberingCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelNumberingCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelNumberingCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelStart ----
                                            labelStart.setText(guiContext.cfg.gs("Renamer.labelStart.text"));
                                            labelStart.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelNumberingCard.add(labelStart, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelNums ========
                                            {
                                                panelNums.setLayout(new BoxLayout(panelNums, BoxLayout.X_AXIS));

                                                //---- textFieldStart ----
                                                textFieldStart.setPreferredSize(new Dimension(96, 30));
                                                textFieldStart.setMinimumSize(new Dimension(96, 30));
                                                textFieldStart.setMaximumSize(new Dimension(96, 30));
                                                textFieldStart.setText("1");
                                                textFieldStart.setToolTipText(guiContext.cfg.gs("Renamer.textFieldStart.toolTipText"));
                                                panelNums.add(textFieldStart);

                                                //---- hSpacer5 ----
                                                hSpacer5.setMinimumSize(new Dimension(10, 10));
                                                hSpacer5.setMaximumSize(new Dimension(32767, 10));
                                                hSpacer5.setPreferredSize(new Dimension(88, 10));
                                                panelNums.add(hSpacer5);

                                                //---- labelZeros ----
                                                labelZeros.setText(guiContext.cfg.gs("Renamer.labelZeros.text"));
                                                panelNums.add(labelZeros);

                                                //---- textFieldZeros ----
                                                textFieldZeros.setPreferredSize(new Dimension(96, 30));
                                                textFieldZeros.setMinimumSize(new Dimension(96, 30));
                                                textFieldZeros.setMaximumSize(new Dimension(96, 30));
                                                textFieldZeros.setText("1");
                                                textFieldZeros.setToolTipText(guiContext.cfg.gs("Renamer.textFieldZeros.toolTipText"));
                                                panelNums.add(textFieldZeros);
                                            }
                                            panelNumberingCard.add(panelNums, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelNumberingPosition ----
                                            labelNumberingPosition.setText(guiContext.cfg.gs("Renamer.labelNumberingPosition.text_2"));
                                            labelNumberingPosition.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelNumberingCard.add(labelNumberingPosition, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelNumberingPostion ========
                                            {
                                                panelNumberingPostion.setLayout(new BoxLayout(panelNumberingPostion, BoxLayout.X_AXIS));

                                                //---- textFieldNumberingPosition ----
                                                textFieldNumberingPosition.setMinimumSize(new Dimension(96, 30));
                                                textFieldNumberingPosition.setPreferredSize(new Dimension(96, 30));
                                                textFieldNumberingPosition.setMaximumSize(new Dimension(96, 30));
                                                panelNumberingPostion.add(textFieldNumberingPosition);

                                                //---- hSpacer4 ----
                                                hSpacer4.setMinimumSize(new Dimension(10, 10));
                                                hSpacer4.setMaximumSize(new Dimension(10, 10));
                                                panelNumberingPostion.add(hSpacer4);

                                                //---- checkBoxNumberingFromEnd ----
                                                checkBoxNumberingFromEnd.setText(guiContext.cfg.gs("Renamer.checkBoxNumberingFromEnd.text_2"));
                                                panelNumberingPostion.add(checkBoxNumberingFromEnd);
                                            }
                                            panelNumberingCard.add(panelNumberingPostion, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelNumberingOther ========
                                            {
                                                panelNumberingOther.setPreferredSize(new Dimension(266, 30));
                                                panelNumberingOther.setMinimumSize(new Dimension(266, 30));
                                                panelNumberingOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxNumberingOverwrite ----
                                                checkBoxNumberingOverwrite.setText(guiContext.cfg.gs("Renamer.checkBoxNumberingOverwrite.text_2"));
                                                checkBoxNumberingOverwrite.setMargin(new Insets(2, 6, 2, 2));
                                                panelNumberingOther.add(checkBoxNumberingOverwrite);
                                            }
                                            panelNumberingCard.add(panelNumberingOther, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOptionsCards.add(panelNumberingCard, "cardNumbering");

                                        //======== panelRemoveCard ========
                                        {
                                            panelRemoveCard.setMaximumSize(new Dimension(32767, 92));
                                            panelRemoveCard.setMinimumSize(new Dimension(328, 92));
                                            panelRemoveCard.setPreferredSize(new Dimension(328, 92));
                                            panelRemoveCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelRemoveCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelRemoveCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelFrom ----
                                            labelFrom.setText(guiContext.cfg.gs("Renamer.labelFrom.text"));
                                            labelFrom.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelRemoveCard.add(labelFrom, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelFrom ========
                                            {
                                                panelFrom.setLayout(new BoxLayout(panelFrom, BoxLayout.X_AXIS));

                                                //---- textFieldFrom ----
                                                textFieldFrom.setPreferredSize(new Dimension(96, 30));
                                                textFieldFrom.setMinimumSize(new Dimension(96, 30));
                                                textFieldFrom.setMaximumSize(new Dimension(96, 30));
                                                textFieldFrom.setText("1");
                                                panelFrom.add(textFieldFrom);

                                                //---- hSpacer6 ----
                                                hSpacer6.setMinimumSize(new Dimension(10, 10));
                                                hSpacer6.setMaximumSize(new Dimension(32767, 10));
                                                hSpacer6.setPreferredSize(new Dimension(217, 10));
                                                hSpacer6.setFocusable(false);
                                                panelFrom.add(hSpacer6);
                                            }
                                            panelRemoveCard.add(panelFrom, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelLength ----
                                            labelLength.setText(guiContext.cfg.gs("Renamer.labelLength.text"));
                                            labelLength.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelRemoveCard.add(labelLength, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelLength ========
                                            {
                                                panelLength.setLayout(new BoxLayout(panelLength, BoxLayout.X_AXIS));

                                                //---- textFieldLength ----
                                                textFieldLength.setMinimumSize(new Dimension(96, 30));
                                                textFieldLength.setPreferredSize(new Dimension(96, 30));
                                                textFieldLength.setMaximumSize(new Dimension(96, 30));
                                                panelLength.add(textFieldLength);
                                            }
                                            panelRemoveCard.add(panelLength, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelRemoveOther ========
                                            {
                                                panelRemoveOther.setPreferredSize(new Dimension(98, 30));
                                                panelRemoveOther.setMinimumSize(new Dimension(98, 30));
                                                panelRemoveOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxRemoveFromEnd ----
                                                checkBoxRemoveFromEnd.setText(guiContext.cfg.gs("Renamer.checkBoxRemoveFromEnd.text"));
                                                checkBoxRemoveFromEnd.setMargin(new Insets(2, 6, 2, 6));
                                                checkBoxRemoveFromEnd.setPreferredSize(new Dimension(98, 21));
                                                checkBoxRemoveFromEnd.setMinimumSize(new Dimension(98, 21));
                                                checkBoxRemoveFromEnd.setMaximumSize(new Dimension(98, 21));
                                                panelRemoveOther.add(checkBoxRemoveFromEnd);
                                            }
                                            panelRemoveCard.add(panelRemoveOther, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOptionsCards.add(panelRemoveCard, "cardRemove");

                                        //======== panelReplaceCard ========
                                        {
                                            panelReplaceCard.setMaximumSize(new Dimension(32767, 92));
                                            panelReplaceCard.setMinimumSize(new Dimension(328, 92));
                                            panelReplaceCard.setPreferredSize(new Dimension(328, 92));
                                            panelReplaceCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelReplaceCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelReplaceCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelFind ----
                                            labelFind.setText(guiContext.cfg.gs("Renamer.labelFind.text"));
                                            labelFind.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelReplaceCard.add(labelFind, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldFind ----
                                            textFieldFind.setPreferredSize(new Dimension(320, 30));
                                            textFieldFind.setMinimumSize(new Dimension(50, 30));
                                            panelReplaceCard.add(textFieldFind, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labeReplace ----
                                            labeReplace.setText(guiContext.cfg.gs("Renamer.labeReplace.text"));
                                            labeReplace.setHorizontalAlignment(SwingConstants.TRAILING);
                                            panelReplaceCard.add(labeReplace, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldReplace ----
                                            textFieldReplace.setPreferredSize(new Dimension(320, 30));
                                            textFieldReplace.setMinimumSize(new Dimension(50, 30));
                                            panelReplaceCard.add(textFieldReplace, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelReplaceOther ========
                                            {
                                                panelReplaceOther.setPreferredSize(new Dimension(266, 30));
                                                panelReplaceOther.setMinimumSize(new Dimension(266, 30));
                                                panelReplaceOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxRegularExpr ----
                                                checkBoxRegularExpr.setText(guiContext.cfg.gs("Renamer.checkBoxRegularExpr.text"));
                                                checkBoxRegularExpr.setMargin(new Insets(2, 6, 2, 6));
                                                panelReplaceOther.add(checkBoxRegularExpr);

                                                //---- checkBoxCase ----
                                                checkBoxCase.setText(guiContext.cfg.gs("Renamer.checkBoxCase.text"));
                                                checkBoxCase.setMargin(new Insets(2, 6, 2, 2));
                                                panelReplaceOther.add(checkBoxCase);
                                            }
                                            panelReplaceCard.add(panelReplaceOther, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelOptionsCards.add(panelReplaceCard, "cardReplace");
                                    }
                                    panelCardBox.add(panelOptionsCards);
                                }
                                topOptions.add(panelCardBox, BorderLayout.SOUTH);
                            }
                            panelControls.add(topOptions, BorderLayout.NORTH);
                        }
                        panelOptions.add(panelControls, BorderLayout.NORTH);

                        //======== scrollPaneExamples ========
                        {

                            //---- tableExamples ----
                            tableExamples.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            tableExamples.setRowSelectionAllowed(false);
                            tableExamples.setModel(new DefaultTableModel(
                                new Object[][] {
                                    {null, null},
                                    {null, null},
                                },
                                new String[] {
                                    "Browser Selections", "New Name"
                                }
                            ));
                            tableExamples.setFillsViewportHeight(true);
                            scrollPaneExamples.setViewportView(tableExamples);
                        }
                        panelOptions.add(scrollPaneExamples, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonRefresh ----
                            buttonRefresh.setText(guiContext.cfg.gs("Z.refresh"));
                            buttonRefresh.setFont(buttonRefresh.getFont().deriveFont(buttonRefresh.getFont().getSize() - 2f));
                            buttonRefresh.setPreferredSize(new Dimension(78, 24));
                            buttonRefresh.setMinimumSize(new Dimension(78, 24));
                            buttonRefresh.setMaximumSize(new Dimension(78, 24));
                            buttonRefresh.setMnemonic(guiContext.cfg.gs("Renamer.buttonRefresh.mnemonic").charAt(0));
                            buttonRefresh.setToolTipText(guiContext.cfg.gs("Z.refresh.tooltip.text"));
                            buttonRefresh.addActionListener(e -> actionRefreshClicked(e));
                            panelOptionsButtons.add(buttonRefresh);
                        }
                        panelOptions.add(panelOptionsButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelOptions);
                }
                contentPanel.add(splitPaneContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 82, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("Z.ok"));
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("Z.cancel"));
                cancelButton.addActionListener(e -> actionCancelClicked(e));
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroupChangeCase ----
        ButtonGroup buttonGroupChangeCase = new ButtonGroup();
        buttonGroupChangeCase.add(radioButtonFirstUpperCase);
        buttonGroupChangeCase.add(radioButtonLowerCase);
        buttonGroupChangeCase.add(radioButtonTitleCase);
        buttonGroupChangeCase.add(radioButtonUpperCase);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JButton buttonNew;
    private JButton buttonCopy;
    private JButton buttonDelete;
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneConfig;
    private JTable configItems;
    private JPanel panelOptions;
    private JPanel panelControls;
    private JPanel topOptions;
    private JPanel vSpacer0;
    private JPanel panelRenameType;
    private JPanel hSpacer1;
    private JLabel labelRenameType;
    private JPanel panelFilenameSegment;
    private JComboBox<String> comboBoxFilenameSegment;
    private JPanel hSpacer2;
    private JPanel panelCardBox;
    private JPanel vSpacer1;
    private JSeparator separator1;
    private JPanel vSpacer2;
    private JPanel panelOptionsCards;
    private JPanel panelCaseChangeCard;
    private JRadioButton radioButtonFirstUpperCase;
    private JRadioButton radioButtonLowerCase;
    private JRadioButton radioButtonTitleCase;
    private JRadioButton radioButtonUpperCase;
    private JPanel panelInsertCard;
    private JLabel labelTextToInsert;
    private JTextField textFieldToInsert;
    private JLabel labelInsertPosition;
    private JPanel panelInsertPostion;
    private JTextField textFieldInsertPosition;
    private JPanel hSpacer3;
    private JCheckBox checkBoxInsertFromEnd;
    private JPanel panelInsertOther;
    private JCheckBox checkBoxInsertOverwrite;
    private JPanel panelNumberingCard;
    private JLabel labelStart;
    private JPanel panelNums;
    private JTextField textFieldStart;
    private JPanel hSpacer5;
    private JLabel labelZeros;
    private JTextField textFieldZeros;
    private JLabel labelNumberingPosition;
    private JPanel panelNumberingPostion;
    private JTextField textFieldNumberingPosition;
    private JPanel hSpacer4;
    private JCheckBox checkBoxNumberingFromEnd;
    private JPanel panelNumberingOther;
    private JCheckBox checkBoxNumberingOverwrite;
    private JPanel panelRemoveCard;
    private JLabel labelFrom;
    private JPanel panelFrom;
    private JTextField textFieldFrom;
    private JPanel hSpacer6;
    private JLabel labelLength;
    private JPanel panelLength;
    private JTextField textFieldLength;
    private JPanel panelRemoveOther;
    private JCheckBox checkBoxRemoveFromEnd;
    private JPanel panelReplaceCard;
    private JLabel labelFind;
    private JTextField textFieldFind;
    private JLabel labeReplace;
    private JTextField textFieldReplace;
    private JPanel panelReplaceOther;
    private JCheckBox checkBoxRegularExpr;
    private JCheckBox checkBoxCase;
    private JScrollPane scrollPaneExamples;
    private JTable tableExamples;
    private JPanel panelOptionsButtons;
    private JButton buttonRefresh;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
