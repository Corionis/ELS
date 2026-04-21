package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.Utils;
import com.corionis.els.repository.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileFilter;

public class InviteContentUI extends JDialog 
{
    private boolean archiveOnly = false;
    private Context context;
    private InviteUI inviteUI;
    public boolean cancelled = true;
    private LibrariesUI.LibMeta libMeta;
    private Logger logger = LogManager.getLogger("applog");

    public InviteContentUI(InviteUI inviteUI, Context context, LibrariesUI.LibMeta libMeta, boolean archiveOnly)
    {
        super(inviteUI);
        this.inviteUI = inviteUI;
        this.context = context;
        this.libMeta = libMeta;
        this.archiveOnly = archiveOnly;
        initComponents();

        if (archiveOnly)
        {
            labelChoose.setVisible(false);
            textFieldChoose.setVisible(false);
            buttonChoose.setVisible(false);
            vSpacer1.setVisible(false);
            Dimension dim = getSize();
            dim.height -= 60;
            setSize(dim);
            repaint();
        }
        else
        {
            // position & size
            this.setLocation(inviteUI.getLocation().x, inviteUI.getLocation().y);
            this.setSize(inviteUI.getSize());
        }

        setDialog();
    }

    private void actionCancelClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void buttonChooseClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                return (file.getName().toLowerCase().endsWith(".tar") || file.getName().toLowerCase().endsWith(".zip"));
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("InviteContent.archive.files");
            }
        });
        fc.setDialogTitle(context.cfg.gs("InviteContent.select.archive"));
        fc.setFileHidingEnabled(false);
        File ld = null;
        ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "output");
        if (!ld.exists() || !ld.isDirectory())
            ld = new File(context.cfg.getWorkingDirectory());
        if (ld.exists() && ld.isDirectory())
            fc.setCurrentDirectory(ld);

