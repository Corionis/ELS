package com.corionis.els.gui.util;

import com.corionis.els.Context;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.awt.*;

public class DirectoryPicker
{
    public JButton browserSelectionButton;
    public JDialog dialog;
    public JTextField locationPathTextField;
    public JLabel minLabel;
    public JTextField minSize;
    public JOptionPane pane;
    public JPanel panel;
    public JLabel prompt;
    private NumberFilter numberFilter;
    public JComboBox scales;
    public JButton selectLocationButton;

    /**
     * DirectoryPicker constructor
     * <p><br/>
     * Allows using either a Browser selection or the
     * standard file picker to select a "real" physical path.
     * Local and remote subscriber paths are supported.
     * <p><br/>
     * <b>Note:</b> Remote Browser paths are relative to
     * that computer only.
     * <p><br/>
     * Implement required methods:<br/>
     *  * browserSelectionButton.addActionListener<br/>
     *  * selectLocationButton.addActionListener<br/>
     *  * pane.addPropertyChangeListener<br/>
     *  <p><br/>
     *  See LibrariesUI.actionSourcesAddClicked(),
     *  LibrariesUI.actionLocationAdd() and related methods
     *  for implementation examples.
     *
     * @param context The Context
     * @param displayName The title
     * @param message The prompt
     * @param includeSize Include minimum-size line, for Locations
     */
    public DirectoryPicker(Context context, String displayName, String message, boolean includeSize)
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
        locationPathTextField = new JTextField();
        locationPathTextField.setEditable(true);
        locationPathTextField.setMinimumSize(new Dimension(360, 30));
        locationPathTextField.setPreferredSize(new Dimension(360, 30));
        panel.add(locationPathTextField, new GridBagConstraints(0, 1, 6, 1, 1.0, 0.0,
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

        // select location button
        selectLocationButton = new JButton("...");
        selectLocationButton.setName("selectLocation");
        selectLocationButton.setActionCommand("selectLocation");
        selectLocationButton.setText("...");
        selectLocationButton.setFont(selectLocationButton.getFont().deriveFont(selectLocationButton.getFont().getStyle() | Font.BOLD));
        selectLocationButton.setMaximumSize(new Dimension(32, 24));
        selectLocationButton.setMinimumSize(new Dimension(32, 24));
        selectLocationButton.setPreferredSize(new Dimension(32, 24));
        selectLocationButton.setVerticalTextPosition(SwingConstants.TOP);
        selectLocationButton.setIconTextGap(0);
        selectLocationButton.setHorizontalTextPosition(SwingConstants.LEADING);
        selectLocationButton.setToolTipText(context.cfg.gs("Libraries.select.location"));

        panel.add(selectLocationButton, new GridBagConstraints(7, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.BOTH,
                new Insets(0, 0, 4, 4), 0, 0));

        if (includeSize)
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

        pane = new JOptionPane(panel);
        pane.setOptionType(JOptionPane.OK_CANCEL_OPTION);

        dialog = pane.createDialog(context.mainFrame, displayName);
        dialog.setModal(false);
    }

    private void setNumberFilter(JTextField field)
    {
        PlainDocument pd = (PlainDocument) field.getDocument();
        pd.setDocumentFilter(numberFilter);
    }


}
