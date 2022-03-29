package com.groksoft.els.gui.jobs;

import java.awt.event.*;
import javax.swing.event.*;
import com.groksoft.els.gui.GuiContext;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class JobsUI extends JDialog
{
    private GuiContext guiContext;

    public JobsUI(Window owner, GuiContext guiContext)
    {
        super(owner);
        this.guiContext = guiContext;

        initComponents();
    }

    private void cancelClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void copyClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void deleteClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void helpClicked(MouseEvent e)
    {
        // TODO add your code here
    }

    private void newClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void okClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void runClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void listItemsMouseClicked(MouseEvent e) {
        // TODO add your code here
    }

    private void listItemsValueChanged(ListSelectionEvent e) {
        // TODO add your code here
    }

    private void textFieldNameChanged(ActionEvent e) {
        // TODO add your code here
    }

    private void textFieldNameFocusLost(FocusEvent e) {
        // TODO add your code here
    }

    private void addRowClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void removeRowClicked(ActionEvent e) {
        // TODO add your code here
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelTop = new JPanel();
        panelTopButtons = new JPanel();
        buttonNew = new JButton();
        buttonCopy = new JButton();
        buttonDelete = new JButton();
        hSpacerBeforeRun = new JPanel(null);
        buttonRun = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneList = new JScrollPane();
        listItems = new JList<>();
        panel1 = new JPanel();
        panel3 = new JPanel();
        textFieldName = new JTextField();
        comboBox1 = new JComboBox<>();
        splitPane1 = new JSplitPane();
        scrollPane1 = new JScrollPane();
        list1 = new JList<>();
        panel2 = new JPanel();
        panelOptionsButtons2 = new JPanel();
        buttonAddRow2 = new JButton();
        buttonRemoveRow2 = new JButton();
        scrollPane2 = new JScrollPane();
        list2 = new JList<>();
        panelOptionsButtons = new JPanel();
        button1 = new JButton();
        button2 = new JButton();
        buttonAddRow = new JButton();
        buttonRemoveRow = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(guiContext.cfg.gs("JobsUI.title"));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout(4, 4));

                //======== panelTop ========
                {
                    panelTop.setMinimumSize(new Dimension(140, 38));
                    panelTop.setPreferredSize(new Dimension(570, 38));
                    panelTop.setLayout(new BorderLayout());

                    //======== panelTopButtons ========
                    {
                        panelTopButtons.setMinimumSize(new Dimension(140, 38));
                        panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                        //---- buttonNew ----
                        buttonNew.setText(guiContext.cfg.gs("JobsUI.buttonNew.text"));
                        buttonNew.setMnemonic(guiContext.cfg.gs("JobsUI.buttonNew.mnemonic").charAt(0));
                        buttonNew.setToolTipText(guiContext.cfg.gs("JobsUI.buttonNew.toolTipText"));
                        buttonNew.addActionListener(e -> newClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JobsUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("JobsUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("JobsUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> copyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JobsUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("JobsUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("JobsUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> deleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("JobsUI.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> runClicked(e));
                        panelTopButtons.add(buttonRun);
                    }
                    panelTop.add(panelTopButtons, BorderLayout.WEST);

                    //======== panelHelp ========
                    {
                        panelHelp.setPreferredSize(new Dimension(40, 38));
                        panelHelp.setMinimumSize(new Dimension(0, 38));
                        panelHelp.setLayout(new FlowLayout(FlowLayout.RIGHT, 4, 4));

                        //---- labelHelp ----
                        labelHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                        labelHelp.setPreferredSize(new Dimension(32, 30));
                        labelHelp.setMinimumSize(new Dimension(32, 30));
                        labelHelp.setMaximumSize(new Dimension(32, 30));
                        labelHelp.setToolTipText(guiContext.cfg.gs("JobsUI.labelHelp.toolTipText"));
                        labelHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        labelHelp.setIconTextGap(0);
                        labelHelp.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                helpClicked(e);
                            }
                        });
                        panelHelp.add(labelHelp);
                    }
                    panelTop.add(panelHelp, BorderLayout.EAST);
                }
                contentPanel.add(panelTop, BorderLayout.NORTH);

                //======== splitPaneContent ========
                {
                    splitPaneContent.setDividerLocation(142);
                    splitPaneContent.setLastDividerLocation(142);
                    splitPaneContent.setMinimumSize(new Dimension(140, 80));

                    //======== scrollPaneList ========
                    {
                        scrollPaneList.setMinimumSize(new Dimension(140, 16));
                        scrollPaneList.setPreferredSize(new Dimension(142, 146));

                        //---- listItems ----
                        listItems.setPreferredSize(new Dimension(128, 54));
                        listItems.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        listItems.setModel(new AbstractListModel<String>() {
                            String[] values = {
                                "Config 1",
                                "Config 2"
                            };
                            @Override
                            public int getSize() { return values.length; }
                            @Override
                            public String getElementAt(int i) { return values[i]; }
                        });
                        listItems.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                listItemsMouseClicked(e);
                            }
                        });
                        listItems.addListSelectionListener(e -> listItemsValueChanged(e));
                        scrollPaneList.setViewportView(listItems);
                    }
                    splitPaneContent.setLeftComponent(scrollPaneList);

                    //======== panel1 ========
                    {
                        panel1.setLayout(new BorderLayout());

                        //======== panel3 ========
                        {
                            panel3.setLayout(new BorderLayout());

                            //---- textFieldName ----
                            textFieldName.setPreferredSize(new Dimension(150, 30));
                            textFieldName.setText("Config 1");
                            textFieldName.addActionListener(e -> textFieldNameChanged(e));
                            textFieldName.addFocusListener(new FocusAdapter() {
                                @Override
                                public void focusLost(FocusEvent e) {
                                    textFieldNameFocusLost(e);
                                }
                            });
                            panel3.add(textFieldName, BorderLayout.CENTER);

                            //---- comboBox1 ----
                            comboBox1.setPrototypeDisplayValue(guiContext.cfg.gs("JobsUI.comboBox1.prototypeDisplayValue"));
                            comboBox1.setModel(new DefaultComboBoxModel<>(new String[] {
                                "Publisher",
                                "Subscriber"
                            }));
                            panel3.add(comboBox1, BorderLayout.EAST);
                        }
                        panel1.add(panel3, BorderLayout.NORTH);

                        //======== splitPane1 ========
                        {
                            splitPane1.setDividerLocation(142);
                            splitPane1.setLastDividerLocation(142);

                            //======== scrollPane1 ========
                            {

                                //---- list1 ----
                                list1.setModel(new AbstractListModel<String>() {
                                    String[] values = {
                                        "Basic web junk",
                                        "Windows junk"
                                    };
                                    @Override
                                    public int getSize() { return values.length; }
                                    @Override
                                    public String getElementAt(int i) { return values[i]; }
                                });
                                scrollPane1.setViewportView(list1);
                            }
                            splitPane1.setLeftComponent(scrollPane1);

                            //======== panel2 ========
                            {
                                panel2.setBorder(null);
                                panel2.setLayout(new BorderLayout());

                                //======== panelOptionsButtons2 ========
                                {
                                    panelOptionsButtons2.setBorder(null);
                                    panelOptionsButtons2.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                    //---- buttonAddRow2 ----
                                    buttonAddRow2.setText(guiContext.cfg.gs("JobsUI.buttonAddRow2.text"));
                                    buttonAddRow2.setFont(buttonAddRow2.getFont().deriveFont(buttonAddRow2.getFont().getSize() - 2f));
                                    buttonAddRow2.setPreferredSize(new Dimension(78, 24));
                                    buttonAddRow2.setMinimumSize(new Dimension(78, 24));
                                    buttonAddRow2.setMaximumSize(new Dimension(78, 24));
                                    buttonAddRow2.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddRow2.mnemonic").charAt(0));
                                    buttonAddRow2.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddRow2.toolTipText"));
                                    buttonAddRow2.addActionListener(e -> addRowClicked(e));
                                    panelOptionsButtons2.add(buttonAddRow2);

                                    //---- buttonRemoveRow2 ----
                                    buttonRemoveRow2.setText(guiContext.cfg.gs("JobsUI.buttonRemoveRow2.text"));
                                    buttonRemoveRow2.setFont(buttonRemoveRow2.getFont().deriveFont(buttonRemoveRow2.getFont().getSize() - 2f));
                                    buttonRemoveRow2.setPreferredSize(new Dimension(78, 24));
                                    buttonRemoveRow2.setMinimumSize(new Dimension(78, 24));
                                    buttonRemoveRow2.setMaximumSize(new Dimension(78, 24));
                                    buttonRemoveRow2.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveRow2.mnemonic").charAt(0));
                                    buttonRemoveRow2.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveRow2.toolTipText"));
                                    buttonRemoveRow2.addActionListener(e -> removeRowClicked(e));
                                    panelOptionsButtons2.add(buttonRemoveRow2);
                                }
                                panel2.add(panelOptionsButtons2, BorderLayout.SOUTH);

                                //======== scrollPane2 ========
                                {

                                    //---- list2 ----
                                    list2.setModel(new AbstractListModel<String>() {
                                        String[] values = {
                                            "Movies",
                                            "TV Shows",
                                            "/home/plex/Plex/pre-publish"
                                        };
                                        @Override
                                        public int getSize() { return values.length; }
                                        @Override
                                        public String getElementAt(int i) { return values[i]; }
                                    });
                                    scrollPane2.setViewportView(list2);
                                }
                                panel2.add(scrollPane2, BorderLayout.CENTER);
                            }
                            splitPane1.setRightComponent(panel2);
                        }
                        panel1.add(splitPane1, BorderLayout.CENTER);

                        //======== panelOptionsButtons ========
                        {
                            panelOptionsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- button1 ----
                            button1.setText(guiContext.cfg.gs("JobsUI.button1.text"));
                            button1.setMaximumSize(new Dimension(24, 24));
                            button1.setMinimumSize(new Dimension(24, 24));
                            button1.setPreferredSize(new Dimension(24, 24));
                            button1.setFont(button1.getFont().deriveFont(button1.getFont().getSize() - 2f));
                            panelOptionsButtons.add(button1);

                            //---- button2 ----
                            button2.setText("v");
                            button2.setFont(button2.getFont().deriveFont(button2.getFont().getSize() - 2f));
                            button2.setMaximumSize(new Dimension(24, 24));
                            button2.setMinimumSize(new Dimension(24, 24));
                            button2.setPreferredSize(new Dimension(24, 24));
                            panelOptionsButtons.add(button2);

                            //---- buttonAddRow ----
                            buttonAddRow.setText(guiContext.cfg.gs("JobsUI.buttonAddRow.text"));
                            buttonAddRow.setFont(buttonAddRow.getFont().deriveFont(buttonAddRow.getFont().getSize() - 2f));
                            buttonAddRow.setPreferredSize(new Dimension(78, 24));
                            buttonAddRow.setMinimumSize(new Dimension(78, 24));
                            buttonAddRow.setMaximumSize(new Dimension(78, 24));
                            buttonAddRow.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddRow.mnemonic").charAt(0));
                            buttonAddRow.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddRow.toolTipText"));
                            buttonAddRow.addActionListener(e -> addRowClicked(e));
                            panelOptionsButtons.add(buttonAddRow);

                            //---- buttonRemoveRow ----
                            buttonRemoveRow.setText(guiContext.cfg.gs("JobsUI.buttonRemoveRow.text"));
                            buttonRemoveRow.setFont(buttonRemoveRow.getFont().deriveFont(buttonRemoveRow.getFont().getSize() - 2f));
                            buttonRemoveRow.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveRow.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveRow.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveRow.mnemonic").charAt(0));
                            buttonRemoveRow.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveRow.toolTipText"));
                            buttonRemoveRow.addActionListener(e -> removeRowClicked(e));
                            panelOptionsButtons.add(buttonRemoveRow);
                        }
                        panel1.add(panelOptionsButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panel1);
                }
                contentPanel.add(splitPaneContent, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 82, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(guiContext.cfg.gs("JobsUI.okButton.text"));
                okButton.addActionListener(e -> okClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("JobsUI.cancelButton.text"));
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
    private JPanel hSpacerBeforeRun;
    private JButton buttonRun;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneList;
    private JList<String> listItems;
    private JPanel panel1;
    private JPanel panel3;
    private JTextField textFieldName;
    private JComboBox<String> comboBox1;
    private JSplitPane splitPane1;
    private JScrollPane scrollPane1;
    private JList<String> list1;
    private JPanel panel2;
    private JPanel panelOptionsButtons2;
    private JButton buttonAddRow2;
    private JButton buttonRemoveRow2;
    private JScrollPane scrollPane2;
    private JList<String> list2;
    private JPanel panelOptionsButtons;
    private JButton button1;
    private JButton button2;
    private JButton buttonAddRow;
    private JButton buttonRemoveRow;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
