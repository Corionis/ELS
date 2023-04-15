package com.groksoft.els.gui;

import java.awt.*;
import javax.swing.*;

/**
 * Startup dialog
 * <br/>
 * Just definition. See GuiLogAppender for implementation.
 */
public class Startup extends JFrame 
{
    public Startup() 
    {
        initComponents();
    }

    private void initComponents() 
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelTopSpacer = new JPanel();
        panelBanner = new JPanel();
        labelLogo = new JLabel();
        labelVersion = new JLabel();
        panelLogText = new JPanel();
        startupTextField = new JTextField();

        //======== this ========
        setResizable(false);
        setPreferredSize(new Dimension(400, 114));
        setMinimumSize(new Dimension(400, 114));
        setMaximumSize(new Dimension(400, 114));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== panelTopSpacer ========
        {
            panelTopSpacer.setPreferredSize(new Dimension(100, 6));
            panelTopSpacer.setMinimumSize(new Dimension(100, 6));
            panelTopSpacer.setMaximumSize(new Dimension(100, 6));
            panelTopSpacer.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        }
        contentPane.add(panelTopSpacer, BorderLayout.NORTH);

        //======== panelBanner ========
        {
            panelBanner.setMaximumSize(new Dimension(376, 48));
            panelBanner.setMinimumSize(new Dimension(376, 48));
            panelBanner.setPreferredSize(new Dimension(376, 48));
            panelBanner.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 0));

            //---- labelLogo ----
            labelLogo.setIcon(new ImageIcon(getClass().getResource("/els-logo-48px.png")));
            labelLogo.setPreferredSize(new Dimension(48, 48));
            labelLogo.setHorizontalAlignment(SwingConstants.CENTER);
            panelBanner.add(labelLogo);

            //---- labelVersion ----
            labelVersion.setText("Version 4.0.0");
            panelBanner.add(labelVersion);
        }
        contentPane.add(panelBanner, BorderLayout.CENTER);

        //======== panelLogText ========
        {
            panelLogText.setMaximumSize(new Dimension(376, 26));
            panelLogText.setMinimumSize(new Dimension(376, 26));
            panelLogText.setPreferredSize(new Dimension(376, 26));
            panelLogText.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

            //---- startupTextField ----
            startupTextField.setPreferredSize(new Dimension(384, 20));
            startupTextField.setMinimumSize(new Dimension(384, 0));
            startupTextField.setMaximumSize(new Dimension(384, 20));
            startupTextField.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            startupTextField.setEditable(false);
            startupTextField.setHorizontalAlignment(SwingConstants.CENTER);
            startupTextField.setBorder(null);
            startupTextField.setMargin(new Insets(2, 0, 18, 0));
            panelLogText.add(startupTextField);
        }
        contentPane.add(panelLogText, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    public JPanel panelTopSpacer;
    public JPanel panelBanner;
    public JLabel labelLogo;
    public JLabel labelVersion;
    public JPanel panelLogText;
    public JTextField startupTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
