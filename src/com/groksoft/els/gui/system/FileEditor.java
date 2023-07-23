package com.groksoft.els.gui.system;

import java.awt.*;
import java.awt.event.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import javax.swing.border.*;

import com.groksoft.els.Context;
import com.groksoft.els.Utils;
import com.groksoft.els.gui.MainFrame;
import com.groksoft.els.gui.NavHelp;
import com.groksoft.els.repository.HintKey;
import com.groksoft.els.repository.HintKeys;
import com.groksoft.els.repository.RepoMeta;
import com.groksoft.els.repository.Repositories;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;

@SuppressWarnings(value = "unchecked")

/**
 * FileEditor class.
 *
 * Edits the fixed-name system files. Only one file is edited at a time.
 */
public class FileEditor extends JDialog
{
    private Context context;
    private DataTableModel dataTableModel;
    private String description;
    private String displayName;
    private String fileName;
    private NavHelp helpDialog;
    private String helpPrefix;
    private String helpTip;
    private HintKeys hintKeys = null;
    private ArrayList<String> ipAddresses = null;
    private Logger logger = LogManager.getLogger("applog");
    private MainFrame mf;
    private EditorTypes type;

    public static enum EditorTypes
    {Authorization, HintKeys, BlackList, WhiteList}

    ;

    private FileEditor()
    {
        // hide default constructor
    }

    public FileEditor(Context context, EditorTypes type)
    {
        super(context.mainFrame);
        this.context = context;
        this.mf = context.mainFrame;
        this.type = type;
        initComponents();
        initialize();
        process();
    }

    private void actionAddClicked(ActionEvent e)
    {
        switch (type)
        {
            case Authorization:
            case HintKeys:
                if (hintKeys.get().size() == 0 || hintKeys.get().get(hintKeys.get().size() - 1).name != null)
                {
                    HintKey hintKey = new HintKey();
                    hintKeys.get().add(hintKey);
                    dataTableModel.fireTableDataChanged();
                }
                tableContent.editCellAt(dataTableModel.getRowCount() - 1, 0);
                tableContent.changeSelection(dataTableModel.getRowCount() - 1, dataTableModel.getRowCount() - 1, false, false);
                tableContent.getEditorComponent().requestFocus();
                ((JTextField) tableContent.getEditorComponent()).selectAll();
                break;
            case BlackList:
            case WhiteList:
                if (ipAddresses.size() == 0 || ipAddresses.get(ipAddresses.size() - 1) != null)
                {
                    ipAddresses.add("");
                    dataTableModel.fireTableDataChanged();
                }
                tableContent.editCellAt(dataTableModel.getRowCount() - 1, 0);
                tableContent.changeSelection(dataTableModel.getRowCount() - 1, dataTableModel.getRowCount() - 1, false, false);
                tableContent.getEditorComponent().requestFocus();
                ((JTextField) tableContent.getEditorComponent()).selectAll();
                break;
        }

    }

    private void actionCancelClicked(ActionEvent e)
    {
        if (dataTableModel.isDataChanged())
        {
            int reply = JOptionPane.showConfirmDialog(this, context.cfg.gs("Z.cancel.all.changes"),
                    context.cfg.gs("Z.cancel.changes"), JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                setVisible(false);
            }
        }
        else
        {
            setVisible(false);
        }
        setMenuItems();
    }

    private void actionHelpClicked(MouseEvent e)
    {
        if (helpDialog == null || !helpDialog.isVisible())
        {
            String title = "";
            String helpName = "";
            switch (type)
            {
                case Authorization:
                    title = context.cfg.gs("FileEditor.labelHelpAuthorization.toolTipText");
                    helpName = "authorization-keys_";
                    break;
                case HintKeys:
                    title = context.cfg.gs("FileEditor.labelHelpHint.toolTipText");
                    helpName = "hint-keys_";
                    break;
                case BlackList:
                    title = context.cfg.gs("FileEditor.labelHelpBlacklist.toolTipText");
                    helpName = "blacklist_";
                    break;
                case WhiteList:
                    title = context.cfg.gs("FileEditor.labelHelpWhitelist.toolTipText");
                    helpName = "whitelist_";
                    break;
            }
            helpDialog = new NavHelp(this, this, context, title, helpName + context.preferences.getLocale() + ".html");
        }
        if (!helpDialog.isVisible())
        {
            // offset the help dialog from the parent dialog
            Point loc = this.getLocation();
            loc.x = loc.x + 32;
            loc.y = loc.y + 32;
            helpDialog.setLocation(loc);
            helpDialog.setVisible(true);
        }
        else
        {
            helpDialog.toFront();
        }
    }

