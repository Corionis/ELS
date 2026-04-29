package com.corionis.els.gui.tools.email;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;

import com.corionis.els.repository.Repository;
import com.corionis.els.repository.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class EmailTemplates extends JDialog
{
    private int configIndex = -1;
    private Context context;
    private Template currentTemplate = null;
    private ArrayList<Template> deletedTemplates = new ArrayList<>();
    private EmailTemplatesModel emailTemplatesModel;
    private NavHelp helpDialog;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private Window owner;
    private Repository repo = null;
    private Template startTemplate = null;
    private User startUser = null;

    public EmailTemplates(Window owner, Context context)
    {
        super(owner);
        this.owner = owner;
        this.context = context;
        initComponents();
        initialize();
        this.startTemplate = null;
        this.startUser = null;
    }

    private void actionBrowserClicked(ActionEvent e)
    {
        try
        {
            currentTemplate.setContext(context);
            String text = currentTemplate.getFormattedText(editorPane.getText(), repo);
            String filename = "emailPreview.html";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, filename);
            File file = new File(filename);
            Files.write(file.toPath(), text.getBytes(Charset.forName("UTF-8")));

            filename = "file:///" + filename;

            URI uri = new URI(filename);
            Desktop.getDesktop().browse(uri);
            editorPane.requestFocus();
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
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
            if (reply == JOptionPane.NO_OPTION)
                return;
        }
        if (helpDialog != null && helpDialog.isVisible())
        {
            helpDialog.setVisible(false);
        }
        setVisible(false);
        owner.requestFocus();
   }

    private void actionCopyClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            Template orig = (Template) emailTemplatesModel.getValueAt(index, 0);
            String rename = orig.getFileName() + context.cfg.gs("Z.copy");
            if (emailTemplatesModel.find(rename) == null)
            {
                Template et = orig.clone();
                et.setFileName(rename);
                emailTemplatesModel.addRow(new Object[]{et});
                et.setChanged();

                load(configItems.getRowCount() - 1);
                emailTemplatesModel.fireTableRowsUpdated(configItems.getRowCount() - 1, configItems.getRowCount() - 1);

                configItems.editCellAt(emailTemplatesModel.getRowCount() - 1, 0);
                configItems.changeSelection(emailTemplatesModel.getRowCount() - 1, emailTemplatesModel.getRowCount() - 1, false, false);
                configItems.getEditorComponent().requestFocus();
                ((JTextField) configItems.getEditorComponent()).selectAll();
            }
            else
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                        rename, context.cfg.gs("EmailTemplates.this.title"), JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void actionDefaultClicked(ActionEvent e)
    {
        int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure"),
                context.cfg.gs("EmailTemplates.this.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (reply == JOptionPane.NO_OPTION)
        {
            return;
        }
        loading = true;
        editorPane.setText(currentTemplate.getDefault());
        currentTemplate.setChanged();
        emailTemplatesModel.fireTableRowsUpdated(configItems.getSelectedRow(), configItems.getSelectedRow());
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        int index = configItems.getSelectedRow();
        if (index >= 0)
        {
            if (configItems.isEditing())
                configItems.getCellEditor().stopCellEditing();

            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure"),
                    context.cfg.gs("EmailTemplates.this.title"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
            {
                return;
            }

            // add to delete list if file exists
            File file = new File(currentTemplate.getFullPath(currentTemplate.getFileName()));
            if (file.exists())
            {
                deletedTemplates.add(currentTemplate);
            }

            emailTemplatesModel.removeRow(configItems.getSelectedRow());
            index = emailTemplatesModel.getRowCount() - 1;
            emailTemplatesModel.fireTableDataChanged();
            if (index >= 0)
            {
                currentTemplate = (Template) emailTemplatesModel.getValueAt(index, 0);
                configItems.changeSelection(index, 0, false, false);
            }
            else
            {
                currentTemplate = null;
                index = 0;
            }
            load(index);
        }
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, context, context.cfg.gs("EmailTemplates.labelHelp.toolTipText"), "templates_" + context.preferences.getLocale() + ".html", false);
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
        String format = "";
        JLabel msg = new JLabel(context.cfg.gs("EmailTemplates.select.format"));
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        ButtonGroup bg = new ButtonGroup();
        JRadioButton html = new JRadioButton("HTML");
        JRadioButton text = new JRadioButton("Text");
        bg.add(html);
        bg.add(text);
        panel.add(html);
        panel.add(text);
        html.setSelected(true);
        Object[] parms = {msg, panel};
        int answer = JOptionPane.showConfirmDialog(this, parms, context.cfg.gs("EmailTemplates.this.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (answer == JOptionPane.CANCEL_OPTION)
            return;

        if (html.isSelected())
            format = "html";
        else if (text.isSelected())
            format = "txt";

        String name = context.cfg.gs("Z.untitled") + "." + format;
        if (emailTemplatesModel.find(name) == null)
        {
            currentTemplate = new Template(context, name, format, false);
            currentTemplate.setChanged();

            emailTemplatesModel.addRow(new Object[]{ currentTemplate });
            emailTemplatesModel.fireTableRowsUpdated(configItems.getRowCount() - 1, configItems.getRowCount() - 1);

            name = Utils.getFileName(currentTemplate.getFileName()); // removes .html and .txt
            int i = name.lastIndexOf("."); // remove locale
            if (i > 0)
                name = name.substring(0, i);
            labelConfig.setText(name + " : " + currentTemplate.getFormat().toUpperCase());

            buttonDefault.setEnabled(false);
            editorPane.setText("");
            editorPane.setCaretPosition(0);

            configItems.editCellAt(emailTemplatesModel.getRowCount() - 1, 0);
            loading = true;
            configItems.changeSelection(emailTemplatesModel.getRowCount() - 1, emailTemplatesModel.getRowCount() - 1, false, false);
            loading = false;
            configItems.getEditorComponent().requestFocus();
            ((JTextField) configItems.getEditorComponent()).selectAll();
        }
        else
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("Z.please.rename.the.existing") +
                    context.cfg.gs("Z.untitled"), context.cfg.gs("EmailTemplates.this.title"), JOptionPane.WARNING_MESSAGE);
        }
    }

    private void actionPreviewClicked(ActionEvent e)
    {
        String text = currentTemplate.getFormattedText(editorPane.getText(), repo);
        currentTemplate.setContext(context);
        EmailPreview preview = new EmailPreview(this, this, context, text, currentTemplate.getFormat(),
                this.getX(), this.getY(), this.getWidth(), this.getHeight());
        preview.setVisible(true);
    }

    private void actionSaveClicked(ActionEvent e)
    {
        currentTemplate.setContent(editorPane.getText());
        saveConfigurations();
        savePreferences();
        if (helpDialog != null && helpDialog.isVisible())
        {
            helpDialog.setVisible(false);
        }
        setVisible(false);
    }

    public boolean checkForChanges()
    {
        boolean changes = false;
        for (int i = 0; i < emailTemplatesModel.getRowCount(); ++i)
        {
            Template template = (Template) emailTemplatesModel.getValueAt(i, 0);
            if (template.isChanged())
            {
                changes = true;
                break;
            }
        }
        if (deletedTemplates.size() > 0)
            changes = true;
        return changes;
    }

    public JTable getConfigItems()
    {
        return configItems;
    }

    public ArrayList<Template> getDeletedTemplates()
    {
        return deletedTemplates;
    }

    private void initialize()
    {
        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position & size
        if (context.preferences.getEmailTemplatesXpos() != -1 && Utils.isOnScreen(context.preferences.getEmailTemplatesXpos(),
                context.preferences.getEmailTemplatesYpos()))
        {
            this.setLocation(context.preferences.getEmailTemplatesXpos(), context.preferences.getEmailTemplatesYpos());
            Dimension dim = new Dimension(context.preferences.getEmailTemplatesWidth(), context.preferences.getEmailTemplatesHeight());
            this.setSize(dim);
            splitPaneContent.setDividerLocation(context.preferences.getEmailTemplatesDividerLocation());
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
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

        // setup editor
        editorPane.setContentType("text/plain");
        editorPane.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    currentTemplate.setChanged();
                    currentTemplate.setContent(editorPane.getText());
                    emailTemplatesModel.fireTableRowsUpdated(configIndex, configIndex);
                }
            }
            @Override
            public void removeUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    currentTemplate.setChanged();
                    currentTemplate.setContent(editorPane.getText());
                    emailTemplatesModel.fireTableRowsUpdated(configIndex, configIndex);
                }
            }
            @Override
            public void changedUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    currentTemplate.setChanged();
                    currentTemplate.setContent(editorPane.getText());
                    emailTemplatesModel.fireTableRowsUpdated(configIndex, configIndex);
                }
            }
        });
        UndoManager undoManager = new UndoManager();
        editorPane.getDocument().addUndoableEditListener(e -> {
            undoManager.addEdit(e.getEdit());
        });
        editorPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK), "Undo");
        editorPane.getActionMap().put("Undo", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (undoManager.canUndo())
                {
                    try
                    {
                        undoManager.undo();
                        currentTemplate.setContent(editorPane.getText());
                    }
                    catch (CannotUndoException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        });
        editorPane.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.SHIFT_MASK + ActionEvent.CTRL_MASK), "Redo");
        editorPane.getActionMap().put("Redo", new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (undoManager.canRedo())
                {
                    try
                    {
                        undoManager.redo();
                        currentTemplate.setContent(editorPane.getText());
                    }
                    catch (CannotRedoException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        });

        // setup the left-side list of configurations
        emailTemplatesModel = new EmailTemplatesModel(context, this);
        emailTemplatesModel.setColumnCount(2);
        configItems.setModel(emailTemplatesModel);

        configItems.getTableHeader().setUI(null);
        configItems.setTableHeader(null);
        scrollPaneConfig.setColumnHeaderView(null);

        configItems.getColumnModel().getColumn(1).setPreferredWidth(6);
        configItems.getColumnModel().getColumn(1).setWidth(6);
        configItems.getColumnModel().getColumn(1).setMaxWidth(6);
        configItems.getColumnModel().getColumn(1).setMinWidth(6);
        configItems.getColumnModel().getColumn(1).setResizable(false);

        ListSelectionModel lsm = configItems.getSelectionModel();
        lsm.addListSelectionListener(new ListSelectionListener()
        {
            @Override
            public void valueChanged(ListSelectionEvent listSelectionEvent)
            {
                if (!loading && !listSelectionEvent.getValueIsAdjusting())
                {
                    if (currentTemplate.isChanged())
                        currentTemplate.setContent(editorPane.getText());

                    ListSelectionModel sm = (ListSelectionModel) listSelectionEvent.getSource();
                    int index = sm.getMinSelectionIndex();
                    if (index >= 0)
                    {
                        configIndex = index;
                        load(index);
                    }
                }
            }
        });

        loadConfigurations();
        load(0);
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void load(int index)
    {
        try
        {
            loading = true;
            configIndex = index;
            configItems.setRowSelectionInterval(index, index);
            currentTemplate = (Template) emailTemplatesModel.getValueAt(index, 0);
            editorPane.setText(currentTemplate.getContent(currentTemplate.getFileName()));
            setTemplateTitle();

            if (currentTemplate.isBuildIn())
            {
                buttonDelete.setEnabled(false);
                buttonDefault.setEnabled(true);
                buttonDefault.setToolTipText(context.cfg.gs("EmailTemplates.buttonDefault.toolTipText"));
            }
            else
            {
                buttonDelete.setEnabled(true);
                buttonDefault.setEnabled(false);
                buttonDefault.setToolTipText(context.cfg.gs(context.cfg.gs("EmailTemplates.not.a.built.in.template")));
            }

            if (currentTemplate.getFormat().equalsIgnoreCase("html"))
            {
                buttonBold.setEnabled(true);
                buttonItalic.setEnabled(true);
                buttonSerif.setEnabled(true);
                buttonSpace.setEnabled(true);
                buttonList.setEnabled(true);
                buttonLine.setEnabled(true);
                buttonUrl.setEnabled(true);
                buttonLogo.setEnabled(true);
            }
            else
            {
                buttonBold.setEnabled(false);
                buttonItalic.setEnabled(false);
                buttonSerif.setEnabled(false);
                buttonSpace.setEnabled(false);
                buttonList.setEnabled(false);
                buttonLine.setEnabled(false);
                buttonUrl.setEnabled(false);
                buttonLogo.setEnabled(false);
            }

            editorPane.requestFocus();
            editorPane.setCaretPosition(0);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        loading = false;
    }

    private void loadConfigurations()
    {
        ArrayList<Template> templates = new ArrayList<>();
        Template tempTemplate = new Template(context, null, null, false);

        ArrayList<String> builtIns = tempTemplate.getBuiltIns();
        for (String name : builtIns)
        {
            String format = Utils.getFileExtension(name);
            Template template = new Template(context, name, format, true);
            templates.add(template);
        }

        File file = new File(tempTemplate.getFullPath(""));
        File[] files = FileSystemView.getFileSystemView().getFiles(file, (Utils.isOsMac() ? true : false));
        for (File entry : files)
        {
            boolean has = false;
            String name = entry.getName();
            for (Template template : templates)
            {
                if (template.getFileName().equals(name))
                {
                    has = true; // physical customized build-in
                    break;
                }
            }
            if (has)
                continue;
            String format = Utils.getFileExtension(name);
            Template template = new Template(context, name, format, false);
            templates.add(template);
        }

        for (Template template : templates)
        {
            emailTemplatesModel.addRow(new Object[]{template});
        }
    }

    private void saveConfigurations()
    {
        try
        {
            // remove any deleted templates
            for (int i = 0; i < deletedTemplates.size(); ++i)
            {
                Template template = (Template) deletedTemplates.get(i);
                File file = new File(template.getFullPath(template.getFileName()));
                if (file.exists())
                {
                    file.delete();
                }
            }
            deletedTemplates = new ArrayList<Template>();

            for (int i = 0; i < emailTemplatesModel.getRowCount(); ++i)
            {
                Template template = (Template) emailTemplatesModel.getValueAt(i, 0);
                if (template.isChanged())
                {
                    template.write();
                    template.setChanged(false);
                }
            }
        }
        catch (Exception ex)
        {
            String msg = context.cfg.gs("Z.exception") + " " + Utils.getStackTrace(ex);
            logger.error(msg);
            JOptionPane.showMessageDialog(context.navigator.dialogEmail, msg, context.cfg.gs("EmailTemplates.this.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void savePreferences()
    {
        context.preferences.setEmailTemplatesHeight(this.getHeight());
        context.preferences.setEmailTemplatesWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setEmailTemplatesXpos(location.x);
        context.preferences.setEmailTemplatesYpos(location.y);
        context.preferences.setEmailTemplatesDividerLocation(splitPaneContent.getDividerLocation());
    }

    /**
     * Set the desired Template for User
     *
     * @param template
     */
    public void setStart(Template template, Repository repo)
    {
        this.startTemplate = template;
        this.repo  = repo;
        this.startUser = repo.getUser();
        for (int i  = 0; i < emailTemplatesModel.getRowCount(); i++)
        {
            if (((Template)emailTemplatesModel.getValueAt(i, 0)).getFileName().equals(template.getFileName()))
            {
                configIndex = i;
                currentTemplate = (Template) emailTemplatesModel.getValueAt(i, 0);
                configItems.setRowSelectionInterval(i, i);
                load(i);
                break;
            }
        }
        setTemplateTitle();
    }

    private void setTemplateTitle()
    {
        String name = Utils.getFileName(currentTemplate.getFileName()); // removes .html and .txt
        int i = name.lastIndexOf("."); // remove locale
        if (i > 0)
            name = name.substring(0, i);

        name += " : " + currentTemplate.getFormat().toUpperCase();

        if (startUser != null)
            name += context.cfg.gs("EmailTemplates.for") + startUser.getName() + " : " + startUser.getTypeString();

        labelConfig.setText(name);
    }

    private void toolBoldClicked(ActionEvent ae)
    {
        try
        {
            String text = editorPane.getSelectedText();
            if (text != null && !text.isEmpty())
            {
                int s = editorPane.getSelectionStart();
                int e = editorPane.getSelectionEnd();
                editorPane.getDocument().insertString(e, "</b>", null);
                editorPane.getDocument().insertString(s, "<b>", null);
                editorPane.select(e + 7, e + 7);
            }
            else
            {
                int p = editorPane.getCaretPosition();
                editorPane.getDocument().insertString(p, "<b></b>", null);
                editorPane.select(p + 3, p + 3);
            }
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolItalicClicked(ActionEvent ae)
    {
        try
        {
            String text = editorPane.getSelectedText();
            if (text != null && !text.isEmpty())
            {
                int s = editorPane.getSelectionStart();
                int e = editorPane.getSelectionEnd();
                editorPane.getDocument().insertString(e, "</i>", null);
                editorPane.getDocument().insertString(s, "<i>", null);
                editorPane.select(e + 7, e + 7);
            }
            else
            {
                int p = editorPane.getCaretPosition();
                editorPane.getDocument().insertString(p, "<i></i>", null);
                editorPane.select(p + 3, p + 3);
            }
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolLineClicked(ActionEvent ae)
    {
        try
        {
            String insert = "<hr/> ";

            int p = editorPane.getCaretPosition();
            editorPane.getDocument().insertString(p, insert, null);
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolListClicked(ActionEvent ae)
    {
        try
        {
            String insert = "&bull; ";

            int p = editorPane.getCaretPosition();
            editorPane.getDocument().insertString(p, insert, null);
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolLogoClicked(ActionEvent ae)
    {
        try
        {
            String insert = "<div><a href=\"https://www.elsnavigator.com/\" target=\"_blank\"><img src=\"https://www.elsnavigator.com/assets/images/els-logo-64px.png\" border=\"0\" style=\"vertical-align: middle;\"/></a>&nbsp;&nbsp;To: <b>${userName}</b></div>";

            int p = editorPane.getCaretPosition();
            editorPane.getDocument().insertString(p, insert, null);
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolSerifClicked(ActionEvent ae)
    {
        try
        {
            String text = editorPane.getSelectedText();
            if (text != null && !text.isEmpty())
            {
                int s = editorPane.getSelectionStart();
                int e = editorPane.getSelectionEnd();
                editorPane.getDocument().insertString(e, "</span>", null);
                editorPane.getDocument().insertString(s, "<span style='font-family: Georgia, serif;'>", null);
                editorPane.select(e + 7, e + 7);
            }
            else
            {
                int p = editorPane.getCaretPosition();
                editorPane.getDocument().insertString(p, "<span style='font-family: Georgia, serif;'></span>", null);
                editorPane.select(p + 3, p + 3);
            }
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolSpaceClicked(ActionEvent ae)
    {
        try
        {
            String insert = "&nbsp;";

            int p = editorPane.getCaretPosition();
            editorPane.getDocument().insertString(p, insert, null);
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void toolUriClicked(ActionEvent ae)
    {
        try
        {
            LinkUI link = new LinkUI(this, context, editorPane);
            link.setVisible(true);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
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
        panelEditor = new JPanel();
        panelEditorTools = new JPanel();
        labelConfig = new JLabel();
        panelLeft = new JPanel();
        toolBar = new JMenuBar();
        buttonBold = new JButton();
        buttonItalic = new JButton();
        buttonSerif = new JButton();
        buttonSpace = new JButton();
        buttonList = new JButton();
        buttonLine = new JButton();
        buttonUrl = new JButton();
        buttonLogo = new JButton();
        scrollPaneEditor = new JScrollPane();
        editorPane = new JEditorPane();
        panelEditorButtons = new JPanel();
        panelLeft2 = new JPanel();
        buttonPreview = new JButton();
        buttonBrowser = new JButton();
        hSpacer1 = new JPanel(null);
        buttonDefault = new JButton();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("EmailTemplates.this.title"));
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                EmailTemplates.this.windowClosing(e);
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
                        buttonNew.setText(context.cfg.gs("EmailTemplates.buttonNew.text"));
                        buttonNew.setMnemonic(context.cfg.gs("EmailTemplates.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(context.cfg.gs("EmailTemplates.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(context.cfg.gs("EmailTemplates.buttonCopy.text"));
                        buttonCopy.setMnemonic(context.cfg.gs("EmailTemplates.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(context.cfg.gs("EmailTemplates.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(context.cfg.gs("EmailTemplates.buttonDelete.text"));
                        buttonDelete.setMnemonic(context.cfg.gs("EmailTemplates.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(context.cfg.gs("EmailTemplates.buttonDelete.toolTipText"));
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
                        labelHelp.setToolTipText(context.cfg.gs("EmailTemplates.labelHelp.toolTipText"));
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
                        scrollPaneConfig.setViewportView(configItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneConfig);

                    //======== panelEditor ========
                    {
                        panelEditor.setLayout(new BorderLayout());

                        //======== panelEditorTools ========
                        {
                            panelEditorTools.setLayout(new BorderLayout());

                            //---- labelConfig ----
                            labelConfig.setFont(labelConfig.getFont().deriveFont(labelConfig.getFont().getStyle() | Font.BOLD, labelConfig.getFont().getSize() + 1f));
                            labelConfig.setText("User: Conn");
                            labelConfig.setHorizontalAlignment(SwingConstants.LEFT);
                            panelEditorTools.add(labelConfig, BorderLayout.NORTH);

                            //======== panelLeft ========
                            {
                                panelLeft.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

                                //======== toolBar ========
                                {
                                    toolBar.setMargin(new Insets(2, 2, 2, 2));

                                    //---- buttonBold ----
                                    buttonBold.setText(context.cfg.gs("EmailTemplates.buttonBold.text"));
                                    buttonBold.setFont(new Font("Inter", buttonBold.getFont().getStyle() | Font.BOLD, buttonBold.getFont().getSize()));
                                    buttonBold.setPreferredSize(new Dimension(26, 26));
                                    buttonBold.setMinimumSize(new Dimension(26, 26));
                                    buttonBold.setMaximumSize(new Dimension(26, 26));
                                    buttonBold.setToolTipText(context.cfg.gs("EmailTemplates.buttonBold.toolTipText"));
                                    buttonBold.setForeground(new Color(0x3592c4));
                                    buttonBold.addActionListener(e -> toolBoldClicked(e));
                                    toolBar.add(buttonBold);

                                    //---- buttonItalic ----
                                    buttonItalic.setText(context.cfg.gs("EmailTemplates.buttonItalic.text"));
                                    buttonItalic.setPreferredSize(new Dimension(26, 26));
                                    buttonItalic.setMinimumSize(new Dimension(26, 26));
                                    buttonItalic.setFont(new Font(Font.SERIF, buttonItalic.getFont().getStyle() | Font.ITALIC, buttonItalic.getFont().getSize()));
                                    buttonItalic.setMaximumSize(new Dimension(26, 26));
                                    buttonItalic.setToolTipText(context.cfg.gs("EmailTemplates.buttonItalic.toolTipText"));
                                    buttonItalic.setForeground(new Color(0x3592c4));
                                    buttonItalic.addActionListener(e -> toolItalicClicked(e));
                                    toolBar.add(buttonItalic);

                                    //---- buttonSerif ----
                                    buttonSerif.setText(context.cfg.gs("EmailTemplates.buttonSerif.text"));
                                    buttonSerif.setMaximumSize(new Dimension(26, 26));
                                    buttonSerif.setMinimumSize(new Dimension(26, 26));
                                    buttonSerif.setPreferredSize(new Dimension(26, 26));
                                    buttonSerif.setForeground(new Color(0x3592c4));
                                    buttonSerif.setFont(new Font(Font.SERIF, Font.PLAIN, 13));
                                    buttonSerif.setToolTipText(context.cfg.gs("EmailTemplates.buttonSerif.toolTipText"));
                                    buttonSerif.addActionListener(e -> toolSerifClicked(e));
                                    toolBar.add(buttonSerif);

                                    //---- buttonSpace ----
                                    buttonSpace.setIcon(new ImageIcon(getClass().getResource("/space.png")));
                                    buttonSpace.setMaximumSize(new Dimension(26, 26));
                                    buttonSpace.setMinimumSize(new Dimension(26, 26));
                                    buttonSpace.setPreferredSize(new Dimension(26, 26));
                                    buttonSpace.setToolTipText(context.cfg.gs("EmailTemplates.buttonSpace.toolTipText"));
                                    buttonSpace.addActionListener(e -> toolSpaceClicked(e));
                                    toolBar.add(buttonSpace);

                                    //---- buttonList ----
                                    buttonList.setIcon(new ImageIcon(getClass().getResource("/list.png")));
                                    buttonList.setMaximumSize(new Dimension(26, 26));
                                    buttonList.setMinimumSize(new Dimension(26, 26));
                                    buttonList.setPreferredSize(new Dimension(26, 26));
                                    buttonList.setToolTipText(context.cfg.gs("EmailTemplates.buttonList.toolTipText"));
                                    buttonList.addActionListener(e -> toolListClicked(e));
                                    toolBar.add(buttonList);

                                    //---- buttonLine ----
                                    buttonLine.setToolTipText(context.cfg.gs("EmailTemplates.buttonLine.toolTipText"));
                                    buttonLine.setIcon(new ImageIcon(getClass().getResource("/line.png")));
                                    buttonLine.setMaximumSize(new Dimension(26, 26));
                                    buttonLine.setPreferredSize(new Dimension(26, 26));
                                    buttonLine.setMinimumSize(new Dimension(26, 26));
                                    buttonLine.addActionListener(e -> toolLineClicked(e));
                                    toolBar.add(buttonLine);

                                    //---- buttonUrl ----
                                    buttonUrl.setMinimumSize(new Dimension(26, 26));
                                    buttonUrl.setPreferredSize(new Dimension(26, 26));
                                    buttonUrl.setMaximumSize(new Dimension(26, 26));
                                    buttonUrl.setToolTipText(context.cfg.gs("EmailTemplates.buttonUrl.toolTipText"));
                                    buttonUrl.setIcon(new ImageIcon(getClass().getResource("/link.png")));
                                    buttonUrl.addActionListener(e -> toolUriClicked(e));
                                    toolBar.add(buttonUrl);

                                    //---- buttonLogo ----
                                    buttonLogo.setPreferredSize(new Dimension(26, 26));
                                    buttonLogo.setMinimumSize(new Dimension(26, 26));
                                    buttonLogo.setMaximumSize(new Dimension(26, 26));
                                    buttonLogo.setToolTipText(context.cfg.gs("EmailTemplates.buttonLogo.toolTipText"));
                                    buttonLogo.setIcon(new ImageIcon(getClass().getResource("/els-logo-18px.png")));
                                    buttonLogo.addActionListener(e -> toolLogoClicked(e));
                                    toolBar.add(buttonLogo);
                                }
                                panelLeft.add(toolBar);
                            }
                            panelEditorTools.add(panelLeft, BorderLayout.WEST);
                        }
                        panelEditor.add(panelEditorTools, BorderLayout.NORTH);

                        //======== scrollPaneEditor ========
                        {

                            //---- editorPane ----
                            editorPane.setMinimumSize(new Dimension(20, 23));
                            editorPane.setPreferredSize(new Dimension(200, 200));
                            editorPane.setMaximumSize(new Dimension(32767, 32767));
                            scrollPaneEditor.setViewportView(editorPane);
                        }
                        panelEditor.add(scrollPaneEditor, BorderLayout.CENTER);

                        //======== panelEditorButtons ========
                        {
                            panelEditorButtons.setLayout(new BorderLayout());

                            //======== panelLeft2 ========
                            {
                                panelLeft2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

                                //---- buttonPreview ----
                                buttonPreview.setText(context.cfg.gs("EmailTemplates.buttonPreview.text"));
                                buttonPreview.setToolTipText(context.cfg.gs("EmailTemplates.buttonPreview.toolTipText"));
                                buttonPreview.setMnemonic('P');
                                buttonPreview.addActionListener(e -> actionPreviewClicked(e));
                                panelLeft2.add(buttonPreview);

                                //---- buttonBrowser ----
                                buttonBrowser.setText(context.cfg.gs("EmailTemplates.buttonBrowser.text"));
                                buttonBrowser.setToolTipText(context.cfg.gs("EmailTemplates.buttonBrowser.toolTipText"));
                                buttonBrowser.setMnemonic('B');
                                buttonBrowser.addActionListener(e -> actionBrowserClicked(e));
                                panelLeft2.add(buttonBrowser);

                                //---- hSpacer1 ----
                                hSpacer1.setPreferredSize(new Dimension(22, 10));
                                hSpacer1.setMinimumSize(new Dimension(22, 12));
                                panelLeft2.add(hSpacer1);

                                //---- buttonDefault ----
                                buttonDefault.setText(context.cfg.gs("EmailTemplates.buttonDefault.text"));
                                buttonDefault.setToolTipText(context.cfg.gs("EmailTemplates.buttonDefault.toolTipText"));
                                buttonDefault.setMnemonic('F');
                                buttonDefault.addActionListener(e -> actionDefaultClicked(e));
                                panelLeft2.add(buttonDefault);
                            }
                            panelEditorButtons.add(panelLeft2, BorderLayout.WEST);
                        }
                        panelEditor.add(panelEditorButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelEditor);
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

                //---- okButton ----
                okButton.setText(context.cfg.gs("Z.save"));
                okButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                okButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

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
    private JPanel panelEditor;
    private JPanel panelEditorTools;
    private JLabel labelConfig;
    private JPanel panelLeft;
    public JMenuBar toolBar;
    private JButton buttonBold;
    private JButton buttonItalic;
    private JButton buttonSerif;
    private JButton buttonSpace;
    private JButton buttonList;
    private JButton buttonLine;
    private JButton buttonUrl;
    private JButton buttonLogo;
    private JScrollPane scrollPaneEditor;
    public JEditorPane editorPane;
    private JPanel panelEditorButtons;
    private JPanel panelLeft2;
    private JButton buttonPreview;
    private JButton buttonBrowser;
    private JPanel hSpacer1;
    private JButton buttonDefault;
    private JPanel buttonBar;
    public JLabel labelStatus;
    public JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

}
