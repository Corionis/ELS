package com.corionis.els.gui;

import javax.swing.border.*;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.jcraft.jsch.SftpProgressMonitor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;

public class Progress extends JFrame implements SftpProgressMonitor
{
    private long averageBps = 0l;
    private ActionListener cancelAction;
    private boolean cancelled = false;
    private boolean dryRun;
    private int fileNumber = 0;
    private int fixedHeight;
    private boolean forcedState = false;
    private Context context;
    private String lastStatus = "";
    private Logger logger = LogManager.getLogger("applog");
    private String name;
    private boolean prefSaved = false;
    private long progressCurrent = 0L;
    private long progressMax = 0L;
    private long progressMaxDivisor = 0L;
    private boolean noIcon = false;
    private Window owner;
    private long start = 0L;
    private long time = 0L;
    private long totalBytesCopied = 0L;
    private long totalBytesDivisor = 0L;
    private int totalFilesToCopy = 0;
    private long totalFilesBytes = 0L;

    /**
     * Progress dialog with progress bars
     *
     * @param context The ELS Context
     * @param owner The window that owns this dialog
     * @param cancelAction The cancel action listener
     * @param dryRun True if in dryrun mode, otherwise false
     */
    public Progress(Context context, Window owner, ActionListener cancelAction, boolean dryRun)
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
        context.mainFrame.labelStatusMiddle.setText("<html><body>&nbsp;</body></html>");

        progressBarFile.setMinimum(0);
        progressBarFile.setMaximum(100);
        progressBarFile.setValue(0);

        progressBarTotal.setMinimum(0);
        progressBarTotal.setMaximum(100);
        progressBarTotal.setValue(0);

