/*
 * Created by JFormDesigner on Tue Jan 25 15:26:24 MST 2022
 */

package com.groksoft.els.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * @author unknown
 */
public class Settings extends JDialog {
    public Settings(Window owner) {
        super(owner);
        initComponents();

        okButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
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
                    okButton.doClick();
                }
            }
        });

        cancelButton.addActionListener(new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                setVisible(false);
            }
        });

        getRootPane().setDefaultButton(okButton);



    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        tabbedPane1 = new JTabbedPane();
        panel1 = new JPanel();
        label2 = new JLabel();
        checkBox1 = new JCheckBox();
        label3 = new JLabel();
        checkBox2 = new JCheckBox();
        label4 = new JLabel();
        checkBox3 = new JCheckBox();
        panel2 = new JPanel();
        label5 = new JLabel();
        comboBox2 = new JComboBox<>();
        label6 = new JLabel();
        comboBox3 = new JComboBox<>();
        label7 = new JLabel();
        checkBox4 = new JCheckBox();
        label8 = new JLabel();
        textField1 = new JTextField();
        label9 = new JLabel();
        panel3 = new JPanel();
        label10 = new JLabel();
        checkBox5 = new JCheckBox();
        label11 = new JLabel();
        checkBox6 = new JCheckBox();
        label12 = new JLabel();
        checkBox7 = new JCheckBox();
        label13 = new JLabel();
        checkBox8 = new JCheckBox();
        label14 = new JLabel();
        checkBox9 = new JCheckBox();
        panel4 = new JPanel();
        panel5 = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle("ELS Navigator Settings");
        setMinimumSize(new Dimension(50, 31));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setMinimumSize(new Dimension(500, 100));
            dialogPane.setPreferredSize(new Dimension(570, 470));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.X_AXIS));

                //======== tabbedPane1 ========
                {

                    //======== panel1 ========
                    {
                        panel1.setLayout(new GridLayout(4, 2));

                        //---- label2 ----
                        label2.setText(bundle.getString("Settings.label2.text"));
                        panel1.add(label2);
                        panel1.add(checkBox1);

                        //---- label3 ----
                        label3.setText(bundle.getString("Settings.label3.text"));
                        panel1.add(label3);
                        panel1.add(checkBox2);

                        //---- label4 ----
                        label4.setText(bundle.getString("Settings.label4.text"));
                        panel1.add(label4);
                        panel1.add(checkBox3);
                    }
                    tabbedPane1.addTab(bundle.getString("Settings.panel1.tab.title"), panel1);

                    //======== panel2 ========
                    {
                        panel2.setLayout(new GridLayout(5, 2, 2, 2));

                        //---- label5 ----
                        label5.setText(bundle.getString("Settings.label5.text"));
                        panel2.add(label5);

                        //---- comboBox2 ----
                        comboBox2.setModel(new DefaultComboBoxModel<>(new String[] {
                            "System default, use for Windows",
                            "Metal",
                            "Nimbus",
                            "Flat light",
                            "Flat dark",
                            "IntelliJ light",
                            "IntelliJ dark"
                        }));
                        panel2.add(comboBox2);

                        //---- label6 ----
                        label6.setText(bundle.getString("Settings.label6.text"));
                        panel2.add(label6);

                        //---- comboBox3 ----
                        comboBox3.setModel(new DefaultComboBoxModel<>(new String[] {
                            "en_US"
                        }));
                        panel2.add(comboBox3);

                        //---- label7 ----
                        label7.setText(bundle.getString("Settings.label7.text"));
                        panel2.add(label7);

                        //---- checkBox4 ----
                        checkBox4.setText("otherwise decimal (1000) K scale");
                        panel2.add(checkBox4);

                        //---- label8 ----
                        label8.setText(bundle.getString("Settings.label8.text"));
                        panel2.add(label8);

                        //---- textField1 ----
                        textField1.setText("yyyy-MM-dd hh:mm:ss aa");
                        panel2.add(textField1);

                        //---- label9 ----
                        label9.setText(bundle.getString("Settings.label9.text"));
                        panel2.add(label9);
                    }
                    tabbedPane1.addTab(bundle.getString("Settings.panel2.tab.title"), panel2);

                    //======== panel3 ========
                    {
                        panel3.setLayout(new GridLayout(5, 2, 2, 2));

                        //---- label10 ----
                        label10.setText(bundle.getString("Settings.label10.text"));
                        panel3.add(label10);

                        //---- checkBox5 ----
                        checkBox5.setText(bundle.getString("Settings.checkBox5.text"));
                        panel3.add(checkBox5);

                        //---- label11 ----
                        label11.setText(bundle.getString("Settings.label11.text"));
                        panel3.add(label11);

                        //---- checkBox6 ----
                        checkBox6.setText(bundle.getString("Settings.checkBox6.text"));
                        panel3.add(checkBox6);

                        //---- label12 ----
                        label12.setText(bundle.getString("Settings.label12.text"));
                        panel3.add(label12);

                        //---- checkBox7 ----
                        checkBox7.setText(bundle.getString("Settings.checkBox7.text"));
                        panel3.add(checkBox7);

                        //---- label13 ----
                        label13.setText(bundle.getString("Settings.label13.text"));
                        panel3.add(label13);

                        //---- checkBox8 ----
                        checkBox8.setText(bundle.getString("Settings.checkBox8.text"));
                        panel3.add(checkBox8);

                        //---- label14 ----
                        label14.setText(bundle.getString("Settings.label14.text"));
                        panel3.add(label14);

                        //---- checkBox9 ----
                        checkBox9.setText(bundle.getString("Settings.checkBox9.text"));
                        panel3.add(checkBox9);
                    }
                    tabbedPane1.addTab(bundle.getString("Settings.panel3.tab.title"), panel3);

                    //======== panel4 ========
                    {
                        panel4.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    tabbedPane1.addTab(bundle.getString("Settings.panel4.tab.title"), panel4);

                    //======== panel5 ========
                    {
                        panel5.setLayout(new GridLayout(1, 2, 2, 2));
                    }
                    tabbedPane1.addTab(bundle.getString("Settings.panel5.tab.title"), panel5);
                }
                contentPanel.add(tabbedPane1);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

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
    private JTabbedPane tabbedPane1;
    private JPanel panel1;
    private JLabel label2;
    private JCheckBox checkBox1;
    private JLabel label3;
    private JCheckBox checkBox2;
    private JLabel label4;
    private JCheckBox checkBox3;
    private JPanel panel2;
    private JLabel label5;
    private JComboBox<String> comboBox2;
    private JLabel label6;
    private JComboBox<String> comboBox3;
    private JLabel label7;
    private JCheckBox checkBox4;
    private JLabel label8;
    private JTextField textField1;
    private JLabel label9;
    private JPanel panel3;
    private JLabel label10;
    private JCheckBox checkBox5;
    private JLabel label11;
    private JCheckBox checkBox6;
    private JLabel label12;
    private JCheckBox checkBox7;
    private JLabel label13;
    private JCheckBox checkBox8;
    private JLabel label14;
    private JCheckBox checkBox9;
    private JPanel panel4;
    private JPanel panel5;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
