package com.groksoft.els.gui;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.jobs.Job;
import com.groksoft.els.tools.AbstractTool;
import com.groksoft.els.tools.operations.OperationsTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.PrintWriter;

public class Generator
{
    private Context context;
    private String consoleLevel = "";
    private String debugLevel = "";
    private String generated = "";
    private File logFile = null;
    private String logOption = "";
    private Logger logger = LogManager.getLogger("applog");

    public Generator(Context context)
    {
        this.context = context;
    }

    private void createDesktopShortcut(Component owner, String name, String commandLine)
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

        // setup name panel
        labelName.setText(context.cfg.gs("Generator.shortcut.name"));
        panelName.add(labelName);
        fieldName.setText(name);
        fieldName.setPreferredSize(new Dimension(200, 30));
        panelName.add(fieldName);

        // setup comment panel
        labelComment.setText(context.cfg.gs("Generator.shortcut.comment"));
        panelComment.add(labelComment);
        fieldComment.setText("Launch " + name);
        fieldComment.setPreferredSize(new Dimension(200, 30));
        panelComment.add(fieldComment);

        // setup termina panel
        labelTerminal.setText(context.cfg.gs("Generator.shortcut.launch.in.terminal"));
        panelTerminal.add(labelTerminal);
        panelTerminal.add(checkboxTerminal);

