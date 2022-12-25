package com.groksoft.els.gui;

import java.awt.event.*;

import com.formdev.flatlaf.FlatLaf;
import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.Collections;
import javax.swing.*;
import javax.swing.border.*;

/**
 * ELS Settings dialog
 */
public class Settings extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    private GuiContext guiContext;
    private NavHelp helpDialog;
    private Settings thisDialog;

    /*
        TODO
         General
            + Default dry run
         Operations
            + Short or long options
            + Default console and debug levels
     */

    public Settings(Window owner, GuiContext ctxt)
    {
        super(owner);
        guiContext = ctxt;
        initComponents();
        thisDialog = this;
        setDialog();

        cancelButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                refreshLookAndFeel(guiContext.preferences.getLookAndFeel());
                if (helpDialog != null && helpDialog.isVisible())
                    helpDialog.setVisible(false);
                setVisible(false);
            }
        });

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (setPreferences())
                {
                    refreshLookAndFeel(guiContext.preferences.getLookAndFeel());
                    if (helpDialog != null && helpDialog.isVisible())
                        helpDialog.setVisible(false);
                    setVisible(false);
                }
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
                if (keyEvent.getKeyChar() == KeyEvent.VK_ENTER)
                {
                    if (keyEvent.getSource() == okButton)
                        okButton.doClick();
                }
            }
        });

        getRootPane().setDefaultButton(okButton);

        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        dateInfoButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (helpDialog == null)
                {
                    helpDialog = new NavHelp(owner, thisDialog, guiContext, guiContext.cfg.gs("Settings.date.format.help.title"), "formats_" + guiContext.preferences.getLocale() + ".html");
                }
                if (!helpDialog.isVisible())
                {
                    helpDialog.setVisible(true);
                    // offset the help dialog from the Settings dialog
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

        lookFeelComboBox.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JComboBox combobox = (JComboBox) actionEvent.getSource();
                int index = combobox.getSelectedIndex();
                refreshLookAndFeel(index);
            }
        });

        tabPlacementComboBox.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                if (actionEvent.getActionCommand().equals("comboBoxChanged"))
                {
                    guiContext.mainFrame.setBrowserTabs(tabPlacementComboBox.getSelectedIndex());
                    guiContext.browser.refreshAll();
                }
            }
        });

    }

    private void chooseColor(ActionEvent e) {
        Color color = new Color(Integer.parseInt(textFieldAccentColor.getText(), 16));
        color = JColorChooser.showDialog(guiContext.mainFrame, guiContext.cfg.gs("Settings.select.accent.color"), color);
        if (color != null)
        {
            textFieldAccentColor.setText(Utils.formatHex(color.getRed(), 2) +
                    Utils.formatHex(color.getGreen(), 2) +
                    Utils.formatHex(color.getBlue(), 2));
            try
            {
                // set accent color for current LaF
                FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#" + textFieldAccentColor.getText()));
                Class<? extends LookAndFeel> lafClass = UIManager.getLookAndFeel().getClass();
                FlatLaf.setup(lafClass.newInstance());
                FlatLaf.updateUI();
            }
            catch (Exception ex)
            {
                logger.error(Utils.getStackTrace(ex));
                JOptionPane.showMessageDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Z.exception") + ex.getMessage(),
                        guiContext.cfg.gs("Settings.this.title"), JOptionPane.ERROR_MESSAGE);

            }
        }
    }

    private void refreshLookAndFeel(int index)
    {
        try
        {
            if (index == 0)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            else
                UIManager.setLookAndFeel(guiContext.mainFrame.getLookAndFeel(index));

            try
            {
                // set accent color for current LaF
                FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#" + guiContext.preferences.getAccentColor()));
                Class<? extends LookAndFeel> lafClass = UIManager.getLookAndFeel().getClass();
                FlatLaf.setup(lafClass.newInstance());
                FlatLaf.updateUI();
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
                JOptionPane.showMessageDialog(guiContext.mainFrame,
                        guiContext.cfg.gs("Z.exception") + e.getMessage(),
                        guiContext.cfg.gs("Settings.this.title"), JOptionPane.ERROR_MESSAGE);

            }

            for (Frame frame : Frame.getFrames())
            {
                updateLAFRecursively(frame);
            }

            guiContext.mainFrame.setBrowserTabs(-1);
            guiContext.browser.refreshAll();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(settingsDialogPane, guiContext.cfg.gs("Settings.error.changing.look.n.feel") + e.getMessage(), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDialog()
    {
        // general
        preserveFileTimestampsCheckBox.setSelected(guiContext.preferences.isPreserveFileTimes());
        showDeleteConfirmationCheckBox.setSelected(guiContext.preferences.isShowDeleteConfirmation());
        showCcpConfirmationCheckBox.setSelected(guiContext.preferences.isShowCcpConfirmation());
        showDndConfirmationCheckBox.setSelected(guiContext.preferences.isShowDnDConfirmation());
        showTouchConfirmationCheckBox.setSelected(guiContext.preferences.isShowTouchConfirmation());

        // appearance
        lookFeelComboBox.setAutoscrolls(true);
        lookFeelComboBox.setSelectedIndex(guiContext.preferences.getLookAndFeel());
        // locale - add new locales to Preferences.availableLocales
        ComboBoxModel<String> model = localeComboBox.getModel();
        localeComboBox.setAutoscrolls(true);
        if (guiContext.context.main.availableLocales.length > 0)
        {
            localeComboBox.removeAllItems();
            for (String loc : guiContext.context.main.availableLocales)
            {
                localeComboBox.addItem(loc);
            }
        }
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
        if (guiContext.preferences.getAccentColor() == null || guiContext.preferences.getAccentColor().length() < 1)
        {
            guiContext.preferences.setAccentColor("2675BF");
        }
        textFieldAccentColor.setText(guiContext.preferences.getAccentColor());

        // browser
        autoRefreshCheckBox.setSelected(guiContext.preferences.isAutoRefresh());
        hideFilesInTreeCheckBox.setSelected(guiContext.preferences.isHideFilesInTree());
        hideHiddenFilesCheckBox.setSelected(guiContext.preferences.isHideHiddenFiles());
        sortCaseSensitiveCheckBox.setSelected(guiContext.preferences.isSortCaseInsensitive());
        sortFoldersBeforeFilesCheckBox.setSelected(guiContext.preferences.isSortFoldersBeforeFiles());
        sortReverseCheckBox.setSelected(guiContext.preferences.isSortReverse());
        tabPlacementComboBox.removeAllItems();
        model = tabPlacementComboBox.getModel();
        tabPlacementComboBox.addItem(guiContext.cfg.gs("Settings.tabPlacement.top"));
        tabPlacementComboBox.addItem(guiContext.cfg.gs("Settings.tabPlacement.bottom"));
        tabPlacementComboBox.addItem(guiContext.cfg.gs("Settings.tabPlacement.left"));
        tabPlacementComboBox.addItem(guiContext.cfg.gs("Settings.tabPlacement.right"));
        tabPlacementComboBox.setSelectedIndex(guiContext.preferences.getTabPlacementIndex());

        // operations

        // libraries

    }

    private boolean setPreferences()
    {
        try
        {
            new SimpleDateFormat(dateFormatTextField.getText());
        }
        catch (Exception e)
        {
            JOptionPane.showMessageDialog(guiContext.mainFrame, guiContext.cfg.gs("Settings.date.format.not.valid"), guiContext.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            settingsTabbedPane.setSelectedIndex(1);
            dateFormatTextField.requestFocus();
            guiContext.mainFrame.labelStatusMiddle.setText(guiContext.cfg.gs("Settings.date.format.not.valid"));
            return false;
        }

        // general
        guiContext.preferences.setPreserveFileTimes(preserveFileTimestampsCheckBox.isSelected());
        guiContext.preferences.setShowDeleteConfirmation(showDeleteConfirmationCheckBox.isSelected());
        guiContext.preferences.setShowCcpConfirmation(showCcpConfirmationCheckBox.isSelected());
        guiContext.preferences.setShowDnDConfirmation(showDndConfirmationCheckBox.isSelected());
        guiContext.preferences.setShowTouchConfirmation(showTouchConfirmationCheckBox.isSelected());

        // appearance
        guiContext.preferences.setLookAndFeel(lookFeelComboBox.getSelectedIndex());
        guiContext.preferences.setLocale((String) localeComboBox.getSelectedItem());
        guiContext.preferences.setBinaryScale(scaleCheckBox.isSelected());
        guiContext.preferences.setDateFormat(dateFormatTextField.getText());
        if (guiContext.preferences.getAccentColor().length() < 6) // use default if empty
            guiContext.preferences.setAccentColor("2675BF");
        guiContext.preferences.setAccentColor(textFieldAccentColor.getText());

        // browser
        guiContext.preferences.setAutoRefresh(autoRefreshCheckBox.isSelected());
        guiContext.preferences.setHideFilesInTree(hideFilesInTreeCheckBox.isSelected());
        guiContext.preferences.setHideHiddenFiles(hideHiddenFilesCheckBox.isSelected());
        guiContext.preferences.setSortCaseInsensitive(sortCaseSensitiveCheckBox.isSelected());
        guiContext.preferences.setSortFoldersBeforeFiles(sortFoldersBeforeFilesCheckBox.isSelected());
        guiContext.preferences.setSortReverse(sortReverseCheckBox.isSelected());
        guiContext.preferences.setTabPlacement(tabPlacementComboBox.getSelectedIndex());

        guiContext.mainFrame.labelStatusMiddle.setText("");
        return true;
    }

    private void thisWindowClosed(WindowEvent e) {
        if (helpDialog != null && helpDialog.isVisible())
            helpDialog.setVisible(false);
    }

    private void thisWindowClosing(WindowEvent e) {
        if (helpDialog != null && helpDialog.isVisible())
            helpDialog.setVisible(false);
    }

    public static void updateLAFRecursively(Window window)
    {
        for (Window childWindow : window.getOwnedWindows())
        {
            updateLAFRecursively(childWindow);
        }
        SwingUtilities.updateComponentTreeUI(window);
    }

    private void initComponents()
    {
        // <editor-fold desc="Generated component code (Fold)">
        // @formatter:off
        //
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        settingsDialogPane = new JPanel();
        settingsContentPanel = new JPanel();
        settingsTabbedPane = new JTabbedPane();
        generalPanel = new JPanel();
        preserveFileTimestampsLabel = new JLabel();
        preserveFileTimestampsCheckBox = new JCheckBox();
        showDeleteConfirmationLabel = new JLabel();
        showDeleteConfirmationCheckBox = new JCheckBox();
        showCcpConfirmationLabel = new JLabel();
        showCcpConfirmationCheckBox = new JCheckBox();
        showDndConfirmationLabel = new JLabel();
        showDndConfirmationCheckBox = new JCheckBox();
        showTouchConfirmationLabel = new JLabel();
        showTouchConfirmationCheckBox = new JCheckBox();
        apperancePanel = new JPanel();
        lookFeelLabel = new JLabel();
        lookFeelComboBox = new JComboBox<>();
        localeLabel = new JLabel();
        localeComboBox = new JComboBox<>();
        scaleLabel = new JLabel();
        scaleCheckBox = new JCheckBox();
        dateFormatLabel = new JLabel();
        dateFormatTextField = new JTextField();
        accentColorButtonLabel = new JLabel();
        dateInfoButton = new JButton();
        textFieldAccentColor = new JTextField();
        buttonChooseColor = new JButton();
        browserPanel = new JPanel();
        autoRefreshLabel = new JLabel();
        autoRefreshCheckBox = new JCheckBox();
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
        tabPlacementlabel = new JLabel();
        tabPlacementComboBox = new JComboBox<>();
        operationsPanel = new JPanel();
        librariesPanel = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("Settings.this.title"));
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
                        preserveFileTimestampsLabel.setText(guiContext.cfg.gs("Settings.preserveFileTimestampsLabel.text"));

                        //---- showDeleteConfirmationLabel ----
                        showDeleteConfirmationLabel.setText(guiContext.cfg.gs("Settings.showDeleteConfirmationLabel.text"));

                        //---- showCcpConfirmationLabel ----
                        showCcpConfirmationLabel.setText(guiContext.cfg.gs("Settings.showCcpConfirmationLabel.text"));

                        //---- showDndConfirmationLabel ----
                        showDndConfirmationLabel.setText(guiContext.cfg.gs("Settings.showDndConfirmationLabel.text"));

                        //---- showTouchConfirmationLabel ----
                        showTouchConfirmationLabel.setText(guiContext.cfg.gs("Settings.showTouchConfirmationLabel.text"));

                        GroupLayout generalPanelLayout = new GroupLayout(generalPanel);
                        generalPanel.setLayout(generalPanelLayout);
                        generalPanelLayout.setHorizontalGroup(
                            generalPanelLayout.createParallelGroup()
                                .addGroup(generalPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(preserveFileTimestampsLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)
                                            .addComponent(preserveFileTimestampsCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(showDeleteConfirmationLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)
                                            .addComponent(showDeleteConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(showCcpConfirmationLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)
                                            .addComponent(showCcpConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(showDndConfirmationLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)
                                            .addComponent(showDndConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(generalPanelLayout.createSequentialGroup()
                                            .addComponent(showTouchConfirmationLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addGap(12, 12, 12)
                                            .addComponent(showTouchConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)))
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        );
                        generalPanelLayout.setVerticalGroup(
                            generalPanelLayout.createParallelGroup()
                                .addGroup(generalPanelLayout.createSequentialGroup()
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addComponent(preserveFileTimestampsLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(preserveFileTimestampsCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup()
                                        .addComponent(showDeleteConfirmationLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showDeleteConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showCcpConfirmationLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showCcpConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showDndConfirmationLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showDndConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addGap(0, 0, 0)
                                    .addGroup(generalPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(showTouchConfirmationLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(showTouchConfirmationCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap(163, Short.MAX_VALUE))
                        );
                    }
                    settingsTabbedPane.addTab(guiContext.cfg.gs("Settings.generalPanel.tab.title"), generalPanel);
                    settingsTabbedPane.setMnemonicAt(0, guiContext.cfg.gs("Settings.generalPanel.tab.mnemonic").charAt(0));

                    //======== apperancePanel ========
                    {

                        //---- lookFeelLabel ----
                        lookFeelLabel.setText(guiContext.cfg.gs("Settings.lookFeelLabel.text"));

                        //---- lookFeelComboBox ----
                        lookFeelComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "System (Windows)",
                            "Metal",
                            "Nimbus",
                            "Flat light",
                            "Flat dark",
                            "IntelliJ light",
                            "IntelliJ dark"
                        }));

                        //---- localeLabel ----
                        localeLabel.setText(guiContext.cfg.gs("Settings.localeLabel.text"));

                        //---- localeComboBox ----
                        localeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "en_US"
                        }));

                        //---- scaleLabel ----
                        scaleLabel.setText(guiContext.cfg.gs("Settings.scaleLabel.text"));

                        //---- scaleCheckBox ----
                        scaleCheckBox.setToolTipText(guiContext.cfg.gs("Settings.scaleCheckBox.toolTipText"));

                        //---- dateFormatLabel ----
                        dateFormatLabel.setText(guiContext.cfg.gs("Settings.dateFormatLabel.text"));

                        //---- dateFormatTextField ----
                        dateFormatTextField.setText("yyyy-MM-dd hh:mm:ss aa");

                        //---- accentColorButtonLabel ----
                        accentColorButtonLabel.setText(guiContext.cfg.gs("Settings.accentColorLabel.text"));

                        //---- dateInfoButton ----
                        dateInfoButton.setText(guiContext.cfg.gs("Settings.button.dateInfo.text"));
                        dateInfoButton.setToolTipText(guiContext.cfg.gs("Settings.button.dateInfo.text.tooltip"));

                        //---- textFieldAccentColor ----
                        textFieldAccentColor.setToolTipText(guiContext.cfg.gs("Settings.textField.HintButtonColor.toolTipText"));

                        //---- buttonChooseColor ----
                        buttonChooseColor.setText(guiContext.cfg.gs("Settings.button.ChooseColor.text"));
                        buttonChooseColor.setToolTipText(guiContext.cfg.gs("Settings.button.ChooseColor.toolTipText"));
                        buttonChooseColor.addActionListener(e -> chooseColor(e));

                        GroupLayout apperancePanelLayout = new GroupLayout(apperancePanel);
                        apperancePanel.setLayout(apperancePanelLayout);
                        apperancePanelLayout.setHorizontalGroup(
                            apperancePanelLayout.createParallelGroup()
                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(apperancePanelLayout.createParallelGroup()
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addGroup(apperancePanelLayout.createParallelGroup()
                                                .addComponent(lookFeelLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(localeLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(scaleLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                                    .addComponent(dateFormatLabel, GroupLayout.PREFERRED_SIZE, 96, GroupLayout.PREFERRED_SIZE)
                                                    .addGap(12, 12, 12)
                                                    .addComponent(dateInfoButton)))
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(apperancePanelLayout.createParallelGroup()
                                                .addComponent(lookFeelComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(scaleCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                                .addComponent(dateFormatTextField, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(accentColorButtonLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(textFieldAccentColor, GroupLayout.PREFERRED_SIZE, 104, GroupLayout.PREFERRED_SIZE)
                                            .addGap(8, 8, 8)
                                            .addComponent(buttonChooseColor)))
                                    .addContainerGap(17, Short.MAX_VALUE))
                        );
                        apperancePanelLayout.setVerticalGroup(
                            apperancePanelLayout.createParallelGroup()
                                .addGroup(apperancePanelLayout.createSequentialGroup()
                                    .addGap(0, 0, 0)
                                    .addGroup(apperancePanelLayout.createParallelGroup()
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(lookFeelLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(localeLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(scaleLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(dateFormatLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addComponent(lookFeelComboBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(localeComboBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(scaleCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(apperancePanelLayout.createSequentialGroup()
                                            .addGap(111, 111, 111)
                                            .addGroup(apperancePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                                .addComponent(dateInfoButton)
                                                .addComponent(dateFormatTextField, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))))
                                    .addGap(0, 0, 0)
                                    .addGroup(apperancePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(accentColorButtonLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(buttonChooseColor)
                                        .addComponent(textFieldAccentColor, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap())
                        );
                    }
                    settingsTabbedPane.addTab(guiContext.cfg.gs("Settings.appearance.tab.title"), apperancePanel);
                    settingsTabbedPane.setMnemonicAt(1, guiContext.cfg.gs("Settings.apperancePanel.tab.mnemonic").charAt(0));

                    //======== browserPanel ========
                    {

                        //---- autoRefreshLabel ----
                        autoRefreshLabel.setText("Auto-refresh:");

                        //---- hideFilesInTreeLabel ----
                        hideFilesInTreeLabel.setText(guiContext.cfg.gs("Settings.hideFilesInTreeLabel.text"));

                        //---- hideHiddenFilesLabel ----
                        hideHiddenFilesLabel.setText(guiContext.cfg.gs("Settings.hideHiddenFilesLabel.text"));

                        //---- sortCaseSensitiveLabel ----
                        sortCaseSensitiveLabel.setText(guiContext.cfg.gs("Settings.sortCaseSensitiveLabel.text"));

                        //---- sortFoldersBeforeFilesLabel ----
                        sortFoldersBeforeFilesLabel.setText(guiContext.cfg.gs("Settings.sortFoldersBeforeFilesLabel.text"));

                        //---- sortReverseLabel ----
                        sortReverseLabel.setText(guiContext.cfg.gs("Settings.sortReverseLabel.text"));

                        //---- tabPlacementlabel ----
                        tabPlacementlabel.setText(guiContext.cfg.gs("Settings.tabPlacementLabel.text"));

                        //---- tabPlacementComboBox ----
                        tabPlacementComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Top",
                            "Bottom",
                            "Left",
                            "Right"
                        }));
                        tabPlacementComboBox.setPreferredSize(new Dimension(100, 30));

                        GroupLayout browserPanelLayout = new GroupLayout(browserPanel);
                        browserPanel.setLayout(browserPanelLayout);
                        browserPanelLayout.setHorizontalGroup(
                            browserPanelLayout.createParallelGroup()
                                .addGroup(browserPanelLayout.createSequentialGroup()
                                    .addContainerGap()
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addComponent(autoRefreshLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideFilesInTreeLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideHiddenFilesLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortCaseSensitiveLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortFoldersBeforeFilesLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortReverseLabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tabPlacementlabel, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE))
                                    .addGap(12, 12, 12)
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addComponent(autoRefreshCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideFilesInTreeCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(hideHiddenFilesCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortCaseSensitiveCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortFoldersBeforeFilesCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sortReverseCheckBox, GroupLayout.PREFERRED_SIZE, 238, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tabPlacementComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        );
                        browserPanelLayout.setVerticalGroup(
                            browserPanelLayout.createParallelGroup()
                                .addGroup(browserPanelLayout.createSequentialGroup()
                                    .addGroup(browserPanelLayout.createParallelGroup()
                                        .addGroup(browserPanelLayout.createSequentialGroup()
                                            .addComponent(autoRefreshLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(hideFilesInTreeLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(hideHiddenFilesLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortCaseSensitiveLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortFoldersBeforeFilesLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortReverseLabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE))
                                        .addGroup(browserPanelLayout.createSequentialGroup()
                                            .addComponent(autoRefreshCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(hideFilesInTreeCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(hideHiddenFilesCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortCaseSensitiveCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortFoldersBeforeFilesCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, 0)
                                            .addComponent(sortReverseCheckBox, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)))
                                    .addGap(0, 0, 0)
                                    .addGroup(browserPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(tabPlacementlabel, GroupLayout.PREFERRED_SIZE, 36, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(tabPlacementComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
                                    .addContainerGap())
                        );
                    }
                    settingsTabbedPane.addTab(guiContext.cfg.gs("Settings.browserPanel.tab.title"), browserPanel);
                    settingsTabbedPane.setMnemonicAt(2, guiContext.cfg.gs("Settings.browserPanel.tab.mnemonic").charAt(0));

                    //======== operationsPanel ========
                    {
                        operationsPanel.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    settingsTabbedPane.addTab(guiContext.cfg.gs("Settings.operationsPanel.tab.title"), operationsPanel);
                    settingsTabbedPane.setMnemonicAt(3, guiContext.cfg.gs("Settings.operationsPanel.tab.mnemonic").charAt(0));

                    //======== librariesPanel ========
                    {
                        librariesPanel.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    settingsTabbedPane.addTab(guiContext.cfg.gs("Settings.librariesPanel.tab.title"), librariesPanel);
                    settingsTabbedPane.setMnemonicAt(4, guiContext.cfg.gs("Settings.librariesPanel.tab.mnemonic").charAt(0));
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
                okButton.setText(guiContext.cfg.gs("Z.save"));
                okButton.setToolTipText(guiContext.cfg.gs("Z.save.toolTip.text"));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(guiContext.cfg.gs("Z.cancel.changes.toolTipText"));
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
    private JLabel showDeleteConfirmationLabel;
    private JCheckBox showDeleteConfirmationCheckBox;
    private JLabel showCcpConfirmationLabel;
    private JCheckBox showCcpConfirmationCheckBox;
    private JLabel showDndConfirmationLabel;
    private JCheckBox showDndConfirmationCheckBox;
    private JLabel showTouchConfirmationLabel;
    private JCheckBox showTouchConfirmationCheckBox;
    private JPanel apperancePanel;
    private JLabel lookFeelLabel;
    private JComboBox<String> lookFeelComboBox;
    private JLabel localeLabel;
    private JComboBox<String> localeComboBox;
    private JLabel scaleLabel;
    private JCheckBox scaleCheckBox;
    private JLabel dateFormatLabel;
    private JTextField dateFormatTextField;
    private JLabel accentColorButtonLabel;
    private JButton dateInfoButton;
    private JTextField textFieldAccentColor;
    private JButton buttonChooseColor;
    private JPanel browserPanel;
    private JLabel autoRefreshLabel;
    private JCheckBox autoRefreshCheckBox;
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
    private JLabel tabPlacementlabel;
    private JComboBox<String> tabPlacementComboBox;
    private JPanel operationsPanel;
    private JPanel librariesPanel;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>
}
