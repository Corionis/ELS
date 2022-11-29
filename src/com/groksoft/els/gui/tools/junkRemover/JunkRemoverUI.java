package com.groksoft.els.gui.tools.junkRemover;

import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Origins;
import com.groksoft.els.jobs.Task;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.Tools;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class JunkRemoverUI extends JDialog
{
    private JunkConfigModel configModel;
    private ArrayList<JunkRemoverTool> deletedTools;
    private GuiContext guiContext;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private boolean isDryRun;
    private boolean isSubscriber;
    private SwingWorker<Void, Void> worker;
    private JunkRemoverTool workerJrt = null;
    private boolean workerRunning = false;

    private JunkRemoverUI()
    {
        // hide default constructor
    }

    public JunkRemoverUI(Window owner, GuiContext guiContext)
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
        if (guiContext.preferences.getToolsJunkRemoverXpos() > 0)
        {
            this.setLocation(guiContext.preferences.getToolsJunkRemoverXpos(), guiContext.preferences.getToolsJunkRemoverYpos());
            Dimension dim = new Dimension(guiContext.preferences.getToolsJunkRemoverWidth(), guiContext.preferences.getToolsJunkRemoverHeight());
            this.setSize(dim);
            this.splitPaneContent.setDividerLocation(guiContext.preferences.getToolsJunkRemoverDividerLocation());
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
        configModel = new JunkConfigModel(guiContext, this);
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
                    loadJunkTable(index);
                }
            }
        });

        adjustJunkTable();
        loadConfigurations();
        deletedTools = new ArrayList<JunkRemoverTool>();
    }

    private void actionAddRowClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        if (((JunkTableModel) tableJunk.getModel()).getTool() != null)
        {
            ((JunkTableModel) tableJunk.getModel()).getTool().addJunkItem();
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
            tableJunk.requestFocus();
            int col = 0;
            int row =  ((JunkTableModel) tableJunk.getModel()).find("");
            if (row >= 0)
            {
                row = tableJunk.convertRowIndexToView(row);
                tableJunk.changeSelection(row, col, false, false);
                tableJunk.editCellAt(row, col);
                if (tableJunk.getEditorComponent() != null)
                    tableJunk.getEditorComponent().requestFocus();
            }
        }
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && workerJrt != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("JunkRemover.stop.running.junk.remover"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerJrt.requestStop();
                guiContext.browser.printLog(java.text.MessageFormat.format(guiContext.cfg.gs("JunkRemover.config.cancelled"), workerJrt.getConfigName()));
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
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            JunkRemoverTool origJrt = (JunkRemoverTool) configModel.getValueAt(index, 0);
            String rename = origJrt.getConfigName() + guiContext.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                JunkRemoverTool jrt = origJrt.clone();
                jrt.setConfigName(rename);
                jrt.setDataHasChanged();
                jrt.addJunkItem();
                configModel.addRow(new Object[]{ jrt });

                // clear patterns table
                ((JunkTableModel) tableJunk.getModel()).setTool(null);
                tableJunk.removeAll();
                ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

                // set patterns table
                ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
                ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                        rename, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            JunkRemoverTool jrt = (JunkRemoverTool) configModel.getValueAt(index, 0);

            // TODO check if Tool is used in any Jobs, prompt user accordingly AND handle for rename too

            int reply = JOptionPane.showConfirmDialog(this, guiContext.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + jrt.getConfigName(),
                    guiContext.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                deletedTools.add(jrt);
                configModel.removeRow(index);
                configModel.fireTableDataChanged();
                if (index > 0)
                    index = configModel.getRowCount() - 1;
                configItems.requestFocus();
                if (index >= 0)
                {
                    configItems.changeSelection(index, 0, false, false);
                    loadJunkTable(index);
                }
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, guiContext, guiContext.cfg.gs("JunkRemover.help"), "junkremover_" + guiContext.preferences.getLocale() + ".html");
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
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        if (configModel.find(guiContext.cfg.gs("Z.untitled"), null) == null)
        {
            JunkRemoverTool jrt = new JunkRemoverTool(guiContext, guiContext.cfg, guiContext.context);
            jrt.setConfigName(guiContext.cfg.gs("Z.untitled"));
            jrt.setDataHasChanged();
            jrt.addJunkItem();

            configModel.addRow(new Object[]{ jrt });

            // clear patterns table
            ((JunkTableModel) tableJunk.getModel()).setTool(null);
            tableJunk.removeAll();
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

            // set patterns table
            ((JunkTableModel) tableJunk.getModel()).setTool(jrt);
            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();

            if (configModel.getRowCount() > 0)
            {
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
                buttonRun.setEnabled(true);
                buttonAddRow.setEnabled(true);
                buttonRemoveRow.setEnabled(true);
            }

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.please.rename.the.existing") +
                    guiContext.cfg.gs("Z.untitled"), guiContext.cfg.gs("JunkRemover.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionRemoveRowClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }

        // convert selected rows to model-based indices
        int rows[] = tableJunk.getSelectedRows();
        if (rows.length > 0)
        {
            for (int i = 0; i < rows.length; ++i)
            {
                int rm = tableJunk.convertRowIndexToModel(rows[i]);
                rows[i] = rm;
            }

            // sort the indices ascending
            Arrays.sort(rows);

            // iterate in reverse so the order does not change as items are removed
            for (int i = rows.length - 1; i >= 0; --i)
            {
                ((JunkTableModel) tableJunk.getModel()).removeRow(rows[i]);
            }

            int index = configItems.getSelectedRow();
            if (index >= 0)
            {
                JunkRemoverTool jrt = (JunkRemoverTool) configModel.getValueAt(index, 0);
                jrt.setDataHasChanged();
            }

            ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
            tableJunk.requestFocus();
            if (tableJunk.getRowCount() > 0)
                tableJunk.changeSelection(0, 0, false, false);
        }
    }

    private void actionRunClicked(ActionEvent e)
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            JunkRemoverTool tool = (JunkRemoverTool) configModel.getValueAt(index, 0);
            workerJrt = tool.clone();
            processSelected(workerJrt);
        }
    }

    private void adjustJunkTable()
    {
        tableJunk.setModel(new JunkTableModel(guiContext.cfg));
        tableJunk.getTableHeader().setReorderingAllowed(false);

        // junk pattern column
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer();
        TableColumn column = tableJunk.getColumnModel().getColumn(0);
        cellRenderer.setHorizontalAlignment(JLabel.LEFT);
        column.setMinWidth(32);
        column.setCellRenderer(cellRenderer);
        column.setResizable(true);

        // case-sensitive column
        column = tableJunk.getColumnModel().getColumn(1);
        column.setResizable(false);
        column.setWidth(62);
        column.setPreferredWidth(62);
        column.setMaxWidth(62);
        column.setMinWidth(62);

        // tab to move to next row & add new row if needed
        tableJunk.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), "Action.NextCell");
        tableJunk.getActionMap().put("Action.NextCell", new NextCellAction());
    }

    public boolean checkForChanges()
    {
        if (tableJunk.isEditing())
        {
            tableJunk.getCellEditor().stopCellEditing();
        }

        if (deletedTools.size() > 0)
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((JunkRemoverTool) configModel.getValueAt(i, 0)).isDataChanged())
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
            if (tableJunk.isEditing())
            {
                tableJunk.getCellEditor().stopCellEditing();
            }

            int index = src.getSelectedRow();
            loadJunkTable(index);
        }
    }

    public ArrayList<JunkRemoverTool> getDeletedTools()
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
            ArrayList<AbstractTool> toolList = tools.loadAllTools(guiContext, JunkRemoverTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                JunkRemoverTool jrt = (JunkRemoverTool) tool;
                configModel.addRow(new Object[]{ jrt });
            }
        }
        catch (Exception e)
        {
            String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (guiContext != null)
            {
                guiContext.browser.printLog(msg, true);
                JOptionPane.showMessageDialog(guiContext.navigator.dialogJunkRemover, msg, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        if (configModel.getRowCount() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonAddRow.setEnabled(false);
            buttonRemoveRow.setEnabled(false);
        }
        else
        {
            loadJunkTable(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadJunkTable(int index)
    {
        if (index >= 0 && index < configModel.getRowCount())
        {
            JunkRemoverTool jrt = (JunkRemoverTool) configModel.getValueAt(index, 0);
            JunkTableModel model = (JunkTableModel) tableJunk.getModel();
            model.setTool(jrt);
            model.fireTableDataChanged();
        }
        else
        {
            JunkTableModel model = (JunkTableModel) tableJunk.getModel();
            for (int i = model.getRowCount() - 1; i >= 0; --i)
            {
                model.removeRow(i);
            }
            model.setTool(null);
            model.fireTableDataChanged();
        }
    }

    public void processSelected(JunkRemoverTool jrt)
    {
        if (jrt != null)
        {
            try
            {
                ArrayList<Origin> origins = new ArrayList<Origin>();
                isSubscriber = Origins.makeOriginsFromSelected(this, origins, jrt.isRealOnly());

                if (origins != null && origins.size() > 0)
                {
                    int count = origins.size();

                    // make dialog pieces
                    String which = (isSubscriber) ? guiContext.cfg.gs("Z.subscriber") : guiContext.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(guiContext.cfg.gs("JunkRemover.run.on.N.locations"), jrt.getConfigName(), count, which);
                    JCheckBox checkbox = new JCheckBox(guiContext.cfg.gs("Navigator.dryrun"));
                    checkbox.setToolTipText(guiContext.cfg.gs("Navigator.dryrun.tooltip"));
                    checkbox.setSelected(true);
                    Object[] params = {message, checkbox};

                    // confirm run of tool
                    int reply = JOptionPane.showConfirmDialog(this, params, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.YES_NO_OPTION);
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

                            Task task = new Task(jrt.getInternalName(), jrt.getConfigName());
                            task.setOrigins(origins);

                            if (isSubscriber)
                                task.setSubscriberKey(Task.ANY_SERVER);
                            else
                                task.setPublisherKey(Task.ANY_SERVER);

                            worker = task.process(guiContext, jrt, isDryRun);
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
                                                processTerminated(task, jrt);
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
                                JOptionPane.showMessageDialog(guiContext.navigator.dialogJunkRemover, msg, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
                            }
                            else
                                logger.error(msg);
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, guiContext.cfg.gs("JunkRemover.nothing.selected.in.browser"),
                            guiContext.cfg.gs("JunkRemover.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (Exception e)
            {
                if (!e.getMessage().equals("HANDLED_INTERNALLY"))
                {
                    String msg = guiContext.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                    if (guiContext != null)
                    {
                        guiContext.browser.printLog(msg, true);
                        JOptionPane.showMessageDialog(guiContext.navigator.dialogJunkRemover, msg, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        logger.error(msg);
                }
            }
        }
    }

    private void processTerminated(Task task, JunkRemoverTool jrt)
    {
        if (guiContext.progress != null)
            guiContext.progress.done();

        Origins.setSelectedFromOrigins(guiContext, this, task.getOrigins());

        setComponentEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        workerRunning = false;
        workerJrt = null;

        if (jrt.isRequestStop())
        {
            guiContext.browser.printLog(jrt.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
            guiContext.mainFrame.labelStatusMiddle.setText(jrt.getConfigName() + guiContext.cfg.gs("Z.cancelled"));
        }
        else
        {
            guiContext.browser.printLog(jrt.getConfigName() + guiContext.cfg.gs("Z.completed"));
            guiContext.mainFrame.labelStatusMiddle.setText(jrt.getConfigName() + guiContext.cfg.gs("Z.completed"));
        }
    }

    private void saveConfigurations()
    {
        JunkRemoverTool jrt = null;
        try
        {
            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                jrt = (JunkRemoverTool) configModel.getValueAt(i, 0);
                if (jrt.isDataChanged())
                    jrt.write();
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                jrt = deletedTools.get(i);
                File file = new File(jrt.getFullPath());
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
                JOptionPane.showMessageDialog(guiContext.navigator.dialogJunkRemover, msg, guiContext.cfg.gs("JunkRemover.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    private void savePreferences()
    {
        guiContext.preferences.setToolsJunkRemoverHeight(this.getHeight());
        guiContext.preferences.setToolsJunkRemoverWidth(this.getWidth());
        Point location = this.getLocation();
        guiContext.preferences.setToolsJunkRemoverXpos(location.x);
        guiContext.preferences.setToolsJunkRemoverYpos(location.y);
        guiContext.preferences.setToolsJunkRemoverDividerLocation(splitPaneContent.getDividerLocation());
    }

    public void setComponentEnabled(boolean enabled)
    {
        setComponentEnabled(enabled, getContentPane());
    }

    private void setComponentEnabled(boolean enabled, Component component) {
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

    private class NextCellAction extends AbstractAction
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            int row = tableJunk.getSelectedRow();
            int col = tableJunk.getSelectedColumn();
            int rowCount = tableJunk.getRowCount();
            int colCount = tableJunk.getColumnCount();
            ++col;
            if (col >= colCount)
            {
                col = 0;
                ++row;
            }
            if (row >= rowCount)
            {
                ((JunkTableModel) tableJunk.getModel()).getTool().addJunkItem();
                ((JunkTableModel) tableJunk.getModel()).fireTableDataChanged();
            }
            tableJunk.changeSelection(row, col, false, false);
            tableJunk.editCellAt(row, col);
            tableJunk.getEditorComponent().requestFocus();
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
        scrollPaneOptions = new JScrollPane();
        tableJunk = new JTable();
        panelOptionsButtons = new JPanel();
        buttonAddRow = new JButton();
        buttonRemoveRow = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("JunkRemover.title"));
        setName("junkRemoverUI");
        setMinimumSize(new Dimension(150, 126));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                JunkRemoverUI.this.windowClosing(e);
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
                        buttonNew.setText(guiContext.cfg.gs("JunkRemover.button.New.text"));
                        buttonNew.setMnemonic(guiContext.cfg.gs("JunkRemover.button.New.mnemonic").charAt(0));
                        buttonNew.setToolTipText(guiContext.cfg.gs("JunkRemover.button.New.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JunkRemover.button.Copy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Copy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Copy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JunkRemover.button.Delete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Delete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Delete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("JunkRemover.button.Run.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("JunkRemover.button.Run.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("JunkRemover.button.Run.toolTipText"));
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
                        labelHelp.setToolTipText(guiContext.cfg.gs("JunkRemover.labelHelp.toolTipText"));
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

                        //======== scrollPaneOptions ========
                        {

                            //---- tableJunk ----
                            tableJunk.setShowVerticalLines(false);
                            tableJunk.setName("tableJunk");
                            tableJunk.setAutoCreateRowSorter(true);
                            tableJunk.setCellSelectionEnabled(true);
                            scrollPaneOptions.setViewportView(tableJunk);
                        }
                        panelOptions.add(scrollPaneOptions, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonAddRow ----
                            buttonAddRow.setText(guiContext.cfg.gs("JunkRemover.button.AddRow.text"));
                            buttonAddRow.setFont(buttonAddRow.getFont().deriveFont(buttonAddRow.getFont().getSize() - 2f));
                            buttonAddRow.setPreferredSize(new Dimension(78, 24));
                            buttonAddRow.setMinimumSize(new Dimension(78, 24));
                            buttonAddRow.setMaximumSize(new Dimension(78, 24));
                            buttonAddRow.setMnemonic(guiContext.cfg.gs("JunkRemover.button.AddRow.mnemonic").charAt(0));
                            buttonAddRow.setToolTipText(guiContext.cfg.gs("JunkRemover.button.AddRow.toolTipText"));
                            buttonAddRow.addActionListener(e -> actionAddRowClicked(e));
                            panelOptionsButtons.add(buttonAddRow);

                            //---- buttonRemoveRow ----
                            buttonRemoveRow.setText(guiContext.cfg.gs("JunkRemover.button.RemoveRow.text"));
                            buttonRemoveRow.setFont(buttonRemoveRow.getFont().deriveFont(buttonRemoveRow.getFont().getSize() - 2f));
                            buttonRemoveRow.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveRow.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMnemonic(guiContext.cfg.gs("JunkRemover.button.RemoveRow.mnemonic").charAt(0));
                            buttonRemoveRow.setToolTipText(guiContext.cfg.gs("JunkRemover.button.RemoveRow.toolTipText"));
                            buttonRemoveRow.addActionListener(e -> actionRemoveRowClicked(e));
                            panelOptionsButtons.add(buttonRemoveRow);
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
                okButton.setToolTipText(guiContext.cfg.gs("Z.save.changes.toolTipText"));
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(guiContext.cfg.gs("Z.cancel.changes.toolTipText"));
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
    private JScrollPane scrollPaneOptions;
    private JTable tableJunk;
    private JPanel panelOptionsButtons;
    private JButton buttonAddRow;
    private JButton buttonRemoveRow;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
