package com.groksoft.els.gui;

import com.groksoft.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

public class Progress extends JFrame
{
    private transient Logger logger = LogManager.getLogger("applog");

    private boolean beingUsed = false;
    private int currentWidth;
    private GuiContext guiContext;
    private int fixedHeight;
    private boolean forcedState = false;
    private String lastStatus = "";
    private Component owner;

    public Progress(GuiContext context, Component owner)
    {
        this.guiContext = context;
        this.owner = owner;

        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
        try
        {
            URL url = getClass().getResource("/running.gif");
            Icon icon = new ImageIcon(url);
            this.labelForIcon.setIcon(icon);
        }
        catch (Exception e)
        {
            this.labelForIcon.setVisible(false);
            this.hSpacer1.setVisible(false);
        }
        this.progressTextField.setBorder(null);
        setLocationByPlatform(false);
        setLocationRelativeTo(owner);
    }

    public void display()
    {
        if (guiContext.preferences.getProgressXpos() > 0)
        {
            setLocation(guiContext.preferences.getProgressXpos(), guiContext.preferences.getProgressYpos());
        }
        else
        {
            int x = guiContext.mainFrame.getX() + (guiContext.mainFrame.getWidth() / 2) - (getWidth() / 2);
            int y = guiContext.mainFrame.getY() + (guiContext.mainFrame.getHeight() / 2) - (getHeight() / 2);
            setLocation(x, y);
        }

        if (guiContext.preferences.getProgressWidth() > 0)
        {
            setSize(guiContext.preferences.getProgressWidth(), guiContext.preferences.getProgressHeight());
        }

        setVisible(true);

        fixedHeight = this.getHeight();
        currentWidth = this.getWidth();
        beingUsed = true;
    }

    public void done()
    {
        savePreferences();
        setVisible(false);
        beingUsed = false;
    }

    public boolean isBeingUsed()
    {
        return beingUsed;
    }

    private void savePreferences()
    {
        guiContext.preferences.setProgressWidth(getWidth());
        guiContext.preferences.setProgressHeight(getHeight());
        guiContext.preferences.setProgressXpos(getX());
        guiContext.preferences.setProgressYpos(getY());
    }

    private void thisComponentResized(ComponentEvent e)
    {
        if (!forcedState)
        {
            currentWidth = getWidth();
            update(lastStatus);
        }
        else
            forcedState = false;
        this.setSize(currentWidth, fixedHeight);
    }

    private void thisWindowActivated(WindowEvent e)
    {
        toFront();
    }

    private void thisWindowClosed(WindowEvent e)
    {
        if (owner != null)
            owner.requestFocus();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        savePreferences();
        if (owner != null)
            owner.requestFocus();
    }

    private void thisWindowStateChanged(WindowEvent e)
    {
        if (e.getNewState() != JFrame.NORMAL && e.getNewState() != JFrame.ICONIFIED)
        {
            forcedState = true;
            setExtendedState(JFrame.NORMAL);
            setSize(currentWidth, fixedHeight);
        }
    }

    public synchronized void update(String status)
    {
        lastStatus = status;
        String ellipse = Utils.ellipseFileString(progressTextField, status);
        progressTextField.setText(ellipse);
        Graphics gfx = progressTextField.getGraphics();
        if (gfx != null)
            progressTextField.update(gfx);
        progressTextField.repaint();
        repaint();
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panel1 = new JPanel();
        hSpacer1 = new JPanel(null);
        labelForIcon = new JLabel();
        progressTextField = new JTextField();

        //======== this ========
        setMinimumSize(new Dimension(184, 75));
        setTitle(guiContext.cfg.gs("Progress.title"));
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
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                thisComponentResized(e);
            }
        });
        addWindowStateListener(e -> thisWindowStateChanged(e));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(4, 0));

        //======== panel1 ========
        {
            panel1.setLayout(new BoxLayout(panel1, BoxLayout.X_AXIS));

            //---- hSpacer1 ----
            hSpacer1.setMinimumSize(new Dimension(8, 32));
            hSpacer1.setPreferredSize(new Dimension(8, 32));
            panel1.add(hSpacer1);

            //---- labelForIcon ----
            labelForIcon.setPreferredSize(new Dimension(32, 32));
            labelForIcon.setMinimumSize(new Dimension(32, 32));
            labelForIcon.setMaximumSize(new Dimension(32, 32));
            labelForIcon.setHorizontalTextPosition(SwingConstants.LEFT);
            labelForIcon.setHorizontalAlignment(SwingConstants.LEFT);
            panel1.add(labelForIcon);
        }
        contentPane.add(panel1, BorderLayout.WEST);

        //---- progressTextField ----
        progressTextField.setPreferredSize(new Dimension(378, 30));
        progressTextField.setMinimumSize(new Dimension(140, 30));
        progressTextField.setMaximumSize(new Dimension(5000, 30));
        progressTextField.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        progressTextField.setEditable(false);
        progressTextField.setHorizontalAlignment(SwingConstants.LEFT);
        progressTextField.setBorder(null);
        progressTextField.setMargin(new Insets(2, 0, 2, 8));
        contentPane.add(progressTextField, BorderLayout.CENTER);
        pack();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JPanel panel1;
    public JPanel hSpacer1;
    public JLabel labelForIcon;
    public JTextField progressTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
