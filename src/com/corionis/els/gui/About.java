package com.corionis.els.gui;

import java.awt.event.*;

import com.corionis.els.Configuration;
import com.corionis.els.Context;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class About extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    Context context;
    Window parent;

    public About(Window owner, Context context)
    {
        super(owner);
        parent = owner;
        this.context = context;
        initComponents();

        labelVersion.setText("Version " + Configuration.getBuildVersionName());
        labelBuild.setText(Configuration.getBuildDate());

        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setVisible(false);
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void dialogPaneMouseClicked(MouseEvent e)
    {
        setVisible(false);
    }

    private void thisWindowClosed(WindowEvent e)
    {
        parent.requestFocus();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        labelTitle = new JLabel();
        hSpacer1 = new JPanel(null);
        labelForIcon = new JLabel();
        panelVersion = new JPanel();
        labelVersion = new JLabel();
        labelBy = new JLabel();
        labelBuild = new JLabel();

        //======== this ========
        setAlwaysOnTop(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setResizable(false);
        setTitle(context.cfg.gs("About.title"));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(360, 210));
            dialogPane.setMinimumSize(new Dimension(360, 210));
            dialogPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    dialogPaneMouseClicked(e);
                }
            });
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setMinimumSize(new Dimension(296, 246));
                contentPanel.setPreferredSize(new Dimension(296, 246));
                contentPanel.setLayout(new BorderLayout());

                //---- labelTitle ----
                labelTitle.setText("Corionis ELS - Entertainment Library Synchronizer");
                labelTitle.setHorizontalTextPosition(SwingConstants.LEADING);
                labelTitle.setHorizontalAlignment(SwingConstants.RIGHT);
                contentPanel.add(labelTitle, BorderLayout.NORTH);

                //---- hSpacer1 ----
                hSpacer1.setPreferredSize(new Dimension(10, 80));
                hSpacer1.setMinimumSize(new Dimension(10, 80));
                contentPanel.add(hSpacer1, BorderLayout.WEST);

                //---- labelForIcon ----
                labelForIcon.setIcon(new ImageIcon(getClass().getResource("/els-logo-98px.png")));
                contentPanel.add(labelForIcon, BorderLayout.CENTER);

                //======== panelVersion ========
                {
                    panelVersion.setLayout(new GridLayout(3, 1, 4, 8));

                    //---- labelVersion ----
                    labelVersion.setText("Version 4.0.0");
                    labelVersion.setHorizontalAlignment(SwingConstants.TRAILING);
                    panelVersion.add(labelVersion);

                    //---- labelBy ----
                    labelBy.setBorder(null);
                    panelVersion.add(labelBy);

                    //---- labelBuild ----
                    labelBuild.setText("Built 19 December 2023, 17:45:00 MST");
                    labelBuild.setHorizontalAlignment(SwingConstants.TRAILING);
                    labelBuild.setVerticalAlignment(SwingConstants.BOTTOM);
                    panelVersion.add(labelBuild);
                }
                contentPanel.add(panelVersion, BorderLayout.EAST);
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
    private JLabel labelTitle;
    private JPanel hSpacer1;
    private JLabel labelForIcon;
    private JPanel panelVersion;
    private JLabel labelVersion;
    private JLabel labelBy;
    private JLabel labelBuild;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
