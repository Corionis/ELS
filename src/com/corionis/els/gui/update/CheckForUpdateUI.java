package com.corionis.els.gui.update;

import com.corionis.els.Context;
import com.corionis.els.gui.NavHelp;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
public class CheckForUpdateUI extends JDialog 
{
    private Context context;
    private String helpPath;
    private NavHelp helpDialog;

    public int result = -1;

    public CheckForUpdateUI(Window owner, Context context, String helpPath)
    {
        super(owner);
        this.context = context;
        this.helpPath = helpPath;
        initComponents();

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                noButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void yesClicked(ActionEvent e)
    {
        result = JOptionPane.YES_OPTION;
        if (helpDialog != null)
            helpDialog.dispose();
        setVisible(false);
    }

    private void noClicked(ActionEvent e)
    {
        result = JOptionPane.NO_OPTION;
        if (helpDialog != null)
            helpDialog.dispose();
        setVisible(false);
    }

    private void changListClicked(ActionEvent e)
    {
        if (helpDialog == null)
        {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            helpDialog = new NavHelp(context.mainFrame, context, context.cfg.gs("Navigator.recent.changes"), helpPath);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            if (!helpDialog.fault)
            {
                helpDialog.buttonFocus();
            }
        }
        else
        {
            helpDialog.setVisible(true);
            helpDialog.toFront();
            helpDialog.requestFocus();
            helpDialog.buttonFocus();
        }
    }

    private void thisWindowClosing(WindowEvent e)
    {
        noButton.doClick();
    }

    private void initComponents() 
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        labelHeader = new JLabel();
        vSpacer1 = new JPanel(null);
        labelInstalled = new JLabel();
        labelInstalledVersion = new JLabel();
        labelUpdate = new JLabel();
        labelUpdateVersion = new JLabel();
        vSpacer2 = new JPanel(null);
        labelPrompt = new JLabel();
        buttonBar = new JPanel();
        yesButton = new JButton();
        noButton = new JButton();
        changListButton = new JButton();

        //======== this ========
        setTitle(bundle.getString("CheckForUpdateUI.this.title"));
        setResizable(false);
        setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        setAlwaysOnTop(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(400, 190));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                //---- labelHeader ----
                labelHeader.setText(bundle.getString("CheckForUpdateUI.labelHeader.text"));
                labelHeader.setFont(labelHeader.getFont().deriveFont(labelHeader.getFont().getStyle() | Font.BOLD));
                contentPanel.add(labelHeader, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- vSpacer1 ----
                vSpacer1.setPreferredSize(new Dimension(10, 16));
                vSpacer1.setMinimumSize(new Dimension(12, 16));
                contentPanel.add(vSpacer1, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- labelInstalled ----
                labelInstalled.setText(bundle.getString("CheckForUpdateUI.labelInstalled.text"));
                contentPanel.add(labelInstalled, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 10, 4, 4), 0, 0));
                contentPanel.add(labelInstalledVersion, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- labelUpdate ----
                labelUpdate.setText(bundle.getString("CheckForUpdateUI.labelUpdate.text"));
                contentPanel.add(labelUpdate, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 10, 4, 4), 0, 0));
                contentPanel.add(labelUpdateVersion, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- vSpacer2 ----
                vSpacer2.setPreferredSize(new Dimension(10, 16));
                vSpacer2.setMinimumSize(new Dimension(12, 16));
                contentPanel.add(vSpacer2, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- labelPrompt ----
                labelPrompt.setText(bundle.getString("CheckForUpdateUI.labelPrompt.text"));
                contentPanel.add(labelPrompt, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 8, 0, 4), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 84, 0};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 1.0, 1.0};

                //---- yesButton ----
                yesButton.setText(bundle.getString("Z.yes"));
                yesButton.setPreferredSize(new Dimension(90, 22));
                yesButton.setToolTipText(bundle.getString("CheckForUpdateUI.yesButton.toolTipText"));
                yesButton.setFocusable(false);
                yesButton.setMnemonic(bundle.getString("Z.y").charAt(0));
                yesButton.addActionListener(e -> yesClicked(e));
                buttonBar.add(yesButton, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 4), 0, 0));

                //---- noButton ----
                noButton.setText(bundle.getString("Z.no"));
                noButton.setPreferredSize(new Dimension(90, 22));
                noButton.setToolTipText(bundle.getString("Z.cancel"));
                noButton.setFocusable(false);
                noButton.setMnemonic(bundle.getString("Z.n").charAt(0));
                noButton.addActionListener(e -> noClicked(e));
                buttonBar.add(noButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 4), 0, 0));

                //---- changListButton ----
                changListButton.setText(bundle.getString("CheckForUpdateUI.changListButton.text"));
                changListButton.setToolTipText(bundle.getString("CheckForUpdateUI.changListButton.toolTipText"));
                changListButton.setFocusable(false);
                changListButton.setMnemonic(bundle.getString("Z.c").charAt(0));
                changListButton.addActionListener(e -> changListClicked(e));
                buttonBar.add(changListButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JLabel labelHeader;
    private JPanel vSpacer1;
    private JLabel labelInstalled;
    public JLabel labelInstalledVersion;
    private JLabel labelUpdate;
    public JLabel labelUpdateVersion;
    private JPanel vSpacer2;
    private JLabel labelPrompt;
    private JPanel buttonBar;
    private JButton yesButton;
    private JButton noButton;
    private JButton changListButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
