/*
 * Created by JFormDesigner on Sat Dec 18 20:22:39 MST 2021
 */

package com.groksoft.els.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 */
public class NavHelp extends JDialog {
    public NavHelp(Window owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        controlsHelpText = new JEditorPane();
        buttonBar = new JPanel();
        okButton = new JButton();

        //======== this ========
        setTitle("Navigator Controls");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(582, 300));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));

                //---- controlsHelpText ----
                controlsHelpText.setPreferredSize(new Dimension(550, 360));
                controlsHelpText.setMinimumSize(new Dimension(550, 360));
                controlsHelpText.setContentType("text/html");
                controlsHelpText.setEditable(false);
                controlsHelpText.setAutoscrolls(false);
                controlsHelpText.setText("<html></html>");
                controlsHelpText.setBorder(new MatteBorder(1, 1, 1, 1, Color.black));
                controlsHelpText.setBackground(UIManager.getColor("TextField.background"));
                contentPanel.add(controlsHelpText);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};

                //---- okButton ----
                okButton.setText(bundle.getString("NavHelp.okButton.text"));
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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    public JEditorPane controlsHelpText;
    private JPanel buttonBar;
    public JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
