package com.groksoft.els.gui;

import java.awt.event.*;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ELS Settings dialog
 */
public class Settings extends JDialog
{
    GuiContext guiContext;
    private transient Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    Settings thisDialog;

    public Settings(Window owner, GuiContext ctxt)
    {
        super(owner);
        guiContext = ctxt;
        initComponents();
        thisDialog = this;
        setDialog();

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setPreferences();
                guiContext.browser.refreshAll();
                if (helpDialog != null && helpDialog.isVisible())
                    helpDialog.setVisible(false);
                setVisible(false);
            }
        });

        cancelButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (helpDialog != null && helpDialog.isVisible())
                    helpDialog.setVisible(false);
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
                    if (keyEvent.getSource() == okButton)
                        okButton.doClick();
                    else if (keyEvent.getSource() == cancelButton)
                        cancelButton.doClick();
                }
            }
        });

        lookFeelComboBox.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JComboBox combobox = (JComboBox) actionEvent.getSource();
                int index = combobox.getSelectedIndex();
                try
                {
                    if (index == 0)
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    else
                        UIManager.setLookAndFeel(guiContext.form.getLookAndFeel(index));

                    for (Frame frame : Frame.getFrames())
                    {
                        updateLAFRecursively(frame);
                    }
                    guiContext.form.rotateBrowserTabs();
                }
                catch (Exception e)
                {
                    logger.error(Utils.getStackTrace(e));
                    JOptionPane.showMessageDialog(settingsDialogPane, "Error changing Look 'n Feel:  " + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        dateInfoButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (helpDialog == null)
                {
                    helpDialog = new NavHelp(owner, thisDialog, guiContext, "Date Format Help", "formats_" + guiContext.preferences.getLocale() + ".html");
                }
                if (!helpDialog.isVisible())
                {
                    helpDialog.setVisible(true);
                    // offset the help dialog from the settings dialog
                    Point loc = thisDialog.getLocation();
                    loc.x = loc.x + 32;
                    loc.y = loc.y + 32;
                    helpDialog.setLocation(loc);
                }
                else
                {
                    helpDialog.toFront();
                }
            }
        });

        getRootPane().setDefaultButton(okButton);
    }

    private void setDialog()
    {
        // general
        preserveFileTimestampsCheckBox.setSelected(guiContext.preferences.isPreserveFileTimes());
        restoreSessionCheckBox.setSelected(false);
        showConfirmationsCheckBox.setSelected(guiContext.preferences.isShowConfirmations());

        // appearance
        lookFeelComboBox.setAutoscrolls(true);
        lookFeelComboBox.setSelectedIndex(guiContext.preferences.getLookAndFeel());
        ComboBoxModel<String> model = localeComboBox.getModel(); // TODO Scan for available locales & populate combobox
        localeComboBox.setAutoscrolls(true);
        for (int i = 0; i < model.getSize(); ++i)
        {
            String loc = model.getElementAt(i);
            if (loc.equals(guiContext.preferences.getLocale()))
            {
                localeComboBox.setSelectedIndex(i);
                break;
            }
        }
        scaleCheckBox.setSelected(guiContext.preferences.isBinaryScale());
        dateFormatTextField.setText(guiContext.preferences.getDateFormat());

        // browser
        hideFilesInTreeCheckBox.setSelected(guiContext.preferences.isHideFilesInTree());
        hideHiddenFilesCheckBox.setSelected(guiContext.preferences.isHideHiddenFiles());
        sortCaseSensitiveCheckBox.setSelected(guiContext.preferences.isSortCaseInsensitive());
        sortFoldersBeforeFilesCheckBox.setSelected(guiContext.preferences.isSortFoldersBeforeFiles());
        sortReverseCheckBox.setSelected(guiContext.preferences.isSortReverse());

        // backup

        // libraries

    }

    private void setPreferences()
    {
        // general
        guiContext.preferences.setPreserveFileTimes(preserveFileTimestampsCheckBox.isSelected());
        // TODO Add Restore Session?
        guiContext.preferences.setShowConfirmations(showConfirmationsCheckBox.isSelected());

        // appearance
        guiContext.preferences.setLookAndFeel(lookFeelComboBox.getSelectedIndex());
        guiContext.preferences.setLocale((String) localeComboBox.getSelectedItem());
        guiContext.preferences.setBinaryScale(scaleCheckBox.isSelected());
        guiContext.preferences.setDateFormat(dateFormatTextField.getText());

        // browser
        guiContext.preferences.setHideFilesInTree(hideFilesInTreeCheckBox.isSelected());
        guiContext.preferences.setHideHiddenFiles(hideHiddenFilesCheckBox.isSelected());
        guiContext.preferences.setSortCaseInsensitive(sortCaseSensitiveCheckBox.isSelected());
        guiContext.preferences.setSortFoldersBeforeFiles(sortFoldersBeforeFilesCheckBox.isSelected());
        guiContext.preferences.setSortReverse(sortReverseCheckBox.isSelected());
    }

    public static void updateLAFRecursively(Window window)
    {
        for (Window childWindow : window.getOwnedWindows())
        {
            updateLAFRecursively(childWindow);
        }
        SwingUtilities.updateComponentTreeUI(window);
    }

    private void thisWindowClosed(WindowEvent e) {
        if (helpDialog != null && helpDialog.isVisible())
            helpDialog.setVisible(false);
    }

    private void thisWindowClosing(WindowEvent e) {
        if (helpDialog != null && helpDialog.isVisible())
            helpDialog.setVisible(false);
    }

    private void initComponents()
    {
        // <editor-fold desc="Generated component code (Fold)">
        // @formatter:off
        //
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        settingsDialogPane = new JPanel();
        settingsContentPanel = new JPanel();
        settingsTabbedPane = new JTabbedPane();
        generalPanel = new JPanel();
        preserveFileTimestampsLabel = new JLabel();
        preserveFileTimestampsCheckBox = new JCheckBox();
        restoreSessionLabel = new JLabel();
        restoreSessionCheckBox = new JCheckBox();
        showConfirmationsLabel = new JLabel();
        showConfirmationsCheckBox = new JCheckBox();
        apperancePanel = new JPanel();
        lookFeelLabel = new JLabel();
        lookFeelComboBox = new JComboBox<>();
        localeLabel = new JLabel();
        localeComboBox = new JComboBox<>();
        scaleLabel = new JLabel();
        scaleCheckBox = new JCheckBox();
        dateFormatLabel = new JLabel();
        dateFormatTextField = new JTextField();
        hintButtonColorLabel = new JLabel();
        dateInfoButton = new JButton();
        browserPanel = new JPanel();
        hideFilesInTreeLabel = new JLabel();
        hideFilesInTreeCheckBox = new JCheckBox();
        hideHiddenFilesLabel = new JLabel();
        hideHiddenFilesCheckBox = new JCheckBox();
        sortCaseSensitiveLabel = new JLabel();
        sortCaseSensitiveCheckBox = new JCheckBox();
        sortFoldersBeforeFilesLabel = new JLabel();
        sortFoldersBeforeFilesCheckBox = new JCheckBox();
        sortReverseLabel = new JLabel();
        sortReverseCheckBox = new JCheckBox();
        backupPanel = new JPanel();
        librariesPanel = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle("ELS Navigator Settings");
        setMinimumSize(new Dimension(100, 50));
        setName("settingsDialog");
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                thisWindowClosed(e);
            }
            @Override
            public void windowClosing(WindowEvent e) {
                thisWindowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== settingsDialogPane ========
        {
            settingsDialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            settingsDialogPane.setMinimumSize(new Dimension(500, 100));
            settingsDialogPane.setPreferredSize(new Dimension(570, 470));
            settingsDialogPane.setLayout(new BorderLayout());

            //======== settingsContentPanel ========
            {
                settingsContentPanel.setLayout(new BoxLayout(settingsContentPanel, BoxLayout.X_AXIS));

                //======== settingsTabbedPane ========
                {
                    settingsTabbedPane.setTabPlacement(SwingConstants.LEFT);

                    //======== generalPanel ========
                    {

                        //---- preserveFileTimestampsLabel ----
                        preserveFileTimestampsLabel.setText(bundle.getString("Settings.preserveFileTimestampsLabel.text"));

                        //---- restoreSessionLabel ----
                        restoreSessionLabel.setText(bundle.getString("Settings.restoreSessionLabel.text"));

                        //---- showConfirmationsLabel ----
                        showConfirmationsLabel.setText(bundle.getString("Settings.showConfirmationsLabel.text"));

                        GroupLayout generalPanelLayout = new GroupLayout(generalPanel);
                        generalPanel.setLayout(generalPanelLayout);
                        generalPanelLayout.setHorizontalGroup(
                            generalPanelLayout.createParallelGroup()
                                .addGroup(generalPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(preserveFileTimestampsLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(preserveFileTimestampsCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(restoreSessionLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(restoreSessionCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(showConfirmationsLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(showConfirmationsCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)))
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        );
                        generalPanelLayout.setVerticalGroup(
                            generalPanelLayout.createParallelGroup()
                                .addGroup(generalPanelLayout.createSequentialGroup()
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(preserveFileTimestampsCheckBox, GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE)
                                        .addComponent(preserveFileTimestampsLabel, GroupLayout.DEFAULT_SIZE, 36, Short.MAX_VALUE))
                                    .addGap(1, 1, 1)
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addComponent(restoreSessionLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(restoreSessionCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addGap(1, 1, 1)
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addComponent(showConfirmationsLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showConfirmationsCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap(265, Short.MAX_VALUE))
                        );
                    }
                    settingsTabbedPane.addTab(bundle.getString("Settings.generalPanel.tab.title"), generalPanel);

                    //======== apperancePanel ========
                    {

                        //---- lookFeelLabel ----
                        lookFeelLabel.setText(bundle.getString("Settings.lookFeelLabel.text"));

                        //---- lookFeelComboBox ----
                        lookFeelComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "System default, use for Windows",
                            "Metal",
                            "Nimbus",
                            "Flat light",
                            "Flat dark",
                            "IntelliJ light",
                            "IntelliJ dark"
                        }));

                        //---- localeLabel ----
                        localeLabel.setText(bundle.getString("Settings.localeLabel.text"));

                        //---- localeComboBox ----
                        localeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "en_US"
                        }));

                        //---- scaleLabel ----
                        scaleLabel.setText(bundle.getString("Settings.scaleLabel.text"));

                        //---- scaleCheckBox ----
                        scaleCheckBox.setText("uncheck for decimal (1000) K");

                        //---- dateFormatLabel ----
                        dateFormatLabel.setText(bundle.getString("Settings.dateFormatLabel.text"));

                        //---- dateFormatTextField ----
                        dateFormatTextField.setText("yyyy-MM-dd hh:mm:ss aa");

                        //---- hintButtonColorLabel ----
                        hintButtonColorLabel.setText(bundle.getString("Settings.hintButtonColorLabel.text"));

                        //---- dateInfoButton ----
                        dateInfoButton.setText("Info");
                        dateInfoButton.setToolTipText("Display date formatting help");

                        GroupLayout apperancePanelLayout = new GroupLayout(apperancePanel);
                        apperancePanel.setLayout(apperancePanelLayout);
                        apperancePanelLayout.setHorizontalGroup(
                            apperancePanelLayout.createParallelGroup()
                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(apperancePanelLayout.createParallelGroup()
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addGroup(apperancePanelLayout.createParallelGroup()
                                                .addComponent(lookFeelLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(localeLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(scaleLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                                    .addComponent(dateFormatLabel, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                    .addComponent(dateInfoButton)))
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(apperancePanelLayout.createParallelGroup()
                                                .addComponent(lookFeelComboBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(scaleCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(dateFormatTextField, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(hintButtonColorLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, Short.MAX_VALUE)))
                                    .addContainerGap())
                        );
                        apperancePanelLayout.setVerticalGroup(
                            apperancePanelLayout.createParallelGroup()
                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                    .addGap(1, 1, 1)
                                    .addGroup(apperancePanelLayout.createParallelGroup()
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(lookFeelLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(2, 2, 2)
                                            .addComponent(localeLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(2, 2, 2)
                                            .addComponent(scaleLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(dateFormatLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(lookFeelComboBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(scaleCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(dateFormatTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addGap(114, 114, 114)
                                            .addComponent(dateInfoButton)))
                                    .addGap(1, 1, 1)
                                    .addComponent(hintButtonColorLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                    .addContainerGap())
                        );
                    }
                    settingsTabbedPane.addTab(bundle.getString("Settings.apperancePanel.tab.title"), apperancePanel);

                    //======== browserPanel ========
                    {

                        //---- hideFilesInTreeLabel ----
                        hideFilesInTreeLabel.setText(bundle.getString("Settings.hideFilesInTreeLabel.text"));

                        //---- hideHiddenFilesLabel ----
                        hideHiddenFilesLabel.setText(bundle.getString("Settings.hideHiddenFilesLabel.text"));

                        //---- sortCaseSensitiveLabel ----
                        sortCaseSensitiveLabel.setText(bundle.getString("Settings.sortCaseSensitiveLabel.text"));

                        //---- sortFoldersBeforeFilesLabel ----
                        sortFoldersBeforeFilesLabel.setText(bundle.getString("Settings.sortFoldersBeforeFilesLabel.text"));

                        //---- sortReverseLabel ----
                        sortReverseLabel.setText(bundle.getString("Settings.sortReverseLabel.text"));

                        GroupLayout browserPanelLayout = new GroupLayout(browserPanel);
                        browserPanel.setLayout(browserPanelLayout);
                        browserPanelLayout.setHorizontalGroup(
                            browserPanelLayout.createParallelGroup()
                                .addGroup(browserPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addComponent(hideFilesInTreeLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideHiddenFilesLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortCaseSensitiveLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortFoldersBeforeFilesLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortReverseLabel, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addComponent(hideFilesInTreeCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideHiddenFilesCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortCaseSensitiveCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortFoldersBeforeFilesCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortReverseCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap())
                        );
                        browserPanelLayout.setVerticalGroup(
                            browserPanelLayout.createParallelGroup()
                                .addGroup(browserPanelLayout.createSequentialGroup()
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addGroup(browserPanelLayout.createSequentialGroup()
                                            .addComponent(hideFilesInTreeLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(hideHiddenFilesLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortCaseSensitiveLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortFoldersBeforeFilesLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortReverseLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(browserPanelLayout.createSequentialGroup()
                                            .addComponent(hideFilesInTreeCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(hideHiddenFilesCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortCaseSensitiveCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortFoldersBeforeFilesCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(1, 1, 1)
                                            .addComponent(sortReverseCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)))
                                    .addGap(187, 187, 187))
                        );
                    }
                    settingsTabbedPane.addTab(bundle.getString("Settings.browserPanel.tab.title"), browserPanel);

                    //======== backupPanel ========
                    {
                        backupPanel.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    settingsTabbedPane.addTab(bundle.getString("Settings.backupPanel.tab.title"), backupPanel);

                    //======== librariesPanel ========
                    {
                        librariesPanel.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    settingsTabbedPane.addTab(bundle.getString("Settings.librariesPanel.tab.title"), librariesPanel);
                }
                settingsContentPanel.add(settingsTabbedPane);
            }
            settingsDialogPane.add(settingsContentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(bundle.getString("Settings.okButton.text"));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(bundle.getString("Settings.cancelButton.text"));
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            settingsDialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(settingsDialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
        //
        // @formatter:on
        // </editor-fold>
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel settingsDialogPane;
    private JPanel settingsContentPanel;
    private JTabbedPane settingsTabbedPane;
    private JPanel generalPanel;
    private JLabel preserveFileTimestampsLabel;
    private JCheckBox preserveFileTimestampsCheckBox;
    private JLabel restoreSessionLabel;
    private JCheckBox restoreSessionCheckBox;
    private JLabel showConfirmationsLabel;
    private JCheckBox showConfirmationsCheckBox;
    private JPanel apperancePanel;
    private JLabel lookFeelLabel;
    private JComboBox<String> lookFeelComboBox;
    private JLabel localeLabel;
    private JComboBox<String> localeComboBox;
    private JLabel scaleLabel;
    private JCheckBox scaleCheckBox;
    private JLabel dateFormatLabel;
    private JTextField dateFormatTextField;
    private JLabel hintButtonColorLabel;
    private JButton dateInfoButton;
    private JPanel browserPanel;
    private JLabel hideFilesInTreeLabel;
    private JCheckBox hideFilesInTreeCheckBox;
    private JLabel hideHiddenFilesLabel;
    private JCheckBox hideHiddenFilesCheckBox;
    private JLabel sortCaseSensitiveLabel;
    private JCheckBox sortCaseSensitiveCheckBox;
    private JLabel sortFoldersBeforeFilesLabel;
    private JCheckBox sortFoldersBeforeFilesCheckBox;
    private JLabel sortReverseLabel;
    private JCheckBox sortReverseCheckBox;
    private JPanel backupPanel;
    private JPanel librariesPanel;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>
}
