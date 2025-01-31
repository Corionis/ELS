package com.corionis.els.gui;

import com.corionis.els.Configuration;
import com.corionis.els.jobs.Job;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import mslinks.ShellLink;
import mslinks.ShellLinkHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generate command line dialog
 * <br/> <br/>
 * See also: <br/>
 *      Configuration.generateCurrentCommandline() <br/>
 *      Job.generateCommandline() <br/>
 *      OperationsTool.generateCommandLine()
 */
@SuppressWarnings(value = "unchecked")
public class Generator
{
    private String consoleLevel = "";
    private Context context;
    private String debugLevel = "";
    private boolean dryRun = false;
    private String generated = "";
    private boolean fileGenerate = false;
    private File logFile = null;
    private Logger logger = LogManager.getLogger("applog");

    private Generator()
    {
    }

    public Generator(Context context, boolean isFileGenerate)
    {
        this.context = context;
        this.fileGenerate = isFileGenerate;
    }

    private void createDesktopShortcut(JDialog owner, String name, String commandLine)
    {
        JPanel panelName = new JPanel();
        panelName.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel labelName = new JLabel();
        JTextField fieldName = new JTextField();

        JPanel panelComment = new JPanel();
        panelComment.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel labelComment = new JLabel();
        JTextField fieldComment = new JTextField();

        JPanel panelTerminal = new JPanel();
        panelTerminal.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel labelTerminal = new JLabel();
        JCheckBox checkboxTerminal = new JCheckBox();

        JPanel panelWarning = new JPanel();
        panelTerminal.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JLabel labelWarning = new JLabel();

        // setup name panel
        labelName.setText(context.cfg.gs("Generator.shortcut.name"));
        labelName.setPreferredSize(new Dimension(100, 16));
        panelName.add(labelName);
        fieldName.setText(name);
        fieldName.setPreferredSize(new Dimension(200, 30));
        panelName.add(fieldName);

        // setup comment panel
        labelComment.setText(context.cfg.gs("Generator.shortcut.comment"));
        labelComment.setPreferredSize(new Dimension(100, 16));
        panelComment.add(labelComment);
        fieldComment.setText(context.cfg.gs("Generator.launch.els") + name);
        fieldComment.setPreferredSize(new Dimension(200, 30));
        panelComment.add(fieldComment);

        // setup terminal panel
        labelTerminal.setText(context.cfg.gs("Generator.shortcut.launch.in.terminal"));
        panelTerminal.add(labelTerminal);
        panelTerminal.add(checkboxTerminal);

        // warning panel
        if (commandLine.length() > 260 && Utils.getOS().toLowerCase().equals("windows"))
        {
            labelWarning.setText(context.cfg.gs(("Generator.warning.command.line.may.be.too.long")));
            panelWarning.add(labelWarning);
        }

        final JDialog dialog = new JDialog(owner);
        dialog.setAlwaysOnTop(true);
//        if (owner == null)
//            dialog.setLocation(Utils.getRelativePosition(localContext.mainFrame, dialog));
//        else
//            dialog.setLocationRelativeTo(owner);
        Object[] params = {panelName, panelComment, panelTerminal, panelWarning};
        int resp = JOptionPane.showConfirmDialog((owner == null) ? context.mainFrame.panelMain : owner, params, context.cfg.gs("Generator.shortcut.title"), JOptionPane.OK_CANCEL_OPTION);
        if (resp == JOptionPane.OK_OPTION)
        {
            name = fieldName.getText();
            String shortcut = System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop" +
                    System.getProperty("file.separator") + name + (Utils.isOsWindows() ? ".lnk" : (Utils.isOsMac() ? "" : ".desktop"));
            File shortFile = new File(shortcut);
            boolean skip = false;

            if (name.length() == 0)
            {
                skip = true;
                JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("Generator.name.required"), context.cfg.getNavigatorName(), JOptionPane.WARNING_MESSAGE);
            }
            else
            {
                // shortcut path to user Desktop
                if (shortFile.exists())
                {
                    resp = JOptionPane.showConfirmDialog(context.mainFrame, context.cfg.gs("Generator.exists.overwrite"), context.cfg.getNavigatorName(), JOptionPane.YES_NO_OPTION);
                    if (resp != JOptionPane.YES_OPTION)
                        skip = true;
                }
            }

            if (!skip)
            {
                if (Utils.isOsMac())
                {
                    try
                    {
                        PrintWriter outputStream = new PrintWriter(shortcut);
                        outputStream.println(commandLine);
                        outputStream.close();
                        // make executable
                        shortFile.setExecutable(true);
                    }
                    catch (Exception e)
                    {
                        // error
                        System.out.println(Utils.getStackTrace(e));
                    }
                }
                else if (Utils.isOsLinux())
                {
                    /*
                        Format:
                            [Desktop Entry]
                            Name=ELS Navigator
                            Exec=java -jar ...
                            Comment=Launch ELS Navigator
                            Terminal=false
                            Icon=/home/...
                            Type=Application
                        chmod 775 "~/Desktop/ELS Navigator.desktop"
                    */

                    StringBuilder sb = new StringBuilder();
                    name = fieldName.getText();
                    if (name != null && name.length() > 0)
                    {
                        sb.append("[Desktop Entry]\n");
                        sb.append("Name=" + name + "\n");
                        sb.append("Exec=" + commandLine + "\n");
                        sb.append("Comment=" + fieldComment.getText() + "\n");
                        sb.append("Terminal=" + checkboxTerminal.isSelected() + "\n");
                        sb.append("Icon=" + context.cfg.getIconPath() + "\n");
                        sb.append("Type=Application\n");
                    }

                    try
                    {
                        PrintWriter outputStream = new PrintWriter(shortcut);
                        outputStream.println(sb);
                        outputStream.close();
                        // make executable
                        shortFile.setExecutable(true);
                    }
                    catch (Exception e)
                    {
                        // error
                        System.out.println(Utils.getStackTrace(e));
                    }
                }
                else // Windows
                {
                    // https://github.com/DmitriiShamrikov/mslinks?tab=readme-ov-file
                    try
                    {
                        name = fieldName.getText();
                        if (name != null && name.length() > 0)
                        {
                            ShellLink shellLink = new ShellLink();
                            shellLink.setWorkingDir(context.cfg.getWorkingDirectory());
                            shellLink.setIconLocation(context.cfg.getIconPath());
                            shellLink.getHeader().setIconIndex(0);

                            // remove exec name from beginning of commandLine
                            String exec = "\"" + context.cfg.getExecutablePath() + "\"";
                            commandLine = commandLine.substring(exec.length()).trim();
                            shellLink.setCMDArgs(commandLine);

                            Path target = Paths.get(context.cfg.getExecutablePath()).toAbsolutePath().normalize();
                            String root = target.getRoot().toString();
                            String pathNoRoot = target.subpath(0, target.getNameCount()).toString();

                            ShellLinkHelper helper = new ShellLinkHelper(shellLink);
                            helper.setLocalTarget(root, pathNoRoot, ShellLinkHelper.Options.ForceTypeFile);
                            helper.saveTo(shortcut);
                            logger.info(context.cfg.gs("Generator.created.shortcut") + shortcut);
                        }
                    }
                    catch (Exception e)
                    {
                        logger.error(Utils.getStackTrace(e));
                        return;
                    }

                }
            }
        }
    }

    private String generate(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwrite, String log, boolean foreground)
    {
        try
        {
            if (tool == null) // File, Generate ...
            {
                generated = context.cfg.generateCurrentCommandline(consoleLevel, debugLevel, overwrite, log);
            }
            else if (tool instanceof Job)
            {
                generated = generateJobCommandline(tool, consoleLevel, debugLevel, overwrite, log, foreground);
            }
/*
    // this has been removed from OperationsUI; But OperationsTool.generateCommandLine() is used when running a Job
            else if (tool instanceof OperationsTool)
            {
                generated = generateOperationsCommandline(tool, consoleLevel, debugLevel, overwrite, log);
            }
*/
            // TODO EXTEND+ Add other command line generators here
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        return generated;
    }

    private String generateJobCommandline(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwriteLog, String log, boolean foreground) throws Exception
    {
        // generate-commandline
        boolean glo = context.preferences.isGenerateLongOptions();
        String exec = context.cfg.getExecutablePath();
        String jar = (Utils.isOsLinux() ? context.cfg.getElsJar() : "");
        String opts = ((Job) tool).generateCommandline(dryRun);
        if (foreground)
            opts += " --logger";
        String overOpt = overwriteLog ? (glo ? "--log-overwrite" : "-F") : (glo ? "--log-file" : "-f");
        String cmd = exec + (jar.length() > 0 ? " -jar " + "\"" + jar + "\"" : "") +
                " " + opts + (glo ? " --console-level " : " -c ") + consoleLevel +
                (glo ? " --debug-level " : " -d ") + debugLevel + " " + overOpt + " \"" + log + "\"";

        return cmd;
    }

    private String getTitle(AbstractTool tool)
    {
        if (tool == null)
            return Configuration.NAVIGATOR_NAME;
        if (tool instanceof Job)
            return context.cfg.gs("JobsUI.title");
        if (tool instanceof OperationsTool)
            return context.cfg.gs("Operations.displayName");
        return "unknown";
    }

    public void showDialog(JDialog owner, AbstractTool tool, String configName)
    {
        context.mainFrame.labelStatusMiddle.setText("");
        String messasge = "<html><body>" + context.cfg.gs((fileGenerate ? "Generator.generate" : "Generator.generate.run")) +
                " <b>" + configName + "</b><br/>&nbsp;<br/></body></html>";

        // log levels panel
        JPanel panelLogLevels = new JPanel();
        panelLogLevels.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel labelConsoleLogLevels = new JLabel();
        JLabel labelDebugLogLevels = new JLabel();
        JComboBox comboBoxConsoleLevel = new JComboBox<>();
        JComboBox comboBoxDebugLevel = new JComboBox<>();

        // log file panel
        JPanel panelLogFile = new JPanel();
        panelLogFile.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel labelLogFile = new JLabel();
        JComboBox comboboxLogOverwrite = new JComboBox();
        JButton selectLogButton = new JButton();

        // dry run panel
        JPanel panelDryrun = new JPanel();
        panelDryrun.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JRadioButton foreground = new JRadioButton(context.cfg.gs("Generator.foreground"));
        JRadioButton background = new JRadioButton(context.cfg.gs("Generator.background"));

        // horizontal panels
        JPanel panelHorizontal1 = new JPanel();
        panelHorizontal1.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 2));
        JPanel panelHorizontal2 = new JPanel();
        panelHorizontal2.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 2));

        // generated panel
        JPanel panelGenerated = new JPanel();
        panelGenerated.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JTextField generatedTextField = new JTextField();

        // actions panel
        JPanel panelActions = new JPanel();
        panelActions.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton copyButton = new JButton();
        JButton shortcutButton = new JButton();

        // setup log levels panel
        labelConsoleLogLevels.setText(context.cfg.gs("Generator.labelConsoleLogLevels.text"));
        panelLogLevels.add(labelConsoleLogLevels);
        comboBoxConsoleLevel.setModel(new DefaultComboBoxModel<>(new String[]{
                "All",
                "Trace",
                "Debug",
                "Info",
                "Warn",
                "Error",
                "Fatal",
                "Off"
        }));
        comboBoxConsoleLevel.setSelectedIndex(3);
        comboBoxConsoleLevel.setToolTipText(context.cfg.gs("Generator.comboConsoleLogLevels.tooltip"));
        comboBoxConsoleLevel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String cmd = actionEvent.getActionCommand();
                if (cmd.equals("comboBoxChanged"))
                {
                    int selected = comboBoxConsoleLevel.getSelectedIndex();
                    consoleLevel = (String) comboBoxConsoleLevel.getItemAt(selected);
                    String generated = "";
                    generated = generate(tool,
                            consoleLevel,
                            (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                    generatedTextField.setText(generated);
                }
            }
        });
        panelLogLevels.add(comboBoxConsoleLevel);
        //
        labelDebugLogLevels.setText(context.cfg.gs("Generator.labelDebugLogLevels.text"));
        panelLogLevels.add(labelDebugLogLevels);
        comboBoxDebugLevel.setModel(new DefaultComboBoxModel<>(new String[]{
                "All",
                "Trace",
                "Debug",
                "Info",
                "Warn",
                "Error",
                "Fatal",
                "Off"
        }));
        comboBoxDebugLevel.setSelectedIndex(2);
        comboBoxDebugLevel.setToolTipText(context.cfg.gs("Generator.comboDebugLogLevels.tooltip"));
        comboBoxDebugLevel.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String cmd = actionEvent.getActionCommand();
                if (cmd.equals("comboBoxChanged"))
                {
                    int selected = comboBoxDebugLevel.getSelectedIndex();
                    debugLevel = (String) comboBoxDebugLevel.getItemAt(selected);
                    String generated = "";
                    generated = generate(tool,
                            (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                            debugLevel,
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                    generatedTextField.setText(generated);
                }
            }
        });
        panelLogLevels.add(comboBoxDebugLevel);

        // setup log file panel
        labelLogFile.setText(context.cfg.gs("Generator.labelLogFile.text"));
        panelLogFile.add(labelLogFile);
        comboboxLogOverwrite.setModel(new DefaultComboBoxModel<>(new String[]{
                context.cfg.gs("Generator.combobox.log.overwrite.0.log"),
                context.cfg.gs("Generator.combobox.log.overwrite.1.logOverwrite")
        }));
        comboboxLogOverwrite.setSelectedIndex(context.cfg.isLogOverwrite() ? 1 : 0);
        comboboxLogOverwrite.setToolTipText(context.cfg.gs("Generator.combobox.log.overwrite.tooltip"));
        comboboxLogOverwrite.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String cmd = actionEvent.getActionCommand();
                if (cmd.equals("comboBoxChanged"))
                {
                    String generated = "";
                    String logOption = (comboboxLogOverwrite.getSelectedIndex() == 0) ? "-f" : "-F";
                    generated = generate(tool,
                            (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                            (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                    generatedTextField.setText(generated);
                }
            }
        });
        panelLogFile.add(comboboxLogOverwrite);
        //
        selectLogButton.setText(context.cfg.gs("Generator.select.log.button"));
        selectLogButton.setMnemonic(context.cfg.gs("Generator.select.log.button.mnemonic").charAt(0));
        selectLogButton.setToolTipText(context.cfg.gs("Generator.select.log.button.tooltip"));
        selectLogButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                JFileChooser fc = new JFileChooser();
                fc.setDialogTitle(context.cfg.gs("Generator.open.log.file"));
                fc.setFileHidingEnabled(false);

                File logDir = new File(context.cfg.getLogFilePath());
                fc.setCurrentDirectory(logDir);
                File lf = new File(configName + ".log");
                fc.setSelectedFile(lf);

                while (true)
                {
                    int selection = fc.showOpenDialog((owner == null) ? context.mainFrame.panelMain : owner);
                    if (selection == JFileChooser.APPROVE_OPTION)
                    {
                        File last = fc.getCurrentDirectory();
                        File file = fc.getSelectedFile();
                        if (file.isDirectory())
                        {
                            JOptionPane.showMessageDialog(context.mainFrame,
                                    context.cfg.gs("Navigator.open.error.select.a.file.only"),
                                    context.cfg.getNavigatorName(), JOptionPane.ERROR_MESSAGE);
                            break;
                        }
                        logFile = file;
                        String generated = "";
                        generated = generate(tool,
                                (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                                (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                                (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                        generatedTextField.setText(generated);
                    }
                    break;
                }
            }
        });
        panelLogFile.add(selectLogButton);

        // nudge labelDebugLogLevels to line-up over selectLogButton
        Dimension dimC = comboBoxConsoleLevel.getPreferredSize();
        Dimension dimL = comboboxLogOverwrite.getPreferredSize();
        int margin = (dimL.width >= dimC.width) ? dimL.width - dimC.width : dimC.width - dimL.width; // allow for different locale
        Border before = labelDebugLogLevels.getBorder();
        Border spacer = new EmptyBorder(0, margin, 0, 0);
        labelDebugLogLevels.setBorder(new CompoundBorder(before, spacer));

        // setup panel dry run
        JCheckBox checkboxDryrun = new JCheckBox(context.cfg.gs("Navigator.dryrun"));
        Dimension dim = labelConsoleLogLevels.getPreferredSize();
        checkboxDryrun.setMargin(new Insets(-8, (int) dim.getWidth() + 5, -12, 0));
        checkboxDryrun.setToolTipText(context.cfg.gs("Navigator.dryrun.tooltip"));
        checkboxDryrun.setSelected(context.preferences.isDefaultDryrun());
        checkboxDryrun.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                String generated = "";
                dryRun = checkboxDryrun.isSelected();
                generated = generate(tool,
                        (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                        (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                        (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                generatedTextField.setText(generated);
            }
        });

        if (!fileGenerate)
        {
            panelDryrun.add(checkboxDryrun);

            JPanel genSpacer1 = new JPanel();
            genSpacer1.setMinimumSize(new Dimension(22, 6));
            genSpacer1.setPreferredSize(new Dimension(22, 6));
            panelDryrun.add(genSpacer1);

            ButtonGroup bg = new ButtonGroup();
            foreground.setToolTipText(context.cfg.gs("Generator.foreground.tooltip"));
            foreground.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String generated = "";
                    dryRun = checkboxDryrun.isSelected();
                    generated = generate(tool,
                            (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                            (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                    generatedTextField.setText(generated);
                }
            });
            background.setToolTipText(context.cfg.gs("Generator.background.tooltip"));
            background.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
                {
                    String generated = "";
                    dryRun = checkboxDryrun.isSelected();
                    generated = generate(tool,
                            (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                            (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
                    generatedTextField.setText(generated);
                }
            });
            bg.add(foreground);
            bg.add(background);
            panelDryrun.add(foreground);
            panelDryrun.add(background);
            if (context.preferences.getRunOption() == 0)
                foreground.setSelected(true);
            else
                background.setSelected(true); // 1
        }

        // horizontal separator
        JSeparator horizontalLine = new JSeparator(SwingConstants.HORIZONTAL);

        // setup copy panel
        generatedTextField.setPreferredSize(new Dimension(558, 30));
        String logDir = context.cfg.getLogFilePath();
        logFile = new File(logDir + configName + ".log");
        String generated = generate(tool,
                (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());
        generatedTextField.setText(generated);
        panelGenerated.add(generatedTextField);

        // setup actions panel
        copyButton.setText(context.cfg.gs("Generator.clipboard.copy"));
        copyButton.setMnemonic(context.cfg.gs("Generator.clipboard.copy.mnemonic").charAt(0));
        copyButton.setToolTipText(context.cfg.gs("Generator.clipboard.copy.tooltip"));
        copyButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                generatedTextField.selectAll();
                generatedTextField.requestFocus();
                StringSelection selection = new StringSelection(generatedTextField.getText());
                Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
                clip.setContents(selection, null);
            }
        });
        panelActions.add(copyButton);
        //
        shortcutButton.setText(context.cfg.gs("Generator.shortcut"));
        shortcutButton.setMnemonic(context.cfg.gs("Generator.shortcut.mnemonic").charAt(0));
        shortcutButton.setToolTipText(context.cfg.gs("Generator.shortcut.tooltip"));
        shortcutButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                createDesktopShortcut(owner, configName, generatedTextField.getText());
            }
        });
        panelActions.add(shortcutButton);

        // run button
        if (!fileGenerate)
        {
            JPanel genSpacer2 = new JPanel();
            genSpacer2.setMinimumSize(new Dimension(22, 6));
            genSpacer2.setPreferredSize(new Dimension(22, 6));
            panelActions.add(genSpacer2);
            //
            JButton runButton = new JButton();
            runButton.setText(context.cfg.gs("Z.run.ellipsis"));
            runButton.setMnemonic(context.cfg.gs("Z.run.mnemonic").charAt(0));
            runButton.setToolTipText(context.cfg.gs("Z.run.tooltip"));
            runButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent actionEvent)
               {
                    if (tool.isDataChanged())
                    {
                        JOptionPane.showMessageDialog((owner == null) ? context.mainFrame.panelMain : owner, context.cfg.gs("Z.please.save.then.run"), context.cfg.gs("JobsUI.title"), JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    String message = java.text.MessageFormat.format(context.cfg.gs("JobsUI.run.as.defined"), tool.getConfigName());
                    Object[] params = {message};

                    // confirm run of job
                    int reply = JOptionPane.showConfirmDialog((owner == null) ? context.mainFrame.panelMain : owner, params, context.cfg.gs("JobsUI.title"), JOptionPane.YES_NO_OPTION);
                    if (reply == JOptionPane.YES_OPTION)
                    {
                        try
                        {
                            String cmd = generate(tool,
                                    (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                                    (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                                    (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath(), foreground.isSelected());

                            String[] parms = Utils.parseCommandLIne(cmd);
                            logger.info(context.cfg.gs("Z.launching") + cmd);
                            Process proc = Runtime.getRuntime().exec(parms);
                        }
                        catch (Exception e)
                        {
                            logger.error(Utils.getStackTrace(e));
                            message = context.cfg.gs("Generator.error.launching") + tool.getConfigName() + ", " + e.getMessage();
                            Object[] opts = {context.cfg.gs("Z.ok")};
                            JOptionPane.showOptionDialog(context.mainFrame, message, getTitle(tool),
                                    JOptionPane.PLAIN_MESSAGE, JOptionPane.ERROR_MESSAGE, null, opts, opts[0]);
                        }
                    }
                }
            });
            panelActions.add(runButton);
        }

        // show the dialog
        Object[] params = {messasge, panelLogLevels, panelLogFile, panelDryrun, panelHorizontal1, horizontalLine, panelHorizontal2, panelGenerated, panelActions};
        JOptionPane.showMessageDialog((owner == null) ? context.mainFrame.panelMain : owner, params, getTitle(tool), JOptionPane.PLAIN_MESSAGE);
        if (!fileGenerate)
        {
            if (foreground.isSelected())
                context.preferences.setRunOption(0);
            else if (background.isSelected())
                context.preferences.setRunOption(1);
        }
    }

}
