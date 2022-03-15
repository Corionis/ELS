package com.groksoft.els.gui;

import java.awt.event.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class About extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    GuiContext guiContext;
    Window parent;

    public About(Window owner, GuiContext gctxt)
    {
        super(owner);
        parent = owner;
        guiContext = gctxt;
        initComponents();

        labelVersion.setText("Version " + guiContext.cfg.getProgramVersion());
        labelBuild.setText(guiContext.context.main.getBuildStamp());
    }

    private void thisWindowClosed(WindowEvent e)
    {
        parent.requestFocus();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        labelForIcon = new JLabel();
        labelTitle = new JLabel();
        panel1 = new JPanel();
        labelVersion = new JLabel();
        labelBy = new JLabel();
        labelBuild = new JLabel();

        //======== this ========
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setTitle(guiContext.cfg.gs("About.this.title"));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(320, 210));
            dialogPane.setMinimumSize(new Dimension(320, 210));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setMinimumSize(new Dimension(296, 246));
                contentPanel.setPreferredSize(new Dimension(296, 246));
                contentPanel.setLayout(new BorderLayout());

                //---- labelForIcon ----
                labelForIcon.setIcon(new ImageIcon(getClass().getResource("/els-logo-98px.png")));
                contentPanel.add(labelForIcon, BorderLayout.WEST);

                //---- labelTitle ----
                labelTitle.setText("ELS - Entertainment Library Synchronizer");
                labelTitle.setFont(labelTitle.getFont().deriveFont(labelTitle.getFont().getStyle() | Font.BOLD, labelTitle.getFont().getSize() + 2f));
                labelTitle.setHorizontalTextPosition(SwingConstants.LEADING);
                contentPanel.add(labelTitle, BorderLayout.NORTH);

                //======== panel1 ========
                {
                    panel1.setLayout(new GridLayout(3, 1, 4, 8));

                    //---- labelVersion ----
                    labelVersion.setText("Version 4.0.0");
                    labelVersion.setHorizontalAlignment(SwingConstants.TRAILING);
                    panel1.add(labelVersion);

                    //---- labelBy ----
                    labelBy.setBorder(null);
                    panel1.add(labelBy);

                    //---- labelBuild ----
                    labelBuild.setText("Built 10 March 2022, 17:45:00 MST");
                    labelBuild.setHorizontalAlignment(SwingConstants.TRAILING);
                    labelBuild.setVerticalAlignment(SwingConstants.BOTTOM);
                    panel1.add(labelBuild);
                }
                contentPanel.add(panel1, BorderLayout.EAST);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel labelForIcon;
    private JLabel labelTitle;
    private JPanel panel1;
    private JLabel labelVersion;
    private JLabel labelBy;
    private JLabel labelBuild;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
