package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.repository.User;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.Tools;
import com.corionis.els.tools.email.EmailTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class InviteUI extends JDialog 
{
    private HintKeys authKeys = null;
    private Context context;
    private ArrayList<AbstractTool> emailToolList = new ArrayList<AbstractTool>();
    private NavHelp helpDialog;
    private HintKeys hintKeys = null;
    private LibrariesUI.LibMeta libMeta;
    private Logger logger = LogManager.getLogger("applog");
    private Template template;
    private String type;

    public InviteUI(Window owner, Context context, LibrariesUI.LibMeta libMeta) throws MungeException
    {
        super(owner);
        this.context = context;
        this.libMeta = libMeta;
        initComponents();
        initialize();
    }

    private void actionBrowserClicked(ActionEvent e)
    {
        try
        {
            String text = template.getContent(type, libMeta.repo.getLibraries().format, false);
            text = getFormattedText(text);
            String filename = "emailPreview.html";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, filename);
            File file = new File(filename);
            Files.write(file.toPath(), text.getBytes(Charset.forName("UTF-8")));

            filename = "file:///" + filename;

            URI uri = new URI(filename);
            Desktop.getDesktop().browse(uri);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
        }
    }

    private void actionEditClicked(ActionEvent e)
    {
        EmailEditor editor = new EmailEditor(this, context, type, libMeta);
        switch (type)
        {
            case "Initial" :
                editor.setTitle(context.cfg.gs(context.cfg.gs("EmailEditor.initial.email.editor")));
                break;
            case "Invite":
                editor.setTitle(context.cfg.gs(context.cfg.gs("EmailEditor.invitation.email.editor")));
                break;
            case "Update":
                editor.setTitle(context.cfg.gs(context.cfg.gs("EmailEditor.update.email.editor")));
                break;
        }

        String title = editor.getTitle();
        if (libMeta.repo.getLibraries().format.equalsIgnoreCase("html"))
            editor.setTitle(title + context.cfg.gs("EmailEditor.html.format"));
        else
        {
            editor.setTitle(title + context.cfg.gs("EmailEditor.text.format"));
            editor.toolBar.setVisible(false);
        }

        editor.setVisible(true);
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null)
        {
            helpDialog = new NavHelp(this, context, context.cfg.gs("InviteUI.help"), "invite_" + context.preferences.getLocale() + ".html", false);
            if (!helpDialog.fault)
                helpDialog.buttonFocus();
        }
        else
        {
            helpDialog.setVisible(true);
            helpDialog.toFront();
            helpDialog.requestFocus();
            helpDialog.buttonFocus();
        }
    }

    private void actionOkClicked(ActionEvent e)
    {
        savePreferences();
        setVisible(false);
        context.mainFrame.requestFocus();
    }

    private void actionSendClicked(ActionEvent e)
    {

    }

    private void actionPreviewClicked(ActionEvent e)
    {
        try
        {
            String text = template.getContent(type, libMeta.repo.getLibraries().format, false);
            text = getFormattedText(text);
            EmailPreview preview = new EmailPreview(this, context, text, libMeta.repo.getLibraries().format,
                    context.preferences.getEmailEditorXpos(), context.preferences.getEmailEditorYpos(),
                    context.preferences.getEmailEditorWidth(), context.preferences.getEmailEditorHeight());
            preview.setVisible(true);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
        }
    }

    private void actionTypeClicked(ActionEvent e)
    {
        // Type radio buttons
        Enumeration<AbstractButton> elements = buttonGroupType.getElements();
        while (elements.hasMoreElements())
        {
            AbstractButton button = elements.nextElement();
            if (button.isSelected())
            {
                type = button.getActionCommand();
                setDialog();
            }
        }
    }

    /**
     * Get formatted text
     * <p>
     * Reformats the raw text that include newlines and inserts a <br/> to match.
     *
     * @param raw Raw text
     * @return Formatted text
     */
    public String getFormattedText(String raw)
    {
        String text = "";

        // if using HTML
        if (libMeta.repo.getLibraries().format.equalsIgnoreCase("html"))
        {
            // copy everything up to the end of the BODY tag
            int i = raw.indexOf("<body");
            text = raw.substring(0, i);
            for ( ; i < raw.length() ; ++i)
            {
                text += raw.charAt(i);
                if (raw.charAt(i) == '>')
                {
                    ++i;
                    text += "\n";
                    if (raw.charAt(i) == '\n')
                        ++i;
                    break;
                }
            }

            // find the end BODY tag
            int k = raw.indexOf("</body>");

            for ( ; i < k; ++i)
            {
                if (raw.charAt(i) == '\n')
                {
                    text += "<br/>\n"; // insert break
                }
                else
                {
                    text += raw.charAt(i);
                }
            }

            for ( ; i < raw.length(); ++i)
            {
                text += raw.charAt(i);
            }
        }
        else
            text = raw;
        text = substituteVariables(text);
        return text;
    }

    private String getFullPath(String filename)
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") + "system";
        path += System.getProperty("file.separator") + filename;
        return path;
    }

    private void initialize() throws MungeException
    {
        // scale the help icon
        Icon icon = labelHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelHelp.setIcon(replacement);

        // position
        if (context.preferences.getEmailInviteXpos() != -1 && Utils.isOnScreen(context.preferences.getEmailInviteXpos(),
                context.preferences.getEmailInviteYpos()))
        {
            this.setLocation(context.preferences.getEmailInviteXpos(), context.preferences.getEmailInviteYpos());
        }
        else
        {
            this.setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

        radioButtonInitial.setSelected(true);
        type = radioButtonInitial.getActionCommand();

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                okButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        // get email servers
        Tools tools = new Tools();
        try
        {
            emailToolList = tools.loadAllTools(context, EmailTool.INTERNAL_NAME);
            if (!emailToolList.isEmpty())
            {
                for (AbstractTool tool : emailToolList)
                {
                    comboBoxServer.addItem(tool.getConfigName());
                }
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
            comboBoxServer.setEnabled(false);
        }
        if (comboBoxServer.getItemCount() < 1)
        {
            JOptionPane.showMessageDialog(context.mainFrame, context.cfg.gs("InviteUI.at.least.one.email.server.must.be.defined"),
                    context.cfg.gs("InviteUI.title"), JOptionPane.WARNING_MESSAGE);
            throw new MungeException("ignore this");
        }

        template = new Template(context);
        setDialog();
    }

    private boolean isKeyListed(int type, String key)
    {
        HintKeys hintKeys = readHintKeys(type);
        if (hintKeys == null || hintKeys.size() < 1)
            return false;
        return hintKeys.findKey(key) != null;
    }

    /**
     * Find entry in Whitelist
     * <p>
     * Note: There are 3 various of this method. See also ServeSftp and Listener.
     *
     * @param host
     * @return true if in Whitelist
     * @throws IOException
     */
    private boolean isWhitelisted(String host) throws IOException
    {
        boolean sense = false;
        String file = getFullPath("whitelist.txt");
        if (file != null && file.length() > 0)
        {
            String filename = Utils.getFullPathLocal(file);
            if (filename.length() > 0)
            {
                if (host != null)
                {
                    host = host.replaceAll("/", "");
                    host = host.replaceAll("\\\\", "");
                    BufferedReader br = new BufferedReader(new FileReader(filename));
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        line = line.trim();
                        if (line.length() > 0 && !line.startsWith("#"))
                        {
                            if (host.equals(line))
                            {
                                sense = true;
                                break;
                            }
                            if (line.matches("[a-zA-Z-]+"))
                            {
                                try
                                {
                                    InetAddress address = InetAddress.getByName(line);
                                    line = address.getHostAddress();
                                    line = line.replaceAll("/", "");
                                    line = line.replaceAll("\\\\", "");
                                }
                                catch (UnknownHostException ex)
                                {
                                    logger.error(MessageFormat.format(context.cfg.gs("Listener.unknown.host.in.choice.whitelist.blacklist"), (true) ? 0 : 1, line));
                                    continue;
                                }
                            }
                            if (host.equals(line))
                            {
                                sense = true;
                                break;
                            }
                        }
                    }
                    br.close();
                }
            }
        }
        return sense;
    }

    private HintKeys readHintKeys(int type)
    {
        HintKeys newKeys = null;

        switch (type)
        {
            case 0:
                if (authKeys != null)
                    newKeys = authKeys;
                break;
            case 1:
                if (hintKeys != null)
                    newKeys = hintKeys;
                break;
        }

        if (newKeys == null)
        {
            newKeys = new HintKeys(context);
            String fileName = "";
            try
            {
                switch (type)
                {
                    case 0:
                        fileName = "authentication.keys";
                        break;
                    case 1:
                        fileName = "hint.keys";
                        break;
                }
                fileName = getFullPath(fileName);

                File file = new File(fileName);
                if (file.exists())
                {
                    newKeys.read(fileName);
                }
                else
                {
                    newKeys.setFilename(fileName);
                }
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }
        }
        switch (type)
        {
            case 0:
                authKeys = newKeys;
                break;
            case 1:
                hintKeys = newKeys;
                break;
        }
        return newKeys;
    }

    private void savePreferences()
    {
        Point location = this.getLocation();
        context.preferences.setEmailInviteXpos(location.x);
        context.preferences.setEmailInviteYpos(location.y);
        context.preferences.setEmailInviteLastServer(comboBoxServer.getSelectedItem().toString());
    }

    private void setDialog()
    {
        // Edit button
        switch (type)
        {
            case "Initial":
                buttonEditTemplate.setText(context.cfg.gs("InviteUI.edit.initial.email.template"));
                buttonEditTemplate.setToolTipText(context.cfg.gs("InviteUI.edit.initial.email.template"));
                break;
            case "Invite":
                buttonEditTemplate.setText(context.cfg.gs("InviteUI.edit.invitation.email.template"));
                buttonEditTemplate.setToolTipText(context.cfg.gs("InviteUI.edit.invitation.email.template"));
                break;
            case "Update":
                buttonEditTemplate.setText(context.cfg.gs("InviteUI.edit.update.email.template"));
                buttonEditTemplate.setToolTipText(context.cfg.gs("InviteUI.edit.update.email.template"));
                break;
        }

        // Authentication keys
        boolean enabled = false;
        boolean listed = false;
        if (libMeta.repo.getLibraries().key != null && libMeta.repo.getLibraries().key.length() > 0)
        {
            try
            {
                if (!isKeyListed(0, libMeta.repo.getLibraries().key))
                    enabled = true;
                else
                    listed = true;
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }
        }
        if (listed)
        {
            checkBoxAuthKeys.setToolTipText(context.cfg.gs(context.cfg.gs("InviteUI.already.in.authentication.keys")));
            checkBoxAuthKeys.setSelected(true);
        }
        if (enabled)
        {
            labelAuthKeys.setEnabled(true);
            checkBoxAuthKeys.setEnabled(true);
            checkBoxAuthKeys.setToolTipText("");
        }
        else
        {
            labelAuthKeys.setEnabled(false);
            checkBoxAuthKeys.setEnabled(false);
            if (!listed)
                checkBoxAuthKeys.setToolTipText("Requires General, Key");
        }

        // Hint keys
        enabled = false;
        listed = false;
        if (libMeta.repo.getUser().getType() != User.BASIC)
        {
            if (libMeta.repo.getLibraries().key != null && libMeta.repo.getLibraries().key.length() > 0)
            {
                try
                {
                    if (!isKeyListed(1, libMeta.repo.getLibraries().key))
                        enabled = true;
                    else
                        listed = true;
                }
                catch (Exception e)
                {
                    logger.error(e.getMessage());
                }
            }
            if (listed)
            {
                checkBoxHintKeys.setToolTipText(context.cfg.gs(context.cfg.gs("InviteUI.already.in.hint.keys")));
                checkBoxHintKeys.setSelected(true);
            }
            if (enabled)
            {
                labelHintKeys.setEnabled(true);
                checkBoxHintKeys.setEnabled(true);
                checkBoxHintKeys.setToolTipText("");
            }
            else
            {
                labelHintKeys.setEnabled(false);
                checkBoxHintKeys.setEnabled(false);
            }
        }
        else
        {
            labelHintKeys.setEnabled(false);
            checkBoxHintKeys.setEnabled(false);
            if (!listed)
                checkBoxHintKeys.setToolTipText(context.cfg.gs(context.cfg.gs("InviteUI.basic.users.do.not.use.hint.keys")));
        }

        // Whitelist
        enabled = false;
        listed = false;
        if (libMeta.repo.getLibraries().host != null && libMeta.repo.getLibraries().host.length() > 0)
        {
            String host = Utils.parseHost(libMeta.repo.getLibraries().host);
            try
            {
                if (!isWhitelisted(host))
                    enabled = true;
                else
                    listed = true;
            }
            catch (Exception e)
            {
                logger.error(e.getMessage());
            }
        }
        if (listed)
        {
            checkBoxWhitelist.setToolTipText(context.cfg.gs(context.cfg.gs("InviteUI.already.in.the.whitelist")));
            checkBoxWhitelist.setSelected(true);
        }
        if (enabled)
        {
            labelWhitelist.setEnabled(true);
            checkBoxWhitelist.setEnabled(true);
            checkBoxWhitelist.setToolTipText("");
        }
        else
        {
            labelWhitelist.setEnabled(false);
            checkBoxWhitelist.setEnabled(false);
            if (!listed)
                checkBoxWhitelist.setToolTipText(context.cfg.gs("InviteUI.requires.general.host.name.or.ip.address"));
        }

        // email servers combobox
        if (context.preferences.getDefaultEmailServer().length() > 0)
        {
            if (context.preferences.getEmailInviteLastServer().length() > 0)
                comboBoxServer.setSelectedItem(context.preferences.getEmailInviteLastServer());
            else
                comboBoxServer.setSelectedItem(context.preferences.getDefaultEmailServer());
        }

        // to address
        textFieldTo.setText(libMeta.repo.getLibraries().email);

        // send button
        switch (type)
        {
            case "Initial":
                buttonSend.setToolTipText(context.cfg.gs("InviteUI.send.initial.email"));
                break;
            case "Invite":
                buttonSend.setToolTipText(context.cfg.gs("InviteUI.send.invitation.email"));
                break;
            case "Update":
                buttonSend.setToolTipText(context.cfg.gs("InviteUI.send.update.email"));
                break;
        }
    }

    /**
     * Substitute embeeded variables
     * <p>
     * Supported variables, case-insensitive:
     * <li>${publisherHost} : Current Publisher host</li>
     * <li>${publisherName} : Current Publisher Collection name</li>
     * <li>${publisherUser} : Current Publisher User name</li>
     * <li>${userName} : User name</li>
     * <li>${userType} : Type of user, Basic, Advanced or Administrator</li>
     *
     * @param content
     * @return Expanded content
     */
    private String substituteVariables(String content)
    {
        content = content.replaceAll("\\$\\{(?i)publishername}", libMeta.repo.getLibraries().description);
        content = content.replaceAll("\\$\\{(?i)publisheruser}", libMeta.repo.getUser().getName());
        content = content.replaceAll("\\$\\{(?i)publisherhost}", libMeta.repo.getLibraries().host);

        content = content.replaceAll("\\$\\{(?i)username}", libMeta.repo.getUser().getName());

        String text = "";
        switch (libMeta.repo.getUser().getType())
        {
            case 0:
                text = context.cfg.gs("Navigator.user.type.basic");
                break;
            case 1:
                text = context.cfg.gs("Navigator.user.type.advanced");
                break;
            case 2:
                text = context.cfg.gs("Navigator.user.type.admin");
                break;
        }
        content = content.replaceAll("\\$\\{(?i)usertype}", text);

        return content;
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        panelTop = new JPanel();
        panelTopRadio = new JPanel();
        menuBarRadio = new JMenuBar();
        labelWhat = new JLabel();
        radioButtonInitial = new JRadioButton();
        radioButtonInvite = new JRadioButton();
        radioButtonUpdate = new JRadioButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        contentPanel = new JPanel();
        panelTopButtons = new JPanel();
        buttonEditTemplate = new JButton();
        panelOptions = new JPanel();
        vSpacer2 = new JPanel(null);
        labelAuthKeys = new JLabel();
        checkBoxAuthKeys = new JCheckBox();
        labelHintKeys = new JLabel();
        checkBoxHintKeys = new JCheckBox();
        labelWhitelist = new JLabel();
        checkBoxWhitelist = new JCheckBox();
        vSpacer1 = new JPanel(null);
        labelServer = new JLabel();
        comboBoxServer = new JComboBox();
        labelTo = new JLabel();
        textFieldTo = new JTextField();
        panelActions = new JPanel();
        buttonPreview = new JButton();
        buttonBrowser = new JButton();
        buttonSend = new JButton();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        okButton = new JButton();
        buttonGroupType = new ButtonGroup();

        //======== this ========
        setTitle(context.cfg.gs("InviteUI.title"));
        setModal(true);
        setResizable(false);
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setLayout(new BorderLayout());

            //======== panelTop ========
            {
                panelTop.setMinimumSize(new Dimension(140, 38));
                panelTop.setPreferredSize(new Dimension(400, 38));
                panelTop.setLayout(new BorderLayout());

                //======== panelTopRadio ========
                {
                    panelTopRadio.setMinimumSize(new Dimension(140, 38));
                    panelTopRadio.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                    //======== menuBarRadio ========
                    {
                        menuBarRadio.setMargin(new Insets(2, 8, 2, 8));

                        //---- labelWhat ----
                        labelWhat.setText(context.cfg.gs("InviteUI.labelWhat.text"));
                        labelWhat.setFont(labelWhat.getFont().deriveFont(labelWhat.getFont().getStyle() & ~Font.BOLD, labelWhat.getFont().getSize() + 1f));
                        menuBarRadio.add(labelWhat);

                        //---- radioButtonInitial ----
                        radioButtonInitial.setText(context.cfg.gs("InviteUI.radioButtonInitial.text"));
                        radioButtonInitial.setToolTipText(context.cfg.gs("InviteUI.radioButtonInitial.toolTipText"));
                        radioButtonInitial.setActionCommand("Initial");
                        radioButtonInitial.setFont(radioButtonInitial.getFont().deriveFont(radioButtonInitial.getFont().getSize() + 1f));
                        radioButtonInitial.addActionListener(e -> actionTypeClicked(e));
                        menuBarRadio.add(radioButtonInitial);

                        //---- radioButtonInvite ----
                        radioButtonInvite.setText(context.cfg.gs("InviteUI.radioButtonInvite.text"));
                        radioButtonInvite.setToolTipText(context.cfg.gs("InviteUI.radioButtonInvite.toolTipText"));
                        radioButtonInvite.setActionCommand("Invite");
                        radioButtonInvite.setFont(radioButtonInvite.getFont().deriveFont(radioButtonInvite.getFont().getSize() + 1f));
                        radioButtonInvite.addActionListener(e -> actionTypeClicked(e));
                        menuBarRadio.add(radioButtonInvite);

                        //---- radioButtonUpdate ----
                        radioButtonUpdate.setText(context.cfg.gs("InviteUI.radioButtonUpdate.text"));
                        radioButtonUpdate.setToolTipText(context.cfg.gs("InviteUI.radioButtonUpdate.toolTipText"));
                        radioButtonUpdate.setActionCommand("Update");
                        radioButtonUpdate.setFont(radioButtonUpdate.getFont().deriveFont(radioButtonUpdate.getFont().getSize() + 1f));
                        radioButtonUpdate.addActionListener(e -> actionTypeClicked(e));
                        menuBarRadio.add(radioButtonUpdate);
                    }
                    panelTopRadio.add(menuBarRadio);
                }
                panelTop.add(panelTopRadio, BorderLayout.WEST);

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
                    labelHelp.setToolTipText(context.cfg.gs("InviteUI.labelHelp.toolTipText"));
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
            dialogPane.add(panelTop, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setLayout(new BorderLayout());

                //======== panelTopButtons ========
                {
                    panelTopButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));

                    //---- buttonEditTemplate ----
                    buttonEditTemplate.setText(context.cfg.gs("InviteUI.buttonEditTemplate.text"));
                    buttonEditTemplate.addActionListener(e -> actionEditClicked(e));
                    panelTopButtons.add(buttonEditTemplate);
                }
                contentPanel.add(panelTopButtons, BorderLayout.NORTH);

                //======== panelOptions ========
                {
                    panelOptions.setAlignmentY(0.0F);
                    panelOptions.setLayout(new GridBagLayout());
                    ((GridBagLayout)panelOptions.getLayout()).columnWidths = new int[] {0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                    ((GridBagLayout)panelOptions.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                    //---- vSpacer2 ----
                    vSpacer2.setPreferredSize(new Dimension(10, 4));
                    vSpacer2.setMinimumSize(new Dimension(12, 4));
                    panelOptions.add(vSpacer2, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- labelAuthKeys ----
                    labelAuthKeys.setText(context.cfg.gs("InviteUI.labelAuthKeys.text"));
                    panelOptions.add(labelAuthKeys, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 15, 4, 4), 0, 0));

                    //---- checkBoxAuthKeys ----
                    checkBoxAuthKeys.setToolTipText(context.cfg.gs("InviteUI.checkBoxAuthKeys.toolTipText"));
                    panelOptions.add(checkBoxAuthKeys, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- labelHintKeys ----
                    labelHintKeys.setText(context.cfg.gs("InviteUI.labelHintKeys.text"));
                    panelOptions.add(labelHintKeys, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 15, 4, 4), 0, 0));
                    panelOptions.add(checkBoxHintKeys, new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- labelWhitelist ----
                    labelWhitelist.setText(context.cfg.gs("InviteUI.labelWhitelist.text"));
                    panelOptions.add(labelWhitelist, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 15, 4, 4), 0, 0));

                    //---- checkBoxWhitelist ----
                    checkBoxWhitelist.setToolTipText(context.cfg.gs("InviteUI.checkBoxWhitelist.toolTipText"));
                    panelOptions.add(checkBoxWhitelist, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- vSpacer1 ----
                    vSpacer1.setPreferredSize(new Dimension(10, 4));
                    vSpacer1.setMinimumSize(new Dimension(12, 4));
                    panelOptions.add(vSpacer1, new GridBagConstraints(0, 4, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- labelServer ----
                    labelServer.setText(context.cfg.gs("InviteUI.labelServer.text"));
                    panelOptions.add(labelServer, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                        new Insets(0, 10, 4, 4), 0, 0));

                    //---- comboBoxServer ----
                    comboBoxServer.setToolTipText(context.cfg.gs("InviteUI.comboBoxServer.toolTipText"));
                    panelOptions.add(comboBoxServer, new GridBagConstraints(1, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- labelTo ----
                    labelTo.setText(context.cfg.gs("InviteUI.labelTo.text"));
                    labelTo.setHorizontalAlignment(SwingConstants.RIGHT);
                    panelOptions.add(labelTo, new GridBagConstraints(0, 6, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 4), 0, 0));

                    //---- textFieldTo ----
                    textFieldTo.setPreferredSize(new Dimension(240, 34));
                    textFieldTo.setMinimumSize(new Dimension(240, 34));
                    textFieldTo.setToolTipText(context.cfg.gs("InviteUI.textFieldTo.toolTipText"));
                    panelOptions.add(textFieldTo, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //======== panelActions ========
                    {
                        panelActions.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));

                        //---- buttonPreview ----
                        buttonPreview.setText(context.cfg.gs("InviteUI.buttonPreview.text"));
                        buttonPreview.setToolTipText(context.cfg.gs("EmailEditor.buttonPreview.toolTipText"));
                        buttonPreview.addActionListener(e -> actionPreviewClicked(e));
                        panelActions.add(buttonPreview);

                        //---- buttonBrowser ----
                        buttonBrowser.setText(context.cfg.gs("InviteUI.buttonBrowser.text"));
                        buttonBrowser.setToolTipText(context.cfg.gs("EmailEditor.buttonBrowser.toolTipText"));
                        buttonBrowser.addActionListener(e -> actionBrowserClicked(e));
                        panelActions.add(buttonBrowser);

                        //---- buttonSend ----
                        buttonSend.setText(context.cfg.gs("InviteUI.buttonSend.text"));
                        buttonSend.addActionListener(e -> actionSendClicked(e));
                        panelActions.add(buttonSend);
                    }
                    panelOptions.add(panelActions, new GridBagConstraints(0, 7, 2, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
                }
                contentPanel.add(panelOptions, BorderLayout.CENTER);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(BorderFactory.createEmptyBorder());
                buttonBar.setMinimumSize(new Dimension(93, 34));
                buttonBar.setPreferredSize(new Dimension(93, 34));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0};
                buttonBar.add(labelStatus, new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 8, 0, 5), 0, 0));

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> actionOkClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroupType ----
        buttonGroupType.add(radioButtonInitial);
        buttonGroupType.add(radioButtonInvite);
        buttonGroupType.add(radioButtonUpdate);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel panelTop;
    private JPanel panelTopRadio;
    private JMenuBar menuBarRadio;
    private JLabel labelWhat;
    private JRadioButton radioButtonInitial;
    private JRadioButton radioButtonInvite;
    private JRadioButton radioButtonUpdate;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JPanel contentPanel;
    private JPanel panelTopButtons;
    private JButton buttonEditTemplate;
    private JPanel panelOptions;
    private JPanel vSpacer2;
    private JLabel labelAuthKeys;
    private JCheckBox checkBoxAuthKeys;
    private JLabel labelHintKeys;
    private JCheckBox checkBoxHintKeys;
    private JLabel labelWhitelist;
    private JCheckBox checkBoxWhitelist;
    private JPanel vSpacer1;
    private JLabel labelServer;
    private JComboBox comboBoxServer;
    private JLabel labelTo;
    private JTextField textFieldTo;
    private JPanel panelActions;
    private JButton buttonPreview;
    private JButton buttonBrowser;
    private JButton buttonSend;
    private JPanel buttonBar;
    private JLabel labelStatus;
    private JButton okButton;
    private ButtonGroup buttonGroupType;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    //
    // @formatter:on
    // </editor-fold>
}
