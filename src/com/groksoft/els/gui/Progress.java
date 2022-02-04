package com.groksoft.els.gui;

import java.awt.*;
import java.util.*;
import javax.swing.*;

public class Progress extends JDialog {

    public Progress(Window owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        progressText = new JLabel();
        progressBar = new JProgressBar();

        //======== this ========
        setName("ProgressBox");
        setMinimumSize(new Dimension(400, 75));
        setTitle("ELS Progress");
        setResizable(false);
        setAlwaysOnTop(true);
        Container contentPane = getContentPane();
        contentPane.setLayout(new FlowLayout());
        ((FlowLayout)contentPane.getLayout()).setAlignOnBaseline(true);

        //---- progressText ----
        progressText.setText(bundle.getString("Progress.progressText.text"));
        progressText.setHorizontalAlignment(SwingConstants.CENTER);
        progressText.setPreferredSize(new Dimension(382, 16));
        progressText.setMinimumSize(new Dimension(382, 16));
        progressText.setMaximumSize(new Dimension(382, 16));
        contentPane.add(progressText);

        //---- progressBar ----
        progressBar.setPreferredSize(new Dimension(382, 10));
        progressBar.setMinimumSize(new Dimension(382, 10));
        progressBar.setMaximumSize(new Dimension(382, 10));
        contentPane.add(progressBar);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JLabel progressText;
    public JProgressBar progressBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
