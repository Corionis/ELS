package com.corionis.els.gui.libraries;

import com.corionis.els.Context;
import com.corionis.els.MungeException;
import com.corionis.els.Utils;
import com.corionis.els.gui.NavHelp;
import com.corionis.els.gui.system.FileEditor;
import com.corionis.els.gui.system.FileEditor.EditorTypes;
import com.corionis.els.gui.tools.email.EmailPreview;
import com.corionis.els.gui.tools.email.EmailTemplates;
import com.corionis.els.gui.tools.email.Template;
import com.corionis.els.hints.HintKey;
import com.corionis.els.hints.HintKeys;
import com.corionis.els.repository.Repository;
import com.corionis.els.tools.AbstractTool;
import com.corionis.els.tools.Tools;
import com.corionis.els.tools.email.EmailHandler;
import com.corionis.els.tools.email.EmailTool;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.text.MessageFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileSystemView;

public class InviteUI extends JDialog
{
    private String attachment = "";
    private HintKeys authKeys = null;
    private String body = "";
    private final Context context;
    private Template currentTemplate = null;
    private ArrayList<AbstractTool> emailToolList = new ArrayList<AbstractTool>();
    private String from = "";
    private NavHelp helpDialog;
    private HintKeys hintKeys = null;
    private ArrayList<String> ips = null;
    private InviteContentUI inviteContentUI = null;
    private final LibrariesUI.LibMeta libMeta;
    private final Logger logger = LogManager.getLogger("applog");
    private EmailTool server;
    private String to;

    public InviteUI(Window owner, Context context, LibrariesUI.LibMeta libMeta) throws MungeException
    {
        super(owner);
        this.context = context;
        this.libMeta = libMeta;
        initComponents();
        initialize();
    }

