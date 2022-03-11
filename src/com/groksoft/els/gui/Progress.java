package com.groksoft.els.gui;

import java.util.*;
import com.groksoft.els.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class Progress extends JFrame
{
    private final int MAX_STATUS = 62;
    private transient Logger logger = LogManager.getLogger("applog");

    private boolean active = false;
    private GuiContext guiContext;
    private int fileNumber;
    private int filesToCopy;
    private long filesSize;
    private long size;
    private String name;

    public Progress(GuiContext context)
    {
        guiContext = context;

/*
        try
        {
            if (guiContext.preferences.getLookAndFeel() == 0)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            else
            {
                LookAndFeel laf = guiContext.form.getLookAndFeel(guiContext.preferences.getLookAndFeel());
                UIManager.setLookAndFeel(laf);
            }
        }
        catch (Exception e)
        {}
*/

        initComponents();
        setIconImage(new ImageIcon(getClass().getResource("/els-logo-98px.png")).getImage());
        //setSize(400, 75);
        setLocationByPlatform(false);
        setLocationRelativeTo(getOwner());
    }

    public void display()
    {
        if (guiContext.preferences.getProgressXpos() > 0)
        {
            setLocation(guiContext.preferences.getProgressXpos(), guiContext.preferences.getProgressYpos());
        }
        else
        {
            int x = guiContext.form.getX() + (guiContext.form.getWidth() / 2) - (getWidth() / 2);
            int y = guiContext.form.getY() + (guiContext.form.getHeight() / 2) - (getHeight() / 2);
            setLocation(x, y);
        }
        setVisible(true);
        active = true;
    }

    public void done()
    {
        saveLocation();
        reset();
        setVisible(false);
        active = false;
    }

    public boolean isActive()
    {
        return active;
    }

    private void reset()
    {
        fileNumber = 0;
        filesToCopy = 0;
        filesSize = 0L;
        size = 0;
        name = "";
    }

    private void saveLocation()
    {
        guiContext.preferences.setProgressXpos(getX());
        guiContext.preferences.setProgressYpos(getY());
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
        saveLocation();
        this.getOwner().requestFocus();
    }

    private synchronized void update()
    {
        String status = fileNumber + guiContext.cfg.gs("NavTransferHandler.progress.of") + filesToCopy +
                ", " + Utils.formatLong(size, false) + ", " + name;

        if (status.length() > MAX_STATUS)
        {
            String ext = Utils.getFileExtension(status);
            status = StringUtils.abbreviate(status, MAX_STATUS - 3 - ext.length());
            status += ext;
        }

        progressTextField.setText(status);
        progressTextField.update(progressTextField.getGraphics());
        progressTextField.repaint();
        repaint();
    }

    public void update(int filesToCopy, long filesSize)
    {
        this.filesToCopy = filesToCopy;
        this.filesSize += filesSize;
        update();
    }

    public void update(int fileNumber, long size, String name)
    {
        this.fileNumber = fileNumber;
        this.size = size;
        this.name = name;
        update();
    }

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = guiContext.cfg.bundle();
        hSpacer1 = new JPanel(null);
        vSpacer1 = new JPanel(null);
        progressTextField = new JTextField();

        //======== this ========
        setMinimumSize(new Dimension(400, 75));
        setTitle(bundle.getString("Progress.this.title"));
        setResizable(false);
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