    private void actionRemoveClicked(ActionEvent e)
    {
        int[] rows = tableContent.getSelectedRows();
        if (rows.length > 0)
        {
            tableContent.requestFocus();
            int count = rows.length;

            // make dialog pieces
            String message = java.text.MessageFormat.format(context.cfg.gs("FileEditor.remove.entries"), count);

            // confirm deletions
            int reply = JOptionPane.showConfirmDialog(this, message, displayName, JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.YES_OPTION)
            {
                for (int i = rows.length - 1; i >= 0; --i)
                {
                    dataTableModel.removeRow(rows[i]);
                }
                dataTableModel.setDataHasChanged();
                dataTableModel.fireTableDataChanged();
            }
        }
    }

    private void actionSaveClicked(ActionEvent e)
    {
        if (saveContent())
        {
            savePreferences();
            setMenuItems();
            setVisible(false);
        }
    }

    private void actionUuidClicked(ActionEvent e)
    {
        if (tableContent.getSelectedRows().length > 0)
        {
            Repositories repositories = getRepositories();
            for (RepoMeta repoMeta : repositories.getList())
            {
                
            }
        }
    }

    private String getConfigName()
    {
        return fileName;
    }

    private String getDirectoryPath()
    {
        String path = System.getProperty("user.dir") + System.getProperty("file.separator") +
                (getSubsystem().length() > 0 ? getSubsystem() : "");
        return path;
    }

    private String getFullPath()
    {
        String path = getDirectoryPath() + System.getProperty("file.separator") +
                Utils.scrubFilename(getConfigName());
        return path;
    }

    public Repositories getRepositories()
    {
        Repositories repositories = null;
        try
        {
            repositories = new Repositories();
            repositories.loadList(context);
        }
        catch (Exception e)
        {
            String msg = context.cfg.gs("Z.exception") + Utils.getStackTrace(e);
            logger.error(msg);
            JOptionPane.showMessageDialog(mf, msg, displayName, JOptionPane.ERROR_MESSAGE);
        }
        return repositories;
    }

    private String getSubsystem()
    {
        return "system";
    }

    private void process()
    {
        setVisible(true);
    }

