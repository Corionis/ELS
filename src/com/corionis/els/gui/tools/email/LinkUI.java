package com.corionis.els.gui.tools.email;

import com.corionis.els.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ResourceBundle;

public class LinkUI extends JDialog
{
    Context context;
    private JEditorPane editorPane;
    private Logger logger = LogManager.getLogger("applog");

    public LinkUI(Window owner, Context context, JEditorPane editorPane)
    {
        super(owner);
        this.context = context;
        this.editorPane = editorPane;
        initComponents();
        initialize();
    }

    private void actionCancelClicked(ActionEvent e)
    {
        setVisible(false);
        editorPane.requestFocus();
    }

    private void actionOkClicked(ActionEvent ae)
    {
        try
        {
            String text = textFieldText.getText();
            String link = textFieldLink.getText();
            if (text.isEmpty() || link.isEmpty())
            {
                JOptionPane.showMessageDialog(LinkUI.this, context.cfg.gs("LinkUI.please.fill.all.the.fields"));
            }

            String replace = "<a href=\"" + link + "\" target=\"_blank\">" + text + "</a>";

            String selected = editorPane.getSelectedText();
            if (selected != null && !selected.isEmpty())
            {
                int s = editorPane.getSelectionStart();
                int e = editorPane.getSelectionEnd();
                editorPane.replaceSelection(replace);
            }
            else
            {
                int p = editorPane.getCaretPosition();
                editorPane.getDocument().insertString(p, replace, null);
            }

            setVisible(false);
            editorPane.requestFocus();
        }
        catch (BadLocationException ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void initialize()
    {
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        String text = editorPane.getSelectedText();
        if (text != null && !text.isEmpty())
            textFieldText.setText(text);
    }

    // ================================================================================================================

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        labelText = new JLabel();
        textFieldText = new JTextField();
        labelLink = new JLabel();
        textFieldLink = new JTextField();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(bundle.getString("LinkUI.this.title"));
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                ((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                //---- labelText ----
                labelText.setText(bundle.getString("LinkUI.labelText.text"));
                contentPanel.add(labelText, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- textFieldText ----
                textFieldText.setPreferredSize(new Dimension(280, 34));
                contentPanel.add(textFieldText, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 0), 0, 0));

                //---- labelLink ----
                labelLink.setText(bundle.getString("LinkUI.labelLink.text"));
                contentPanel.add(labelLink, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 4), 0, 0));

                //---- textFieldLink ----
                textFieldLink.setPreferredSize(new Dimension(280, 34));
                contentPanel.add(textFieldLink, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
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
    private JLabel labelText;
    private JTextField textFieldText;
    private JLabel labelLink;
    private JTextField textFieldLink;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
