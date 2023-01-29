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
    private ActionListener cancelAction;
    private int currentWidth;
    private boolean dryRun;
    private int fixedHeight;
    private boolean forcedState = false;
    private GuiContext guiContext;
    private String lastStatus = "";
    private boolean noIcon = false;
    private Component owner;

    public Progress(GuiContext context, Component owner, ActionListener cancelAction, boolean dryRun)
    {
        this.guiContext = context;
        this.owner = owner;
        this.cancelAction = cancelAction;
        this.dryRun = dryRun;

        initComponents();
        loadIcon();
        this.progressTextField.setBorder(null);
        setLocationByPlatform(false);
        setLocationRelativeTo(null);

        guiContext.mainFrame.labelStatusMiddle.setText("");
    }

    private void cancelClicked(ActionEvent e)
    {
        if (isBeingUsed())
        {
            Object[] opts = {guiContext.cfg.gs("Z.yes"), guiContext.cfg.gs("Z.no")};
            int r = JOptionPane.showOptionDialog(this,
                    guiContext.cfg.gs("Z.cancel.the.current.operation"),
                    getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[1]);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                return;
            this.cancelAction.actionPerformed(e);
        }
        done();
    }

    public void display()
    {
        beingUsed = true;

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

        if (!dryRun)
            setTitle(guiContext.cfg.gs("Progress.title"));
        else
            setTitle(guiContext.cfg.gs("Progress.title.dryrun"));

        if (!noIcon)
            this.labelForIcon.setVisible(true);

        setVisible(true);

        setState(JFrame.NORMAL);
        guiContext.progress.toFront();

        update(guiContext.cfg.gs("Progress.not.active"));

        fixedHeight = this.getHeight();
        currentWidth = this.getWidth();
    }

    public void done()
    {
        if (isBeingUsed())
        {
            savePreferences();

            // clear the progress content
            labelForIcon.setVisible(false);
            progressTextField.setText(guiContext.cfg.gs("Progress.not.active"));
            redraw();
        }
        beingUsed = false;
        setVisible(false);
    }

    public boolean isBeingUsed()
    {
        return beingUsed;
    }

    private void loadIcon()
    {
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());

        try
        {
            URL url = getClass().getResource("/running.gif");
            Icon icon = new ImageIcon(url);
            this.labelForIcon.setIcon(icon);
        }
        catch (Exception e)
        {
            noIcon = true;
            this.labelForIcon.setVisible(false);
            this.hSpacer1.setVisible(false);
        }
    }

    private void redraw()
    {
        Graphics gfx = progressTextField.getGraphics();
        if (gfx != null)
            progressTextField.update(gfx);
        progressTextField.repaint();
        repaint();
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
            setState(JFrame.NORMAL);
            setExtendedState(JFrame.NORMAL);
            setSize(currentWidth, fixedHeight);
        }
    }

    public synchronized void update(String status)
    {
        lastStatus = status;
        String ellipse = Utils.ellipseFileString(progressTextField, status);
        progressTextField.setText(ellipse);
        redraw();
    }

    public void view()
    {
        boolean bu = beingUsed;
        display();
        if (!bu)
        {
            labelForIcon.setVisible(false);
            beingUsed = false;
        }
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        panelWidget = new JPanel();
        hSpacer1 = new JPanel(null);
        labelForIcon = new JLabel();
        progressTextField = new JTextField();
        panel1 = new JPanel();
        vSpacer1 = new JPanel(null);
        buttonCancel = new JButton();
        vSpacer2 = new JPanel(null);
        hSpacer2 = new JPanel(null);

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

        //======== panelWidget ========
        {
            panelWidget.setMaximumSize(new Dimension(32799, 32));
            panelWidget.setLayout(new BoxLayout(panelWidget, BoxLayout.X_AXIS));

            //---- hSpacer1 ----
            hSpacer1.setMinimumSize(new Dimension(8, 32));
            hSpacer1.setPreferredSize(new Dimension(8, 32));
            panelWidget.add(hSpacer1);

            //---- labelForIcon ----
            labelForIcon.setPreferredSize(new Dimension(32, 32));
            labelForIcon.setMinimumSize(new Dimension(32, 32));
            labelForIcon.setMaximumSize(new Dimension(32, 32));
            labelForIcon.setHorizontalTextPosition(SwingConstants.LEFT);
            labelForIcon.setHorizontalAlignment(SwingConstants.LEFT);
            panelWidget.add(labelForIcon);
        }
        contentPane.add(panelWidget, BorderLayout.WEST);

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

        //======== panel1 ========
        {
            panel1.setLayout(new BorderLayout());

            //---- vSpacer1 ----
            vSpacer1.setPreferredSize(new Dimension(10, 6));
            vSpacer1.setMinimumSize(new Dimension(10, 6));
            vSpacer1.setMaximumSize(new Dimension(10, 6));
            panel1.add(vSpacer1, BorderLayout.NORTH);

            //---- buttonCancel ----
            buttonCancel.setText(guiContext.cfg.gs("Progress.buttonCancel.text_2"));
            buttonCancel.addActionListener(e -> cancelClicked(e));
            panel1.add(buttonCancel, BorderLayout.CENTER);

            //---- vSpacer2 ----
            vSpacer2.setPreferredSize(new Dimension(10, 6));
            vSpacer2.setMinimumSize(new Dimension(10, 6));
            vSpacer2.setMaximumSize(new Dimension(10, 6));
            panel1.add(vSpacer2, BorderLayout.SOUTH);

            //---- hSpacer2 ----
            hSpacer2.setPreferredSize(new Dimension(4, 10));
            hSpacer2.setMinimumSize(new Dimension(4, 10));
            hSpacer2.setMaximumSize(new Dimension(4, 10));
            panel1.add(hSpacer2, BorderLayout.EAST);
        }
        contentPane.add(panel1, BorderLayout.EAST);
        pack();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JPanel panelWidget;
    public JPanel hSpacer1;
    public JLabel labelForIcon;
    public JTextField progressTextField;
    public JPanel panel1;
    public JPanel vSpacer1;
    public JButton buttonCancel;
    public JPanel vSpacer2;
    public JPanel hSpacer2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
