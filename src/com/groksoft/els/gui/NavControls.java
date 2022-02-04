package com.groksoft.els.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ResourceBundle;

/**
 *
 */
public class NavControls extends JDialog
{
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    public JEditorPane controlsHelpText;
    private JPanel vSpacer1;
    private JPanel buttonBar;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    public NavControls(Window owner)
    {
        super(owner);
        initComponents();

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setVisible(false);
            }
        });

        okButton.addKeyListener(new KeyListener()
        {
            @Override
            public void keyPressed(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyReleased(KeyEvent keyEvent)
            {
            }

            @Override
            public void keyTyped(KeyEvent keyEvent)
            {
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER || keyEvent.getKeyChar() == KeyEvent.VK_ESCAPE)
                {
                    okButton.doClick();
                }
            }
        });

        getRootPane().setDefaultButton(okButton);
        okButton.requestFocus();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        controlsHelpText = new JEditorPane();
        vSpacer1 = new JPanel(null);
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setTitle("ELS Navigator Controls");
        setName("controlsDialog");
        setResizable(false);
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 400));
            dialogPane.setMinimumSize(new Dimension(570, 400));
            dialogPane.setMaximumSize(new Dimension(570, 400));
            dialogPane.setFocusable(false);
            dialogPane.setLayout(new BorderLayout(0, 4));

            //======== contentPanel ========
            {
                contentPanel.setMinimumSize(new Dimension(550, 360));
                contentPanel.setPreferredSize(new Dimension(550, 360));
                contentPanel.setMaximumSize(new Dimension(550, 360));
                contentPanel.setFocusable(false);
                contentPanel.setRequestFocusEnabled(false);
                contentPanel.setVerifyInputWhenFocusTarget(false);
                contentPanel.setDoubleBuffered(false);
                contentPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
                ((FlowLayout)contentPanel.getLayout()).setAlignOnBaseline(true);

                //---- controlsHelpText ----
                controlsHelpText.setPreferredSize(new Dimension(550, 360));
                controlsHelpText.setMinimumSize(new Dimension(550, 360));
                controlsHelpText.setContentType("text/html");
                controlsHelpText.setEditable(false);
                controlsHelpText.setAutoscrolls(false);
                controlsHelpText.setText("<html></html>");
                controlsHelpText.setBorder(new BevelBorder(BevelBorder.LOWERED));
                controlsHelpText.setBackground(UIManager.getColor("TextField.background"));
                controlsHelpText.setMaximumSize(new Dimension(550, 360));
                controlsHelpText.setRequestFocusEnabled(false);
                controlsHelpText.setMargin(new Insets(0, 0, 0, 0));
                controlsHelpText.setFocusable(false);
                contentPanel.add(controlsHelpText);
            }
            dialogPane.add(contentPanel, BorderLayout.NORTH);
            dialogPane.add(vSpacer1, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setMinimumSize(new Dimension(102, 36));
                buttonBar.setPreferredSize(new Dimension(102, 36));
                buttonBar.setFocusable(false);
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //---- okButton ----
                okButton.setText(bundle.getString("NavHelp.okButton.text"));
                okButton.setMnemonic(bundle.getString("NavHelp.okButton.mnemonic").charAt(0));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
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
}