        Object[] params = {panelName, panelComment, panelTerminal};
        int resp = JOptionPane.showConfirmDialog(owner, params, context.cfg.gs("Generator.shortcut.title"), JOptionPane.OK_CANCEL_OPTION);
        if (resp == JOptionPane.OK_OPTION)
        {
            StringBuilder sb = new StringBuilder();

            name = fieldName.getText();
            if (name != null && name.length() > 0)
            {
                String icon = context.cfg.getIconPath() + System.getProperty("file.separator") +
                sb.append("[Desktop Entry]\n");
                sb.append("Name=" + name + "\n");
                sb.append("Exec=" + commandLine + "\n");
                sb.append("Comment=" + fieldComment.getText() + "\n");
                sb.append("Terminal=" + checkboxTerminal.isSelected() + "\n");
                sb.append("Icon=" + context.cfg.getIconPath() + "\n");
                sb.append("Type=Application\n");
            }
            else
            {
                // name required
            }

            String shortcut = System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop" +
                    System.getProperty("file.separator") + name + ".desktop";
            File shortFile = new File(shortcut);
            if (shortFile.exists())
            {
                // exists, overwrite?
            }

            try
            {
                PrintWriter outputStream = new PrintWriter(shortcut);
                outputStream.println(sb);
                outputStream.close();

                if (Utils.getOS().equalsIgnoreCase("Linux"))
                {
                    shortFile.setExecutable(true);
                }
            }
            catch (Exception e)
            {
                // error
                System.out.println(Utils.getStackTrace(e));
            }
        }
    }

    private String generate(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwrite, String log)
    {
        try
        {
            if (tool instanceof Job)
            {
                generated = generateJobCommandline(tool, consoleLevel, debugLevel, overwrite, log);
            }
            else if (tool instanceof OperationsTool)
            {
                generated = generateOperationsCommandline(tool, consoleLevel, debugLevel, overwrite, log);
            }
            // TODO EXTEND+ Add other command line generators here
        }
        catch (Exception e)
        {
        }
        return generated;
    }

    private String generateJobCommandline(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwriteLog, String log) throws Exception
    {
        // TODO change when JRE is embedded in ELS distro
        boolean glo = context.preferences.isGenerateLongOptions();
        String jar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        String overOpt = overwriteLog ? "-F" : "-f";
        String cmd = "java -jar " + jar + " -j \"" + tool.getConfigName() + "\"";

        // --- hint keys
        if (context.cfg.getHintKeysFile().length() > 0)
        {
            if (context.cfg.isHintSkipMainProcess())
                cmd += " " + (glo ? "--keys-only" : "-K");
            else
                cmd += " " + (glo ? "--keys" : "-k");
            cmd += " \"" + context.cfg.getHintKeysFile() + "\"";
        }

        // --- hints & hint server
        if (context.cfg.getHintTrackerFilename().length() > 0)
            cmd += " " + (glo ? "--hints" : "-h") + " \"" + context.cfg.getHintTrackerFilename() + "\"";
        else
            if (context.cfg.getHintsDaemonFilename().length() > 0)
                cmd += " " + (glo ? "--hint-server" : "-H") + " \"" + context.cfg.getHintsDaemonFilename() + "\"";

        cmd += " -c " + consoleLevel + " -d " + debugLevel + " " + overOpt + " \"" + log + "\"";
        return cmd;
    }

    private String generateOperationsCommandline(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwriteLog, String log) throws Exception
    {
        // TODO change when JRE is embedded in ELS distro
        String jar = new File(MainFrame.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getPath();
        // tool has all the parameter data, use it's generate method
        String opts = ((OperationsTool)tool).generateCommandLine(context.publisherRepo.getJsonFilename(), context.subscriberRepo.getJsonFilename());
        String overOpt = overwriteLog ? "-F" : "-f";
        String cmd = "java -jar " + jar + " " + opts + " -c " + consoleLevel + " -d " + debugLevel + " " + overOpt + " \"" + log + "\"";
        return cmd;
    }

    public String getConsoleLevel()
    {
        return consoleLevel;
    }

    public String getDebugLevel()
    {
        return debugLevel;
    }

    public String getGenerated()
    {
        return generated;
    }

    public File getLogFile()
    {
        return logFile;
    }

    public String getLogOption()
    {
        return logOption;
    }

    private String getTitle(AbstractTool tool)
    {
        if (tool instanceof Job)
            return context.cfg.gs("JobsUI.title");
        if (tool instanceof OperationsTool)
            return context.cfg.gs("Operations.title");
        return "unknown";
    }

    public boolean showDialog(Component owner, AbstractTool tool, String configName)
    {
        boolean completed = false;
        String messasge = "<html><body>" + context.cfg.gs("Generator.generated") + "<b>" + configName +
                "</b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                //"&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<br/>&nbsp;<br/></body></html>";

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
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath());
                    generatedTextField.setText(generated);
                    generatedTextField.selectAll();
                    generatedTextField.requestFocus();
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
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath());
                    generatedTextField.setText(generated);
                    generatedTextField.selectAll();
                    generatedTextField.requestFocus();
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
        comboboxLogOverwrite.setSelectedIndex(1);
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
                    logOption = (comboboxLogOverwrite.getSelectedIndex() == 0) ? "-f" : "-F";
                    generated = generate(tool,
                            (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                            (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                            (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath());
                    generatedTextField.setText(generated);
                    generatedTextField.selectAll();
                    generatedTextField.requestFocus();
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

                File logDir = new File(context.cfg.getWorkingDirectory());
                fc.setCurrentDirectory(logDir);
                File lf = new File(configName + ".log");
                fc.setSelectedFile(lf);

                while (true)
                {
                    int selection = fc.showOpenDialog(owner);
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
                                (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath());
                        generatedTextField.setText(generated);
                        generatedTextField.selectAll();
                        generatedTextField.requestFocus();
                    }
                    break;
                }
            }
        });
        panelLogFile.add(selectLogButton);

        // horizontal separator
        JSeparator horizontalLine = new JSeparator(SwingConstants.HORIZONTAL);

        // setup copy panel
        generatedTextField.setPreferredSize(new Dimension(500, 30));
        String logDir = context.cfg.getLogFilePath();
        logFile = new File(logDir + configName + ".log");
        String generated = generate(tool,
                (String) comboBoxConsoleLevel.getItemAt(comboBoxConsoleLevel.getSelectedIndex()),
                (String) comboBoxDebugLevel.getItemAt(comboBoxDebugLevel.getSelectedIndex()),
                (comboboxLogOverwrite.getSelectedIndex() == 0) ? false : true, logFile.getPath());
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
                generatedTextField.selectAll();
                generatedTextField.requestFocus();
                createDesktopShortcut(owner, configName, generatedTextField.getText());
            }
        });
        panelActions.add(shortcutButton);

        // show the dialog
        Object[] params = {messasge, panelLogLevels, panelLogFile, panelHorizontal1, horizontalLine, panelHorizontal2, panelGenerated, panelActions};
        JOptionPane.showMessageDialog(owner, params, getTitle(tool), JOptionPane.PLAIN_MESSAGE);
        completed = true;

        return completed;
    }

}
