package com.corionis.els.gui;

import com.corionis.els.Configuration;
import com.corionis.els.jobs.Job;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.operations.OperationsTool;
import com.corionis.els.Context;
import com.corionis.els.Utils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

@SuppressWarnings(value = "unchecked")
public class Generator
{
    private String consoleLevel = "";
    private Context context;
    private String debugLevel = "";
    private String generated = "";
    private File logFile = null;
    private String logOption = "";
    private Logger logger = LogManager.getLogger("applog");

    public Generator(Context context)
    {
        this.context = context;
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
        panelName.add(labelName);
        fieldName.setText(name);
        fieldName.setPreferredSize(new Dimension(200, 30));
        panelName.add(fieldName);

        // setup comment panel
        labelComment.setText(context.cfg.gs("Generator.shortcut.comment"));
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
        dialog.setLocationRelativeTo(owner);
        Object[] params = {panelName, panelComment, panelTerminal, panelWarning};
        int resp = JOptionPane.showConfirmDialog(dialog, params, context.cfg.gs("Generator.shortcut.title"), JOptionPane.OK_CANCEL_OPTION);
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
                    // Shortcut.exe :: https://www.optimumx.com/downloads.html
                    /* Shortcut.exe ReadMe.txt:
                        Shortcut [Version 1.20]
                        Creates, modifies or queries Windows shell links (shortcuts)
                        The syntax of this command is:
                         /F:filename    : Specifies the .LNK shortcut file.
                         /A:action      : Defines the action to take (C=Create, E=Edit or Q=Query).
                         /T:target      : Defines the target path and file name the shortcut points to.
                         /P:parameters  : Defines the command-line parameters to pass to the target.
                         /W:working dir : Defines the working directory the target starts with.
                         /R:run style   : Defines the window state (1=Normal, 3=Max, 7=Min).
                         /I:icon,index  : Defines the icon and optional index (file.exe or file.exe,0).
                         /H:hotkey      : Defines the hotkey, a numeric value of the keyboard shortcut.
                         /D:description : Defines the description (or comment) for the shortcut.

                         Notes:
                         - Any argument that contains spaces must be enclosed in "double quotes".
                         - If Query is specified (/A:Q), all arguments except /F: are ignored.
                         - To find the numeric hotkey value, use Explorer to set a hotkey and then /A:Q
                         - To prevent an environment variable from being expanded until the shortcut
                           is launched, use the ^ carat escape character like this: ^%WINDIR^%

                         Examples:
                           /f:"%ALLUSERSPROFILE%\Start Menu\Programs\My App.lnk" /a:q
                           /f:"%USERPROFILE%\Desktop\Notepad.lnk" /a:c /t:^%WINDIR^%\Notepad.exe /h:846
                           /f:"%USERPROFILE%\Desktop\Notepad.lnk" /a:e /p:C:\Setup.log /r:3
                    */
                    String target = context.cfg.getExecutablePath();
                    if (!checkboxTerminal.isSelected())
                        target = target.replace("java.exe", "javaw.exe");

                    // remove "java.exe" target /T to have only parameters /P
                    commandLine = commandLine.substring(("\"" + context.cfg.getExecutablePath() + "\" ").length());
                    // escape embedded quotes
                    commandLine = commandLine.replace("\"", "\\\"");

                    StringBuilder wb = new StringBuilder();
                    wb.append("\"" + context.cfg.getElsJarPath() + System.getProperty("file.separator") + "Shortcut.exe" + "\" ");
                    wb.append("/F:\"" + shortFile.getAbsolutePath() + "\" ");
                    wb.append("/A:C ");
                    wb.append("/T:\"" + target + "\" ");
                    wb.append("/I:\"" + context.cfg.getIconPath() + "\" ");
                    wb.append("/W:\"" + context.cfg.getWorkingDirectory() + "\" ");
                    wb.append("/D:\"" + fieldComment.getText() + "\" ");
                    wb.append("/P:\"" + commandLine + "\" ");
                    String wc = wb.toString();
                    context.navigator.execExternalExe(wc);
                }
            }
        }
    }

    private String generate(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwrite, String log)
    {
        try
        {
            if (tool == null)
            {
                generated = context.cfg.generateCurrentCommandline();
            }
            else if (tool instanceof Job)
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
            logger.error(Utils.getStackTrace(e));
        }
        return generated;
    }

    private String generateJobCommandline(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwriteLog, String log) throws Exception
    {
        boolean glo = context.preferences.isGenerateLongOptions();
        String exec = context.cfg.getExecutablePath();
        String jar = (!Utils.isOsWindows() ? context.cfg.getElsJar() : "");

        String conf = getCfgOpt();
        String overOpt = overwriteLog ? "-F" : "-f";

        String cmd = "\"" + exec + "\"" +
                (jar.length() > 0 ? " -jar " + "\"" + jar + "\" " : "");

        cmd += conf + " -j \"" + tool.getConfigName() + "\"";

        // --- hint keys
        if (context.preferences.getLastHintKeysOpenFile().length() > 0)
        {
            if (context.cfg.isHintSkipMainProcess())
                cmd += " " + (glo ? "--keys-only" : "-K");
            else
                cmd += " " + (glo ? "--keys" : "-k");
            cmd += " \"" + Utils.makeRelativePath(context.cfg.getWorkingDirectory(), context.preferences.getLastHintKeysOpenFile()) + "\"";
        }

        // --- hints & hint server
        if (context.preferences.getLastHintTrackingOpenFile().length() > 0)
        {
            String hf = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), context.preferences.getLastHintTrackingOpenFile());
            if (context.preferences.isLastHintTrackingIsRemote())
                cmd += " " + (glo ? "--hint-server" : "-H") + " \"" + hf + "\"";
            else
                cmd += " " + (glo ? "--hints" : "-h") + " \"" + hf + "\"";
        }

        // Jobs use Origins and not Publisher or Subscriber arguments

        cmd += " -c " + consoleLevel + " -d " + debugLevel + " " + overOpt + " \"" + log + "\"";
        return cmd;
    }

    private String generateOperationsCommandline(AbstractTool tool, String consoleLevel, String debugLevel, boolean overwriteLog, String log) throws Exception
    {
        String exec = context.cfg.getExecutablePath();
        String jar = (!Utils.isOsWindows() ? context.cfg.getElsJar() : "");
        // tool has all the parameter data, use it's generate method
        String conf = getCfgOpt();
        // use context.preferences because the subscriber filename can change if requested from a remote listener
        String opts = ((OperationsTool) tool).generateCommandLine(context.preferences.getLastPublisherOpenFile(), context.preferences.getLastSubscriberOpenFile());
        String overOpt = overwriteLog ? "-F" : "-f";
        String cmd = "\"" + exec + "\"" +
                (jar.length() > 0 ? " -jar " + "\"" + jar + "\"" : "") +
                " " + conf + opts + " -c " + consoleLevel + " -d " + debugLevel + " " + overOpt + " \"" + log + "\"";
        return cmd;
    }

    private String getCfgOpt()
    {
        return "-C \"" + context.cfg.getWorkingDirectory() + "\" ";
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
        if (tool == null)
            return Configuration.NAVIGATOR_NAME;
        if (tool instanceof Job)
            return context.cfg.gs("JobsUI.title");
        if (tool instanceof OperationsTool)
            return context.cfg.gs("Operations.displayName");
        return "unknown";
    }

    public boolean showDialog(JDialog owner, AbstractTool tool, String configName)
    {
        boolean completed = false;
        String messasge = "<html><body>" + context.cfg.gs("Generator.generated") + " <b>" + configName +
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
        generatedTextField.setPreferredSize(new Dimension(530, 30));
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
        //if (!(owner == context.mainFrame && Utils.getOS().toLowerCase().equals("windows")))       // disable?
        {
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
        }

        // show the dialog
        Object[] params = {messasge, panelLogLevels, panelLogFile, panelHorizontal1, horizontalLine, panelHorizontal2, panelGenerated, panelActions};
        JOptionPane.showMessageDialog(owner, params, getTitle(tool), JOptionPane.PLAIN_MESSAGE);
        completed = true;

        return completed;
    }

}
