package com.corionis.els.gui.tools.sleep;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.ConfigModel;
import com.corionis.els.gui.util.NumberFilter;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.sleep.SleepTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.PlainDocument;

public class SleepUI extends AbstractToolDialog
{
    private ConfigModel configModel;
    private Context context;
    private SleepTool currentSleepTool = null;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private NumberFilter numberFilter;
    private SleepTool workerTool = null;
    private boolean workerRunning = false;

    public SleepUI(Window owner, Context context)
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
        if (context.preferences.getToolsSleepXpos() != -1 && Utils.isOnScreen(context.preferences.getToolsSleepXpos(),
                context.preferences.getToolsSleepYpos()))
        {
            this.setLocation(context.preferences.getToolsSleepXpos(), context.preferences.getToolsSleepYpos());
            Dimension dim = new Dimension(context.preferences.getToolsSleepWidth(), context.preferences.getToolsSleepHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        this.splitPaneContent.setDividerLocation(context.preferences.getToolsSleepDividerLocation());

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
                    loadTime(index);
                }
            }
        });

        loadConfigurations();
        numberFilter = new NumberFilter();
        setNumberFilter(textFieldTime);
        context.navigator.enableDisableToolMenus(this, false);
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (workerRunning && workerTool != null)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Sleep.stop.running.sleep"),
                    "Z.cancel.run", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                workerTool.requestStop();
                logger.info(java.text.MessageFormat.format(context.cfg.gs("Sleep.sleep.cancelled"), workerTool.getConfigName()));
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
            currentSleepTool = (SleepTool) configModel.getValueAt(index, 0);
            String rename = currentSleepTool.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                SleepTool tool = currentSleepTool.clone();
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
                        rename, context.cfg.gs("Sleep.title"), JOptionPane.WARNING_MESSAGE);
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

            SleepTool tool = (SleepTool) configModel.getValueAt(index, 0);

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
                        tool.setDataHasChanged(false);
                    }

                    tool.setDataHasChanged();
                    configModel.removeRow(index);
                    if (index > configModel.getRowCount() - 1)
                        index = configModel.getRowCount() - 1;
                    configModel.fireTableDataChanged();
                    if (index >= 0)
                    {
                        configItems.changeSelection(index, 0, false, false);
                        loadTime(index);
                    }
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        helpDialog = new NavHelp(this, this, context, context.cfg.gs("Sleep.help"), "sleep_" + context.preferences.getLocale() + ".html", false);
        if (!helpDialog.fault)
            helpDialog.buttonFocus();
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            SleepTool tool = new SleepTool(context);
            tool.setConfigName(context.cfg.gs("Z.untitled"));
            tool.setDataHasChanged();

            configModel.addRow(new Object[]{ tool });

            if (configModel.getRowCount() > 0)
            {
                buttonCopy.setEnabled(true);
                buttonDelete.setEnabled(true);
            }

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("Sleep.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        saveConfigurations();
        savePreferences();
        setVisible(false);
    }

    public void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((SleepTool) configModel.getValueAt(i, 0)).reset();
            ((SleepTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
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
            if (((SleepTool) configModel.getValueAt(i, 0)).isDataChanged())
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
            loadTime(index);
        }
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
        if (e.getOppositeComponent() instanceof JTable)
        {
            // ignore config selection changes to avoid invalid updates
        }
        else
            updateOnChange(e.getSource());
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    private void loadConfigurations()
    {
        ArrayList<AbstractTool> toolList = null;
        try
        {
            toolList = context.tools.loadAllTools(context, SleepTool.INTERNAL_NAME);
            for (AbstractTool atool : toolList)
            {
                SleepTool tool = (SleepTool) atool;
                configModel.addRow(new Object[]{ tool });
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogSleep, msg, context.cfg.gs("Sleep.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        configModel.setToolList(toolList);
        configModel.loadJobsConfigurations(this, null);

        if (configModel.getRowCount() == 0)
        {
            textFieldTime.setText("");
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
        }
        else
        {
            loadTime(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadTime(int index)
    {
        if (index >= 0 && index < configModel.getRowCount())
        {
            currentSleepTool = (SleepTool) configModel.getValueAt(index, 0);
            textFieldTime.setText(Integer.toString(currentSleepTool.getSleepTime()));
            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);
        }
        else
        {
            currentSleepTool = null;
            textFieldTime.setText("");
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
        }
    }

    private void saveConfigurations()
    {
        SleepTool tool = null;
        try
        {
            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = (SleepTool) deletedTools.get(i);
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
                tool = (SleepTool) configModel.getValueAt(i, 0);
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
                JOptionPane.showMessageDialog(context.navigator.dialogSleep, msg, context.cfg.gs("Sleep.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    private void savePreferences()
    {
        context.preferences.setToolsSleepHeight(this.getHeight());
        context.preferences.setToolsSleepWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsSleepXpos(location.x);
        context.preferences.setToolsSleepYpos(location.y);
        context.preferences.setToolsSleepDividerLocation(splitPaneContent.getDividerLocation());
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

    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
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
            current = Integer.toString(currentSleepTool.getSleepTime());
            if (!current.equals(tf.getText()))
            {
                currentSleepTool.setSleepTime(Integer.parseInt(textFieldTime.getText()));
                currentSleepTool.setDataHasChanged();
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
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        configItems = new JTable();
        panelOptions = new JPanel();
        labelTime = new JLabel();
        textFieldTime = new JTextField();
        buttonBar = new JPanel();
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("Sleep.title"));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setName("sleepUI");
        setPreferredSize(new Dimension(560, 395));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                SleepUI.this.windowClosing(e);
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
                    panelTop.setPreferredSize(new Dimension(570, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText("New");
                        buttonNew.setMnemonic('N');
                        buttonNew.setToolTipText(context.cfg.gs("Sleep.add.new.tool.configuration"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("Navigator.buttonCopy.text"));
                        buttonCopy.setMnemonic('C');
                        buttonCopy.setToolTipText(context.cfg.gs("Navigator.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("Navigator.buttonDelete.text"));
                        buttonDelete.setMnemonic('D');
                        buttonDelete.setToolTipText(context.cfg.gs("Navigator.buttonDelete.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("Sleep.help"));
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
                        panelOptions.setLayout(new GridBagLayout());
                        ((GridBagLayout)panelOptions.getLayout()).columnWidths = new int[] {0, 0, 0};
                        ((GridBagLayout)panelOptions.getLayout()).rowHeights = new int[] {0, 0};
                        ((GridBagLayout)panelOptions.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                        ((GridBagLayout)panelOptions.getLayout()).rowWeights = new double[] {1.0, 1.0E-4};

                        //---- labelTime ----
                        labelTime.setText(context.cfg.gs("Sleep.labelTime.text"));
                        panelOptions.add(labelTime, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            new Insets(0, 10, 0, 0), 0, 0));

                        //---- textFieldTime ----
                        textFieldTime.addActionListener(e -> genericAction(e));
                        textFieldTime.addFocusListener(new FocusAdapter() {
                            @Override
                            public void focusLost(FocusEvent e) {
                                genericTextFieldFocusLost(e);
                            }
                        });
                        textFieldTime.addKeyListener(new KeyAdapter() {
                            @Override
                            public void keyPressed(KeyEvent e) {
                                tabKeyPressed(e);
                            }
                        });
                        panelOptions.add(textFieldTime, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                            new Insets(0, 0, 0, 0), 0, 0));
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

                //---- saveButton ----
                saveButton.setText(context.cfg.gs("Z.save"));
                saveButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("Z.cancel"));
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
    public JPanel dialogPane;
    public JPanel contentPanel;
    public JPanel panelTop;
    public JPanel panelTopButtons;
    public JButton buttonNew;
    public JButton buttonCopy;
    public JButton buttonDelete;
    public JPanel panelHelp;
    public JLabel labelHelp;
    public JSplitPane splitPaneContent;
    public JScrollPane scrollPaneConfig;
    public JTable configItems;
    public JPanel panelOptions;
    public JLabel labelTime;
    public JTextField textFieldTime;
    public JPanel buttonBar;
    public JButton saveButton;
    public JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
