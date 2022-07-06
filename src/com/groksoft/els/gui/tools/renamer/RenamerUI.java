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
    private String[] cardNames = {"cardCaseChange", "cardInsert", "cardNumbering", "cardReplace", "cardRemove" };
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

    /*
     * IDEA:
     *  - Types of renames
     *     + Case change
     *        + First letter uppercase
     *        + Lower case
     *        + Title case
     *        + Upper case
     *     + Insert
     *        + 0, 0 Text: (label)
     *        + 0, 1 Text field
     *        + 1, 0 Position (label)
     *        + 1, 1 Box container X
     *           - Position field
     *           - From end checkbox
     *        + 2, 0 Before .extension checkbox
     *        + 2, 1 Overwrite
     *     + Numbering
     *     + Replace
     *     + Remove
     */

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
        displayNames[3] = guiContext.cfg.gs("RenameUI.type.combobox.replace");
        displayNames[4] = guiContext.cfg.gs("RenameUI.type.combobox.remove"); // 5

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
                ((CardLayout)panelTypeOptions.getLayout()).show(panelTypeOptions, cardNames[renamer.getType()]);
                labelRenameType.setText(displayNames[renamer.getType()]);

                configModel.addRow(new Object[]{renamer});

                if (configModel.getRowCount() > 0)
                {
                    buttonCopy.setEnabled(true);
                    buttonDelete.setEnabled(true);
                    buttonRun.setEnabled(true);
                    buttonUpdate.setEnabled(true);
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

    private void actionUpdateClicked(ActionEvent e)
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
            ArrayList<AbstractTool> toolList = tools.loadAllTools(guiContext, guiContext.cfg, guiContext.context, RenamerTool.INTERNAL_NAME);
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
            buttonUpdate.setEnabled(false);
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
            ((CardLayout) panelTypeOptions.getLayout()).show(panelTypeOptions, cardNames[renamer.getType()]);
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
        topControls = new JPanel();
        vSpacerOptions = new JPanel(null);
        panelRenameType = new JPanel();
        hSpacer1 = new JPanel(null);
        labelRenameType = new JLabel();
        panelFilenameSegment = new JPanel();
        comboBoxFilenameSegment = new JComboBox<>();
        hSpacer2 = new JPanel(null);
        panelTypeOptionsBox = new JPanel();
        vSpacer1 = new JPanel(null);
        separatorTypeOptions = new JSeparator();
        vSpacer2 = new JPanel(null);
        panelTypeOptions = new JPanel();
        panelCaseChangeCard = new JPanel();
        radioButtonFirstUpperCase = new JRadioButton();
        radioButtonLowerCase = new JRadioButton();
        radioButtonTitleCase = new JRadioButton();
        radioButtonUpperCase = new JRadioButton();
        panelInsert = new JPanel();
        labelTextToInsert = new JLabel();
        textFieldToInsert = new JTextField();
        labelPosition = new JLabel();
        panelRenamePostion = new JPanel();
        textFieldPosition = new JTextField();
        hSpacer3 = new JPanel(null);
        checkBoxFromEnd = new JCheckBox();
        panelRenameOther = new JPanel();
        checkBoxBeforeExtension = new JCheckBox();
        checkBoxOverwrite = new JCheckBox();
        scrollPaneExamples = new JScrollPane();
        tableExamples = new JTable();
        panelOptionsButtons = new JPanel();
        buttonUpdate = new JButton();
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

                            //======== topControls ========
                            {
                                topControls.setLayout(new BorderLayout());

                                //---- vSpacerOptions ----
                                vSpacerOptions.setPreferredSize(new Dimension(10, 2));
                                vSpacerOptions.setMinimumSize(new Dimension(10, 2));
                                vSpacerOptions.setMaximumSize(new Dimension(10, 2));
                                topControls.add(vSpacerOptions, BorderLayout.NORTH);

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
                                topControls.add(panelRenameType, BorderLayout.CENTER);

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
                                topControls.add(panelFilenameSegment, BorderLayout.EAST);

                                //======== panelTypeOptionsBox ========
                                {
                                    panelTypeOptionsBox.setLayout(new BoxLayout(panelTypeOptionsBox, BoxLayout.Y_AXIS));

                                    //---- vSpacer1 ----
                                    vSpacer1.setMinimumSize(new Dimension(12, 2));
                                    vSpacer1.setMaximumSize(new Dimension(32767, 2));
                                    vSpacer1.setPreferredSize(new Dimension(10, 2));
                                    panelTypeOptionsBox.add(vSpacer1);
                                    panelTypeOptionsBox.add(separatorTypeOptions);

                                    //---- vSpacer2 ----
                                    vSpacer2.setMinimumSize(new Dimension(12, 2));
                                    vSpacer2.setMaximumSize(new Dimension(32767, 2));
                                    vSpacer2.setPreferredSize(new Dimension(10, 2));
                                    panelTypeOptionsBox.add(vSpacer2);

                                    //======== panelTypeOptions ========
                                    {
                                        panelTypeOptions.setLayout(new CardLayout());

                                        //======== panelCaseChangeCard ========
                                        {
                                            panelCaseChangeCard.setPreferredSize(new Dimension(391, 92));
                                            panelCaseChangeCard.setMinimumSize(new Dimension(391, 92));
                                            panelCaseChangeCard.setMaximumSize(new Dimension(32767, 92));
                                            panelCaseChangeCard.setLayout(new GridLayout());

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
                                        panelTypeOptions.add(panelCaseChangeCard, "cardCaseChange");

                                        //======== panelInsert ========
                                        {
                                            panelInsert.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelInsert.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelInsert.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelTextToInsert ----
                                            labelTextToInsert.setText(guiContext.cfg.gs("Renamer.labelTextToInsert.text"));
                                            labelTextToInsert.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelInsert.add(labelTextToInsert, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldToInsert ----
                                            textFieldToInsert.setPreferredSize(new Dimension(320, 30));
                                            textFieldToInsert.setMinimumSize(new Dimension(50, 30));
                                            panelInsert.add(textFieldToInsert, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelPosition ----
                                            labelPosition.setText(guiContext.cfg.gs("Renamer.labelPosition.text"));
                                            labelPosition.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelInsert.add(labelPosition, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelRenamePostion ========
                                            {
                                                panelRenamePostion.setLayout(new BoxLayout(panelRenamePostion, BoxLayout.X_AXIS));

                                                //---- textFieldPosition ----
                                                textFieldPosition.setMinimumSize(new Dimension(64, 30));
                                                textFieldPosition.setPreferredSize(new Dimension(64, 30));
                                                textFieldPosition.setMaximumSize(new Dimension(64, 30));
                                                panelRenamePostion.add(textFieldPosition);

                                                //---- hSpacer3 ----
                                                hSpacer3.setPreferredSize(new Dimension(4, 10));
                                                hSpacer3.setMinimumSize(new Dimension(4, 10));
                                                hSpacer3.setMaximumSize(new Dimension(4, 10));
                                                panelRenamePostion.add(hSpacer3);

                                                //---- checkBoxFromEnd ----
                                                checkBoxFromEnd.setText(guiContext.cfg.gs("Renamer.checkBoxFromEnd.text"));
                                                panelRenamePostion.add(checkBoxFromEnd);
                                            }
                                            panelInsert.add(panelRenamePostion, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelRenameOther ========
                                            {
                                                panelRenameOther.setPreferredSize(new Dimension(266, 30));
                                                panelRenameOther.setMinimumSize(new Dimension(266, 30));
                                                panelRenameOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxBeforeExtension ----
                                                checkBoxBeforeExtension.setText(guiContext.cfg.gs("Renamer.checkBoxBeforeExtension.text"));
                                                checkBoxBeforeExtension.setHorizontalAlignment(SwingConstants.CENTER);
                                                checkBoxBeforeExtension.setMargin(new Insets(2, 2, 2, 6));
                                                panelRenameOther.add(checkBoxBeforeExtension);

                                                //---- checkBoxOverwrite ----
                                                checkBoxOverwrite.setText(guiContext.cfg.gs("Renamer.checkBoxOverwrite.text"));
                                                checkBoxOverwrite.setMargin(new Insets(2, 6, 2, 2));
                                                panelRenameOther.add(checkBoxOverwrite);
                                            }
                                            panelInsert.add(panelRenameOther, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));
                                        }
                                        panelTypeOptions.add(panelInsert, "cardInsert");
                                    }
                                    panelTypeOptionsBox.add(panelTypeOptions);
                                }
                                topControls.add(panelTypeOptionsBox, BorderLayout.SOUTH);
                            }
                            panelControls.add(topControls, BorderLayout.NORTH);
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
                                    "Selection(s) from Browser:", "Examples of changes:"
                                }
                            ));
                            tableExamples.setFillsViewportHeight(true);
                            scrollPaneExamples.setViewportView(tableExamples);
                        }
                        panelOptions.add(scrollPaneExamples, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonUpdate ----
                            buttonUpdate.setText(guiContext.cfg.gs("Z.update"));
                            buttonUpdate.setFont(buttonUpdate.getFont().deriveFont(buttonUpdate.getFont().getSize() - 2f));
                            buttonUpdate.setPreferredSize(new Dimension(78, 24));
                            buttonUpdate.setMinimumSize(new Dimension(78, 24));
                            buttonUpdate.setMaximumSize(new Dimension(78, 24));
                            buttonUpdate.setMnemonic(guiContext.cfg.gs("Renamer.button.AddRow.mnemonic").charAt(0));
                            buttonUpdate.setToolTipText(guiContext.cfg.gs("Z.update.tooltip.text"));
                            buttonUpdate.addActionListener(e -> actionUpdateClicked(e));
                            panelOptionsButtons.add(buttonUpdate);
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
    private JPanel topControls;
    private JPanel vSpacerOptions;
    private JPanel panelRenameType;
    private JPanel hSpacer1;
    private JLabel labelRenameType;
    private JPanel panelFilenameSegment;
    private JComboBox<String> comboBoxFilenameSegment;
    private JPanel hSpacer2;
    private JPanel panelTypeOptionsBox;
    private JPanel vSpacer1;
    private JSeparator separatorTypeOptions;
    private JPanel vSpacer2;
    private JPanel panelTypeOptions;
    private JPanel panelCaseChangeCard;
    private JRadioButton radioButtonFirstUpperCase;
    private JRadioButton radioButtonLowerCase;
    private JRadioButton radioButtonTitleCase;
    private JRadioButton radioButtonUpperCase;
    private JPanel panelInsert;
    private JLabel labelTextToInsert;
    private JTextField textFieldToInsert;
    private JLabel labelPosition;
    private JPanel panelRenamePostion;
    private JTextField textFieldPosition;
    private JPanel hSpacer3;
    private JCheckBox checkBoxFromEnd;
    private JPanel panelRenameOther;
    private JCheckBox checkBoxBeforeExtension;
    private JCheckBox checkBoxOverwrite;
    private JScrollPane scrollPaneExamples;
    private JTable tableExamples;
    private JPanel panelOptionsButtons;
    private JButton buttonUpdate;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
