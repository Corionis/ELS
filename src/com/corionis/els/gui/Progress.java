package com.corionis.els.gui;

import com.corionis.els.Context;
import com.corionis.els.Utils;
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
    private Context context;
    private String lastStatus = "";
    private boolean noIcon = false;
    private Component owner;

    public Progress(Context context, Component owner, ActionListener cancelAction, boolean dryRun)
    {
        this.context = context;
        this.owner = owner;
        this.cancelAction = cancelAction;
        this.dryRun = dryRun;
        this.context.progress = this;

        initComponents();
        loadIcon();
        this.progressTextField.setBorder(null);
        setLocationByPlatform(false);
        setLocationRelativeTo(null);

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelClicked(actionEvent);
            }
        };

        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        context.mainFrame.labelStatusMiddle.setText("");
    }

    private void cancelClicked(ActionEvent e)
    {
        if (isBeingUsed())
        {
            Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no")};
            int r = JOptionPane.showOptionDialog(this,
                    context.cfg.gs("Z.cancel.the.current.operation"),
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

        if (context.preferences.getProgressXpos() != 0 && Utils.isOnScreen(context.preferences.getProgressXpos(), context.preferences.getProgressYpos()))
        {
            setLocation(context.preferences.getProgressXpos(), context.preferences.getProgressYpos());
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(this));
        }

        if (context.preferences.getProgressWidth() > 0)
        {
            setSize(context.preferences.getProgressWidth(), context.preferences.getProgressHeight());
        }

        if (!dryRun)
            setTitle(context.cfg.gs("Progress.title"));
        else
            setTitle(context.cfg.gs("Progress.title.dryrun"));

        if (!noIcon)
            this.labelForIcon.setVisible(true);

        setVisible(true);

        setState(JFrame.NORMAL);
        context.progress.toFront();

        update(context.cfg.gs("Progress.not.active"));

        fixedHeight = this.getHeight();
        currentWidth = this.getWidth();
    }

    public void done()
    {
        if (isBeingUsed())
        {
            storePreferences();

            // clear the progress content
            labelForIcon.setVisible(false);
            setVisible(false);
            progressTextField.setText(context.cfg.gs("Progress.not.active"));
            //redraw();
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

    private void storePreferences()
    {
        context.preferences.setProgressWidth(getWidth());
        context.preferences.setProgressHeight(getHeight());
        context.preferences.setProgressXpos(getX());
        context.preferences.setProgressYpos(getY());
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
        storePreferences();
        if (!beingUsed)
            setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        beingUsed = false;
        setVisible(false);
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
        String ls = lastStatus;
        display();
        if (bu)
        {
            update(ls);
        }
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
        setTitle(context.cfg.gs("Progress.title"));
        setName("ProgressBox");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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
        var contentPane = getContentPane();
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
            buttonCancel.setText(context.cfg.gs("Progress.buttonCancel.text_2"));
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
