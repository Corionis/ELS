package com.groksoft.els.gui.tools;

import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.tools.junkremover.JunkRemoverTool;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

public class ToolUI_CopyMe extends JDialog {
    GuiContext guiContext;
    JunkRemoverTool tool;

    public ToolUI_CopyMe(Window owner) {
        super(owner);
        this.guiContext = guiContext;
        initComponents();
    }

    private void doneClicked(ActionEvent e) {
        setVisible(false);
    }

    private void cancelClicked(ActionEvent e) {
        setVisible(false);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        textFieldName = new JTextField();
        separatorSections = new JSeparator();
        splitPaneContent = new JSplitPane();
        scrollPaneList = new JScrollPane();
        listItems = new JList<>();
        panelOptions = new JPanel();
        scrollPaneOptions = new JScrollPane();
        tableOptions = new JTable();
        panelOptionsButtons = new JPanel();
        buttonDeleteRow = new JButton();
        buttonBar = new JPanel();
        doneButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("ToolUI.this.title"));
        setName("junkRemoverUI");
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setPreferredSize(new Dimension(640, 500));
                contentPanel.setLayout(new BorderLayout());

                //======== panelTop ========
                {
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("ToolUI.buttonNew.text"));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("ToolUI.buttonCopy.text"));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("ToolUI.buttonDelete.text"));
                        panelTopButtons.add(buttonDelete);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.LINE_START);

                    //---- textFieldName ----
                    textFieldName.setPreferredSize(new Dimension(280, 30));
                    textFieldName.setHorizontalAlignment(SwingConstants.RIGHT);
                    panelTop.add(textFieldName, BorderLayout.CENTER);

                    //---- separatorSections ----
                    separatorSections.setPreferredSize(new Dimension(0, 3));
                    separatorSections.setMinimumSize(new Dimension(0, 3));
                    panelTop.add(separatorSections, BorderLayout.SOUTH);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);

                    //======== scrollPaneList ========
                    {

                        //---- listItems ----
                        listItems.setPreferredSize(new Dimension(128, 54));
                        listItems.setModel(new AbstractListModel<String>() {
                            String[] values = {
                                "Tool config 1",
                                "Tool config 2",
                                "Tool config 3"
                            };
                            @Override
                            public int getSize() { return values.length; }
                            @Override
                            public String getElementAt(int i) { return values[i]; }
                        });
                        scrollPaneList.setViewportView(listItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneList);

                    //======== panelOptions ========
                    {
                        panelOptions.setLayout(new BorderLayout());

                        //======== scrollPaneOptions ========
                        {

                            //---- tableOptions ----
                            tableOptions.setShowVerticalLines(false);
                            scrollPaneOptions.setViewportView(tableOptions);
                        }
                        panelOptions.add(scrollPaneOptions, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new GridBagLayout());
                            ((GridBagLayout)panelOptionsButtons.getLayout()).columnWidths = new int[] {0, 0};
                            ((GridBagLayout)panelOptionsButtons.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};

                            //---- buttonDeleteRow ----
                            buttonDeleteRow.setText(guiContext.cfg.gs("ToolUI.buttonDeleteRow.text"));
                            buttonDeleteRow.setFont(buttonDeleteRow.getFont().deriveFont(buttonDeleteRow.getFont().getSize() - 2f));
                            buttonDeleteRow.setPreferredSize(new Dimension(68, 24));
                            buttonDeleteRow.setMinimumSize(new Dimension(68, 24));
                            buttonDeleteRow.setMaximumSize(new Dimension(68, 24));
                            panelOptionsButtons.add(buttonDeleteRow, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                new Insets(0, 0, 0, 0), 0, 0));
                        }
                        panelOptions.add(panelOptionsButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelOptions);
                }
                contentPanel.add(splitPaneContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- doneButton ----
                doneButton.setText(guiContext.cfg.gs("ToolUI.doneButton.text"));
                doneButton.addActionListener(e -> doneClicked(e));
                buttonBar.add(doneButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("ToolUI.cancelButton.text"));
                cancelButton.addActionListener(e -> cancelClicked(e));
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
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
    private JPanel panelTop;
    private JPanel panelTopButtons;
    private JButton buttonNew;
    private JButton buttonCopy;
    private JButton buttonDelete;
    private JTextField textFieldName;
    private JSeparator separatorSections;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneList;
    private JList<String> listItems;
    private JPanel panelOptions;
    private JScrollPane scrollPaneOptions;
    private JTable tableOptions;
    private JPanel panelOptionsButtons;
    private JButton buttonDeleteRow;
    private JPanel buttonBar;
    private JButton doneButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