        start = System.currentTimeMillis();
    }

    /**
     * Cancel has been clicked. Check if NavTransferWorker is running and prompt "Are you sure?".
     * <p>
     * Calls the define Cancel action listener if confirmed.
     *
     * @param e ActionEvent
     */
    private void cancelClicked(ActionEvent e)
    {
        if (context.browser.navTransferHandler.getTransferWorker() != null &&
                context.browser.navTransferHandler.getTransferWorker().isRunning())
        {
            Object[] opts = {context.cfg.gs("Z.yes"), context.cfg.gs("Z.no")};
            int r = JOptionPane.showOptionDialog(this,
                    context.cfg.gs("Z.cancel.the.current.operation"),
                    getTitle(), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                    null, opts, opts[1]);
            if (r == JOptionPane.NO_OPTION || r == JOptionPane.CANCEL_OPTION)
                return;

            // cancel sftp action
            context.browser.navTransferHandler.getTransferWorker().setIsRunning(false);
            cancelled = true;

            // give sftp a moment to stop
            try
            {
                Thread.sleep(1000);
            }
            catch (Exception e1)
            {}

            // proceed
            this.cancelAction.actionPerformed(e);
        }

        done();
    }

    /**
     * Position & size Progress dialog and display
     */
    public void display()
    {
        if (context.preferences.getProgressXpos() != -1 && Utils.isOnScreen(context.preferences.getProgressXpos(), context.preferences.getProgressYpos()))
        {
            setLocation(context.preferences.getProgressXpos(), context.preferences.getProgressYpos());
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, context.mainFrame));
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

        fixedHeight = this.getHeight();
        toFront();
    }

    /**
     * Progress updating is done, save Preferences and close.
     * <p>
     * Used by sftp client SftpProgressMonitor interface, and called directly
     * by other processes.
     */
    public void done()
    {
        // always store Progress preferences so the dialog can be viewed and moved/resized when not active
        storePreferences();
        setVisible(false);
    }

    /**
     * Initialize a file transfer, set start time and progress parameters
     *
     * @param op The operation. 0=put, 1=get, 2=copy
     * @param src The source filename
     * @param dest The destination filename
     * @param size The size of the file
     * <p>
     * Used by sftp client SftpProgressMonitor interface, and called directly
     * by other processes.
     */
    @Override
    public void init(int op, String src, String dest, long size)
    {
        progressCurrent = 0L;
        progressMax = size;
        progressBarFile.setValue(0);
        progressMaxDivisor = progressMax / 100;
        if (progressMaxDivisor <= 0)
            progressMaxDivisor = 1;
        time = System.currentTimeMillis();

        String[] actions = {"put", "get", "copy"};
        String action = actions[op];

        logger.trace(action + " " + src + " " + dest + " " + size);
    }

    /**
     * Update progress bars with new values.
     * <p>
     * Used by sftp client SftpProgressMonitor interface, and called directly
     * by other processes.
     *
     * @param count Progress count, incremental
     * @return true to keep running, otherwise false to cancel
     */
    @Override
    public boolean count(long count)
    {
        progressCurrent += count;
        totalBytesCopied += count;

        progressBarFile.setValue((int)(progressCurrent / progressMaxDivisor));
        int pc = (int)(((double)progressCurrent / (double)progressMax) * 100.0);
        progressBarFile.setString(context.cfg.gs("NavTransferHandler.progress.file") + " " + pc + "%");

        progressBarTotal.setValue((int)(totalBytesCopied / totalBytesDivisor));
        pc = (int)(((double) totalBytesCopied / (double) totalFilesBytes) * 100.0);
        progressBarTotal.setString(context.cfg.gs("NavTransferHandler.progress.total") + " " + pc + "%");

        return !cancelled;
    }

    /**
     * Transfer is complete, report statistics
     * <p>
     * Used by sftp client SftpProgressMonitor interface, and called directly
     * by other processes.
     */
    @Override
    public void end()
    {
        long now = System.currentTimeMillis();
        long diff = now - time;
        diff = diff / 1000; // convert to seconds
        if (diff > 0)
        {
            long rate = progressMax / diff;
            String duration = Utils.formatDuration(diff);
            logger.info("Transfer " + (cancelled ? "cancelled" : "complete: " +
                    Utils.formatLong(progressMax, false, context.cfg.getLongScale()) + ", " +
                    duration + ", " + Utils.formatRate(rate,  context.cfg.getLongScale())));
            if (averageBps > 0)
                averageBps = (averageBps + rate) / 2;
            else
                averageBps = rate;
        }
    }

    public long getAverageBps()
    {
        return averageBps;
    }

    public long getStartTime()
    {
        return start;
    }

    public long getTotalBytesCopied()
    {
        return totalBytesCopied;
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
        }
    }

    public void redraw()
    {
        Graphics gfx = progressTextField.getGraphics();
        if (gfx != null)
            progressTextField.update(gfx);
        progressTextField.repaint();
        repaint();
    }

    /**
     * Set the counts when a new Batch is added.
     *
     * @param filesToCopy Total number of files to copy
     * @param totalFilesBytes Total number of bytes to copy
     */
    public void setCountAndBytes(int filesToCopy, long totalFilesBytes)
    {
        this.totalFilesToCopy = filesToCopy;
        this.totalFilesBytes = totalFilesBytes;
        this.totalBytesDivisor = totalFilesBytes / 100L;
        if (this.totalBytesDivisor <= 0)
            this.totalBytesDivisor = 1;
        update(fileNumber, progressMax, name);
    }

    private void storePreferences()
    {
        context.preferences.setProgressWidth(getWidth());
        context.preferences.setProgressHeight(getHeight());
        context.preferences.setProgressXpos(getX());
        context.preferences.setProgressYpos(getY());
        prefSaved = true;
    }

    private void thisComponentResized(ComponentEvent e)
    {
        if (!forcedState)
        {
            context.preferences.setProgressWidth(getWidth());
            update(lastStatus);
        }
        else
            forcedState = false;
        this.setSize(context.preferences.getProgressWidth(), fixedHeight);
    }

    private void thisWindowClosed(WindowEvent e)
    {
        if (owner != null)
            owner.requestFocus();
    }

    private void thisWindowClosing(WindowEvent e)
    {
        if (!prefSaved)
            storePreferences();

        setVisible(false);
        if (owner != null)
        {
            owner.requestFocus();
            owner = null;
        }
    }

    private void thisWindowStateChanged(WindowEvent e)
    {
        if (e.getNewState() != JFrame.NORMAL && e.getNewState() != JFrame.ICONIFIED)
        {
            forcedState = true;
            setState(JFrame.NORMAL);
            setExtendedState(JFrame.NORMAL);
            setSize(context.preferences.getProgressWidth(), fixedHeight);
        }
    }

    /**
     * Update the status String. Ellipse if needed.
     * @param status String
     */
    public synchronized void update(String status)
    {
        lastStatus = status;
        String ellipse = Utils.ellipseFileString(progressTextField, status);
        progressTextField.setText(ellipse);
        redraw();
    }

    /**
     * Update the status String with file number, size and name.
     * @param fileNumber The counter for the file number
     * @param size The size of the file
     * @param name The name of the name
     */
    public synchronized void update(int fileNumber, long size, String name)
    {
        if (fileNumber > 0)
        {
            this.fileNumber = fileNumber;
            this.progressMax = size;
            progressMaxDivisor = this.progressMax / 100;
            this.name = name;
            String status = " " + context.cfg.gs("NavTransferHandler.progress.file") +
                    " " + fileNumber +
                    " (" + Utils.formatLong(size, false, context.cfg.getLongScale()) +
                    ") " + context.cfg.gs("NavTransferHandler.progress.of") +
                    " " + totalFilesToCopy +
                    " (" + Utils.formatLong(totalFilesBytes, false, context.cfg.getLongScale()) +
                    "): " + name;
            progressTextField.setToolTipText(name);
            update(status);
        }
    }

    /**
     * For the View, Progress menu item.
     * <p>
     * Progress may be closed, this reopens and updates the dialog accordingly.
     */
    public void view()
    {
        String ls = lastStatus;
        display();
        if (totalFilesToCopy > 0 || totalBytesCopied > 0)
        {
            update(ls);
        }
        else
        {
            this.labelForIcon.setVisible(false);
            update(context.cfg.gs("Progress.not.active"));
        }
        toFront();
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
        panelButton = new JPanel();
        hSpacer3 = new JPanel(null);
        buttonCancel = new JButton();
        hSpacer2 = new JPanel(null);
        hSpacer4 = new JPanel(null);
        panelProgress = new JPanel();
        progressBarFile = new JProgressBar();
        progressBarTotal = new JProgressBar();
        vSpacer3 = new JPanel(null);
        vSpacer5 = new JPanel(null);

        //======== this ========
        setMinimumSize(new Dimension(184, 128));
        setTitle(context.cfg.gs("Progress.title"));
        setName("ProgressBox");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setPreferredSize(new Dimension(10, 128));
        setMaximumSize(new Dimension(2147483647, 128));
        addWindowListener(new WindowAdapter() {
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
        contentPane.setLayout(new BorderLayout(4, 4));

        //======== panelWidget ========
        {
            panelWidget.setMaximumSize(new Dimension(40, 36));
            panelWidget.setAlignmentY(1.0F);
            panelWidget.setMinimumSize(new Dimension(40, 36));
            panelWidget.setPreferredSize(new Dimension(40, 36));
            panelWidget.setLayout(new BoxLayout(panelWidget, BoxLayout.X_AXIS));

            //---- hSpacer1 ----
            hSpacer1.setPreferredSize(new Dimension(4, 10));
            hSpacer1.setMinimumSize(new Dimension(4, 10));
            hSpacer1.setMaximumSize(new Dimension(4, 10));
            panelWidget.add(hSpacer1);

            //---- labelForIcon ----
            labelForIcon.setPreferredSize(new Dimension(36, 32));
            labelForIcon.setMinimumSize(new Dimension(36, 32));
            labelForIcon.setMaximumSize(new Dimension(36, 32));
            labelForIcon.setHorizontalTextPosition(SwingConstants.LEFT);
            labelForIcon.setHorizontalAlignment(SwingConstants.LEFT);
            labelForIcon.setIcon(new ImageIcon(getClass().getResource("/running.gif")));
            panelWidget.add(labelForIcon);
        }
        contentPane.add(panelWidget, BorderLayout.WEST);

        //---- progressTextField ----
        progressTextField.setPreferredSize(new Dimension(140, 30));
        progressTextField.setMinimumSize(new Dimension(140, 30));
        progressTextField.setMaximumSize(new Dimension(5000, 30));
        progressTextField.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        progressTextField.setEditable(false);
        progressTextField.setHorizontalAlignment(SwingConstants.LEFT);
        progressTextField.setBorder(null);
        progressTextField.setMargin(new Insets(2, 0, 2, 8));
        progressTextField.setText(context.cfg.gs("Progress.not.active"));
        contentPane.add(progressTextField, BorderLayout.CENTER);

        //======== panelButton ========
        {
            panelButton.setPreferredSize(new Dimension(88, 20));
            panelButton.setMinimumSize(new Dimension(88, 20));
            panelButton.setMaximumSize(new Dimension(88, 20));
            panelButton.setLayout(new BorderLayout());

            //---- hSpacer3 ----
            hSpacer3.setMinimumSize(new Dimension(10, 3));
            hSpacer3.setMaximumSize(new Dimension(32767, 3));
            hSpacer3.setPreferredSize(new Dimension(10, 3));
            panelButton.add(hSpacer3, BorderLayout.NORTH);

            //---- buttonCancel ----
            buttonCancel.setText(context.cfg.gs("Progress.buttonCancel.text"));
            buttonCancel.setMaximumSize(new Dimension(78, 20));
            buttonCancel.setMinimumSize(new Dimension(78, 20));
            buttonCancel.setPreferredSize(new Dimension(78, 20));
            buttonCancel.setActionCommand(context.cfg.gs("Progress.buttonCancel.text"));
            buttonCancel.setToolTipText(context.cfg.gs("Z.cancel.current.operation"));
            buttonCancel.addActionListener(e -> cancelClicked(e));
            panelButton.add(buttonCancel, BorderLayout.CENTER);

            //---- hSpacer2 ----
            hSpacer2.setPreferredSize(new Dimension(4, 10));
            hSpacer2.setMinimumSize(new Dimension(4, 10));
            hSpacer2.setMaximumSize(new Dimension(4, 10));
            panelButton.add(hSpacer2, BorderLayout.EAST);

            //---- hSpacer4 ----
            hSpacer4.setMinimumSize(new Dimension(10, 3));
            hSpacer4.setMaximumSize(new Dimension(32767, 3));
            hSpacer4.setPreferredSize(new Dimension(10, 3));
            panelButton.add(hSpacer4, BorderLayout.SOUTH);
        }
        contentPane.add(panelButton, BorderLayout.EAST);

        //======== panelProgress ========
        {
            panelProgress.setMaximumSize(new Dimension(2147483647, 49));
            panelProgress.setMinimumSize(new Dimension(2147483647, 49));
            panelProgress.setPreferredSize(new Dimension(2147483647, 49));
            panelProgress.setBorder(new EmptyBorder(0, 4, 4, 4));
            panelProgress.setAlignmentY(0.0F);
            panelProgress.setLayout(new BorderLayout(8, 4));

            //---- progressBarFile ----
            progressBarFile.setForeground(Color.lightGray);
            progressBarFile.setPreferredSize(new Dimension(2147483647, 18));
            progressBarFile.setMinimumSize(new Dimension(2147483647, 18));
            progressBarFile.setMaximumSize(new Dimension(2147483647, 18));
            progressBarFile.setMaximum(1000);
            progressBarFile.setFocusable(false);
            progressBarFile.setToolTipText(context.cfg.gs("NavTransferHandler.progressBarFile.toolTipText"));
            progressBarFile.setStringPainted(true);
            panelProgress.add(progressBarFile, BorderLayout.NORTH);

            //---- progressBarTotal ----
            progressBarTotal.setForeground(Color.lightGray);
            progressBarTotal.setPreferredSize(new Dimension(2147483647, 18));
            progressBarTotal.setMinimumSize(new Dimension(2147483647, 18));
            progressBarTotal.setMaximumSize(new Dimension(2147483647, 18));
            progressBarTotal.setMaximum(1000);
            progressBarTotal.setFocusable(false);
            progressBarTotal.setStringPainted(true);
            progressBarTotal.setToolTipText(context.cfg.gs("NavTransferHandler.progressBarTotal.toolTipText"));
            panelProgress.add(progressBarTotal, BorderLayout.CENTER);

            //---- vSpacer3 ----
            vSpacer3.setPreferredSize(new Dimension(10, 1));
            vSpacer3.setMinimumSize(new Dimension(10, 1));
            vSpacer3.setMaximumSize(new Dimension(10, 1));
            panelProgress.add(vSpacer3, BorderLayout.SOUTH);
        }
        contentPane.add(panelProgress, BorderLayout.SOUTH);

        //---- vSpacer5 ----
        vSpacer5.setMaximumSize(new Dimension(32767, 2));
        vSpacer5.setMinimumSize(new Dimension(12, 2));
        vSpacer5.setPreferredSize(new Dimension(10, 2));
        contentPane.add(vSpacer5, BorderLayout.NORTH);
        pack();
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    public JPanel panelWidget;
    public JPanel hSpacer1;
    public JLabel labelForIcon;
    public JTextField progressTextField;
    public JPanel panelButton;
    public JPanel hSpacer3;
    public JButton buttonCancel;
    public JPanel hSpacer2;
    public JPanel hSpacer4;
    public JPanel panelProgress;
    public JProgressBar progressBarFile;
    public JProgressBar progressBarTotal;
    public JPanel vSpacer3;
    public JPanel vSpacer5;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
    //
    // @formatter:on
    // </editor-fold>

}
