package com.groksoft.els.gui.tools.renamer;

import javax.swing.table.*;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.browser.NavTreeNode;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.gui.jobs.AbstractToolDialog;
import com.groksoft.els.gui.jobs.ConfigModel;
import com.groksoft.els.gui.util.NumberFilter;
import com.groksoft.els.gui.util.PathFilter;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Origins;
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
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings(value = "unchecked")

public class RenamerUI extends AbstractToolDialog
{
    private String[] cardNames = {"cardCaseChange", "cardInsert", "cardNumbering", "cardRemove", "cardReplace"};
    private String caseChangeActions = "firstupper lower titlecase upper";
    private ChangesTableModel changeModel;
    private String[][] changeStrings;
    private ConfigModel configModel;
    private Context context;
    private JPanel currentCard = null;
    private int currentConfigIndex = -1;
    private RenamerTool currentRenamer = null;
    private String[] displayNames;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private boolean isSubscriber;
    private boolean loading = false;
    private NumberFilter numberFilter;
    private PathFilter pathFilter;
    private SwingWorker<Void, Void> worker;
    private RenamerTool workerRenamer = null;
    private boolean workerRunning = false;

    public RenamerUI(Window owner, Context context)
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
        if (context.preferences.getToolsRenamerXpos() > 0)
        {
            this.setLocation(context.preferences.getToolsRenamerXpos(), context.preferences.getToolsRenamerYpos());
            Dimension dim = new Dimension(context.preferences.getToolsRenamerWidth(), context.preferences.getToolsRenamerHeight());
            this.setSize(dim);
            this.splitPaneContent.setDividerLocation(context.preferences.getToolsRenamerDividerLocation());
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

        // setup the left-side list of configurations
        configModel = new ConfigModel(context, this);
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
                    if (index != currentConfigIndex && currentConfigIndex >= 0)
                    {
                        currentConfigIndex = index;
                        loadOptions(currentConfigIndex);
                        updateState();
                        processTable();
                    }
                }
            }
        });

        configItems.setTableHeader(null);
        panelControls.setBorder(scrollPaneExamples.getBorder());

        // get display names once
        displayNames = new String[6];
        displayNames[0] = context.cfg.gs("RenameUI.type.combobox.0.case.change"); // 0
        displayNames[1] = context.cfg.gs("RenameUI.type.combobox.1.insert");
        displayNames[2] = context.cfg.gs("RenameUI.type.combobox.2.numbering");
        displayNames[3] = context.cfg.gs("RenameUI.type.combobox.3.remove");
        displayNames[4] = context.cfg.gs("RenameUI.type.combobox.4.replace"); // 4

        addHandlers();

        loadTable();
        loadConfigurations();
        numberFilter = new NumberFilter();
        pathFilter = new PathFilter();
        context.navigator.enableDisableToolMenus(this, false);
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && workerRenamer != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Renamer.stop.running.renamer"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerRenamer.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Renamer.config.cancelled"), workerRenamer.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.cancel.all.changes"),
                        context.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
                if (reply == JOptionPane.YES_OPTION)
                {
                    cancelChanges();
                    setVisible(false);
                }
            }
            else
                setVisible(false);
        }
    }

    private void actionCaseChangeClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null && currentRenamer != null && changeStrings != null)
        {
            if (caseChangeActions.contains(e.getActionCommand()))
            {
                setRenamerOptions(currentRenamer);
                processTable();
            }
        }
    }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            RenamerTool origRenamer = (RenamerTool) configModel.getValueAt(index, 0);
            String rename = origRenamer.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                RenamerTool renamer = origRenamer.clone();
                renamer.setConfigName(rename);
                renamer.setDataHasChanged();
                configModel.addRow(new Object[]{renamer});

                currentConfigIndex = configModel.getRowCount() - 1;
                loadOptions(currentConfigIndex);
                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, context.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            RenamerTool tool = (RenamerTool) configModel.getValueAt(index, 0);

            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + tool.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                int answer = configModel.checkJobConflicts(tool.getConfigName(), null, tool.getInternalName(), false);
                if (answer >= 0)
                {
                    // add to delete list if file exists
                    File file = new File(tool.getFullPath());
                    if (file.exists())
                    {
                        deletedTools.add(tool);
                    }

                    configModel.removeRow(index);
                    if (index > configModel.getRowCount() - 1)
                        index = configModel.getRowCount() - 1;
                    currentConfigIndex = index;
                    configModel.fireTableDataChanged();
                    if (index >= 0)
                    {
                        configItems.changeSelection(index, 0, false, false);
                        loadOptions(index);
                    }
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionFilenameSegmentClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("comboBoxChanged"))
            {
                if (currentRenamer != null)
                {
                    if (currentRenamer.getSegment() != comboBoxFilenameSegment.getSelectedIndex())
                    {
                        setRenamerOptions(currentRenamer);
                        processTable();
                    }
                }
            }
        }
    }

    private void actionFilesOnlyClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("filesOnlyChanged"))
            {
                if (currentRenamer != null)
                {
                    if (currentRenamer.isFilesOnly() != checkBoxFilesOnly.isSelected())
                    {
                        setRenamerOptions(currentRenamer);
                        processTable();
                    }
                }
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("Renamer.help"), "renamer_" + context.preferences.getLocale() + ".html");
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

    private void actionSaveClicked(ActionEvent e)
    {
        saveConfigurations();
        savePreferences();
        setVisible(false);
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            JComboBox comboBoxRenameType = new JComboBox<>();
            comboBoxRenameType.setModel(new DefaultComboBoxModel<>(new String[]{
            }));

            // set Rename Type combobox
            String message = context.cfg.gs("RenameUI.type.combobox.select.type");
            comboBoxRenameType.removeAllItems();
            comboBoxRenameType.addItem(displayNames[0]);
            comboBoxRenameType.addItem(displayNames[1]);
            comboBoxRenameType.addItem(displayNames[2]);
            comboBoxRenameType.addItem(displayNames[3]);
            comboBoxRenameType.addItem(displayNames[4]);

            Object[] params = {message, comboBoxRenameType};

            // get renamer type
            int opt = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("Renamer.title"), JOptionPane.OK_CANCEL_OPTION);
            if (opt == JOptionPane.YES_OPTION)
            {
                RenamerTool renamer = new RenamerTool(context);
                renamer.setConfigName(context.cfg.gs("Z.untitled"));
                renamer.setType(comboBoxRenameType.getSelectedIndex());
                currentRenamer = renamer;
                initNewCard(renamer.getType());

                labelRenameType.setText(displayNames[renamer.getType()]);
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
                buttonRun.setEnabled(true);
                buttonRefresh.setEnabled(true);

                configModel.addRow(new Object[]{renamer});
                currentConfigIndex = configModel.getRowCount() - 1;
                loadOptions(currentConfigIndex);

                configItems.editCellAt(currentConfigIndex, 0);
                configItems.changeSelection(currentConfigIndex, currentConfigIndex, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
                configItems.requestFocus();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionRecursiveClicked(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            if (e.getActionCommand().equals("recursiveChanged"))
            {
                if (currentRenamer != null)
                {
                    if (currentRenamer.isRecursive() != checkBoxRecursive.isSelected())
                    {
                        setRenamerOptions(currentRenamer);
                        processTable();
                    }
                }
            }
        }
    }

    private void actionRunClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            currentConfigIndex = index;
            RenamerTool tool = (RenamerTool) configModel.getValueAt(index, 0);
            workerRenamer = tool.clone();
            processSelected(workerRenamer);
        }
    }

    private void actionRefreshClicked(ActionEvent e)
    {
        loadTable();
        processTable();
    }

    private void addHandlers()
    {
        Action actTabKey = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setRenamerOptions(currentRenamer);
                processTable();
            }
        };

        // insert card components
        addTabHandler(textFieldToInsert, actTabKey);
        addTabHandler(textFieldInsertPosition, actTabKey);
        addTabHandler(checkBoxInsertFromEnd, actTabKey);
        addTabHandler(checkBoxInsertOverwrite, actTabKey);
    }

    private void addTabHandler(JCheckBox box, Action action)
    {
        Set forwardKeys = box.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        Set newForwardKeys = new HashSet(forwardKeys);
        newForwardKeys.remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        box.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newForwardKeys);
        box.getInputMap().put(KeyStroke.getKeyStroke("VK_TAB"), "doTab");
        box.getActionMap().put("doTab", action);
    }

    private void addTabHandler(JTextField field, Action action)
    {
        Set forwardKeys = field.getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
        Set newForwardKeys = new HashSet(forwardKeys);
        newForwardKeys.remove(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0));
        field.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newForwardKeys);
        field.getInputMap().put(KeyStroke.getKeyStroke("VK_TAB"), "doTab");
        field.getActionMap().put("doTab", action);
    }

    private void cardShown(ComponentEvent e)
    {
        loadTable();
        updateState();
        processTable();
    }

    public void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((RenamerTool) configModel.getValueAt(i, 0)).reset();
            ((RenamerTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }

        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
    }

    public boolean checkForChanges()
    {
        for (int i = 0; i < deletedTools.size(); ++i)
        {
            if (deletedTools.get(i).isDataChanged())
                return true;
        }

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
/*
        JTable src = (JTable) e.getSource();
        if (e.getClickCount() == 1)
        {
            int index = src.getSelectedRow();
            if (index != currentConfigIndex)
            {
                setRenamerOptions(currentConfigIndex);
                currentConfigIndex = index;
                loadOptions(currentConfigIndex);
            }
        }
*/
    }

    private void genericAction(ActionEvent e)
    {
        if (e.getActionCommand() != null)
        {
            updateOnChange(e.getSource());
        }
    }

    private void genericTextFieldFocusLost(FocusEvent e)
    {
        updateOnChange(e.getSource());
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    private void initNewCard(int type)
    {
        currentRenamer.setDataHasChanged();
        checkBoxRecursive.setSelected(false);
        checkBoxFilesOnly.setSelected(true);
        comboBoxFilenameSegment.setSelectedIndex(0);
        switch (type)
        {
            case 0: // Case change
                break;
            case 1: // Insert
                currentRenamer.setText1("");
                currentRenamer.setText2("0");
                currentRenamer.setOption1(false);
                currentRenamer.setOption2(false);
                currentRenamer.setOption3(false);
                break;
            case 2: // Numbering
                currentRenamer.setText1("0");
                currentRenamer.setText2("0");
                currentRenamer.setText3("0");
                currentRenamer.setOption1(false);
                currentRenamer.setOption2(false);
                currentRenamer.setOption3(false);
                break;
            case 3: // Remove
                currentRenamer.setText1("0");
                currentRenamer.setText2("0");
                currentRenamer.setOption1(false);
                break;
            case 4: // Replace
                currentRenamer.setText1("");
                currentRenamer.setText2("");
                currentRenamer.setOption1(false);
                currentRenamer.setOption2(false);
                break;
        }
    }

    private void loadConfigurations()
    {
        try
        {
            Tools tools = new Tools();
            ArrayList<AbstractTool> toolList = tools.loadAllTools(context, RenamerTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                RenamerTool renamer = (RenamerTool) tool;
                configModel.addRow(new Object[]{renamer});
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context.navigator != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogRenamer, msg, context.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        configModel.loadJobsConfigurations(this, null);

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
            processTable();
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
        currentConfigIndex = 0;
    }

    private void loadOptions(int index)
    {
        currentRenamer = null;
        if (index >= 0 && index < configModel.getRowCount())
        {
            currentRenamer = (RenamerTool) configModel.getValueAt(index, 0);
        }

        if ((index >= 0 && index < configModel.getRowCount()) && currentRenamer != null)
        {
            loading = true;

            labelRenameType.setText(displayNames[currentRenamer.getType()]);
            checkBoxRecursive.setSelected(currentRenamer.isRecursive());
            checkBoxFilesOnly.setSelected(currentRenamer.isFilesOnly());
            comboBoxFilenameSegment.setSelectedIndex(currentRenamer.getSegment());

            ((CardLayout) panelOptionsCards.getLayout()).show(panelOptionsCards, cardNames[currentRenamer.getType()]);

            // populate card
            switch (currentRenamer.getType())
            {
                case 0: // Case change
                    currentCard = panelCaseChangeCard;
                    Enumeration<AbstractButton> elements = buttonGroupChangeCase.getElements();
                    if (currentRenamer.getText1().length() == 0)
                    {
                        AbstractButton button = elements.nextElement();
                        button.setSelected(true);
                    }
                    else
                    {
                        while (elements.hasMoreElements())
                        {
                            AbstractButton button = elements.nextElement();
                            if (button.getActionCommand().equals(currentRenamer.getText1()))
                                button.setSelected(true);
                            else
                                button.setSelected(false);
                        }
                    }
                    break;
                case 1: // Insert
                    currentCard = panelInsertCard;
                    textFieldToInsert.setText(currentRenamer.getText1());
                    textFieldInsertPosition.setText(currentRenamer.getText2());
                    checkBoxInsertFromEnd.setSelected(currentRenamer.isOption1());
                    checkBoxInsertAtEnd.setSelected(currentRenamer.isOption2());
                    checkBoxInsertOverwrite.setSelected(currentRenamer.isOption3());
                    break;
                case 2: // Numbering
                    currentCard = panelNumberingCard;
                    textFieldNumberingStart.setText(currentRenamer.getText1());
                    textFieldNumberingZeros.setText(currentRenamer.getText2());
                    textFieldNumberingPosition.setText(currentRenamer.getText3());
                    checkBoxNumberingFromEnd.setSelected(currentRenamer.isOption1());
                    checkBoxNumberingAtEnd.setSelected(currentRenamer.isOption2());
                    checkBoxNumberingOverwrite.setSelected(currentRenamer.isOption3());
                    break;
                case 3: // Remove
                    currentCard = panelRemoveCard;
                    textFieldFrom.setText(currentRenamer.getText1());
                    textFieldLength.setText(currentRenamer.getText2());
                    checkBoxRemoveFromEnd.setSelected(currentRenamer.isOption1());
                    break;
                case 4: // Replace
                    currentCard = panelReplaceCard;
                    textFieldFind.setText(currentRenamer.getText1());
                    textFieldReplace.setText(currentRenamer.getText2());
                    checkBoxRegularExpr.setSelected(currentRenamer.isOption1());
                    checkBoxCase.setSelected(currentRenamer.isOption2());
                    break;
            }

            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);
            buttonRun.setEnabled(true);
            buttonRefresh.setEnabled(true);
            loading = false;
        }
        else
        {
            currentCard = panelGettingStarted;
            ((CardLayout) panelOptionsCards.getLayout()).show(panelOptionsCards, "gettingStarted");
            labelRenameType.setText("");
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonRefresh.setEnabled(false);
        }
    }

    private void loadTable()
    {
        isSubscriber = makeChangeStringsFromSelected();
        refreshTable();
    }

    public boolean makeChangeStringsFromSelected()
    {
        int index = 0;
        boolean isSubscriber = false;

        Object object = context.browser.lastComponent;
        if (object instanceof JTree)
        {
            JTree sourceTree = (JTree) object;
            int row = sourceTree.getLeadSelectionRow();
            if (row > -1)
            {
                changeStrings = new String[sourceTree.getSelectionCount()][2];
                TreePath[] paths = sourceTree.getSelectionPaths();
                for (TreePath tp : paths)
                {
                    NavTreeNode ntn = (NavTreeNode) tp.getLastPathComponent();
                    NavTreeUserObject tuo = ntn.getUserObject();
                    isSubscriber = tuo.isSubscriber();
                    changeStrings[index][0] = tuo.name;
                    changeStrings[index][1] = tuo.name;
                    ++index;
                }
            }
        }
        else if (object instanceof JTable)
        {
            JTable sourceTable = (JTable) object;
            int row = sourceTable.getSelectedRow();
            if (row > -1)
            {
                changeStrings = new String[sourceTable.getSelectedRowCount()][2];
                int[] rows = sourceTable.getSelectedRows();
                for (int i = 0; i < rows.length; ++i)
                {
                    NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                    isSubscriber = tuo.isSubscriber();
                    changeStrings[index][0] = tuo.name;
                    changeStrings[index][1] = tuo.name;
                    ++index;
                }
            }
        }

        return isSubscriber;
    }

    public void processSelected(RenamerTool renamer)
    {
        if (renamer != null)
        {
            try
            {
                ArrayList<Origin> origins = new ArrayList<Origin>();
                isSubscriber = Origins.makeOriginsFromSelected(context, this, origins, renamer.isRealOnly());

                if (origins != null && origins.size() > 0)
                {
                    int count = origins.size();

                    // make dialog pieces
                    String which = (isSubscriber) ? context.cfg.gs("Z.subscriber") : context.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(context.cfg.gs("Renamer.run.on.N.locations"), renamer.getConfigName(), count, which);
                    JCheckBox checkbox = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
                    checkbox.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
                    checkbox.setSelected(true);
                    Object[] params = {message, checkbox};

                    // confirm run of tool
                    int reply = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("Renamer.title"), JOptionPane.YES_NO_OPTION);
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

                            worker = task.process(context, renamer, isDryRun);
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
                                                processTerminated(task, renamer);
                                        }
                                    }
                                });
                            }
                        }
                        catch (Exception e)
                        {
                            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                            if (context.navigator != null)
                            {
                                logger.error(msg);
                                JOptionPane.showMessageDialog(context.navigator.dialogRenamer, msg, context.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                            }
                            else
                                logger.error(msg);
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, context.cfg.gs("Renamer.nothing.selected.in.browser"),
                            context.cfg.gs("Renamer.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (Exception e)
            {
                if (!e.getMessage().equals("HANDLED_INTERNALLY"))
                {
                    String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                    if (context.navigator != null)
                    {
                        logger.error(msg);
                        JOptionPane.showMessageDialog(context.navigator.dialogRenamer, msg, context.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        logger.error(msg);
                }
            }
        }
    }

    private void processTerminated(Task task, RenamerTool renamer)
    {
        // reset and reload relevant trees
        if (!isDryRun) // && task.getTool().renameCount > 0)
        {
            if (!isSubscriber)
            {
                context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionOne, context.publisherRepo, false, false);
                context.browser.deepScanSystemTree(context.mainFrame.treeSystemOne, context.publisherRepo, false, false);
            }
            else
            {
                context.browser.deepScanCollectionTree(context.mainFrame.treeCollectionTwo, context.subscriberRepo, context.cfg.isRemoteSession(), false);
                context.browser.deepScanSystemTree(context.mainFrame.treeSystemTwo, context.subscriberRepo, context.cfg.isRemoteSession(), false);
            }
        }

        if (context.progress != null)
            context.progress.done();

        Origins.setSelectedFromOrigins(context, this, task.getOrigins());

        setComponentEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;
        workerRenamer = null;

        loadTable();
        processTable();

        if (renamer.isRequestStop())
        {
            logger.info(renamer.getConfigName() + context.cfg.gs("Z.cancelled"));
            context.mainFrame.labelStatusMiddle.setText(renamer.getConfigName() + context.cfg.gs("Z.cancelled"));
        }
    }

    private void processTable()
    {
        if (currentRenamer != null && changeStrings != null && changeStrings.length > 0)
        {
            currentRenamer.reset();
            for (int i = 0; i < changeStrings.length; ++i)
            {
                String left = changeStrings[i][0];
                String right = currentRenamer.exec(left);
                changeStrings[i][1] = right;
            }
        }
        refreshTable();
    }

    private void refreshTable()
    {
        changeModel = new ChangesTableModel(context);
        tableChanges.removeAll();
        if (changeStrings != null && changeStrings.length > 0)
        {
            for (int index = 0; index < changeStrings.length; ++index)
            {
                changeModel.addRow(new Object[]{changeStrings[index][0], changeStrings[index][1]});
            }
        }
        tableChanges.setModel(changeModel);
        changeModel.fireTableDataChanged();
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
                renamer.setDataHasChanged(false);
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                renamer = (RenamerTool) deletedTools.get(i);
                File file = new File(renamer.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
                renamer.setDataHasChanged(false);
            }

            // write/update changed Job JSON configuration files
            configModel.saveJobsConfigurations(null);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context.navigator != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogRenamer, msg, context.cfg.gs("Renamer.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    private void savePreferences()
    {
        context.preferences.setToolsRenamerHeight(this.getHeight());
        context.preferences.setToolsRenamerWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsRenamerXpos(location.x);
        context.preferences.setToolsRenamerYpos(location.y);
        context.preferences.setToolsRenamerDividerLocation(splitPaneContent.getDividerLocation());
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

    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
    }

    private void setRenamerOptions(int index)
    {
        if (index < 0)
            index = currentConfigIndex;

        RenamerTool renamer = (RenamerTool) configItems.getModel().getValueAt(index, 0);
        setRenamerOptions(renamer);
    }

    private void setRenamerOptions(RenamerTool renamer)
    {
        if (renamer != null && !loading)
        {
            renamer.setDataHasChanged();
            renamer.setIsRecursive(checkBoxRecursive.isSelected());
            renamer.setIsFilesOnly(checkBoxFilesOnly.isSelected());
            renamer.setSegment(comboBoxFilenameSegment.getSelectedIndex());
            switch (renamer.getType())
            {
                case 0: // Case change
                    Enumeration<AbstractButton> elements = buttonGroupChangeCase.getElements();
                    while (elements.hasMoreElements())
                    {
                        AbstractButton button = elements.nextElement();
                        if (button.isSelected())
                        {
                            String act = button.getActionCommand();
                            renamer.setText1(act);
                        }
                    }
                    renamer.setText2("");
                    renamer.setText3("");
                    renamer.setOption1(false);
                    renamer.setOption2(false);
                    renamer.setOption2(false);
                    break;
                case 1: // Insert
                    renamer.setText1(textFieldToInsert.getText());
                    renamer.setText2(setZeroEmpty(textFieldInsertPosition.getText()));
                    renamer.setText3("");
                    renamer.setOption1(checkBoxInsertFromEnd.isSelected());
                    renamer.setOption2(checkBoxInsertAtEnd.isSelected());
                    renamer.setOption3(checkBoxInsertOverwrite.isSelected());
                    break;
                case 2: // Numbering
                    renamer.setText1(setZeroEmpty(textFieldNumberingStart.getText()));
                    renamer.setText2(setZeroEmpty(textFieldNumberingZeros.getText()));
                    renamer.setText3(setZeroEmpty(textFieldNumberingPosition.getText()));
                    renamer.setOption1(checkBoxNumberingFromEnd.isSelected());
                    renamer.setOption2(checkBoxNumberingAtEnd.isSelected());
                    renamer.setOption3(checkBoxNumberingOverwrite.isSelected());
                    break;
                case 3: // Remove
                    renamer.setText1(textFieldFrom.getText());
                    renamer.setText2(textFieldLength.getText());
                    renamer.setText3("");
                    renamer.setOption1(checkBoxRemoveFromEnd.isSelected());
                    renamer.setOption2(false);
                    renamer.setOption3(false);
                    break;
                case 4: // Replace
                    renamer.setText1(textFieldFind.getText());
                    renamer.setText2(textFieldReplace.getText());
                    renamer.setText3("");
                    renamer.setOption1(checkBoxRegularExpr.isSelected());
                    renamer.setOption2(checkBoxCase.isSelected());
                    renamer.setOption3(false);
                    break;
            }
        }
    }

    private String setZeroEmpty(String value)
    {
        return (value.length() == 0) ? "0" : value;
    }

    private void tabKeyPressed(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_TAB)
        {
            updateOnChange(e.getSource());
        }
    }

    private void updateOnChange(Object source)
    {
        String name = null;
        if (source instanceof JTextField)
        {
            String current = null;
            JTextField tf = (JTextField) source;
            name = tf.getName();
            switch (name)
            {
                case "text1":
                    current = currentRenamer.getText1();
                    break;
                case "text2":
                    current = currentRenamer.getText2();
                    break;
                case "text3":
                    current = currentRenamer.getText3();
                    break;
            }
            if (!current.equals(tf.getText()))
            {
                setRenamerOptions(currentRenamer);
                updateState();
                processTable();
            }
        }
        else if (source instanceof JCheckBox)
        {
            boolean state = false;
            JCheckBox cb = (JCheckBox) source;
            name = cb.getName();
            switch (name)
            {
                case "option1":
                    state = currentRenamer.isOption1();
                    break;
                case "option2":
                    state = currentRenamer.isOption2();
                    break;
                case "option3":
                    state = currentRenamer.isOption3();
                    break;
            }
            if (state != cb.isSelected())
            {
                setRenamerOptions(currentRenamer);
                updateState();
                processTable();
            }
        }
    }

    private void updateState()
    {
        if (currentCard == panelCaseChangeCard)
        {
        }
        else if (currentCard == panelInsertCard)
        {
            if (currentRenamer.isOption1())
            {
                checkBoxInsertAtEnd.setEnabled(false);
            }
            else
            {
                checkBoxInsertAtEnd.setEnabled(true);
            }
            if (currentRenamer.isOption2())
            {
                textFieldInsertPosition.setEnabled(false);
                checkBoxInsertFromEnd.setEnabled(false);
                checkBoxInsertOverwrite.setEnabled(false);
            }
            else
            {
                textFieldInsertPosition.setEnabled(true);
                checkBoxInsertFromEnd.setEnabled(true);
                checkBoxInsertOverwrite.setEnabled(true);
            }
            setNumberFilter(textFieldInsertPosition);
        }
        else if (currentCard == panelNumberingCard)
        {
            if (currentRenamer.isOption1())
            {
                checkBoxNumberingAtEnd.setEnabled(false);
            }
            else
            {
                checkBoxNumberingAtEnd.setEnabled(true);
            }
            if (currentRenamer.isOption2())
            {
                textFieldNumberingPosition.setEnabled(false);
                checkBoxNumberingFromEnd.setEnabled(false);
                checkBoxNumberingOverwrite.setEnabled(false);
            }
            else
            {
                textFieldNumberingPosition.setEnabled(true);
                checkBoxNumberingFromEnd.setEnabled(true);
                checkBoxNumberingOverwrite.setEnabled(true);
            }
            setNumberFilter(textFieldNumberingStart);
            setNumberFilter(textFieldNumberingZeros);
            setNumberFilter(textFieldNumberingPosition);
        }
        else if (currentCard == panelRemoveCard)
        {
            if (currentRenamer.isOption1())
                textFieldFrom.setEnabled(false);
            else
                textFieldFrom.setEnabled(true);
            setNumberFilter(textFieldFrom);
            setNumberFilter(textFieldLength);
        }
        else if (currentCard == panelReplaceCard)
        {
            if (currentRenamer.isOption1())
            {
                checkBoxRegularExpr.setEnabled(true);
                checkBoxCase.setEnabled(false);
            }
            else if (currentRenamer.isOption2())
            {
                checkBoxRegularExpr.setEnabled(false);
                checkBoxCase.setEnabled(true);
            }
            else
            {
                checkBoxRegularExpr.setEnabled(true);
                checkBoxCase.setEnabled(true);
            }
        }
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    private void windowHidden(ComponentEvent e)
    {
        context.navigator.enableDisableToolMenus(this, true);
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
        panelCbOpts = new JPanel();
        checkBoxRecursive = new JCheckBox();
        checkBoxFilesOnly = new JCheckBox();
        panelFilenameSegment = new JPanel();
        comboBoxFilenameSegment = new JComboBox<>();
        hSpacer2 = new JPanel(null);
        panelCardBox = new JPanel();
        vSpacer1 = new JPanel(null);
        separator1 = new JSeparator();
        vSpacer2 = new JPanel(null);
        panelOptionsCards = new JPanel();
        panelGettingStarted = new JPanel();
        labelOperationGettingStarted = new JLabel();
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
        hSpacer7 = new JPanel(null);
        checkBoxInsertAtEnd = new JCheckBox();
        panelInsertOther = new JPanel();
        checkBoxInsertOverwrite = new JCheckBox();
        panelNumberingCard = new JPanel();
        labelStart = new JLabel();
        panelNums = new JPanel();
        textFieldNumberingStart = new JTextField();
        hSpacer5 = new JPanel(null);
        labelZeros = new JLabel();
        textFieldNumberingZeros = new JTextField();
        labelNumberingPosition = new JLabel();
        panelNumberingPostion = new JPanel();
        textFieldNumberingPosition = new JTextField();
        hSpacer4 = new JPanel(null);
        checkBoxNumberingFromEnd = new JCheckBox();
        hSpacer8 = new JPanel(null);
        checkBoxNumberingAtEnd = new JCheckBox();
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
        tableChanges = new JTable();
        panelOptionsButtons = new JPanel();
        buttonRefresh = new JButton();
        buttonBar = new JPanel();
        saveButton = new JButton();
        cancelButton = new JButton();
        buttonGroupChangeCase = new ButtonGroup();

        //======== this ========
        setTitle(context.cfg.gs("Renamer.title"));
        setName("renamerUI");
        setMinimumSize(new Dimension(150, 126));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                RenamerUI.this.windowClosing(e);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                windowHidden(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(630, 470));
            dialogPane.setMinimumSize(new Dimension(150, 80));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(614, 470));
                contentPanel.setMinimumSize(new Dimension(140, 120));
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setMinimumSize(new Dimension(140, 38));
                    panelTop.setPreferredSize(new Dimension(614, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(context.cfg.gs("Renamer.button.New.text"));
                        buttonNew.setMnemonic(context.cfg.gs("Renamer.button.New.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("Renamer.button.New.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("Renamer.button.Copy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("Renamer.button.Delete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("Renamer.button.Run.text"));
                        buttonRun.setMnemonic(context.cfg.gs("Renamer.button.Run.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("Renamer.button.Run.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("Renamer.help"));
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
                        configItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        configItems.setShowVerticalLines(false);
                        configItems.setFillsViewportHeight(true);
                        configItems.setShowHorizontalLines(false);
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
                                    labelRenameType.setMaximumSize(new Dimension(110, 16));
                                    labelRenameType.setFont(labelRenameType.getFont().deriveFont(labelRenameType.getFont().getStyle() | Font.BOLD, labelRenameType.getFont().getSize() + 1f));
                                    labelRenameType.setPreferredSize(new Dimension(110, 16));
                                    labelRenameType.setMinimumSize(new Dimension(110, 16));
                                    panelRenameType.add(labelRenameType);
                                }
                                topOptions.add(panelRenameType, BorderLayout.WEST);

                                //======== panelCbOpts ========
                                {
                                    panelCbOpts.setMaximumSize(new Dimension(32767, 21));
                                    panelCbOpts.setMinimumSize(new Dimension(120, 21));
                                    panelCbOpts.setPreferredSize(new Dimension(240, 21));
                                    panelCbOpts.setLayout(new BoxLayout(panelCbOpts, BoxLayout.X_AXIS));

                                    //---- checkBoxRecursive ----
                                    checkBoxRecursive.setText(context.cfg.gs("Renamer.checkBoxRecursive.text"));
                                    checkBoxRecursive.setHorizontalAlignment(SwingConstants.CENTER);
                                    checkBoxRecursive.setToolTipText(context.cfg.gs("Renamer.checkBoxRecursive.toolTipText"));
                                    checkBoxRecursive.setActionCommand("recursiveChanged");
                                    checkBoxRecursive.setPreferredSize(new Dimension(120, 21));
                                    checkBoxRecursive.setMaximumSize(new Dimension(32767, 21));
                                    checkBoxRecursive.setMinimumSize(new Dimension(80, 21));
                                    checkBoxRecursive.addActionListener(e -> actionRecursiveClicked(e));
                                    panelCbOpts.add(checkBoxRecursive);

                                    //---- checkBoxFilesOnly ----
                                    checkBoxFilesOnly.setText(context.cfg.gs("Renamer.checkBoxFilesOnly.text"));
                                    checkBoxFilesOnly.setHorizontalAlignment(SwingConstants.CENTER);
                                    checkBoxFilesOnly.setAlignmentX(0.5F);
                                    checkBoxFilesOnly.setPreferredSize(new Dimension(120, 21));
                                    checkBoxFilesOnly.setMaximumSize(new Dimension(32767, 21));
                                    checkBoxFilesOnly.setActionCommand("filesOnlyChanged");
                                    checkBoxFilesOnly.setToolTipText(context.cfg.gs("Renamer.checkBoxFilesOnly.toolTipText"));
                                    checkBoxFilesOnly.addActionListener(e -> actionFilesOnlyClicked(e));
                                    panelCbOpts.add(checkBoxFilesOnly);
                                }
                                topOptions.add(panelCbOpts, BorderLayout.CENTER);

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
                                    comboBoxFilenameSegment.addActionListener(e -> actionFilenameSegmentClicked(e));
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

                                        //======== panelGettingStarted ========
                                        {
                                            panelGettingStarted.setLayout(new BorderLayout());

                                            //---- labelOperationGettingStarted ----
                                            labelOperationGettingStarted.setText(context.cfg.gs("Renamer.labelGettingStarted.text"));
                                            labelOperationGettingStarted.setFont(labelOperationGettingStarted.getFont().deriveFont(labelOperationGettingStarted.getFont().getStyle() | Font.BOLD));
                                            labelOperationGettingStarted.setHorizontalAlignment(SwingConstants.CENTER);
                                            panelGettingStarted.add(labelOperationGettingStarted, BorderLayout.CENTER);
                                        }
                                        panelOptionsCards.add(panelGettingStarted, "gettingStarted");

                                        //======== panelCaseChangeCard ========
                                        {
                                            panelCaseChangeCard.setPreferredSize(new Dimension(328, 92));
                                            panelCaseChangeCard.setMinimumSize(new Dimension(328, 92));
                                            panelCaseChangeCard.setMaximumSize(new Dimension(32767, 92));
                                            panelCaseChangeCard.addComponentListener(new ComponentAdapter() {
                                                @Override
                                                public void componentShown(ComponentEvent e) {
                                                    cardShown(e);
                                                }
                                            });
                                            panelCaseChangeCard.setLayout(new GridLayout(2, 2));

                                            //---- radioButtonFirstUpperCase ----
                                            radioButtonFirstUpperCase.setText(context.cfg.gs("Renamer.radioButtonFirstUpperCase.text"));
                                            radioButtonFirstUpperCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            radioButtonFirstUpperCase.setActionCommand("firstupper");
                                            radioButtonFirstUpperCase.addActionListener(e -> actionCaseChangeClicked(e));
                                            panelCaseChangeCard.add(radioButtonFirstUpperCase);

                                            //---- radioButtonLowerCase ----
                                            radioButtonLowerCase.setText(context.cfg.gs("Renamer.radioButtonLowerCase.text"));
                                            radioButtonLowerCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            radioButtonLowerCase.addActionListener(e -> actionCaseChangeClicked(e));
                                            panelCaseChangeCard.add(radioButtonLowerCase);

                                            //---- radioButtonTitleCase ----
                                            radioButtonTitleCase.setText(context.cfg.gs("Renamer.radioButtonTitleCase.text"));
                                            radioButtonTitleCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            radioButtonTitleCase.setActionCommand("titlecase");
                                            radioButtonTitleCase.addActionListener(e -> actionCaseChangeClicked(e));
                                            panelCaseChangeCard.add(radioButtonTitleCase);

                                            //---- radioButtonUpperCase ----
                                            radioButtonUpperCase.setText(context.cfg.gs("Renamer.radioButtonUpperCase.text"));
                                            radioButtonUpperCase.setHorizontalAlignment(SwingConstants.CENTER);
                                            radioButtonUpperCase.setActionCommand("upper");
                                            radioButtonUpperCase.addActionListener(e -> actionCaseChangeClicked(e));
                                            panelCaseChangeCard.add(radioButtonUpperCase);
                                        }
                                        panelOptionsCards.add(panelCaseChangeCard, "cardCaseChange");

                                        //======== panelInsertCard ========
                                        {
                                            panelInsertCard.setMaximumSize(new Dimension(32767, 92));
                                            panelInsertCard.setMinimumSize(new Dimension(328, 92));
                                            panelInsertCard.setPreferredSize(new Dimension(328, 92));
                                            panelInsertCard.addComponentListener(new ComponentAdapter() {
                                                @Override
                                                public void componentShown(ComponentEvent e) {
                                                    cardShown(e);
                                                }
                                            });
                                            panelInsertCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelInsertCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelInsertCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelTextToInsert ----
                                            labelTextToInsert.setText(context.cfg.gs("Renamer.labelTextToInsert.text"));
                                            labelTextToInsert.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelInsertCard.add(labelTextToInsert, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldToInsert ----
                                            textFieldToInsert.setPreferredSize(new Dimension(320, 30));
                                            textFieldToInsert.setMinimumSize(new Dimension(50, 30));
                                            textFieldToInsert.setToolTipText(context.cfg.gs("Renamer.textFieldToInsert.toolTipText"));
                                            textFieldToInsert.setName("text1");
                                            textFieldToInsert.addActionListener(e -> genericAction(e));
                                            textFieldToInsert.addKeyListener(new KeyAdapter() {
                                                @Override
                                                public void keyPressed(KeyEvent e) {
                                                    tabKeyPressed(e);
                                                }
                                            });
                                            textFieldToInsert.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    genericTextFieldFocusLost(e);
                                                }
                                            });
                                            panelInsertCard.add(textFieldToInsert, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelInsertPosition ----
                                            labelInsertPosition.setText(context.cfg.gs("Renamer.labelInsertPosition.text"));
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
                                                textFieldInsertPosition.setToolTipText(context.cfg.gs("Renamer.textFieldInsertPosition.toolTipText"));
                                                textFieldInsertPosition.setName("text2");
                                                textFieldInsertPosition.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                textFieldInsertPosition.addActionListener(e -> genericAction(e));
                                                textFieldInsertPosition.addFocusListener(new FocusAdapter() {
                                                    @Override
                                                    public void focusLost(FocusEvent e) {
                                                        genericTextFieldFocusLost(e);
                                                    }
                                                });
                                                panelInsertPostion.add(textFieldInsertPosition);

                                                //---- hSpacer3 ----
                                                hSpacer3.setMinimumSize(new Dimension(10, 10));
                                                hSpacer3.setMaximumSize(new Dimension(10, 10));
                                                panelInsertPostion.add(hSpacer3);

                                                //---- checkBoxInsertFromEnd ----
                                                checkBoxInsertFromEnd.setText(context.cfg.gs("Renamer.checkBoxInsertFromEnd.text"));
                                                checkBoxInsertFromEnd.setToolTipText(context.cfg.gs("Renamer.checkBoxInsertFromEnd.toolTipText"));
                                                checkBoxInsertFromEnd.setName("option1");
                                                checkBoxInsertFromEnd.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxInsertFromEnd.addActionListener(e -> genericAction(e));
                                                panelInsertPostion.add(checkBoxInsertFromEnd);

                                                //---- hSpacer7 ----
                                                hSpacer7.setMinimumSize(new Dimension(10, 10));
                                                hSpacer7.setMaximumSize(new Dimension(10, 10));
                                                panelInsertPostion.add(hSpacer7);

                                                //---- checkBoxInsertAtEnd ----
                                                checkBoxInsertAtEnd.setText(context.cfg.gs("Renamer.checkBoxInsertAtEnd.text"));
                                                checkBoxInsertAtEnd.setToolTipText(context.cfg.gs("Renamer.checkBoxInsertAtEnd.toolTipText"));
                                                checkBoxInsertAtEnd.setName("option2");
                                                checkBoxInsertAtEnd.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxInsertAtEnd.addActionListener(e -> genericAction(e));
                                                panelInsertPostion.add(checkBoxInsertAtEnd);
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
                                                checkBoxInsertOverwrite.setText(context.cfg.gs("Renamer.checkBoxInsertOverwrite.text"));
                                                checkBoxInsertOverwrite.setMargin(new Insets(2, 6, 2, 2));
                                                checkBoxInsertOverwrite.setName("option3");
                                                checkBoxInsertOverwrite.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxInsertOverwrite.addActionListener(e -> genericAction(e));
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
                                            panelNumberingCard.addComponentListener(new ComponentAdapter() {
                                                @Override
                                                public void componentShown(ComponentEvent e) {
                                                    cardShown(e);
                                                }
                                            });
                                            panelNumberingCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelNumberingCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelNumberingCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelStart ----
                                            labelStart.setText(context.cfg.gs("Renamer.labelStart.text"));
                                            labelStart.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelNumberingCard.add(labelStart, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //======== panelNums ========
                                            {
                                                panelNums.setLayout(new BoxLayout(panelNums, BoxLayout.X_AXIS));

                                                //---- textFieldNumberingStart ----
                                                textFieldNumberingStart.setPreferredSize(new Dimension(96, 30));
                                                textFieldNumberingStart.setMinimumSize(new Dimension(96, 30));
                                                textFieldNumberingStart.setMaximumSize(new Dimension(96, 30));
                                                textFieldNumberingStart.setText("1");
                                                textFieldNumberingStart.setToolTipText(context.cfg.gs("Renamer.textFieldNumberingStart.toolTipText"));
                                                textFieldNumberingStart.setName("text1");
                                                textFieldNumberingStart.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                textFieldNumberingStart.addActionListener(e -> genericAction(e));
                                                textFieldNumberingStart.addFocusListener(new FocusAdapter() {
                                                    @Override
                                                    public void focusLost(FocusEvent e) {
                                                        genericTextFieldFocusLost(e);
                                                    }
                                                });
                                                panelNums.add(textFieldNumberingStart);

                                                //---- hSpacer5 ----
                                                hSpacer5.setMinimumSize(new Dimension(10, 10));
                                                hSpacer5.setMaximumSize(new Dimension(32767, 10));
                                                hSpacer5.setPreferredSize(new Dimension(88, 10));
                                                panelNums.add(hSpacer5);

                                                //---- labelZeros ----
                                                labelZeros.setText(context.cfg.gs("Renamer.labelZeros.text"));
                                                panelNums.add(labelZeros);

                                                //---- textFieldNumberingZeros ----
                                                textFieldNumberingZeros.setPreferredSize(new Dimension(96, 30));
                                                textFieldNumberingZeros.setMinimumSize(new Dimension(96, 30));
                                                textFieldNumberingZeros.setMaximumSize(new Dimension(96, 30));
                                                textFieldNumberingZeros.setText("1");
                                                textFieldNumberingZeros.setToolTipText(context.cfg.gs("Renamer.textFieldNumberingZeros.toolTipText"));
                                                textFieldNumberingZeros.setName("text2");
                                                textFieldNumberingZeros.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                textFieldNumberingZeros.addActionListener(e -> genericAction(e));
                                                textFieldNumberingZeros.addFocusListener(new FocusAdapter() {
                                                    @Override
                                                    public void focusLost(FocusEvent e) {
                                                        genericTextFieldFocusLost(e);
                                                    }
                                                });
                                                panelNums.add(textFieldNumberingZeros);
                                            }
                                            panelNumberingCard.add(panelNums, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labelNumberingPosition ----
                                            labelNumberingPosition.setText(context.cfg.gs("Renamer.labelNumberingPosition.text_2"));
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
                                                textFieldNumberingPosition.setName("text3");
                                                textFieldNumberingPosition.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                textFieldNumberingPosition.addActionListener(e -> genericAction(e));
                                                textFieldNumberingPosition.addFocusListener(new FocusAdapter() {
                                                    @Override
                                                    public void focusLost(FocusEvent e) {
                                                        genericTextFieldFocusLost(e);
                                                    }
                                                });
                                                panelNumberingPostion.add(textFieldNumberingPosition);

                                                //---- hSpacer4 ----
                                                hSpacer4.setMinimumSize(new Dimension(10, 10));
                                                hSpacer4.setMaximumSize(new Dimension(10, 10));
                                                panelNumberingPostion.add(hSpacer4);

                                                //---- checkBoxNumberingFromEnd ----
                                                checkBoxNumberingFromEnd.setText(context.cfg.gs("Renamer.checkBoxNumberingFromEnd.text_2"));
                                                checkBoxNumberingFromEnd.setName("option1");
                                                checkBoxNumberingFromEnd.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxNumberingFromEnd.addActionListener(e -> genericAction(e));
                                                panelNumberingPostion.add(checkBoxNumberingFromEnd);

                                                //---- hSpacer8 ----
                                                hSpacer8.setMinimumSize(new Dimension(10, 10));
                                                hSpacer8.setMaximumSize(new Dimension(10, 10));
                                                panelNumberingPostion.add(hSpacer8);

                                                //---- checkBoxNumberingAtEnd ----
                                                checkBoxNumberingAtEnd.setText(context.cfg.gs("Renamer.checkBoxNumberingAtEnd.text"));
                                                checkBoxNumberingAtEnd.setToolTipText(context.cfg.gs("Renamer.checkBoxNumberingAtEnd.toolTipText"));
                                                checkBoxNumberingAtEnd.setName("option2");
                                                checkBoxNumberingAtEnd.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxNumberingAtEnd.addActionListener(e -> genericAction(e));
                                                panelNumberingPostion.add(checkBoxNumberingAtEnd);
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
                                                checkBoxNumberingOverwrite.setText(context.cfg.gs("Renamer.checkBoxNumberingOverwrite.text_2"));
                                                checkBoxNumberingOverwrite.setMargin(new Insets(2, 6, 2, 2));
                                                checkBoxNumberingOverwrite.setName("option3");
                                                checkBoxNumberingOverwrite.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                checkBoxNumberingOverwrite.addActionListener(e -> genericAction(e));
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
                                            panelRemoveCard.addComponentListener(new ComponentAdapter() {
                                                @Override
                                                public void componentShown(ComponentEvent e) {
                                                    cardShown(e);
                                                }
                                            });
                                            panelRemoveCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelRemoveCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelRemoveCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelFrom ----
                                            labelFrom.setText(context.cfg.gs("Renamer.labelFrom.text"));
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
                                                textFieldFrom.setName("text1");
                                                textFieldFrom.addActionListener(e -> genericAction(e));
                                                textFieldFrom.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
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
                                            labelLength.setText(context.cfg.gs("Renamer.labelLength.text"));
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
                                                textFieldLength.setName("text2");
                                                textFieldLength.addActionListener(e -> genericAction(e));
                                                textFieldLength.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                panelLength.add(textFieldLength);
                                            }
                                            panelRemoveCard.add(panelLength, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelRemoveOther ========
                                            {
                                                panelRemoveOther.setPreferredSize(new Dimension(98, 30));
                                                panelRemoveOther.setMinimumSize(new Dimension(98, 30));
                                                panelRemoveOther.setLayout(new GridLayout(1, 2, 8, 0));

                                                //---- checkBoxRemoveFromEnd ----
                                                checkBoxRemoveFromEnd.setText(context.cfg.gs("Renamer.checkBoxRemoveFromEnd.text"));
                                                checkBoxRemoveFromEnd.setMargin(new Insets(2, 6, 2, 6));
                                                checkBoxRemoveFromEnd.setPreferredSize(new Dimension(98, 21));
                                                checkBoxRemoveFromEnd.setMinimumSize(new Dimension(98, 21));
                                                checkBoxRemoveFromEnd.setMaximumSize(new Dimension(98, 21));
                                                checkBoxRemoveFromEnd.setName("option1");
                                                checkBoxRemoveFromEnd.addActionListener(e -> genericAction(e));
                                                checkBoxRemoveFromEnd.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
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
                                            panelReplaceCard.addComponentListener(new ComponentAdapter() {
                                                @Override
                                                public void componentShown(ComponentEvent e) {
                                                    cardShown(e);
                                                }
                                            });
                                            panelReplaceCard.setLayout(new GridBagLayout());
                                            ((GridBagLayout)panelReplaceCard.getLayout()).rowHeights = new int[] {0, 0, 0, 0};
                                            ((GridBagLayout)panelReplaceCard.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};

                                            //---- labelFind ----
                                            labelFind.setText(context.cfg.gs("Renamer.labelFind.text"));
                                            labelFind.setHorizontalAlignment(SwingConstants.RIGHT);
                                            panelReplaceCard.add(labelFind, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldFind ----
                                            textFieldFind.setPreferredSize(new Dimension(320, 30));
                                            textFieldFind.setMinimumSize(new Dimension(50, 30));
                                            textFieldFind.setName("text1");
                                            textFieldFind.addActionListener(e -> genericAction(e));
                                            textFieldFind.addKeyListener(new KeyAdapter() {
                                                @Override
                                                public void keyPressed(KeyEvent e) {
                                                    tabKeyPressed(e);
                                                }
                                            });
                                            textFieldFind.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    genericTextFieldFocusLost(e);
                                                }
                                            });
                                            panelReplaceCard.add(textFieldFind, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //---- labeReplace ----
                                            labeReplace.setText(context.cfg.gs("Renamer.labelReplace.text"));
                                            labeReplace.setHorizontalAlignment(SwingConstants.TRAILING);
                                            panelReplaceCard.add(labeReplace, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 4), 0, 0));

                                            //---- textFieldReplace ----
                                            textFieldReplace.setPreferredSize(new Dimension(320, 30));
                                            textFieldReplace.setMinimumSize(new Dimension(50, 30));
                                            textFieldReplace.setName("text2");
                                            textFieldReplace.addActionListener(e -> genericAction(e));
                                            textFieldReplace.addKeyListener(new KeyAdapter() {
                                                @Override
                                                public void keyPressed(KeyEvent e) {
                                                    tabKeyPressed(e);
                                                }
                                            });
                                            textFieldReplace.addFocusListener(new FocusAdapter() {
                                                @Override
                                                public void focusLost(FocusEvent e) {
                                                    genericTextFieldFocusLost(e);
                                                }
                                            });
                                            panelReplaceCard.add(textFieldReplace, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                                new Insets(0, 0, 0, 0), 0, 0));

                                            //======== panelReplaceOther ========
                                            {
                                                panelReplaceOther.setPreferredSize(new Dimension(266, 30));
                                                panelReplaceOther.setMinimumSize(new Dimension(266, 30));
                                                panelReplaceOther.setLayout(new GridLayout(1, 0, 8, 0));

                                                //---- checkBoxRegularExpr ----
                                                checkBoxRegularExpr.setText(context.cfg.gs("Renamer.checkBoxRegularExpr.text"));
                                                checkBoxRegularExpr.setMargin(new Insets(2, 6, 2, 6));
                                                checkBoxRegularExpr.setName("option1");
                                                checkBoxRegularExpr.addActionListener(e -> genericAction(e));
                                                checkBoxRegularExpr.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
                                                panelReplaceOther.add(checkBoxRegularExpr);

                                                //---- checkBoxCase ----
                                                checkBoxCase.setText(context.cfg.gs("Renamer.checkBoxCase.text"));
                                                checkBoxCase.setMargin(new Insets(2, 6, 2, 2));
                                                checkBoxCase.setName("option2");
                                                checkBoxCase.addActionListener(e -> genericAction(e));
                                                checkBoxCase.addKeyListener(new KeyAdapter() {
                                                    @Override
                                                    public void keyPressed(KeyEvent e) {
                                                        tabKeyPressed(e);
                                                    }
                                                });
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

                            //---- tableChanges ----
                            tableChanges.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                            tableChanges.setRowSelectionAllowed(false);
                            tableChanges.setModel(new DefaultTableModel(
                                new Object[][] {
                                    {null, null},
                                    {null, null},
                                },
                                new String[] {
                                    "Old Name", "New Name"
                                }
                            ));
                            tableChanges.setFillsViewportHeight(true);
                            tableChanges.setFocusable(false);
                            tableChanges.setToolTipText(context.cfg.gs("Renamer.tableChanges.toolTipText"));
                            scrollPaneExamples.setViewportView(tableChanges);
                        }
                        panelOptions.add(scrollPaneExamples, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonRefresh ----
                            buttonRefresh.setText(context.cfg.gs("Z.refresh"));
                            buttonRefresh.setFont(buttonRefresh.getFont().deriveFont(buttonRefresh.getFont().getSize() - 2f));
                            buttonRefresh.setPreferredSize(new Dimension(78, 24));
                            buttonRefresh.setMinimumSize(new Dimension(78, 24));
                            buttonRefresh.setMaximumSize(new Dimension(78, 24));
                            buttonRefresh.setMnemonic(context.cfg.gs("Renamer.buttonRefresh.mnemonic").charAt(0));
                            buttonRefresh.setToolTipText(context.cfg.gs("Z.refresh.tooltip.text"));
                            buttonRefresh.setMargin(new Insets(0, -10, 0, -10));
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

                //---- saveButton ----
                saveButton.setText(context.cfg.gs("Z.save"));
                saveButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
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
        buttonGroupChangeCase.add(radioButtonFirstUpperCase);
        buttonGroupChangeCase.add(radioButtonLowerCase);
        buttonGroupChangeCase.add(radioButtonTitleCase);
        buttonGroupChangeCase.add(radioButtonUpperCase);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JPanel dialogPane;
    public JPanel contentPanel;
    public JPanel panelTop;
    public JPanel panelTopButtons;
    public JButton buttonNew;
    public JButton buttonCopy;
    public JButton buttonDelete;
    public JPanel hSpacerBeforeRun;
    public JButton buttonRun;
    public JPanel panelHelp;
    public JLabel labelHelp;
    public JSplitPane splitPaneContent;
    public JScrollPane scrollPaneConfig;
    public JTable configItems;
    public JPanel panelOptions;
    public JPanel panelControls;
    public JPanel topOptions;
    public JPanel vSpacer0;
    public JPanel panelRenameType;
    public JPanel hSpacer1;
    public JLabel labelRenameType;
    public JPanel panelCbOpts;
    public JCheckBox checkBoxRecursive;
    public JCheckBox checkBoxFilesOnly;
    public JPanel panelFilenameSegment;
    public JComboBox<String> comboBoxFilenameSegment;
    public JPanel hSpacer2;
    public JPanel panelCardBox;
    public JPanel vSpacer1;
    public JSeparator separator1;
    public JPanel vSpacer2;
    public JPanel panelOptionsCards;
    public JPanel panelGettingStarted;
    public JLabel labelOperationGettingStarted;
    public JPanel panelCaseChangeCard;
    public JRadioButton radioButtonFirstUpperCase;
    public JRadioButton radioButtonLowerCase;
    public JRadioButton radioButtonTitleCase;
    public JRadioButton radioButtonUpperCase;
    public JPanel panelInsertCard;
    public JLabel labelTextToInsert;
    public JTextField textFieldToInsert;
    public JLabel labelInsertPosition;
    public JPanel panelInsertPostion;
    public JTextField textFieldInsertPosition;
    public JPanel hSpacer3;
    public JCheckBox checkBoxInsertFromEnd;
    public JPanel hSpacer7;
    public JCheckBox checkBoxInsertAtEnd;
    public JPanel panelInsertOther;
    public JCheckBox checkBoxInsertOverwrite;
    public JPanel panelNumberingCard;
    public JLabel labelStart;
    public JPanel panelNums;
    public JTextField textFieldNumberingStart;
    public JPanel hSpacer5;
    public JLabel labelZeros;
    public JTextField textFieldNumberingZeros;
    public JLabel labelNumberingPosition;
    public JPanel panelNumberingPostion;
    public JTextField textFieldNumberingPosition;
    public JPanel hSpacer4;
    public JCheckBox checkBoxNumberingFromEnd;
    public JPanel hSpacer8;
    public JCheckBox checkBoxNumberingAtEnd;
    public JPanel panelNumberingOther;
    public JCheckBox checkBoxNumberingOverwrite;
    public JPanel panelRemoveCard;
    public JLabel labelFrom;
    public JPanel panelFrom;
    public JTextField textFieldFrom;
    public JPanel hSpacer6;
    public JLabel labelLength;
    public JPanel panelLength;
    public JTextField textFieldLength;
    public JPanel panelRemoveOther;
    public JCheckBox checkBoxRemoveFromEnd;
    public JPanel panelReplaceCard;
    public JLabel labelFind;
    public JTextField textFieldFind;
    public JLabel labeReplace;
    public JTextField textFieldReplace;
    public JPanel panelReplaceOther;
    public JCheckBox checkBoxRegularExpr;
    public JCheckBox checkBoxCase;
    public JScrollPane scrollPaneExamples;
    public JTable tableChanges;
    public JPanel panelOptionsButtons;
    public JButton buttonRefresh;
    public JPanel buttonBar;
    public JButton saveButton;
    public JButton cancelButton;
    public ButtonGroup buttonGroupChangeCase;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
