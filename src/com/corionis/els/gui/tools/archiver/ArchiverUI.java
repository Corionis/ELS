package com.corionis.els.gui.tools.archiver;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.ConfigModel;
import com.corionis.els.jobs.Origin;
import com.corionis.els.jobs.Origins;
import com.corionis.els.jobs.Task;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.archiver.ArchiverTool;
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
import javax.swing.filechooser.FileFilter;

public class ArchiverUI extends AbstractToolDialog
{
    private ConfigModel configModel;
    private Context context;
    private ArchiverTool currentTool = null;
    private NavHelp helpDialog;
    private boolean inUpdateOnChange = false;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private ArchiverUI me;
    private SwingWorker<Void, Void> worker;
    private ArchiverTool workerTool = null;
    private boolean workerRunning = false;
    private Task workerTask = null;

    public ArchiverUI(Window owner, Context context)
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
        if (context.preferences.getToolsArchiverXpos() != -1 && Utils.isOnScreen(context.preferences.getToolsArchiverXpos(),
                context.preferences.getToolsArchiverYpos()))
        {
            this.setLocation(context.preferences.getToolsArchiverXpos(), context.preferences.getToolsArchiverYpos());
            Dimension dim = new Dimension(context.preferences.getToolsArchiverWidth(), context.preferences.getToolsArchiverHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        this.splitPaneContent.setDividerLocation(context.preferences.getToolsArchiverDividerLocation());

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
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Archiver.stop.running"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerTool.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Archiver.config.cancelled"), workerTool.getConfigName()));
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
            ArchiverTool origTool = (ArchiverTool) configModel.getValueAt(index, 0);
            String rename = origTool.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                ArchiverTool tool = origTool.clone();
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
                        rename, context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
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

            ArchiverTool tool = (ArchiverTool) configModel.getValueAt(index, 0);
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
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("Archiver.help"), "archiver_" + context.preferences.getLocale() + ".html", false);
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
            ArchiverTool tool = new ArchiverTool(context);
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
                    context.cfg.gs("Z.untitled"), context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
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
                            context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int count = origins.size();
                if (origins != null && origins.size() > 0)
                {
                    final ArchiverTool tool = (ArchiverTool) configModel.getValueAt(index, 0);

                    if (tool.isDataChanged())
                    {
                        JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.save.then.run"), context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    // make dialog pieces
                    String which = context.cfg.gs("Z.publisher");
                    String message = java.text.MessageFormat.format(context.cfg.gs("Archiver.run.on.N.locations"), tool.getConfigName(), count, which);
                    Object[] params = {message};

                    // confirm run of tool
                    int reply = JOptionPane.showConfirmDialog(this, params, context.cfg.gs("ArchiverUI.title"), JOptionPane.YES_NO_OPTION);
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
                                            JOptionPane.showMessageDialog(me, msg, context.cfg.gs("ArchiverUI.title"), JOptionPane.ERROR_MESSAGE);
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
                    JOptionPane.showMessageDialog(this, context.cfg.gs("Archiver.nothing.selected.in.browser"),
                            context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
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
                        JOptionPane.showMessageDialog(this, msg, context.cfg.gs("ArchiverUI.title"), JOptionPane.ERROR_MESSAGE);
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

    private void actionSelectTargetClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                return true;
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("ArchiverUI.selectTarget.description");
            }
        });

        fc.setDialogTitle(context.cfg.gs("ArchiverUI.selectTarget.title"));
        fc.setFileHidingEnabled(false);
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (currentTool.getTarget().length() > 0)
        {
            File target;
            String path = currentTool.getTargetDirectory();
            path = Utils.getLastExistingPath(path, null);
            if (!path.isEmpty())
                target = new File(path);
            else
                target = new File(context.cfg.getWorkingDirectory());
            fc.setCurrentDirectory(target);
        }
        else
        {
            File ld = new File(context.cfg.getWorkingDirectory());
            if (ld.exists() && ld.isDirectory())
                fc.setCurrentDirectory(ld);
        }

        int selection = fc.showOpenDialog(me);
        if (selection == JFileChooser.APPROVE_OPTION)
        {
            String path = fc.getSelectedFile().getAbsolutePath();
            path = context.cfg.makeRelativePath(path);
            textFieldTarget.setText(path);
            updateOnChange(textFieldTarget);
        }
    }

    public void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((ArchiverTool) configModel.getValueAt(i, 0)).reset();
            ((ArchiverTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }

        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
    }

    public boolean checkForChanges()
    {
        if (!deletedTools.isEmpty())
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((ArchiverTool) configModel.getValueAt(i, 0)).isDataChanged())
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
            toolList = context.tools.loadAllTools(context, ArchiverTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                ArchiverTool archiverTool = (ArchiverTool) tool;
                configModel.addRow(new Object[]{ archiverTool });
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("ArchiverUI.title"), JOptionPane.ERROR_MESSAGE);
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
            currentTool = (ArchiverTool) configModel.getValueAt(index, 0);
            checkBoxAppendPubsub.setSelected(currentTool.isAppendPubSub());
            checkBoxAppendDate.setSelected(currentTool.isAppendDate());
            checkBoxDeleteFiles.setSelected(currentTool.isDeleteFiles());
            comboBoxFormat.setSelectedItem(currentTool.getFormat());
            textFieldTarget.setText(currentTool.getTarget());
            textFieldTarget.setEnabled(true);

            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);
            buttonRun.setEnabled(true);
            String example = currentTool.formatArchiveFilename(context.publisherRepo, context.subscriberRepo);
            if (example.length() == 0)
                example = context.cfg.gs("Z.empty");
            labelStatus.setText(context.cfg.gs("Archiver.example") + " " + example);
            labelStatus.setToolTipText(example);
        }
        else
        {
            currentTool = null;
            textFieldTarget.setEnabled(false);
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            labelStatus.setText("  ");
        }
    }

    private void processSelected(ArchiverTool tool, ArrayList<Origin> origins) throws Exception
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
        ArchiverTool tool = null;
        try
        {
            // check that Target is populated
            for (int i = 0; i < configModel.getRowCount(); ++i)
            {
                tool = (ArchiverTool) configModel.getValueAt(i, 0);
                if (tool.getTarget().isEmpty() && !currentTool.isAppendPubSub() && !currentTool.isAppendDate())
                {
                    JOptionPane.showMessageDialog(this, context.cfg.gs("Archiver.missing.target"),
                            context.cfg.gs("ArchiverUI.title"), JOptionPane.WARNING_MESSAGE);
                    configItems.setRowSelectionInterval(i, i);
                    loadTool(i);
                    return false;
                }
            }

            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = (ArchiverTool) deletedTools.get(i);
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
                tool = (ArchiverTool) configModel.getValueAt(i, 0);
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
                JOptionPane.showMessageDialog(this, msg, context.cfg.gs("ArchiverUI.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
        return true;
    }

    private void savePreferences()
    {
        context.preferences.setToolsArchiverHeight(this.getHeight());
        context.preferences.setToolsArchiverWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsArchiverXpos(location.x);
        context.preferences.setToolsArchiverYpos(location.y);
        context.preferences.setToolsArchiverDividerLocation(splitPaneContent.getDividerLocation());
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
                    case "target":
                        current = currentTool.getTarget();
                        value = tf.getText();
                        currentTool.setTarget(value);
                        break;
                }
            }
            else if (source instanceof JComboBox)
            {
                JComboBox combo = (JComboBox) source;
                name = combo.getName();
                switch (name.toLowerCase())
                {
                    case "format":
                        current = currentTool.getFormat();
                        value = comboBoxFormat.getSelectedItem().toString();
                        currentTool.setFormat(value);
                        break;
                }
            }
            else if (source instanceof JCheckBox)
            {
                JCheckBox checkBox = (JCheckBox) source;
                name = checkBox.getName();
                boolean state;
                switch (name.toLowerCase())
                {
                    case "appendpubsub":
                        current = (currentTool.isAppendPubSub() ? "true" : "false");
                        state = checkBox.isSelected();
                        currentTool.setAppendPubSub(state);
                        break;
                    case "deletefiles":
                        current = (currentTool.isDeleteFiles() ? "true" : "false");
                        state = checkBox.isSelected();
                        currentTool.setDeleteFiles(state);
                        break;
                    case "appenddate":
                        current = (currentTool.isAppendDate() ? "true" : "false");
                        state = checkBox.isSelected();
                        currentTool.setAppendDate(state);
                        break;
                }
            }
            if (!current.equals(value))
            {
                currentTool.setDataHasChanged();
            }
        }
        String example = currentTool.formatArchiveFilename(context.publisherRepo, context.subscriberRepo);
        if (example.length() == 0)
            example = context.cfg.gs("Z.empty");
        labelStatus.setText(context.cfg.gs("Archiver.example") + " " + example);
        labelStatus.setToolTipText(example);
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
        panelArchiver = new JPanel();
        labelPanelTitle = new JLabel();
        labelAppendPubSub = new JLabel();
        checkBoxAppendPubsub = new JCheckBox();
        labelAppendDate = new JLabel();
        checkBoxAppendDate = new JCheckBox();
        labelDeleteFiles = new JLabel();
        checkBoxDeleteFiles = new JCheckBox();
        labelFormat = new JLabel();
        comboBoxFormat = new JComboBox<>();
        labelTarget = new JLabel();
        textFieldTarget = new JTextField();
        buttonSelectTarget = new JButton();
        panelOptionsButtons = new JPanel();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("ArchiverUI.title"));
        setName("archiverUI");
        setMinimumSize(new Dimension(150, 126));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ArchiverUI.this.windowClosing(e);
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
            dialogPane.setPreferredSize(new Dimension(700, 365));
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

                        //---- buttonNew ----
                        buttonNew.setText(context.cfg.gs("ArchiverUI.buttonNew.text"));
                        buttonNew.setMnemonic(context.cfg.gs("ArchiverUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("ArchiverUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("ArchiverUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("ArchiverUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("ArchiverUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("ArchiverUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("ArchiverUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("ArchiverUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(context.cfg.gs("ArchiverUI.buttonRun.text"));
                        buttonRun.setMnemonic(context.cfg.gs("ArchiverUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(context.cfg.gs("ArchiverUI.buttonRun.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("ArchiverUI.labelHelp.toolTipText"));
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

                            //======== panelArchiver ========
                            {
                                panelArchiver.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelArchiver.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
                                ((GridBagLayout)panelArchiver.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                //---- labelPanelTitle ----
                                labelPanelTitle.setText(context.cfg.gs("ArchiverUI.labelPanelTitle.text"));
                                labelPanelTitle.setFont(labelPanelTitle.getFont().deriveFont(labelPanelTitle.getFont().getStyle() | Font.BOLD, labelPanelTitle.getFont().getSize() + 1f));
                                panelArchiver.add(labelPanelTitle, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(4, 0, 6, 4), 0, 0));

                                //---- labelAppendPubSub ----
                                labelAppendPubSub.setText(context.cfg.gs("ArchiverUI.labelAppendPubSub.text"));
                                panelArchiver.add(labelAppendPubSub, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 16), 0, 0));

                                //---- checkBoxAppendPubsub ----
                                checkBoxAppendPubsub.setToolTipText(context.cfg.gs("ArchiverUI.checkBoxAppendPubsub.toolTipText"));
                                checkBoxAppendPubsub.setName("appendPubSub");
                                checkBoxAppendPubsub.addActionListener(e -> genericAction(e));
                                panelArchiver.add(checkBoxAppendPubsub, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 4), 0, 0));

                                //---- labelAppendDate ----
                                labelAppendDate.setText(context.cfg.gs("ArchiverUI.labelAppendDate.text"));
                                panelArchiver.add(labelAppendDate, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 16), 0, 0));

                                //---- checkBoxAppendDate ----
                                checkBoxAppendDate.setToolTipText(context.cfg.gs("ArchiverUI.checkBoxAppendDate.toolTipText"));
                                checkBoxAppendDate.setName("appendDate");
                                checkBoxAppendDate.addActionListener(e -> genericAction(e));
                                panelArchiver.add(checkBoxAppendDate, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 4), 0, 0));

                                //---- labelDeleteFiles ----
                                labelDeleteFiles.setText(context.cfg.gs("ArchiverUI.labelDeleteFiles.text"));
                                panelArchiver.add(labelDeleteFiles, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(8, 4, 4, 16), 0, 0));

                                //---- checkBoxDeleteFiles ----
                                checkBoxDeleteFiles.setToolTipText(context.cfg.gs("ArchiverUI.checkBoxDeleteFiles.toolTipText"));
                                checkBoxDeleteFiles.setName("deleteFiles");
                                checkBoxDeleteFiles.addActionListener(e -> genericAction(e));
                                panelArchiver.add(checkBoxDeleteFiles, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(8, 0, 4, 4), 0, 0));

                                //---- labelFormat ----
                                labelFormat.setText(context.cfg.gs("ArchiverUI.labelFormat.text"));
                                panelArchiver.add(labelFormat, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- comboBoxFormat ----
                                comboBoxFormat.setToolTipText(context.cfg.gs("ArchiverUI.comboBoxFormat.toolTipText"));
                                comboBoxFormat.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "tar",
                                    "zip"
                                }));
                                comboBoxFormat.setName("format");
                                comboBoxFormat.addActionListener(e -> genericAction(e));
                                panelArchiver.add(comboBoxFormat, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                    new Insets(0, 0, 4, 4), 0, 0));

                                //---- labelTarget ----
                                labelTarget.setText(context.cfg.gs("ArchiverUI.labelTarget.text"));
                                panelArchiver.add(labelTarget, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 4), 0, 0));

                                //---- textFieldTarget ----
                                textFieldTarget.setPreferredSize(new Dimension(240, 34));
                                textFieldTarget.setMinimumSize(new Dimension(240, 34));
                                textFieldTarget.setToolTipText(context.cfg.gs("ArchiverUI.textFieldTarget.toolTipText"));
                                textFieldTarget.setName("target");
                                textFieldTarget.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        genericTextFieldFocusLost(e);
                                    }
                                });
                                panelArchiver.add(textFieldTarget, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 4), 0, 0));

                                //---- buttonSelectTarget ----
                                buttonSelectTarget.setText(context.cfg.gs("Z.ellipsis"));
                                buttonSelectTarget.setMinimumSize(new Dimension(32, 24));
                                buttonSelectTarget.setMaximumSize(new Dimension(32, 24));
                                buttonSelectTarget.setPreferredSize(new Dimension(32, 24));
                                buttonSelectTarget.setToolTipText(context.cfg.gs("ArchiverUI.buttonSelectTarget.toolTipText"));
                                buttonSelectTarget.addActionListener(e -> actionSelectTargetClicked(e));
                                panelArchiver.add(buttonSelectTarget, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            scrollPaneOptions.setViewportView(panelArchiver);
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
                saveButton.setText(context.cfg.gs("ArchiverUI.saveButton.text"));
                saveButton.setToolTipText(context.cfg.gs("ArchiverUI.button.save.toolTipText"));
                saveButton.setMnemonic('S');
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("ArchiverUI.cancelButton.text"));
                cancelButton.setToolTipText(context.cfg.gs("ArchiverUI.button.cancel.tooltip"));
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
    private JPanel panelArchiver;
    private JLabel labelPanelTitle;
    private JLabel labelAppendPubSub;
    private JCheckBox checkBoxAppendPubsub;
    private JLabel labelAppendDate;
    private JCheckBox checkBoxAppendDate;
    private JLabel labelDeleteFiles;
    private JCheckBox checkBoxDeleteFiles;
    private JLabel labelFormat;
    private JComboBox<String> comboBoxFormat;
    private JLabel labelTarget;
    private JTextField textFieldTarget;
    private JButton buttonSelectTarget;
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
