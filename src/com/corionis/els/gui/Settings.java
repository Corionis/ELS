package com.corionis.els.gui;

import java.awt.event.*;

import com.formdev.flatlaf.FlatLaf;
import com.corionis.els.Configuration;
import com.corionis.els.Context;
import com.corionis.els.Utils;
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
    private Context context;
    private NavHelp helpDialog;
    private int laf;
    private Settings thisDialog;

    public Settings(Window owner, Context context)
    {
        super(owner);
        this.context = context;
        initComponents();
        thisDialog = this;
        setDialog();

        cancelButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                context.preferences.setLookAndFeel(laf);
                refreshLookAndFeel(context.preferences.getLookAndFeel());
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
                    refreshLookAndFeel(context.preferences.getLookAndFeel());
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
                    helpDialog = new NavHelp(owner, thisDialog, context, context.cfg.gs("Settings.date.format.help.title"), "formats_" + context.preferences.getLocale() + ".html");
                }
                if (!helpDialog.fault)
                {
                    if (!helpDialog.isVisible())
                    {
                        helpDialog.setVisible(true);
                    }
                    else
                    {
                        helpDialog.toFront();
                    }
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
                if (Utils.getOS().equals("Linux") && index == 0) // System, for Windows, will fail on Linux
                {
                    index = 4;
                    combobox.setSelectedIndex(index);
                }
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
                    context.mainFrame.setBrowserTabs(tabPlacementComboBox.getSelectedIndex());
                    context.browser.refreshAll();
                }
            }
        });

        laf = context.preferences.getLookAndFeel();
    }

    private void chooseColor(ActionEvent e) {
        Color color = new Color(Integer.parseInt(textFieldAccentColor.getText(), 16));
        color = JColorChooser.showDialog(context.mainFrame, context.cfg.gs("Settings.select.accent.color"), color);
        if (color != null)
        {
            textFieldAccentColor.setText(Utils.formatHex(color.getRed(), 2) +
                    Utils.formatHex(color.getGreen(), 2) +
                    Utils.formatHex(color.getBlue(), 2));
            setAccentColor();
        }
    }

    private void defaultAccentColor(ActionEvent e)
    {
        textFieldAccentColor.setText(context.preferences.DEFAULT_ACCENT_COLOR);
        setAccentColor();
    }

    private void updateLookAndFeel(ActionEvent ae)
    {
        try
        {
            UIManager.put("ScrollBar.showButtons", showArrowsCheckBox.isSelected()); // show scrollbar up/down buttons
            UIManager.put("Component.hideMnemonics", !showMnemonicsCheckBox.isSelected()); // show/hide mnemonic letters
            Class<? extends LookAndFeel> lafClass = UIManager.getLookAndFeel().getClass();
            FlatLaf.setup(lafClass.newInstance());
            FlatLaf.updateUI();
            context.mainFrame.panelToolbar.setBackground(context.mainFrame.menuToolbar.getBackground());
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            JOptionPane.showMessageDialog(context.mainFrame,
                    context.cfg.gs("Z.exception") + ex.getMessage(),
                    context.cfg.gs("Settings.this.title"), JOptionPane.ERROR_MESSAGE);

        }
    }

    private void refreshLookAndFeel(int index)
    {
        try
        {
            context.preferences.setLookAndFeel(index);
            context.preferences.initLookAndFeel(context.cfg.APPLICATION_NAME, false);

            for (Frame frame : Frame.getFrames())
            {
                updateLAFRecursively(frame);
            }

            context.mainFrame.panelToolbar.setBackground(context.mainFrame.menuToolbar.getBackground());
            context.mainFrame.setBrowserTabs(-1);
            context.browser.refreshAll();
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            JOptionPane.showMessageDialog(settingsDialogPane, context.cfg.gs("Settings.error.changing.look.n.feel") + e.getMessage(), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setAccentColor()
    {
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
            JOptionPane.showMessageDialog(context.mainFrame,
                    context.cfg.gs("Z.exception") + ex.getMessage(),
                    context.cfg.gs("Settings.this.title"), JOptionPane.ERROR_MESSAGE);

        }
    }

    private void setDialog()
    {
        // general
        preserveFileTimestampsCheckBox.setSelected(context.preferences.isPreserveFileTimes());
        showDeleteConfirmationCheckBox.setSelected(context.preferences.isShowDeleteConfirmation());
        showCcpConfirmationCheckBox.setSelected(context.preferences.isShowCcpConfirmation());
        showDndConfirmationCheckBox.setSelected(context.preferences.isShowDnDConfirmation());
        showTouchConfirmationCheckBox.setSelected(context.preferences.isShowTouchConfirmation());
        defaultDryrunCheckBox.setSelected(context.preferences.isDefaultDryrun());

        // appearance
        lookFeelComboBox.setAutoscrolls(true);
        lookFeelComboBox.setSelectedIndex(context.preferences.getLookAndFeel());
        // locale - add new locales to Preferences.availableLocales
        ComboBoxModel<String> model = localeComboBox.getModel();
        localeComboBox.setAutoscrolls(true);
        if (Configuration.availableLocales.length > 0)
        {
            localeComboBox.removeAllItems();
            for (String loc : Configuration.availableLocales)
            {
                localeComboBox.addItem(loc);
            }
        }
        for (int i = 0; i < model.getSize(); ++i)
        {
            String loc = model.getElementAt(i);
            if (loc.equals(context.preferences.getLocale()))
            {
                localeComboBox.setSelectedIndex(i);
                break;
            }
        }
        dateFormatTextField.setText(context.preferences.getDateFormat());
        if (context.preferences.getAccentColor() == null || context.preferences.getAccentColor().length() < 1)
        {
            context.preferences.setAccentColor(context.preferences.DEFAULT_ACCENT_COLOR);
        }
        textFieldAccentColor.setText(context.preferences.getAccentColor());
        scaleCheckBox.setSelected(context.preferences.isBinaryScale());
        showArrowsCheckBox.setSelected(context.preferences.isShowArrows());
        showMnemonicsCheckBox.setSelected(context.preferences.isShowMnemonics());

        // browser
        hideFilesInTreeCheckBox.setSelected(context.preferences.isHideFilesInTree());
        sortCaseSensitiveCheckBox.setSelected(context.preferences.isSortCaseInsensitive());
        sortFoldersBeforeFilesCheckBox.setSelected(context.preferences.isSortFoldersBeforeFiles());
        sortReverseCheckBox.setSelected(context.preferences.isSortReverse());
        tabPlacementComboBox.removeAllItems();
        model = tabPlacementComboBox.getModel();
        tabPlacementComboBox.addItem(context.cfg.gs("Settings.tabPlacement.top"));
        tabPlacementComboBox.addItem(context.cfg.gs("Settings.tabPlacement.bottom"));
        tabPlacementComboBox.addItem(context.cfg.gs("Settings.tabPlacement.left"));
        tabPlacementComboBox.addItem(context.cfg.gs("Settings.tabPlacement.right"));
        tabPlacementComboBox.setSelectedIndex(context.preferences.getTabPlacementIndex());
        uselastPubSubCheckBox.setSelected(context.preferences.isUseLastPublisherSubscriber());

        // operationsUI

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
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Settings.date.format.not.valid"), context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
            settingsTabbedPane.setSelectedIndex(1);
            dateFormatTextField.requestFocus();
            context.mainFrame.labelStatusMiddle.setText(context.cfg.gs("Settings.date.format.not.valid"));
            return false;
        }

        // general
        context.preferences.setPreserveFileTimes(preserveFileTimestampsCheckBox.isSelected());
        context.cfg.setPreserveDates(context.preferences.isPreserveFileTimes());
        context.preferences.setShowDeleteConfirmation(showDeleteConfirmationCheckBox.isSelected());
        context.preferences.setShowCcpConfirmation(showCcpConfirmationCheckBox.isSelected());
        context.preferences.setShowDnDConfirmation(showDndConfirmationCheckBox.isSelected());
        context.preferences.setShowTouchConfirmation(showTouchConfirmationCheckBox.isSelected());
        context.preferences.setDefaultDryrun(defaultDryrunCheckBox.isSelected());

        // appearance
        context.preferences.setLookAndFeel(lookFeelComboBox.getSelectedIndex());
        context.preferences.setLocale((String) localeComboBox.getSelectedItem());
        context.preferences.setDateFormat(dateFormatTextField.getText());
        if (textFieldAccentColor.getText().length() == 0) // use default if empty
        {
            context.preferences.setAccentColor(context.preferences.DEFAULT_ACCENT_COLOR);
            // set accent color for current LaF
            FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#" + context.preferences.getAccentColor()));
            Class<? extends LookAndFeel> lafClass = UIManager.getLookAndFeel().getClass();
            try
            {
                FlatLaf.setup(lafClass.newInstance());
            }
            catch (Exception e)
            {}
            FlatLaf.updateUI();
        }
        else
            context.preferences.setAccentColor(textFieldAccentColor.getText());
        context.cfg.setLongScale(context.preferences.isBinaryScale());
        context.preferences.setBinaryScale(scaleCheckBox.isSelected());
        context.preferences.setShowArrows(showArrowsCheckBox.isSelected());
        context.preferences.setShowMnemonics(showMnemonicsCheckBox.isSelected());

        // browser
        context.preferences.setHideFilesInTree(hideFilesInTreeCheckBox.isSelected());
        context.preferences.setSortCaseInsensitive(sortCaseSensitiveCheckBox.isSelected());
        context.preferences.setSortFoldersBeforeFiles(sortFoldersBeforeFilesCheckBox.isSelected());
        context.preferences.setSortReverse(sortReverseCheckBox.isSelected());
        context.preferences.setTabPlacement(tabPlacementComboBox.getSelectedIndex());
        context.preferences.setUseLastPublisherSubscriber(uselastPubSubCheckBox.isSelected());

        context.mainFrame.labelStatusMiddle.setText("");
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
        showDefaultDryrunLabel = new JLabel();
        defaultDryrunCheckBox = new JCheckBox();
        apperancePanel = new JPanel();
        lookFeelLabel = new JLabel();
        lookFeelComboBox = new JComboBox<>();
        localeLabel = new JLabel();
        localeComboBox = new JComboBox<>();
        dateFormatLabel = new JLabel();
        dateInfoButton = new JButton();
        dateFormatTextField = new JTextField();
        accentColorButtonLabel = new JLabel();
        defaultAccentButton = new JButton();
        textFieldAccentColor = new JTextField();
        buttonChooseColor = new JButton();
        scaleLabel = new JLabel();
        scaleCheckBox = new JCheckBox();
        showArrowseLabel = new JLabel();
        showArrowsCheckBox = new JCheckBox();
        showMnemonicsLabel = new JLabel();
        showMnemonicsCheckBox = new JCheckBox();
        browserPanel = new JPanel();
        hideFilesInTreeLabel = new JLabel();
        hideFilesInTreeCheckBox = new JCheckBox();
        sortCaseSensitiveLabel = new JLabel();
        sortCaseSensitiveCheckBox = new JCheckBox();
        sortFoldersBeforeFilesLabel = new JLabel();
        sortFoldersBeforeFilesCheckBox = new JCheckBox();
        sortReverseLabel = new JLabel();
        sortReverseCheckBox = new JCheckBox();
        tabPlacementlabel = new JLabel();
        tabPlacementComboBox = new JComboBox<>();
        useLastPubSubLabel = new JLabel();
        uselastPubSubCheckBox = new JCheckBox();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("Settings.this.title"));
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
        var contentPane = getContentPane();
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
                        generalPanel.setLayout(new GridBagLayout());
                        ((GridBagLayout)generalPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
                        ((GridBagLayout)generalPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
                        ((GridBagLayout)generalPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                        ((GridBagLayout)generalPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                        //---- preserveFileTimestampsLabel ----
                        preserveFileTimestampsLabel.setText(context.cfg.gs("Settings.preserveFileTimestampsLabel.text"));
                        generalPanel.add(preserveFileTimestampsLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(8, 8, 20, 45), 0, 0));
                        generalPanel.add(preserveFileTimestampsCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(8, 0, 20, 0), 0, 0));

                        //---- showDeleteConfirmationLabel ----
                        showDeleteConfirmationLabel.setText(context.cfg.gs("Settings.showDeleteConfirmationLabel.text"));
                        generalPanel.add(showDeleteConfirmationLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        generalPanel.add(showDeleteConfirmationCheckBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- showCcpConfirmationLabel ----
                        showCcpConfirmationLabel.setText(context.cfg.gs("Settings.showCcpConfirmationLabel.text"));
                        generalPanel.add(showCcpConfirmationLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        generalPanel.add(showCcpConfirmationCheckBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- showDndConfirmationLabel ----
                        showDndConfirmationLabel.setText(context.cfg.gs("Settings.showDndConfirmationLabel.text"));
                        generalPanel.add(showDndConfirmationLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        generalPanel.add(showDndConfirmationCheckBox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- showTouchConfirmationLabel ----
                        showTouchConfirmationLabel.setText(context.cfg.gs("Settings.showTouchConfirmationLabel.text"));
                        generalPanel.add(showTouchConfirmationLabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        generalPanel.add(showTouchConfirmationCheckBox, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- showDefaultDryrunLabel ----
                        showDefaultDryrunLabel.setText(context.cfg.gs("Settings.default.dry.runLabel.text"));
                        generalPanel.add(showDefaultDryrunLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        generalPanel.add(defaultDryrunCheckBox, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));
                    }
                    settingsTabbedPane.addTab(context.cfg.gs("Settings.generalPanel.tab.title"), generalPanel);
                    settingsTabbedPane.setMnemonicAt(0, context.cfg.gs("Settings.generalPanel.tab.mnemonic").charAt(0));

                    //======== apperancePanel ========
                    {
                        apperancePanel.setLayout(new GridBagLayout());
                        ((GridBagLayout)apperancePanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
                        ((GridBagLayout)apperancePanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
                        ((GridBagLayout)apperancePanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0E-4};
                        ((GridBagLayout)apperancePanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                        //---- lookFeelLabel ----
                        lookFeelLabel.setText(context.cfg.gs("Settings.lookFeelLabel.text"));
                        apperancePanel.add(lookFeelLabel, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 5), 0, 0));

                        //---- lookFeelComboBox ----
                        lookFeelComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "System (Windows)",
                            "Flat light",
                            "Flat dark",
                            "IntelliJ light",
                            "IntelliJ dark",
                            "macOS light",
                            "macOS dark"
                        }));
                        lookFeelComboBox.setName("lafCombo");
                        apperancePanel.add(lookFeelComboBox, new GridBagConstraints(2, 0, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 0), 0, 0));

                        //---- localeLabel ----
                        localeLabel.setText(context.cfg.gs("Settings.localeLabel.text"));
                        apperancePanel.add(localeLabel, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 5), 0, 0));

                        //---- localeComboBox ----
                        localeComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "en_US"
                        }));
                        localeComboBox.setName("localeCombo");
                        apperancePanel.add(localeComboBox, new GridBagConstraints(2, 1, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 0), 0, 0));

                        //---- dateFormatLabel ----
                        dateFormatLabel.setText(context.cfg.gs("Settings.dateFormatLabel.text"));
                        apperancePanel.add(dateFormatLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 35), 0, 0));

                        //---- dateInfoButton ----
                        dateInfoButton.setText(context.cfg.gs("Settings.button.dateInfo.text"));
                        dateInfoButton.setToolTipText(context.cfg.gs("Settings.button.dateInfo.text.tooltip"));
                        apperancePanel.add(dateInfoButton, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 5), 0, 0));

                        //---- dateFormatTextField ----
                        dateFormatTextField.setText("yyyy-MM-dd hh:mm:ss aa");
                        apperancePanel.add(dateFormatTextField, new GridBagConstraints(2, 2, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 0), 0, 0));

                        //---- accentColorButtonLabel ----
                        accentColorButtonLabel.setText(context.cfg.gs("Settings.accentColorLabel.text"));
                        apperancePanel.add(accentColorButtonLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 33), 0, 0));

                        //---- defaultAccentButton ----
                        defaultAccentButton.setText(context.cfg.gs("Settings.defaultAccentButton.text"));
                        defaultAccentButton.setToolTipText(context.cfg.gs("Settings.defaultAccentButton.toolTipText"));
                        defaultAccentButton.addActionListener(e -> defaultAccentColor(e));
                        apperancePanel.add(defaultAccentButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 5), 0, 0));

                        //---- textFieldAccentColor ----
                        textFieldAccentColor.setToolTipText(context.cfg.gs("Settings.textField.HintButtonColor.toolTipText"));
                        apperancePanel.add(textFieldAccentColor, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 5), 0, 0));

                        //---- buttonChooseColor ----
                        buttonChooseColor.setText(context.cfg.gs("Settings.button.ChooseColor.text"));
                        buttonChooseColor.setToolTipText(context.cfg.gs("Settings.button.ChooseColor.toolTipText"));
                        buttonChooseColor.addActionListener(e -> chooseColor(e));
                        apperancePanel.add(buttonChooseColor, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 0), 0, 0));

                        //---- scaleLabel ----
                        scaleLabel.setText(context.cfg.gs("Settings.scaleLabel.text"));
                        apperancePanel.add(scaleLabel, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 5), 0, 0));

                        //---- scaleCheckBox ----
                        scaleCheckBox.setToolTipText(context.cfg.gs("Settings.scaleCheckBox.toolTipText"));
                        apperancePanel.add(scaleCheckBox, new GridBagConstraints(2, 4, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 0), 0, 0));

                        //---- showArrowseLabel ----
                        showArrowseLabel.setText(context.cfg.gs("Settings.showArrowseLabel.text"));
                        apperancePanel.add(showArrowseLabel, new GridBagConstraints(0, 5, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 14, 5), 0, 0));

                        //---- showArrowsCheckBox ----
                        showArrowsCheckBox.addActionListener(e -> updateLookAndFeel(e));
                        apperancePanel.add(showArrowsCheckBox, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 14, 5), 0, 0));

                        //---- showMnemonicsLabel ----
                        showMnemonicsLabel.setText(context.cfg.gs("Settings.showMnemonicsLabel.text"));
                        apperancePanel.add(showMnemonicsLabel, new GridBagConstraints(0, 6, 2, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 0, 5), 0, 0));

                        //---- showMnemonicsCheckBox ----
                        showMnemonicsCheckBox.addActionListener(e -> updateLookAndFeel(e));
                        apperancePanel.add(showMnemonicsCheckBox, new GridBagConstraints(2, 6, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 0, 5), 0, 0));
                    }
                    settingsTabbedPane.addTab(context.cfg.gs("Settings.appearance.tab.title"), apperancePanel);
                    settingsTabbedPane.setMnemonicAt(1, context.cfg.gs("Settings.appearancePanel.tab.mnemonic").charAt(0));

                    //======== browserPanel ========
                    {
                        browserPanel.setLayout(new GridBagLayout());
                        ((GridBagLayout)browserPanel.getLayout()).columnWidths = new int[] {0, 0, 0};
                        ((GridBagLayout)browserPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                        ((GridBagLayout)browserPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                        ((GridBagLayout)browserPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                        //---- hideFilesInTreeLabel ----
                        hideFilesInTreeLabel.setText(context.cfg.gs("Settings.hideFilesInTreeLabel.text"));
                        browserPanel.add(hideFilesInTreeLabel, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(8, 8, 20, 5), 0, 0));
                        browserPanel.add(hideFilesInTreeCheckBox, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(8, 0, 20, 0), 0, 0));

                        //---- sortCaseSensitiveLabel ----
                        sortCaseSensitiveLabel.setText(context.cfg.gs("Settings.sortCaseSensitiveLabel.text"));
                        browserPanel.add(sortCaseSensitiveLabel, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        browserPanel.add(sortCaseSensitiveCheckBox, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- sortFoldersBeforeFilesLabel ----
                        sortFoldersBeforeFilesLabel.setText(context.cfg.gs("Settings.sortFoldersBeforeFilesLabel.text"));
                        browserPanel.add(sortFoldersBeforeFilesLabel, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 49), 0, 0));
                        browserPanel.add(sortFoldersBeforeFilesCheckBox, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- sortReverseLabel ----
                        sortReverseLabel.setText(context.cfg.gs("Settings.sortReverseLabel.text"));
                        browserPanel.add(sortReverseLabel, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));
                        browserPanel.add(sortReverseCheckBox, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- tabPlacementlabel ----
                        tabPlacementlabel.setText(context.cfg.gs("Settings.tabPlacementLabel.text"));
                        browserPanel.add(tabPlacementlabel, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 5), 0, 0));

                        //---- tabPlacementComboBox ----
                        tabPlacementComboBox.setModel(new DefaultComboBoxModel<>(new String[] {
                            "Top",
                            "Bottom",
                            "Left",
                            "Right"
                        }));
                        tabPlacementComboBox.setPreferredSize(new Dimension(100, 30));
                        tabPlacementComboBox.setName("tabPlacementCombo");
                        browserPanel.add(tabPlacementComboBox, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));

                        //---- useLastPubSubLabel ----
                        useLastPubSubLabel.setText(context.cfg.gs("Settings.useLastPubSubLabel.text"));
                        browserPanel.add(useLastPubSubLabel, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 8, 20, 19), 0, 0));

                        //---- uselastPubSubCheckBox ----
                        uselastPubSubCheckBox.setToolTipText(context.cfg.gs("Settings.uselastPubSubCheckBox.toolTipText"));
                        browserPanel.add(uselastPubSubCheckBox, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                            GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                            new Insets(0, 0, 20, 0), 0, 0));
                    }
                    settingsTabbedPane.addTab(context.cfg.gs("Settings.browserPanel.tab.title"), browserPanel);
                    settingsTabbedPane.setMnemonicAt(2, context.cfg.gs("Settings.browserPanel.tab.mnemonic").charAt(0));
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
                okButton.setText(context.cfg.gs("Z.save"));
                okButton.setToolTipText(context.cfg.gs("Z.save.toolTip.text"));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(context.cfg.gs("Z.cancel"));
                cancelButton.setToolTipText(context.cfg.gs("Z.cancel.changes.toolTipText"));
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
    private JLabel showDefaultDryrunLabel;
    private JCheckBox defaultDryrunCheckBox;
    private JPanel apperancePanel;
    private JLabel lookFeelLabel;
    private JComboBox<String> lookFeelComboBox;
    private JLabel localeLabel;
    private JComboBox<String> localeComboBox;
    private JLabel dateFormatLabel;
    private JButton dateInfoButton;
    private JTextField dateFormatTextField;
    private JLabel accentColorButtonLabel;
    private JButton defaultAccentButton;
    private JTextField textFieldAccentColor;
    private JButton buttonChooseColor;
    private JLabel scaleLabel;
    private JCheckBox scaleCheckBox;
    private JLabel showArrowseLabel;
    private JCheckBox showArrowsCheckBox;
    private JLabel showMnemonicsLabel;
    private JCheckBox showMnemonicsCheckBox;
    private JPanel browserPanel;
    private JLabel hideFilesInTreeLabel;
    private JCheckBox hideFilesInTreeCheckBox;
    private JLabel sortCaseSensitiveLabel;
    private JCheckBox sortCaseSensitiveCheckBox;
    private JLabel sortFoldersBeforeFilesLabel;
    private JCheckBox sortFoldersBeforeFilesCheckBox;
    private JLabel sortReverseLabel;
    private JCheckBox sortReverseCheckBox;
    private JLabel tabPlacementlabel;
    private JComboBox<String> tabPlacementComboBox;
    private JLabel useLastPubSubLabel;
    private JCheckBox uselastPubSubCheckBox;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>
}