/*
        if (libMeta.repo.getJsonFilename().length() > 0)
        {
            File lf = new File(libMeta.repo.getJsonFilename());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }
        else if (context.preferences.getLastSubscriberOpenFile().length() > 0)
        {
            File lf = new File(context.preferences.getLastSubscriberOpenFile());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }
*/

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }

                textFieldChoose.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath()));
            }
            else
            {
                textFieldChoose.setText("");
            }
            break;
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        if (checkBoxPublisher.isSelected() && textFieldCurrentPublisher.getText().isEmpty() ||
            checkBoxSubscriber.isSelected() && textFieldCurrentSubscriber.getText().isEmpty() ||
            checkBoxHintServer.isSelected() && textFieldCurrentHintServer.getText().isEmpty() ||
            checkBoxHints.isSelected() && textFieldCurrentHints.getText().isEmpty() )
        {
            JOptionPane.showConfirmDialog(this, context.cfg.gs("InviteContent.please.complete.missing.selected.files"), this.getTitle(), JOptionPane.OK_OPTION, JOptionPane.WARNING_MESSAGE);
            return;
        }

        cancelled = false;
        setVisible(false);
    }

    private void buttonPublisherClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                return (file.getName().toLowerCase().endsWith(".json"));
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("Navigator.menu.Open.publisher.files");
            }
        });
        fc.setDialogTitle(context.cfg.gs("InviteContent.select.publisher"));
        fc.setFileHidingEnabled(false);
        File ld = null;
        if (context.preferences.getLastSubscriberOpenPath().length() > 0)
        {
            ld = new File(context.preferences.getLastSubscriberOpenPath());
            if (!ld.exists() || !ld.isDirectory())
                ld = null;
        }
        if (ld == null)
        {
            ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
            if (!ld.exists() || !ld.isDirectory())
                ld = new File(context.cfg.getWorkingDirectory());
        }
        if (ld.exists() && ld.isDirectory())
            fc.setCurrentDirectory(ld);

        if (libMeta.repo.getJsonFilename().length() > 0)
        {
            File lf = new File(libMeta.repo.getJsonFilename());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }
        else if (context.preferences.getLastSubscriberOpenFile().length() > 0)
        {
            File lf = new File(context.preferences.getLastSubscriberOpenFile());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }

                textFieldCurrentPublisher.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath()));
                checkBoxPublisher.setSelected(true);
            }
            else
            {
                textFieldCurrentPublisher.setText("");
                checkBoxPublisher.setSelected(false);
            }
            break;
        }
    }

    private void buttonSubscriberClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                return (file.getName().toLowerCase().endsWith(".json"));
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("Navigator.menu.Open.publisher.files");
            }
        });
        fc.setDialogTitle(context.cfg.gs("InviteContent.select.subscriber"));
        fc.setFileHidingEnabled(false);
        File ld = null;
        if (context.preferences.getLastPublisherOpenPath().length() > 0)
        {
            ld = new File(context.preferences.getLastPublisherOpenPath());
            if (!ld.exists() || !ld.isDirectory())
                ld = null;
        }
        if (ld == null)
        {
            ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
            if (!ld.exists() || !ld.isDirectory())
                ld = new File(context.cfg.getWorkingDirectory());
        }
        if (ld.exists() && ld.isDirectory())
            fc.setCurrentDirectory(ld);

        if (context.preferences.getLastPublisherOpenFile().length() > 0)
        {
            File lf = new File(context.preferences.getLastPublisherOpenFile());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }

                textFieldCurrentSubscriber.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath()));
                checkBoxSubscriber.setSelected(true);
            }
            else
            {
                textFieldCurrentSubscriber.setText("");
                checkBoxSubscriber.setSelected(false);
            }
            break;
        }
    }

    private void buttonHintServerClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                return (file.getName().toLowerCase().endsWith(".json"));
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("Navigator.menu.Open.publisher.files");
            }
        });
        fc.setDialogTitle(context.cfg.gs("InviteContent.select.hintServer"));
        fc.setFileHidingEnabled(false);
        File ld = null;
        if (context.preferences.getLastHintTrackingOpenPath().length() > 0)
        {
            ld = new File(context.preferences.getLastHintTrackingOpenPath());
            if (!ld.exists() || !ld.isDirectory())
                ld = null;
        }
        if (ld == null)
        {
            ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "libraries");
            if (!ld.exists() || !ld.isDirectory())
                ld = new File(context.cfg.getWorkingDirectory());
        }
        if (ld.exists() && ld.isDirectory())
            fc.setCurrentDirectory(ld);

        if (context.preferences.getLastHintTrackingOpenFile().length() > 0)
        {
            File lf = new File(context.preferences.getLastHintTrackingOpenFile());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }

                textFieldCurrentHintServer.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath()));
                checkBoxHintServer.setSelected(true);
            }
            else
            {
                textFieldCurrentHintServer.setText("");
                checkBoxHintServer.setSelected(false);
            }
            break;
        }
    }

    private void buttonHintsClicked(ActionEvent e)
    {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileFilter()
        {
            @Override
            public boolean accept(File file)
            {
                if (file.isDirectory())
                    return true;
                return (file.getName().toLowerCase().endsWith(".keys"));
            }

            @Override
            public String getDescription()
            {
                return context.cfg.gs("Navigator.menu.Open.hint.keys.files");
            }
        });
        fc.setDialogTitle(context.cfg.gs("InviteContent.select.hint.keys"));
        fc.setFileHidingEnabled(false);
        File ld = null;
        if (context.preferences.getLastHintKeysOpenPath().length() > 0)
        {
            ld = new File(context.preferences.getLastHintKeysOpenPath());
            if (!ld.exists() || !ld.isDirectory())
                ld = null;
        }
        if (ld == null)
        {
            ld = new File(context.cfg.getWorkingDirectory() + System.getProperty("file.separator") + "system");
            if (!ld.exists() || !ld.isDirectory())
                ld = new File(context.cfg.getWorkingDirectory());
        }
        if (ld.exists() && ld.isDirectory())
            fc.setCurrentDirectory(ld);

        if (context.preferences.getLastHintKeysOpenFile().length() > 0)
        {
            File lf = new File(context.preferences.getLastHintKeysOpenFile());
            if (lf.exists())
                fc.setSelectedFile(lf);
        }

        while (true)
        {
            int selection = fc.showOpenDialog(context.mainFrame);
            if (selection == JFileChooser.APPROVE_OPTION)
            {
                File file = fc.getSelectedFile();
                if (!file.exists())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.file.not.found") + file.getName(),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }
                if (file.isDirectory())
                {
                    JOptionPane.showMessageDialog(context.mainFrame,
                            context.cfg.gs("Navigator.open.error.select.a.file.only"),
                            context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                    break;
                }

                textFieldCurrentHints.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), file.getPath()));
                checkBoxHints.setSelected(true);
            }
            else
            {
                textFieldCurrentHints.setText("");
                checkBoxHints.setSelected(false);
            }
            break;
        }
    }

    private void setDialog()
    {
        if (libMeta.repo != null)
        {
            textFieldCurrentPublisher.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectorySubscriber(), libMeta.repo.getJsonFilename()));
            checkBoxPublisher.setSelected(true);
        }
        else
        {
            checkBoxPublisher.setSelected(false);
        }

        if (context.publisherRepo != null && !context.publisherRepo.getJsonFilename().equals(libMeta.repo.getJsonFilename()))
        {
            textFieldCurrentSubscriber.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), context.publisherRepo.getJsonFilename()));
            checkBoxSubscriber.setSelected(true);
        }
        else
        {
            checkBoxSubscriber.setSelected(false);
        }

        if (context.hintsRepo != null)
        {
            textFieldCurrentHintServer.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), context.hintsRepo.getJsonFilename()));
            checkBoxHintServer.setSelected(true);
        }
        else
        {
            checkBoxHintServer.setSelected(false);
        }

        if (context.hintKeys != null)
        {
            textFieldCurrentHints.setText(Utils.makeRelativePath(context.cfg.getWorkingDirectory(), context.cfg.getHintKeysFile()));
            checkBoxHints.setSelected(true);
        }
        else
        {
            checkBoxHintServer.setSelected(false);
        }
    }

    // ================================================================================================================

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        panelOptions = new JPanel();
        labelPub = new JLabel();
        checkBoxPublisher = new JCheckBox();
        textFieldCurrentPublisher = new JTextField();
        buttonPublisher = new JButton();
        labelSub = new JLabel();
        checkBoxSubscriber = new JCheckBox();
        textFieldCurrentSubscriber = new JTextField();
        buttonSubscriber = new JButton();
        labelHintServer = new JLabel();
        checkBoxHintServer = new JCheckBox();
        textFieldCurrentHintServer = new JTextField();
        buttonHintServer = new JButton();
        labelHints = new JLabel();
        checkBoxHints = new JCheckBox();
        textFieldCurrentHints = new JTextField();
        buttonHints = new JButton();
        vSpacer1 = new JPanel(null);
        labelChoose = new JLabel();
        textFieldChoose = new JTextField();
        buttonChoose = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("InviteContentUI.this.title"));
        setModal(true);
        setResizable(false);
        setName("dialog1");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelOptions ========
                {
                    panelOptions.setAlignmentY(1.0F);
                    panelOptions.setLayout(new GridBagLayout());
                    ((GridBagLayout)panelOptions.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0E-4};
                    ((GridBagLayout)panelOptions.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                    //---- labelPub ----
                    labelPub.setText(context.cfg.gs("InviteContentUI.labelPub.text"));
                    panelOptions.add(labelPub, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 8, 4, 4), 0, 0));

                    //---- checkBoxPublisher ----
                    checkBoxPublisher.setToolTipText(context.cfg.gs("InviteContentUI.checkBoxPublisher.toolTipText"));
                    panelOptions.add(checkBoxPublisher, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- textFieldCurrentPublisher ----
                    textFieldCurrentPublisher.setEditable(false);
                    textFieldCurrentPublisher.setPreferredSize(new Dimension(240, 34));
                    textFieldCurrentPublisher.setMinimumSize(new Dimension(240, 34));
                    textFieldCurrentPublisher.setToolTipText(context.cfg.gs("InviteContentUI.checkBoxPublisher.toolTipText"));
                    panelOptions.add(textFieldCurrentPublisher, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- buttonPublisher ----
                    buttonPublisher.setText(context.cfg.gs("Z.ellipsis"));
                    buttonPublisher.setPreferredSize(new Dimension(30, 30));
                    buttonPublisher.setMinimumSize(new Dimension(30, 30));
                    buttonPublisher.setMaximumSize(new Dimension(30, 30));
                    buttonPublisher.addActionListener(e -> buttonPublisherClicked(e));
                    panelOptions.add(buttonPublisher, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 0, 6, 8), 0, 0));

                    //---- labelSub ----
                    labelSub.setText(context.cfg.gs("InviteContentUI.labelSub.text"));
                    panelOptions.add(labelSub, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 8, 4, 4), 0, 0));

                    //---- checkBoxSubscriber ----
                    checkBoxSubscriber.setToolTipText(context.cfg.gs("InviteContentUI.checkBoxSubscriber.toolTipText"));
                    panelOptions.add(checkBoxSubscriber, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- textFieldCurrentSubscriber ----
                    textFieldCurrentSubscriber.setEditable(false);
                    textFieldCurrentSubscriber.setPreferredSize(new Dimension(240, 34));
                    textFieldCurrentSubscriber.setMinimumSize(new Dimension(240, 34));
                    textFieldCurrentSubscriber.setToolTipText(context.cfg.gs("InviteContentUI.checkBoxSubscriber.toolTipText"));
                    panelOptions.add(textFieldCurrentSubscriber, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- buttonSubscriber ----
                    buttonSubscriber.setText(context.cfg.gs("InviteContentUI.buttonSubscriber.text"));
                    buttonSubscriber.setPreferredSize(new Dimension(30, 30));
                    buttonSubscriber.setMinimumSize(new Dimension(30, 30));
                    buttonSubscriber.setMaximumSize(new Dimension(30, 30));
                    buttonSubscriber.addActionListener(e -> buttonSubscriberClicked(e));
                    panelOptions.add(buttonSubscriber, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 0, 6, 8), 0, 0));

                    //---- labelHintServer ----
                    labelHintServer.setText(context.cfg.gs("InviteContentUI.labelHintServer.text"));
                    panelOptions.add(labelHintServer, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 8, 4, 4), 0, 0));

                    //---- checkBoxHintServer ----
                    checkBoxHintServer.setToolTipText(context.cfg.gs("InviteUI.only.add.to.hint.keys.if.performing.automated.back.ups"));
                    panelOptions.add(checkBoxHintServer, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- textFieldCurrentHintServer ----
                    textFieldCurrentHintServer.setEditable(false);
                    textFieldCurrentHintServer.setMinimumSize(new Dimension(240, 34));
                    textFieldCurrentHintServer.setPreferredSize(new Dimension(240, 34));
                    panelOptions.add(textFieldCurrentHintServer, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- buttonHintServer ----
                    buttonHintServer.setText(context.cfg.gs("InviteContentUI.buttonHintServer.text"));
                    buttonHintServer.setPreferredSize(new Dimension(30, 30));
                    buttonHintServer.setMinimumSize(new Dimension(30, 30));
                    buttonHintServer.setMaximumSize(new Dimension(30, 30));
                    buttonHintServer.addActionListener(e -> buttonHintServerClicked(e));
                    panelOptions.add(buttonHintServer, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 0, 6, 8), 0, 0));

                    //---- labelHints ----
                    labelHints.setText(context.cfg.gs("InviteContentUI.labelHints.text"));
                    panelOptions.add(labelHints, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 8, 4, 4), 0, 0));

                    //---- checkBoxHints ----
                    checkBoxHints.setToolTipText(context.cfg.gs("InviteUI.only.add.to.hint.keys.if.performing.automated.back.ups"));
                    panelOptions.add(checkBoxHints, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- textFieldCurrentHints ----
                    textFieldCurrentHints.setEditable(false);
                    textFieldCurrentHints.setMinimumSize(new Dimension(240, 34));
                    textFieldCurrentHints.setPreferredSize(new Dimension(240, 34));
                    panelOptions.add(textFieldCurrentHints, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- buttonHints ----
                    buttonHints.setText(context.cfg.gs("InviteContentUI.buttonHints.text"));
                    buttonHints.setPreferredSize(new Dimension(30, 30));
                    buttonHints.setMinimumSize(new Dimension(30, 30));
                    buttonHints.setMaximumSize(new Dimension(30, 30));
                    buttonHints.addActionListener(e -> buttonHintsClicked(e));
                    panelOptions.add(buttonHints, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 0, 6, 8), 0, 0));

                    //---- vSpacer1 ----
                    vSpacer1.setMinimumSize(new Dimension(10, 11));
                    vSpacer1.setPreferredSize(new Dimension(10, 11));
                    panelOptions.add(vSpacer1, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- labelChoose ----
                    labelChoose.setText(context.cfg.gs("InviteContentUI.labelChoose.text"));
                    panelOptions.add(labelChoose, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 4), 0, 0));

                    //---- textFieldChoose ----
                    textFieldChoose.setEditable(false);
                    textFieldChoose.setToolTipText(context.cfg.gs("InviteContentUI.textFieldChoose.toolTipText"));
                    panelOptions.add(textFieldChoose, new GridBagConstraints(2, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 4), 0, 0));

                    //---- buttonChoose ----
                    buttonChoose.setText(context.cfg.gs("InviteContentUI.buttonChoose.text"));
                    buttonChoose.setPreferredSize(new Dimension(30, 30));
                    buttonChoose.setMinimumSize(new Dimension(30, 30));
                    buttonChoose.setMaximumSize(new Dimension(30, 30));
                    buttonChoose.addActionListener(e -> buttonChooseClicked(e));
                    panelOptions.add(buttonChoose, new GridBagConstraints(3, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(2, 0, 2, 8), 0, 0));
                }
                contentPanel.add(panelOptions, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(e -> actionCancelClicked(e));
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

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel panelOptions;
    private JLabel labelPub;
    public JCheckBox checkBoxPublisher;
    public JTextField textFieldCurrentPublisher;
    private JButton buttonPublisher;
    private JLabel labelSub;
    public JCheckBox checkBoxSubscriber;
    public JTextField textFieldCurrentSubscriber;
    private JButton buttonSubscriber;
    private JLabel labelHintServer;
    public JCheckBox checkBoxHintServer;
    public JTextField textFieldCurrentHintServer;
    private JButton buttonHintServer;
    private JLabel labelHints;
    public JCheckBox checkBoxHints;
    public JTextField textFieldCurrentHints;
    private JButton buttonHints;
    private JPanel vSpacer1;
    private JLabel labelChoose;
    public JTextField textFieldChoose;
    private JButton buttonChoose;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
