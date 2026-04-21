package com.corionis.els.gui.libraries;

import java.awt.event.*;
import java.beans.*;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;

public class EmailEditor extends JDialog
{
    private boolean changed = false;
    private String content;
    private Context context;
    private NavHelp helpDialog;
    private LibrariesUI.LibMeta libMeta;
    private boolean loading = false;
    private Logger logger = LogManager.getLogger("applog");
    private InviteUI owner;
    private Template template;
    private String type;

    public EmailEditor(InviteUI owner, Context context, String type, LibrariesUI.LibMeta libMeta)
    {
        super(owner);
        this.owner = owner;
        this.context = context;
        this.type = type;
        this.libMeta = libMeta;
        initComponents();
        initialize();
    }

    private void actionBrowserClicked(ActionEvent e)
    {
        try
        {
            String text = owner.getFormattedText(editorPane.getText());
            String filename = "emailPreview.html";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, filename);
            File file = new File(filename);
            Files.write(file.toPath(), text.getBytes(Charset.forName("UTF-8")));

            filename = "file:///" + filename;

            URI uri = new URI(filename);
            Desktop.getDesktop().browse(uri);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (changed)
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.cancel.all.changes"), context.cfg.gs("Email Editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (reply == JOptionPane.NO_OPTION)
            {
                return;
            }
        }
        context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Z.changes.cancelled"));
        setVisible(false);
    }

    private void actionDefaultClicked(ActionEvent e)
    {
        int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.are.you.sure"), context.cfg.gs("Email Editor"), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (reply == JOptionPane.NO_OPTION)
        {
            return;
        }
        changed = true;
        labelStatus.setText(context.cfg.gs("Z.changed"));
        load(true);
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, context, context.cfg.gs("InviteUI.help"), "editor_" + context.preferences.getLocale() + ".html", false);
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

    private void actionPreviewClicked(ActionEvent e)
    {
        String text = owner.getFormattedText(editorPane.getText());
        EmailPreview preview = new EmailPreview(this, context, text, libMeta.repo.getLibraries().format,
                this.getX(), this.getY(), this.getWidth(), this.getHeight());
        preview.setVisible(true);
    }

    private void actionSaveClicked(ActionEvent e)
    {
        try
        {
            String text = editorPane.getText();
            template.setContent(text);
            template.write(type, libMeta.repo.getLibraries().format);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
        }

        savePreferences();
        setVisible(false);
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
        if (context.preferences.getEmailEditorXpos() != -1 && Utils.isOnScreen(context.preferences.getEmailEditorXpos(),
                context.preferences.getEmailEditorYpos()))
        {
            this.setLocation(context.preferences.getEmailEditorXpos(), context.preferences.getEmailEditorYpos());
            Dimension dim = new Dimension(context.preferences.getEmailEditorWidth(), context.preferences.getEmailEditorHeight());
            this.setSize(dim);
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

        template = new Template(context);
        load(false);

        editorPane.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    changed = true;
                    labelStatus.setText(context.cfg.gs("Z.changed"));
                }
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    changed = true;
                    labelStatus.setText(context.cfg.gs("Z.changed"));
                }
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent)
            {
                if (!loading)
                {
                    changed = true;
                    labelStatus.setText(context.cfg.gs("Z.changed"));
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
                    }
                    catch (CannotRedoException ex)
                    {
                        ex.printStackTrace();
                    }
                }
            }
        });

        if (!libMeta.repo.getLibraries().format.equalsIgnoreCase("html"))
        {
            toolBar.setVisible(false);
            buttonPreview.setVisible(false);
            buttonBrowser.setVisible(false);
        }

        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");
    }

    private void load(boolean reset)
    {
        try
        {
            loading = true;

            editorPane.setContentType("text/plain");
            content = template.getContent(type, libMeta.repo.getLibraries().format, reset);
            editorPane.setText(content);
            editorPane.requestFocus();
            editorPane.setCaretPosition(0);
        }
        catch (Exception e)
        {
            logger.error(e.getMessage());
        }
        loading = false;
    }

    private void savePreferences()
    {
        context.preferences.setEmailEditorHeight(this.getHeight());
        context.preferences.setEmailEditorWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setEmailEditorXpos(location.x);
        context.preferences.setEmailEditorYpos(location.y);
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

    private void toolLineClicked(ActionEvent e)
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

    private void toolListClicked(ActionEvent e)
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

    private void toolLogoClicked(ActionEvent e)
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

    private void toolSpaceClicked(ActionEvent e)
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

    private void toolUriClicked(ActionEvent e)
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

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTopButtons = new JPanel();
        panelLeft = new JPanel();
        toolBar = new JMenuBar();
        buttonBold = new JButton();
        buttonItalic = new JButton();
        buttonSpace = new JButton();
        buttonList = new JButton();
        buttonLine = new JButton();
        buttonUrl = new JButton();
        buttonLogo = new JButton();
        buttonDefault = new JButton();
        buttonPreview = new JButton();
        buttonBrowser = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        scrollPaneEditor = new JScrollPane();
        editorPane = new JEditorPane();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("EmailEditor.this.title"));
        setModal(true);
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(580, 420));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelTopButtons ========
                {
                    panelTopButtons.setLayout(new BorderLayout());

                    //======== panelLeft ========
                    {
                        panelLeft.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

                        //======== toolBar ========
                        {
                            toolBar.setMargin(new Insets(2, 8, 2, 8));

                            //---- buttonBold ----
                            buttonBold.setText(context.cfg.gs("EmailEditor.buttonBold.text"));
                            buttonBold.setFont(new Font("Inter", buttonBold.getFont().getStyle() | Font.BOLD, buttonBold.getFont().getSize()));
                            buttonBold.setPreferredSize(new Dimension(34, 34));
                            buttonBold.setMinimumSize(new Dimension(34, 34));
                            buttonBold.setMaximumSize(new Dimension(34, 34));
                            buttonBold.setToolTipText(context.cfg.gs("EmailEditor.buttonBold.toolTipText"));
                            buttonBold.setForeground(new Color(0x3592c4));
                            buttonBold.addActionListener(e -> toolBoldClicked(e));
                            toolBar.add(buttonBold);

                            //---- buttonItalic ----
                            buttonItalic.setText(context.cfg.gs("EmailEditor.buttonItalic.text"));
                            buttonItalic.setPreferredSize(new Dimension(34, 34));
                            buttonItalic.setMinimumSize(new Dimension(34, 34));
                            buttonItalic.setFont(new Font(Font.SERIF, buttonItalic.getFont().getStyle() | Font.ITALIC, buttonItalic.getFont().getSize()));
                            buttonItalic.setMaximumSize(new Dimension(34, 34));
                            buttonItalic.setToolTipText(context.cfg.gs("EmailEditor.buttonItalic.toolTipText"));
                            buttonItalic.setForeground(new Color(0x3592c4));
                            buttonItalic.addActionListener(e -> toolItalicClicked(e));
                            toolBar.add(buttonItalic);

                            //---- buttonSpace ----
                            buttonSpace.setIcon(new ImageIcon(getClass().getResource("/space.png")));
                            buttonSpace.setMaximumSize(new Dimension(34, 34));
                            buttonSpace.setMinimumSize(new Dimension(34, 34));
                            buttonSpace.setPreferredSize(new Dimension(34, 34));
                            buttonSpace.setToolTipText(context.cfg.gs("EmailEditor.buttonSpace.toolTipText"));
                            buttonSpace.addActionListener(e -> toolSpaceClicked(e));
                            toolBar.add(buttonSpace);

                            //---- buttonList ----
                            buttonList.setIcon(new ImageIcon(getClass().getResource("/list.png")));
                            buttonList.setMaximumSize(new Dimension(34, 34));
                            buttonList.setMinimumSize(new Dimension(34, 34));
                            buttonList.setPreferredSize(new Dimension(34, 34));
                            buttonList.setToolTipText(context.cfg.gs("EmailEditor.buttonList.toolTipText"));
                            buttonList.addActionListener(e -> toolListClicked(e));
                            toolBar.add(buttonList);

                            //---- buttonLine ----
                            buttonLine.setToolTipText(context.cfg.gs("EmailEditor.buttonLine.toolTipText"));
                            buttonLine.setIcon(new ImageIcon(getClass().getResource("/line.png")));
                            buttonLine.setMaximumSize(new Dimension(34, 34));
                            buttonLine.setPreferredSize(new Dimension(34, 34));
                            buttonLine.setMinimumSize(new Dimension(34, 34));
                            buttonLine.addActionListener(e -> toolLineClicked(e));
                            toolBar.add(buttonLine);

                            //---- buttonUrl ----
                            buttonUrl.setMinimumSize(new Dimension(34, 34));
                            buttonUrl.setPreferredSize(new Dimension(34, 34));
                            buttonUrl.setMaximumSize(new Dimension(34, 34));
                            buttonUrl.setToolTipText(context.cfg.gs("EmailEditor.buttonUrl.toolTipText"));
                            buttonUrl.setIcon(new ImageIcon(getClass().getResource("/link.png")));
                            buttonUrl.addActionListener(e -> toolUriClicked(e));
                            toolBar.add(buttonUrl);

                            //---- buttonLogo ----
                            buttonLogo.setPreferredSize(new Dimension(34, 34));
                            buttonLogo.setMinimumSize(new Dimension(34, 34));
                            buttonLogo.setMaximumSize(new Dimension(34, 34));
                            buttonLogo.setToolTipText(context.cfg.gs("EmailEditor.buttonLogo.toolTipText"));
                            buttonLogo.setIcon(new ImageIcon(getClass().getResource("/els-logo-18px.png")));
                            buttonLogo.addActionListener(e -> toolLogoClicked(e));
                            toolBar.add(buttonLogo);
                        }
                        panelLeft.add(toolBar);

                        //---- buttonDefault ----
                        buttonDefault.setText(context.cfg.gs("EmailEditor.buttonDefault.text"));
                        buttonDefault.setToolTipText(context.cfg.gs("EmailEditor.buttonDefault.toolTipText"));
                        buttonDefault.addActionListener(e -> actionDefaultClicked(e));
                        panelLeft.add(buttonDefault);

                        //---- buttonPreview ----
                        buttonPreview.setText(context.cfg.gs("EmailEditor.buttonPreview.text"));
                        buttonPreview.setToolTipText(context.cfg.gs("EmailEditor.buttonPreview.toolTipText"));
                        buttonPreview.addActionListener(e -> actionPreviewClicked(e));
                        panelLeft.add(buttonPreview);

                        //---- buttonBrowser ----
                        buttonBrowser.setText(context.cfg.gs("EmailEditor.buttonBrowser.text"));
                        buttonBrowser.setToolTipText(context.cfg.gs("EmailEditor.buttonBrowser.toolTipText"));
                        buttonBrowser.addActionListener(e -> actionBrowserClicked(e));
                        panelLeft.add(buttonBrowser);
                    }
                    panelTopButtons.add(panelLeft, BorderLayout.WEST);

                    //======== panelHelp ========
                    {
                        panelHelp.setPreferredSize(new Dimension(40, 38));
                        panelHelp.setMinimumSize(new Dimension(0, 38));
                        panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 6));

                        //---- labelHelp ----
                        labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelHelp.setPreferredSize(new Dimension(32, 30));
                        labelHelp.setMinimumSize(new Dimension(32, 30));
                        labelHelp.setMaximumSize(new Dimension(32, 30));
                        labelHelp.setToolTipText(context.cfg.gs("EmailEditor.labelHelp.toolTipText"));
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
                    panelTopButtons.add(panelHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelTopButtons, BorderLayout.NORTH);

                //======== scrollPaneEditor ========
                {
                    scrollPaneEditor.setViewportView(editorPane);
                }
                contentPanel.add(scrollPaneEditor, BorderLayout.CENTER);
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
                    new Insets(0, 8, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText(context.cfg.gs("Z.save"));
                okButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                okButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes"));
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
    private JPanel panelTopButtons;
    private JPanel panelLeft;
    public JMenuBar toolBar;
    private JButton buttonBold;
    private JButton buttonItalic;
    private JButton buttonSpace;
    private JButton buttonList;
    private JButton buttonLine;
    private JButton buttonUrl;
    private JButton buttonLogo;
    private JButton buttonDefault;
    private JButton buttonPreview;
    private JButton buttonBrowser;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JScrollPane scrollPaneEditor;
    private JEditorPane editorPane;
    private JPanel buttonBar;
    private JLabel labelStatus;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
