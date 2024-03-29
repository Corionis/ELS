package com.corionis.els.gui.util;

import com.corionis.els.Context;
import com.corionis.els.Utils;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

public class DirectoryPicker
{
    public JCheckBox allCheckbox;
    public JButton browserSelectionButton;
    public JDialog dialog;
    public JTextField directoryPathTextField;
    public JLabel minLabel;
    public JTextField minSize;
    public JOptionPane pane;
    public JPanel panel;
    public JLabel prompt;
    private NumberFilter numberFilter;
    public JComboBox scales;
    public JTable table;

    /**
     * DirectoryPicker constructor
     * <p><br/>
     * Works in 3 modes depending on includeSize and includeLibraries.
     * <p><br/>
     * Allows using either a Browser selection or the
     * standard file picker to select a "real" physical path.
     * Local and remote subscriber paths are supported.
     * <p><br/>
     * <b>Note:</b> Remote Browser paths are relative to
     * that computer.
     * <p><br/>
     * Implement required methods:<br/>
     *  * browserSelectionButton.addActionListener<br/>
     *  * selectLocationButton.addActionListener<br/>
     *  * pane.addPropertyChangeListener<br/>
     *  <p><br/>
     *  See:
     *  <ul>
     *      <li>LibrariesUI.actionLocationAddClicked()</li>
     *      <li>LibrariesUI.actionSourcesAddClicked()</li>
     *      <li>LibrariesUI.actionSourcesMultiClicked()</li>
     *      <li>LibrariesUI.actionSelectTempLocationClicked</li>
     *  </ul>
     *
     * @param context The Context
     * @param displayName The title
     * @param message The prompt
     * @param includeSize Include minimum-size line, for Locations
     * @param includeLibraries Include the Multiple-Add libraries list and All checkbox
     */
    public DirectoryPicker(Context context, String displayName, String message, boolean includeSize, boolean includeLibraries)
    {
        numberFilter = new NumberFilter();

        // controls panel
        panel = new JPanel();
        GridBagLayout gridBagLayout = new GridBagLayout();
        panel.setLayout(gridBagLayout);

        // prompt
        prompt = new JLabel(message);
        panel.add(prompt, new GridBagConstraints(0, 0, 7, 1, 1.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.BOTH,
                new Insets(4, 0, 4, 4), 0, 0));

        // location path
        directoryPathTextField = new JTextField();
        directoryPathTextField.setEditable(false);
        directoryPathTextField.setMinimumSize(new Dimension(360, 30));
        directoryPathTextField.setPreferredSize(new Dimension(360, 30));
        panel.add(directoryPathTextField, new GridBagConstraints(0, 1, 6, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 4, 4, 4), 0, 0));

        // Browser selection button
        browserSelectionButton = new JButton(context.cfg.gs("Libraries.browser.button.title"));
        // let the size of this button float based on locale
        browserSelectionButton.setIconTextGap(0);
        browserSelectionButton.setHorizontalTextPosition(SwingConstants.LEADING);
        browserSelectionButton.setToolTipText(context.cfg.gs("Libraries.use.browser.selection"));

        panel.add(browserSelectionButton, new GridBagConstraints(6, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.BOTH,
                new Insets(0, 0, 4, 4), 0, 0));

        if (includeSize) // for locations
        {
            // minimum size label
            minLabel = new JLabel(context.cfg.gs("Libraries.minimum.free.space.for.location"));
            panel.add(minLabel, new GridBagConstraints(1, 2, 2, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    new Insets(0, 12, 4, 4), 0, 0));

            // minimum size field
            minSize = new JTextField();
            minSize.setText(context.preferences.getLibrariesDefaultMinimum());
            setNumberFilter(minSize);
            panel.add(minSize, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
                    new Insets(0, 0, 4, 4), 0, 0));

            // minimum size scale
            scales = new JComboBox<>();
            scales.addItem("KB");
            scales.addItem("MB");
            scales.addItem("GB");
            scales.addItem("TB");
            int defScale = 0;
            switch (context.preferences.getLibrariesDefaultMinimumScale())
            {
                case "KB":
                    defScale = 0;
                    break;
                case "MB":
                    defScale = 1;
                    break;
                case "GB":
                    defScale = 2;
                    break;
                case "TB":
                    defScale = 3;
                    break;
            }
            scales.setSelectedIndex(defScale);
            panel.add(scales, new GridBagConstraints(4, 2, 1, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));
        }

        if (includeLibraries) // for multi-add sources
        {
            // horizontal line
            JSeparator separator = new JSeparator();
            panel.add(separator, new GridBagConstraints(0, 3, 8, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(2, 0, 4, 0), 0, 0));

            // source to libraries prompt
            JLabel sourcesToLibrariesLabel = new JLabel(context.cfg.gs("Libraries.select.new.multiple.source.to.libraries"));
            panel.add(sourcesToLibrariesLabel, new GridBagConstraints(0, 4, 8, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

            // create libraries table & all checkbox
            table = new JTable();
            table.setCellSelectionEnabled(false);
            table.setRowSelectionAllowed(false);
            table.setColumnSelectionAllowed(false);
            table.setFillsViewportHeight(true);

            JScrollPane scrollPane = new JScrollPane();
            scrollPane.setMinimumSize(new Dimension(100, 100));
            scrollPane.setPreferredSize(new Dimension(240, 260));

            scrollPane.setViewportView(table);
            panel.add(scrollPane, new GridBagConstraints(0, 5, 8, 4, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL,
                    new Insets(0, 4, 4, 4), 0, 0));

            // "all" checkbox
            allCheckbox = new JCheckBox(context.cfg.gs("Libraries.toggle.all"));
            panel.add(allCheckbox, new GridBagConstraints(0, 9, 7, 1, 0.0, 0.0,
                    GridBagConstraints.WEST, GridBagConstraints.BOTH,
                    new Insets(0, 15, 0, 4), 0, 0));
        }

        pane = new JOptionPane(panel);
        pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);

        dialog = pane.createDialog(context.mainFrame, displayName);
        dialog.setModal(false);

        if (context.preferences.getDirectoryPickerXpos() != -1 && Utils.isOnScreen(context.preferences.getDirectoryPickerXpos(), context.preferences.getDirectoryPickerYpos()))
        {
            dialog.setLocation(context.preferences.getDirectoryPickerXpos(), context.preferences.getDirectoryPickerYpos());
        }
        else
        {
            dialog.setLocation(Utils.getRelativePosition(context.mainFrame, dialog));
        }

        dialog.addWindowListener(new WindowListener()
        {
            @Override
            public void windowOpened(WindowEvent windowEvent)
            {
            }

            @Override
            public void windowClosing(WindowEvent windowEvent)
            {
            }

            @Override
            public void windowClosed(WindowEvent windowEvent)
            {
                context.preferences.setDirectoryPickerXpos(dialog.getX());
                context.preferences.setDirectoryPickerYpos(dialog.getY());
            }

            @Override
            public void windowIconified(WindowEvent windowEvent)
            {
            }

            @Override
            public void windowDeiconified(WindowEvent windowEvent)
            {
            }

            @Override
            public void windowActivated(WindowEvent windowEvent)
            {
            }

            @Override
            public void windowDeactivated(WindowEvent windowEvent)
            {
            }
        });
    }

    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
    }

}