    private void actionArchiveClicked(ActionEvent e)
    {
        try
        {
            labelStatus.setText("<html><body>&nbsp;</body></html>");
            createArchive(true);
            if (!inviteContentUI.cancelled)
            {
                String head = context.cfg.gs("InviteUI.archive.file.created");
                String msg = head + attachment;
                JOptionPane.showConfirmDialog(this, msg, context.cfg.gs("InviteUI.title"), JOptionPane.PLAIN_MESSAGE);
                labelStatus.setText("<html><body>&nbsp;</body></html>");
            }
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionBrowserClicked(ActionEvent e)
    {
        try
        {
            labelStatus.setText("<html><body>&nbsp;</body></html>");
            currentTemplate = (Template) comboBoxTemplates.getSelectedItem();
            currentTemplate.setContext(context);
            String text = currentTemplate.getContent(null);
            text = currentTemplate.getFormattedText(text, libMeta.repo);
            String filename = "emailPreview.html";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, filename);
            File file = new File(filename);
            Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));
            filename = "file:///" + filename;
            URI uri = new URI(filename);
            Desktop.getDesktop().browse(uri);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage(), ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionEditClicked(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
        currentTemplate = (Template) comboBoxTemplates.getSelectedItem();
        EmailTemplates emailTemplates = new EmailTemplates(this, context);
        emailTemplates.setStart(currentTemplate, libMeta.repo);
        emailTemplates.setVisible(true);
        emailTemplates.toFront();
        emailTemplates.requestFocus();
    }

    private void actionHelpClicked(MouseEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
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
        labelStatus.setText("<html><body>&nbsp;</body></html>");
        savePreferences();
        if (helpDialog != null && helpDialog.isVisible())
        {
            helpDialog.setVisible(false);
        }

        setVisible(false);
        context.mainFrame.requestFocus();
    }

    private void actionPreviewClicked(ActionEvent e)
    {
        try
        {
            labelStatus.setText("<html><body>&nbsp;</body></html>");
            currentTemplate = (Template) comboBoxTemplates.getSelectedItem();
            currentTemplate.setContext(context);
            String text = currentTemplate.getContent(null);
            text = currentTemplate.getFormattedText(text, libMeta.repo);
            EmailPreview preview = new EmailPreview(this, null, context, text, libMeta.repo.getLibraries().format,
                    context.preferences.getEmailTemplatesXpos(), context.preferences.getEmailTemplatesYpos(),
                    context.preferences.getEmailTemplatesWidth(), context.preferences.getEmailTemplatesHeight());
            preview.setVisible(true);
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actionSendClicked(ActionEvent e)
    {
        try
        {
            labelStatus.setText("<html><body>&nbsp;</body></html>");
            if (!checkParameters())
                return;

            createArchive(false);
            if (!inviteContentUI.cancelled)
            {
                performAdds();
                currentTemplate = (Template) comboBoxTemplates.getSelectedItem();
                currentTemplate.setContext(context);
                body = currentTemplate.getContent(null);
                body = currentTemplate.getFormattedText(body, libMeta.repo);
                sendInviteEmail();
            }
        }
        catch (Exception ex)
        {
            logger.error(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean checkParameters()
    {
        server = (EmailTool) comboBoxServer.getSelectedItem();
        if (server == null)
        {
            JOptionPane.showMessageDialog(this, context.cfg.gs("InviteUI.please.select.a.server"), "Send", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        else
        {
            to = textFieldTo.getText();
            if (to.isEmpty())
            {
                JOptionPane.showMessageDialog(this, context.cfg.gs("InviteUI.please.enter.a.to.address"), "Send", JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            else
            {
                return true;
            }
        }
    }


    private void compAuthKeys(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void compHintKeys(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void compServer(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void compTemplates(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void compTo(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void compWhitelist(ActionEvent e)
    {
        labelStatus.setText("<html><body>&nbsp;</body></html>");
    }

    private void createArchive(boolean archiveOnly) throws Exception
    {
        inviteContentUI = new InviteContentUI(this, context, libMeta, archiveOnly);
        inviteContentUI.setVisible(true);

        if (!inviteContentUI.cancelled)
        {
            ArrayList<String> filesToCompress = new ArrayList();
            if (inviteContentUI.textFieldChoose.getText().isEmpty())
            {
                if (inviteContentUI.checkBoxPublisher.isSelected() && !inviteContentUI.textFieldCurrentPublisher.getText().isEmpty())
                {
                    String relPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), inviteContentUI.textFieldCurrentPublisher.getText());
                    filesToCompress.add(relPath);
                }

                String repoFilename = "";
                if (inviteContentUI.checkBoxSubscriber.isSelected() && !inviteContentUI.textFieldCurrentSubscriber.getText().isEmpty())
                {
                    Repository subRepo = new Repository(context, Repository.SUBSCRIBER);
                    subRepo.read(inviteContentUI.textFieldCurrentSubscriber.getText(), "Subscriber", false);
                    subRepo = subRepo.cloneConnection();
                    String filename = subRepo.getJsonFilename();
                    String dir = Utils.getLeftPath(filename, subRepo.getSeparator());
                    String ren = Utils.getFileName(filename);
                    repoFilename = dir + System.getProperty("file.separator") + ren + "_connection.json";
                    subRepo.setJsonFilename(repoFilename);
                    subRepo.write();
                    String relPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), subRepo.getJsonFilename());
                    filesToCompress.add(relPath);
                }

                if (inviteContentUI.checkBoxHintServer.isSelected() && !inviteContentUI.textFieldCurrentHintServer.getText().isEmpty())
                {
                    String relPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), inviteContentUI.textFieldCurrentHintServer.getText());
                    filesToCompress.add(relPath);
                }

                if (inviteContentUI.checkBoxHints.isSelected() && !inviteContentUI.textFieldCurrentHints.getText().isEmpty())
                {
                    String relPath = Utils.makeRelativePath(context.cfg.getWorkingDirectory(), inviteContentUI.textFieldCurrentHints.getText());
                    filesToCompress.add(relPath);
                }

                if (!filesToCompress.isEmpty())
                {
                    if (libMeta.repo.getLibraries().flavor.equalsIgnoreCase("Windows"))
                        attachment = makeZip(filesToCompress);
                    else
                        attachment = makeTar(filesToCompress);
                }

                if (!repoFilename.isEmpty())
                {
                    File reduced = new File(repoFilename);
                    if (reduced.exists())
                    {
                        Files.delete(reduced.toPath());
                    }
                }
            }
            else
                attachment = inviteContentUI.textFieldChoose.getText();
        }
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
            setLocation(context.preferences.getEmailInviteXpos(), context.preferences.getEmailInviteYpos());
        }
        else
        {
            setLocation(Utils.getRelativePosition(context.mainFrame, this));
        }

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

        comboBoxTemplates.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent itemEvent)
            {
                labelStatus.setText(" ");
            }
        });

        comboBoxServer.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent itemEvent)
            {
                labelStatus.setText(" ");
            }
        });

        // get email servers
        Tools tools = new Tools();
        try
        {
            emailToolList = tools.loadAllTools(context, EmailTool.INTERNAL_NAME);
            if (!emailToolList.isEmpty())
            {
                for (AbstractTool tool : emailToolList)
                {
                    comboBoxServer.addItem(tool);
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
            JOptionPane.showMessageDialog(this, context.cfg.gs("InviteUI.at.least.one.email.server.must.be.defined"),
                    context.cfg.gs("InviteUI.title"), JOptionPane.WARNING_MESSAGE);
            throw new MungeException("ignore this");
        }

        loadConfigurations();
        currentTemplate = (Template) comboBoxTemplates.getItemAt(0);
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
                                    logger.error(MessageFormat.format(context.cfg.gs("Listener.unknown.host.in.choice.whitelist.blacklist"), 0, line));
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

    private void loadConfigurations()
    {
        String userFormat = libMeta.repo.getLibraries().format;
        if (userFormat.equalsIgnoreCase("text"))
        {
            userFormat = "txt";
        }

        ArrayList<Template> templates = new ArrayList();
        Template tempTemplate = new Template(context, null, null, false, libMeta.repo);

        for (String name : tempTemplate.getBuiltIns())
        {
            String format = Utils.getFileExtension(name);
            if (format.equalsIgnoreCase(userFormat))
            {
                Template template = new Template(context, name, format, true, libMeta.repo);
                templates.add(template);
            }
        }

        File file = new File(tempTemplate.getFullPath(""));
        File[] files = FileSystemView.getFileSystemView().getFiles(file, Utils.isOsMac());

        for (File entry : files)
        {
            boolean has = false;
            String name = entry.getName();
            String format = Utils.getFileExtension(name);
            if (format.equalsIgnoreCase(userFormat))
            {
                for (Template template : templates)
                {
                    if (template.getFileName().equals(name))
                    {
                        has = true;
                        break;
                    }
                }

                if (!has)
                {
                    Template template = new Template(context, name, format, false, libMeta.repo);
                    templates.add(template);
                }
            }
        }

        for (Template template : templates)
        {
            comboBoxTemplates.addItem(template);
        }

        comboBoxTemplates.setSelectedIndex(0);
    }

    private String makeTar(ArrayList<String> filesToCompress)
    {
        String filename = "";

        try
        {
            String archive = "ELS-Configuration.tar";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, archive);
            File file = new File(filename);
            if (file.exists())
            {
                file.delete();
            }

            String tarFilePath = file.getAbsolutePath();
            TarArchiveOutputStream taos = new TarArchiveOutputStream(new FileOutputStream(tarFilePath));
            int count = 0;

            for (String path : filesToCompress)
            {
                File input = new File(path);
                if (!input.exists())
                {
                    logger.warn(context.cfg.gs("Repository.file.does.not.exist") + path);
                }
                else
                {
                    TarArchiveEntry entry = new TarArchiveEntry(input, path);
                    entry.setModTime(input.lastModified());
                    taos.putArchiveEntry(entry);

                    try (FileInputStream fis = new FileInputStream(path))
                    {
                        byte[] buffer = new byte[1024];

                        int len;
                        while ((len = fis.read(buffer)) > 0)
                        {
                            taos.write(buffer, 0, len);
                        }

                        ++count;
                        logger.info("  + " + context.cfg.gs("Archiver.compressed.file") + path);
                    }

                    taos.closeArchiveEntry();
                }
            }

            taos.close();
            String msg = count + context.cfg.gs("Archiver.files.successfully.to") + tarFilePath;
            logger.info(msg);
            labelStatus.setText(msg);
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }

        return filename;
    }

    private String makeZip(ArrayList<String> filesToCompress)
    {
        String filename = "";

        try
        {
            String archive = "ELS-Configuration.zip";
            filename = Utils.getTemporaryFilePrefix(context.publisherRepo, archive);
            File file = new File(filename);
            if (file.exists())
            {
                file.delete();
            }

            String zipFilePath = file.getAbsolutePath();
            ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(new FileOutputStream(zipFilePath));
            int count = 0;

            for (String path : filesToCompress)
            {
                File input = new File(path);
                if (!input.exists())
                {
                    logger.warn(context.cfg.gs("Repository.file.does.not.exist") + path);
                }
                else
                {
                    ZipArchiveEntry entry = new ZipArchiveEntry(path);
                    entry.setLastModifiedTime(FileTime.fromMillis(input.lastModified()));
                    zaos.putArchiveEntry(entry);

                    try (FileInputStream fis = new FileInputStream(path))
                    {
                        byte[] buffer = new byte[1024];

                        int len;
                        while ((len = fis.read(buffer)) > 0)
                        {
                            zaos.write(buffer, 0, len);
                        }

                        ++count;
                        logger.info("  + " + context.cfg.gs("Archiver.compressed.file") + path);
                    }

                    zaos.closeArchiveEntry();
                }
            }

            zaos.close();
            String msg = count + context.cfg.gs("Archiver.files.successfully.to") + zipFilePath;
            logger.info(msg);
            labelStatus.setText(msg);
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }

        return filename;
    }

    private void performAdds()
    {
        String address = Utils.parseHost(libMeta.repo.getLibraries().host);
        String key = libMeta.repo.getLibraries().key;
        String system = libMeta.repo.getLibraries().description;
        system = system.replaceAll(" ", "");

        if (checkBoxAuthKeys.isSelected())
        {
            FileEditor authEdit = new FileEditor(context, EditorTypes.Authentication);
            HintKeys keys = authKeys;
            if (keys.findKey(key) == null)
            {
                HintKey addKey = new HintKey();
                addKey.uuid = key;
                addKey.system = system;
                keys.get().add(addKey);
                authEdit.saveContent((ArrayList) null, keys, EditorTypes.Authentication);
            }
        }

        if (checkBoxHintKeys.isSelected())
        {
            FileEditor hintsEdit = new FileEditor(context, FileEditor.EditorTypes.HintKeys);
            HintKeys keys = hintKeys;
            if (keys.findKey(key) == null)
            {
                HintKey addKey = new HintKey();
                addKey.uuid = key;
                addKey.system = system;
                keys.get().add(addKey);
                hintsEdit.saveContent((ArrayList) null, keys, EditorTypes.HintKeys);
            }
        }

        if (checkBoxWhitelist.isSelected())
        {
            FileEditor wlEdit = new FileEditor(context, EditorTypes.WhiteList);
            ips = wlEdit.readIpAddresses(EditorTypes.WhiteList);
            if (!wlEdit.has(ips, address))
            {
                ips.add(address);
            }

            wlEdit.saveContent(ips, (HintKeys) null, FileEditor.EditorTypes.WhiteList);
        }

        context.navigator.enableDisableSystemMenus(null, true);
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
        Point location = getLocation();
        context.preferences.setEmailInviteXpos(location.x);
        context.preferences.setEmailInviteYpos(location.y);
        context.preferences.setEmailInviteLastServer(comboBoxServer.getSelectedItem().toString());
    }

    private void sendInviteEmail()
    {
        Tools emailTools = new Tools();

        try
        {
            emailTools.loadAllTools(context, "Email");
        }
        catch (Exception ex)
        {
            logger.error(Utils.getStackTrace(ex));
            JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        EmailTool tool = (EmailTool) emailTools.getTool("Email", server.getConfigName());
        if (tool != null)
        {
            from = tool.getUsername();
            EmailHandler emailHandler = new EmailHandler(context, this, tool, libMeta.repo.getLibraries().format, to, body, attachment);
            emailHandler.start();
        }
        else
        {
            String msg = context.cfg.gs("Process.email.tool.not.found") + server.getConfigName();
            logger.error(msg);
            JOptionPane.showMessageDialog(this, msg, context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setDialog()
    {
        boolean enabled = false;
        boolean listed = false;
        if (libMeta.repo.getLibraries().key != null && libMeta.repo.getLibraries().key.length() > 0)
        {
            try
            {
                if (isKeyListed(0, libMeta.repo.getLibraries().key))
                {
                    listed = true;
                }
                else
                {
                    enabled = true;
                }
            }
            catch (Exception ex)
            {
                logger.error(Utils.getStackTrace(ex));
                JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
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
            checkBoxAuthKeys.setSelected(true);
            checkBoxAuthKeys.setToolTipText(context.cfg.gs("InviteUI.add.to.authentication.keys.to.allow.login"));
        }
        else
        {
            labelAuthKeys.setEnabled(false);
            checkBoxAuthKeys.setEnabled(false);
            if (!listed)
            {
                checkBoxAuthKeys.setToolTipText(context.cfg.gs("InviteUI.requires.general.key"));
            }
        }

        enabled = false;
        listed = false;
        if (libMeta.repo.getLibraries().key != null && libMeta.repo.getLibraries().key.length() > 0)
        {
            try
            {
                if (isKeyListed(1, libMeta.repo.getLibraries().key))
                {
                    listed = true;
                }
                else
                {
                    enabled = true;
                }
            }
            catch (Exception ex)
            {
                logger.error(ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
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
            checkBoxHintKeys.setToolTipText(context.cfg.gs("InviteUI.only.add.to.hint.keys.if.performing.automated.back.ups"));
        }
        else
        {
            labelHintKeys.setEnabled(false);
            checkBoxHintKeys.setEnabled(false);
        }

        enabled = false;
        listed = false;
        if (libMeta.repo.getLibraries().host != null && libMeta.repo.getLibraries().host.length() > 0)
        {
            String host = Utils.parseHost(libMeta.repo.getLibraries().host);

            try
            {
                if (isWhitelisted(host))
                {
                    listed = true;
                }
                else
                {
                    enabled = true;
                }
            }
            catch (Exception ex)
            {
                logger.error(ex.getMessage());
                JOptionPane.showMessageDialog(this, ex.getMessage(), context.cfg.gs("InviteUI.title"), JOptionPane.ERROR_MESSAGE);
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
            checkBoxWhitelist.setSelected(true);
            checkBoxWhitelist.setToolTipText(context.cfg.gs("InviteUI.add.to.whitelist.to.allow.connection"));
        }
        else
        {
            labelWhitelist.setEnabled(false);
            checkBoxWhitelist.setEnabled(false);
            if (!listed)
            {
                checkBoxWhitelist.setToolTipText(context.cfg.gs("InviteUI.requires.general.host.name.or.ip.address"));
            }
        }

        String defSrv = "";
        if (context.preferences.getEmailInviteLastServer().length() > 0)
        {
            defSrv = context.preferences.getEmailInviteLastServer();
        }
        else if (context.preferences.getDefaultEmailServer().length() > 0)
        {
            defSrv = context.preferences.getDefaultEmailServer();
        }

        if (!defSrv.isEmpty())
        {
            for (int i = 0; i < comboBoxServer.getItemCount(); ++i)
            {
                if (((EmailTool) comboBoxServer.getItemAt(i)).getConfigName().equals(defSrv))
                {
                    comboBoxServer.setSelectedIndex(i);
                }
            }
        }

        textFieldTo.setText(libMeta.repo.getLibraries().email);
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        panelTop = new JPanel();
        panelTopTemplates = new JPanel();
        labelWhat = new JLabel();
        comboBoxTemplates = new JComboBox();
        buttonEditTemplate = new JButton();
        panelHelp = new JPanel();
        labelHelp = new JLabel();
        contentPanel = new JPanel();
        panelOptions = new JPanel();
        labelAuthKeys = new JLabel();
        checkBoxAuthKeys = new JCheckBox();
        labelHintKeys = new JLabel();
        checkBoxHintKeys = new JCheckBox();
        labelWhitelist = new JLabel();
        checkBoxWhitelist = new JCheckBox();
        labelServer = new JLabel();
        comboBoxServer = new JComboBox();
        labelTo = new JLabel();
        textFieldTo = new JTextField();
        panelActions = new JPanel();
        buttonPreview = new JButton();
        buttonBrowser = new JButton();
        hSpacer1 = new JPanel(null);
        buttonArchive = new JButton();
        buttonSend = new JButton();
        buttonBar = new JPanel();
        labelStatus = new JLabel();
        okButton = new JButton();

        //======== this ========
        setTitle(context.cfg.gs("InviteUI.title"));
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

                //======== panelTopTemplates ========
                {
                    panelTopTemplates.setMinimumSize(new Dimension(140, 38));
                    panelTopTemplates.setLayout(new FlowLayout(FlowLayout.LEFT, 2, 4));

                    //---- labelWhat ----
                    labelWhat.setText(context.cfg.gs("InviteUI.labelWhat.text"));
                    labelWhat.setFont(labelWhat.getFont().deriveFont(labelWhat.getFont().getStyle() & ~Font.BOLD, labelWhat.getFont().getSize() + 1f));
                    panelTopTemplates.add(labelWhat);

                    //---- comboBoxTemplates ----
                    comboBoxTemplates.setToolTipText(context.cfg.gs("InviteUI.comboBoxTemplates.toolTipText"));
                    comboBoxTemplates.addActionListener(e -> compTemplates(e));
                    panelTopTemplates.add(comboBoxTemplates);

                    //---- buttonEditTemplate ----
                    buttonEditTemplate.setText(context.cfg.gs("InviteUI.buttonEditTemplate.text"));
                    buttonEditTemplate.setMnemonic('E');
                    buttonEditTemplate.addActionListener(e -> actionEditClicked(e));
                    panelTopTemplates.add(buttonEditTemplate);
                }
                panelTop.add(panelTopTemplates, BorderLayout.WEST);

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

                //======== panelOptions ========
                {
                    panelOptions.setAlignmentY(0.0F);
                    panelOptions.setLayout(new GridBagLayout());
                    ((GridBagLayout)panelOptions.getLayout()).columnWidths = new int[] {0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0};
                    ((GridBagLayout)panelOptions.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
                    ((GridBagLayout)panelOptions.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                    //---- labelAuthKeys ----
                    labelAuthKeys.setText(context.cfg.gs("InviteUI.labelAuthKeys.text"));
                    panelOptions.add(labelAuthKeys, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 15, 4, 4), 0, 0));

                    //---- checkBoxAuthKeys ----
                    checkBoxAuthKeys.setToolTipText(context.cfg.gs("InviteUI.checkBoxAuthKeys.toolTipText"));
                    checkBoxAuthKeys.addActionListener(e -> compAuthKeys(e));
                    panelOptions.add(checkBoxAuthKeys, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- labelHintKeys ----
                    labelHintKeys.setText(context.cfg.gs("InviteUI.labelHintKeys.text"));
                    panelOptions.add(labelHintKeys, new GridBagConstraints(0, 2, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 15, 4, 4), 0, 0));

                    //---- checkBoxHintKeys ----
                    checkBoxHintKeys.addActionListener(e -> compHintKeys(e));
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
                    checkBoxWhitelist.addActionListener(e -> compWhitelist(e));
                    panelOptions.add(checkBoxWhitelist, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //---- labelServer ----
                    labelServer.setText(context.cfg.gs("InviteUI.labelServer.text"));
                    panelOptions.add(labelServer, new GridBagConstraints(0, 5, 1, 1, 0.0, 0.0,
                        GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                        new Insets(0, 10, 4, 4), 0, 0));

                    //---- comboBoxServer ----
                    comboBoxServer.setToolTipText(context.cfg.gs("InviteUI.comboBoxServer.toolTipText"));
                    comboBoxServer.addActionListener(e -> compServer(e));
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
                    textFieldTo.setMinimumSize(new Dimension(240, 22));
                    textFieldTo.setToolTipText(context.cfg.gs("InviteUI.textFieldTo.toolTipText"));
                    textFieldTo.setPreferredSize(new Dimension(240, 22));
                    textFieldTo.addActionListener(e -> compTo(e));
                    panelOptions.add(textFieldTo, new GridBagConstraints(1, 6, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                        new Insets(0, 0, 4, 0), 0, 0));

                    //======== panelActions ========
                    {
                        panelActions.setLayout(new FlowLayout(FlowLayout.CENTER, 4, 4));

                        //---- buttonPreview ----
                        buttonPreview.setText(context.cfg.gs("InviteUI.buttonPreview.text"));
                        buttonPreview.setToolTipText(context.cfg.gs("EmailTemplates.buttonPreview.toolTipText"));
                        buttonPreview.setMnemonic('P');
                        buttonPreview.addActionListener(e -> actionPreviewClicked(e));
                        panelActions.add(buttonPreview);

                        //---- buttonBrowser ----
                        buttonBrowser.setText(context.cfg.gs("InviteUI.buttonBrowser.text"));
                        buttonBrowser.setToolTipText(context.cfg.gs("EmailTemplates.buttonBrowser.toolTipText"));
                        buttonBrowser.setMnemonic('B');
                        buttonBrowser.addActionListener(e -> actionBrowserClicked(e));
                        panelActions.add(buttonBrowser);

                        //---- hSpacer1 ----
                        hSpacer1.setPreferredSize(new Dimension(22, 10));
                        hSpacer1.setMinimumSize(new Dimension(22, 12));
                        panelActions.add(hSpacer1);

                        //---- buttonArchive ----
                        buttonArchive.setText(context.cfg.gs("InviteUI.buttonArchive.text"));
                        buttonArchive.setToolTipText(context.cfg.gs("InviteUI.buttonArchive.toolTipText"));
                        buttonArchive.addActionListener(e -> actionArchiveClicked(e));
                        panelActions.add(buttonArchive);

                        //---- buttonSend ----
                        buttonSend.setText(context.cfg.gs("InviteUI.buttonSend.text"));
                        buttonSend.setToolTipText(context.cfg.gs("InviteUI.buttonSend.toolTipText"));
                        buttonSend.setMnemonic('S');
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
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel panelTop;
    private JPanel panelTopTemplates;
    private JLabel labelWhat;
    private JComboBox comboBoxTemplates;
    private JButton buttonEditTemplate;
    private JPanel panelHelp;
    private JLabel labelHelp;
    private JPanel contentPanel;
    private JPanel panelOptions;
    private JLabel labelAuthKeys;
    private JCheckBox checkBoxAuthKeys;
    private JLabel labelHintKeys;
    private JCheckBox checkBoxHintKeys;
    private JLabel labelWhitelist;
    private JCheckBox checkBoxWhitelist;
    private JLabel labelServer;
    private JComboBox comboBoxServer;
    private JLabel labelTo;
    private JTextField textFieldTo;
    private JPanel panelActions;
    private JButton buttonPreview;
    private JButton buttonBrowser;
    private JPanel hSpacer1;
    private JButton buttonArchive;
    private JButton buttonSend;
    private JPanel buttonBar;
    public JLabel labelStatus;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on

    //
    // @formatter:on
    // </editor-fold>
}
