package com.groksoft.els.gui;

import java.beans.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Progress extends JDialog
{
    private transient Logger logger = LogManager.getLogger("applog");
    GuiContext guiContext;

    public Progress(Window owner, GuiContext context)
    {
        super(owner);
        guiContext = context;
        initComponents();
        setSize(400, 75);
    }

    public void done()
    {
        javax.swing.SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                setVisible(false);
            }
        });

    }

    private void thisWindowActivated(WindowEvent e)
    {
        toFront();
    }

    private void thisWindowClosed(WindowEvent e)
    {
        this.getOwner().requestFocus();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        this.getOwner().requestFocus();
    }

    public void update(String status)
    {
        progressTextField.setText(status);
        progressTextField.update(progressTextField.getGraphics());
        progressTextField.repaint();
        repaint();
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        hSpacer1 = new JPanel(null);
        vSpacer1 = new JPanel(null);
        progressTextField = new JTextField();

        //======== this ========
        setMinimumSize(new Dimension(400, 75));
        setTitle("ELS Progress");
        setResizable(false);
        setAlwaysOnTop(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setName("ProgressBox");
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                thisWindowActivated(e);
            }
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
        contentPane.setLayout(new FlowLayout());
        ((FlowLayout)contentPane.getLayout()).setAlignOnBaseline(true);
        contentPane.add(hSpacer1);
        contentPane.add(vSpacer1);

        //---- progressTextField ----
        progressTextField.setPreferredSize(new Dimension(378, 30));
        progressTextField.setMinimumSize(new Dimension(378, 30));
        progressTextField.setMaximumSize(new Dimension(378, 30));
        progressTextField.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        progressTextField.setEditable(false);
        progressTextField.setHorizontalAlignment(SwingConstants.CENTER);
        progressTextField.setBorder(null);
        contentPane.add(progressTextField);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JPanel hSpacer1;
    public JPanel vSpacer1;
    public JTextField progressTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>
}
