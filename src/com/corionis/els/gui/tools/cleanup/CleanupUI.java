package com.corionis.els.gui.tools.cleanup;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.ConfigModel;
import com.corionis.els.gui.util.NumberFilter;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Origins;
import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.cleanup.CleanupTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.PlainDocument;

public class CleanupUI extends AbstractToolDialog
{
    private ConfigModel configModel;
    private Context context;
    private CleanupTool currentTool = null;
    private NavHelp helpDialog;
    private boolean inUpdateOnChange = false;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private CleanupUI me;
    private SwingWorker<Void, Void> worker;
    private CleanupTool workerTool = null;
    private boolean workerRunning = false;
    private Task workerTask = null;

    public CleanupUI(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        this.me = this;

        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position, size & divider
        if (context.preferences.getToolsCleanupXpos() != -1 && Utils.isOnScreen(context.preferences.getToolsCleanupXpos(),
                context.preferences.getToolsCleanupYpos()))
        {
            this.setLocation(context.preferences.getToolsCleanupXpos(), context.preferences.getToolsCleanupYpos());
            Dimension dim = new Dimension(context.preferences.getToolsCleanupWidth(), context.preferences.getToolsCleanupHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        this.splitPaneContent.setDividerLocation(context.preferences.getToolsCleanupDividerLocation());

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

        PlainDocument pd = (PlainDocument) textFieldMaxAge.getDocument();
        pd.setDocumentFilter(new NumberFilter());

        // setup the left-side list of configurations
        configModel = new ConfigModel(context, this);
        configModel.setColumnCount(1);
        configItems.setModel(configModel);

        configItems.getTableHeader().setUI(null);
        configItems.setTableHeader(null);
        scrollPaneConfig.setColumnHeaderView(null);

        loadConfigurations();
        context.navigator.enableDisableToolMenus(this, false);
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && workerTool != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Cleanup.stop.running"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerTool.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Cleanup.config.cancelled"), workerTool.getConfigName()));
            }
        }
        else
        {
            if (checkForChanges())
            {
                Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no")};
                int reply = JOptionPane.showOptionDialog(this,
                        context.cfg.gs("Z.cancel.all.changes"),
                        getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, opts, opts[1]);
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

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            CleanupTool origTool = (CleanupTool) configModel.getValueAt(index, 0);
            String rename = origTool.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                CleanupTool tool = origTool.clone();
                tool.setConfigName(rename);
                tool.setDataHasChanged();
                configModel.addRow(new Object[]{ tool });

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            if (configItems.isEditing())
                configItems.getCellEditor().stopCellEditing();

            CleanupTool tool = (CleanupTool) configModel.getValueAt(index, 0);
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
                    configModel.fireTableDataChanged();
                    if (index >= 0)
                    {
                        configItems.changeSelection(index, 0, false, false);
                    }
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("Cleanup.help"), "cleanup_" + context.preferences.getLocale() + ".html", false);
            if (!helpDialog.fault)
                helpDialog.buttonFocus();
        }
        else
        {
            helpDialog.setVisible(true);
            helpDialog.toFront();
            helpDialog.requestFocus();
            helpDialog.buttonFocus();
        }
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            CleanupTool tool = new CleanupTool(context);
            tool.setConfigName(context.cfg.gs("Z.untitled"));
            tool.setDataHasChanged();

            configModel.addRow(new Object[]{ tool });

            if (configModel.getRowCount() > 0)
            {
                loadTool(configItems.getRowCount() - 1);
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
                buttonRun.setEnabled(true);
            }

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionRunClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            try
            {
                ArrayList<Origin> origins = new ArrayList<Origin>();
                boolean isSubscriber = Origins.makeOriginsFromSelected(context, this, origins);
                if (isSubscriber)
                {
                    JOptionPane.showMessageDialog(this, context.cfg.gs(context.cfg.gs("Z.this.tool.is.for.the.local.publisher.only")),
                            context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int count = origins.size();
                if (origins != null && origins.size() > 0)
                {
                    final CleanupTool tool = (CleanupTool) configModel.getValueAt(index, 0);

                    if (tool.isDataChanged())
                    {
                        JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.save.then.run"), context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // make dialog pieces
                    String which = context.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(context.cfg.gs("Cleanup.run.on.N.locations"), tool.getConfigName(), count, which);
                    Object[] params = {message};

                    // confirm run of tool
                    int reply = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("CleanupUI.title"), JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                        setComponentEnabled(false);
                        cancelButton.setEnabled(true);
                        cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        labelHelp.setEnabled(true);
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                        worker = new SwingWorker<Void, Void>()
                        {
                            @Override
                            protected Void doInBackground() throws Exception
                            {
                                try
                                {
                                    workerTool = tool.clone();
                                    processSelected(workerTool, origins);
                                }
                                catch (Exception e)
                                {
                                    if (!e.getMessage().equals("HANDLED_INTERNALLY"))
                                    {
                                        String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
                                        if (context != null)
                                        {
                                            logger.error(msg);
                                            JOptionPane.showMessageDialog(me, msg, context.cfg.gs("CleanupUI.title"), JOptionPane.ERROR_MESSAGE);
                                        }
                                        else
                                            logger.error(msg);
                                    }
                                }
                                return null;
                            }
                        };

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
                                            processTerminated();
                                    }
                                }
                            });
                            worker.execute();
                        }
                    }
                }
                else
                {
                    JOptionPane.showMessageDialog(this, context.cfg.gs("Cleanup.nothing.selected.in.browser"),
                            context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
                }
            }
            catch (Exception e1)
            {
                if (!e1.getMessage().equals("HANDLED_INTERNALLY"))
                {
                    String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e1);
                    if (context != null)
                    {
                        logger.error(msg);
                        JOptionPane.showMessageDialog(this, msg, context.cfg.gs("CleanupUI.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    else
                        logger.error(msg);
                }
            }
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        if (saveConfigurations())
        {
            savePreferences();
            setVisible(false);
        }
    }

    public void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((CleanupTool) configModel.getValueAt(i, 0)).reset();
            ((CleanupTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }

        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
    }

    public boolean checkForChanges()
    {
        if (!deletedTools.isEmpty())
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((CleanupTool) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    private void configItemsMouseClicked(MouseEvent e)
    {
        if (e.getClickCount() == 1)
        {
            int index = configItems.getSelectedRow();
            loadTool(index);
        }
    }

    public void genericAction(ActionEvent e)
    {
        updateOnChange(e.getSource());
    }

    public void genericTextFieldFocusLost(FocusEvent e)
    {
        updateOnChange(e.getSource());
    }

    @Override
    public JTable getConfigItems()
    {
        return configItems;
    }

    private void loadConfigurations()
    {
        ArrayList<AbstractTool> toolList = null;
        try
        {
            toolList = context.tools.loadAllTools(context, CleanupTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                CleanupTool cleanupTool = (CleanupTool) tool;
                configModel.addRow(new Object[]{ cleanupTool });
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("CleanupUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        configModel.setToolList(toolList);
        configModel.loadJobsConfigurations(this, null);

        if (configModel.getRowCount() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
        }
        else
        {
            loadTool(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadTool(int index)
    {
        if (index >= 0 && index < configModel.getRowCount())
        {
            currentTool = (CleanupTool) configModel.getValueAt(index, 0);
            textFieldMaxAge.setText(String.valueOf(currentTool.getAge()));
            textFieldMaxAge.setEnabled(true);

            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);
            buttonRun.setEnabled(true);
        }
        else
        {
            currentTool = null;
            textFieldMaxAge.setEnabled(false);
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            labelStatus.setText("  ");
        }
    }

    private void processSelected(CleanupTool tool, ArrayList<Origin> origins) throws Exception
    {
        if (tool != null)
        {
            if (origins != null && origins.size() > 0)
            {
                workerTask = new Task(tool.getInternalName(), tool.getConfigName());
                workerTask.setContext(tool.getContext());
                workerTask.setOrigins(origins);
                workerTask.setPublisherKey(Task.ANY_SERVER);

                workerTask.process(context);
            }
        }
    }

    private void processTerminated()
    {
        Origins.setSelectedFromOrigins(context, this, workerTask.getOrigins());

        setComponentEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

        if (workerTool.isRequestStop())
        {
            logger.info(workerTool.getConfigName() + context.cfg.gs("Z.cancelled"));
            labelStatus.setText(workerTool.getConfigName() + context.cfg.gs("Z.cancelled"));
        }

        workerRunning = false;
        workerTool = null;
    }

    private boolean saveConfigurations()
    {
        CleanupTool tool = null;
        try
        {
            // check that Target is populated
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (CleanupTool) configModel.getValueAt(i, 0);
                if (tool.getAge() < 0)
                {
                    JOptionPane.showMessageDialog(this, context.cfg.gs("CleanupUI.bad.value"),
                            context.cfg.gs("CleanupUI.title"), JOptionPane.WARNING_MESSAGE);
                    configItems.setRowSelectionInterval(i, i);
                    loadTool(i);
                    return false;
                }
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = (CleanupTool) deletedTools.get(i);
                File file = new File(tool.getFullPath());
                if (file.exists())
                {
                    file.delete();
                }
            }
            deletedTools = new ArrayList<AbstractTool>();

            // write/update changed tool JSON configuration files
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (CleanupTool) configModel.getValueAt(i, 0);
                if (tool.isDataChanged())
                    tool.write();
                tool.setDataHasChanged(false);
            }

            // write/update changed Job JSON configuration files
            configModel.saveJobsConfigurations(null);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("CleanupUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
        return true;
    }

    private void savePreferences()
    {
        context.preferences.setToolsCleanupHeight(this.getHeight());
        context.preferences.setToolsCleanupWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsCleanupXpos(location.x);
        context.preferences.setToolsCleanupYpos(location.y);
        context.preferences.setToolsCleanupDividerLocation(splitPaneContent.getDividerLocation());
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

    private void updateOnChange(Object source)
    {
        if (inUpdateOnChange || loading)
            return;

        inUpdateOnChange = true;
        String name = null;
        if (source != null && currentTool != null && !loading)
        {
            String current = "";
            String value = "";
            if (source instanceof JTextField)
            {
                JTextField tf = (JTextField) source;
                name = tf.getName();
                switch (name.toLowerCase())
                {
                    case "age":
                        current = String.valueOf(currentTool.getAge());
                        value = tf.getText();
                        currentTool.setAge(Integer.parseInt(value));
                        break;
                }
            }
            if (!current.equals(value))
            {
                currentTool.setDataHasChanged();
            }
        }
        inUpdateOnChange = false;
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
        scrollPaneOptions = new JScrollPane();
        panelCleanup = new JPanel();
        labelPanelTitle = new JLabel();
        labelAge = new JLabel();
        textFieldMaxAge = new JTextField();
        panelOptionsButtons = new JPanel();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("CleanupUI.title"));
        setName("cleanupUI");
        setMinimumSize(new Dimension(150, 126));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                CleanupUI.this.windowClosing(e);
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                windowHidden(e);
            }
        });
        var contentPane = getContentPane();
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
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(context.cfg.gs("CleanupUI.buttonNew.text"));
                        buttonNew.setMnemonic(context.cfg.gs("CleanupUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("CleanupUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("CleanupUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("CleanupUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("CleanupUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("CleanupUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("CleanupUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("CleanupUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("CleanupUI.buttonRun.text"));
                        buttonRun.setMnemonic(context.cfg.gs("CleanupUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("CleanupUI.buttonRun.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("CleanupUI.labelHelp.toolTipText"));
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

                        //======== scrollPaneOptions ========
                        {

                            //======== panelCleanup ========
                            {
                                panelCleanup.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelCleanup.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
                                ((GridBagLayout)panelCleanup.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                //---- labelPanelTitle ----
                                labelPanelTitle.setText(context.cfg.gs("CleanupUI.labelPanelTitle.text"));
                                labelPanelTitle.setFont(labelPanelTitle.getFont().deriveFont(labelPanelTitle.getFont().getStyle() | Font.BOLD, labelPanelTitle.getFont().getSize() + 1f));
                                panelCleanup.add(labelPanelTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(4, 0, 6, 4), 0, 0));

                                //---- labelAge ----
                                labelAge.setText(context.cfg.gs("CleanupUI.labelAge.text"));
                                panelCleanup.add(labelAge, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 16), 0, 0));

                                //---- textFieldMaxAge ----
                                textFieldMaxAge.setPreferredSize(new Dimension(64, 34));
                                textFieldMaxAge.setMinimumSize(new Dimension(64, 34));
                                textFieldMaxAge.setName("age");
                                textFieldMaxAge.addActionListener(e -> genericAction(e));
                                textFieldMaxAge.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        genericTextFieldFocusLost(e);
                                    }
                                });
                                panelCleanup.add(textFieldMaxAge, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 4), 0, 0));
                            }
                            scrollPaneOptions.setViewportView(panelCleanup);
                        }
                        panelOptions.add(scrollPaneOptions, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));
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
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};
                buttonBar.add(labelStatus, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- saveButton ----
                saveButton.setText(context.cfg.gs("CleanupUI.saveButton.text"));
                saveButton.setToolTipText(context.cfg.gs("CleanupUI.button.save.toolTipText"));
                saveButton.setMnemonic('S');
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("CleanupUI.cancelButton.text"));
                cancelButton.setToolTipText(context.cfg.gs("CleanupUI.button.cancel.tooltip"));
                cancelButton.setMnemonic('L');
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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
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
    private JPanel panelCleanup;
    private JLabel labelPanelTitle;
    private JLabel labelAge;
    private JTextField textFieldMaxAge;
    private JPanel panelOptionsButtons;
    private JPanel buttonBar;
    public JLabel labelStatus;
    private JButton saveButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    //
    // @formatter:on
    // </editor-fold>
}
