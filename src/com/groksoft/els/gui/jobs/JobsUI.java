package com.groksoft.els.gui.jobs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.groksoft.els.MungeException;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.GuiContext;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.gui.browser.NavTreeUserObject;
import com.groksoft.els.tools.junkremover.JunkRemoverTool;
import com.groksoft.els.gui.util.RotatedIcon;
import com.groksoft.els.gui.util.TextIcon;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.jobs.Origin;
import com.groksoft.els.jobs.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class JobsUI extends JDialog
{
    private boolean active = false;
    private ArrayList<Job> deletedJobs;
    private GuiContext guiContext;
    private Logger logger = LogManager.getLogger("applog");
    private NavHelp helpDialog;
    private ArrayList<Job> jobs;
    private boolean isDryRun;
    private DefaultListModel<Job> listModel;
    private boolean somethingChanged = false;
    private SwingWorker<Void, Void>  worker;
    private boolean workerRunning = false;

    public JobsUI(Window owner, GuiContext guiContext)
    {
        super(owner);
        this.guiContext = guiContext;

        initComponents();

        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // Rotate up/down button labels
        // http://www.camick.com/java/source/TextIcon.java
        TextIcon t1 = new TextIcon(buttonToolUp, ">", TextIcon.Layout.HORIZONTAL);
        buttonToolUp.setText("");
        // http://www.camick.com/java/source/RotatedIcon.java
        RotatedIcon r1 = new RotatedIcon(t1, RotatedIcon.Rotate.UP);
        buttonToolUp.setIcon(r1);
        //
        t1 = new TextIcon(buttonToolDown, ">", TextIcon.Layout.HORIZONTAL);
        buttonToolDown.setText("");
        r1 = new RotatedIcon(t1, RotatedIcon.Rotate.DOWN);
        buttonToolDown.setIcon(r1);

        listModel = new DefaultListModel<Job>();
        listItems.setModel(listModel);
        loadConfigurations();

    }

    private void actionCancelClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void actionCopyClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void actionDeleteClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void actionHelpClicked(MouseEvent e)
    {
        // TODO add your code here
    }

    private void actionNewClicked(ActionEvent evt)
    {

        // LEFTOFF
        //  pub / sub :: based on tool dualRepositories
        //    * if not dualRepositories add Current Publisher, Current Subscriber -divider- list of repo descriptions
        //    * if dualRepositories add pub to top sub to bottom - divider- list of repo descriptions
        //  lib include / exclude
        //    * Origin Add button has dialog with exclude chechbox and list of libs from top combo-box repo
        //  directories
        //  files
        //  remote

        // IDEA
        //  * Change Origin to 1 or 2 tabs if dualRepositories is enabled
        //      + The Add button adds

        // IDEA
        //   * Panel with:
        //      + Combo:
        //          + ????????? Loaded now or whatever is loaded - how to describe???????
        //      + origins
        //    * If dualRepositories two combo boxes
        //       + Items for Current Publisher, Current Subscriber, or Select ... with file open???????????????
        //  * ...
        //  * Erg, humnph. Working on this part.

//        if (!listItemExists(null, guiContext.cfg.gs("Z.untitled")))
        if (true)
        {
//            Job job = new Job(guiContext.cfg.gs("Z.untitled"));
//            job.setDataHasChanged(true);
//            textFieldJobName.setToolTipText(job.getConfigName());

            // FIXME HACK *****************************************************************
            Job job = new Job("Pre-Publish tasks"); //guiContext.cfg.gs("Z.untitled"));
            job.setDataHasChanged(true);
//            job.setSubscriber(comboBoxPubSub.getItemAt(comboBoxPubSub.getSelectedIndex()).equalsIgnoreCase("subscriber"));
            textFieldJobName.setText(job.getConfigName());

            JunkRemoverTool jrt = new JunkRemoverTool(guiContext);
            Task task = new Task(jrt.getInternalName(), "Plex junk");

            ArrayList<Origin> origins = new ArrayList<Origin>();

            Object object = guiContext.browser.lastComponent;
            if (object instanceof JTable)
            {
                JTable sourceTable = (JTable) object;
                int row = sourceTable.getSelectedRow();
                if (row > -1)
                {
                    int[] rows = sourceTable.getSelectedRows();
                    for (int i = 0; i < rows.length; ++i)
                    {
                        NavTreeUserObject tuo = (NavTreeUserObject) sourceTable.getValueAt(rows[i], 1);
                        Origin origin = new Origin(tuo);
                        origins.add(origin);
                    }
                }
                task.setPublisherKey(guiContext.context.publisherRepo.getLibraryData().libraries.key);
                task.setOrigins(origins);

                ArrayList<Task> tasks = new ArrayList<Task>();
                tasks.add(task);
                job.setTasks(tasks);

                try
                {
                    write(job);
                }
                catch (Exception e)
                {

                }

            }
        }
        else
        {
            JOptionPane.showMessageDialog(this, guiContext.cfg.gs("JunkRemover.please.rename.the.existing") +
                    guiContext.cfg.gs("Z.untitled"), guiContext.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
            textFieldJobName.requestFocus();
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        setVisible(false);
    }

    private void actionRunClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    public String getDirectoryPath()
    {
        String path = System.getProperty("user.home") + System.getProperty("file.separator") +
                ".els" + System.getProperty("file.separator") +
                "jobs";
        return path;
    }

    public String getFullPath(Job job)
    {
        String path = getDirectoryPath() + System.getProperty("file.separator")+
                Utils.scrubFilename(job.getConfigName()) + ".json";
        return path;
    }

    private boolean listItemExists(Job job, String configName)
    {
        boolean exists = false;
        for (int i = 0; i < listModel.getSize(); ++i)
        {
            if (listModel.getElementAt(i).getConfigName().equalsIgnoreCase(configName))
            {
                if (job == null || job != listModel.getElementAt(i))
                {
                    exists = true;
                    break;
                }
            }
        }
        return exists;
    }

    private void listItemsMouseClicked(MouseEvent e)
    {
        // TODO add your code here
    }

    private void listItemsValueChanged(ListSelectionEvent e)
    {
        // TODO add your code here
    }

    private void loadConfigurations()
    {
        File jobsDir = new File(getDirectoryPath());
        if (jobsDir.exists())
        {
            File[] files = FileSystemView.getFileSystemView().getFiles(jobsDir, false);
            Arrays.sort(files);
            for (File entry : files)
            {
                if (!entry.isDirectory())
                {
                    try
                    {
                        Gson gson = new Gson();
                        String json = new String(Files.readAllBytes(Paths.get(entry.getAbsolutePath())));
                        if (json != null)
                        {
                            Job job = gson.fromJson(json, Job.class);
                            if (job != null)
                            {
                                listModel.addElement(job);
                            }
                        }
                    }
                    catch (IOException e)
                    {
                        JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.error.reading") + entry.getName(),
                                guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                    }
                    catch (JsonSyntaxException e)
                    {
                        JOptionPane.showMessageDialog(this, guiContext.cfg.gs("Z.error.parsing") + entry.getName(),
                                guiContext.cfg.gs("JobsUI.title"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }
        if (listModel.getSize() == 0)
        {
            buttonCopy.setEnabled(false);
            buttonDelete.setEnabled(false);
            buttonRun.setEnabled(false);
            buttonToolUp.setEnabled(false);
            buttonToolDown.setEnabled(false);
            buttonAddTool.setEnabled(false);
            buttonRemoveTool.setEnabled(false);
            buttonAddOrigin.setEnabled(false);
            buttonRemoveOrigin.setEnabled(false);
            textFieldJobName.setEnabled(false);
        }
        else
        {
            //TODO loadTable(0);
            listItems.requestFocus();
            listItems.setSelectedIndex(0);
        }
    }

    private void stopEditing()
    {
        if (!listItemExists(null, guiContext.cfg.gs("Z.untitled")))
        {
        }
    }

    private void textFieldNameChanged(ActionEvent e)
    {
        // TODO add your code here
    }

    private void textFieldNameFocusLost(FocusEvent e)
    {
        // TODO add your code here
    }

    private void addRowClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    private void removeRowClicked(ActionEvent e)
    {
        // TODO add your code here
    }

    public void write(Job job) throws Exception
    {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(job);
        try
        {
            File f = new File(getFullPath(job));
            if (f != null)
            {
                f.getParentFile().mkdirs();
            }
            PrintWriter outputStream = new PrintWriter(getFullPath(job));
            outputStream.println(json);
            outputStream.close();
        }
        catch (FileNotFoundException fnf)
        {
            throw new MungeException(guiContext.cfg.gs("Z.error.writing") + getFullPath(job) + ": " + Utils.getStackTrace(fnf));
        }
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off

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
        hSpacerBeforeGenerate = new JPanel(null);
        buttonGenerate = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        splitPaneContent = new JSplitPane();
        scrollPaneList = new JScrollPane();
        listItems = new JList();
        panelJob = new JPanel();
        textFieldJobName = new JTextField();
        splitPaneToolsOrigin = new JSplitPane();
        panelTools = new JPanel();
        labelTools = new JLabel();
        scrollPaneTools = new JScrollPane();
        listTools = new JList();
        panelOrigin = new JPanel();
        labelOrigin = new JLabel();
        panelOriginInstance = new JPanel();
        panelPubSub = new JPanel();
        comboBoxPubSub1 = new JComboBox();
        comboBoxPubSub2 = new JComboBox();
        scrollPaneOrigins = new JScrollPane();
        listOrigins = new JList();
        panelOriginsButtons = new JPanel();
        buttonAddOrigin = new JButton();
        buttonRemoveOrigin = new JButton();
        panelToolButtons = new JPanel();
        buttonToolUp = new JButton();
        buttonToolDown = new JButton();
        buttonAddTool = new JButton();
        buttonRemoveTool = new JButton();
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
                contentPanel.setLayout(new BorderLayout());

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
                        buttonNew.addActionListener(e -> actionNewClicked(e));
                        panelTopButtons.add(buttonNew);

                        //---- buttonCopy ----
                        buttonCopy.setText(guiContext.cfg.gs("JobsUI.buttonCopy.text"));
                        buttonCopy.setMnemonic(guiContext.cfg.gs("JobsUI.buttonCopy.mnemonic").charAt(0));
                        buttonCopy.setToolTipText(guiContext.cfg.gs("JobsUI.buttonCopy.toolTipText"));
                        buttonCopy.addActionListener(e -> actionCopyClicked(e));
                        panelTopButtons.add(buttonCopy);

                        //---- buttonDelete ----
                        buttonDelete.setText(guiContext.cfg.gs("JobsUI.buttonDelete.text"));
                        buttonDelete.setMnemonic(guiContext.cfg.gs("JobsUI.buttonDelete.mnemonic").charAt(0));
                        buttonDelete.setToolTipText(guiContext.cfg.gs("JobsUI.buttonDelete.toolTipText"));
                        buttonDelete.addActionListener(e -> actionDeleteClicked(e));
                        panelTopButtons.add(buttonDelete);

                        //---- hSpacerBeforeRun ----
                        hSpacerBeforeRun.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeRun.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeRun);

                        //---- buttonRun ----
                        buttonRun.setText(guiContext.cfg.gs("JobsUI.buttonRun.text"));
                        buttonRun.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRun.mnemonic").charAt(0));
                        buttonRun.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRun.toolTipText"));
                        buttonRun.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonRun);

                        //---- hSpacerBeforeGenerate ----
                        hSpacerBeforeGenerate.setMinimumSize(new Dimension(22, 6));
                        hSpacerBeforeGenerate.setPreferredSize(new Dimension(22, 6));
                        panelTopButtons.add(hSpacerBeforeGenerate);

                        //---- buttonGenerate ----
                        buttonGenerate.setText(guiContext.cfg.gs("JobsUI.buttonGenerate.text"));
                        buttonGenerate.setMnemonic(guiContext.cfg.gs("JobsUI.buttonGenerate.mnemonic_2").charAt(0));
                        buttonGenerate.setToolTipText(guiContext.cfg.gs("JobsUI.buttonGenerate.toolTipText"));
                        buttonGenerate.addActionListener(e -> actionRunClicked(e));
                        panelTopButtons.add(buttonGenerate);
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
                                actionHelpClicked(e);
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

                    //======== panelJob ========
                    {
                        panelJob.setLayout(new BorderLayout());

                        //---- textFieldJobName ----
                        textFieldJobName.setPreferredSize(new Dimension(150, 30));
                        textFieldJobName.addActionListener(e -> textFieldNameChanged(e));
                        textFieldJobName.addFocusListener(new FocusAdapter() {
                            @Override
                            public void focusLost(FocusEvent e) {
                                textFieldNameFocusLost(e);
                            }
                        });
                        panelJob.add(textFieldJobName, BorderLayout.NORTH);

                        //======== splitPaneToolsOrigin ========
                        {
                            splitPaneToolsOrigin.setDividerLocation(142);
                            splitPaneToolsOrigin.setLastDividerLocation(142);

                            //======== panelTools ========
                            {
                                panelTools.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelTools.getLayout()).columnWidths = new int[] {0, 0};
                                ((GridBagLayout)panelTools.getLayout()).rowHeights = new int[] {0, 0, 0};
                                ((GridBagLayout)panelTools.getLayout()).columnWeights = new double[] {1.0, 1.0E-4};
                                ((GridBagLayout)panelTools.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                //---- labelTools ----
                                labelTools.setText(guiContext.cfg.gs("JobsUI.labelTools.text"));
                                labelTools.setHorizontalAlignment(SwingConstants.LEFT);
                                labelTools.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelTools.setFont(labelTools.getFont().deriveFont(labelTools.getFont().getSize() + 1f));
                                labelTools.setMaximumSize(new Dimension(37, 18));
                                labelTools.setMinimumSize(new Dimension(37, 18));
                                labelTools.setPreferredSize(new Dimension(37, 18));
                                panelTools.add(labelTools, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 0), 0, 0));

                                //======== scrollPaneTools ========
                                {
                                    scrollPaneTools.setViewportView(listTools);
                                }
                                panelTools.add(scrollPaneTools, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            splitPaneToolsOrigin.setLeftComponent(panelTools);

                            //======== panelOrigin ========
                            {
                                panelOrigin.setLayout(new GridBagLayout());
                                ((GridBagLayout)panelOrigin.getLayout()).columnWidths = new int[] {0, 0};
                                ((GridBagLayout)panelOrigin.getLayout()).rowHeights = new int[] {0, 0, 0};
                                ((GridBagLayout)panelOrigin.getLayout()).columnWeights = new double[] {0.0, 1.0E-4};
                                ((GridBagLayout)panelOrigin.getLayout()).rowWeights = new double[] {0.0, 0.0, 1.0E-4};

                                //---- labelOrigin ----
                                labelOrigin.setText(guiContext.cfg.gs("JobsUI.labelOrigin.text"));
                                labelOrigin.setFont(labelOrigin.getFont().deriveFont(labelOrigin.getFont().getSize() + 1f));
                                labelOrigin.setHorizontalAlignment(SwingConstants.LEFT);
                                labelOrigin.setHorizontalTextPosition(SwingConstants.LEFT);
                                labelOrigin.setMaximumSize(new Dimension(57, 18));
                                labelOrigin.setMinimumSize(new Dimension(57, 18));
                                labelOrigin.setPreferredSize(new Dimension(57, 18));
                                panelOrigin.add(labelOrigin, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 4, 0, 0), 0, 0));

                                //======== panelOriginInstance ========
                                {
                                    panelOriginInstance.setBorder(null);
                                    panelOriginInstance.setLayout(new BorderLayout());

                                    //======== panelPubSub ========
                                    {
                                        panelPubSub.setLayout(new BoxLayout(panelPubSub, BoxLayout.Y_AXIS));

                                        //---- comboBoxPubSub1 ----
                                        comboBoxPubSub1.setSelectedIndex(-1);
                                        panelPubSub.add(comboBoxPubSub1);

                                        //---- comboBoxPubSub2 ----
                                        comboBoxPubSub2.setSelectedIndex(-1);
                                        panelPubSub.add(comboBoxPubSub2);
                                    }
                                    panelOriginInstance.add(panelPubSub, BorderLayout.NORTH);

                                    //======== scrollPaneOrigins ========
                                    {
                                        scrollPaneOrigins.setViewportView(listOrigins);
                                    }
                                    panelOriginInstance.add(scrollPaneOrigins, BorderLayout.CENTER);

                                    //======== panelOriginsButtons ========
                                    {
                                        panelOriginsButtons.setBorder(null);
                                        panelOriginsButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                                        //---- buttonAddOrigin ----
                                        buttonAddOrigin.setText(guiContext.cfg.gs("JobsUI.buttonAddOrigin.text"));
                                        buttonAddOrigin.setFont(buttonAddOrigin.getFont().deriveFont(buttonAddOrigin.getFont().getSize() - 2f));
                                        buttonAddOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonAddOrigin.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddOrigin.mnemonic").charAt(0));
                                        buttonAddOrigin.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddOrigin.toolTipText"));
                                        buttonAddOrigin.addActionListener(e -> addRowClicked(e));
                                        panelOriginsButtons.add(buttonAddOrigin);

                                        //---- buttonRemoveOrigin ----
                                        buttonRemoveOrigin.setText(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.text"));
                                        buttonRemoveOrigin.setFont(buttonRemoveOrigin.getFont().deriveFont(buttonRemoveOrigin.getFont().getSize() - 2f));
                                        buttonRemoveOrigin.setPreferredSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMinimumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMaximumSize(new Dimension(78, 24));
                                        buttonRemoveOrigin.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.mnemonic").charAt(0));
                                        buttonRemoveOrigin.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveOrigin.toolTipText"));
                                        buttonRemoveOrigin.addActionListener(e -> removeRowClicked(e));
                                        panelOriginsButtons.add(buttonRemoveOrigin);
                                    }
                                    panelOriginInstance.add(panelOriginsButtons, BorderLayout.SOUTH);
                                }
                                panelOrigin.add(panelOriginInstance, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0,
                                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                    new Insets(0, 0, 0, 0), 0, 0));
                            }
                            splitPaneToolsOrigin.setRightComponent(panelOrigin);
                        }
                        panelJob.add(splitPaneToolsOrigin, BorderLayout.CENTER);

                        //======== panelToolButtons ========
                        {
                            panelToolButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                            //---- buttonToolUp ----
                            buttonToolUp.setText("^");
                            buttonToolUp.setMaximumSize(new Dimension(24, 24));
                            buttonToolUp.setMinimumSize(new Dimension(24, 24));
                            buttonToolUp.setPreferredSize(new Dimension(24, 24));
                            buttonToolUp.setFont(buttonToolUp.getFont().deriveFont(buttonToolUp.getFont().getSize() - 2f));
                            buttonToolUp.setToolTipText(guiContext.cfg.gs("JobsUI.buttonToolUp.toolTipText"));
                            panelToolButtons.add(buttonToolUp);

                            //---- buttonToolDown ----
                            buttonToolDown.setText("v");
                            buttonToolDown.setFont(buttonToolDown.getFont().deriveFont(buttonToolDown.getFont().getSize() - 2f));
                            buttonToolDown.setMaximumSize(new Dimension(24, 24));
                            buttonToolDown.setMinimumSize(new Dimension(24, 24));
                            buttonToolDown.setPreferredSize(new Dimension(24, 24));
                            buttonToolDown.setToolTipText(guiContext.cfg.gs("JobsUI.buttonToolDown.toolTipText"));
                            panelToolButtons.add(buttonToolDown);

                            //---- buttonAddTool ----
                            buttonAddTool.setText(guiContext.cfg.gs("JobsUI.buttonAddTool.text"));
                            buttonAddTool.setFont(buttonAddTool.getFont().deriveFont(buttonAddTool.getFont().getSize() - 2f));
                            buttonAddTool.setPreferredSize(new Dimension(78, 24));
                            buttonAddTool.setMinimumSize(new Dimension(78, 24));
                            buttonAddTool.setMaximumSize(new Dimension(78, 24));
                            buttonAddTool.setMnemonic(guiContext.cfg.gs("JobsUI.buttonAddTool.mnemonic").charAt(0));
                            buttonAddTool.setToolTipText(guiContext.cfg.gs("JobsUI.buttonAddTool.toolTipText"));
                            buttonAddTool.addActionListener(e -> addRowClicked(e));
                            panelToolButtons.add(buttonAddTool);

                            //---- buttonRemoveTool ----
                            buttonRemoveTool.setText(guiContext.cfg.gs("JobsUI.buttonRemoveTool.text"));
                            buttonRemoveTool.setFont(buttonRemoveTool.getFont().deriveFont(buttonRemoveTool.getFont().getSize() - 2f));
                            buttonRemoveTool.setPreferredSize(new Dimension(78, 24));
                            buttonRemoveTool.setMinimumSize(new Dimension(78, 24));
                            buttonRemoveTool.setMaximumSize(new Dimension(78, 24));
                            buttonRemoveTool.setMnemonic(guiContext.cfg.gs("JobsUI.buttonRemoveTool.mnemonic").charAt(0));
                            buttonRemoveTool.setToolTipText(guiContext.cfg.gs("JobsUI.buttonRemoveTool.toolTipText"));
                            buttonRemoveTool.addActionListener(e -> removeRowClicked(e));
                            panelToolButtons.add(buttonRemoveTool);
                        }
                        panelJob.add(panelToolButtons, BorderLayout.SOUTH);
                    }
                    splitPaneContent.setRightComponent(panelJob);
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
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 2), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(guiContext.cfg.gs("JobsUI.cancelButton.text"));
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
    private JPanel hSpacerBeforeGenerate;
    private JButton buttonGenerate;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JSplitPane splitPaneContent;
    private JScrollPane scrollPaneList;
    private JList listItems;
    private JPanel panelJob;
    private JTextField textFieldJobName;
    private JSplitPane splitPaneToolsOrigin;
    private JPanel panelTools;
    private JLabel labelTools;
    private JScrollPane scrollPaneTools;
    private JList listTools;
    private JPanel panelOrigin;
    private JLabel labelOrigin;
    private JPanel panelOriginInstance;
    private JPanel panelPubSub;
    private JComboBox comboBoxPubSub1;
    private JComboBox comboBoxPubSub2;
    private JScrollPane scrollPaneOrigins;
    private JList listOrigins;
    private JPanel panelOriginsButtons;
    private JButton buttonAddOrigin;
    private JButton buttonRemoveOrigin;
    private JPanel panelToolButtons;
    private JButton buttonToolUp;
    private JButton buttonToolDown;
    private JButton buttonAddTool;
    private JButton buttonRemoveTool;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    //
    // @formatter:on
    // </editor-fold>

}
