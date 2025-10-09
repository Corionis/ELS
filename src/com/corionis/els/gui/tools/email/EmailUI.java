package com.corionis.els.gui.tools.email;

import java.awt.event.*;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.jobs.AbstractToolDialog;
import com.corionis.els.gui.jobs.ConfigModel;
import com.corionis.els.gui.util.NumberFilter;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.email.EmailHandler;
import com.corionis.els.tools.email.EmailTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.PlainDocument;

public class EmailUI extends AbstractToolDialog
{
    private ConfigModel configModel;
    private Context context;
    private EmailTool currentTool = null;
    private EmailHandler emailHandler = null;
    private NavHelp helpDialog;
    private boolean hidePassword = true;
    private boolean inUpdateOnChange = false;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private NumberFilter numberFilter;

    public EmailUI(Window owner, Context context)
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
        if (context.preferences.getToolsEmailXpos() != -1 && Utils.isOnScreen(context.preferences.getToolsEmailXpos(),
                context.preferences.getToolsEmailYpos()))
        {
            this.setLocation(context.preferences.getToolsEmailXpos(), context.preferences.getToolsEmailYpos());
            Dimension dim = new Dimension(context.preferences.getToolsEmailWidth(), context.preferences.getToolsEmailHeight());
            this.setSize(dim);
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        this.splitPaneContent.setDividerLocation(context.preferences.getToolsEmailDividerLocation());

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

        // only allow numbers in port field
        numberFilter = new NumberFilter();
        PlainDocument pd = (PlainDocument) textFieldPort.getDocument();
        pd.setDocumentFilter(numberFilter);

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
                    loadEmail(index);
                }
            }
        });

        loadConfigurations();
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void actionAuthClicked(ActionEvent event)
    {
        if (emailHandler != null && emailHandler.isWorkerRunning())
        {
            interruptEmailHandler();
            emailHandler = null;
            return;
        }
        emailHandler = null;

        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            if (configItems.isEditing())
                configItems.getCellEditor().stopCellEditing();

            currentTool = (EmailTool) configModel.getValueAt(index, 0);

            labelStatus.setText(" ");
            updateControls(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            buttonAuth.setText(context.cfg.gs("EmailUI.button.cancel"));
            buttonAuth.setToolTipText(context.cfg.gs("EmailUI.buttonAuth.toolTipCancel"));
            buttonAuth.setEnabled(true);
            buttonAuth.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            emailHandler = new EmailHandler(context, this, currentTool, EmailHandler.Function.AUTH);
            emailHandler.start();
        }
    }

    private void actionCancelClicked(ActionEvent e)
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
                cleanUp();
                cancelChanges();
                setVisible(false);
            }
        }
        else
        {
            cleanUp();
            setVisible(false);
        }
    }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            EmailTool orig = (EmailTool) configModel.getValueAt(index, 0);
            String rename = orig.getConfigName() + context.cfg.gs("Z.copy");
            if (configModel.find(rename, null) == null)
            {
                EmailTool et = orig.clone();
                et.setConfigName(rename);
                et.setDataHasChanged();
                configModel.addRow(new Object[]{ et });
                currentTool = (EmailTool) configModel.getValueAt(configModel.getRowCount() - 1, 0);

                configItems.editCellAt(configModel.getRowCount() - 1, 0);
                configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, context.cfg.gs("Email.title"), JOptionPane.WARNING_MESSAGE);
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

            currentTool = (EmailTool) configModel.getValueAt(index, 0);
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure.you.want.to.delete.configuration") + currentTool.getConfigName(),
                    context.cfg.gs("Z.delete.configuration"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                int answer = configModel.checkJobConflicts(currentTool.getConfigName(), null, currentTool.getInternalName(), false);
                if (answer >= 0)
                {
                    // add to delete list if file exists
                    File file = new File(currentTool.getFullPath());
                    if (file.exists())
                    {
                        deletedTools.add(currentTool);
                    }

                    configModel.removeRow(index);
                    index = configModel.getRowCount() - 1;
                    configModel.fireTableDataChanged();
                    if (index >= 0)
                    {
                        currentTool = (EmailTool) configModel.getValueAt(index, 0);
                        configItems.changeSelection(index, 0, false, false);
                    }
                    else
                        currentTool = null;
                    loadEmail(index);
                }
                configItems.requestFocus();
            }
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, this, context, context.cfg.gs("EmailUI.help"), "email_" + context.preferences.getLocale() + ".html", false);
            if (!helpDialog.fault)
                helpDialog.buttonFocus();
        }
        else
        {
            helpDialog.toFront();
            helpDialog.requestFocus();
            helpDialog.buttonFocus();
        }
    }

    private void actionNewClicked(ActionEvent e)
    {
        if (configModel.find(context.cfg.gs("Z.untitled"), null) == null)
        {
            EmailTool emt = new EmailTool(context);
            emt.setConfigName(context.cfg.gs("Z.untitled"));
            emt.setDataHasChanged();

            configModel.addRow(new Object[]{ emt });
            loadEmail(configModel.getRowCount() - 1);
            updateControls();

            configItems.editCellAt(configModel.getRowCount() - 1, 0);
            configItems.changeSelection(configModel.getRowCount() - 1, configModel.getRowCount() - 1, false, false);
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("Email.title"), JOptionPane.WARNING_MESSAGE);
        }
        
    }

    private void actionPasswordClicked(ActionEvent e)
    {
        if (hidePassword)
        {
            passwordField.setEchoChar((char) 0);
            hidePassword = false;
        }
        else
        {
            passwordField.setEchoChar('\u25CF');
            hidePassword = true;
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        cleanUp();
        saveConfigurations();
        savePreferences();
        setVisible(false);
    }

    private void actionTestClicked(ActionEvent event)
    {
        if (emailHandler != null && emailHandler.isWorkerRunning())
        {
            interruptEmailHandler();
            emailHandler = null;
            return;
        }
        emailHandler = null;

        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            if (configItems.isEditing())
                configItems.getCellEditor().stopCellEditing();

            currentTool = (EmailTool) configModel.getValueAt(index, 0);

            labelStatus.setText(" ");
            updateControls(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            buttonTest.setText(context.cfg.gs("EmailUI.button.cancel"));
            buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonAuth.toolTipCancel"));
            buttonTest.setEnabled(true);
            buttonTest.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));

            emailHandler = new EmailHandler(context, this, currentTool, EmailHandler.Function.TEST);
            emailHandler.start();
        }
    }

    public void cancelChanges()
    {
        if (deletedTools.size() > 0)
            deletedTools = new ArrayList<AbstractTool>();

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            ((EmailTool) configModel.getValueAt(i, 0)).setDataHasChanged(false);
        }

        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
    }

    public boolean checkForChanges()
    {
        if (!deletedTools.isEmpty())
            return true;

        for (int i = 0; i < configModel.getRowCount(); ++i)
        {
            if (((EmailTool) configModel.getValueAt(i, 0)).isDataChanged())
            {
                return true;
            }
        }
        return false;
    }

    private void cleanUp()
    {
        if (emailHandler != null)
        {
            if (emailHandler.isWorkerRunning())
                emailHandler.interrupt();
            emailHandler = null;
        }
    }

    private void configItemsMouseClicked(MouseEvent e)
    {
        JTable src = (JTable) e.getSource();
        if (e.getClickCount() == 1)
        {
            int index = src.getSelectedRow();
            loadEmail(index);
        }
    }

    @Override
    public JTable getConfigItems()
    {
        return configItems;
    }

    public void genericAction(ActionEvent e)
    {
        updateOnChange(e.getSource());
    }

    public void genericTextFieldFocusLost(FocusEvent e)
    {
        updateOnChange(e.getSource());
    }

    public EmailTool getCurrentTool()
    {
        return currentTool;
    }

    public EmailHandler getEmailHandler()
    {
        return emailHandler;
    }

    private void interruptEmailHandler()
    {
        emailHandler.interrupt();
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        updateControls();
        buttonAuth.setText(context.cfg.gs("EmailUI.buttonAuth.text"));
        buttonAuth.setToolTipText(context.cfg.gs("EmailUI.buttonAuth.toolTipText"));
        labelStatus.setText(context.cfg.gs("EmailUI.authentication.cancel"));
    }

    private void loadConfigurations()
    {
        ArrayList<AbstractTool> toolList = null;
        try
        {
            toolList = context.tools.loadAllTools(context, EmailTool.INTERNAL_NAME);
            for (AbstractTool tool : toolList)
            {
                EmailTool jrt = (EmailTool) tool;
                configModel.addRow(new Object[]{jrt});
            }
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(e);
            if (context != null)
            {
                logger.error(msg);
                JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("Email.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }

        configModel.setToolList(toolList);
        configModel.loadJobsConfigurations(this, null);

        if (configModel.getRowCount() == 0)
        {
            loadEmail(-1);
        }
        else
        {
            loadEmail(0);
            configItems.requestFocus();
            configItems.setRowSelectionInterval(0, 0);
        }
    }

    private void loadEmail(int index)
    {
        loading = true;
        if (index >= 0 && index < configModel.getRowCount())
        {
            currentTool = (EmailTool) configModel.getValueAt(index, 0);
            textFieldServerName.setText(currentTool.getServer());
            textFieldUsername.setText(currentTool.getUsername());
            if (currentTool.getPassword().length() > 0)
            {
                passwordField.setText(currentTool.getPassword());
                hidePassword = false;
                buttonPassword.doClick();
            }
            else
            {
                passwordField.setText("");
                hidePassword = true;
                buttonPassword.doClick();
            }
            textFieldPort.setText(currentTool.getPort());
            comboBoxProfile.setSelectedItem(currentTool.getProfile());
            comboBoxSecurity.setSelectedItem(currentTool.getSecurity());
            comboBoxAuthMethod.setSelectedItem(currentTool.getAuthMethod());
            updateControls();
        }
        else
        {
            currentTool = null;
            textFieldServerName.setText("");
            textFieldUsername.setText("");
            passwordField.setText("");
            textFieldPort.setText("");
            comboBoxProfile.setSelectedIndex(0);
            comboBoxSecurity.setSelectedIndex(0);
            comboBoxAuthMethod.setSelectedIndex(0);
            updateControls();
            hidePassword = true;
            buttonPassword.doClick();
        }

        loading = false;
    }

    private void saveConfigurations()
    {
        EmailTool tool = null;
        try
        {
            // remove any deleted tools JSON configuration file
            for (int i = 0; i < deletedTools.size(); ++i)
            {
                tool = (EmailTool) deletedTools.get(i);
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
                tool = (EmailTool) configModel.getValueAt(i, 0);
                updateOnChange(comboBoxProfile);
                updateOnChange(comboBoxSecurity);
                updateOnChange(comboBoxAuthMethod);
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
                JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("Email.title"), JOptionPane.ERROR_MESSAGE);
            }
            else
                logger.error(msg);
        }
    }

    private void savePreferences()
    {
        context.preferences.setToolsEmailHeight(this.getHeight());
        context.preferences.setToolsEmailWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setToolsEmailXpos(location.x);
        context.preferences.setToolsEmailYpos(location.y);
        context.preferences.setToolsEmailDividerLocation(splitPaneContent.getDividerLocation());
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
            if (source instanceof JPasswordField)
            {
                JPasswordField pf = (JPasswordField) source;
                current = currentTool.getPassword();
                char[] chars = pf.getPassword();
                value = new String(chars);
                if (value.isEmpty())
                    value = "";
                currentTool.setPassword(value);
            }
            else if (source instanceof JTextField)
            {
                JTextField tf = (JTextField) source;
                name = tf.getName();
                switch (name.toLowerCase())
                {
                    case "server":
                        current = currentTool.getServer();
                        value = tf.getText();
                        currentTool.setServer(value);
                        break;
                    case "username":
                        current = currentTool.getUsername();
                        value = tf.getText();
                        currentTool.setUsername(value);
                        break;
                    case "port":
                        current = currentTool.getPort();
                        value = tf.getText();
                        currentTool.setPort(value);
                        break;
                }
            }
            else if (source instanceof JComboBox)
            {
                JComboBox combo = (JComboBox) source;
                name = combo.getName();
                switch (name.toLowerCase())
                {
                    case "profile":
                        current = currentTool.getProfile();
                        value = comboBoxProfile.getSelectedItem().toString();
                        currentTool.setProfile(value);
                        break;
                    case "security":
                        current = currentTool.getSecurity();
                        value = comboBoxSecurity.getSelectedItem().toString();
                        currentTool.setSecurity(value);
                        break;
                    case "authmethod":
                        current = currentTool.getAuthMethod();
                        value = comboBoxAuthMethod.getSelectedItem().toString();
                        currentTool.setAuthMethod(value);
                        if (currentTool.getAuthMethod().equalsIgnoreCase("oauth2"))
                            buttonAuth.setEnabled(true);
                        else
                            buttonAuth.setEnabled(false);
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

    public void updateControls()
    {
        buttonNew.setEnabled(true);
        if (configModel.getRowCount() > 0)
        {
            buttonCopy.setEnabled(true);
            buttonDelete.setEnabled(true);

            if (currentTool != null)
            {
                if (currentTool.getAuthMethod().equalsIgnoreCase("oauth2"))
                {
                    SimpleDateFormat dateFormatter = new SimpleDateFormat(context.preferences.getDateFormat());
                    String stamp = dateFormatter.format(currentTool.getAccessExpires());
                    logger.info(currentTool.getUsername() + " access expires: " + stamp);
                    stamp = dateFormatter.format(currentTool.getRefreshExpires());
                    logger.info(currentTool.getUsername() + " refresh expires: " + stamp);

                    if (!currentTool.isExpired(currentTool.getAccessExpires()) && !currentTool.isExpired(currentTool.getRefreshExpires()))
                    {
                        buttonAuth.setEnabled(false);
                        buttonTest.setEnabled(true);
                    }
                    else if (currentTool.isExpired(currentTool.getAccessExpires()) && currentTool.isExpired(currentTool.getRefreshExpires()))
                    {
                        buttonAuth.setEnabled(true);
                        buttonTest.setEnabled(false);
                        labelStatus.setText(context.cfg.gs("EmailUI.authentication.expired"));
                    }
                    else
                    {
                        labelStatus.setText("<html><body>&nbsp;</body></html>");
                        buttonAuth.setEnabled(false);
                        buttonTest.setEnabled(true);
                    }
                }
                else
                {
                    labelStatus.setText("<html><body>&nbsp;</body></html>");
                    buttonAuth.setEnabled(false);
                    buttonTest.setEnabled(true);
                }

                if (context.publisherRepo == null ||
                        context.publisherRepo.getLibraryData().libraries.email == null || context.publisherRepo.getLibraryData().libraries.email.length() == 0)
                {
                    buttonTest.setEnabled(false);
                    buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonTest.toolTipText.disabled"));
                }
                else
                {
                    buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonTest.toolTipText"));
                }
            }
            else
            {
                buttonAuth.setEnabled(false);
                buttonTest.setEnabled(false);
                buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonTest.toolTipText"));
            }
        }
        else
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonAuth.setEnabled(false);
            buttonTest.setEnabled(false);
        }
        saveButton.setEnabled(true);
        cancelButton.setEnabled(true);
    }

    // used by auth and test buttons
    private void updateControls(boolean enable)
    {
        buttonNew.setEnabled(enable);
        buttonCopy.setEnabled(enable);
        buttonDelete.setEnabled(enable);
        if (currentTool != null && currentTool.getAuthMethod().equalsIgnoreCase("oauth2"))
           buttonAuth.setEnabled(enable);
        else
            buttonAuth.setEnabled(false);
        buttonTest.setEnabled(enable);
        saveButton.setEnabled(enable);
        cancelButton.setEnabled(enable);
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    private void windowHidden(ComponentEvent e)
    {
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
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneConfig = new JScrollPane();
        configItems = new JTable();
        panelOptions = new JPanel();
        scrollPaneOptions = new JScrollPane();
        panelServer = new JPanel();
        label1 = new JLabel();
        labelProfile = new JLabel();
        comboBoxProfile = new JComboBox<>();
        labelServerName = new JLabel();
        textFieldServerName = new JTextField();
        labelUsername = new JLabel();
        textFieldUsername = new JTextField();
        labelPassword = new JLabel();
        panelPassword = new JPanel();
        buttonPassword = new JButton();
        passwordField = new JPasswordField();
        labelPort = new JLabel();
        textFieldPort = new JTextField();
        labelSecurity = new JLabel();
        comboBoxSecurity = new JComboBox<>();
        labelAuthMethod = new JLabel();
        comboBoxAuthMethod = new JComboBox<>();
        panelOptionsButtons = new JPanel();
        buttonAuth = new JButton();
        buttonTest = new JButton();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        saveButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle(context.cfg.gs("EmailUI.title"));
        setName("emailUI");
        setMinimumSize(new Dimension(150, 126));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                EmailUI.this.windowClosing(e);
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
            dialogPane.setPreferredSize(new Dimension(595, 525));
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
                        buttonNew.setText(context.cfg.gs("EmailUI.buttonNew.text"));
                        buttonNew.setMnemonic(context.cfg.gs("EmailUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("EmailUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("EmailUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("EmailUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("EmailUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("EmailUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("EmailUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("EmailUI.buttonDelete.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("EmailUI.labelHelp.toolTipText"));
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

                            //======== panelServer ========
                            {
                                panelServer.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelServer.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                                ((GridBagLayout)panelServer.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                                //---- label1 ----
                                label1.setText(context.cfg.gs("EmailUI.label1.text"));
                                label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.BOLD, label1.getFont().getSize() + 1f));
                                panelServer.add(label1, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(4, 0, 6, 0), 0, 0));

                                //---- labelProfile ----
                                labelProfile.setText(context.cfg.gs("EmailUI.labelProfile.text"));
                                panelServer.add(labelProfile, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- comboBoxProfile ----
                                comboBoxProfile.setName("profile");
                                comboBoxProfile.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "Apple",
                                    "GMail",
                                    "Outlook",
                                    "SMTP",
                                    "Zoho"
                                }));
                                comboBoxProfile.setToolTipText(context.cfg.gs("EmailUI.comboBoxProfile.toolTipText"));
                                comboBoxProfile.addActionListener(e -> genericAction(e));
                                panelServer.add(comboBoxProfile, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelServerName ----
                                labelServerName.setText(context.cfg.gs("EmailUI.labelServerName.text"));
                                panelServer.add(labelServerName, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- textFieldServerName ----
                                textFieldServerName.setMinimumSize(new Dimension(240, 34));
                                textFieldServerName.setName("server");
                                textFieldServerName.setPreferredSize(new Dimension(240, 34));
                                textFieldServerName.addActionListener(e -> genericAction(e));
                                textFieldServerName.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        genericTextFieldFocusLost(e);
                                    }
                                });
                                panelServer.add(textFieldServerName, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelUsername ----
                                labelUsername.setText(context.cfg.gs("EmailUI.labelUsername.text"));
                                panelServer.add(labelUsername, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- textFieldUsername ----
                                textFieldUsername.setName("username");
                                textFieldUsername.addActionListener(e -> genericAction(e));
                                textFieldUsername.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        genericTextFieldFocusLost(e);
                                    }
                                });
                                panelServer.add(textFieldUsername, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelPassword ----
                                labelPassword.setText(context.cfg.gs("EmailUI.labelPassword.text"));
                                panelServer.add(labelPassword, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //======== panelPassword ========
                                {
                                    panelPassword.setLayout(new BorderLayout());

                                    //---- buttonPassword ----
                                    buttonPassword.setIcon(new ImageIcon(getClass().getResource("/password.png")));
                                    buttonPassword.setToolTipText(context.cfg.gs("EmailUI.buttonPassword.toolTipText"));
                                    buttonPassword.setPreferredSize(new Dimension(28, 28));
                                    buttonPassword.setMinimumSize(new Dimension(28, 28));
                                    buttonPassword.setMaximumSize(new Dimension(28, 28));
                                    buttonPassword.setOpaque(false);
                                    buttonPassword.addActionListener(e -> actionPasswordClicked(e));
                                    panelPassword.add(buttonPassword, BorderLayout.EAST);

                                    //---- passwordField ----
                                    passwordField.setName("password");
                                    passwordField.addActionListener(e -> genericAction(e));
                                    passwordField.addFocusListener(new FocusAdapter() {
                                        @Override
                                        public void focusLost(FocusEvent e) {
                                            genericTextFieldFocusLost(e);
                                        }
                                    });
                                    panelPassword.add(passwordField, BorderLayout.CENTER);
                                }
                                panelServer.add(panelPassword, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelPort ----
                                labelPort.setText(context.cfg.gs("EmailUI.labelPort.text"));
                                panelServer.add(labelPort, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- textFieldPort ----
                                textFieldPort.setName("port");
                                textFieldPort.setPreferredSize(new Dimension(64, 34));
                                textFieldPort.setMaximumSize(new Dimension(64, 2147483647));
                                textFieldPort.addActionListener(e -> genericAction(e));
                                textFieldPort.addFocusListener(new FocusAdapter() {
                                    @Override
                                    public void focusLost(FocusEvent e) {
                                        genericTextFieldFocusLost(e);
                                    }
                                });
                                panelServer.add(textFieldPort, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelSecurity ----
                                labelSecurity.setText(context.cfg.gs("EmailUI.labelSecurity.text"));
                                panelServer.add(labelSecurity, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 4), 0, 0));

                                //---- comboBoxSecurity ----
                                comboBoxSecurity.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "STARTTLS",
                                    "SSL/TLS",
                                    "None"
                                }));
                                comboBoxSecurity.setName("security");
                                comboBoxSecurity.addActionListener(e -> genericAction(e));
                                panelServer.add(comboBoxSecurity, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                    new Insets(0, 0, 4, 0), 0, 0));

                                //---- labelAuthMethod ----
                                labelAuthMethod.setText(context.cfg.gs("EmailUI.labelAuthMethod.text"));
                                panelServer.add(labelAuthMethod, new GridBagConstraints(0, 7, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 4, 8), 0, 0));

                                //---- comboBoxAuthMethod ----
                                comboBoxAuthMethod.setModel(new DefaultComboBoxModel<>(new String[] {
                                    "Plain",
                                    "Login",
                                    "OAuth2"
                                }));
                                comboBoxAuthMethod.setName("authmethod");
                                comboBoxAuthMethod.addActionListener(e -> genericAction(e));
                                panelServer.add(comboBoxAuthMethod, new GridBagConstraints(1, 7, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.WEST, GridBagConstraints.VERTICAL,
                                    new Insets(0, 0, 4, 0), 0, 0));
                            }
                            scrollPaneOptions.setViewportView(panelServer);
                        }
                        panelOptions.add(scrollPaneOptions, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonAuth ----
                            buttonAuth.setText(context.cfg.gs("EmailUI.buttonAuth.text"));
                            buttonAuth.setFont(buttonAuth.getFont().deriveFont(buttonAuth.getFont().getSize() - 2f));
                            buttonAuth.setPreferredSize(new Dimension(110, 24));
                            buttonAuth.setMinimumSize(new Dimension(110, 24));
                            buttonAuth.setMaximumSize(new Dimension(110, 24));
                            buttonAuth.setMnemonic('A');
                            buttonAuth.setToolTipText(context.cfg.gs("EmailUI.buttonAuth.toolTipText"));
                            buttonAuth.setMargin(new Insets(0, -10, 0, -10));
                            buttonAuth.addActionListener(e -> actionAuthClicked(e));
                            panelOptionsButtons.add(buttonAuth);

                            //---- buttonTest ----
                            buttonTest.setText(context.cfg.gs("EmailUI.buttonTest.text"));
                            buttonTest.setFont(buttonTest.getFont().deriveFont(buttonTest.getFont().getSize() - 2f));
                            buttonTest.setPreferredSize(new Dimension(78, 24));
                            buttonTest.setMinimumSize(new Dimension(78, 24));
                            buttonTest.setMaximumSize(new Dimension(78, 24));
                            buttonTest.setMnemonic('T');
                            buttonTest.setToolTipText(context.cfg.gs("EmailUI.buttonTest.toolTipText"));
                            buttonTest.setMargin(new Insets(0, -10, 0, -10));
                            buttonTest.addActionListener(e -> actionTestClicked(e));
                            panelOptionsButtons.add(buttonTest);
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
                saveButton.setText("Save");
                saveButton.setToolTipText(context.cfg.gs("EmailUI.saveButton.toolTipText"));
                saveButton.setMnemonic('S');
                saveButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(saveButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setToolTipText(context.cfg.gs("EmailUI.cancelButton.toolTipText"));
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
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneConfig;
    private JTable configItems;
    private JPanel panelOptions;
    private JScrollPane scrollPaneOptions;
    private JPanel panelServer;
    private JLabel label1;
    private JLabel labelProfile;
    private JComboBox<String> comboBoxProfile;
    private JLabel labelServerName;
    private JTextField textFieldServerName;
    private JLabel labelUsername;
    private JTextField textFieldUsername;
    private JLabel labelPassword;
    private JPanel panelPassword;
    private JButton buttonPassword;
    private JPasswordField passwordField;
    private JLabel labelPort;
    private JTextField textFieldPort;
    private JLabel labelSecurity;
    private JComboBox<String> comboBoxSecurity;
    private JLabel labelAuthMethod;
    private JComboBox<String> comboBoxAuthMethod;
    private JPanel panelOptionsButtons;
    public JButton buttonAuth;
    public JButton buttonTest;
    private JPanel buttonBar;
    public JLabel labelStatus;
    private JButton saveButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    //
    // @formatter:on
    // </editor-fold>

}