    private void initialize()
    {
        // scale the help icon
        Icon icon = labelSystemHelp.getIcon();
        Image image = Utils.iconToImage(icon);
        Image scaled = image.getScaledInstance(30, 30, Image.SCALE_SMOOTH);
        Icon replacement = new ImageIcon(scaled);
        labelSystemHelp.setIcon(replacement);

        // position, size & dividers
        if (context.preferences.getFileEditorXpos() > 0)
        {
            Dimension dim = new Dimension(context.preferences.getFileEditorWidth(), context.preferences.getFileEditorHeight());
            this.setSize(dim);
            this.setLocation(context.preferences.getFileEditorXpos(), context.preferences.getFileEditorYpos());
        }
        else
        {
            Point parentPos = this.getParent().getLocation();
            Dimension parentSize = this.getParent().getSize();
            Dimension mySize = this.getSize();
            Point myPos = new Point(parentPos.x + (parentSize.width / 2 - mySize.width / 2),
                    parentPos.y + (parentSize.height / 2 - mySize.height / 2));
            this.setLocation(myPos);
        }

        // Escape key
        ActionListener escListener = new AbstractAction()
        {
            @Override
            public void actionPerformed(ActionEvent actionEvent)
            {
                cancelButton.doClick();
            }
        };
        getRootPane().registerKeyboardAction(escListener, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        switch (type)
        {
            case Authorization:
                displayName = context.cfg.gs("Authorization Keys");
                description = context.cfg.gs("Library keys authorized to connect.");
                helpPrefix = "authorization";
                buttonUuidList.setVisible(true);
                mf.menuItemAuthKeys.setEnabled(true);
                mf.menuItemHintKeys.setEnabled(false);
                mf.menuItemBlacklist.setEnabled(false);
                mf.menuItemWhitelist.setEnabled(false);
                break;
            case HintKeys:
                displayName = context.cfg.gs("Hint Keys");
                description = context.cfg.gs("Library keys involved in processing Hints.");
                helpPrefix = "hintkeys";
                buttonUuidList.setVisible(true);
                mf.menuItemAuthKeys.setEnabled(false);
                mf.menuItemHintKeys.setEnabled(true);
                mf.menuItemBlacklist.setEnabled(false);
                mf.menuItemWhitelist.setEnabled(false);
                break;
            case BlackList:
                displayName = context.cfg.gs("Blacklist");
                description = context.cfg.gs("IP addresses that may not connect.");
                helpPrefix = "blacklist";
                buttonUuidList.setVisible(false);
                mf.menuItemAuthKeys.setEnabled(false);
                mf.menuItemHintKeys.setEnabled(false);
                mf.menuItemBlacklist.setEnabled(true);
                mf.menuItemWhitelist.setEnabled(false);
                break;
            case WhiteList:
                displayName = context.cfg.gs("Whitelist");
                description = context.cfg.gs("IP addresses authorized to connect.");
                helpPrefix = "whitelist";
                buttonUuidList.setVisible(false);
                mf.menuItemAuthKeys.setEnabled(false);
                mf.menuItemHintKeys.setEnabled(false);
                mf.menuItemBlacklist.setEnabled(false);
                mf.menuItemWhitelist.setEnabled(true);
                break;
        }
        helpTip = displayName + context.cfg.gs(" Help");

        setTitle(displayName);
        labelDescription.setText(description);
        labelSystemHelp.setToolTipText(helpTip);

        loadTable();
    }

    private void loadTable()
    {
        HintKeys hintKeys = null;
        ArrayList<String> ipAddresses = null;

        switch (type)
        {
            case Authorization:
                hintKeys = readHintKeys();
                break;
            case HintKeys:
                hintKeys = readHintKeys();
                break;
            case BlackList:
                ipAddresses = readIpAddresses();
                break;
            case WhiteList:
                ipAddresses = readIpAddresses();
                break;
        }

        dataTableModel = new DataTableModel(context, hintKeys != null ? hintKeys.get() : null, ipAddresses);
        tableContent.setModel(dataTableModel);
    }

    private HintKeys readHintKeys()
    {
        hintKeys = new HintKeys(context);
        try
        {
            switch (type)
            {
                case Authorization:
                    fileName = "authorization.keys";
                    break;
                case HintKeys:
                    fileName = "hint.keys";
                    break;
            }
            fileName = getFullPath();

            File file = new File(fileName);
            if (file.exists())
            {
                hintKeys.read(fileName);
            }
            else
            {
                hintKeys.setFilename(fileName);
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        return hintKeys;
    }

    private ArrayList<String> readIpAddresses()
    {
        ipAddresses = new ArrayList<>();
        try
        {
            switch (type)
            {
                case BlackList:
                    fileName = "blacklist.txt";
                    break;
                case WhiteList:
                    fileName = "whitelist.txt";
                    break;
            }
            fileName = getFullPath();

            File file = new File(fileName);
            if (file.exists())
            {
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String line;
                while ((line = br.readLine()) != null)
                {
                    line = line.trim();
                    if (line.length() > 0 && !line.startsWith("#"))
                    {
                        ipAddresses.add(line);
                    }
                }
                br.close();
            }
        }
        catch (Exception e)
        {
            logger.error(Utils.getStackTrace(e));
        }
        logger.info("Read IP addresses " + fileName + " successfully");
        return ipAddresses;
    }

    private boolean saveContent()
    {
        if (tableContent.isEditing())
            tableContent.getCellEditor().stopCellEditing();

        if (dataTableModel.isDataChanged())
        {
            try
            {
                String header = "";
                switch (type)
                {
                    case Authorization:
                        header = "# ELS Authorization Keys" + System.getProperty("line.separator");
                        break;
                    case HintKeys:
                        header = "# ELS Hint Keys" + System.getProperty("line.separator");
                        break;
                    case BlackList:
                        header = "# ELS Blacklist" + System.getProperty("line.separator");
                        break;
                    case WhiteList:
                        header = "# ELS Whitelist" + System.getProperty("line.separator");
                        break;
                }
                switch (type)
                {
                    case Authorization:
                    case HintKeys:
                        header += "#" + System.getProperty("line.separator") +
                                "# Format: name uuid" + System.getProperty("line.separator") +
                                "#" + System.getProperty("line.separator") +
                                "#   Name is any shortname, no spaces." + System.getProperty("line.separator") +
                                "#   UUID is the key from the library JSON file." + System.getProperty("line.separator") +
                                "#" + System.getProperty("line.separator") +
                                "# Use space/tab separators, no quotes." + System.getProperty("line.separator") +
                                "#" + System.getProperty("line.separator");
                        hintKeys.write(header);
                        break;
                    case BlackList:
                    case WhiteList:
                        header += "#" + System.getProperty("line.separator") +
                                "# Format: xxx.xxx.xxx.xxx" + System.getProperty("line.separator") +
                                "#" + System.getProperty("line.separator") +
                                "#   A standard IPv4 address." + System.getProperty("line.separator") +
                                "#   One address per line." + System.getProperty("line.separator") +
                                "#" + System.getProperty("line.separator");
                        writeIpAddresses(header);
                        break;
                }
            }
            catch (Exception e)
            {
                logger.error(Utils.getStackTrace(e));
            }


        }
        return true;
    }

    private void savePreferences()
    {
        context.preferences.setFileEditorHeight(this.getHeight());
        context.preferences.setFileEditorWidth(this.getWidth());
        Point location = this.getLocation();
        context.preferences.setFileEditorXpos(location.x);
        context.preferences.setFileEditorYpos(location.y);
    }

    private void setMenuItems()
    {
        mf.menuItemAuthKeys.setEnabled(true);
        mf.menuItemHintKeys.setEnabled(true);
        mf.menuItemBlacklist.setEnabled(true);
        mf.menuItemWhitelist.setEnabled(true);
    }

    private void writeIpAddresses(String header) throws Exception
    {
        if (ipAddresses != null)
        {
            BufferedWriter bw = new BufferedWriter(new FileWriter(fileName));
            bw.write(header);
            bw.write(System.getProperty("line.separator"));
            for (int i = 0; i < ipAddresses.size(); ++i)
            {
                if (ipAddresses.get(i) != null && ipAddresses.get(i).length() > 0)
                    bw.write(ipAddresses.get(i) + System.getProperty("line.separator"));
            }
            bw.write(System.getProperty("line.separator"));
            bw.close();
        }
    }

    private void windowClosing(WindowEvent e)
    {
        cancelButton.doClick();
    }

    // ================================================================================================================

    // <editor-fold desc="Generated code (Fold)">
    // @formatter:off
    //
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        ResourceBundle bundle = ResourceBundle.getBundle("com.groksoft.els.locales.bundle");
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        labelDescription = new JLabel();
        labelSystemHelp = new JLabel();
        scrollPane = new JScrollPane();
        tableContent = new JTable();
        panelActionButtons = new JPanel();
        buttonAdd = new JButton();
        buttonRemove = new JButton();
        hSpacer1 = new JPanel(null);
        buttonUuidList = new JButton();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();

        //======== this ========
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                FileEditor.this.windowClosing(e);
            }
        });
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setPreferredSize(new Dimension(560, 390));
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new GridBagLayout());
                ((GridBagLayout)contentPanel.getLayout()).columnWidths = new int[] {0, 0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).rowHeights = new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
                ((GridBagLayout)contentPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};
                ((GridBagLayout)contentPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

                //---- labelDescription ----
                labelDescription.setPreferredSize(new Dimension(360, 30));
                contentPanel.add(labelDescription, new GridBagConstraints(0, 0, 6, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 4), 0, 0));

                //---- labelSystemHelp ----
                labelSystemHelp.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
                labelSystemHelp.setPreferredSize(new Dimension(32, 30));
                labelSystemHelp.setMinimumSize(new Dimension(32, 30));
                labelSystemHelp.setMaximumSize(new Dimension(32, 30));
                labelSystemHelp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                labelSystemHelp.setIconTextGap(0);
                labelSystemHelp.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        actionHelpClicked(e);
                    }
                });
                contentPanel.add(labelSystemHelp, new GridBagConstraints(6, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.EAST, GridBagConstraints.VERTICAL,
                    new Insets(0, 0, 4, 0), 0, 0));

                //======== scrollPane ========
                {

                    //---- tableContent ----
                    tableContent.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                    tableContent.setShowVerticalLines(false);
                    scrollPane.setViewportView(tableContent);
                }
                contentPanel.add(scrollPane, new GridBagConstraints(0, 1, 7, 10, 1.0, 1.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 4, 0), 0, 0));

                //======== panelActionButtons ========
                {
                    panelActionButtons.setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

                    //---- buttonAdd ----
                    buttonAdd.setText(bundle.getString("FileEditor.buttonAdd.text"));
                    buttonAdd.setFont(buttonAdd.getFont().deriveFont(buttonAdd.getFont().getSize() - 2f));
                    buttonAdd.setPreferredSize(new Dimension(78, 24));
                    buttonAdd.setMinimumSize(new Dimension(78, 24));
                    buttonAdd.setMaximumSize(new Dimension(78, 24));
                    buttonAdd.setMnemonic(bundle.getString("FileEditor.buttonAdd.mnemonic").charAt(0));
                    buttonAdd.setToolTipText(bundle.getString("FileEditor.buttonAdd.toolTipText"));
                    buttonAdd.setMargin(new Insets(0, -10, 0, -10));
                    buttonAdd.addActionListener(e -> actionAddClicked(e));
                    panelActionButtons.add(buttonAdd);

                    //---- buttonRemove ----
                    buttonRemove.setText(bundle.getString("FileEditor.buttonRemove.text"));
                    buttonRemove.setFont(buttonRemove.getFont().deriveFont(buttonRemove.getFont().getSize() - 2f));
                    buttonRemove.setPreferredSize(new Dimension(78, 24));
                    buttonRemove.setMinimumSize(new Dimension(78, 24));
                    buttonRemove.setMaximumSize(new Dimension(78, 24));
                    buttonRemove.setMnemonic(bundle.getString("FileEditor.buttonRemove.mnemonic").charAt(0));
                    buttonRemove.setToolTipText(bundle.getString("FileEditor.buttonRemove.toolTipText"));
                    buttonRemove.setMargin(new Insets(0, -10, 0, -10));
                    buttonRemove.addActionListener(e -> actionRemoveClicked(e));
                    panelActionButtons.add(buttonRemove);

                    //---- hSpacer1 ----
                    hSpacer1.setPreferredSize(new Dimension(22, 10));
                    hSpacer1.setMinimumSize(new Dimension(22, 12));
                    panelActionButtons.add(hSpacer1);

                    //---- buttonUuidList ----
                    buttonUuidList.setText(bundle.getString("FileEditor.buttonUuidList.text"));
                    buttonUuidList.setFont(buttonUuidList.getFont().deriveFont(buttonUuidList.getFont().getSize() - 2f));
                    buttonUuidList.setPreferredSize(new Dimension(78, 24));
                    buttonUuidList.setMinimumSize(new Dimension(78, 24));
                    buttonUuidList.setMaximumSize(new Dimension(78, 24));
                    buttonUuidList.setMnemonic('U');
                    buttonUuidList.setToolTipText(bundle.getString("FileEditor.buttonUuidList.toolTipText"));
                    buttonUuidList.setMargin(new Insets(0, -10, 0, -10));
                    buttonUuidList.addActionListener(e -> actionUuidClicked(e));
                    panelActionButtons.add(buttonUuidList);
                }
                contentPanel.add(panelActionButtons, new GridBagConstraints(0, 11, 7, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setPreferredSize(new Dimension(190, 36));
                buttonBar.setMinimumSize(new Dimension(190, 36));
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout)buttonBar.getLayout()).columnWidths = new int[] {0, 85, 80};
                ((GridBagLayout)buttonBar.getLayout()).columnWeights = new double[] {1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText(bundle.getString("Z.save"));
                okButton.setToolTipText(bundle.getString("Z.save.toolTip.text"));
                okButton.addActionListener(e -> actionSaveClicked(e));
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                    GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                    new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText(bundle.getString("Z.cancel"));
                cancelButton.setToolTipText(bundle.getString("Z.cancel.changes.toolTipText"));
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
    private JLabel labelDescription;
    private JLabel labelSystemHelp;
    private JScrollPane scrollPane;
    private JTable tableContent;
    private JPanel panelActionButtons;
    private JButton buttonAdd;
    private JButton buttonRemove;
    private JPanel hSpacer1;
    private JButton buttonUuidList;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
    //
    // @formatter:on
    // </editor-fold>
}
